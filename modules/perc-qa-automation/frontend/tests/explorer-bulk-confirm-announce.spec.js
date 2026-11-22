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
 * Playwright surface: #7037 / parent #4530 — bulk-action confirm dialogs
 * (Recycle / Purge / Restore, plus Bulk Stage) announce outcomes through a
 * shared polite live region, cancel announces the no-op, Escape cancels, and
 * focus returns to the invoking control.
 *
 * Keyboard-driven: checkboxes via Space, Content menu via Enter, dialog
 * confirm via Tab + Enter, cancel via Escape. Hermetic listing mocks (same
 * shape as explorer-single-stage.spec.js) so no seed data is required.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-bulk-confirm-announce.spec.js
 * from modules/perc-qa-automation/frontend.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const {
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-item");

const SITES_LISTING = {
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

const RECYCLING_LISTING = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "guid-ok",
        name: "OkPage",
        path: "/Recycling/OkPage",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "guid-miss",
        name: "Missing",
        path: "/Recycling/Missing",
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
 */
async function installBulkConfirmRoutes(page) {
  /** @type {string[]} */
  const deleted = [];
  /** @type {string[]} */
  const purged = [];
  /** @type {string[]} */
  const restored = [];
  // Mock the checkout-owner lookup so the mocked row ids never hit the
  // real server (a lookup for a fake id logs a server ERROR). Benign
  // reader assignment: nothing is checked out (#6997, same as takedown).
  await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: "",
          currentUser: "Admin",
          assignmentType: "Reader",
        },
      }),
    });
  });
  // Row identity / enablement lookups keyed by the mocked listing ids must
  // never reach the real server: unknown ids log server ERRORs
  // (PSComponentSummary / PSGuid mapping). Empty payloads keep the
  // visibility evaluator permissive and the bulk menus enabled (#7037).
  await page.route("**/rest/explorer/identity/batch", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "[]",
    });
  });
  await page.route("**/services/actions/find/types", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ActionMenuList: [] }),
    });
  });
  await page.route("**/services/actions/find/templates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ActionMenuList: [] }),
    });
  });
  await page.route(
    "**/services/itemmanagement/workflow/getTransitions/**",
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "{}",
      });
    },
  );
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    const url = route.request().url();
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(
        url.includes("Recycling") ? RECYCLING_LISTING : SITES_LISTING,
      ),
    });
  });
  await page.route("**/pathmanagement/path/folder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ PathItem: [] }),
    });
  });
  await page.route("**/rest/folders/item/**", async (route) => {
    if (route.request().method() === "DELETE") {
      deleted.push(route.request().url());
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  });
  await page.route("**/rest/folders/recycle/**", async (route) => {
    const req = route.request();
    const url = req.url();
    if (req.method() === "DELETE" && !/\/restore|\/empty/.test(url)) {
      purged.push(url);
      const fail = url.includes("guid-miss");
      await route.fulfill({
        status: fail ? 409 : 200,
        contentType: "application/json",
        body: "{}",
      });
      return;
    }
    if (req.method() === "PUT" && /\/restore\//.test(url)) {
      restored.push(url);
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  });
  return { deleted, purged, restored };
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} [path]
 */
async function openExplorer(page, path) {
  const url =
    explorerSpaUrl(BASE_URL) + (path ? `&path=${encodeURIComponent(path)}` : "");
  await page.goto(url);
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
}

/**
 * Keyboard-check two detail rows by their checkbox testids.
 * @param {import('@playwright/test').Page} page
 * @param {string} first
 * @param {string} second
 */
async function checkTwoRows(page, first, second) {
  await expect(page.locator(`[data-testid="${first}"]`)).toBeVisible({
    timeout: 20_000,
  });
  await page.locator(`[data-testid="${first}"]`).focus();
  await page.keyboard.press("Space");
  await page.locator(`[data-testid="${second}"]`).focus();
  await page.keyboard.press("Space");
}

/**
 * Keyboard-open the Content group and activate a menu item by testid.
 * @param {import('@playwright/test').Page} page
 * @param {string} itemTestId
 */
async function activateContentMenuItem(page, itemTestId) {
  await page.locator('[data-testid="explorer-menu-content"]').focus();
  await page.keyboard.press("Enter");
  const item = page.locator(`[data-testid="${itemTestId}"]`);
  await expect(item).toBeVisible({ timeout: 10_000 });
  await item.focus();
  await page.keyboard.press("Enter");
}

test.describe("Explorer bulk-confirm announcements (#7037)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "keyboard: recycle confirm announces success and returns focus",
    { tag: ["@explorer", "@explorer-bulk-confirm", "@a11y"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") pageErrors.push(msg.text());
      });
      const { deleted } = await installBulkConfirmRoutes(page);
      await openExplorer(page);

      await checkTwoRows(page, "detail-select-42", "detail-select-43");
      await activateContentMenuItem(page, "explorer-multi-recycle");

      const dialog = page.locator('[data-testid="explorer-recycle-confirm"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await expect(dialog).toHaveAttribute("role", "dialog");
      // Destructive confirms start on Cancel so Enter cannot confirm (#7003).
      await expect(
        page.locator('[data-testid="explorer-recycle-cancel"]'),
      ).toBeFocused();
      // Tab to OK, then Enter confirms.
      await page.keyboard.press("Tab");
      await expect(
        page.locator('[data-testid="explorer-recycle-ok"]'),
      ).toBeFocused();
      await page.keyboard.press("Enter");

      const result = page.locator(
        '[data-testid="explorer-multi-recycle-result"]',
      );
      await expect(result).toBeVisible({ timeout: 15_000 });
      await expect(result).toHaveAttribute("data-outcome", "success");
      await expect(result).toHaveAttribute("role", "status");
      await expect(result).toContainText(/Recycled 2 item\(s\)/);
      expect(deleted.length).toBe(2);
      // The Content menu closed when the command ran: focus returns to the
      // Content menu button.
      await expect(
        page.locator('[data-testid="explorer-menu-content"]'),
      ).toBeFocused();
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      const related = pageErrors.filter(
        (text) => !isKnownExplorerSitesConsoleNoise(text),
      );
      expect(related, related.join("\n")).toEqual([]);
    },
  );

  test(
    "keyboard: Escape cancels recycle with the no-op announcement",
    { tag: ["@explorer", "@explorer-bulk-confirm"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { deleted } = await installBulkConfirmRoutes(page);
      await openExplorer(page);

      await checkTwoRows(page, "detail-select-42", "detail-select-43");
      await activateContentMenuItem(page, "explorer-multi-recycle");
      const dialog = page.locator('[data-testid="explorer-recycle-confirm"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });

      await page.keyboard.press("Escape");
      await expect(dialog).toHaveCount(0);
      expect(deleted).toEqual([]);

      const result = page.locator(
        '[data-testid="explorer-multi-recycle-result"]',
      );
      await expect(result).toBeVisible({ timeout: 10_000 });
      await expect(result).toHaveAttribute("data-outcome", "cancelled");
      await expect(result).toHaveAttribute("role", "status");
      await expect(result).toContainText(/Recycle cancelled/);
      await expect(
        page.locator('[data-testid="explorer-menu-content"]'),
      ).toBeFocused();
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "keyboard: purge partial names the failed item and restore cancel is a no-op",
    { tag: ["@explorer", "@explorer-bulk-confirm"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const { purged, restored } = await installBulkConfirmRoutes(page);
      await openExplorer(page, "/Recycling");

      await checkTwoRows(page, "detail-select-guid-ok", "detail-select-guid-miss");
      await activateContentMenuItem(page, "explorer-multi-purge");
      const purgeDialog = page.locator(
        '[data-testid="explorer-purge-confirm"]',
      );
      await expect(purgeDialog).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-purge-cancel"]'),
      ).toBeFocused();
      await page.locator('[data-testid="explorer-purge-ok"]').click();

      const purgeResult = page.locator(
        '[data-testid="explorer-multi-purge-result"]',
      );
      await expect(purgeResult).toBeVisible({ timeout: 15_000 });
      await expect(purgeResult).toHaveAttribute("data-outcome", "partial");
      await expect(purgeResult).toHaveAttribute("role", "alert");
      await expect(purgeResult).toContainText(/Missing/);
      expect(purged.length).toBe(2);

      // Restore cancel from the toolbar: nothing sent, polite no-op, focus
      // returns to the invoking control.
      const restoreButton = page.locator('[data-testid="action-restore"]');
      await expect(restoreButton).toBeVisible({ timeout: 10_000 });
      await restoreButton.focus();
      await page.keyboard.press("Enter");
      const restoreDialog = page.locator(
        '[data-testid="explorer-restore-confirm"]',
      );
      await expect(restoreDialog).toBeVisible({ timeout: 10_000 });
      await page.keyboard.press("Escape");
      await expect(restoreDialog).toHaveCount(0);
      expect(restored).toEqual([]);
      const restoreResult = page.locator(
        '[data-testid="explorer-multi-restore-result"]',
      );
      await expect(restoreResult).toBeVisible({ timeout: 10_000 });
      await expect(restoreResult).toHaveAttribute("data-outcome", "cancelled");
      await expect(restoreResult).toHaveAttribute("role", "status");
      await expect(restoreResult).toContainText(/Restore cancelled/);
      await expect(restoreButton).toBeFocused();
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
