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
 * Playwright surface: #6472 / parent #4531 — publish several item ids now
 * from the PublishingShell site workspace with one confirm, a per-item
 * report naming each failure, and a retry that republishes only the failed
 * ids (a per-item 403 is a named failure, not full success; review alone
 * writes nothing).
 *
 * Tags: @publishing-item-publish-now-batch @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing/publishing-item-publish-now-batch.spec.js
 * from modules/perc-qa-automation/frontend
 */

"use strict";

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const TAGS = ["@publishing-item-publish-now-batch", "@publish", "@smoke"];

const FIRST_ID = "9001";
const SECOND_ID = "9002";

function publishingSitesUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=publish`;
}

function publishIdFromUrl(url) {
  const match = /\/(page|resource)(?:\/staging)?\/([^/?#]+)/.exec(
    String(url || ""),
  );
  return match ? decodeURIComponent(match[2]) : "";
}

function isPublishRoute(url) {
  return /\/sitemanage\/publish\/(page|resource)(\/staging)?\/[^/?#]+/.test(
    String(url || ""),
  );
}

async function stubBatchApis(page, opts = {}) {
  const state = {
    publishCalls: [],
    failIds: opts.failIds || {},
  };
  // The workspace shares one item id across every item panel: looking up ids
  // in the publish-now panel also nudges the history, actions-menu, and
  // schedule panels to read. Stub those sibling reads so the fake lookup ids
  // never reach the real backend (zero server.log noise); the publish routes
  // under test keep their own stubs per test.
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
      body: JSON.stringify({ ArrayList: [] }),
    });
  });
  await page.route("**/sitemanage/site**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        SiteSummary: [{ name: "Demo", siteId: "9", id: "9" }],
      }),
    });
  });
  await page.route("**/publishmanagement/servers/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/sitemanage/pubstatus/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/sitemanage/publish/takedown/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ status: "Success" }),
    });
  });
  const stubPublish = async (route) => {
    const url = route.request().url();
    if (url.includes("/staging/")) {
      await route.continue();
      return;
    }
    const id = publishIdFromUrl(url);
    state.publishCalls.push({
      method: route.request().method(),
      url,
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
  };
  await page.route("**/sitemanage/publish/page/**", stubPublish);
  await page.route("**/sitemanage/publish/resource/**", stubPublish);
  return state;
}

async function openSiteWorkspacePublishNow(page) {
  await page.goto(publishingSitesUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-sites")).toBeVisible({
    timeout: 30000,
  });
  const siteCard = page
    .locator('[data-testid^="publish-site-card-"]')
    .first();
  await expect(siteCard).toBeVisible({ timeout: 30000 });
  await siteCard.click();
  await expect(page.getByTestId("publish-site-workspace")).toBeVisible();
  const panel = page.getByTestId("item-publish-now");
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
    if (/Download the React DevTools/i.test(text)) {
      return;
    }
    consoleErrors.push(text);
  });
  return consoleErrors;
}

test.describe("PublishingShell multi-item publish now (#6472)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(`publishes two ids with one confirm and a per-item report ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    const state = await stubBatchApis(page);

    const panel = await openSiteWorkspacePublishNow(page);
    await panel.getByTestId("item-publish-now-id").fill(`${FIRST_ID}, ${SECOND_ID}`);
    await panel.getByTestId("item-publish-now-review").click();

    // One confirm lists both ids; review alone writes nothing.
    await expect(panel.getByTestId("item-publish-now-confirm")).toBeVisible({
      timeout: 15000,
    });
    await expect(
      panel.getByTestId(`item-publish-now-batch-item-${FIRST_ID}`),
    ).toBeVisible();
    await expect(
      panel.getByTestId(`item-publish-now-batch-item-${SECOND_ID}`),
    ).toBeVisible();
    expect(state.publishCalls).toEqual([]);

    await panel.getByTestId("item-publish-now-submit").click();
    await expect(panel.getByTestId("item-publish-now-success")).toContainText(
      /all 2 items/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId("item-publish-now-error")).toHaveCount(0);
    const report = panel.getByTestId("item-publish-now-batch-report");
    await expect(report).toBeVisible();
    await expect(
      panel.getByTestId(`item-publish-now-batch-report-item-${FIRST_ID}`),
    ).toContainText(/publish started/i);
    await expect(
      panel.getByTestId(`item-publish-now-batch-report-item-${SECOND_ID}`),
    ).toContainText(/publish started/i);
    const publishedIds = state.publishCalls.map((call) => call.id);
    expect(publishedIds).toContain(FIRST_ID);
    expect(publishedIds).toContain(SECOND_ID);
    for (const call of state.publishCalls) {
      expect(isPublishRoute(call.url)).toBe(true);
    }
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`a per-item 403 is named and retry republishes only the failed id ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsoleErrors(page);
    const state = await stubBatchApis(page, {
      failIds: { [SECOND_ID]: "User demo is editing this page." },
    });

    const panel = await openSiteWorkspacePublishNow(page);
    await panel.getByTestId("item-publish-now-id").fill(`${FIRST_ID} ${SECOND_ID}`);
    await panel.getByTestId("item-publish-now-review").click();
    await expect(panel.getByTestId("item-publish-now-confirm")).toBeVisible({
      timeout: 15000,
    });
    await panel.getByTestId("item-publish-now-submit").click();

    await expect(panel.getByTestId("item-publish-now-error")).toContainText(
      /Published 1 of 2/i,
      { timeout: 15000 },
    );
    await expect(panel.getByTestId("item-publish-now-error")).toContainText(
      new RegExp(`${SECOND_ID} \\(User demo is editing this page\\.\\)`),
    );
    await expect(
      panel.getByTestId(`item-publish-now-batch-report-item-${FIRST_ID}`),
    ).toContainText(/publish started/i);
    await expect(panel.getByTestId("item-publish-now-success")).toHaveCount(0);
    await expect(panel.getByTestId("item-publish-now-retry")).toBeVisible();

    // The retry run publishes only the failed id; the row flips to started.
    await page.unroute("**/sitemanage/publish/page/**");
    const retryCalls = [];
    await page.route("**/sitemanage/publish/page/**", async (route) => {
      if (route.request().url().includes("/staging/")) {
        await route.continue();
        return;
      }
      retryCalls.push(publishIdFromUrl(route.request().url()));
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ status: "Success" }),
      });
    });
    const before = state.publishCalls.length;
    await panel.getByTestId("item-publish-now-retry").click();
    await expect(panel.getByTestId("item-publish-now-success")).toContainText(
      /all 1 items/i,
      { timeout: 15000 },
    );
    expect(retryCalls).toEqual([SECOND_ID]);
    expect(state.publishCalls.length).toBe(before);
    await expect(
      panel.getByTestId(`item-publish-now-batch-report-item-${SECOND_ID}`),
    ).toContainText(/publish started/i);
    await expect(panel.getByTestId("item-publish-now-retry")).toHaveCount(0);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
