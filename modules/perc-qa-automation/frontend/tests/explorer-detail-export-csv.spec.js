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
 * Explorer detail-list export-current-view to CSV (#6079 / parent #4530).
 *
 * <p>The Export CSV button above the detail list downloads the displayed
 * rows in display order with the visible columns only. The file is built
 * client-side from the already-loaded page: exporting sends no server
 * request and covers the loaded page only.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-detail-export-csv.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const fs = require("fs");
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
        name: 'apple, "crisp"',
        path: '/Sites/Demo/apple, "crisp"',
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
async function installExportRoutes(page) {
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
 * Click Export CSV and read the downloaded file as normalized text.
 * @param {import('@playwright/test').Page} page
 */
async function downloadCurrentView(page) {
  const downloadEvent = page.waitForEvent("download");
  await page.locator('[data-testid="detail-export-csv"]').click();
  const download = await downloadEvent;
  expect(download.suggestedFilename()).toMatch(/^explorer-.*\.csv$/);
  const path = await download.path();
  expect(path).toBeTruthy();
  return fs.readFileSync(path, "utf8").replace(/\r\n/g, "\n");
}

test.describe("Explorer detail-list export current view to CSV", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "exports the displayed rows with default headers and RFC 4180 quoting",
    {
      tag: ["@explorer", "@explorer-detail-export-csv"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const routes = await installExportRoutes(page);
      await openListing(page);

      const before = routes.listingHits();
      const body = await downloadCurrentView(page);
      expect(body).toBe(
        "Name,Type,Path\n" +
          "Cherry,percPage,/Sites/Demo/Cherry\n" +
          '"apple, ""crisp""",percPage,"/Sites/Demo/apple, ""crisp"""\n' +
          "Banana,percPage,/Sites/Demo/Banana\n",
      );

      // Client-side only: exporting issues no listing request.
      expect(routes.listingHits()).toBe(before);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "exports rows in the active sort order",
    {
      tag: ["@explorer", "@explorer-detail-export-csv"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installExportRoutes(page);
      await openListing(page);

      await page.locator('[data-testid="detail-col-sort-name"]').click();
      await expect(
        page.locator('[data-testid="detail-col-header-name"]'),
      ).toHaveAttribute("aria-sort", "ascending");

      const body = await downloadCurrentView(page);
      expect(body).toBe(
        "Name,Type,Path\n" +
          '"apple, ""crisp""",percPage,"/Sites/Demo/apple, ""crisp"""\n' +
          "Banana,percPage,/Sites/Demo/Banana\n" +
          "Cherry,percPage,/Sites/Demo/Cherry\n",
      );
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
