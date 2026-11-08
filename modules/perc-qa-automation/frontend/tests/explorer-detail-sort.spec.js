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
 * Explorer detail-list column sorting (#5759 / parent #4530).
 *
 * <p>Column headers re-sort the loaded page client-side: the first click
 * sorts ascending, the second toggles descending, and the active header
 * carries {@code aria-sort}. Sorting issues no new listing requests and
 * preserves the selected row.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-detail-sort.spec.js}
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
async function installSortRoutes(page) {
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

async function rowNames(page) {
  return page
    .locator('[data-testid="detail-list"] tbody tr[data-item-name]')
    .evaluateAll((rows) => rows.map((r) => r.getAttribute("data-item-name") ?? ""));
}

test.describe("Explorer detail-list column sorting", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "name header toggles ascending then descending with aria-sort",
    {
      tag: ["@explorer", "@explorer-detail-sort"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const routes = await installSortRoutes(page);
      await openListing(page);

      await expect.poll(() => rowNames(page)).toEqual(["Cherry", "apple", "Banana"]);
      await expect(page.locator('[data-testid="detail-col-header-name"]')).toHaveAttribute(
        "aria-sort",
        "none",
      );

      await page.locator('[data-testid="detail-col-sort-name"]').click();
      await expect(page.locator('[data-testid="detail-col-header-name"]')).toHaveAttribute(
        "aria-sort",
        "ascending",
      );
      await expect.poll(() => rowNames(page)).toEqual(["apple", "Banana", "Cherry"]);

      await page.locator('[data-testid="detail-col-sort-name"]').click();
      await expect(page.locator('[data-testid="detail-col-header-name"]')).toHaveAttribute(
        "aria-sort",
        "descending",
      );
      await expect.poll(() => rowNames(page)).toEqual(["Cherry", "Banana", "apple"]);

      // Client-side only: the folder listing was fetched exactly once.
      expect(routes.listingHits()).toBe(1);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "sorting preserves the selected row",
    {
      tag: ["@explorer", "@explorer-detail-sort"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installSortRoutes(page);
      await openListing(page);

      await page.locator('[data-testid="detail-row-b-2"]').click();
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="b-2"]'),
      ).toBeVisible({ timeout: 10_000 });

      await page.locator('[data-testid="detail-col-sort-name"]').click();
      await expect(page.locator('[data-testid="detail-col-header-name"]')).toHaveAttribute(
        "aria-sort",
        "ascending",
      );
      await expect.poll(() => rowNames(page)).toEqual(["apple", "Banana", "Cherry"]);
      await expect(page.locator('[data-testid="detail-row-b-2"]')).toHaveAttribute(
        "data-selected",
        "true",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="b-2"]'),
      ).toBeVisible({ timeout: 10_000 });

      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
