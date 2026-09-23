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
 * Playwright surface: #4788 / parent #4531 — remove one incremental queue item.
 *
 * Tags: @publishing-incremental-queue-remove @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-incremental-queue-remove.spec.js
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

function isRemoveUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/content\/[^/]+\/[^/]+\/[^/?#]+/i.test(
    raw,
  );
}

async function stubQueueApis(page, { removeStatus = 204 } = {}) {
  const removed = [];
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
    if (req.method() === "DELETE" && isRemoveUrl(url)) {
      removed.push(url);
      if (removeStatus >= 400) {
        await route.fulfill({
          status: removeStatus,
          contentType: "text/plain",
          body: removeStatus === 403 ? "Publish forbidden" : "Queued item not found",
        });
        return;
      }
      await route.fulfill({ status: 204, body: "" });
      return;
    }
    if (req.method() === "GET" && isIncrementalContentListUrl(url) && !isRemoveUrl(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          pagedQueue([
            { id: "301", name: "Home Page" },
            { id: "88", title: "About Us" },
          ]),
        ),
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
  return removed;
}

async function openPreview(page) {
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
  await preview.click();
  await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2, {
    timeout: 15000,
  });
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

const TAGS = ["@publishing-incremental-queue-remove", "@publish"];

test.describe("PublishingShell remove one incremental queue item (#4788)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`confirm removes the row ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    const removed = await stubQueueApis(page, { removeStatus: 204 });
    page.once("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-remove").first().click();
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(1);
    await expect(page.getByTestId("publish-incremental-queue-row")).toContainText("88");
    expect(removed.length).toBe(1);
    expect(removed[0]).toMatch(/\/301(?:\?|$)/);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`cancel leaves the queue unchanged ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    const removed = await stubQueueApis(page);
    page.once("dialog", (dialog) => dialog.dismiss());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-remove").first().click();
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
    expect(removed).toEqual([]);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`403 and 404 stay visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { removeStatus: 403 });
    page.on("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-remove").first().click();
    const err = page.getByTestId("publish-incremental-queue-remove-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/not allowed/i);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
  });

  test(`404 stays visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { removeStatus: 404 });
    page.on("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-remove").first().click();
    const err = page.getByTestId("publish-incremental-queue-remove-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/not on the incremental queue/i);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
  });
});
