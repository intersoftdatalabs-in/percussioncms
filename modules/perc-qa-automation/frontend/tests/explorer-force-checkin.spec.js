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
 * Explorer Admin force check-in (#4561).
 *
 * {@code npm run test:surface -- --path tests/explorer-force-checkin.spec.js}
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

test.describe("modern React Content Explorer — force check-in", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "Admin Force Check-in confirms and calls forceCheckIn then refreshes",
    { tag: ["@explorer-force-checkin", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      let forceCheckinHits = 0;
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
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
          }),
        });
      });
      await page.route("**/itemmanagement/workflow/forceCheckIn/**", async (route) => {
        forceCheckinHits += 1;
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

      const force = page.locator('[data-testid="action-toolbar-item-Force_Checkin"]');
      await expect(force).toBeVisible({ timeout: 15_000 });
      await force.click();
      await expect
        .poll(() => forceCheckinHits, { timeout: 10_000 })
        .toBeGreaterThan(0);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Force Check-in HTTP 409 shows not-checked-out error",
    { tag: ["@explorer-force-checkin", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
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
          }),
        });
      });
      await page.route("**/itemmanagement/workflow/forceCheckIn/**", async (route) => {
        await route.fulfill({
          status: 409,
          contentType: "text/plain",
          body: "Item is not checked out",
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      const force = page.locator('[data-testid="action-toolbar-item-Force_Checkin"]');
      await expect(force).toBeVisible({ timeout: 15_000 });
      await force.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/not checked out/i);
      expect(pageErrors).toEqual([]);
    },
  );
});
