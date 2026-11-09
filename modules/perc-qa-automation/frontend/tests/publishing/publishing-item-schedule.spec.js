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
 * Playwright surface: #5929 / parent #4531 — set/clear item schedule
 * publish dates on the PublishingShell site workspace selected item via the
 * existing setitemdates route (a failed save or clear is a mapped error and
 * cancel never writes).
 *
 * Tags: @publishing-item-schedule @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing/publishing-item-schedule.spec.js
 * from modules/perc-qa-automation/frontend
 */

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

const TAGS = ["@publishing-item-schedule", "@publish", "@smoke"];

const STORED_START = "09/18/2026 09:00 am";
const STORED_END = "09/19/2026 10:00 am";

async function stubWorkspaceApis(page, opts = {}) {
  const state = {
    cleared: false,
    setStatus: opts.setStatus ?? 200,
    setBody: opts.setBody ?? { operation: "Success" },
    setCalls: [],
  };
  // The workspace shares one item id across every item panel: looking up an
  // id in the schedule panel also nudges the history, actions-menu, and
  // takedown panels to read. Stub those sibling reads so the fake lookup id
  // never reaches the real backend (zero server.log noise); the dates
  // routes under test keep their own stubs per test.
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
    const body = state.cleared
      ? mockItemDates("42", "", "")
      : mockItemDates("42", STORED_START, STORED_END);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
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
    const dates = (posted && posted.ItemDates) || {};
    if (!dates.startDate && !dates.endDate) {
      state.cleared = true;
    }
    const setBody =
      typeof state.setBody === "function"
        ? state.setBody(route)
        : state.setBody;
    await route.fulfill({
      status: state.setStatus,
      contentType: "application/json",
      body:
        typeof setBody === "string" ? setBody : JSON.stringify(setBody),
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

function trackPage(page) {
  const getCalls = [];
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
    if (isGetItemDatesUrl(url) && req.method() === "GET") {
      getCalls.push(url);
    }
  });
  return { getCalls, consoleErrors };
}

test.describe("PublishingShell site-workspace item schedule (#5929)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`sets and saves schedule dates on the selected item ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const { getCalls, consoleErrors } = trackPage(page);
    const state = await stubWorkspaceApis(page);

    const panel = await openSiteWorkspaceSchedule(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.lookup).click();

    await expect(panel.getByTestId(TEST_IDS.start)).toHaveValue(
      "2026-09-18T09:00",
      { timeout: 15000 },
    );
    // The workspace echoes the looked-up id back as a prop, so the panel may
    // legitimately issue the same read-only GET twice. Every call is /42.
    await expect.poll(() => getCalls.length).toBeGreaterThanOrEqual(1);
    for (const url of getCalls) {
      expect(url).toMatch(/\/getitemdates\/42(?:$|[?#])/);
    }

    await panel.getByTestId(TEST_IDS.save).click();
    await expect(panel.getByTestId(TEST_IDS.success)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.success)).toContainText(
      /Schedule dates saved/i,
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    expect(state.setCalls.length).toBeGreaterThanOrEqual(1);
    const saved = state.setCalls[state.setCalls.length - 1];
    expect(saved.ItemDates.itemId).toEqual("42");
    expect(saved.ItemDates.startDate).toEqual(STORED_START);
    expect(saved.ItemDates.endDate).toEqual(STORED_END);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`clears scheduled dates after one confirm ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const { consoleErrors } = trackPage(page);
    const state = await stubWorkspaceApis(page);

    const panel = await openSiteWorkspaceSchedule(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.lookup).click();
    await expect(panel.getByTestId(TEST_IDS.start)).toHaveValue(
      "2026-09-18T09:00",
      { timeout: 15000 },
    );

    await panel.getByTestId(TEST_IDS.clearSubmit).click();
    await expect(panel.getByTestId(TEST_IDS.clearConfirm)).toBeVisible();
    // The confirm posts nothing by itself.
    expect(state.setCalls).toEqual([]);

    await panel.getByTestId(TEST_IDS.clearYes).click();
    await expect(panel.getByTestId(TEST_IDS.success)).toContainText(
      /Scheduled dates cleared/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId(TEST_IDS.start)).toHaveValue("");
    await expect(panel.getByTestId(TEST_IDS.end)).toHaveValue("");
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    expect(state.setCalls.length).toEqual(1);
    expect(state.setCalls[0].ItemDates).toMatchObject({
      itemId: "42",
      startDate: "",
      endDate: "",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`failed clear is an error and cancel writes nothing ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const { consoleErrors } = trackPage(page);
    const state = await stubWorkspaceApis(page, {
      setStatus: 403,
      setBody: { message: "FORBIDDEN" },
    });

    const panel = await openSiteWorkspaceSchedule(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.lookup).click();
    await expect(panel.getByTestId(TEST_IDS.start)).toHaveValue(
      "2026-09-18T09:00",
      { timeout: 15000 },
    );

    await panel.getByTestId(TEST_IDS.clearSubmit).click();
    await panel.getByTestId(TEST_IDS.clearYes).click();
    await expect(panel.getByTestId(TEST_IDS.error)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /FORBIDDEN|Publish Forbidden|HTTP 403/i,
    );
    await expect(panel.getByTestId(TEST_IDS.success)).toHaveCount(0);
    // The failed clear stays on the confirm — it is not success.
    await expect(panel.getByTestId(TEST_IDS.clearConfirm)).toBeVisible();
    expect(state.setCalls.length).toEqual(1);

    await panel.getByTestId(TEST_IDS.clearCancel).click();
    await expect(panel.getByTestId(TEST_IDS.clearConfirm)).toHaveCount(0);
    expect(state.setCalls.length).toEqual(1);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
