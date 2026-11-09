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
 * Explorer single-item Take Down / unpublish (#5630 / parent #4530).
 *
 * <p>One selected page row + Take Down asks once, unpublishes that item,
 * and refreshes the row. Cancel takes nothing down. HTTP 400/403/404/409 names
 * the item and leaves the selection intact.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-takedown.spec.js}
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
    ],
    childrenCount: 2,
    startIndex: 0,
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ homeStatus?: number }} [opts]
 */
async function installTakedownRoutes(page, opts = {}) {
  const homeStatus = opts.homeStatus ?? 200;
  /** @type {string[]} */
  const takenDown = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/itemmanagement/item/findLinkedItems/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ArrayList: [] }),
    });
  });
  const fulfill = async (route) => {
    const url = route.request().url();
    if (url.includes("/staging/")) {
      await route.fallback();
      return;
    }
    takenDown.push(url);
    if (url.includes("/takedown/page/42") || url.includes("/takedown/resource/42")) {
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
  await page.route("**/services/sitemanage/publish/takedown/page/**", fulfill);
  await page.route("**/services/sitemanage/publish/takedown/resource/**", fulfill);
  return takenDown;
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

test.describe("Explorer single-item Take Down", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "one confirm takes the selected page down and refreshes the row",
    {
      tag: ["@explorer", "@explorer-takedown", "@explorer-single-takedown"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/Take down \(unpublish\) this item/i);
        void dialog.accept();
      });
      const takenDown = await installTakedownRoutes(page);
      await openListing(page);
      const button = page.locator('[data-testid="action-toolbar-item-Take_Down"]');
      await expect(button).toBeVisible({ timeout: 15_000 });
      await button.click();
      await expect.poll(() => takenDown.length).toBe(1);
      expect(takenDown.some((url) => url.includes("/takedown/page/42"))).toBe(true);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "cancel takes nothing down",
    {
      tag: ["@explorer", "@explorer-takedown", "@explorer-single-takedown"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.dismiss();
      });
      const takenDown = await installTakedownRoutes(page);
      await openListing(page);
      await page.locator('[data-testid="action-toolbar-item-Take_Down"]').click();
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(takenDown).toEqual([]);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 403 names the item and keeps the selection",
    {
      tag: ["@explorer", "@explorer-takedown", "@explorer-single-takedown"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const takenDown = await installTakedownRoutes(page, { homeStatus: 403 });
      await openListing(page);
      await page.locator('[data-testid="action-toolbar-item-Take_Down"]').click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Home \(HTTP 403\)/i);
      await expect(error).toContainText(/Take down failed/i);
      await expect.poll(() => takenDown.length).toBe(1);
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
    "HTTP 400 names the item and keeps the selection",
    {
      tag: ["@explorer", "@explorer-takedown", "@explorer-single-takedown"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const takenDown = await installTakedownRoutes(page, { homeStatus: 400 });
      await openListing(page);
      await page.locator('[data-testid="action-toolbar-item-Take_Down"]').click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Home \(HTTP 400\)/i);
      await expect(error).toContainText(/Take down failed/i);
      await expect.poll(() => takenDown.length).toBe(1);
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
