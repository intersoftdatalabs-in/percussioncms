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
 * Explorer multi-select Set folder display format (#5180 / parent #4530).
 *
 * <p>One catalog display format is written on each checked folder. The name
 * is shown on a folder only after that folder's save returns and a properties
 * refresh shows the new id and catalog name. Pages and assets are not written.
 * HTTP 409 is not full success. Cancel does not POST.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-set-folder-display-format-multi.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl, openContentMenu } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  isKnownExplorerSetFolderDisplayFormatConsoleNoise,
} = require("./helpers/explorer-set-folder-display-format");

const TAGS = [
  "@explorer",
  "@explorer-set-folder-display-format",
  "@explorer-set-folder-display-format-multi",
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
      {
        id: "44",
        name: "Logo",
        path: "/Assets/Logo",
        type: "percImage",
        category: "asset",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "45",
        name: "Banner",
        path: "/Assets/Banner",
        type: "percImage",
        category: "asset",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 6,
    startIndex: 0,
  },
};

const CHOICES = [
  { id: "3", name: "Default" },
  { id: "12", name: "Simple" },
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
 * @param {string} body
 * @returns {string}
 */
function postedFormatId(body) {
  try {
    const parsed = JSON.parse(body || "{}");
    const folder = parsed.FolderProperties || parsed.folderProperties || {};
    return String(folder.displayFormatId ?? "").trim();
  } catch {
    return "";
  }
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ blogStatus?: number }} [opts]
 */
async function installFolderDisplayFormatRoutes(page, opts = {}) {
  const blogStatus = opts.blogStatus ?? 200;
  /** @type {string[]} */
  const posts = [];
  /** @type {string[]} */
  const formatIds = [];
  /** @type {Record<string, { id: string, name: string }>} */
  const stored = {
    "101": { id: "3", name: "Default" },
    "102": { id: "3", name: "Default" },
  };
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/pathmanagement/path/folderDisplayFormatCatalog**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        FolderDisplayFormatCatalog: { choices: CHOICES },
      }),
    });
  });
  await page.route("**/pathmanagement/path/folderProperties/**", async (route) => {
    const url = route.request().url();
    const id = decodeURIComponent(url.split("/folderProperties/")[1] || "").split(/[?#]/)[0];
    const current = stored[id] || { id: "3", name: "Default" };
    const label = id === "102" ? "Blog" : "News";
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        FolderProperties: {
          id,
          name: label,
          displayFormatId: current.id,
          displayFormatName: current.name,
          permission: { accessLevel: "ADMIN" },
        },
      }),
    });
  });
  await page.route("**/pathmanagement/path/saveFolderProperties**", async (route) => {
    const body = route.request().postData() || "";
    const id = postedFolderId(body);
    posts.push(id || body);
    formatIds.push(postedFormatId(body));
    const status = id === "102" ? blogStatus : 200;
    if (status === 200 && Object.prototype.hasOwnProperty.call(stored, id)) {
      stored[id] = { id: "12", name: "Simple" };
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
      url.includes("/folderDisplayFormatCatalog") ||
      url.includes("/folderProperties") ||
      url.includes("/saveFolderProperties") ||
      url.includes("/paginatedFolder")
    ) {
      await route.fallback();
      return;
    }
    if (/\/(101|102|42|43|44|45)(?:\/|$|\?)/.test(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "{}",
      });
      return;
    }
    await route.fallback();
  });
  return { posts, formatIds };
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
    if (
      msg.type() === "error" &&
      !isKnownExplorerSetFolderDisplayFormatConsoleNoise(msg.text())
    ) {
      errors.push(msg.text());
    }
  });
  return errors;
}

test.describe("Explorer multi-select Set folder display format (#5180)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "save shows the display format on each folder only after that refresh, and skips pages and assets",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts, formatIds } = await installFolderDisplayFormatRoutes(page);
      await openCheckedListing(page, ["101", "102", "42", "44"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-display-format"]').click();
      const dialog = page.locator('[data-testid="explorer-set-folder-display-format-dialog"]');
      await expect(dialog).toBeVisible();
      await expect(
        page.locator('[data-testid="explorer-set-folder-display-format-multi"]'),
      ).toBeVisible();
      await expect(page.locator('[data-testid="explorer-set-folder-display-format-status"]')).toHaveCount(
        0,
      );
      await expect(page.locator('[data-testid^="detail-folder-display-format-"]')).toHaveCount(0);
      await page.locator('[data-testid="explorer-set-folder-display-format-select"]').selectOption("12");
      await page.locator('[data-testid="explorer-set-folder-display-format-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-display-format-status"]');
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-reason", "items-skipped");
      await expect(status).toHaveAttribute("data-format-id", "12");
      await expect(status).toHaveAttribute("data-format-name", "Simple");
      await expect(status).toContainText("Folder display format saved");
      await expect(status).toContainText("Simple");
      await expect(status).toContainText("Home");
      await expect(status).toContainText("Logo");
      await expect(dialog).toHaveCount(0);
      const news = page.locator('[data-testid="detail-folder-display-format-101"]');
      const blog = page.locator('[data-testid="detail-folder-display-format-102"]');
      await expect(news).toHaveAttribute("data-format-id", "12");
      await expect(news).toContainText("Simple");
      await expect(blog).toHaveAttribute("data-format-id", "12");
      await expect(blog).toContainText("Simple");
      await expect(page.locator('[data-testid="detail-folder-display-format-42"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-folder-display-format-44"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-row-42"]')).not.toHaveAttribute(
        "data-format-id",
        "12",
      );
      await expect(page.locator('[data-testid="detail-row-44"]')).not.toHaveAttribute(
        "data-format-id",
        "12",
      );
      expect(posts).toEqual(["101", "102"]);
      expect(formatIds).toEqual(["12", "12"]);
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
      const { posts } = await installFolderDisplayFormatRoutes(page);
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-display-format"]').click();
      await expect(
        page.locator('[data-testid="explorer-set-folder-display-format-dialog"]'),
      ).toBeVisible();
      await page.locator('[data-testid="explorer-set-folder-display-format-cancel"]').click();
      await expect(
        page.locator('[data-testid="explorer-set-folder-display-format-dialog"]'),
      ).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-set-folder-display-format-status"]')).toHaveCount(
        0,
      );
      await expect(page.locator('[data-testid^="detail-folder-display-format-"]')).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "pages only are not a saved folder display format",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts } = await installFolderDisplayFormatRoutes(page);
      await openCheckedListing(page, ["42", "43"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-display-format"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-display-format-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "page");
      await expect(status).toContainText("Pages are not given a folder display format");
      await expect(status).toContainText("Home");
      await expect(status).toContainText("About");
      await expect(status).not.toContainText("Folder display format saved");
      await expect(
        page.locator('[data-testid="explorer-set-folder-display-format-dialog"]'),
      ).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "assets only are not a saved folder display format",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts } = await installFolderDisplayFormatRoutes(page);
      await openCheckedListing(page, ["44", "45"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-display-format"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-display-format-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "asset");
      await expect(status).toContainText("Assets are not given a folder display format");
      await expect(status).toContainText("Logo");
      await expect(status).toContainText("Banner");
      await expect(status).not.toContainText("Folder display format saved");
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "HTTP 409 on one folder does not claim the whole selection succeeded",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts, formatIds } = await installFolderDisplayFormatRoutes(page, {
        blogStatus: 409,
      });
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-display-format"]').click();
      await page.locator('[data-testid="explorer-set-folder-display-format-select"]').selectOption("12");
      await page.locator('[data-testid="explorer-set-folder-display-format-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-display-format-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "partial");
      await expect(status).toHaveAttribute("data-format-id", "");
      await expect(status).toContainText("Blog (HTTP 409)");
      await expect(status).not.toContainText("Folder display format saved");
      await expect(page.locator('[data-testid="detail-folder-display-format-101"]')).toHaveAttribute(
        "data-format-id",
        "12",
      );
      await expect(page.locator('[data-testid="detail-folder-display-format-102"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-folder-display-format-42"]')).toHaveCount(0);
      expect(posts).toEqual(["101", "102"]);
      expect(formatIds).toEqual(["12", "12"]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );
});
