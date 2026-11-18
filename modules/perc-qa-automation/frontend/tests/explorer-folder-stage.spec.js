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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Explorer Stage / Remove from Staging for a selected folder (#6419 /
 * parent #4530; dialog parity #6530).
 *
 * <p>Folder-children Stage / Remove-from-Staging routes through the same
 * BulkStageDialog chrome as multi-select (#6469): the dialog lists the
 * direct children, runs the batch, and reports every row outcome with a
 * summary — replacing the native confirm plus the transient Server-actions
 * banner.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl.py qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-folder-stage.spec.js}
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
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        category: "folder",
        accessLevel: "WRITE",
        leaf: false,
      },
    ],
    childrenCount: 1,
    startIndex: 0,
  },
};

const CHILDREN = {
  PathItem: [
    {
      id: "42",
      name: "Home",
      path: "/Sites/Demo/News/Home",
      type: "percPage",
      category: "page",
    },
    {
      id: "99",
      name: "logo",
      path: "/Assets/logo.png",
      type: "percImageAsset",
      category: "asset",
    },
    {
      id: "8",
      name: "Nested",
      path: "/Sites/Demo/News/Nested",
      type: "folder",
      category: "folder",
      leaf: false,
    },
  ],
};

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ homeBody?: string, homeStatus?: number }} [opts]
 */
async function installFolderStageRoutes(page, opts = {}) {
  const homeBody = opts.homeBody ?? "{}";
  const homeStatus = opts.homeStatus ?? 200;
  /** @type {string[]} */
  const staged = [];
  /** @type {string[]} */
  const unstaged = [];
  /** @type {string[]} */
  const listed = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/pathmanagement/path/folder/**", async (route) => {
    listed.push(route.request().url());
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(CHILDREN),
    });
  });
  const fulfillStage = async (route) => {
    const url = route.request().url();
    staged.push(url);
    if (url.includes("/staging/42")) {
      await route.fulfill({
        status: homeStatus,
        contentType: "application/json",
        body: homeStatus === 200 ? homeBody : "denied",
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
    "**/services/sitemanage/publish/page/staging/**",
    fulfillStage,
  );
  await page.route(
    "**/services/sitemanage/publish/resource/staging/**",
    fulfillStage,
  );
  const fulfillUnstage = async (route) => {
    const url = route.request().url();
    unstaged.push(url);
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
  return { staged, unstaged, listed };
}

async function openFolder(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const folder = page.locator('[data-testid="detail-row-7"][data-row-kind="folder"]');
  await expect(folder).toBeVisible({ timeout: 20_000 });
  await folder.click();
  await expect(
    page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="7"]'),
  ).toBeVisible({ timeout: 10_000 });
}

test.describe("Explorer folder Stage / Remove from Staging", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "folder Stage dialog lists children and reports every row",
    { tag: ["@explorer", "@explorer-staging", "@explorer-folder-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged, unstaged, listed } = await installFolderStageRoutes(page);
      await openFolder(page);
      const stage = page.locator('[data-testid="action-toolbar-item-Stage"]');
      await expect(stage).toBeVisible({ timeout: 15_000 });
      await stage.click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-items"]'),
      ).toContainText(/Home/);
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-items"]'),
      ).toContainText(/logo/);
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-skipped"]'),
      ).toContainText(/Nested/i);
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-summary"]'),
      ).toContainText(/2 staged, 1 skipped, 0 failed/i, { timeout: 10_000 });
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows).toHaveCount(3);
      await expect(rows.nth(0)).toContainText(/Home.*Staged/i);
      await expect(rows.nth(1)).toContainText(/logo.*Staged/i);
      await expect(rows.nth(2)).toContainText(/Nested/i);
      await expect.poll(() => staged.length).toBe(2);
      expect(staged.some((url) => url.includes("/page/staging/42"))).toBe(true);
      expect(staged.some((url) => url.includes("/resource/staging/99"))).toBe(true);
      expect(unstaged).toEqual([]);
      expect(listed.length).toBeGreaterThan(0);
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
        path: "test-results/live-proof-6419-folder-stage.png",
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "folder Remove from Staging dialog lists children and reports every row",
    { tag: ["@explorer", "@explorer-staging", "@explorer-folder-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged, unstaged } = await installFolderStageRoutes(page);
      await openFolder(page);
      const unstage = page.locator(
        '[data-testid="action-toolbar-item-Remove_from_Staging"]',
      );
      await expect(unstage).toBeVisible({ timeout: 15_000 });
      await unstage.click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-items"]'),
      ).toContainText(/Home/);
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      await expect(
        page.locator('[data-testid="explorer-bulk-stage-summary"]'),
      ).toContainText(/2 removed from staging, 1 skipped, 0 failed/i, {
        timeout: 10_000,
      });
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows).toHaveCount(3);
      await expect(rows.nth(0)).toContainText(/Home.*Removed from staging/i);
      await expect(rows.nth(1)).toContainText(/logo.*Removed from staging/i);
      await expect(rows.nth(2)).toContainText(/Nested/i);
      await expect.poll(() => unstaged.length).toBe(2);
      expect(
        unstaged.some((url) => url.includes("/takedown/page/staging/42")),
      ).toBe(true);
      expect(
        unstaged.some((url) => url.includes("/takedown/resource/staging/99")),
      ).toBe(true);
      expect(staged).toEqual([]);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "folder dialog cancel stages nothing",
    { tag: ["@explorer", "@explorer-staging", "@explorer-folder-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged, unstaged, listed } = await installFolderStageRoutes(page);
      await openFolder(page);
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
      // The dialog confirm list needs the children up front (#6530).
      expect(listed.some((url) => url.includes("News"))).toBe(true);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 403 on a folder child is reported per row in the dialog",
    { tag: ["@explorer", "@explorer-staging", "@explorer-folder-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { staged } = await installFolderStageRoutes(page, { homeStatus: 403 });
      await openFolder(page);
      await page.locator('[data-testid="action-toolbar-item-Stage"]').click();
      const dialog = page.locator('[data-testid="explorer-bulk-stage-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="explorer-bulk-stage-confirm"]').click();
      const summary = page.locator(
        '[data-testid="explorer-bulk-stage-summary"]',
      );
      await expect(summary).toContainText(/1 staged, 1 skipped, 1 failed/i, {
        timeout: 10_000,
      });
      const rows = page.locator(
        '[data-testid="explorer-bulk-stage-report-row"]',
      );
      await expect(rows.nth(0)).toContainText(/Home.*HTTP 403/i);
      await expect(rows.nth(1)).toContainText(/logo.*Staged/i);
      await expect.poll(() => staged.length).toBe(2);
      expect(staged.some((url) => url.includes("/page/staging/42"))).toBe(true);
      expect(staged.some((url) => url.includes("/resource/staging/99"))).toBe(true);
      await page.locator('[data-testid="explorer-bulk-stage-close"]').click();
      await expect(dialog).toHaveCount(0);
      // One child staged, so the list refreshes.
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
