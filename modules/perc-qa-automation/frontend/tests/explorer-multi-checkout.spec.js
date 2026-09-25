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
 * Explorer multi-select Check Out (#4835 / parent #4530).
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-multi-checkout.spec.js}
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
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "43",
        name: "About",
        path: "/Sites/Demo/About",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
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
    childrenCount: 3,
    startIndex: 0,
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ aboutStatus?: number }} [opts]
 */
async function installCheckoutRoutes(page, opts = {}) {
  const aboutStatus = opts.aboutStatus ?? 200;
  /** @type {string[]} */
  const checkedOut = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/itemmanagement/workflow/checkOut/**", async (route) => {
    const url = route.request().url();
    checkedOut.push(url);
    const status = url.includes("/checkOut/43") ? aboutStatus : 200;
    await route.fulfill({
      status,
      contentType: "application/json",
      body:
        status === 200
          ? JSON.stringify({
              ItemUserInfo: { checkOutUser: "Admin", currentUser: "Admin" },
            })
          : "denied",
    });
  });
  return checkedOut;
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
  await page.locator('[data-testid="detail-select-42"]').check();
  await page.locator('[data-testid="detail-select-43"]').check();
  await page.locator('[data-testid="detail-select-7"]').check();
}

test.describe("Explorer multi-select Check Out", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "one confirm checks out selected pages and names skipped folders",
    { tag: ["@explorer", "@explorer-checkout", "@explorer-multi-checkout"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/Check out 2 selected items/i);
        void dialog.accept();
      });
      const checkedOut = await installCheckoutRoutes(page);
      await openListing(page);
      const checkout = page.locator('[data-testid="action-toolbar-item-Check_Out"]');
      await expect(checkout).toBeVisible({ timeout: 15_000 });
      await checkout.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/Folders are not checked out: News/i, { timeout: 10_000 });
      await expect.poll(() => checkedOut.length).toBe(2);
      expect(checkedOut.some((url) => url.includes("/checkOut/42"))).toBe(true);
      expect(checkedOut.some((url) => url.includes("/checkOut/43"))).toBe(true);
      expect(checkedOut.some((url) => url.includes("/checkOut/7"))).toBe(false);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "cancel checks out nothing",
    { tag: ["@explorer", "@explorer-checkout", "@explorer-multi-checkout"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.dismiss();
      });
      const checkedOut = await installCheckoutRoutes(page);
      await openListing(page);
      await page.locator('[data-testid="action-toolbar-item-Check_Out"]').click();
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(checkedOut).toEqual([]);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 409 on one item is not a full-batch success",
    { tag: ["@explorer", "@explorer-checkout", "@explorer-multi-checkout"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const checkedOut = await installCheckoutRoutes(page, { aboutStatus: 409 });
      await openListing(page);
      await page.locator('[data-testid="action-toolbar-item-Check_Out"]').click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/About \(HTTP 409\)/i);
      await expect(error).toContainText(/Not every selected item was checked out/i);
      await expect.poll(() => checkedOut.length).toBe(2);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
