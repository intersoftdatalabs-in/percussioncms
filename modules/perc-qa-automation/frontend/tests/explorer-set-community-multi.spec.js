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
 * Explorer multi-select Set community (#5133 / parent #4530).
 *
 * <p>One community is written on each checked page and asset. The name is
 * shown on a row only after that item's save returns. Folders are not
 * written. HTTP 400/403/409 is not full success. Cancel does not POST.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-set-community-multi.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl, openContentMenu } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  isKnownExplorerSetCommunityConsoleNoise,
} = require("./helpers/explorer-set-community");

const TAGS = ["@explorer", "@explorer-set-community", "@explorer-set-community-multi"];

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
        path: "/Sites/Demo/News/",
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

const CATALOG = {
  ItemCommunityChoices: {
    itemId: "42",
    currentCommunityId: "10",
    choices: [
      { id: "10", name: "Default" },
      { id: "20", name: "Enterprise" },
    ],
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ aboutStatus?: number }} [opts]
 */
async function installCommunityRoutes(page, opts = {}) {
  const aboutStatus = opts.aboutStatus ?? 200;
  /** @type {string[]} */
  const posts = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/itemmanagement/item/community/allowed/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(CATALOG),
    });
  });
  await page.route("**/itemmanagement/item/community/change/**", async (route) => {
    const url = route.request().url();
    posts.push(url);
    const status = url.includes("/change/43/") ? aboutStatus : 200;
    await route.fulfill({
      status,
      contentType: "application/json",
      body:
        status === 200
          ? JSON.stringify({
              ItemCommunityChoices: {
                currentCommunityId: "20",
                choices: CATALOG.ItemCommunityChoices.choices,
              },
            })
          : "denied",
    });
  });
  // Synthetic rows are not real content. Other Explorer probes (workflow
  // transitions) must not hit the CMS and log a missing-item error.
  await page.route("**/services/**", async (route) => {
    const url = route.request().url();
    if (url.includes("/community/") || url.includes("/paginatedFolder")) {
      await route.fallback();
      return;
    }
    if (/\/(42|43|7)(?:\/|$|\?)/.test(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "{}",
      });
      return;
    }
    await route.fallback();
  });
  return posts;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function openCheckedListing(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(home).toBeVisible({ timeout: 20_000 });
  await page.locator('[data-testid="detail-select-42"]').check();
  await page.locator('[data-testid="detail-select-43"]').check();
  await page.locator('[data-testid="detail-select-7"]').check();
}

/**
 * @param {import('@playwright/test').Page} page
 */
function trackErrors(page) {
  /** @type {string[]} */
  const errors = [];
  page.on("pageerror", (err) => errors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerSetCommunityConsoleNoise(msg.text())) {
      errors.push(msg.text());
    }
  });
  return errors;
}

test.describe("Explorer multi-select Set community (#5133)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "save shows the community on each page only after that save, and skips folders",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const posts = await installCommunityRoutes(page);
      await openCheckedListing(page);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-community"]').click();
      const dialog = page.locator('[data-testid="explorer-set-community-dialog"]');
      await expect(dialog).toBeVisible();
      await expect(page.locator('[data-testid="explorer-set-community-multi"]')).toBeVisible();
      await expect(page.locator('[data-testid="explorer-set-community-status"]')).toHaveCount(0);
      await expect(page.locator('[data-testid^="detail-item-community-"]')).toHaveCount(0);
      await page.locator('[data-testid="explorer-set-community-select"]').selectOption("20");
      await page.locator('[data-testid="explorer-set-community-save"]').click();
      const status = page.locator('[data-testid="explorer-set-community-status"]');
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-community-id", "20");
      await expect(status).toHaveAttribute("data-community-name", "Enterprise");
      await expect(status).toContainText("Community saved");
      await expect(status).toContainText("Enterprise");
      await expect(status).toContainText("News");
      await expect(dialog).toHaveCount(0);
      const homeCommunity = page.locator('[data-testid="detail-item-community-42"]');
      const aboutCommunity = page.locator('[data-testid="detail-item-community-43"]');
      await expect(homeCommunity).toHaveAttribute("data-community-id", "20");
      await expect(homeCommunity).toContainText("Enterprise");
      await expect(aboutCommunity).toHaveAttribute("data-community-id", "20");
      await expect(aboutCommunity).toContainText("Enterprise");
      await expect(page.locator('[data-testid="detail-item-community-7"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-row-7"]')).not.toHaveAttribute(
        "data-community-id",
        "20",
      );
      expect(posts.some((url) => url.includes("/change/42/"))).toBe(true);
      expect(posts.some((url) => url.includes("/change/43/"))).toBe(true);
      expect(posts.some((url) => url.includes("/change/7/"))).toBe(false);
      expect(posts).toHaveLength(2);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "cancel does not call the server",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const posts = await installCommunityRoutes(page);
      await openCheckedListing(page);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-community"]').click();
      await expect(page.locator('[data-testid="explorer-set-community-dialog"]')).toBeVisible();
      await page.locator('[data-testid="explorer-set-community-cancel"]').click();
      await expect(page.locator('[data-testid="explorer-set-community-dialog"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-set-community-status"]')).toHaveCount(0);
      await expect(page.locator('[data-testid^="detail-item-community-"]')).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "HTTP 409 on one item does not claim the whole selection succeeded",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const posts = await installCommunityRoutes(page, { aboutStatus: 409 });
      await openCheckedListing(page);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-community"]').click();
      await page.locator('[data-testid="explorer-set-community-select"]').selectOption("20");
      await page.locator('[data-testid="explorer-set-community-save"]').click();
      const status = page.locator('[data-testid="explorer-set-community-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "partial");
      await expect(status).toHaveAttribute("data-community-id", "");
      await expect(status).toContainText("About (HTTP 409)");
      await expect(status).not.toContainText("Community saved");
      await expect(page.locator('[data-testid="detail-item-community-42"]')).toHaveAttribute(
        "data-community-id",
        "20",
      );
      await expect(page.locator('[data-testid="detail-item-community-43"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-item-community-7"]')).toHaveCount(0);
      expect(posts).toHaveLength(2);
      expect(posts.some((url) => url.includes("/change/7/"))).toBe(false);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );
});
