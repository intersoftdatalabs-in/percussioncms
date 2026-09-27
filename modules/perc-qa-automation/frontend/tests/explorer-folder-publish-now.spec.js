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
 * Explorer Publish Now for a selected folder (#4945 / parent #4530).
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-folder-publish-now.spec.js}
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
      id: "99",
      name: "logo",
      path: "/Assets/logo.png",
      type: "percImageAsset",
      category: "asset",
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
 * @param {{ homeBody?: string }} [opts]
 */
async function installFolderPublishRoutes(page, opts = {}) {
  const homeBody = opts.homeBody ?? "{}";
  /** @type {string[]} */
  const published = [];
  /** @type {string[]} */
  const listed = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/pathmanagement/path/folder/**", async (route) => {
    listed.push(route.request().url());
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(CHILDREN),
    });
  });
  const fulfillPublish = async (route) => {
    const url = route.request().url();
    if (url.includes("/staging/")) {
      await route.continue();
      return;
    }
    published.push(url);
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: url.includes("/publish/page/42") || homeBody !== "{}" ? homeBody : "{}",
    });
  };
  await page.route("**/services/sitemanage/publish/page/**", fulfillPublish);
  await page.route("**/services/sitemanage/publish/resource/**", fulfillPublish);
  return { published, listed };
}

async function openFolder(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const folder = page.locator('[data-testid="detail-row-7"][data-row-kind="folder"]');
  await expect(folder).toBeVisible({ timeout: 20_000 });
  await folder.click();
  await expect(
    page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="7"]'),
  ).toBeVisible({ timeout: 10_000 });
}

function publishNowButton(page) {
  return page.locator(
    '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
  );
}

test.describe("Explorer folder Publish now", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "one confirm publishes pages and assets in the folder",
    { tag: ["@explorer", "@explorer-publish", "@explorer-folder-publish"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/pages and assets in this folder/i);
        void dialog.accept();
      });
      const { published, listed } = await installFolderPublishRoutes(page);
      await openFolder(page);
      const publishNow = publishNowButton(page);
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/Folders are not published: Nested/i, { timeout: 10_000 });
      await expect.poll(() => published.length).toBe(2);
      expect(published.some((url) => url.includes("/publish/page/42"))).toBe(true);
      expect(published.some((url) => url.includes("/publish/resource/99"))).toBe(true);
      expect(listed.length).toBeGreaterThan(0);
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
    { tag: ["@explorer", "@explorer-publish", "@explorer-folder-publish"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.dismiss();
      });
      const { published, listed } = await installFolderPublishRoutes(page);
      await openFolder(page);
      await publishNowButton(page).first().click();
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(published).toEqual([]);
      expect(listed.some((url) => url.includes("News"))).toBe(false);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "FORBIDDEN preflight does not refresh as if the job started",
    { tag: ["@explorer", "@explorer-publish", "@explorer-folder-publish"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const { published } = await installFolderPublishRoutes(page, {
        homeBody: JSON.stringify({ status: "FORBIDDEN" }),
      });
      await openFolder(page);
      await publishNowButton(page).first().click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/FORBIDDEN/i);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(published.some((url) => url.includes("/publish/page/42"))).toBe(true);
      expect(published.some((url) => url.includes("/publish/resource/99"))).toBe(true);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
