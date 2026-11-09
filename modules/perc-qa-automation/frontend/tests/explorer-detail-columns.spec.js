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
 * Explorer detail-list column visibility toggle (#5881 / parent #4530).
 *
 * <p>A Columns menu toggles the resolved display-format columns per
 * operator, persisted client-side in {@code localStorage} with zero new
 * REST traffic. At least one column always stays visible (the last
 * visible checkbox locks on); selection and arrow-key row navigation
 * keep working with hidden columns.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-detail-columns.spec.js}
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
        id: "c-3",
        name: "Cherry",
        path: "/Sites/Demo/Cherry",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "a-1",
        name: "apple",
        path: "/Sites/Demo/apple",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "b-2",
        name: "Banana",
        path: "/Sites/Demo/Banana",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 3,
    startIndex: 0,
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<{ listingHits: () => number }>}
 */
async function installColumnsRoutes(page) {
  let listingHits = 0;
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    listingHits += 1;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  return { listingHits: () => listingHits };
}

async function openListing(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="detail-row-c-3"]')).toBeVisible({
    timeout: 20_000,
  });
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function focusedRowTestId(page) {
  return page.evaluate(() => {
    const el = document.activeElement;
    return el ? el.getAttribute("data-testid") : null;
  });
}

test.describe("Explorer detail-list column visibility", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "unchecking a column hides its header and cells and persists the choice",
    {
      tag: ["@explorer", "@explorer-detail-columns"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const routes = await installColumnsRoutes(page);
      await openListing(page);

      await expect(page.locator('[data-testid="detail-col-header-type"]')).toBeVisible();
      await page.locator('[data-testid="detail-columns-toggle"]').click();
      await expect(page.locator('[data-testid="detail-columns-menu"]')).toBeVisible();

      await page.locator('[data-testid="detail-column-toggle-type"]').click();
      await expect(
        page.locator('[data-testid="detail-col-header-type"]'),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="detail-cell-type-c-3"]'),
      ).toHaveCount(0);
      // Name + path stay visible.
      await expect(page.locator('[data-testid="detail-col-header-name"]')).toBeVisible();
      await expect(page.locator('[data-testid="detail-col-header-path"]')).toBeVisible();

      // Persisted per operator in localStorage.
      const stored = await page.evaluate(() =>
        window.localStorage.getItem("perc.explorer.detailColumns.hidden.v1"),
      );
      expect(JSON.parse(stored)).toEqual(["type"]);

      // Fresh page load restores the hidden column (localStorage is
      // origin-scoped, so re-navigation keeps it; a bare reload drops
      // the shell's selected folder and renders no listing).
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="detail-row-c-3"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="detail-col-header-type"]'),
      ).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-col-header-name"]')).toBeVisible();

      // Client-side only: one fetch per page load, no visibility traffic.
      expect(routes.listingHits()).toBe(2);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "the last visible column locks on and keyboard nav keeps working",
    {
      tag: ["@explorer", "@explorer-detail-columns"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installColumnsRoutes(page);
      await openListing(page);

      await page.locator('[data-testid="detail-columns-toggle"]').click();
      await page.locator('[data-testid="detail-column-toggle-type"]').click();
      await page.locator('[data-testid="detail-column-toggle-path"]').click();
      await expect(
        page.locator('[data-testid="detail-col-header-path"]'),
      ).toHaveCount(0);

      // Only name remains: its checkbox is disabled and stays checked.
      const nameToggle = page.locator('[data-testid="detail-column-toggle-name"]');
      await expect(nameToggle).toBeDisabled();
      await expect(nameToggle).toBeChecked();
      await nameToggle.click({ force: true });
      await expect(page.locator('[data-testid="detail-col-header-name"]')).toBeVisible();
      await expect(page.locator('[data-testid="detail-cell-name-c-3"]')).toBeVisible();

      // Arrow-key row navigation still moves focus and selection.
      await page.locator('[data-testid="detail-row-c-3"]').focus();
      await page.keyboard.press("ArrowDown");
      expect(await focusedRowTestId(page)).toBe("detail-row-a-1");
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="a-1"]'),
      ).toBeVisible({ timeout: 10_000 });

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
