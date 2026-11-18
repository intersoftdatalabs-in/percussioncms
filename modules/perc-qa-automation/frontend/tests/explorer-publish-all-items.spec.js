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
 * Explorer publish submits Pages, Assets, and generic Items alike
 * (#6580 reversal / parent #4530).
 *
 * Owner direction: Folder Publish Now submits EVERY child with an id —
 * like the ondemand/publish-now generator, which takes any numeric
 * content ids — and a single custom-type row is never hidden. Generic
 * Items ride the page endpoint; the engine accepts or rejects each row
 * with a named error instead of a pre-skip. There is no skipped-other
 * separation anywhere in the flow.
 *
 * Route-mocked like {@code explorer-folder-publish-now.spec.js}: the mocked
 * child row pins a non-Sites/Assets generic with an outside path.
 *
 * Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-publish-all-items.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } =
  require("./helpers/auth");
const { isKnownExplorerSitesConsoleNoise } = require("./helpers/explorer-copy-item");
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

const CHILDREN_LISTING = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/News/Home",
        type: "percPage",
        category: "page",
        leaf: true,
      },
      {
        // Generic custom-type row outside /Sites and /Assets (#6580).
        // First-class item: submitted via the page endpoint like pages.
        id: "9001999",
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
    childrenCount: 3,
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
      id: "9001999",
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
 * Track uncaught page errors (never filtered) and console errors (known
 * Explorer Sites resource noise filtered, same precedent as
 * explorer-generic-content-items.spec.js) for the C5 zero-noise assertion.
 *
 * @param {import('@playwright/test').Page} page
 * @returns {{ pageErrors: string[], consoleErrors: string[] }}
 */
function trackJsErrors(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") consoleErrors.push(msg.text());
  });
  return { pageErrors, consoleErrors };
}

/**
 * @param {string[]} consoleErrors
 * @returns {string[]}
 */
function relatedConsoleErrors(consoleErrors) {
  return consoleErrors.filter(
    (text) => !isKnownExplorerSitesConsoleNoise(text),
  );
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ children?: object, genericStatus?: number }} [opts]
 */
async function installAllItemsRoutes(page, opts = {}) {
  const children = opts.children ?? CHILDREN;
  const genericStatus = opts.genericStatus ?? 200;
  /** @type {string[]} */
  const published = [];
  // Item-scoped panel chrome is not the submit SUT: keep the checkout-owner
  // panel and the Change-Template menu quiet for mocked rows so the live
  // cell logs no fixture-artifact ERRORs (real generic rows carry numeric
  // Rhythmyx ids that resolve server-side).
  await page.route("**/editor/items/*/checkout-owner**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  });
  await page.route("**/actions/find/templates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ActionMenuList: { ActionMenu: [] } }),
    });
  });
  await page.route("**/itemmanagement/workflow/getTransitions/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ItemStateTransition: { transitionTriggers: [] } }),
    });
  });
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {    const url = route.request().url();
    const inNews =
      url.includes(encodeURIComponent("/Sites/Demo/News")) ||
      url.includes("/Sites/Demo/News");
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(inNews ? CHILDREN_LISTING : LISTING),
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
    const url = route.request().url();
    published.push(url);
    if (url.includes("9001999")) {
      await route.fulfill({ status: genericStatus, body: genericStatus === 200 ? "{}" : "denied" });
      return;
    }
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

test.describe("Explorer publish submits all items (#6580 reversal)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "generic row shows the publish family and publishes like any item",
    { tags: ["@explorer", "@explorer-publish", "@explorer-all-items"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = trackJsErrors(page);
      const dialogs = [];
      page.on("dialog", (dialog) => {
        dialogs.push(dialog.message());
        void dialog.accept();
      });
      const { published } = await installAllItemsRoutes(page);
      await openFolder(page);

      // Open the folder so the detail list shows its children (#6514).
      await page.locator('[data-testid="action-open"]').click();
      await page.waitForLoadState("networkidle").catch(() => undefined);
      const genericRow = page.locator('[data-testid="detail-row-9001999"]');
      await expect(genericRow).toBeVisible({ timeout: 20_000 });
      await expect(genericRow).toHaveAttribute("data-row-kind", "item");
      await genericRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="9001999"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      // The publish family is visible for the generic selection — nothing
      // stays hidden for a custom-type row.
      const publishNowVisible = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await expect(publishNowVisible.first()).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="action-toolbar-item-Take_Down"]'),
      ).toHaveCount(1);
      await expect(
        page.locator('[data-testid="action-toolbar-item-Schedule"]'),
      ).toHaveCount(1);
      await expect(
        page.locator('[data-testid="action-toolbar-item-Publishing_History"]'),
      ).toHaveCount(1);

      // Single generic Publish Now submits via the page endpoint.
      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await publishNow.first().click();
      await expect.poll(() => published.length).toBe(1);
      expect(
        published.some((url) => url.includes("/publish/page/9001999")),
      ).toBe(true);

      // Keyboard parity: the generic row stays in the roving tabindex.
      await genericRow.focus();
      await expect(genericRow).toHaveAttribute("tabindex", /^(-1|0)$/);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      const relatedConsole = relatedConsoleErrors(consoleErrors);
      expect(
        [...pageErrors, ...relatedConsole],
        [...pageErrors, ...relatedConsole].join(" | "),
      ).toEqual([]);
    },
  );

  test(
    "folder publish submits page and generic children with no type segregation",
    { tags: ["@explorer", "@explorer-publish", "@explorer-all-items"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = trackJsErrors(page);
      const dialogs = [];
      page.on("dialog", (dialog) => {
        dialogs.push(dialog.message());
        void dialog.accept();
      });
      const { published } = await installAllItemsRoutes(page);
      await openFolder(page);
      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();
      // Folder confirm is type-neutral: items publish, nested folders do not.
      expect(dialogs.some((text) => /Publish the items in this folder now/i.test(text))).toBe(
        true,
      );
      expect(dialogs.some((text) => /Other item types/i.test(text))).toBe(
        false,
      );
      await expect.poll(() => published.length).toBe(2);
      expect(published.some((url) => url.includes("/publish/page/42"))).toBe(
        true,
      );
      expect(
        published.some((url) => url.includes("/publish/page/9001999")),
      ).toBe(true);
      const banner = page.locator(
        '[data-testid="explorer-server-actions-error"]',
      );
      // Only the nested folder is skipped — never a second-class row.
      await expect(banner).toContainText(/Nested/i, { timeout: 10_000 });
      await expect(banner).not.toContainText(/not pages or assets/i);
      await expect(banner).not.toContainText(/PromoSnippet/i);
      await banner.screenshot({
        path: "test-results/live-proof-6580-publish-all-items.png",
      });
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      const relatedConsole = relatedConsoleErrors(consoleErrors);
      expect(
        [...pageErrors, ...relatedConsole],
        [...pageErrors, ...relatedConsole].join(" | "),
      ).toEqual([]);
    },
  );

  test(
    "engine rejection of a generic row is a named failure, not a pre-skip",
    { tags: ["@explorer", "@explorer-publish", "@explorer-all-items"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = trackJsErrors(page);
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const { published } = await installAllItemsRoutes(page, {
        genericStatus: 403,
      });
      await openFolder(page);
      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();
      await expect.poll(() => published.length).toBe(2);
      const banner = page.locator(
        '[data-testid="explorer-server-actions-error"]',
      );
      await expect(banner).toContainText(/PromoSnippet \(HTTP 403\)/i, {
        timeout: 10_000,
      });
      await expect(banner).toContainText(/Not every selected item was published/i);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      const relatedConsole = relatedConsoleErrors(consoleErrors);
      expect(
        [...pageErrors, ...relatedConsole],
        [...pageErrors, ...relatedConsole].join(" | "),
      ).toEqual([]);
    },
  );

  test(
    "empty folder publish keeps the type-neutral empty state",
    { tags: ["@explorer", "@explorer-publish", "@explorer-all-items"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = trackJsErrors(page);
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await installAllItemsRoutes(page, { children: { PathItem: [] } });
      await openFolder(page);
      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/no items to publish/i, { timeout: 10_000 });
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      const relatedConsole = relatedConsoleErrors(consoleErrors);
      expect(
        [...pageErrors, ...relatedConsole],
        [...pageErrors, ...relatedConsole].join(" | "),
      ).toEqual([]);
    },
  );
});
