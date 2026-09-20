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
 * Playwright surface: #4615 / parent #4531 — PublishingShell cancel/stop
 * of an in-flight publish job from Status (confirm; forbidden when not running).
 *
 * Tags: @publishing-cancel-job @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-cancel-job.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  publishingProductUrl,
  isStopPublishingUrl,
  isCurrentStatusUrl,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-cancel-job");

const TAGS = ["@publishing-cancel-job", "@publish", "@smoke"];

const JOBS = [
  {
    jobId: 4615,
    siteName: "FastForward",
    status: "Running",
    completedItems: 1,
    totalItems: 10,
  },
  {
    jobId: 99,
    siteName: "DoneSite",
    status: "Completed",
    completedItems: 5,
    totalItems: 5,
  },
];

test.describe("PublishingShell cancel/stop publish job (#4615)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`stop only on running job after confirm ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
    const stopPosts = [];
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

    await page.route("**/publishmanagement/servers/stopPublishing/**", async (route) => {
      if (isStopPublishingUrl(route.request().url())) {
        stopPosts.push({
          method: route.request().method(),
          url: route.request().url(),
        });
        await route.fulfill({
          status: 204,
          contentType: "application/json",
          body: "",
        });
        return;
      }
      await route.continue();
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

    await expect(page.getByTestId("publish-section-status")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-stop-job-4615")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-stop-job-99")).toHaveCount(0);

    page.once("dialog", (dialog) => dialog.dismiss());
    await page.getByTestId("publish-stop-job-4615").click();
    expect(stopPosts.length).toBe(0);

    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("publish-stop-job-4615").click();
    await expect.poll(() => stopPosts.length).toBe(1);
    expect(stopPosts[0].method).toMatch(/POST/i);
    expect(consoleErrors).toEqual([]);
  });
});
