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
 * Explorer generic items stay publish-gated (#6580 / parent #4530).
 *
 * Custom-type rows that live outside /Sites and /Assets (no page/asset
 * token) deliberately stay {@code "none"} from {@code resolvePublishKind}:
 * the demand endpoints are page|resource addressed, so there is no generic
 * publish contract server-side. The surface must therefore hide the publish
 * family for those rows and report them as skipped-other (never silently
 * dropped) when a folder publishes around them.
 *
 * Route-mocked like {@code explorer-folder-publish-now.spec.js}: the mocked
 * child row pins the non-Sites/Assets branch with an outside path.
 *
 * Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-generic-publish-gated.spec.js}
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
      // Generic custom-type row outside /Sites and /Assets (#6580).
      id: "g-9",
      name: "PromoSnippet",
      path: "/ContentLib/PromoSnippet",
      type: "MyCustomArticle",
      category: "item",
      leaf: true,
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
 * @param {{ children?: object }} [opts]
 */
async function installGenericGatedRoutes(page, opts = {}) {
  const children = opts.children ?? CHILDREN;
  /** @type {string[]} */
  const published = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/pathmanagement/path/folder/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(children),
    });
  });
  await page.route("**/services/sitemanage/publish/page/**", async (route) => {
    published.push(route.request().url());
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  });
  await page.route(
    "**/services/sitemanage/publish/resource/**",
    async (route) => {
      published.push(route.request().url());
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "{}",
      });
    },
  );
  return { published };
}

async function openFolder(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(
    page.locator('[data-testid="content-explorer-shell"]'),
  ).toBeVisible({ timeout: 20_000 });
  const folder = page.locator(
    '[data-testid="detail-row-7"][data-row-kind="folder"]',
  );
  await expect(folder).toBeVisible({ timeout: 20_000 });
  await folder.click();
  await expect(
    page.locator(
      '[data-testid="content-explorer-shell"][data-selected-item-id="7"]',
    ),
  ).toBeVisible({ timeout: 10_000 });
}

test.describe("Explorer generic items stay publish-gated (#6580)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "generic row hides the publish family but stays keyboard-focusable",
    { tag: ["@explorer", "@explorer-publish", "@explorer-generic-gated"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installGenericGatedRoutes(page);
      await openFolder(page);

      const genericRow = page.locator('[data-testid="detail-row-g-9"]');
      await expect(genericRow).toBeVisible({ timeout: 20_000 });
      await expect(genericRow).toHaveAttribute("data-row-kind", "item");
      await genericRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="g-9"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      // The publish family stays hidden for the generic selection.
      await expect(
        page.locator(
          '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
        ),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="action-toolbar-item-Take_Down"]'),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="action-toolbar-item-Schedule"]'),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="action-toolbar-item-Publishing_History"]'),
      ).toHaveCount(0);

      // Keyboard parity: the generic row stays in the roving tabindex.
      await genericRow.focus();
      await expect(genericRow).toHaveAttribute("tabindex", /^(-1|0)$/);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "folder publish confirm scopes other types and skips the generic row honestly",
    { tag: ["@explorer", "@explorer-publish", "@explorer-generic-gated"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(
          /Other item types and nested folders are not included/i,
        );
        void dialog.accept();
      });
      const { published } = await installGenericGatedRoutes(page);
      await openFolder(page);
      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();
      const banner = page.locator(
        '[data-testid="explorer-server-actions-error"]',
      );
      await expect(banner).toContainText(/PromoSnippet/i, { timeout: 10_000 });
      await expect(banner).toContainText(/not pages or assets/i);
      await expect.poll(() => published.length).toBe(1);
      expect(published.some((url) => url.includes("/publish/page/42"))).toBe(
        true,
      );
      expect(published.some((url) => url.includes("g-9"))).toBe(false);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "empty folder publish keeps the no-pages-or-assets empty state",
    { tag: ["@explorer", "@explorer-publish", "@explorer-generic-gated"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await installGenericGatedRoutes(page, { children: { PathItem: [] } });
      await openFolder(page);
      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/no pages or assets to publish/i, { timeout: 10_000 });
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
