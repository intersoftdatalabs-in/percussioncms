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
 * PublishingShell Status job items open in the editor (#4886 / parent #4531).
 *
 * Surface filter (H2 QA):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/statusJobItems.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  publishingProductUrl,
  isCurrentStatusUrl,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-cancel-job");
const {
  isLogDetailsUrl,
  isItemFieldsUrl,
} = require("../helpers/publishing-log-open-editor");

const JOBS = [
  {
    jobId: 4886,
    siteName: "FastForward",
    siteId: 10,
    editionName: "Nightly Full",
    status: "Running",
    completedItems: 1,
    totalItems: 2,
  },
];

const DETAILS = {
  SitePublishItem: [
    { contentid: 4886, title: "Home page", fileName: "index.html" },
    { fileName: "no-id.html" },
  ],
};

function isProbeNoise(text) {
  return /Failed to load resource: the server responded with a status of (403|404)/i.test(
    String(text || ""),
  );
}

async function openStatus(page, detailsStatus, detailsBody) {
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const text = msg.text();
      if (!isKnownPublishConsoleNoise(text) && !isProbeNoise(text)) {
        consoleErrors.push(text);
      }
    }
  });
  await page.route("**/sitemanage/pubstatus/current**", async (route) => {
    if (
      isCurrentStatusUrl(route.request().url()) &&
      route.request().method() === "GET"
    ) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(JOBS),
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
        status: detailsStatus,
        contentType: "application/json",
        body: JSON.stringify(
          detailsBody != null
            ? detailsBody
            : detailsStatus === 200
              ? DETAILS
              : { message: "denied" },
        ),
      });
      return;
    }
    await route.continue();
  });
  await page.goto(publishingProductUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await page.getByTestId("publish-status-job-4886").click();
  await expect(page.getByTestId("publish-status-job-detail")).toBeVisible();
  return consoleErrors;
}

test.describe("PublishingShell Status job items (#4886)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("lists items and opens the editor", async ({ page }) => {
    await page.route("**/itemmanagement/item/fields/**", async (route) => {
      if (
        isItemFieldsUrl(route.request().url()) &&
        route.request().method() === "GET"
      ) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorFields: { contentId: "4886", fields: [] },
          }),
        });
        return;
      }
      await route.continue();
    });
    const consoleErrors = await openStatus(page, 200);
    await expect(page.getByTestId("publish-status-item-id-4886")).toHaveText(
      "4886",
    );
    await expect(page.getByTestId("publish-status-item-name-4886")).toHaveText(
      "Home page",
    );
    await expect(page.getByTestId("publish-status-items-empty")).toHaveCount(0);
    await expect(page.getByTestId("publish-status-open-item-4886")).toBeVisible();
    const noIdRow = page.getByTestId("publish-status-item-row-1");
    await expect(noIdRow).toBeVisible();
    await expect(noIdRow.getByRole("button")).toHaveCount(0);

    const popupPromise = page.waitForEvent("popup", { timeout: 15000 });
    await page.getByTestId("publish-status-open-item-4886").click();
    const popup = await popupPromise;
    await expect(popup).toHaveURL(/spa\.jsp\?/);
    await expect(popup).toHaveURL(/entry=editor/);
    await expect(popup).toHaveURL(/contentId=4886/);
    await expect(page.getByTestId("publish-status-job-detail")).toBeVisible();
    expect(consoleErrors).toEqual([]);
    await popup.close();
  });

  test("empty item list is an empty state", async ({ page }) => {
    const consoleErrors = await openStatus(page, 200, { SitePublishItem: [] });
    await expect(page.getByTestId("publish-status-items-empty")).toHaveText(
      "No content items for this job",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("403 on the item list stays in the detail panel", async ({ page }) => {
    const consoleErrors = await openStatus(page, 403);
    await expect(page.getByTestId("publish-status-items-error")).toContainText(
      /not allowed/i,
    );
    await expect(page.getByTestId("publish-status-detail-job-id")).toHaveText(
      "4886",
    );
    await expect(page.getByTestId("publish-status-items-empty")).toHaveCount(0);
    expect(consoleErrors).toEqual([]);
  });

  test("404 on the item list stays in the detail panel", async ({ page }) => {
    const consoleErrors = await openStatus(page, 404);
    await expect(page.getByTestId("publish-status-items-error")).toContainText(
      /not found/i,
    );
    await expect(page.getByTestId("publish-status-job-detail")).toBeVisible();
    expect(consoleErrors).toEqual([]);
  });
});
