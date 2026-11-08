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
 * Explorer tree expand-all / collapse-all (#5760 / parent #4530).
 *
 * <p>The folder-tree toolbar offers Expand all / Collapse all buttons.
 * Expand-all reveals loaded descendants (unopened subtrees lazy-load one
 * level, never the whole hierarchy); collapse-all folds back to the roots.
 * Either action keeps the current folder and selection unchanged.</p>
 *
 * <p>Routes are fully mocked so the surface is deterministic on H2 qa-up:
 * folder roots Sites/Assets, Sites children Foo/Leaf, Foo child Bar, and
 * an empty detail-list page to keep the shell quiet.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-tree-expand-collapse.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const SITES = { id: "t-sites", path: "/Sites", name: "Sites", type: "folder", leaf: false, hasFolderChildren: true };
const ASSETS = { id: "t-assets", path: "/Assets", name: "Assets", type: "folder", leaf: false, hasFolderChildren: true };
const FOO = { id: "t-foo", path: "/Sites/Foo", name: "Foo", type: "folder", leaf: false, hasFolderChildren: true };
const LEAF = { id: "t-leaf", path: "/Sites/Leaf", name: "Leaf", type: "folder", leaf: false, hasFolderChildren: false };
const BAR = { id: "t-bar", path: "/Sites/Foo/Bar", name: "Bar", type: "folder", leaf: false, hasFolderChildren: false };

const EMPTY_PAGE = {
  PagedItemList: { childrenInPage: [], childrenCount: 0, startIndex: 0 },
};

/**
 * Mock folder-listing payloads keyed by request-URL suffix. Table-driven
 * so route dispatch stays flat (#5760).
 */
const TREE_ROUTES = [
  { suffix: "/path/folder/Sites/Foo/Bar", items: [] },
  { suffix: "/path/folder/Sites/Foo", items: [BAR] },
  { suffix: "/path/folder/Sites", items: [FOO, LEAF] },
  { suffix: "/path/folder/Assets", items: [] },
];

/**
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<{ folderHits: () => string[] }>}
 */
async function installTreeRoutes(page) {
  const folderHits = [];
  await page.route("**/pathmanagement/path/folder**", async (route) => {
    const url = route.request().url();
    folderHits.push(url);
    const hit = TREE_ROUTES.find((r) => url.endsWith(r.suffix) || url.endsWith(`${r.suffix}/`));
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ PathItem: hit ? hit.items : [SITES, ASSETS] }),
    });
  });
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(EMPTY_PAGE),
    });
  });
  return { folderHits: () => folderHits };
}

async function openExplorer(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="tree-node-/Sites"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Explorer tree expand-all / collapse-all", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "toolbar buttons exist, are named, and expand-all reveals loaded descendants",
    {
      tag: ["@explorer", "@explorer-tree-expand-collapse"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const consoleErrors = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });
      const routes = await installTreeRoutes(page);
      await openExplorer(page);

      await expect(page.locator('[data-testid="tree-expand-all"]')).toHaveAccessibleName(/expand all/i);
      await expect(page.locator('[data-testid="tree-collapse-all"]')).toHaveAccessibleName(/collapse all/i);

      // Open Sites one level so Foo/Leaf load, then Foo so Bar loads.
      await page.locator('[data-testid="tree-toggle-/Sites"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo"]')).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="tree-toggle-/Sites/Foo"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Bar"]')).toBeVisible({ timeout: 10_000 });
      const fooLoads = () =>
        routes.folderHits().filter((u) => /\/path\/folder\/Sites\/Foo\/?$/.test(u)).length;
      expect(fooLoads()).toBe(1);

      // Collapse-all folds back to the roots.
      await page.locator('[data-testid="tree-collapse-all"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Bar"]')).toBeHidden({ timeout: 10_000 });
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo"]')).toBeVisible();
      await expect(page.locator('[data-testid="tree-node-/Sites"]')).toBeVisible();
      await expect(page.locator('[data-testid="tree-node-/Assets"]')).toBeVisible();

      // Expand-all reveals the loaded descendant without refetching its
      // loaded parent (unopened folders may one-level lazy-load on demand).
      await page.locator('[data-testid="tree-expand-all"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Bar"]')).toBeVisible({ timeout: 10_000 });
      expect(fooLoads()).toBe(1);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "expand-all and collapse-all preserve the selected folder",
    {
      tag: ["@explorer", "@explorer-tree-expand-collapse"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const consoleErrors = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });
      await installTreeRoutes(page);
      await openExplorer(page);

      await page.locator('[data-testid="tree-toggle-/Sites"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo"]')).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="tree-toggle-/Sites/Foo"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Bar"]')).toBeVisible({ timeout: 10_000 });

      // Select Foo via its row (not the toggle).
      await page.locator('[data-testid="tree-node-/Sites/Foo"] [role="treeitem"]').first().click();
      await expect(
        page.locator('[data-testid="tree-node-/Sites/Foo"] [role="treeitem"][aria-selected="true"]'),
      ).toBeVisible({ timeout: 10_000 });

      await page.locator('[data-testid="tree-expand-all"]').click();
      await expect(
        page.locator('[data-testid="tree-node-/Sites/Foo"] [role="treeitem"][aria-selected="true"]'),
      ).toBeVisible({ timeout: 10_000 });

      await page.locator('[data-testid="tree-collapse-all"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Bar"]')).toBeHidden({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="tree-node-/Sites/Foo"] [role="treeitem"][aria-selected="true"]'),
      ).toBeVisible({ timeout: 10_000 });

      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );
});
