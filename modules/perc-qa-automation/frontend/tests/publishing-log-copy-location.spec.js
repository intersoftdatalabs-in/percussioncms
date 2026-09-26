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
 * Playwright surface: #4937 / parent #4531 — PublishingShell copy a log
 * item location. Dismiss does not close the details. A blank location is
 * named and is not written.
 *
 * Tags: @publishing-log-copy-location @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-log-copy-location.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { isPubstatusLogsUrl } = require("./helpers/publishing-logs-filter");
const {
  publishingLogsUrl,
  isKnownPublishConsoleNoise,
  isLogDetailsUrl,
} = require("./helpers/publishing-log-open-editor");

const TAGS = ["@publishing-log-copy-location", "@publish", "@smoke"];

const LOGS = [
  {
    jobId: 4937,
    siteName: "FastForward",
    serverName: "prod",
    status: "Completed",
  },
];

const DETAILS = {
  SitePublishItem: [
    {
      contentid: 4937,
      status: "Success",
      operation: "publish",
      fileName: "home.html",
      fileLocation: "/sites/ff/home.html",
    },
    {
      contentid: 4938,
      status: "Failed",
      operation: "publish",
      fileName: "",
      fileLocation: " ",
    },
  ],
};

function attachConsole(page, consoleErrors) {
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const text = msg.text();
      if (!isKnownPublishConsoleNoise(text)) {
        consoleErrors.push(text);
      }
    }
  });
}

async function stubLogs(page) {
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
}

test.describe("PublishingShell copy log item location (#4937)", () => {
  test.beforeEach(async ({ page, context }) => {
    await context.grantPermissions(["clipboard-read", "clipboard-write"]);
    await loginAsAdmin(page);
  });

  test(`copies a location and names a missing one ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
    attachConsole(page, consoleErrors);
    await stubLogs(page);

    await page.goto(publishingLogsUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await page.getByTestId("logs-filter-apply").click();
    await expect(page.getByTestId("publish-log-row-4937")).toBeVisible({
      timeout: 15000,
    });
    await page.getByRole("button", { name: "details" }).click();
    await expect(page.getByTestId("publish-log-details")).toBeVisible();

    await page.getByTestId("publish-log-copy-location-0").click();
    await expect(page.getByTestId("publish-log-copy-status")).toContainText(
      "Location copied",
    );
    const copied = await page.evaluate(() => navigator.clipboard.readText());
    expect(copied).toBe("/sites/ff/home.html");

    await page.getByTestId("publish-log-copy-dismiss").click();
    await expect(page.getByTestId("publish-log-copy-status")).toHaveCount(0);
    await expect(page.getByTestId("publish-log-details")).toBeVisible();
    await expect(page.getByTestId("publish-log-row-4937")).toBeVisible();

    await page.getByTestId("publish-log-copy-location-1").click();
    await expect(page.getByTestId("publish-log-copy-status")).toContainText(
      "No location to copy",
    );
    await expect(page.getByTestId("publish-log-details")).toBeVisible();
    const afterMissing = await page.evaluate(() =>
      navigator.clipboard.readText(),
    );
    expect(afterMissing).toBe("/sites/ff/home.html");
    expect(consoleErrors).toEqual([]);
  });
});
