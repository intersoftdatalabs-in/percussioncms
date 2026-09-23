/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Playwright surface: #4766 / parent #4531 — PublishingShell log item
 * opens the React content editor. 403/404 stay on the log detail.
 *
 * Tags: @publishing-log-open-editor @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-log-open-editor.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { isPubstatusLogsUrl } = require("./helpers/publishing-logs-filter");
const {
  publishingLogsUrl,
  isKnownPublishConsoleNoise,
  isLogDetailsUrl,
  isItemFieldsUrl,
} = require("./helpers/publishing-log-open-editor");

const TAGS = ["@publishing-log-open-editor", "@publish", "@smoke"];

const LOGS = [
  {
    jobId: 4766,
    siteName: "FastForward",
    serverName: "prod",
    status: "Completed",
  },
];

const DETAILS = {
  SitePublishItem: [
    {
      contentid: 4766,
      status: "Success",
      operation: "publish",
      fileName: "home.html",
    },
  ],
};

function isIntentionalProbeNoise(text) {
  return /Failed to load resource: the server responded with a status of (403|404)/i.test(
    String(text || ""),
  );
}

function attachConsole(page, consoleErrors) {
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const text = msg.text();
      if (!isKnownPublishConsoleNoise(text) && !isIntentionalProbeNoise(text)) {
        consoleErrors.push(text);
      }
    }
  });
}

/** about:blank reserved on click is not an editor open; it must close. */
async function expectNoEditorPopup(popups) {
  for (const popup of popups) {
    const url = popup.url();
    expect(url).not.toMatch(/entry=editor/);
    expect(url).not.toMatch(/contentId=/);
    if (!popup.isClosed()) {
      await popup.waitForEvent("close", { timeout: 5000 }).catch(() => undefined);
    }
    expect(popup.isClosed() || !/entry=editor/.test(popup.url())).toBe(true);
  }
}

async function stubLogs(page, fieldsStatus) {
  await page.route("**/sitemanage/pubstatus/logs**", async (route) => {
    if (
      isPubstatusLogsUrl(route.request().url()) &&
      route.request().method() === "POST"
    ) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(LOGS),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/sitemanage/pubstatus/details**", async (route) => {
    if (
      isLogDetailsUrl(route.request().url()) &&
      route.request().method() === "POST"
    ) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(DETAILS),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/itemmanagement/item/fields/**", async (route) => {
    if (
      isItemFieldsUrl(route.request().url()) &&
      route.request().method() === "GET"
    ) {
      await route.fulfill({
        status: fieldsStatus,
        contentType: "application/json",
        body: JSON.stringify(
          fieldsStatus === 200
            ? { ItemEditorFields: { contentId: "4766", fields: [] } }
            : { message: "denied" },
        ),
      });
      return;
    }
    await route.continue();
  });
}

test.describe("PublishingShell open log item in editor (#4766)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`opens the editor for a log item content id ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
    attachConsole(page, consoleErrors);
    await stubLogs(page, 200);

    await page.goto(publishingLogsUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await page.getByTestId("logs-filter-apply").click();
    await expect(page.getByTestId("publish-log-row-4766")).toBeVisible({
      timeout: 15000,
    });
    await page.getByRole("button", { name: "details" }).click();
    await expect(page.getByTestId("publish-log-details")).toBeVisible();
    await page.getByRole("button", { name: /item details/i }).click();

    const popupPromise = page.waitForEvent("popup", { timeout: 15000 });
    await page.getByTestId("publish-log-open-editor").click();
    const popup = await popupPromise;
    await expect(popup).toHaveURL(/entry=editor/);
    await expect(popup).toHaveURL(/contentId=4766/);
    await expect(page.getByTestId("publish-log-item-open-error")).toHaveCount(0);
    expect(consoleErrors).toEqual([]);
    await popup.close();
  });

  test(`maps 403 without opening the editor ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
    attachConsole(page, consoleErrors);
    await stubLogs(page, 403);
    const popups = [];
    page.on("popup", (popup) => {
      popups.push(popup);
    });

    await page.goto(publishingLogsUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await page.getByTestId("logs-filter-apply").click();
    await page.getByRole("button", { name: "details" }).click();
    await page.getByRole("button", { name: /item details/i }).click();
    await page.getByTestId("publish-log-open-editor").click();
    await expect(page.getByTestId("publish-log-item-open-error")).toContainText(
      /not allowed/i,
      { timeout: 10000 },
    );
    await expectNoEditorPopup(popups);
    expect(consoleErrors).toEqual([]);
  });

  test(`maps 404 without opening the editor ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
    attachConsole(page, consoleErrors);
    await stubLogs(page, 404);

    await page.goto(publishingLogsUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await page.getByTestId("logs-filter-apply").click();
    await page.getByRole("button", { name: "details" }).click();
    await page.getByRole("button", { name: /item details/i }).click();
    await page.getByTestId("publish-log-open-editor").click();
    await expect(page.getByTestId("publish-log-item-open-error")).toContainText(
      /not found/i,
      { timeout: 10000 },
    );
    expect(consoleErrors).toEqual([]);
  });
});
