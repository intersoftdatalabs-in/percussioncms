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
 * Explorer check-out / check-in of the selected item (#4699).
 *
 * {@code npm run test:surface -- --path tests/explorer-checkout-checkin.spec.js}
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

const PAGE_LIST = {
  PagedItemList: {
    childrenInPage: [
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
    childrenCount: 1,
    startIndex: 0,
  },
};

test.describe("modern React Content Explorer — check-out / check-in", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "Check Out then Check In call workflow endpoints and refresh",
    { tag: ["@explorer-checkout-checkin", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      let checkoutHits = 0;
      let checkinHits = 0;
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(PAGE_LIST),
        });
      });
      await page.route("**/itemmanagement/workflow/checkOut/**", async (route) => {
        checkoutHits += 1;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemUserInfo: { checkOutUser: "Admin", currentUser: "Admin" },
          }),
        });
      });
      await page.route("**/itemmanagement/workflow/checkIn/**", async (route) => {
        checkinHits += 1;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ PSNoContent: { operation: "checkIn" } }),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
        timeout: 20_000,
      });

      const checkout = page.locator('[data-testid="action-toolbar-item-Check_Out"]');
      const checkin = page.locator('[data-testid="action-toolbar-item-Check_In"]');
      await expect(checkout).toHaveCount(0);

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="42"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      await expect(checkout).toBeVisible({ timeout: 15_000 });
      await expect(checkin).toBeVisible({ timeout: 15_000 });
      await checkout.click();
      await expect.poll(() => checkoutHits, { timeout: 10_000 }).toBeGreaterThan(0);
      await checkin.click();
      await expect.poll(() => checkinHits, { timeout: 10_000 }).toBeGreaterThan(0);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Check Out HTTP 409 shows conflict error",
    { tag: ["@explorer-checkout-checkin", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(PAGE_LIST),
        });
      });
      await page.route("**/itemmanagement/workflow/checkOut/**", async (route) => {
        await route.fulfill({
          status: 409,
          contentType: "text/plain",
          body: "Checked out to another user",
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      const checkout = page.locator('[data-testid="action-toolbar-item-Check_Out"]');
      await expect(checkout).toBeVisible({ timeout: 15_000 });
      await checkout.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/another user/i);
      expect(pageErrors).toEqual([]);
    },
  );

  test(
    "Check In HTTP 403 shows forbidden error",
    { tag: ["@explorer-checkout-checkin", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(PAGE_LIST),
        });
      });
      await page.route("**/itemmanagement/workflow/checkIn/**", async (route) => {
        await route.fulfill({
          status: 403,
          contentType: "text/plain",
          body: "Forbidden",
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      const checkin = page.locator('[data-testid="action-toolbar-item-Check_In"]');
      await expect(checkin).toBeVisible({ timeout: 15_000 });
      await checkin.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/not allowed to check in/i);
      expect(pageErrors).toEqual([]);
    },
  );
});
