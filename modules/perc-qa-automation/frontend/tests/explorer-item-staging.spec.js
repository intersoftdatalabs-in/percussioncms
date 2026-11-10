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
 * Explorer single-item Stage / Remove from Staging HTTP 400 (#5943 / parent #4530).
 *
 * <p>One selected page row + Stage (or Remove from Staging) with a malformed
 * request names the item ({@code Home (HTTP 400)}) in the Server actions
 * error region, keeps the selection, and does not refresh the list — the
 * same named-failure mapping as 403/404/409 (#5631). Confirm and success
 * behavior stay on {@code explorer-staging.spec.js}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-item-staging.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

const LISTING = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 1,
    startIndex: 0,
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ homeStatus?: number }} [opts]
 */
async function installItemStagingRoutes(page, opts = {}) {
  const homeStatus = opts.homeStatus ?? 400;
  /** @type {string[]} */
  const staged = [];
  /** @type {string[]} */
  const unstaged = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route(
    "**/services/sitemanage/publish/page/staging/**",
    async (route) => {
      const url = route.request().url();
      staged.push(url);
      if (url.includes("/staging/42")) {
        await route.fulfill({
          status: homeStatus,
          contentType: "application/json",
          body: homeStatus === 200 ? "{}" : "denied",
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "{}",
      });
    },
  );
  const unstage = async (route) => {
    const url = route.request().url();
    unstaged.push(url);
    if (url.includes("/staging/42")) {
      await route.fulfill({
        status: homeStatus,
        contentType: "application/json",
        body: homeStatus === 200 ? "{}" : "denied",
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  };
  await page.route(
    "**/services/sitemanage/publish/takedown/page/staging/**",
    unstage,
  );
  return { staged, unstaged };
}

async function openListing(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(home).toBeVisible({ timeout: 20_000 });
  await home.click();
  await expect(
    page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
  ).toBeVisible({ timeout: 10_000 });
}

test.describe("Explorer single-item Stage HTTP 400", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "HTTP 400 on Stage names the item and keeps the selection",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const { staged } = await installItemStagingRoutes(page, { homeStatus: 400 });
      await openListing(page);
      await page.locator('[data-testid="action-toolbar-item-Stage"]').click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Home \(HTTP 400\)/i);
      await expect(error).toContainText(/Stage failed/i);
      await expect.poll(() => staged.length).toBe(1);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 400 on Remove from Staging names the item and keeps the selection",
    {
      tag: ["@explorer", "@explorer-staging", "@explorer-single-staging"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const { unstaged } = await installItemStagingRoutes(page, { homeStatus: 400 });
      await openListing(page);
      await page
        .locator('[data-testid="action-toolbar-item-Remove_from_Staging"]')
        .click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Home \(HTTP 400\)/i);
      await expect(error).toContainText(/Remove from staging failed/i);
      await expect.poll(() => unstaged.length).toBe(1);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
