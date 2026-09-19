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
 * Playwright surface: #4614 / parent #4531 — PublishingShell incremental
 * site publish from the site workspace (confirm + job/status).
 *
 * Tags: @publishing-incremental-site @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-incremental-site.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  publishingProductUrl,
  isIncrementalPublishUrl,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-incremental-site");

const TAGS = ["@publishing-incremental-site", "@publish", "@smoke"];

test.describe("PublishingShell incremental site publish (#4614)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`incremental confirm then job status ${TAGS.join(" ")}`, async ({
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

    await page.route("**/sitemanage/publish/incremental/publish/**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          SitePublishResponse: {
            status: "Queuing content",
            delivered: "0",
            failures: "0",
            jobid: 4614,
          },
        }),
      });
    });

    await page.goto(publishingProductUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 30000,
    });

    const siteCard = page.locator('[data-testid^="publish-site-card-"]').first();
    await expect(siteCard).toBeVisible({ timeout: 30000 });
    await siteCard.click();
    await expect(page.getByTestId("publish-site-workspace")).toBeVisible();

    const incremental = page.getByTestId("publish-incremental-confirm");
    const enabled = await incremental.isEnabled();
    if (!enabled) {
      test.info().annotations.push({
        type: "note",
        description: "No publish server selected; incremental stays disabled",
      });
      expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
      return;
    }

    page.once("dialog", (dialog) => dialog.accept());
    await incremental.click();

    await expect(page.getByText(/Publish Job Started/i)).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-site-jobs")).toBeVisible();
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`incremental confirm cancel does not publish ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    let incrementalHits = 0;
    await page.route("**/sitemanage/publish/incremental/publish/**", async (route) => {
      if (isIncrementalPublishUrl(route.request().url())) {
        incrementalHits += 1;
      }
      await route.continue();
    });

    await page.goto(publishingProductUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    const siteCard = page.locator('[data-testid^="publish-site-card-"]').first();
    await expect(siteCard).toBeVisible({ timeout: 30000 });
    await siteCard.click();
    const incremental = page.getByTestId("publish-incremental-confirm");
    if (!(await incremental.isEnabled())) {
      return;
    }
    page.once("dialog", (dialog) => dialog.dismiss());
    await incremental.click();
    await expect(page.getByText(/Publish Job Started/i)).toHaveCount(0);
    expect(incrementalHits).toBe(0);
  });
});
