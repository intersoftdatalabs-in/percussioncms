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
 * PublishingShell Status edition filter (#4914 / parent #4531).
 *
 * Surface filter (H2 QA):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/statusEditionFilter.spec.js
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
    jobId: 4914,
    siteName: "FastForward",
    siteId: 10,
    editionName: "Nightly Full",
    status: "Running",
    completedItems: 1,
    totalItems: 8,
  },
  {
    jobId: 22,
    siteName: "DoneSite",
    siteId: 22,
    editionName: "Hourly Incremental",
    status: "Completed",
    completedItems: 4,
    totalItems: 4,
  },
];

test.describe("PublishingShell Status edition filter (#4914)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("filters current jobs by edition name", async ({ page }) => {
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

    let statusHits = 0;
    await page.route("**/sitemanage/pubstatus/current**", async (route) => {
      if (
        isCurrentStatusUrl(route.request().url()) &&
        route.request().method() === "GET"
      ) {
        statusHits += 1;
        if (statusHits > 1) {
          await route.fulfill({
            status: 403,
            contentType: "application/json",
            body: JSON.stringify({ message: "Forbidden" }),
          });
          return;
        }
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
    await expect(page.getByTestId("publish-stop-job-4914")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByText("DoneSite")).toBeVisible();

    const filter = page.getByTestId("publish-status-edition-filter");
    await filter.fill("hourly");
    await expect(page.getByText("DoneSite")).toBeVisible();
    await expect(page.getByTestId("publish-stop-job-4914")).toHaveCount(0);

    await filter.fill("missing-edition");
    await expect(page.getByTestId("publish-status-empty-filter")).toBeVisible();
    await expect(page.getByTestId("publish-status-empty-filter")).toContainText(
      /edition filter/i,
    );

    await filter.fill("");
    await expect(page.getByText("DoneSite")).toBeVisible();
    await expect(page.getByTestId("publish-stop-job-4914")).toBeVisible();

    await expect(page.getByTestId("publish-section-status")).toBeVisible({
      timeout: 12000,
    });
    await expect(page.getByRole("alert")).toBeVisible({ timeout: 12000 });
    await expect(page.getByTestId("publish-section-status")).toBeVisible();
    expect(consoleErrors).toEqual([]);
  });
});
