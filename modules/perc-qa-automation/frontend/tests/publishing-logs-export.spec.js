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
 * Playwright surface: #4767 / parent #4531 — PublishingShell Logs
 * export of the currently filtered rows.
 *
 * Tags: @publishing-logs-export @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-logs-export.spec.js
 * from modules/perc-qa-automation/frontend
 */

const fs = require("fs");
const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  publishingLogsUrl,
  isPubstatusLogsUrl,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-logs-filter");

const TAGS = ["@publishing-logs-export", "@publish", "@smoke"];

const LOGS = [
  {
    jobId: 4767,
    siteName: "FastForward",
    serverName: "prod",
    status: "Completed",
  },
  {
    jobId: 4768,
    siteName: "Marketing",
    serverName: "stage",
    status: "Failed",
  },
];

test.describe("PublishingShell export filtered logs (#4767)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`downloads the filtered log rows as CSV ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
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

    await page.goto(publishingLogsUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publish-section-logs")).toBeVisible({
      timeout: 30000,
    });
    await page.getByTestId("logs-filter-apply").click();
    await expect(page.getByTestId("publish-log-row-4767")).toBeVisible({
      timeout: 15000,
    });

    await page.getByTestId("logs-filter-query").fill("market");
    await expect(page.getByTestId("publish-log-row-4767")).toHaveCount(0);

    const filteredDownload = page.waitForEvent("download");
    await page.getByTestId("logs-export-filtered").click();
    const filtered = await filteredDownload;
    const filteredPath = await filtered.path();
    expect(filteredPath).toBeTruthy();
    const body = fs.readFileSync(filteredPath, "utf8").replace(/\r\n/g, "\n");
    expect(body).toBe("Job,Site,Server,Status\n4768,Marketing,stage,Failed\n");
    await expect(page.getByTestId("publish-logs-export-error")).toHaveCount(0);

    await page.getByTestId("logs-filter-query").fill("no-such-site");
    await expect(page.getByTestId("publish-logs-empty")).toBeVisible();
    const emptyDownload = page.waitForEvent("download");
    await page.getByTestId("logs-export-filtered").click();
    const empty = await emptyDownload;
    const emptyPath = await empty.path();
    expect(emptyPath).toBeTruthy();
    const emptyBody = fs.readFileSync(emptyPath, "utf8").replace(/\r\n/g, "\n");
    expect(emptyBody).toBe("Job,Site,Server,Status\n");

    expect(consoleErrors).toEqual([]);
  });
});
