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
 * PublishingShell Status lists a finished failure that is no longer an active id (#4807).
 *
 * Surface filter (H2 QA):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/statusFailedJobs.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  publishingProductUrl,
  isCurrentStatusUrl,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-cancel-job");

const JOBS = [
  {
    jobId: 4807,
    siteName: "FastForward",
    siteId: 10,
    editionName: "Nightly Full",
    status: "Completed with failures",
    errorMessage: "template missing",
    completedItems: 3,
    failedItems: 1,
    totalItems: 4,
  },
  {
    jobId: 12,
    siteId: 12,
    siteName: "Enterprise",
    status: "Running",
    completedItems: 0,
    totalItems: 2,
  },
];

test.describe("PublishingShell Status lists finished failures (#4807)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("shows completed-with-failures and running, not a stop on the failure", async ({
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

    await page.goto(publishingProductUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-status-job-4807")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByText("Completed with failures")).toBeVisible();
    await expect(page.getByTestId("publish-stop-job-4807")).toHaveCount(0);
    await expect(page.getByTestId("publish-status-job-12")).toBeVisible();
    await expect(page.getByTestId("publish-stop-job-12")).toBeVisible();

    await page.getByTestId("publish-status-job-4807").click();
    await expect(page.getByTestId("publish-status-detail-status")).toHaveText(
      "Completed with failures",
    );
    await expect(page.getByTestId("publish-status-detail-error")).toHaveText(
      "template missing",
    );
    await expect(page.getByTestId("publish-status-detail-edition")).toHaveText(
      "Nightly Full",
    );
    expect(consoleErrors).toEqual([]);
  });
});
