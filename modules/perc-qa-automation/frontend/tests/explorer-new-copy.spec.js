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
 * Playwright: #5006 / parent #4530 — Explorer Create → New Copy.
 *
 * Tags: @explorer-new-copy @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-new-copy.spec.js
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
  explorerNewCopyUrl,
  isItemNewCopyUrl,
  isKnownExplorerNewCopyConsoleNoise,
  isNewCopyHttpFailure,
  isNewCopySuccess,
  pickContentFolderIndex,
} = require("./helpers/explorer-new-copy");

const TAGS = ["@explorer-new-copy", "@explorer", "@smoke"];

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerNewCopyConsoleNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerNewCopyUrl(BASE_URL), { waitUntil: "domcontentloaded" });
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
 * One snapshot of detail rows. Reading attributes together avoids
 * {@code locator.nth(i).getAttribute} waiting out the test timeout when a
 * refresh shrinks the list (#5028).
 *
 * @param {import("@playwright/test").Locator} rows
 * @returns {Promise<{ id: string, name: string }[]>}
 */
async function detailRowMeta(rows) {
  return rows.evaluateAll((els) =>
    els.map((el) => ({
      id: el.getAttribute("data-testid") || "",
      name: (el.getAttribute("data-item-name") || "").trim(),
    })),
  );
}

/**
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<string>}
 */
async function detailSignature(page) {
  const rows = page.locator(
    `[data-testid="${TEST_IDS.detailList}"] tbody tr[data-testid^="detail-row-"]`,
  );
  const meta = await detailRowMeta(rows);
  return meta.map((row) => row.id).join("|");
}

/**
 * Folder open does not bump {@code data-list-epoch}. Wait until the detail
 * rows change so the walk is not scored against the parent listing (#5028).
 *
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} row
 * @param {string} beforeSignature
 */
async function openDetailFolder(page, row, beforeSignature) {
  const icon = row.locator('[data-testid^="detail-folder-icon-"]');
  if ((await icon.count()) > 0) {
    await icon.first().click({ force: true });
  } else {
    await row.dblclick({ force: true });
  }
  await expect
    .poll(async () => detailSignature(page), { timeout: 15_000 })
    .not.toBe(beforeSignature);
  await listReady(page);
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {"page"|"asset"} kind
 */
async function selectFirstContentItem(page, kind) {
  const rootName = kind === "asset" ? "Assets" : "Sites";
  const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
  const root = tree.locator(
    `[data-testid="tree-node-/${rootName}/"], [data-testid="tree-node-/${rootName}"]`,
  );
  await expect(root.first()).toBeVisible({ timeout: 30_000 });
  await root.first().click({ force: true });
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
  const seenFolderIds = new Set();
  for (let depth = 0; depth < 8; depth += 1) {
    const itemRow = list.locator(
      'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
    );
    const folders = list.locator(
      'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
    );
    try {
      await expect
        .poll(async () => (await itemRow.count()) + (await folders.count()), {
          timeout: 20_000,
        })
        .toBeGreaterThan(0);
    } catch {
      return false;
    }
    if ((await itemRow.count()) > 0) {
      await itemRow.first().click({ force: true });
      return true;
    }
    const folderMeta = await detailRowMeta(folders);
    const chosen = pickContentFolderIndex(folderMeta, kind, seenFolderIds);
    if (chosen < 0) {
      return false;
    }
    const id = folderMeta[chosen].id;
    if (id) seenFolderIds.add(id);
    const beforeSignature = await detailSignature(page);
    await openDetailFolder(page, folders.nth(chosen), beforeSignature);
  }
  return false;
}

async function clickNewCopy(page) {
  const create = page
    .locator(`[data-testid="${TEST_IDS.createMenu}"][aria-haspopup="menu"]`)
    .first();
  await expect(create).toBeVisible({ timeout: 20_000 });
  const menuItem = page
    .locator(`[data-testid="${TEST_IDS.newCopy}"][role="menuitem"]`)
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

test.describe("Explorer create a new copy of the selected item (#5006 / #4530)", () => {
  test(
    "UI: confirm on a page creates a new copy and refreshes the folder",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      await page.route("**/itemmanagement/item/newCopy/**", async (route) => {
        posts.push(route.request().url());
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemCopyResult: {
              itemId: "9001",
              folderPath: "//Sites",
              promotable: false,
            },
          }),
        });
      });
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/new copy/i);
        void dialog.accept();
      });
      const jsErrors = await openExplorer(page);
      const found = await selectFirstContentItem(page, "page");
      expect(found, "H2 Explorer has no selectable page").toBe(true);
      const before = await listEpoch(page);
      await clickNewCopy(page);
      await expect.poll(() => posts.length, { timeout: 15_000 }).toBe(1);
      expect(isItemNewCopyUrl(posts[0])).toBe(true);
      await expect
        .poll(async () => listEpoch(page), { timeout: 15_000 })
        .not.toBe(before);
      const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
      const errorText = (await error.count()) > 0 ? await error.innerText() : "";
      expect(isNewCopySuccess(before, await listEpoch(page), errorText)).toBe(true);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.shell}"]`,
      });
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: confirm on an asset creates a new copy and refreshes the folder",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(120_000);
      const stamp = Date.now().toString(36);
      const folderName = `qa5006a${stamp}`;
      const itemName = `qa5006i${stamp}`;
      const folder = await seedDisposableEmptyFolder(
        request,
        BASE_URL,
        adminBasicAuthHeaders(),
        { parentPath: "Assets", name: folderName },
      );
      await seedDisposableAsset(request, BASE_URL, adminBasicAuthHeaders(), {
        parentPath: String(folder.path || `/Assets/${folderName}`),
        name: itemName,
      });
      const posts = [];
      await page.route("**/itemmanagement/item/newCopy/**", async (route) => {
        posts.push(route.request().url());
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ itemId: "9002", folderPath: "//Assets", promotable: false }),
        });
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const jsErrors = await openExplorer(page);
      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      const assets = tree
        .locator('[data-testid="tree-node-/Assets/"], [data-testid="tree-node-/Assets"]')
        .first();
      await expect(assets).toBeVisible({ timeout: 30_000 });
      await assets.click({ force: true });
      await listReady(page);
      const toggle = tree
        .locator('[data-testid="tree-toggle-/Assets/"], [data-testid="tree-toggle-/Assets"]')
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
      const before = await listEpoch(page);
      await clickNewCopy(page);
      await expect.poll(() => posts.length, { timeout: 15_000 }).toBe(1);
      expect(isItemNewCopyUrl(posts[0])).toBe(true);
      const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
      await expect
        .poll(async () => {
          const text = (await error.count()) > 0 ? await error.innerText() : "";
          return isNewCopySuccess(before, await listEpoch(page), text);
        })
        .toBe(true);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: cancel does not create a copy",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      await page.route("**/itemmanagement/item/newCopy/**", async (route) => {
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
      const found = await selectFirstContentItem(page, "page");
      expect(found, "H2 Explorer has no selectable page").toBe(true);
      const before = await listEpoch(page);
      await clickNewCopy(page);
      await page.waitForTimeout(500);
      expect(posts).toEqual([]);
      expect(await listEpoch(page)).toBe(before);
      await expect(page.locator(`[data-testid="${TEST_IDS.serverError}"]`)).toHaveCount(0);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: a folder does not claim a new copy",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      await page.route("**/itemmanagement/item/newCopy/**", async (route) => {
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
      await clickNewCopy(page);
      const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Select a content item first/i);
      expect(posts).toEqual([]);
      expect(await listEpoch(page)).toBe(before);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: an empty selection does not claim a new copy",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      await page.route("**/itemmanagement/item/newCopy/**", async (route) => {
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
      await clickNewCopy(page);
      const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
      await expect(error).toContainText(/Select a content item first/i);
      expect(posts).toEqual([]);
      expect(await listEpoch(page)).toBe(before);
      expect(isNewCopySuccess(before, await listEpoch(page), "")).toBe(false);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `UI: HTTP ${status} does not claim a new copy`,
      { tag: TAGS },
      async ({ page }) => {
        test.setTimeout(120_000);
        let posts = 0;
        await page.route("**/itemmanagement/item/newCopy/**", async (route) => {
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
        const found = await selectFirstContentItem(page, "page");
        expect(found, "H2 Explorer has no selectable page").toBe(true);
        const before = await listEpoch(page);
        await clickNewCopy(page);
        await expect.poll(() => posts, { timeout: 15_000 }).toBe(1);
        const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
        await expect(error).toBeVisible({ timeout: 10_000 });
        const text = await error.innerText();
        expect(isNewCopyHttpFailure(text, status)).toBe(true);
        expect(await listEpoch(page)).toBe(before);
        expect(jsErrors, jsErrors.join("\n")).toEqual([]);
      },
    );
  }
});
