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
 * Playwright surface: #5887 / parent #4531 — staged-item log severity
 * filter on the PublishingShell site workspace (client-side only:
 * narrowing, clearing, and the empty-filter state issue no new REST and
 * never write).
 *
 * Tags: @publish-sites-staged-status-severity @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publish-sites-staged-status-severity.spec.js
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

const TAGS = ["@publish-sites-staged-status-severity", "@publish", "@smoke"];

function failureOnlyLines() {
  return {
    ItemPublishingHistory: [
      {
        server: "FTP-Prod",
        location: "/home",
        revisionId: 2,
        publishedDate: 100,
        operation: "publish",
        status: "FAILURE",
        errorMessage: "denied",
      },
    ],
  };
}

async function openStagedStatusPanel(page, lines) {
  await page.route(
    "**/services/itemmanagement/item/pubhistory/**",
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(lines),
      });
    },
  );
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
  await panel.getByTestId(TEST_IDS.input).fill("42");
  await panel.getByTestId(TEST_IDS.lookup).click();
  await expect(panel.getByTestId(TEST_IDS.table)).toBeVisible({
    timeout: 15000,
  });
  return panel;
}

function trackTraffic(page, statusCalls, writeCalls, consoleErrors) {
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
}

test.describe("PublishingShell staged-item severity filter (#5887)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`severity filter narrows and clearing restores without new REST ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const statusCalls = [];
    const writeCalls = [];
    const consoleErrors = [];
    trackTraffic(page, statusCalls, writeCalls, consoleErrors);

    const panel = await openStagedStatusPanel(page, mockStagedStatusLines());
    await expect(panel.getByTestId(TEST_IDS.row)).toHaveCount(2);
    const callsAfterLookup = statusCalls.length;
    expect(callsAfterLookup).toBeGreaterThanOrEqual(1);

    await panel.getByTestId(TEST_IDS.severity).selectOption("failure");
    await expect(panel.getByTestId(TEST_IDS.row)).toHaveCount(1);
    await expect(panel.getByTestId(TEST_IDS.row).first()).toContainText(
      "FAILURE",
    );

    await panel.getByTestId(TEST_IDS.severity).selectOption("all");
    await expect(panel.getByTestId(TEST_IDS.row)).toHaveCount(2);
    await expect(panel.getByTestId(TEST_IDS.filterEmpty)).toHaveCount(0);

    // Client-side only: filtering issues zero new REST and never writes.
    expect(statusCalls.length).toBe(callsAfterLookup);
    expect(writeCalls, `unexpected writes: ${writeCalls.join(" | ")}`).toEqual(
      [],
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`empty-filter state shows when nothing matches ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const statusCalls = [];
    const writeCalls = [];
    const consoleErrors = [];
    trackTraffic(page, statusCalls, writeCalls, consoleErrors);

    const panel = await openStagedStatusPanel(page, failureOnlyLines());
    await expect(panel.getByTestId(TEST_IDS.row)).toHaveCount(1);
    const callsAfterLookup = statusCalls.length;

    await panel.getByTestId(TEST_IDS.severity).selectOption("success");
    await expect(panel.getByTestId(TEST_IDS.filterEmpty)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.filterEmpty)).toContainText(
      /no log lines match/i,
    );
    await expect(panel.getByTestId(TEST_IDS.table)).toHaveCount(0);

    expect(statusCalls.length).toBe(callsAfterLookup);
    expect(writeCalls, `unexpected writes: ${writeCalls.join(" | ")}`).toEqual(
      [],
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
