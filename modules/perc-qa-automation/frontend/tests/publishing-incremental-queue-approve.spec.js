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
 * Playwright surface: #4912 / parent #4531 — approve one incremental queue item.
 *
 * Tags: @publishing-incremental-queue-approve @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-incremental-queue-approve.spec.js
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

function isApproveUrl(url) {
  const raw = String(url || "");
  return /\/sitemanage\/publish\/incremental\/content\/[^/]+\/[^/]+\/[^/]+\/approve(?:\?|$)/i.test(
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

async function stubQueueApis(page, { approveStatus = 204 } = {}) {
  const approved = [];
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
    if (req.method() === "POST" && isApproveUrl(url)) {
      approved.push(url);
      if (approveStatus >= 400) {
        await route.fulfill({
          status: approveStatus,
          contentType: "text/plain",
          body:
            approveStatus === 403
              ? "Publish forbidden"
              : approveStatus === 400
                ? "Content id is not valid"
                : "Queued item not found",
        });
        return;
      }
      await route.fulfill({ status: 204, body: "" });
      return;
    }
    if (req.method() === "GET" && isContentListGet(url)) {
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
  return approved;
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

const TAGS = ["@publishing-incremental-queue-approve", "@publish"];

test.describe("PublishingShell approve one incremental queue item (#4912)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`confirm approves the row and reloads it ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    const approved = await stubQueueApis(page, { approveStatus: 204 });
    page.once("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-approve").first().click();
    await expect(page.getByTestId("publish-incremental-queue-approved")).toBeVisible();
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
    await expect(page.getByTestId("publish-incremental-queue-row").first()).toContainText(
      "301",
    );
    expect(approved.length).toBe(1);
    expect(approved[0]).toMatch(/\/301\/approve(?:\?|$)/);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`cancel approves nothing ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    const approved = await stubQueueApis(page);
    page.once("dialog", (dialog) => dialog.dismiss());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-approve").first().click();
    await expect(page.getByTestId("publish-incremental-queue-approved")).toHaveCount(0);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
    expect(approved).toEqual([]);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`400 stays visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { approveStatus: 400 });
    page.on("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-approve").first().click();
    const err = page.getByTestId("publish-incremental-queue-approve-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/could not be approved/i);
    await expect(page.getByTestId("publish-incremental-queue-approved")).toHaveCount(0);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
  });

  test(`403 stays visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { approveStatus: 403 });
    page.on("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-approve").first().click();
    const err = page.getByTestId("publish-incremental-queue-approve-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/not allowed/i);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
  });

  test(`404 stays visible ${TAGS.join(" ")}`, async ({ page }) => {
    await stubQueueApis(page, { approveStatus: 404 });
    page.on("dialog", (dialog) => dialog.accept());
    await openPreview(page);
    await page.getByTestId("publish-incremental-queue-approve").first().click();
    const err = page.getByTestId("publish-incremental-queue-approve-error");
    await expect(err).toBeVisible();
    await expect(err).toContainText(/not on the incremental queue/i);
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(2);
  });
});
