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
 * PublishingShell Status job detail (#4789 / parent #4531).
 *
 * Surface filter (H2 QA):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/statusJobDetail.spec.js
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
    jobId: 4789,
    siteName: "FastForward",
    siteId: 10,
    editionName: "Nightly Full",
    status: "Failed",
    errorMessage: "disk full",
    completedItems: 1,
    totalItems: 4,
  },
  {
    jobId: 12,
    siteId: 12,
    status: "Running",
    completedItems: 0,
    totalItems: 2,
  },
];

test.describe("PublishingShell Status job detail (#4789)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("opens job detail and close does not stop", async ({ page }) => {
    const consoleErrors = [];
    const stopCalls = [];
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
    await page.route("**/stopPublishing/**", async (route) => {
      stopCalls.push(route.request().url());
      await route.fulfill({ status: 204, body: "" });
    });

    await page.goto(publishingProductUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-status-job-4789")).toBeVisible({
      timeout: 15000,
    });

    await page.getByTestId("publish-status-job-4789").click();
    await expect(page.getByTestId("publish-status-detail-job-id")).toHaveText(
      "4789",
    );
    await expect(page.getByTestId("publish-status-detail-site")).toHaveText(
      "FastForward",
    );
    await expect(page.getByTestId("publish-status-detail-edition")).toHaveText(
      "Nightly Full",
    );
    await expect(page.getByTestId("publish-status-detail-status")).toHaveText(
      "Failed",
    );
    await expect(page.getByTestId("publish-status-detail-error")).toHaveText(
      "disk full",
    );
    await expect(page.getByTestId("publish-status-detail-heading")).toHaveText(
      "Job details",
    );
    await expect(
      page.getByTestId("publish-status-detail-job-id-label"),
    ).toHaveText("Job ID");
    await expect(
      page.getByTestId("publish-status-detail-edition-label"),
    ).toHaveText("Edition");
    await expect(
      page.getByTestId("publish-status-detail-error-label"),
    ).toHaveText("Error");

    await page.getByTestId("publish-status-job-detail-close").click();
    await expect(page.getByTestId("publish-status-job-detail")).toHaveCount(0);
    expect(stopCalls).toEqual([]);

    await page.getByTestId("publish-status-job-12").click();
    await expect(page.getByTestId("publish-status-detail-site")).toHaveText("");
    await expect(page.getByTestId("publish-status-detail-edition")).toHaveText(
      "",
    );
    await expect(page.getByTestId("publish-status-detail-error")).toHaveCount(0);
    await expect(page.getByTestId("publish-stop-job-12")).toBeVisible();
    expect(consoleErrors).toEqual([]);
  });
});
