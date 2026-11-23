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
 * Views catalog keyboard navigation (#7043 / parent #4530).
 *
 * <p>The Views catalog tree is a single Tab stop (roving tabindex):
 * ArrowUp/ArrowDown move focus and the selection together across the
 * visible rows (clamped at the ends per the WAI-ARIA APG tree pattern,
 * skipping collapsed subtrees); Home/End jump to the first/last row.
 * Moving onto a leaf runs the view like activating it; moving onto a
 * group row or the Views root moves focus only. Rows carry their tree
 * level and the focused row carries a {@code :focus-visible} ring.</p>
 *
 * <p>Routes are fully mocked so the surface is deterministic on H2
 * qa-up: folder roots Sites/Assets plus one nested Foo subtree for the
 * folder tree, an empty detail-list page, a four-group Views catalog,
 * and an empty view-execute result (arrowing onto a leaf runs the view
 * through the shell).</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-views-catalog-keyboard-nav.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const SITES = { id: "t-sites", path: "/Sites", name: "Sites", type: "folder", leaf: false, hasFolderChildren: true };
const ASSETS = { id: "t-assets", path: "/Assets", name: "Assets", type: "folder", leaf: false, hasFolderChildren: true };
const FOO = { id: "t-foo", path: "/Sites/Foo", name: "Foo", type: "folder", leaf: false, hasFolderChildren: false };

const EMPTY_PAGE = {
  PagedItemList: { childrenInPage: [], childrenCount: 0, startIndex: 0 },
};

const TREE_ROUTES = [
  { suffix: "/path/folder/Sites", items: [FOO] },
  { suffix: "/path/folder/Assets", items: [] },
];

const TREE_ITEMS = [
  { suffix: "/path/item/Sites/Foo", item: FOO },
  { suffix: "/path/item/Sites", item: SITES },
  { suffix: "/path/item/Assets", item: ASSETS },
];

const VIEWS_CATALOG = [
  { name: "MyPages", label: "My Pages", parentCategory: 1, standardView: true },
  { name: "Inbox", label: "Inbox", parentCategory: 1, customView: true },
  { name: "CommunityNews", label: "Community News", parentCategory: 2 },
  { name: "View_All", label: "All Content", parentCategory: 3, standardView: true },
];

const EMPTY_EXECUTE = { children: [], totalCount: 0, startIndex: 1 };

/**
 * @param {import('@playwright/test').Page} page
 */
async function installCatalogRoutes(page) {
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
  await page.route("**/services/views/*/execute", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(EMPTY_EXECUTE),
    });
  });
  await page.route("**/services/views", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(VIEWS_CATALOG),
    });
  });
}

async function openCatalog(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="explorer-views-tree"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="explorer-views-group-1"]')).toBeVisible({
    timeout: 20_000,
  });
}

function catalogTabStops(page) {
  return page.locator('[data-testid="explorer-views-tree"] [role="treeitem"][tabindex="0"]');
}

function catalogRow(page, testId) {
  return page.locator(`[data-testid="${testId}"]`).first();
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function focusedCatalogTestId(page) {
  return page.evaluate(() => {
    const el = document.activeElement;
    return el ? el.getAttribute("data-testid") : null;
  });
}

test.describe("Views catalog keyboard navigation", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "ArrowUp/ArrowDown step focus and selection, Home/End jump",
    {
      tag: ["@explorer", "@explorer-views-catalog-keyboard-nav"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const consoleErrors = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });
      await installCatalogRoutes(page);
      await openCatalog(page);

      // The catalog starts on exactly one tab stop (the Views root).
      await expect(catalogTabStops(page)).toHaveCount(1);
      await expect(catalogRow(page, "explorer-views-root-row")).toHaveAttribute("tabindex", "0");

      // ArrowDown steps root -> group 1 with focus but no selection:
      // groups expand on click/Enter, never on arrow moves.
      await catalogRow(page, "explorer-views-root-row").focus();
      await page.keyboard.press("ArrowDown");
      expect(await focusedCatalogTestId(page)).toBe("explorer-views-group-1-row");
      await expect(catalogTabStops(page)).toHaveCount(1);
      await expect(
        page.locator('[data-testid="explorer-views-tree"] [role="treeitem"][aria-selected="true"]'),
      ).toHaveCount(0);

      // ArrowDown steps onto the first group-1 leaf: focus and selection
      // move together (the leaf runs through the shell like a click).
      await page.keyboard.press("ArrowDown");
      const leafTestId = await focusedCatalogTestId(page);
      expect(String(leafTestId).startsWith("explorer-views-leaf-")).toBe(true);
      await expect(catalogTabStops(page)).toHaveCount(1);
      const selected = page.locator(
        '[data-testid="explorer-views-tree"] [role="treeitem"][aria-selected="true"]',
      );
      await expect(selected).toHaveCount(1);
      expect(await selected.getAttribute("data-testid")).toBe(leafTestId);

      // End jumps to the last visible row (group 4, closed), Home back to root.
      await page.keyboard.press("End");
      expect(await focusedCatalogTestId(page)).toBe("explorer-views-group-4-row");
      await page.keyboard.press("Home");
      expect(await focusedCatalogTestId(page)).toBe("explorer-views-root-row");

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-views-tree"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "arrow moves skip collapsed subtrees and clamp at the ends",
    {
      tag: ["@explorer", "@explorer-views-catalog-keyboard-nav"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const consoleErrors = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });
      await installCatalogRoutes(page);
      await openCatalog(page);

      // Group 2 is closed: ArrowDown from its row lands on the group 3
      // row, not on the CommunityNews leaf.
      await catalogRow(page, "explorer-views-group-2-row").focus();
      await page.keyboard.press("ArrowDown");
      expect(await focusedCatalogTestId(page)).toBe("explorer-views-group-3-row");

      // Clamp: ArrowDown on the last row stays on the last row (APG tree
      // pattern) instead of wrapping to the Views root.
      await page.keyboard.press("End");
      expect(await focusedCatalogTestId(page)).toBe("explorer-views-group-4-row");
      await page.keyboard.press("ArrowDown");
      expect(await focusedCatalogTestId(page)).toBe("explorer-views-group-4-row");

      // Clamp: ArrowUp on the first row stays on the first row.
      await page.keyboard.press("Home");
      expect(await focusedCatalogTestId(page)).toBe("explorer-views-root-row");
      await page.keyboard.press("ArrowUp");
      expect(await focusedCatalogTestId(page)).toBe("explorer-views-root-row");

      // Collapsing an ancestor of the focused leaf keeps exactly one tab
      // stop on a visible row: arrow down into group 1, collapse it with
      // the toggle, and the tab stop falls back to the group row instead
      // of stranding every row at tabindex="-1".
      await catalogRow(page, "explorer-views-root-row").focus();
      await page.keyboard.press("ArrowDown");
      await page.keyboard.press("ArrowDown");
      expect(String(await focusedCatalogTestId(page)).startsWith("explorer-views-leaf-")).toBe(true);
      await catalogRow(page, "explorer-views-group-1-row").click();
      await expect(catalogRow(page, "explorer-views-leaf-MyPages")).toBeHidden({ timeout: 10_000 });
      await expect(catalogTabStops(page)).toHaveCount(1);

      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "roving tabindex keeps one tab stop with tree levels and a visible focus ring",
    {
      tag: ["@explorer", "@explorer-views-catalog-keyboard-nav"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const consoleErrors = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });
      await installCatalogRoutes(page);
      await openCatalog(page);

      // Exactly one catalog row is in the tab order at a time (scoped to
      // the catalog: the folder tree owns its own treeitems elsewhere).
      await expect(catalogTabStops(page)).toHaveCount(1);
      // Rows carry their tree level and the focus-ring class.
      await expect(catalogRow(page, "explorer-views-root-row")).toHaveAttribute("aria-level", "1");
      await expect(catalogRow(page, "explorer-views-group-1-row")).toHaveAttribute("aria-level", "2");
      await expect(catalogRow(page, "explorer-views-leaf-MyPages")).toHaveAttribute("aria-level", "3");
      await expect(catalogRow(page, "explorer-views-leaf-MyPages")).toHaveClass(
        /perc-explorer-views-catalog-row/,
      );

      // The tab stop follows keyboard focus across an arrow move.
      await catalogRow(page, "explorer-views-root-row").focus();
      await page.keyboard.press("ArrowDown");
      await expect(catalogTabStops(page)).toHaveCount(1);
      expect(await catalogTabStops(page).getAttribute("data-testid")).toBe(
        "explorer-views-group-1-row",
      );

      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );
});
