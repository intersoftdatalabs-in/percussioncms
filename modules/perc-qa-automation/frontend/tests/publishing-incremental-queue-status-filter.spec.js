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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Playwright surface: #6259 / parent #4531 — Site workspace incremental
 * queue publish-status column + All/Approved/Pending filter facet.
 * The facet narrows already-loaded rows client-side (zero new REST).
 *
 * Tags: @publishing-incremental-queue @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-incremental-queue-status-filter.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  publishingProductUrl,
  isPubServersListUrl,
  isIncrementalContentListUrl,
  isIncrementalRelatedListUrl,
  mockPublishServer,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-incremental-site");

function pagedQueue(children) {
  return {
    PagedItemList: {
      childrenInPage: children,
      childrenCount: children.length,
    },
  };
}

async function stubQueueApis(page, { contentBody }) {
  await page.route("**/publishmanagement/servers/**", async (route) => {
    const req = route.request();
    if (req.method() === "GET" && isPubServersListUrl(req.url())) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ PubServer: [mockPublishServer()] }),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/sitemanage/publish/incremental/**", async (route) => {
    const req = route.request();
    const url = req.url();
    if (req.method() === "GET" && isIncrementalContentListUrl(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(contentBody),
      });
      return;
    }
    if (req.method() === "GET" && isIncrementalRelatedListUrl(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(pagedQueue([])),
      });
      return;
    }
    await route.continue();
  });
}

async function openSiteWorkspace(page) {
  await page.goto(publishingProductUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  const siteCard = page.locator('[data-testid^="publish-site-card-"]').first();
  await expect(siteCard).toBeVisible({ timeout: 30000 });
  await siteCard.click();
  await expect(page.getByTestId("publish-site-workspace")).toBeVisible();
  const preview = page.getByTestId("publish-incremental-preview-btn");
  await expect(preview).toBeEnabled({ timeout: 15000 });
  return preview;
}

function trackConsole(page) {
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
  return consoleErrors;
}

const TAGS = ["@publishing-incremental-queue", "@publish"];

test.describe("PublishingShell incremental queue status filter (#6259)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`status column plus All Approved Pending facet ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubQueueApis(page, {
      contentBody: pagedQueue([
        { id: "301", name: "Home Page", status: "Approved" },
        { contentid: 88, title: "About Us" },
      ]),
    });
    const preview = await openSiteWorkspace(page);
    await preview.click();
    const list = page.getByTestId("publish-incremental-queue-list");
    await expect(list).toBeVisible({ timeout: 15000 });
    const rows = page.getByTestId("publish-incremental-queue-row");
    await expect(rows).toHaveCount(2);
    await expect(
      page.getByTestId("publish-incremental-queue-status-approved"),
    ).toContainText(/Approved/i);
    await expect(
      page.getByTestId("publish-incremental-queue-status-pending"),
    ).toContainText(/Pending/i);

    const facet = page.getByTestId("publish-incremental-queue-status-filter");
    await expect(facet).toBeVisible();
    await facet.selectOption("approved");
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText("301");

    await facet.selectOption("pending");
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText("88");

    await facet.selectOption("all");
    await expect(rows).toHaveCount(2);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`facet with no matches shows empty-filter state ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubQueueApis(page, {
      contentBody: pagedQueue([{ contentid: 88, title: "About Us" }]),
    });
    const preview = await openSiteWorkspace(page);
    await preview.click();
    const rows = page.getByTestId("publish-incremental-queue-row");
    await expect(rows).toHaveCount(1);
    const facet = page.getByTestId("publish-incremental-queue-status-filter");
    await facet.selectOption("approved");
    await expect(
      page.getByTestId("publish-incremental-queue-empty-filter"),
    ).toBeVisible({ timeout: 15000 });
    await expect(rows).toHaveCount(0);
    await expect(page.getByTestId("publish-incremental-queue-list")).toHaveCount(
      0,
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
