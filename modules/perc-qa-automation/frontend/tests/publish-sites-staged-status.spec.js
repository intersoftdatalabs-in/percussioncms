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
 * Playwright surface: #5838 / parent #4531 — staged-item status/log
 * readback on the PublishingShell site workspace (read-only: status summary
 * plus log lines; a failed fetch keeps the previous view with the mapped
 * error and never writes).
 *
 * Tags: @publish-sites-staged-status @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publish-sites-staged-status.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  publishingSitesUrl,
  isStagedStatusUrl,
  isWritePublishUrl,
  mockStagedStatusLines,
  isKnownPublishConsoleNoise,
} = require("./helpers/publish-sites-staged-status");

const TAGS = ["@publish-sites-staged-status", "@publish", "@smoke"];

async function openSiteWorkspace(page) {
  await page.goto(publishingSitesUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId(TEST_IDS.sitesSection)).toBeVisible({
    timeout: 30000,
  });
  const siteCard = page
    .locator(`[data-testid^="${TEST_IDS.siteCardPrefix}"]`)
    .first();
  await expect(siteCard).toBeVisible({ timeout: 30000 });
  await siteCard.click();
  await expect(page.getByTestId(TEST_IDS.workspace)).toBeVisible();
  const panel = page.getByTestId(TEST_IDS.panel);
  await expect(panel).toBeVisible({ timeout: 15000 });
  await panel.scrollIntoViewIfNeeded();
  return panel;
}

test.describe("PublishingShell staged-item status readback (#5838)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`reads back status plus log lines without writing ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const statusCalls = [];
    const writeCalls = [];
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      // Browser reports stubbed and unrelated CMS resource failures as console
      // errors. Uncaught page errors still fail the test.
      if (text.startsWith("Failed to load resource:")) {
        return;
      }
      if (!isKnownPublishConsoleNoise(text)) {
        consoleErrors.push(text);
      }
    });
    page.on("request", (req) => {
      const url = req.url();
      if (isStagedStatusUrl(url) && req.method() === "GET") {
        statusCalls.push(url);
      } else if (
        isWritePublishUrl(url) &&
        req.method() !== "GET" &&
        req.method() !== "OPTIONS"
      ) {
        writeCalls.push(`${req.method()} ${url}`);
      }
    });
    await page.route(
      "**/services/itemmanagement/item/pubhistory/**",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(mockStagedStatusLines()),
        });
      },
    );

    const panel = await openSiteWorkspace(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.lookup).click();

    await expect(panel.getByTestId(TEST_IDS.table)).toBeVisible({
      timeout: 15000,
    });
    // The workspace echoes the looked-up id back as a prop, so the panel may
    // legitimately issue the same read-only GET twice. Every call is /42.
    await expect.poll(() => statusCalls.length).toBeGreaterThanOrEqual(1);
    expect(statusCalls.length).toBeGreaterThanOrEqual(1);
    for (const url of statusCalls) {
      expect(url).toMatch(/\/pubhistory\/42(?:$|[?#])/);
    }
    await expect(panel.getByTestId(TEST_IDS.summary)).toContainText(
      /SUCCESS/,
    );
    await expect(
      panel.getByTestId(TEST_IDS.row).first(),
    ).toContainText("FTP-Prod");
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    expect(writeCalls, `unexpected writes: ${writeCalls.join(" | ")}`).toEqual(
      [],
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`failed fetch keeps the previous view with the mapped error ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      // Browser reports stubbed and unrelated CMS resource failures as console
      // errors. Uncaught page errors still fail the test.
      if (text.startsWith("Failed to load resource:")) {
        return;
      }
      if (!isKnownPublishConsoleNoise(text)) {
        consoleErrors.push(text);
      }
    });
    let failNext = false;
    await page.route(
      "**/services/itemmanagement/item/pubhistory/**",
      async (route) => {
        if (failNext) {
          await route.fulfill({
            status: 409,
            contentType: "application/json",
            body: JSON.stringify({ message: "Conflict" }),
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(mockStagedStatusLines()),
        });
      },
    );

    const panel = await openSiteWorkspace(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.lookup).click();
    await expect(panel.getByTestId(TEST_IDS.table)).toBeVisible({
      timeout: 15000,
    });

    failNext = true;
    await panel.getByTestId(TEST_IDS.input).fill("43");
    await panel.getByTestId(TEST_IDS.lookup).click();
    await expect(panel.getByTestId(TEST_IDS.error)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /conflict/i,
    );
    // Previous view is kept: the earlier rows stay on screen.
    await expect(panel.getByTestId(TEST_IDS.table)).toBeVisible();
    await expect(
      panel.getByTestId(TEST_IDS.row).first(),
    ).toContainText("FTP-Prod");
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
