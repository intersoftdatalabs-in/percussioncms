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
 * Playwright surface: #5930 / parent #4531 — take down (unpublish) the
 * PublishingShell site-workspace selected page or asset via the existing
 * takedown route (review lists linked pages, confirm submits, HTTP
 * 400/403/409 stay on the panel as errors).
 *
 * Tags: @publishing-item-takedown @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing/publishing-item-takedown.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  TEST_IDS,
  publishingSitesUrl,
  isFindLinkedItemsUrl,
  isTakedownUrl,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-item-takedown");

const TAGS = ["@publishing-item-takedown", "@publish", "@smoke"];

async function stubWorkspaceApis(page, opts = {}) {
  const state = {
    takedownStatus: opts.takedownStatus ?? 200,
    takedownBody: opts.takedownBody ?? { status: "Success" },
    takedownCalls: [],
  };
  // The workspace shares one item id across every item panel: looking up an
  // id in the takedown panel also nudges the history, actions-menu, and
  // schedule panels to read. Stub those sibling reads so the fake lookup id
  // never reaches the real backend (zero server.log noise); the linked-page
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
    const linkedBody =
      opts.linkedBody ?? { ArrayList: [{ pagePath: "/Sites/Demo/Home", id: "7" }] };
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body:
        typeof linkedBody === "string"
          ? linkedBody
          : JSON.stringify(linkedBody),
    });
  });
  await page.route("**/sitemanage/publish/takedown/**", async (route) => {
    let posted = null;
    try {
      posted = route.request().postDataJSON();
    } catch {
      posted = null;
    }
    state.takedownCalls.push({
      method: route.request().method(),
      url: route.request().url(),
      body: posted,
    });
    const takedownBody =
      typeof state.takedownBody === "function"
        ? state.takedownBody(route)
        : state.takedownBody;
    await route.fulfill({
      status: state.takedownStatus,
      contentType: "application/json",
      body:
        typeof takedownBody === "string"
          ? takedownBody
          : JSON.stringify(takedownBody),
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

function trackPage(page) {
  const takedownReads = [];
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
    if (isFindLinkedItemsUrl(url) && req.method() === "GET") {
      takedownReads.push(url);
    }
  });
  return { takedownReads, consoleErrors };
}

test.describe("PublishingShell site-workspace item takedown (#5930)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(`reviews linked pages then takes down the selected page ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const { takedownReads, consoleErrors } = trackPage(page);
    const state = await stubWorkspaceApis(page);

    const panel = await openSiteWorkspaceTakedown(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.review).click();

    // Review lists the linked page paths on the confirm and writes nothing.
    await expect(panel.getByTestId(TEST_IDS.confirm)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.linked)).toContainText(
      "/Sites/Demo/Home",
    );
    await expect.poll(() => takedownReads.length).toBeGreaterThanOrEqual(1);
    for (const url of takedownReads) {
      expect(url).toMatch(/\/findLinkedItems\/42(?:$|[?#])/);
    }
    expect(state.takedownCalls).toEqual([]);

    await panel.getByTestId(TEST_IDS.submit).click();
    await expect(panel.getByTestId(TEST_IDS.success)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.success)).toContainText(
      /Take down started/i,
    );
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    expect(state.takedownCalls.length).toBeGreaterThanOrEqual(1);
    const submitted = state.takedownCalls[state.takedownCalls.length - 1];
    expect(isTakedownUrl(submitted.url)).toBe(true);
    expect(submitted.url).toMatch(/\/takedown\/page\/42(?:$|[?#])/);
    // A non-empty confirm list PUTs the linked pages (classic Finder).
    expect(submitted.method).toEqual("PUT");
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`takes down an asset with no linked pages via GET ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const { consoleErrors } = trackPage(page);
    const state = await stubWorkspaceApis(page, {
      linkedBody: { ArrayList: [] },
    });

    const panel = await openSiteWorkspaceTakedown(page);
    await panel.getByTestId(TEST_IDS.input).fill("99");
    await panel.getByTestId(TEST_IDS.kindResource).check();
    await panel.getByTestId(TEST_IDS.review).click();

    await expect(panel.getByTestId(TEST_IDS.confirm)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.linkedEmpty)).toBeVisible();
    expect(state.takedownCalls).toEqual([]);

    await panel.getByTestId(TEST_IDS.submit).click();
    await expect(panel.getByTestId(TEST_IDS.success)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.error)).toHaveCount(0);
    expect(state.takedownCalls.length).toBeGreaterThanOrEqual(1);
    const submitted = state.takedownCalls[state.takedownCalls.length - 1];
    expect(submitted.url).toMatch(/\/takedown\/resource\/99(?:$|[?#])/);
    // An empty confirm list keeps the GET contract (no linked-page PUT).
    expect(submitted.method).toEqual("GET");
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`failed takedown is an error and review alone writes nothing ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const { consoleErrors } = trackPage(page);
    const state = await stubWorkspaceApis(page, {
      takedownStatus: 403,
      takedownBody: { message: "FORBIDDEN" },
    });

    const panel = await openSiteWorkspaceTakedown(page);
    await panel.getByTestId(TEST_IDS.input).fill("42");
    await panel.getByTestId(TEST_IDS.review).click();
    await expect(panel.getByTestId(TEST_IDS.confirm)).toBeVisible({
      timeout: 15000,
    });
    // The confirm posts nothing by itself.
    expect(state.takedownCalls).toEqual([]);

    await panel.getByTestId(TEST_IDS.submit).click();
    await expect(panel.getByTestId(TEST_IDS.error)).toBeVisible({
      timeout: 15000,
    });
    await expect(panel.getByTestId(TEST_IDS.error)).toContainText(
      /Forbidden|HTTP 403/i,
    );
    await expect(panel.getByTestId(TEST_IDS.success)).toHaveCount(0);
    // The failed takedown stays on the confirm — it is not success.
    await expect(panel.getByTestId(TEST_IDS.confirm)).toBeVisible();
    expect(state.takedownCalls.length).toEqual(1);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
