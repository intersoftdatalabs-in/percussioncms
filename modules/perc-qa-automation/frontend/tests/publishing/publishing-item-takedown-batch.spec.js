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
 * Playwright surface: #6462 / parent #4531 — take down (unpublish) several
 * item ids from the PublishingShell site workspace with one confirm and a
 * per-item report (a per-item 403 is a named failure, not full success;
 * review alone writes nothing).
 *
 * Tags: @publishing-item-takedown-batch @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing/publishing-item-takedown-batch.spec.js
 * from modules/perc-qa-automation/frontend
 */

"use strict";

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  TEST_IDS,
  publishingSitesUrl,
  isFindLinkedItemsUrl,
  isTakedownUrl,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-item-takedown");

const TAGS = ["@publishing-item-takedown-batch", "@publish", "@smoke"];

const FIRST_ID = "9001";
const SECOND_ID = "9002";

function takedownIdFromUrl(url) {
  const match = /\/takedown\/(?:page|resource)(?:\/staging)?\/([^/?#]+)/.exec(
    String(url || ""),
  );
  return match ? decodeURIComponent(match[1]) : "";
}

async function stubBatchApis(page, opts = {}) {
  const state = {
    takedownCalls: [],
    failIds: opts.failIds || {},
  };
  // The workspace shares one item id across every item panel: looking up ids
  // in the takedown panel also nudges the history, actions-menu, and
  // schedule panels to read. Stub those sibling reads so the fake lookup ids
  // never reach the real backend (zero server.log noise); the linked-page
  // and takedown routes under test keep their own stubs per test.
  await page.route("**/services/itemmanagement/item/pubhistory/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ItemPublishingHistory: [] }),
    });
  });
  await page.route("**/publishingActions/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemDates: { itemId: "42", startDate: "", endDate: "", comments: "" },
      }),
    });
  });
  await page.route("**/itemmanagement/item/findLinkedItems/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ArrayList: [{ pagePath: "/Sites/Demo/Home", id: "7" }],
      }),
    });
  });
  await page.route("**/sitemanage/publish/takedown/**", async (route) => {
    const id = takedownIdFromUrl(route.request().url());
    state.takedownCalls.push({
      method: route.request().method(),
      url: route.request().url(),
      id,
    });
    if (state.failIds[id]) {
      await route.fulfill({
        status: 403,
        contentType: "application/json",
        body: JSON.stringify({ message: state.failIds[id] }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ status: "Success" }),
    });
  });
  return state;
}

async function openSiteWorkspaceTakedown(page) {
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

function trackConsoleErrors(page) {
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (text.startsWith("Failed to load resource:")) {
      return;
    }
    if (!isKnownPublishConsoleNoise(text)) {
      consoleErrors.push(text);
    }
  });
  return consoleErrors;
}

test.describe("PublishingShell multi-item takedown (#6462)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(`takes down two ids with one confirm and a per-item report ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    const state = await stubBatchApis(page);
    const linkedReads = [];
    page.on("request", (req) => {
      if (isFindLinkedItemsUrl(req.url()) && req.method() === "GET") {
        linkedReads.push(req.url());
      }
    });

    const panel = await openSiteWorkspaceTakedown(page);
    await panel.getByTestId(TEST_IDS.input).fill(`${FIRST_ID}, ${SECOND_ID}`);
    await panel.getByTestId(TEST_IDS.review).click();

    // One confirm lists both ids; review alone writes nothing.
    await expect(panel.getByTestId(TEST_IDS.confirm)).toBeVisible({
      timeout: 15000,
    });
    await expect(
      panel.getByTestId(`${TEST_IDS.batchGroupPrefix}${FIRST_ID}`),
    ).toContainText("/Sites/Demo/Home");
    await expect(
      panel.getByTestId(`${TEST_IDS.batchGroupPrefix}${SECOND_ID}`),
    ).toBeVisible();
    expect(linkedReads.length).toBeGreaterThanOrEqual(2);
    expect(state.takedownCalls).toEqual([]);

    await panel.getByTestId(TEST_IDS.submit).click();
    await expect(panel.getByTestId(TEST_IDS.success)).toContainText(
      /all 2 items/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    const report = panel.getByTestId(TEST_IDS.batchReport);
    await expect(report).toBeVisible();
    await expect(
      panel.getByTestId(`${TEST_IDS.batchReportItemPrefix}${FIRST_ID}`),
    ).toContainText(/taken down/i);
    await expect(
      panel.getByTestId(`${TEST_IDS.batchReportItemPrefix}${SECOND_ID}`),
    ).toContainText(/taken down/i);
    const takenDownIds = state.takedownCalls.map((call) => call.id);
    expect(takenDownIds).toContain(FIRST_ID);
    expect(takenDownIds).toContain(SECOND_ID);
    for (const call of state.takedownCalls) {
      expect(isTakedownUrl(call.url)).toBe(true);
    }
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`a per-item 403 is named and keeps the taken-down row ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    await stubBatchApis(page, {
      failIds: { [SECOND_ID]: "User demo is editing this page." },
    });

    const panel = await openSiteWorkspaceTakedown(page);
    await panel.getByTestId(TEST_IDS.input).fill(`${FIRST_ID} ${SECOND_ID}`);
    await panel.getByTestId(TEST_IDS.review).click();
    await expect(panel.getByTestId(TEST_IDS.confirm)).toBeVisible({
      timeout: 15000,
    });
    await panel.getByTestId(TEST_IDS.submit).click();

    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /Taken down 1 of 2/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      new RegExp(`${SECOND_ID} \\(User demo is editing this page\\.\\)`),
    );
    await expect(
      panel.getByTestId(`${TEST_IDS.batchReportItemPrefix}${FIRST_ID}`),
    ).toContainText(/taken down/i);
    await expect(panel.getByTestId(TEST_IDS.success)).toHaveCount(0);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
