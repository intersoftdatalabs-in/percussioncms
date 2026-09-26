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
 * Playwright surface: #4913 / parent #4531 — unapprove one incremental queue item.
 *
 * Tags: @publishing-incremental-queue-unapprove @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-incremental-queue-unapprove.spec.js
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

function isUnapproveUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/content\/[^/]+\/[^/]+\/[^/]+\/unapprove(?:\?|$)/i.test(
    raw,
  );
}

function isContentListGet(url) {
  const raw = String(url || "");
  return (
    isIncrementalContentListUrl(raw) &&
    !/\/incremental\/content\/[^/]+\/[^/]+\/[^/?#]+/i.test(raw)
  );
}

function queueRows(clearedIds) {
  const home = { id: "301", name: "Home Page", status: "Approved" };
  const about = { id: "88", title: "About Us", status: "Approved" };
  if (clearedIds.has("301")) {
    delete home.status;
  }
  if (clearedIds.has("88")) {
    delete about.status;
  }
  return [home, about];
}

async function stubQueueApis(page, { unapproveStatus = 204 } = {}) {
  const unapproved = [];
  const cleared = new Set();
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
    if (req.method() === "POST" && isUnapproveUrl(url)) {
      unapproved.push(url);
      if (unapproveStatus >= 400) {
        await route.fulfill({
          status: unapproveStatus,
          contentType: "text/plain",
          body:
            unapproveStatus === 403
              ? "Publish forbidden"
              : unapproveStatus === 400
                ? "Item could not be unapproved"
                : "Queued item not found",
        });
        return;
      }
      const match = String(url).match(/\/([^/]+)\/unapprove(?:\?|$)/i);
      if (match) {
        cleared.add(decodeURIComponent(match[1]));
      }
      await route.fulfill({ status: 204, body: "" });
      return;
    }
    if (req.method() === "GET" && isContentListGet(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(pagedQueue(queueRows(cleared))),
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
  return unapproved;
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
  await expect(page.getByTestId("publish-incremental-queue-unapprove")).toHaveCount(2);
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

const TAGS = ["@publishing-incremental-queue-unapprove", "@publish"];

test.describe("PublishingShell unapprove one incremental queue item (#4913)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`confirm clears approval on one row and reloads ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    const calls = await stubQueueApis(page, { unapproveStatus: 204 });
    page.once("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-unapprove").first().click();
    await expect(page.getByTestId("publish-action-message")).toContainText(
      /Approval removed/i,
    );
    await expect(page.getByTestId("publish-incremental-queue-approved")).toHaveCount(1);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
    await expect(page.getByTestId("publish-incremental-queue-row").first()).toContainText(
      "301",
    );
    await expect(page.getByTestId("publish-incremental-queue-row").nth(1)).toContainText(
      /Approved/i,
    );
    expect(calls.length).toBe(1);
    expect(calls[0]).toMatch(/\/301\/unapprove(?:\?|$)/);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`cancel writes nothing ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    const calls = await stubQueueApis(page);
    page.once("dialog", (dialog) => dialog.dismiss());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-unapprove").first().click();
    await expect(page.getByTestId("publish-incremental-queue-approved")).toHaveCount(2);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
    expect(calls).toEqual([]);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`400 stays visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { unapproveStatus: 400 });
    page.on("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-unapprove").first().click();
    const err = page.getByTestId("publish-incremental-queue-unapprove-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/could not be unapproved/i);
    await expect(page.getByTestId("publish-incremental-queue-approved")).toHaveCount(2);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
  });

  test(`403 stays visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { unapproveStatus: 403 });
    page.on("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-unapprove").first().click();
    const err = page.getByTestId("publish-incremental-queue-unapprove-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/not allowed/i);
    await expect(page.getByTestId("publish-incremental-queue-approved")).toHaveCount(2);
  });

  test(`404 stays visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { unapproveStatus: 404 });
    page.on("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-unapprove").first().click();
    const err = page.getByTestId("publish-incremental-queue-unapprove-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/not on the incremental queue/i);
    await expect(page.getByTestId("publish-incremental-queue-approved")).toHaveCount(2);
  });
});
