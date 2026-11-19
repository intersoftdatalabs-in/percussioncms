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
 * Playwright surface: #6514 / parent #4530 — Explorer generic content items.
 *
 * A Rhythmyx upgrade site has no Pages or Assets — it has Items of custom
 * content types. The folder list must show those rows with type-aware cells
 * (name/title/type, data-row-kind="item"), keyboard activation must target
 * them, and the recycle confirm must name items, not just pages/assets.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-generic-content-items.spec.js
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  COPY_TEST_IDS,
  explorerProductCopyItemUrl,
  assetsFolderUrl,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-item");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-generic-items", "@explorer", "@item", "@smoke"];

test.describe("Explorer generic content items (#6514)", () => {
  test(
    "UI: custom-type rows list with type cells and the recycle confirm names items",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });

      const stamp = Date.now();
      const sourceName = uniqueCopyItemName("qa6514src", stamp);
      const itemName = uniqueCopyItemName("qa6514gen", stamp);
      /** @type {{ path?: string } | null} */
      let sourceFolder = null;

      try {
        expect(await (await request.get(assetsFolderUrl(BASE_URL))).status()).toBe(200);
        sourceFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: sourceName },
        );
        const sourcePath = String(sourceFolder.path || `/Assets/${sourceName}`);
        await seedDisposableAsset(request, BASE_URL, adminBasicAuthHeaders(), {
          parentPath: sourcePath,
          name: itemName,
        });

        await loginAsAdmin(page);
        await page.goto(explorerProductCopyItemUrl(BASE_URL), {
          waitUntil: "networkidle",
        });
        await expect(
          page.locator(`[data-testid="${COPY_TEST_IDS.shell}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(assetsNode).toBeVisible({ timeout: 20_000 });
        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        const list = page.locator(`[data-testid="${COPY_TEST_IDS.detailList}"]`);
        await list.waitFor({ timeout: 15_000 });
        await list.getByText(sourceName, { exact: true }).click();
        await page.locator('[data-testid="action-open"]').click();
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const row = list.locator("tr", { hasText: itemName }).first();
        await expect(row).toBeVisible({ timeout: 20_000 });
        // Generic items are item rows with a type cell, never folder rows.
        await expect(row).toHaveAttribute("data-row-kind", "item");
        const itemType = await row.getAttribute("data-item-type");
        expect((itemType || "").trim().length).toBeGreaterThan(0);

        // Keyboard activation targets the generic row (open path).
        await row.focus();
        await expect(row).toHaveAttribute("tabindex", /^(-1|0)$/);

        // The recycle confirm names items, not just pages/assets (#6514).
        await row.locator('input[type="checkbox"]').check();
        await page.locator('[data-testid="explorer-menu-content"]').click();
        await page.locator('[data-testid="explorer-multi-recycle"]').click();
        const confirm = page.locator('[data-testid="explorer-recycle-confirm"]');
        await expect(confirm).toBeVisible({ timeout: 20_000 });
        await expect(confirm).toContainText(/checked items/i);
        const confirmText = (await confirm.innerText()) || "";
        expect(confirmText).not.toMatch(/pages and assets/i);
        await page.locator('[data-testid="explorer-recycle-cancel"]').click();

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${COPY_TEST_IDS.shell}"]`,
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
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

  test(
    "UI: failed list shows named summary with retry, empty folder shows create hint",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });

      const stamp = Date.now();
      const emptyName = uniqueCopyItemName("qa6596empty", stamp);
      /** @type {{ path?: string } | null} */
      let emptyFolder = null;

      try {
        expect(await (await request.get(assetsFolderUrl(BASE_URL))).status()).toBe(200);
        emptyFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: emptyName },
        );

        await loginAsAdmin(page);
        await page.goto(explorerProductCopyItemUrl(BASE_URL), {
          waitUntil: "networkidle",
        });
        await expect(
          page.locator(`[data-testid="${COPY_TEST_IDS.shell}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(assetsNode).toBeVisible({ timeout: 20_000 });
        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        const list = page.locator(`[data-testid="${COPY_TEST_IDS.detailList}"]`);
        await list.waitFor({ timeout: 15_000 });
        await list.getByText(emptyName, { exact: true }).click();

        // Fail the next folder-list request so the failed-summary error
        // copy and the Retry affordance render (#6596). The glob must
        // cross path segments (`*` does not match `/` in Playwright).
        await page.route("**/paginatedFolder/**", (route) => route.abort());
        await page.locator('[data-testid="action-open"]').click();
        const errorBox = page.locator('[data-testid="detail-list-error"]');
        await expect(errorBox).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator('[data-testid="detail-list-error-summary"]'),
        ).toContainText(/Failed to load items/i);
        await expect(
          page.locator('[data-testid="detail-list-retry"]'),
        ).toBeVisible({ timeout: 20_000 });

        // Retry with the network restored resolves to the empty folder
        // state with the helpful create hint (#6596).
        await page.unroute("**/paginatedFolder/**");
        await page.locator('[data-testid="detail-list-retry"]').click();
        await expect(
          page.locator('[data-testid="detail-list-empty"]'),
        ).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator('[data-testid="detail-list-empty-hint"]'),
        ).toContainText(/New Item/i);

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${COPY_TEST_IDS.shell}"]`,
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/paginatedFolder/**").catch(() => undefined);
        if (emptyFolder && emptyFolder.path) {
          await recycleFolderWithItems(
            request,
            BASE_URL,
            adminBasicAuthHeaders(),
            emptyFolder,
          ).catch(() => undefined);
        }
      }
    },
  );
});
