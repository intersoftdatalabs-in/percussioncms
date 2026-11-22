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
 * Explorer tree keyboard navigation (#7035 / parent #4530).
 *
 * <p>ArrowUp/ArrowDown step DOM focus and selection together across the
 * visible rows (clamped at the ends per the WAI-ARIA APG tree pattern,
 * skipping collapsed subtrees); Home/End jump to the first/last visible
 * row. Roving tabindex keeps the whole tree on a single tab stop, and
 * the focused row carries a {@code :focus-visible} ring. Expand/collapse,
 * activation, type-ahead, and context-menu behavior are unchanged.</p>
 *
 * <p>Routes are fully mocked so the surface is deterministic on H2 qa-up:
 * folder roots Sites/Assets, Sites child Foo, Foo children Holly/Birch/
 * Cedar (nested three-segment paths, so the shell keeps the tree selection
 * instead of resolving a site Pages-chrome browse path like #3696), and an
 * empty detail-list page to keep the shell quiet.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-tree-keyboard-nav.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const SITES = { id: "t-sites", path: "/Sites", name: "Sites", type: "folder", leaf: false, hasFolderChildren: true };
const ASSETS = { id: "t-assets", path: "/Assets", name: "Assets", type: "folder", leaf: false, hasFolderChildren: true };
const FOO = { id: "t-foo", path: "/Sites/Foo", name: "Foo", type: "folder", leaf: false, hasFolderChildren: true };
const HOLLY = { id: "t-holly", path: "/Sites/Foo/Holly", name: "Holly", type: "folder", leaf: false, hasFolderChildren: false };
const BIRCH = { id: "t-birch", path: "/Sites/Foo/Birch", name: "Birch", type: "folder", leaf: false, hasFolderChildren: false };
const CEDAR = { id: "t-cedar", path: "/Sites/Foo/Cedar", name: "Cedar", type: "folder", leaf: false, hasFolderChildren: false };

const EMPTY_PAGE = {
  PagedItemList: { childrenInPage: [], childrenCount: 0, startIndex: 0 },
};

const TREE_ROUTES = [
  { suffix: "/path/folder/Sites/Foo", items: [HOLLY, BIRCH, CEDAR] },
  { suffix: "/path/folder/Sites", items: [FOO] },
  { suffix: "/path/folder/Assets", items: [] },
];

const TREE_ITEMS = [
  { suffix: "/path/item/Sites/Foo/Holly", item: HOLLY },
  { suffix: "/path/item/Sites/Foo/Birch", item: BIRCH },
  { suffix: "/path/item/Sites/Foo/Cedar", item: CEDAR },
  { suffix: "/path/item/Sites/Foo", item: FOO },
  { suffix: "/path/item/Sites", item: SITES },
  { suffix: "/path/item/Assets", item: ASSETS },
];

/**
 * @param {import('@playwright/test').Page} page
 */
async function installTreeRoutes(page) {
  await page.route("**/pathmanagement/path/folder**", async (route) => {
    const url = route.request().url();
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
  await page.route("**/pathmanagement/path/item/**", async (route) => {
    const url = route.request().url();
    const hit = TREE_ITEMS.find((r) => url.endsWith(r.suffix) || url.endsWith(`${r.suffix}/`));
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ PathItem: hit ? hit.item : {} }),
    });
  });
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

/**
 * Expand Sites one level and Foo one level so the Holly/Birch/Cedar
 * siblings are all visible.
 */
async function revealSiblings(page) {
  await page.locator('[data-testid="tree-toggle-/Sites"]').click();
  await expect(page.locator('[data-testid="tree-node-/Sites/Foo"]')).toBeVisible({ timeout: 10_000 });
  await page.locator('[data-testid="tree-toggle-/Sites/Foo"]').click();
  await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Cedar"]')).toBeVisible({ timeout: 10_000 });
}

function selectedTreeItem(page, path) {
  return page.locator(
    `[data-testid="tree-node-${path}"] [role="treeitem"][aria-selected="true"]`,
  );
}

function treeRow(page, path) {
  return page.locator(`[data-testid="tree-node-${path}"] [role="treeitem"]`).first();
}

test.describe("Explorer tree keyboard navigation", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "ArrowUp/ArrowDown step focus and selection, Home/End jump",
    {
      tag: ["@explorer", "@explorer-tree-keyboard-nav"],
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
      await revealSiblings(page);

      // ArrowDown steps Holly -> Birch with focus and selection together.
      await treeRow(page, "/Sites/Foo/Holly").focus();
      await page.keyboard.press("ArrowDown");
      await expect(selectedTreeItem(page, "/Sites/Foo/Birch")).toBeVisible({ timeout: 10_000 });
      await expect(treeRow(page, "/Sites/Foo/Birch")).toBeFocused();

      // ArrowUp steps back Birch -> Holly.
      await page.keyboard.press("ArrowUp");
      await expect(selectedTreeItem(page, "/Sites/Foo/Holly")).toBeVisible({ timeout: 10_000 });
      await expect(treeRow(page, "/Sites/Foo/Holly")).toBeFocused();

      // End jumps to the last visible row (Assets root), Home to Sites.
      await page.keyboard.press("End");
      await expect(selectedTreeItem(page, "/Assets")).toBeVisible({ timeout: 10_000 });
      await expect(treeRow(page, "/Assets")).toBeFocused();
      await page.keyboard.press("Home");
      await expect(selectedTreeItem(page, "/Sites")).toBeVisible({ timeout: 10_000 });
      await expect(treeRow(page, "/Sites")).toBeFocused();

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "arrow moves skip collapsed subtrees and clamp at the ends",
    {
      tag: ["@explorer", "@explorer-tree-keyboard-nav"],
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
      await revealSiblings(page);

      // Collapse Foo: Holly/Birch/Cedar leave the visible order, so
      // ArrowDown from Foo lands on Assets instead of Holly.
      await page.locator('[data-testid="tree-toggle-/Sites/Foo"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Cedar"]')).toBeHidden({ timeout: 10_000 });
      await treeRow(page, "/Sites/Foo").focus();
      await page.keyboard.press("ArrowDown");
      await expect(selectedTreeItem(page, "/Assets")).toBeVisible({ timeout: 10_000 });
      await expect(treeRow(page, "/Assets")).toBeFocused();

      // Clamp: ArrowDown on the last row stays on the last row (APG tree
      // pattern, #7045) instead of wrapping to Sites.
      await page.keyboard.press("ArrowDown");
      await expect(treeRow(page, "/Assets")).toBeFocused();
      await expect(selectedTreeItem(page, "/Assets")).toBeVisible({ timeout: 10_000 });

      // Clamp: ArrowUp on the first row stays on the first row.
      await page.keyboard.press("Home");
      await expect(treeRow(page, "/Sites")).toBeFocused();
      await page.keyboard.press("ArrowUp");
      await expect(treeRow(page, "/Sites")).toBeFocused();

      // Collapsing an ancestor of the focused row keeps exactly one tab
      // stop on a visible row (#7045): arrow down into Birch, collapse
      // Foo with the toggle, and the tab stop falls back to a rendered
      // row instead of stranding every row at tabindex="-1".
      await page.locator('[data-testid="tree-toggle-/Sites/Foo"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Cedar"]')).toBeVisible({ timeout: 10_000 });
      await treeRow(page, "/Sites/Foo/Holly").focus();
      await page.keyboard.press("ArrowDown");
      await expect(treeRow(page, "/Sites/Foo/Birch")).toBeFocused();
      await page.locator('[data-testid="tree-toggle-/Sites/Foo"]').click();
      await expect(page.locator('[data-testid="tree-node-/Sites/Foo/Birch"]')).toBeHidden({ timeout: 10_000 });
      await expect(page.locator('[data-testid="explorer-tree"] [role="treeitem"][tabindex="0"]')).toHaveCount(1);

      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "roving tabindex keeps one tab stop with a visible focus ring",
    {
      tag: ["@explorer", "@explorer-tree-keyboard-nav"],
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
      await revealSiblings(page);

      // Exactly one folder-tree row is in the tab order at a time
      // (scoped to the folder tree: the views catalog owns its own
      // treeitems outside this slice).
      await expect(page.locator('[data-testid="explorer-tree"] [role="treeitem"][tabindex="0"]')).toHaveCount(1);
      // Rows carry their tree level and the focus-ring class.
      await expect(treeRow(page, "/Sites/Foo/Holly")).toHaveAttribute("aria-level", "3");
      await expect(treeRow(page, "/Sites/Foo/Holly")).toHaveClass(/perc-explorer-tree-row/);

      // The tab stop follows keyboard focus across an arrow move.
      await treeRow(page, "/Sites/Foo/Holly").focus();
      await page.keyboard.press("ArrowDown");
      await expect(treeRow(page, "/Sites/Foo/Birch")).toBeFocused();
      await expect(page.locator('[data-testid="explorer-tree"] [role="treeitem"][tabindex="0"]')).toHaveCount(1);
      await expect(page.locator('[data-testid="explorer-tree"] [role="treeitem"][tabindex="0"]')).toHaveAttribute(
        "aria-level",
        "3",
      );

      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "external selection change moves the tab stop to the selected row",
    {
      tag: ["@explorer", "@explorer-tree-keyboard-nav"],
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
      await revealSiblings(page);

      // Arrow down Holly -> Birch: focus, selection, and the tab stop move
      // together onto Birch.
      await treeRow(page, "/Sites/Foo/Holly").focus();
      await page.keyboard.press("ArrowDown");
      await expect(selectedTreeItem(page, "/Sites/Foo/Birch")).toBeVisible({ timeout: 10_000 });
      await expect(treeRow(page, "/Sites/Foo/Birch")).toBeFocused();

      // External selection change via the breadcrumb: Birch is still
      // rendered, but the tab stop must move to the newly selected row
      // instead of staying on the previously arrowed row (#7045 review).
      await page.locator('[data-testid="explorer-breadcrumb-segment"][data-path="/Sites/Foo"]').click();
      await expect(selectedTreeItem(page, "/Sites/Foo")).toBeVisible({ timeout: 10_000 });
      const tabStops = page.locator('[data-testid="explorer-tree"] [role="treeitem"][tabindex="0"]');
      await expect(tabStops).toHaveCount(1);
      await expect(
        page.locator('[data-testid="tree-node-/Sites/Foo"] [role="treeitem"][tabindex="0"]'),
      ).toHaveCount(1);

      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );
});
