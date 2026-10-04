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
 * Explorer multi-select Set folder workflow (#5179 / parent #4530).
 *
 * <p>One catalog workflow is written on each checked folder. The name is
 * shown on a folder only after that folder's save returns and a properties
 * refresh shows the new id. Pages and assets are not written. HTTP 409 is
 * not full success. Cancel does not POST.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-set-folder-workflow-multi.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl, openContentMenu } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  isKnownExplorerSetFolderWorkflowConsoleNoise,
} = require("./helpers/explorer-set-folder-workflow");

const TAGS = [
  "@explorer",
  "@explorer-set-folder-workflow",
  "@explorer-set-folder-workflow-multi",
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
    ],
    childrenCount: 5,
    startIndex: 0,
  },
};

const CHOICES = [
  { id: "4", name: "Simple" },
  { id: "7", name: "Local" },
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
function postedWorkflowId(body) {
  try {
    const parsed = JSON.parse(body || "{}");
    const folder = parsed.FolderProperties || parsed.folderProperties || {};
    return String(folder.workflowId ?? "").trim();
  } catch {
    return "";
  }
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ blogStatus?: number }} [opts]
 */
async function installFolderWorkflowRoutes(page, opts = {}) {
  const blogStatus = opts.blogStatus ?? 200;
  /** @type {string[]} */
  const posts = [];
  /** @type {string[]} */
  const workflowIds = [];
  /** @type {Record<string, string>} */
  const stored = {
    "101": "4",
    "102": "4",
  };
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/pathmanagement/path/folderWorkflowCatalog**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        FolderWorkflowCatalog: { choices: CHOICES },
      }),
    });
  });
  await page.route("**/pathmanagement/path/folderProperties/**", async (route) => {
    const url = route.request().url();
    const id = decodeURIComponent(url.split("/folderProperties/")[1] || "").split(/[?#]/)[0];
    const workflowId = stored[id] || "4";
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        FolderProperties: {
          id,
          name: id === "102" ? "Blog" : "News",
          workflowId,
          permission: { accessLevel: "ADMIN" },
        },
      }),
    });
  });
  await page.route("**/pathmanagement/path/saveFolderProperties**", async (route) => {
    const body = route.request().postData() || "";
    const id = postedFolderId(body);
    posts.push(id || body);
    workflowIds.push(postedWorkflowId(body));
    const status = id === "102" ? blogStatus : 200;
    if (status === 200 && Object.prototype.hasOwnProperty.call(stored, id)) {
      stored[id] = "7";
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
      url.includes("/folderWorkflowCatalog") ||
      url.includes("/folderProperties") ||
      url.includes("/saveFolderProperties") ||
      url.includes("/paginatedFolder")
    ) {
      await route.fallback();
      return;
    }
    if (/\/(101|102|42|43|44)(?:\/|$|\?)/.test(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "{}",
      });
      return;
    }
    await route.fallback();
  });
  return { posts, workflowIds };
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
    if (msg.type() === "error" && !isKnownExplorerSetFolderWorkflowConsoleNoise(msg.text())) {
      errors.push(msg.text());
    }
  });
  return errors;
}

test.describe("Explorer multi-select Set folder workflow (#5179)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "save shows the workflow on each folder only after that save, and skips pages and assets",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts, workflowIds } = await installFolderWorkflowRoutes(page);
      await openCheckedListing(page, ["101", "102", "42", "44"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-workflow"]').click();
      const dialog = page.locator('[data-testid="explorer-set-folder-workflow-dialog"]');
      await expect(dialog).toBeVisible();
      await expect(page.locator('[data-testid="explorer-set-folder-workflow-multi"]')).toBeVisible();
      await expect(page.locator('[data-testid="explorer-set-folder-workflow-status"]')).toHaveCount(0);
      await expect(page.locator('[data-testid^="detail-folder-workflow-"]')).toHaveCount(0);
      await page.locator('[data-testid="explorer-set-folder-workflow-select"]').selectOption("7");
      await page.locator('[data-testid="explorer-set-folder-workflow-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-workflow-status"]');
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-reason", "items-skipped");
      await expect(status).toHaveAttribute("data-workflow-id", "7");
      await expect(status).toHaveAttribute("data-workflow-name", "Local");
      await expect(status).toContainText("Folder workflow saved");
      await expect(status).toContainText("Local");
      await expect(status).toContainText("Home");
      await expect(status).toContainText("Logo");
      await expect(dialog).toHaveCount(0);
      const news = page.locator('[data-testid="detail-folder-workflow-101"]');
      const blog = page.locator('[data-testid="detail-folder-workflow-102"]');
      await expect(news).toHaveAttribute("data-workflow-id", "7");
      await expect(news).toContainText("Local");
      await expect(blog).toHaveAttribute("data-workflow-id", "7");
      await expect(blog).toContainText("Local");
      await expect(page.locator('[data-testid="detail-folder-workflow-42"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-folder-workflow-44"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-row-42"]')).not.toHaveAttribute(
        "data-workflow-id",
        "7",
      );
      await expect(page.locator('[data-testid="detail-row-44"]')).not.toHaveAttribute(
        "data-workflow-id",
        "7",
      );
      expect(posts).toEqual(["101", "102"]);
      expect(workflowIds).toEqual(["7", "7"]);
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
      const { posts } = await installFolderWorkflowRoutes(page);
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-workflow"]').click();
      await expect(page.locator('[data-testid="explorer-set-folder-workflow-dialog"]')).toBeVisible();
      await page.locator('[data-testid="explorer-set-folder-workflow-cancel"]').click();
      await expect(page.locator('[data-testid="explorer-set-folder-workflow-dialog"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-set-folder-workflow-status"]')).toHaveCount(0);
      await expect(page.locator('[data-testid^="detail-folder-workflow-"]')).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "pages only are not a saved folder workflow",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts } = await installFolderWorkflowRoutes(page);
      await openCheckedListing(page, ["42", "43"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-workflow"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-workflow-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "page");
      await expect(status).toContainText("Pages are not given a folder workflow");
      await expect(status).not.toContainText("Folder workflow saved");
      await expect(page.locator('[data-testid="explorer-set-folder-workflow-dialog"]')).toHaveCount(0);
      expect(posts).toEqual([]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );

  test(
    "HTTP 409 on one folder does not claim the whole selection succeeded",
    { tag: TAGS },
    async ({ page }) => {
      const errors = trackErrors(page);
      const { posts, workflowIds } = await installFolderWorkflowRoutes(page, { blogStatus: 409 });
      await openCheckedListing(page, ["101", "102", "42"]);
      await openContentMenu(page);
      await page.locator('[data-testid="explorer-set-folder-workflow"]').click();
      await page.locator('[data-testid="explorer-set-folder-workflow-select"]').selectOption("7");
      await page.locator('[data-testid="explorer-set-folder-workflow-save"]').click();
      const status = page.locator('[data-testid="explorer-set-folder-workflow-status"]');
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "partial");
      await expect(status).toHaveAttribute("data-workflow-id", "");
      await expect(status).toContainText("Blog (HTTP 409)");
      await expect(status).not.toContainText("Folder workflow saved");
      await expect(page.locator('[data-testid="detail-folder-workflow-101"]')).toHaveAttribute(
        "data-workflow-id",
        "7",
      );
      await expect(page.locator('[data-testid="detail-folder-workflow-102"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="detail-folder-workflow-42"]')).toHaveCount(0);
      expect(posts).toEqual(["101", "102"]);
      expect(workflowIds).toEqual(["7", "7"]);
      expect(errors, errors.join("\n")).toEqual([]);
    },
  );
});
