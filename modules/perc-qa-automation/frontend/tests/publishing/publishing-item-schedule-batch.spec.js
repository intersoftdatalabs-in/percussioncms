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
 * Playwright surface: #6461 / parent #4531 — schedule one date set for
 * several item ids on the PublishingShell site workspace and report each
 * row (a per-item 403 is a named failure, not full success; cancel and
 * blank input never write). Load-path parity (#6695): a partial multi-id
 * load names the failed id and Retry failed reloads only it. Save/clear
 * retry parity (#6697, mirrors the takedown-batch retry spec from #6677):
 * a partial save or clear shows Retry failed, retry re-runs only the
 * failed ids, and full success clears the button.
 *
 * Tags: @publishing-item-schedule-batch @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing/publishing-item-schedule-batch.spec.js
 * from modules/perc-qa-automation/frontend
 */

"use strict";

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  TEST_IDS,
  publishingSitesUrl,
  isGetItemDatesUrl,
  isSetItemDatesUrl,
  mockItemDates,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-item-schedule");

const TAGS = ["@publishing-item-schedule-batch", "@publish", "@smoke"];

const FIRST_ID = "9001";
const SECOND_ID = "9002";
const STORED_START = "09/18/2026 09:00 am";
const STORED_END = "09/19/2026 10:00 am";

function itemIdFromDatesUrl(url) {
  const match = /\/getitemdates\/([^/?#]+)/.exec(String(url || ""));
  return match ? decodeURIComponent(match[1]) : "";
}

async function stubBatchApis(page, opts = {}) {
  const state = {
    setCalls: [],
    getCalls: [],
    failIds: opts.failIds || {},
    clearedIds: new Set(),
  };
  const failGetOnce = new Set(opts.failGetOnceIds || []);
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
  await page.route("**/findLinkedItems/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
    const id = itemIdFromDatesUrl(route.request().url());
    state.getCalls.push(id);
    if (failGetOnce.has(id)) {
      failGetOnce.delete(id);
      await route.fulfill({
        status: 404,
        contentType: "application/json",
        body: JSON.stringify({ message: "Item not found." }),
      });
      return;
    }
    // A cleared id reads back empty dates so the panel's clear
    // verification reload (save then re-fetch) sees the clear.
    const cleared = state.clearedIds.has(id);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(
        cleared
          ? mockItemDates(id, "", "")
          : mockItemDates(id, STORED_START, STORED_END),
      ),
    });
  });
  await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
    let posted = null;
    try {
      posted = route.request().postDataJSON();
    } catch {
      posted = null;
    }
    state.setCalls.push(posted);
    const id = posted && posted.ItemDates ? String(posted.ItemDates.itemId) : "";
    const dates = (posted && posted.ItemDates) || {};
    if (!dates.startDate && !dates.endDate && id) {
      state.clearedIds.add(id);
    }
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
      body: JSON.stringify({ operation: "Success" }),
    });
  });
  return state;
}

async function openSiteWorkspaceSchedule(page) {
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

test.describe("PublishingShell multi-item schedule dates (#6461, save/clear retry #6697)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`saves one date set to two ids with a per-item report ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    const state = await stubBatchApis(page);
    const setUrls = [];
    page.on("request", (req) => {
      if (isSetItemDatesUrl(req.url()) && req.method() === "POST") {
        setUrls.push(req.url());
      }
    });

    const panel = await openSiteWorkspaceSchedule(page);
    await panel.getByTestId(TEST_IDS.input).fill(`${FIRST_ID}, ${SECOND_ID}`);
    await panel.getByTestId(TEST_IDS.lookup).click();
    await expect(panel.getByTestId(TEST_IDS.start)).toHaveValue(
      "2026-09-18T09:00",
      { timeout: 15000 },
    );

    await panel.getByTestId(TEST_IDS.save).click();
    await expect(panel.getByTestId(TEST_IDS.success)).toContainText(
      /all 2 items/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    const report = panel.getByTestId(TEST_IDS.batchReport);
    await expect(report).toBeVisible();
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${FIRST_ID}`),
    ).toContainText(/saved/i);
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${SECOND_ID}`),
    ).toContainText(/saved/i);
    const postedIds = state.setCalls.map((call) =>
      String(call.ItemDates.itemId),
    );
    expect(postedIds).toContain(FIRST_ID);
    expect(postedIds).toContain(SECOND_ID);
    expect(setUrls.length).toBeGreaterThanOrEqual(2);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`a per-item 403 is named and keeps the saved row ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    await stubBatchApis(page, {
      failIds: { [SECOND_ID]: "User demo is editing this page." },
    });

    const panel = await openSiteWorkspaceSchedule(page);
    await panel.getByTestId(TEST_IDS.input).fill(`${FIRST_ID} ${SECOND_ID}`);
    await panel.getByTestId(TEST_IDS.start).fill("2026-09-18T09:00");
    await panel.getByTestId(TEST_IDS.save).click();

    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /Saved 1 of 2/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      new RegExp(`${SECOND_ID} \\(User demo is editing this page\\.\\)`),
    );
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${FIRST_ID}`),
    ).toContainText(/saved/i);
    await expect(panel.getByTestId(TEST_IDS.success)).toHaveCount(0);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`a partial load names the failure and Retry failed reloads only it ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    const state = await stubBatchApis(page, {
      failGetOnceIds: [SECOND_ID],
    });

    const panel = await openSiteWorkspaceSchedule(page);
    await panel.getByTestId(TEST_IDS.input).fill(`${FIRST_ID}, ${SECOND_ID}`);
    await panel.getByTestId(TEST_IDS.lookup).click();

    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /Loaded 1 of 2/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      new RegExp(`${SECOND_ID} \\(Item not found\\.\\)`),
    );
    await expect(panel.getByTestId(TEST_IDS.retry)).toBeVisible();
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${FIRST_ID}`),
    ).toContainText(/saved/i);

    await panel.getByTestId(TEST_IDS.retry).click();

    await expect(panel.getByTestId(TEST_IDS.success)).toContainText(
      /loaded for all 1 items/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    await expect(panel.getByTestId(TEST_IDS.retry)).toHaveCount(0);
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${FIRST_ID}`),
    ).toContainText(/saved/i);
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${SECOND_ID}`),
    ).toContainText(/saved/i);
    expect(
      state.getCalls.filter((id) => id === FIRST_ID).length,
    ).toBe(1);
    expect(
      state.getCalls.filter((id) => id === SECOND_ID).length,
    ).toBe(2);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`a partial save offers Retry failed that re-saves only the failed id ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    const state = await stubBatchApis(page, {
      failIds: { [SECOND_ID]: "User demo is editing this page." },
    });

    const panel = await openSiteWorkspaceSchedule(page);
    await panel.getByTestId(TEST_IDS.input).fill(`${FIRST_ID} ${SECOND_ID}`);
    await panel.getByTestId(TEST_IDS.start).fill("2026-09-18T09:00");
    await panel.getByTestId(TEST_IDS.save).click();

    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /Saved 1 of 2/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      new RegExp(`${SECOND_ID} \\(User demo is editing this page\\.\\)`),
    );
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${FIRST_ID}`),
    ).toContainText(/saved/i);
    await expect(panel.getByTestId(TEST_IDS.success)).toHaveCount(0);
    await expect(panel.getByTestId(TEST_IDS.retry)).toBeVisible();

    // Heal the failure; the retry re-saves only the failed id and flips
    // its row, mirroring the takedown-batch retry spec (#6677). The
    // original stubs stay in place so the post-save verification reads
    // through the same routes.
    delete state.failIds[SECOND_ID];
    const before = state.setCalls.length;
    await panel.getByTestId(TEST_IDS.retry).click();
    await expect(panel.getByTestId(TEST_IDS.success)).toContainText(
      /all 1 items/i,
      { timeout: 15000 },
    );
    expect(state.setCalls.length).toBe(before + 1);
    expect(
      state.setCalls
        .slice(before)
        .map((call) => String(call.ItemDates.itemId)),
    ).toEqual([SECOND_ID]);
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${SECOND_ID}`),
    ).toContainText(/saved/i);
    await expect(panel.getByTestId(TEST_IDS.retry)).toHaveCount(0);
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${SECOND_ID}`),
    ).toContainText(/saved/i);
    await expect(panel.getByTestId(TEST_IDS.retry)).toHaveCount(0);
    await page.screenshot({
      path: "test-results/live-proof-6697-schedule-save-retry.png",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`a partial clear offers Retry failed that re-clears only the failed id ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    const state = await stubBatchApis(page, {
      failIds: { [SECOND_ID]: "User other is editing this page." },
    });

    const panel = await openSiteWorkspaceSchedule(page);
    await panel.getByTestId(TEST_IDS.input).fill(`${FIRST_ID}, ${SECOND_ID}`);
    await panel.getByTestId(TEST_IDS.clearSubmit).click();
    await expect(panel.getByTestId(TEST_IDS.clearConfirm)).toBeVisible();
    await panel.getByTestId(TEST_IDS.clearYes).click();

    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /Cleared 1 of 2/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      new RegExp(`${SECOND_ID} \\(User other is editing this page\\.\\)`),
    );
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${FIRST_ID}`),
    ).toContainText(/saved/i);
    await expect(panel.getByTestId(TEST_IDS.success)).toHaveCount(0);
    await expect(panel.getByTestId(TEST_IDS.retry)).toBeVisible();

    // Heal the failure; the retry re-clears only the failed id and the
    // button clears once nothing is left to retry. The original stubs stay
    // in place so the post-clear verification reload reads the cleared
    // (empty) dates through the same routes.
    delete state.failIds[SECOND_ID];
    const before = state.setCalls.length;
    await panel.getByTestId(TEST_IDS.retry).click();
    await expect(panel.getByTestId(TEST_IDS.success)).toContainText(
      /cleared for all 1 items/i,
      { timeout: 15000 },
    );
    expect(state.setCalls.length).toBe(before + 1);
    expect(
      state.setCalls
        .slice(before)
        .map((call) => String(call.ItemDates.itemId)),
    ).toEqual([SECOND_ID]);
    await expect(
      panel.getByTestId(`${TEST_IDS.batchItemPrefix}${SECOND_ID}`),
    ).toContainText(/saved/i);
    await expect(panel.getByTestId(TEST_IDS.retry)).toHaveCount(0);
    await expect(panel.getByTestId(TEST_IDS.clearConfirm)).toHaveCount(0);
    await page.screenshot({
      path: "test-results/live-proof-6697-schedule-clear-retry.png",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
