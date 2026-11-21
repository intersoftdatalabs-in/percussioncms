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
 * Playwright surface: #6864 (parent #6826) — closing the server context
 * menu returns focus to the invoking row instead of stranding keyboard
 * operators on {@code <body>}.
 *
 * <p>Covers both row kinds and both open paths: a pointer right-click on a
 * seeded detail row, and a keyboard (Shift+F10) open on the Sites tree row.
 * Each arc opens the popup, presses Escape, and asserts the menu unmounts
 * with DOM focus back on the invoking row.</p>
 *
 * <p>Tags: {@code @explorer-focus-restore} {@code @explorer} {@code @a11y}</p>
 *
 * <p>Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/bugs/bug-6864-context-menu-focus-restore.spec.js
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

"use strict";

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");
const { expectNoSeriousA11yViolations } = require("../helpers/a11y");
const { explorerSpaUrl } = require("../helpers/explorer-menu-bar");
const {
  uniqueCopyItemName,
  assetsFolderUrl,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  isKnownExplorerSitesConsoleNoise,
} = require("../helpers/explorer-copy-item");

const TAGS = ["@explorer-focus-restore", "@explorer", "@a11y"];

/**
 * Single synthetic item row for the seeded id (same shape as the #6874
 * catalog-error spec — deterministic regardless of H2 seed content).
 */
function singleItemListBody(id, name) {
  return JSON.stringify({
    PagedItemList: {
      childrenInPage: [
        {
          id: String(id),
          name,
          path: `/ContentLib/${name}`,
          accessLevel: "WRITE",
          leaf: true,
        },
      ],
      childrenCount: 1,
      startIndex: 0,
    },
  });
}

test.describe("Explorer context-menu close restores row focus (#6864)", () => {
  test(
    "right-click detail row + Shift+F10 tree row: Escape returns focus to the invoker",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") return;
        const text = msg.text();
        if (
          /Failed to load resource: the server responded with a status of (404|400|500)/i.test(
            text,
          )
        ) {
          return;
        }
        jsErrors.push(text);
      });
      const stamp = Date.now();
      /** @type {{ path?: string } | null} */
      let sourceFolder = null;
      try {
        expect(await (await request.get(assetsFolderUrl(BASE_URL))).status()).toBe(
          200,
        );
        const folderName = uniqueCopyItemName("qa6864f", stamp);
        const itemName = uniqueCopyItemName("qa6864m", stamp);
        sourceFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: folderName },
        );
        const sourcePath = String(sourceFolder.path || `/Assets/${folderName}`);
        const seeded = await seedDisposableAsset(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          {
            parentPath: sourcePath,
            name: itemName,
            contentTypes: ["rffFile", "percFile"],
          },
        );
        const id = String(seeded.guid);
        await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: singleItemListBody(id, itemName),
          });
        });

        await loginAsAdmin(page);
        await page.goto(explorerSpaUrl(BASE_URL));
        await page.waitForLoadState("networkidle");
        await expect(
          page.locator('[data-testid="content-explorer-shell"]'),
        ).toBeVisible({ timeout: 20_000 });

        // Arc 1: pointer right-click on the detail row.
        const itemRow = page.locator(
          `[data-testid="detail-row-${id}"][data-row-kind="item"]`,
        );
        await expect(itemRow).toBeVisible({ timeout: 20_000 });
        await itemRow.focus();
        await expect(itemRow).toBeFocused();
        await itemRow.click({ button: "right" });
        const menu = page.locator('[data-testid="context-menu"]');
        await expect(menu).toBeVisible({ timeout: 15_000 });
        await expect(menu).toBeFocused({ timeout: 15_000 });
        await page.screenshot({
          path: `test-results/live-proof-6864-context-menu-open.png`,
        });

        await page.keyboard.press("Escape");
        await expect(menu).toHaveCount(0, { timeout: 10_000 });
        await expect(itemRow).toBeFocused({ timeout: 10_000 });

        // Arc 2: keyboard (Shift+F10) open on the Sites tree row.
        const treeRow = page
          .locator(
            '[data-testid="tree-node-/Sites/"] [role="treeitem"], [data-testid="tree-node-/Sites"] [role="treeitem"]',
          )
          .first();
        await expect(treeRow).toBeVisible({ timeout: 20_000 });
        await treeRow.focus();
        await expect(treeRow).toBeFocused();
        await page.keyboard.press("Shift+F10");
        await expect(menu).toBeVisible({ timeout: 15_000 });
        // Let the row catalog settle (cold-cell first load is slow) before
        // asserting focus, mirroring the Vitest arc that waits for items.
        await expect(
          menu.locator('[data-testid^="context-menu-item-"]').first(),
        ).toBeVisible({ timeout: 15_000 });
        await expect(menu).toBeFocused({ timeout: 15_000 });
        await page.keyboard.press("Escape");
        await expect(menu).toHaveCount(0, { timeout: 10_000 });
        await expect(treeRow).toBeFocused({ timeout: 10_000 });
        await page.screenshot({
          path: `test-results/live-proof-6864-context-menu-focus-restored.png`,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/pathmanagement/path/paginatedFolder**").catch(() => undefined);
        if (sourceFolder && sourceFolder.path) {
          await recycleFolderWithItems(
            request,
            BASE_URL,
            adminBasicAuthHeaders(),
            sourceFolder,
          ).catch(() => undefined);
        }
      }
    },
  );
});
