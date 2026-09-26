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
 * Explorer shows who has the selected page or asset checked out (#4910).
 *
 * {@code npm run test:surface -- --path tests/explorer-checkout-owner.spec.js}
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

const PAGE_LIST = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "7",
        name: "Demo",
        path: "/Sites/Demo",
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
    ],
    childrenCount: 2,
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

test.describe("modern React Content Explorer — checkout owner", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "selected item shows another user's checkout; folders do not",
    { tag: ["@explorer-checkout-owner", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });
      let lookups = 0;
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(PAGE_LIST),
        });
      });
      await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
        lookups += 1;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ownerBody("editor"),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const folder = page.locator('[data-testid="detail-row-7"]');
      await expect(folder).toBeVisible({ timeout: 20_000 });
      await folder.click();
      await expect(page.locator('[data-testid="explorer-checkout-owner"]')).toHaveCount(0);

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await itemRow.click();
      await expect(page.locator('[data-testid="explorer-checkout-owner-user"]')).toHaveText(
        "editor",
        { timeout: 10_000 },
      );
      expect(lookups).toBeGreaterThan(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "not checked out shows none",
    { tag: ["@explorer-checkout-owner", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(PAGE_LIST),
        });
      });
      await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ownerBody(""),
        });
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await page.locator('[data-testid="detail-row-42"][data-row-kind="item"]').click();
      await expect(page.locator('[data-testid="explorer-checkout-owner-none"]')).toBeVisible({
        timeout: 10_000,
      });
      expect(pageErrors).toEqual([]);
    },
  );

  test(
    "HTTP 403 stays on the checkout owner panel",
    { tag: ["@explorer-checkout-owner", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(PAGE_LIST),
        });
      });
      await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
        await route.fulfill({
          status: 403,
          contentType: "text/plain",
          body: "Forbidden",
        });
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await page.locator('[data-testid="detail-row-42"][data-row-kind="item"]').click();
      await expect(page.locator('[data-testid="explorer-checkout-owner-error"]')).toContainText(
        /not allowed/i,
        { timeout: 10_000 },
      );
      expect(pageErrors).toEqual([]);
    },
  );
});
