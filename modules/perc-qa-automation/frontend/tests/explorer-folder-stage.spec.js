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
 * parent #4530).
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
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
    "one confirm stages pages and assets in the folder",
    { tag: ["@explorer", "@explorer-staging", "@explorer-folder-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/Stage the pages and assets in this folder/i);
        void dialog.accept();
      });
      const { staged, unstaged, listed } = await installFolderStageRoutes(page);
      await openFolder(page);
      const stage = page.locator('[data-testid="action-toolbar-item-Stage"]');
      await expect(stage).toBeVisible({ timeout: 15_000 });
      await stage.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/Folders are not staged: Nested/i, { timeout: 10_000 });
      await expect.poll(() => staged.length).toBe(2);
      expect(staged.some((url) => url.includes("/page/staging/42"))).toBe(true);
      expect(staged.some((url) => url.includes("/resource/staging/99"))).toBe(true);
      expect(unstaged).toEqual([]);
      expect(listed.length).toBeGreaterThan(0);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
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
    "one confirm removes pages and assets in the folder from staging",
    { tag: ["@explorer", "@explorer-staging", "@explorer-folder-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(
          /Remove the pages and assets in this folder from staging/i,
        );
        void dialog.accept();
      });
      const { staged, unstaged } = await installFolderStageRoutes(page);
      await openFolder(page);
      const unstage = page.locator(
        '[data-testid="action-toolbar-item-Remove_from_Staging"]',
      );
      await expect(unstage).toBeVisible({ timeout: 15_000 });
      await unstage.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/Folders are not removed from staging: Nested/i, {
        timeout: 10_000,
      });
      await expect.poll(() => unstaged.length).toBe(2);
      expect(
        unstaged.some((url) => url.includes("/takedown/page/staging/42")),
      ).toBe(true);
      expect(
        unstaged.some((url) => url.includes("/takedown/resource/staging/99")),
      ).toBe(true);
      expect(staged).toEqual([]);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "cancel stages nothing",
    { tag: ["@explorer", "@explorer-staging", "@explorer-folder-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.dismiss();
      });
      const { staged, unstaged, listed } = await installFolderStageRoutes(page);
      await openFolder(page);
      await page.locator('[data-testid="action-toolbar-item-Stage"]').click();
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(staged).toEqual([]);
      expect(unstaged).toEqual([]);
      expect(listed.some((url) => url.includes("News"))).toBe(false);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 403 on a folder child names the item and does not refresh",
    { tag: ["@explorer", "@explorer-staging", "@explorer-folder-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const { staged } = await installFolderStageRoutes(page, { homeStatus: 403 });
      await openFolder(page);
      await page.locator('[data-testid="action-toolbar-item-Stage"]').click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Home \(HTTP 403\)/i);
      await expect(error).toContainText(/Not every selected item was staged/i);
      await expect.poll(() => staged.length).toBe(2);
      expect(staged.some((url) => url.includes("/page/staging/42"))).toBe(true);
      expect(staged.some((url) => url.includes("/resource/staging/99"))).toBe(true);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
