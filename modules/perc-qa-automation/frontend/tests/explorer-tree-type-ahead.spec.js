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
 * Explorer tree type-ahead (#5841 / parent #4530).
 *
 * <p>Classic Explorer jumps to a sibling folder when the operator types its
 * first letters. The React tree accumulates printable keys typed while the
 * tree is focused (~1s window) into a prefix, moves focus to the next
 * visible row whose label starts with the prefix (wrap-around), and
 * selection follows focus. Non-printable keys are ignored; a pause resets
 * the buffer; no match keeps the current selection. Client-side only —
 * zero new REST.</p>
 *
 * <p>Routes are fully mocked so the surface is deterministic on H2 qa-up:
 * folder roots Sites/Assets, Sites child Foo, Foo children Holly/Birch/
 * Cedar (nested three-segment paths, so the shell keeps the tree selection
 * instead of resolving a site Pages-chrome browse path like #3696), and an
 * empty detail-list page to keep the shell quiet.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-tree-type-ahead.spec.js}
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

function focusedTreeItem(page, path) {
  return page.locator(`[data-testid="tree-node-${path}"] [role="treeitem"]`).first();
}

test.describe("Explorer tree type-ahead", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "typing a visible prefix moves focus and selection with wrap-around",
    {
      tag: ["@explorer", "@explorer-tree-type-ahead"],
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

      // Focus Holly, type the multi-char prefix "bi": rapid keys
      // accumulate, focus jumps to Birch and selection follows.
      await focusedTreeItem(page, "/Sites/Foo/Holly").focus();
      await page.keyboard.type("bi");
      await expect(selectedTreeItem(page, "/Sites/Foo/Birch")).toBeVisible({ timeout: 10_000 });
      await expect(focusedTreeItem(page, "/Sites/Foo/Birch")).toBeFocused();

      // After a pause (fresh buffer), "c" moves to Cedar.
      await page.waitForTimeout(1_300);
      await page.keyboard.type("c");
      await expect(selectedTreeItem(page, "/Sites/Foo/Cedar")).toBeVisible({ timeout: 10_000 });
      await expect(focusedTreeItem(page, "/Sites/Foo/Cedar")).toBeFocused();

      // Wrap-around past the end: after a pause, "h" wraps back to Holly.
      await page.waitForTimeout(1_300);
      await page.keyboard.type("h");
      await expect(selectedTreeItem(page, "/Sites/Foo/Holly")).toBeVisible({ timeout: 10_000 });
      await expect(focusedTreeItem(page, "/Sites/Foo/Holly")).toBeFocused();

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "pause resets the buffer and no-match keeps the selection",
    {
      tag: ["@explorer", "@explorer-tree-type-ahead"],
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

      await focusedTreeItem(page, "/Sites/Foo/Holly").focus();
      await page.keyboard.type("b");
      await expect(selectedTreeItem(page, "/Sites/Foo/Birch")).toBeVisible({ timeout: 10_000 });

      // Pause past the ~1s window: the next key starts a fresh buffer, so
      // "c" jumps to Cedar (a stale "bc" buffer would match nothing).
      await page.waitForTimeout(1_500);
      await page.keyboard.type("c");
      await expect(selectedTreeItem(page, "/Sites/Foo/Cedar")).toBeVisible({ timeout: 10_000 });

      // No-match prefix keeps the current selection and focus.
      await page.keyboard.type("z");
      await expect(selectedTreeItem(page, "/Sites/Foo/Cedar")).toBeVisible({ timeout: 10_000 });
      await expect(focusedTreeItem(page, "/Sites/Foo/Cedar")).toBeFocused();

      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );
});
