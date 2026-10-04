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
 * Explorer multi-select Set folder community (#5156 / parent #4530).
 *
 * <p>One community is written on each checked folder. The name is shown on a
 * folder only after that folder's save returns and a properties refresh shows
 * the new id. Pages and assets are not written. HTTP 409 is not full success.
 * Cancel does not POST.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-set-folder-community-multi.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl, openContentMenu } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  isKnownExplorerSetFolderCommunityConsoleNoise,
} = require("./helpers/explorer-set-folder-community");

const TAGS = [
  "@explorer",
  "@explorer-set-folder-community",
  "@explorer-set-folder-community-multi",
];

const LISTING = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "101",
        name: "News",
        path: "/Sites/Demo/News/",
        type: "folder",
        category: "folder",
        accessLevel: "WRITE",
        leaf: false,
      },
      {
        id: "102",
        name: "Blog",
        path: "/Sites/Demo/Blog/",
        type: "folder",
        category: "folder",
        accessLevel: "WRITE",
        leaf: false,
      },
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
    childrenCount: 4,
    startIndex: 0,
  },
};

const CHOICES = [
  { id: "10", name: "Default" },
  { id: "12", name: "Enterprise" },
];

/**
 * @param {string} body
 * @returns {string}
 */
function postedFolderId(body) {
  try {
    const parsed = JSON.parse(body || "{}");
    const folder = parsed.FolderProperties || parsed.folderProperties || {};
    return String(folder.id ?? "").trim();
  } catch {
    return "";
  }
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ blogStatus?: number }} [opts]
 */
async function installFolderCommunityRoutes(page, opts = {}) {
  const blogStatus = opts.blogStatus ?? 200;
  /** @type {string[]} */
  const posts = [];
  /** @type {Record<string, { id: number, name: string }>} */
  const stored = {
    "101": { id: 10, name: "Default" },
    "102": { id: 10, name: "Default" },
  };
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/pathmanagement/path/folderCommunityCatalog**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        FolderCommunityCatalog: { choices: CHOICES },
      }),
    });
  });
  await page.route("**/pathmanagement/path/folderProperties/**", async (route) => {
    const url = route.request().url();
    const id = decodeURIComponent(url.split("/folderProperties/")[1] || "").split(/[?#]/)[0];
    const current = stored[id] || { id: 10, name: "Default" };
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        FolderProperties: {
          id,
          name: id === "102" ? "Blog" : "News",
          communityId: current.id,
          communityName: current.name,
          permission: { accessLevel: "ADMIN" },
        },
      }),
    });
  });
  await page.route("**/pathmanagement/path/saveFolderProperties**", async (route) => {
    const body = route.request().postData() || "";
    const id = postedFolderId(body);
    posts.push(id || body);
    const status = id === "102" ? blogStatus : 200;
    if (status === 200 && stored[id]) {
      stored[id] = { id: 12, name: "Enterprise" };
    }
    await route.fulfill({
      status,
      contentType: "application/json",
      body: status === 200 ? "{}" : JSON.stringify({ error: status }),
    });
  });
  await page.route("**/services/**", async (route) => {
    const url = route.request().url();
    if (
      url.includes("/folderCommunityCatalog") ||
      url.includes("/folderProperties") ||
      url.includes("/saveFolderProperties") ||
      url.includes("/paginatedFolder")
    ) {
      await route.fallback();
      return;
    }
    if (/\/(101|102|42|43)(?:\/|$|\?)/.test(url)) {
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
 * @param {string[]} ids
 */
async function openCheckedListing(page, ids) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="detail-row-101"][data-row-kind="folder"]')).toBeVisible({
    timeout: 20_000,
  });
  for (const id of ids) {
    await page.locator(`[data-testid="detail-select-${id}"]`).check();
  }
}

/**
 * @param {import('@playwright/test').Page} page
 */
function trackErrors(page) {
  /** @type {string[]} */
  const errors = [];
  page.on("pageerror", (err) => errors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerSetFolderCommunityConsoleNoise(msg.text())) {
      errors.push(msg.text());
    }
  });
  return errors;
}

test.describe("Explorer multi-select Set folder community (#5156)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "save shows the community on each folder only after that save, and skips pages",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const posts = await installFolderCommunityRoutes(page);
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-community"]').click();
      const dialog = page.locator('[data-testid="explorer-set-folder-community-dialog"]');
      await expect(dialog).toBeVisible();
      await expect(page.locator('[data-testid="explorer-set-folder-community-multi"]')).toBeVisible();
      await expect(page.locator('[data-testid="explorer-set-folder-community-status"]')).toHaveCount(0);
      await expect(page.locator('[data-testid^="detail-folder-community-"]')).toHaveCount(0);
      await page.locator('[data-testid="explorer-set-folder-community-select"]').selectOption("12");
      await page.locator('[data-testid="explorer-set-folder-community-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-community-status"]');
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-reason", "items-skipped");
      await expect(status).toHaveAttribute("data-community-id", "12");
      await expect(status).toHaveAttribute("data-community-name", "Enterprise");
      await expect(status).toContainText("Folder community saved");
      await expect(status).toContainText("Enterprise");
      await expect(status).toContainText("Home");
      await expect(dialog).toHaveCount(0);
      const news = page.locator('[data-testid="detail-folder-community-101"]');
      const blog = page.locator('[data-testid="detail-folder-community-102"]');
      await expect(news).toHaveAttribute("data-community-id", "12");
      await expect(news).toContainText("Enterprise");
      await expect(blog).toHaveAttribute("data-community-id", "12");
      await expect(blog).toContainText("Enterprise");
      await expect(page.locator('[data-testid="detail-folder-community-42"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-item-community-42"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-row-42"]')).not.toHaveAttribute(
        "data-community-id",
        "12",
      );
      expect(posts).toEqual(["101", "102"]);
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
      const posts = await installFolderCommunityRoutes(page);
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-community"]').click();
      await expect(page.locator('[data-testid="explorer-set-folder-community-dialog"]')).toBeVisible();
      await page.locator('[data-testid="explorer-set-folder-community-cancel"]').click();
      await expect(page.locator('[data-testid="explorer-set-folder-community-dialog"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-set-folder-community-status"]')).toHaveCount(0);
      await expect(page.locator('[data-testid^="detail-folder-community-"]')).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "pages only are not a saved folder community",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const posts = await installFolderCommunityRoutes(page);
      await openCheckedListing(page, ["42", "43"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-community"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-community-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "page");
      await expect(status).toContainText("Pages are not given a folder community");
      await expect(status).not.toContainText("Folder community saved");
      await expect(page.locator('[data-testid="explorer-set-folder-community-dialog"]')).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "HTTP 409 on one folder does not claim the whole selection succeeded",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const posts = await installFolderCommunityRoutes(page, { blogStatus: 409 });
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-community"]').click();
      await page.locator('[data-testid="explorer-set-folder-community-select"]').selectOption("12");
      await page.locator('[data-testid="explorer-set-folder-community-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-community-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "partial");
      await expect(status).toHaveAttribute("data-community-id", "");
      await expect(status).toContainText("Blog (HTTP 409)");
      await expect(status).not.toContainText("Folder community saved");
      await expect(page.locator('[data-testid="detail-folder-community-101"]')).toHaveAttribute(
        "data-community-id",
        "12",
      );
      await expect(page.locator('[data-testid="detail-folder-community-102"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-folder-community-42"]')).toHaveCount(0);
      expect(posts).toEqual(["101", "102"]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );
});
