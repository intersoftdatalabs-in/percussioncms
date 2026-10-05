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
 * Explorer check-in of one selected page or asset with a revision comment (#5199).
 *
 * {@code npm run test:surface -- --path tests/explorer-checkin-comment.spec.js}
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

const PAGE_LIST = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "Folder",
        category: "folder",
        leaf: false,
      },
      {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "44",
        name: "Logo",
        path: "/Sites/Demo/Logo",
        type: "percImageAsset",
        category: "asset",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 3,
    startIndex: 0,
  },
};

function ownerBody(checkOutUser) {
  return JSON.stringify({
    EditorItemLockInfo: {
      itemName: "Home",
      checkOutUser,
      currentUser: "Admin",
      assignmentType: "Reader",
    },
  });
}

function attachPageErrors(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (/Failed to load resource/i.test(text)) {
      return;
    }
    consoleErrors.push(text);
  });
  return { pageErrors, consoleErrors };
}

async function stubFolder(page) {
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_LIST),
    });
  });
}

async function openExplorerItem(page, rowId) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  const itemRow = page.locator(
    `[data-testid="detail-row-${rowId}"][data-row-kind="item"]`,
  );
  await expect(itemRow).toBeVisible({ timeout: 20_000 });
  await itemRow.click();
  await expect(page.locator('[data-testid="explorer-checkout-owner-user"]')).toHaveText(
    "editor",
    { timeout: 10_000 },
  );
  return itemRow;
}

test.describe("Explorer check-in comment (#5199)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "confirm sends the revision comment and reloads the checkout user",
    { tag: ["@explorer", "@explorer-checkin", "@explorer-checkin-comment"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = attachPageErrors(page);
      const checkinUrls = [];
      let ownerLookups = 0;
      let released = false;
      await stubFolder(page);
      await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
        ownerLookups += 1;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ownerBody(released ? "" : "editor"),
        });
      });
      await page.route("**/rest/editor/items/**/checkin**", async (route) => {
        checkinUrls.push(route.request().url());
        expect(route.request().method()).toBe("POST");
        released = true;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ownerBody(""),
        });
      });

      await openExplorerItem(page, "42");
      const lookupsBefore = ownerLookups;
      await page.locator('[data-testid="action-toolbar-item-Check_In"]').click();
      await page.locator('[data-testid="explorer-checkin-comment-input"]').fill("shipped copy");
      await page.locator('[data-testid="explorer-checkin-confirm"]').click();
      await expect.poll(() => checkinUrls.length, { timeout: 10_000 }).toBe(1);
      expect(checkinUrls[0]).toMatch(/\/rest\/editor\/items\/42\/checkin\?comment=shipped(?:%20|\+)copy/);
      await expect(page.locator('[data-testid="explorer-checkout-owner-none"]')).toBeVisible({
        timeout: 10_000,
      });
      expect(ownerLookups).toBeGreaterThan(lookupsBefore);
      await expect(page.locator('[data-testid="explorer-server-actions-error"]')).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "cancel does not check in and leaves the checkout user",
    { tag: ["@explorer", "@explorer-checkin", "@explorer-checkin-comment"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = attachPageErrors(page);
      let checkinHits = 0;
      let ownerLookups = 0;
      await stubFolder(page);
      await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
        ownerLookups += 1;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ownerBody("editor"),
        });
      });
      await page.route("**/rest/editor/items/**/checkin**", async (route) => {
        checkinHits += 1;
        await route.fulfill({ status: 500, body: "should not check in" });
      });

      await openExplorerItem(page, "42");
      const lookupsBefore = ownerLookups;
      await page.locator('[data-testid="action-toolbar-item-Check_In"]').click();
      await page.locator('[data-testid="explorer-checkin-comment-input"]').fill("do not send");
      await page.locator('[data-testid="explorer-checkin-cancel"]').click();
      await expect(page.locator('[data-testid="explorer-checkin-comment"]')).toHaveCount(0);
      expect(checkinHits).toBe(0);
      expect(ownerLookups).toBe(lookupsBefore);
      await expect(page.locator('[data-testid="explorer-checkout-owner-user"]')).toHaveText(
        "editor",
      );
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "a blank comment checks in without a comment query",
    { tag: ["@explorer", "@explorer-checkin", "@explorer-checkin-comment"] },
    async ({ page }) => {
      const { pageErrors } = attachPageErrors(page);
      const checkinUrls = [];
      await stubFolder(page);
      await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ownerBody("editor"),
        });
      });
      await page.route("**/rest/editor/items/**/checkin**", async (route) => {
        checkinUrls.push(route.request().url());
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ownerBody(""),
        });
      });
      await openExplorerItem(page, "44");
      await page.locator('[data-testid="action-toolbar-item-Check_In"]').click();
      await page.locator('[data-testid="explorer-checkin-confirm"]').click();
      await expect.poll(() => checkinUrls.length, { timeout: 10_000 }).toBe(1);
      expect(checkinUrls[0]).toMatch(/\/rest\/editor\/items\/44\/checkin$/);
      expect(checkinUrls[0]).not.toContain("comment=");
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "a folder selection does not call check-in",
    { tag: ["@explorer", "@explorer-checkin", "@explorer-checkin-comment"] },
    async ({ page }) => {
      const { pageErrors } = attachPageErrors(page);
      let checkinHits = 0;
      await stubFolder(page);
      await page.route("**/rest/editor/items/**/checkin**", async (route) => {
        checkinHits += 1;
        await route.fulfill({ status: 500, body: "folder must not check in" });
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const folder = page.locator('[data-testid="detail-row-7"]');
      await expect(folder).toBeVisible({ timeout: 20_000 });
      await folder.click();
      await expect(page.locator('[data-testid="action-toolbar-item-Check_In"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-checkin-comment"]')).toHaveCount(0);
      expect(checkinHits).toBe(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `HTTP ${status} does not claim success and leaves the checkout user`,
      { tag: ["@explorer", "@explorer-checkin", "@explorer-checkin-comment"] },
      async ({ page }) => {
        const { pageErrors } = attachPageErrors(page);
        let ownerLookups = 0;
        await stubFolder(page);
        await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
          ownerLookups += 1;
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: ownerBody("editor"),
          });
        });
        await page.route("**/rest/editor/items/**/checkin**", async (route) => {
          await route.fulfill({
            status,
            contentType: "text/plain",
            body: `HTTP ${status}`,
          });
        });
        await openExplorerItem(page, "42");
        const lookupsBefore = ownerLookups;
        await page.locator('[data-testid="action-toolbar-item-Check_In"]').click();
        await page.locator('[data-testid="explorer-checkin-comment-input"]').fill("kept");
        await page.locator('[data-testid="explorer-checkin-confirm"]').click();
        const error = page.locator('[data-testid="explorer-server-actions-error"]');
        await expect(error).toBeVisible({ timeout: 10_000 });
        if (status === 400) {
          await expect(error).toContainText(/rejected/i);
        } else if (status === 403) {
          await expect(error).toContainText(/not allowed to check in/i);
        } else {
          await expect(error).toContainText(/not checked out to you/i);
        }
        await expect(page.locator('[data-testid="explorer-checkout-owner-user"]')).toHaveText(
          "editor",
        );
        expect(ownerLookups).toBe(lookupsBefore);
        await expect(page.locator('[data-testid="explorer-flush-cache-status"]')).toHaveCount(0);
        expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      },
    );
  }
});
