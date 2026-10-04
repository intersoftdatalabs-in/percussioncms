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
 * Explorer multi-select Set allowed publish sites (#5181 / parent #4530).
 *
 * <p>One site list, or a clear, is written on each checked folder. The names
 * are shown on a folder only after that folder's save returns and a properties
 * refresh shows the same id list. Pages and assets are not written. HTTP 409
 * is not full success. Cancel does not POST.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-set-folder-allowed-sites-multi.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl, openContentMenu } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  isKnownExplorerSetFolderAllowedSitesConsoleNoise,
} = require("./helpers/explorer-set-folder-allowed-sites");

const TAGS = [
  "@explorer",
  "@explorer-set-folder-allowed-sites",
  "@explorer-set-folder-allowed-sites-multi",
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
 * @returns {string|null}
 */
function postedAllowedSites(body) {
  try {
    const parsed = JSON.parse(body || "{}");
    const folder = parsed.FolderProperties || parsed.folderProperties || {};
    if (folder.allowedSites == null) {
      return null;
    }
    return String(folder.allowedSites);
  } catch {
    return null;
  }
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ blogStatus?: number, initial?: string }} [opts]
 */
async function installAllowedSitesRoutes(page, opts = {}) {
  const blogStatus = opts.blogStatus ?? 200;
  const initial = opts.initial ?? "301";
  /** @type {string[]} */
  const posts = [];
  /** @type {string[]} */
  const siteLists = [];
  /** @type {Record<string, string>} */
  const stored = {
    "101": initial,
    "102": initial,
  };
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/pathmanagement/path/folderAllowedSitesCatalog**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        FolderAllowedSitesCatalog: {
          choices: [
            { id: "301", name: "Enterprise" },
            { id: "302", name: "Corporate" },
          ],
        },
      }),
    });
  });
  await page.route("**/pathmanagement/path/folderProperties/**", async (route) => {
    const url = route.request().url();
    const id = decodeURIComponent(url.split("/folderProperties/")[1] || "").split(/[?#]/)[0];
    const current = Object.prototype.hasOwnProperty.call(stored, id) ? stored[id] : initial;
    const label = id === "102" ? "Blog" : "News";
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        FolderProperties: {
          id,
          name: label,
          allowedSites: current,
          permission: { accessLevel: "ADMIN" },
        },
      }),
    });
  });
  await page.route("**/pathmanagement/path/saveFolderProperties**", async (route) => {
    const body = route.request().postData() || "";
    const id = postedFolderId(body);
    const sites = postedAllowedSites(body);
    posts.push(id || body);
    if (sites != null) {
      siteLists.push(sites);
    }
    const status = id === "102" ? blogStatus : 200;
    if (status === 200 && sites != null && Object.prototype.hasOwnProperty.call(stored, id)) {
      stored[id] = sites;
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
      url.includes("/folderAllowedSitesCatalog") ||
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
  return { posts, siteLists };
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
      !isKnownExplorerSetFolderAllowedSitesConsoleNoise(msg.text())
    ) {
      errors.push(msg.text());
    }
  });
  return errors;
}

test.describe("Explorer multi-select Set allowed publish sites (#5181)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "save shows the allowed sites on each folder only after that refresh, and skips pages and assets",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts, siteLists } = await installAllowedSitesRoutes(page);
      await openCheckedListing(page, ["101", "102", "42", "44"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-allowed-sites"]').click();
      const dialog = page.locator('[data-testid="explorer-set-folder-allowed-sites-dialog"]');
      await expect(dialog).toBeVisible();
      await expect(
        page.locator('[data-testid="explorer-set-folder-allowed-sites-multi"]'),
      ).toBeVisible();
      await expect(page.locator('[data-testid="explorer-set-folder-allowed-sites-status"]')).toHaveCount(
        0,
      );
      await expect(page.locator('[data-testid^="detail-folder-allowed-sites-"]')).toHaveCount(0);
      await page.locator('[data-testid="explorer-set-folder-allowed-site-302"]').check();
      await page.locator('[data-testid="explorer-set-folder-allowed-sites-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-allowed-sites-status"]');
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-reason", "items-skipped");
      await expect(status).toHaveAttribute("data-allowed-sites", "301,302");
      await expect(status).toHaveAttribute("data-allowed-site-names", "Enterprise, Corporate");
      await expect(status).toContainText("Allowed publish sites saved");
      await expect(status).toContainText("Enterprise");
      await expect(status).toContainText("Corporate");
      await expect(status).toContainText("Home");
      await expect(status).toContainText("Logo");
      await expect(dialog).toHaveCount(0);
      const news = page.locator('[data-testid="detail-folder-allowed-sites-101"]');
      const blog = page.locator('[data-testid="detail-folder-allowed-sites-102"]');
      await expect(news).toHaveAttribute("data-allowed-sites", "301,302");
      await expect(news).toContainText("Enterprise, Corporate");
      await expect(blog).toHaveAttribute("data-allowed-sites", "301,302");
      await expect(blog).toContainText("Corporate");
      await expect(page.locator('[data-testid="detail-folder-allowed-sites-42"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-folder-allowed-sites-44"]')).toHaveCount(0);
      expect(posts).toEqual(["101", "102"]);
      expect(siteLists).toEqual(["301,302", "301,302"]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "an empty site selection clears each folder only after that refresh",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts, siteLists } = await installAllowedSitesRoutes(page);
      await openCheckedListing(page, ["101", "102"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-allowed-sites"]').click();
      await expect(
        page.locator('[data-testid="explorer-set-folder-allowed-sites-dialog"]'),
      ).toBeVisible();
      await page.locator('[data-testid="explorer-set-folder-allowed-site-301"]').uncheck();
      await page.locator('[data-testid="explorer-set-folder-allowed-sites-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-allowed-sites-status"]');
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-reason", "cleared");
      await expect(status).toHaveAttribute("data-allowed-sites", "");
      await expect(status).toContainText("Allowed publish sites cleared");
      await expect(status).toContainText("all sites");
      await expect(status).not.toContainText("Allowed publish sites saved");
      const news = page.locator('[data-testid="detail-folder-allowed-sites-101"]');
      const blog = page.locator('[data-testid="detail-folder-allowed-sites-102"]');
      await expect(news).toHaveAttribute("data-cleared", "true");
      await expect(news).toContainText("All sites");
      await expect(blog).toHaveAttribute("data-cleared", "true");
      expect(posts).toEqual(["101", "102"]);
      expect(siteLists).toEqual(["", ""]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "cancel does not call the server",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts } = await installAllowedSitesRoutes(page);
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-allowed-sites"]').click();
      await expect(
        page.locator('[data-testid="explorer-set-folder-allowed-sites-dialog"]'),
      ).toBeVisible();
      await page.locator('[data-testid="explorer-set-folder-allowed-sites-cancel"]').click();
      await expect(
        page.locator('[data-testid="explorer-set-folder-allowed-sites-dialog"]'),
      ).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-set-folder-allowed-sites-status"]')).toHaveCount(
        0,
      );
      await expect(page.locator('[data-testid^="detail-folder-allowed-sites-"]')).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "pages only are not given allowed publish sites",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts } = await installAllowedSitesRoutes(page);
      await openCheckedListing(page, ["42", "43"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-allowed-sites"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-allowed-sites-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "page");
      await expect(status).toContainText("Pages are not given allowed publish sites");
      await expect(status).toContainText("Home");
      await expect(status).toContainText("About");
      await expect(status).not.toContainText("Allowed publish sites saved");
      await expect(
        page.locator('[data-testid="explorer-set-folder-allowed-sites-dialog"]'),
      ).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "assets only are not given allowed publish sites",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts } = await installAllowedSitesRoutes(page);
      await openCheckedListing(page, ["44", "45"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-allowed-sites"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-allowed-sites-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "asset");
      await expect(status).toContainText("Assets are not given allowed publish sites");
      await expect(status).toContainText("Logo");
      await expect(status).toContainText("Banner");
      await expect(status).not.toContainText("Allowed publish sites saved");
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "HTTP 409 on one folder does not claim the whole selection succeeded",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts, siteLists } = await installAllowedSitesRoutes(page, { blogStatus: 409 });
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-allowed-sites"]').click();
      await page.locator('[data-testid="explorer-set-folder-allowed-site-302"]').check();
      await page.locator('[data-testid="explorer-set-folder-allowed-sites-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-allowed-sites-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "partial");
      await expect(status).toHaveAttribute("data-allowed-sites", "");
      await expect(status).toContainText("Blog (HTTP 409)");
      await expect(status).not.toContainText("Allowed publish sites saved");
      await expect(status).not.toContainText("Allowed publish sites cleared");
      await expect(page.locator('[data-testid="detail-folder-allowed-sites-101"]')).toHaveAttribute(
        "data-allowed-sites",
        "301,302",
      );
      await expect(page.locator('[data-testid="detail-folder-allowed-sites-102"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-folder-allowed-sites-42"]')).toHaveCount(0);
      expect(posts).toEqual(["101", "102"]);
      expect(siteLists).toEqual(["301,302", "301,302"]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );
});
