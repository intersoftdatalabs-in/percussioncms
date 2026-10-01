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
 * Playwright: #5007 / parent #4530 — Explorer Create → Promotable Version.
 *
 * Tags: @explorer-promotable-version @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-promotable-version.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const {
  seedDisposableAsset,
  seedDisposableEmptyFolder,
} = require("./helpers/explorer-copy-item");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerPromotableUrl,
  isItemPromotableUrl,
  isKnownExplorerPromotableConsoleNoise,
  isPromotableHttpFailure,
  isPromotableSuccess,
} = require("./helpers/explorer-promotable-version");

const TAGS = ["@explorer-promotable-version", "@explorer", "@smoke"];

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (
      msg.type() === "error" &&
      !isKnownExplorerPromotableConsoleNoise(msg.text())
    ) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerPromotableUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

async function listReady(page) {
  await page.locator(`[data-testid="${TEST_IDS.detailList}"]`).waitFor({ timeout: 20_000 });
}

async function listEpoch(page) {
  return page.locator(`[data-testid="${TEST_IDS.nav}"]`).getAttribute("data-list-epoch");
}

/**
 * Seed a folder + asset under Assets or Sites and select the asset row.
 * Stock H2 often has no selectable page (#5023); do not depend on sample pages.
 *
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {"Assets"|"Sites"} rootName
 * @param {string} stamp
 */
async function selectSeededItem(page, request, rootName, stamp) {
  const folderName = `qa5007${rootName.slice(0, 1).toLowerCase()}${stamp}`;
  const itemName = `qa5007i${stamp}`;
  const folder = await seedDisposableEmptyFolder(
    request,
    BASE_URL,
    adminBasicAuthHeaders(),
    { parentPath: rootName, name: folderName },
  );
  await seedDisposableAsset(request, BASE_URL, adminBasicAuthHeaders(), {
    parentPath: String(folder.path || `/${rootName}/${folderName}`),
    name: itemName,
  });
  const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
  const root = tree
    .locator(
      `[data-testid="tree-node-/${rootName}/"], [data-testid="tree-node-/${rootName}"]`,
    )
    .first();
  await expect(root).toBeVisible({ timeout: 30_000 });
  await root.click({ force: true });
  await listReady(page);
  const toggle = tree
    .locator(
      `[data-testid="tree-toggle-/${rootName}/"], [data-testid="tree-toggle-/${rootName}"]`,
    )
    .first();
  if ((await toggle.count()) > 0 && (await toggle.innerText()) !== "▾") {
    await toggle.click({ force: true });
  }
  const list = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
  const folderRow = list
    .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]')
    .filter({ hasText: folderName })
    .first();
  await expect(folderRow).toBeVisible({ timeout: 20_000 });
  const icon = folderRow.locator('[data-testid^="detail-folder-icon-"]');
  if ((await icon.count()) > 0) {
    await icon.first().click({ force: true });
  } else {
    await folderRow.dblclick({ force: true });
  }
  await listReady(page);
  const itemRow = list
    .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="item"]')
    .filter({ hasText: itemName })
    .first();
  await expect(itemRow).toBeVisible({ timeout: 20_000 });
  await itemRow.click({ force: true });
}

async function clickPromotable(page) {
  const create = page
    .locator(`[data-testid="${TEST_IDS.createMenu}"][aria-haspopup="menu"]`)
    .first();
  await expect(create).toBeVisible({ timeout: 20_000 });
  const menuItem = page
    .locator(`[data-testid="${TEST_IDS.promotable}"][role="menuitem"]`)
    .first();
  for (let attempt = 0; attempt < 3; attempt += 1) {
    if (!(await menuItem.isVisible().catch(() => false))) {
      await create.click({ force: true });
    }
    try {
      await menuItem.click({ timeout: 5_000 });
      return;
    } catch (err) {
      if (attempt === 2) {
        throw err;
      }
    }
  }
}

test.describe("Explorer promotable version of the selected item (#5007 / #4530)", () => {
  test(
    "UI: confirm on a seeded asset creates a promotable version",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(120_000);
      const stamp = Date.now().toString(36);
      const posts = [];
      await page.route("**/itemmanagement/item/promotableVersion/**", async (route) => {
        posts.push(route.request().url());
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemCopyResult: {
              itemId: "9101",
              folderPath: "//Assets",
              promotable: true,
            },
          }),
        });
      });
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/promotable version/i);
        void dialog.accept();
      });
      const jsErrors = await openExplorer(page);
      // Assets, not Sites: H2 rejects addNewFolder on the Sites root, and stock
      // H2 often has no selectable page (#5023). Pages use the same dispatcher.
      await selectSeededItem(page, request, "Assets", stamp);
      const before = await listEpoch(page);
      await clickPromotable(page);
      await expect.poll(() => posts.length, { timeout: 15_000 }).toBe(1);
      expect(isItemPromotableUrl(posts[0])).toBe(true);
      const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
      await expect
        .poll(async () => {
          const text = (await error.count()) > 0 ? await error.innerText() : "";
          return isPromotableSuccess(before, await listEpoch(page), text);
        })
        .toBe(true);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.shell}"]`,
      });
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: cancel does not create a promotable version",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(120_000);
      const stamp = Date.now().toString(36);
      const posts = [];
      await page.route("**/itemmanagement/item/promotableVersion/**", async (route) => {
        posts.push(route.request().url());
        await route.fulfill({
          status: 500,
          contentType: "text/plain",
          body: "should not be called",
        });
      });
      page.on("dialog", (dialog) => {
        void dialog.dismiss();
      });
      const jsErrors = await openExplorer(page);
      await selectSeededItem(page, request, "Assets", stamp);
      const before = await listEpoch(page);
      await clickPromotable(page);
      await page.waitForTimeout(500);
      expect(posts).toEqual([]);
      expect(await listEpoch(page)).toBe(before);
      await expect(page.locator(`[data-testid="${TEST_IDS.serverError}"]`)).toHaveCount(0);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: a folder does not claim a promotable version",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      await page.route("**/itemmanagement/item/promotableVersion/**", async (route) => {
        posts.push(route.request().url());
        await route.abort();
      });
      const jsErrors = await openExplorer(page);
      const root = page.locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      );
      await expect(root.first()).toBeVisible({ timeout: 30_000 });
      await root.first().click();
      await listReady(page);
      const folderRow = page.locator(
        '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
      );
      await expect(folderRow.first()).toBeVisible({ timeout: 20_000 });
      await folderRow.first().click({ force: true });
      const before = await listEpoch(page);
      page.once("dialog", (dialog) => {
        void dialog.accept();
      });
      await clickPromotable(page);
      const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Select a content item first/i);
      expect(posts).toEqual([]);
      expect(await listEpoch(page)).toBe(before);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: an empty selection does not claim a promotable version",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      await page.route("**/itemmanagement/item/promotableVersion/**", async (route) => {
        posts.push(route.request().url());
        await route.abort();
      });
      const jsErrors = await openExplorer(page);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toHaveAttribute("data-selected-item-id", "");
      const before = await listEpoch(page);
      page.once("dialog", (dialog) => {
        void dialog.accept();
      });
      await clickPromotable(page);
      const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
      await expect(error).toContainText(/Select a content item first/i);
      expect(posts).toEqual([]);
      expect(await listEpoch(page)).toBe(before);
      expect(isPromotableSuccess(before, await listEpoch(page), "")).toBe(false);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `UI: HTTP ${status} does not claim a promotable version`,
      { tag: TAGS },
      async ({ page, request }) => {
        test.setTimeout(120_000);
        const stamp = `${status}${Date.now().toString(36)}`;
        let posts = 0;
        await page.route("**/itemmanagement/item/promotableVersion/**", async (route) => {
          posts += 1;
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message: "no" }),
          });
        });
        page.on("dialog", (dialog) => {
          void dialog.accept();
        });
        const jsErrors = await openExplorer(page);
        await selectSeededItem(page, request, "Assets", stamp);
        const before = await listEpoch(page);
        await clickPromotable(page);
        await expect.poll(() => posts, { timeout: 15_000 }).toBe(1);
        const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
        await expect(error).toBeVisible({ timeout: 10_000 });
        const text = await error.innerText();
        expect(isPromotableHttpFailure(text, status)).toBe(true);
        expect(await listEpoch(page)).toBe(before);
        expect(jsErrors, jsErrors.join("\n")).toEqual([]);
      },
    );
  }
});
