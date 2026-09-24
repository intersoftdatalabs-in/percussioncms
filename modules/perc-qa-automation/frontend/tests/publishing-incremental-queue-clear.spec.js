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
 * Playwright surface: #4836 / parent #4531 — clear the incremental queue.
 *
 * Tags: @publishing-incremental-queue-clear @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-incremental-queue-clear.spec.js
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

const TWO_ITEMS = [
  { id: "301", name: "Home Page" },
  { id: "88", title: "About Us" },
];

function isClearUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/content\/[^/]+\/[^/]+\/?(\?|$)/i.test(
    raw,
  );
}

function isRemoveUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/content\/[^/]+\/[^/]+\/[^/?#]+/i.test(
    raw,
  );
}

async function stubQueueApis(page, { clearStatus = 204, afterClear = [] } = {}) {
  const cleared = [];
  let clearedOk = false;
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
    if (req.method() === "DELETE" && isClearUrl(url) && !isRemoveUrl(url)) {
      cleared.push(url);
      if (clearStatus >= 400) {
        await route.fulfill({
          status: clearStatus,
          contentType: "text/plain",
          body: clearStatus === 403 ? "Publish forbidden" : "Site not found",
        });
        return;
      }
      clearedOk = true;
      await route.fulfill({ status: 204, body: "" });
      return;
    }
    if (req.method() === "GET" && isIncrementalContentListUrl(url) && !isRemoveUrl(url)) {
      const children = clearedOk ? afterClear : TWO_ITEMS;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(pagedQueue(children)),
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
  return cleared;
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

const TAGS = ["@publishing-incremental-queue-clear", "@publish"];

test.describe("PublishingShell clear incremental queue (#4836)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`confirm clears and reloads empty ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    const cleared = await stubQueueApis(page, { clearStatus: 204, afterClear: [] });
    page.once("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-clear").click();
    await expect(page.getByTestId("publish-incremental-queue-empty")).toBeVisible();
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(0);
    await expect(page.getByTestId("publish-action-message")).toContainText(/cleared/i);
    expect(cleared.length).toBe(1);
    expect(cleared[0]).not.toMatch(/\/301(?:\?|$)/);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`cancel leaves the queue unchanged ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    const cleared = await stubQueueApis(page);
    page.once("dialog", (dialog) => dialog.dismiss());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-clear").click();
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
    expect(cleared).toEqual([]);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`remaining rows stay with a message ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    await stubQueueApis(page, {
      clearStatus: 204,
      afterClear: [{ id: "88", title: "About Us" }],
    });
    page.once("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-clear").click();
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(1);
    await expect(page.getByTestId("publish-action-message")).toContainText(
      /still on the incremental queue/i,
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`403 stays visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { clearStatus: 403 });
    page.once("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-clear").click();
    const err = page.getByTestId("publish-incremental-queue-remove-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/not allowed to clear/i);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
  });
});
