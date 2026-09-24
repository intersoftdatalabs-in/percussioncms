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
 * Explorer multi-select Publish now (#4814 / parent #4530).
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-multi-publish-now.spec.js}
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
async function installPublishRoutes(page, opts = {}) {
  const aboutStatus = opts.aboutStatus ?? 200;
  /** @type {string[]} */
  const published = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  const fulfillPublish = async (route) => {
    const url = route.request().url();
    if (url.includes("/staging/")) {
      await route.continue();
      return;
    }
    published.push(url);
    if (url.includes("/publish/page/43")) {
      await route.fulfill({
        status: aboutStatus,
        contentType: "application/json",
        body: aboutStatus === 200 ? "{}" : "denied",
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  };
  await page.route("**/services/sitemanage/publish/page/**", fulfillPublish);
  await page.route("**/services/sitemanage/publish/resource/**", fulfillPublish);
  return published;
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

function publishNowButton(page) {
  return page.locator(
    '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
  );
}

test.describe("Explorer multi-select Publish now", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "one confirm publishes selected pages and names skipped folders",
    { tag: ["@explorer", "@explorer-publish", "@explorer-multi-publish-now"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/Publish 2 selected items/i);
        void dialog.accept();
      });
      const published = await installPublishRoutes(page);
      await openListing(page);
      const publishNow = publishNowButton(page);
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/Folders are not published: News/i, { timeout: 10_000 });
      await expect
        .poll(() => published.filter((url) => url.includes("/publish/")).length)
        .toBe(2);
      expect(published.some((url) => url.includes("/publish/page/42"))).toBe(true);
      expect(published.some((url) => url.includes("/publish/page/43"))).toBe(true);
      expect(published.some((url) => url.includes("/publish/page/7"))).toBe(false);
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
    "cancel publishes nothing",
    { tag: ["@explorer", "@explorer-publish", "@explorer-multi-publish-now"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.dismiss();
      });
      const published = await installPublishRoutes(page);
      await openListing(page);
      await publishNowButton(page).first().click();
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(published).toEqual([]);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 403 on one item is not a full-batch success",
    { tag: ["@explorer", "@explorer-publish", "@explorer-multi-publish-now"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const published = await installPublishRoutes(page, { aboutStatus: 403 });
      await openListing(page);
      await publishNowButton(page).first().click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/About \(HTTP 403\)/i);
      await expect(error).toContainText(/Not every selected item was published/i);
      await expect.poll(() => published.length).toBe(2);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
