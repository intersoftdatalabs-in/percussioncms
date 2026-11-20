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
 * Explorer single-item Stage / Remove from Staging (#5631 / parent #4530).
 *
 * <p>Dialog-parity edition (#6678): the production shell always routes
 * single-item Stage/Unstage through {@code BulkStageDialog}
 * ({@code confirmBulkStage}, #6530), so no native {@code window.confirm}
 * fires. One selected page row + Stage opens the bulk dialog, confirms that
 * item, and reports the row; Remove from Staging behaves the same. Cancel
 * stages nothing. HTTP 400/403/409 name the item in the dialog report row
 * (for example {@code Home — Failed (HTTP 403)}) and keep the selection —
 * the transient Server-actions banner no longer appears for this flow.</p>
 *
 * <p>Success-path coverage overlaps
 * {@code explorer-single-stage.spec.js}; it is kept here so this file still
 * matches the retired confirm-flow coverage one for one.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-staging.spec.js}
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
    ],
    childrenCount: 2,
    startIndex: 0,
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ homeStatus?: number }} [opts]
 */
async function installStagingRoutes(page, opts = {}) {
  const homeStatus = opts.homeStatus ?? 200;
  /** @type {string[]} */
  const staged = [];
  /** @type {string[]} */
  const unstaged = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route(
    "**/services/sitemanage/publish/page/staging/**",
    async (route) => {
      const url = route.request().url();
      staged.push(url);
      if (url.includes("/staging/42")) {
        await route.fulfill({
          status: homeStatus,
          contentType: "application/json",
          body: homeStatus === 200 ? "{}" : "denied",
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "{}",
      });
    },
  );
  await page.route(
    "**/services/sitemanage/publish/resource/staging/**",
    async (route) => {
      staged.push(route.request().url());
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "{}",
      });
    },
  );
  const unstage = async (route) => {
    const url = route.request().url();
    unstaged.push(url);
    if (url.includes("/staging/42")) {
      await route.fulfill({
        status: homeStatus,
        contentType: "application/json",
        body: homeStatus === 200 ? "{}" : "denied",
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  };
  await page.route(
    "**/services/sitemanage/publish/takedown/page/staging/**",
    unstage,
  );
  await page.route(
    "**/services/sitemanage/publish/takedown/resource/staging/**",
    unstage,
  );
  return { staged, unstaged };
}

async function openListing(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(home).toBeVisible({ timeout: 20_000 });
  await home.click();
  await expect(
    page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
  ).toBeVisible({ timeout: 10_000 });
  await page.locator('[data-testid="detail-select-42"]').check();
}

test.describe("Explorer single-item Stage / Remove from Staging", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "one confirm stages the selected page and refreshes the row",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged, unstaged } = await installStagingRoutes(page);
      await openListing(page);
      const button = page.locator('[data-testid="action-toolbar-item-Stage"]');
      await expect(button).toBeVisible({ timeout: 15_000 });
      await button.click();
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
      await expect.poll(() => staged.length).toBe(1);
      expect(staged.some((url) => url.includes("/page/staging/42"))).toBe(true);
      expect(unstaged).toEqual([]);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "one confirm removes the selected page from staging and refreshes the row",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged, unstaged } = await installStagingRoutes(page);
      await openListing(page);
      const button = page.locator(
        '[data-testid="action-toolbar-item-Remove_from_Staging"]',
      );
      await expect(button).toBeVisible({ timeout: 15_000 });
      await button.click();
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
      expect(
        unstaged.some((url) => url.includes("/takedown/page/staging/42")),
      ).toBe(true);
      expect(staged).toEqual([]);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "cancel changes nothing",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged, unstaged } = await installStagingRoutes(page);
      await openListing(page);
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
      expect(unstaged).toEqual([]);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 403 on Stage names the item and keeps the selection",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged } = await installStagingRoutes(page, { homeStatus: 403 });
      await openListing(page);
      await page.locator('[data-testid="action-toolbar-item-Stage"]').click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      const summary = page.locator(
        '[data-testid="explorer-bulk-stage-summary"]',
      );
      await expect(summary).toContainText(/0 staged, 0 skipped, 1 failed/i, {
        timeout: 10_000,
      });
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows).toHaveCount(1);
      await expect(rows.nth(0)).toContainText(/Home.*Failed.*HTTP 403/i);
      await expect.poll(() => staged.length).toBe(1);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 409 on Remove from Staging names the item and keeps the selection",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { unstaged } = await installStagingRoutes(page, { homeStatus: 409 });
      await openListing(page);
      await page
        .locator('[data-testid="action-toolbar-item-Remove_from_Staging"]')
        .click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      const summary = page.locator(
        '[data-testid="explorer-bulk-stage-summary"]',
      );
      await expect(summary).toContainText(
        /0 removed from staging, 0 skipped, 1 failed/i,
        { timeout: 10_000 },
      );
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows).toHaveCount(1);
      await expect(rows.nth(0)).toContainText(/Home.*Failed.*HTTP 409/i);
      await expect.poll(() => unstaged.length).toBe(1);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 400 on Stage names the item and keeps the selection",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged } = await installStagingRoutes(page, { homeStatus: 400 });
      await openListing(page);
      await page.locator('[data-testid="action-toolbar-item-Stage"]').click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      const summary = page.locator(
        '[data-testid="explorer-bulk-stage-summary"]',
      );
      await expect(summary).toContainText(/0 staged, 0 skipped, 1 failed/i, {
        timeout: 10_000,
      });
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows).toHaveCount(1);
      await expect(rows.nth(0)).toContainText(/Home.*Failed.*HTTP 400/i);
      await expect.poll(() => staged.length).toBe(1);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 400 on Remove from Staging names the item and keeps the selection",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { unstaged } = await installStagingRoutes(page, { homeStatus: 400 });
      await openListing(page);
      await page
        .locator('[data-testid="action-toolbar-item-Remove_from_Staging"]')
        .click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      const summary = page.locator(
        '[data-testid="explorer-bulk-stage-summary"]',
      );
      await expect(summary).toContainText(
        /0 removed from staging, 0 skipped, 1 failed/i,
        { timeout: 10_000 },
      );
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows).toHaveCount(1);
      await expect(rows.nth(0)).toContainText(/Home.*Failed.*HTTP 400/i);
      await expect.poll(() => unstaged.length).toBe(1);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
