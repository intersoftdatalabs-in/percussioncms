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
 * Playwright surface: #4809 / parent #4531 — PublishingShell Logs day window
 * is posted to the server (not applied only in the browser).
 *
 * Tags: @publishing-logs-day-window @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing/logsDayWindow.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  publishingLogsUrl,
  isPubstatusLogsUrl,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-logs-filter");

const TAGS = ["@publishing-logs-day-window", "@publish"];

test.describe("PublishingShell logs day window (#4809)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`posts the selected day window on the logs request ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
    const bodies = [];
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
        bodies.push(route.request().postDataJSON());
        const days = bodies[bodies.length - 1].days;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([
            {
              jobId: 4809,
              siteName: "DayWindow",
              serverName: "prod",
              status: "Completed",
              startDate: "window-" + days,
            },
          ]),
        });
        return;
      }
      await route.continue();
    });

    await page.goto(publishingLogsUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("logs-filter-days")).toBeVisible();

    await page.getByTestId("logs-filter-days").selectOption("3");
    await page.getByTestId("logs-filter-failures").check();
    await page.getByTestId("logs-filter-apply").click();
    await expect(page.getByTestId("publish-log-row-4809")).toBeVisible({
      timeout: 15000,
    });

    expect(bodies.length).toBeGreaterThan(0);
    const posted = bodies[bodies.length - 1];
    expect(posted.days).toBe(3);
    expect(posted.showOnlyFailures).toBe(true);

    expect(consoleErrors).toEqual([]);
  });
});
