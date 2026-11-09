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
 * Explorer detail-list arrow-key row navigation (#5761 / parent #4530)
 * plus client-page keys and full-folder row count (#5842).
 *
 * <p>ArrowDown/ArrowUp move focus and selection row-to-row (wrapping at
 * the ends); Home/End jump to the first/last row. PageUp/PageDown move
 * by one client page and clamp at the ends; the grid carries
 * {@code aria-rowcount} for the full folder size. Rows use roving
 * tabindex with {@code aria-rowindex} so screen-reader position stays
 * correct. Client-side only — the folder listing is fetched once.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-detail-keyboard.spec.js}
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
 * @param {number} [childrenCount]
 * @returns {Promise<{ listingHits: () => number }>}
 */
async function installKeyboardRoutes(page, childrenCount = 3) {
  let listingHits = 0;
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    listingHits += 1;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PagedItemList: {
          ...LISTING.PagedItemList,
          childrenCount,
        },
      }),
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

test.describe("Explorer detail-list arrow-key navigation", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "ArrowDown/ArrowUp move focus and selection with wrap",
    {
      tag: ["@explorer", "@explorer-detail-keyboard"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const routes = await installKeyboardRoutes(page);
      await openListing(page);

      // Roving tabindex: only the first row is a tab stop.
      await expect(page.locator('[data-testid="detail-row-c-3"]')).toHaveAttribute(
        "tabindex",
        "0",
      );
      await expect(page.locator('[data-testid="detail-row-a-1"]')).toHaveAttribute(
        "tabindex",
        "-1",
      );

      await page.locator('[data-testid="detail-row-c-3"]').focus();
      await page.keyboard.press("ArrowDown");
      expect(await focusedRowTestId(page)).toBe("detail-row-a-1");
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="a-1"]'),
      ).toBeVisible({ timeout: 10_000 });

      await page.keyboard.press("ArrowDown");
      expect(await focusedRowTestId(page)).toBe("detail-row-b-2");
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="b-2"]'),
      ).toBeVisible({ timeout: 10_000 });

      // Wraps from the last row back to the first.
      await page.keyboard.press("ArrowDown");
      expect(await focusedRowTestId(page)).toBe("detail-row-c-3");

      // ArrowUp from the first row wraps to the last.
      await page.keyboard.press("ArrowUp");
      expect(await focusedRowTestId(page)).toBe("detail-row-b-2");
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="b-2"]'),
      ).toBeVisible({ timeout: 10_000 });

      // Client-side only: the folder listing was fetched exactly once.
      expect(routes.listingHits()).toBe(1);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "Home/End jump to the first/last row and keep row positions",
    {
      tag: ["@explorer", "@explorer-detail-keyboard"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installKeyboardRoutes(page);
      await openListing(page);

      await page.locator('[data-testid="detail-row-a-1"]').focus();
      await page.keyboard.press("End");
      expect(await focusedRowTestId(page)).toBe("detail-row-b-2");
      await page.keyboard.press("Home");
      expect(await focusedRowTestId(page)).toBe("detail-row-c-3");

      await expect(page.locator('[data-testid="detail-row-c-3"]')).toHaveAttribute(
        "aria-rowindex",
        "1",
      );
      await expect(page.locator('[data-testid="detail-row-b-2"]')).toHaveAttribute(
        "aria-rowindex",
        "3",
      );

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "PageUp/PageDown move selection a client page, clamp at ends, announce row count",
    {
      tag: ["@explorer", "@explorer-detail-keyboard"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      // Folder holds 55 rows; only the first 3-row client page is loaded.
      const routes = await installKeyboardRoutes(page, 55);
      await openListing(page);

      // The grid announces the full folder size, not the loaded page.
      await expect(page.locator('[data-testid="detail-list"] table')).toHaveAttribute(
        "aria-rowcount",
        "55",
      );

      // Visible page is 3 rows: PageDown from the first row lands last.
      await page.locator('[data-testid="detail-row-c-3"]').focus();
      await page.keyboard.press("PageDown");
      expect(await focusedRowTestId(page)).toBe("detail-row-b-2");
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="b-2"]'),
      ).toBeVisible({ timeout: 10_000 });

      // A second PageDown clamps on the last row instead of wrapping.
      await page.keyboard.press("PageDown");
      expect(await focusedRowTestId(page)).toBe("detail-row-b-2");

      // PageUp returns to the first row and clamps there.
      await page.keyboard.press("PageUp");
      expect(await focusedRowTestId(page)).toBe("detail-row-c-3");
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="c-3"]'),
      ).toBeVisible({ timeout: 10_000 });
      await page.keyboard.press("PageUp");
      expect(await focusedRowTestId(page)).toBe("detail-row-c-3");

      // Client-side only: the folder listing was fetched exactly once.
      expect(routes.listingHits()).toBe(1);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
