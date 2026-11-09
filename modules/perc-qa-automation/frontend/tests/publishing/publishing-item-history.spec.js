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
 * Playwright surface: #5928 / parent #4531 — read-only item publishing
 * history on the PublishingShell site workspace (editions/runs for the
 * selected item via the existing history route; a failed fetch is a mapped
 * error and never writes).
 *
 * Tags: @publishing-item-history @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing/publishing-item-history.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  TEST_IDS,
  publishingSitesUrl,
  isItemHistoryUrl,
  isWritePublishUrl,
  mockItemHistoryRows,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-item-history");

const TAGS = ["@publishing-item-history", "@publish", "@smoke"];

async function stubWorkspaceReads(page, historyBody, historyStatus = 200) {
  // The workspace shares one item id across every item panel: looking up an
  // id in the history panel also nudges the schedule-dates, actions-menu,
  // and takedown panels to read. Stub those sibling reads so the fake lookup
  // id never reaches the real backend (zero server.log noise); the history
  // route under test keeps its own stub per test.
  await page.route("**/services/itemmanagement/item/pubhistory/**", async (route) => {
    const body =
      typeof historyBody === "function" ? historyBody(route) : historyBody;
    await route.fulfill({
      status: historyStatus,
      contentType: "application/json",
      body: typeof body === "string" ? body : JSON.stringify(body),
    });
  });
  await page.route("**/publishingActions/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/getitemdates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({}),
    });
  });
  await page.route("**/findLinkedItems/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
}
async function openSiteWorkspaceHistory(page) {
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

function trackPage(page) {
  const historyCalls = [];
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
    if (isItemHistoryUrl(url) && req.method() === "GET") {
      historyCalls.push(url);
    } else if (
      isWritePublishUrl(url) &&
      req.method() !== "GET" &&
      req.method() !== "OPTIONS"
    ) {
      writeCalls.push(`${req.method()} ${url}`);
    }
  });
  return { historyCalls, writeCalls, consoleErrors };
}

test.describe("PublishingShell site-workspace item history (#5928)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`reads back editions and runs without writing ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const { historyCalls, writeCalls, consoleErrors } = trackPage(page);
    await stubWorkspaceReads(page, mockItemHistoryRows());

    const panel = await openSiteWorkspaceHistory(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.lookup).click();

    await expect(panel.getByTestId(TEST_IDS.table)).toBeVisible({
      timeout: 15000,
    });
    // The workspace echoes the looked-up id back as a prop, so the panel may
    // legitimately issue the same read-only GET twice. Every call is /42.
    await expect.poll(() => historyCalls.length).toBeGreaterThanOrEqual(1);
    for (const url of historyCalls) {
      expect(url).toMatch(/\/pubhistory\/42(?:$|[?#])/);
    }
    await expect(panel.getByTestId(TEST_IDS.row).first()).toContainText(
      "FTP-Prod",
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    expect(writeCalls, `unexpected writes: ${writeCalls.join(" | ")}`).toEqual(
      [],
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`failed fetch is a mapped error, not empty history ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const { writeCalls, consoleErrors } = trackPage(page);
    await stubWorkspaceReads(
      page,
      {
        message: "Not authorized to view publishing history for this item.",
      },
      403,
    );

    const panel = await openSiteWorkspaceHistory(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.lookup).click();

    await expect(panel.getByTestId(TEST_IDS.error)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /Not authorized|Publish Forbidden|HTTP 403/i,
    );
    await expect(panel.getByTestId(TEST_IDS.empty)).toHaveCount(0);
    await expect(panel.getByTestId(TEST_IDS.table)).toHaveCount(0);
    expect(writeCalls, `unexpected writes: ${writeCalls.join(" | ")}`).toEqual(
      [],
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
