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
 * Explorer single-item Stage / Remove-from-Staging confirm dialog parity
 * with the bulk report (#6530, parent #4530).
 *
 * <p>A single checked page or asset routes through the same BulkStageDialog
 * chrome as multi-select (#6469): confirm list, per-item outcomes, and
 * summary — instead of window.confirm plus the transient Server-actions
 * banner. Folder-children dialog coverage lives in
 * explorer-folder-stage.spec.js.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl.py qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-single-stage.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const LISTING = {
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
      {
        id: "43",
        name: "About",
        path: "/Sites/Demo/About",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        category: "folder",
        accessLevel: "WRITE",
        leaf: false,
      },
    ],
    childrenCount: 3,
    startIndex: 0,
  },
};

const NEWS_CHILDREN = {
  PathItem: [
    {
      id: "44",
      name: "Story1",
      path: "/Sites/Demo/News/Story1",
      type: "percPage",
      category: "page",
    },
  ],
};

/**
 * @param {import('@playwright/test').Page} page
 */
async function installListingRoutes(page) {
  /** @type {string[]} */
  const staged = [];
  /** @type {string[]} */
  const unstaged = [];
  // Mock the checkout-owner lookup so the mocked row ids never hit the
  // real server (a lookup for a fake id logs a server ERROR). Benign
  // reader assignment: nothing is checked out (#6997, same as takedown).
  await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: "",
          currentUser: "Admin",
          assignmentType: "Reader",
        },
      }),
    });
  });
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/pathmanagement/path/folder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(NEWS_CHILDREN),
    });
  });
  const fulfillStage = async (route) => {
    staged.push(route.request().url());
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  };
  await page.route(
    "**/services/sitemanage/publish/page/staging/**",
    fulfillStage,
  );
  await page.route(
    "**/services/sitemanage/publish/resource/staging/**",
    fulfillStage,
  );
  const fulfillUnstage = async (route) => {
    unstaged.push(route.request().url());
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  };
  await page.route(
    "**/services/sitemanage/publish/takedown/page/staging/**",
    fulfillUnstage,
  );
  await page.route(
    "**/services/sitemanage/publish/takedown/resource/staging/**",
    fulfillUnstage,
  );
  return { staged, unstaged };
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function openListing(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Explorer single-item and folder-children Stage dialog", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "single item Stage opens the bulk dialog and reports the row",
    { tag: ["@explorer", "@explorer-staging", "@explorer-single-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged } = await installListingRoutes(page);
      await openListing(page);
      const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
      await expect(home).toBeVisible({ timeout: 20_000 });
      await home.click();
      await page.locator('[data-testid="detail-select-42"]').check();
      await page.locator('[data-testid="action-toolbar-item-Stage"]').click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-items"]'),
      ).toContainText(/Home/);
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-summary"]'),
      ).toContainText(/1 staged, 0 skipped, 0 failed/i, { timeout: 10_000 });
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows).toHaveCount(1);
      await expect(rows.nth(0)).toContainText(/Home.*Staged/i);
      await expect
        .poll(() => staged.filter((url) => url.includes("/staging/")).length)
        .toBe(1);
      expect(staged.some((url) => url.includes("/staging/42"))).toBe(true);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      await page.screenshot({
        path: "test-results/live-proof-6530-single-stage.png",
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "single item Stage cancel stages nothing",
    { tag: ["@explorer", "@explorer-staging", "@explorer-single-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged } = await installListingRoutes(page);
      await openListing(page);
      const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
      await expect(home).toBeVisible({ timeout: 20_000 });
      await home.click();
      await page.locator('[data-testid="detail-select-42"]').check();
      await page.locator('[data-testid="action-toolbar-item-Stage"]').click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="explorer-bulk-stage-cancel"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(staged).toEqual([]);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "single item Remove from Staging opens the bulk dialog and reports the row",
    { tag: ["@explorer", "@explorer-staging", "@explorer-single-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { unstaged } = await installListingRoutes(page);
      await openListing(page);
      const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
      await expect(home).toBeVisible({ timeout: 20_000 });
      await home.click();
      await page.locator('[data-testid="detail-select-42"]').check();
      await page
        .locator('[data-testid="action-toolbar-item-Remove_from_Staging"]')
        .click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-items"]'),
      ).toContainText(/Home/);
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-summary"]'),
      ).toContainText(/1 removed from staging, 0 skipped, 0 failed/i, {
        timeout: 10_000,
      });
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows).toHaveCount(1);
      await expect(rows.nth(0)).toContainText(/Home.*Removed from staging/i);
      await expect.poll(() => unstaged.length).toBe(1);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "keyboard Enter opens Stage; Escape cancels and returns focus (#6997)",
    { tag: ["@explorer", "@explorer-staging", "@explorer-single-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged } = await installListingRoutes(page);
      await openListing(page);
      const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
      await expect(home).toBeVisible({ timeout: 20_000 });
      await home.click();
      await page.locator('[data-testid="detail-select-42"]').check();
      const stage = page.locator('[data-testid="action-toolbar-item-Stage"]');
      await expect(stage).toBeVisible({ timeout: 15_000 });
      // Keyboard path: focus the toolbar control and activate with Enter.
      await stage.focus();
      await page.keyboard.press("Enter");
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await expect(dialog).toHaveAttribute("role", "dialog");
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-bulk-stage-dialog"]',
      });
      // Escape cancels: nothing staged, no refresh, focus returns.
      await page.keyboard.press("Escape");
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(staged).toEqual([]);
      await expect(stage).toBeFocused();
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
