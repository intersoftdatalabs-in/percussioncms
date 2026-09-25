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
 * Playwright surface: #4857 / parent #4530 — Content → Recycle selected.
 *
 * One confirm recycles checked pages/assets, names skipped folders, and
 * does not report a partial HTTP failure as full success.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-multi-recycle.spec.js
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
  hasRxFolderMutationsQuery,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-item");
const {
  isFoldersDeleteItemUrl,
  isPathmanagementDeleteFolderUrl,
} = require("./helpers/explorer-delete-item");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-multi-recycle", "@explorer", "@item", "@smoke"];

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} url
 */
async function getStatus(request, url) {
  const res = await request.get(url, { headers: adminBasicAuthHeaders() });
  return res.status();
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} list
 * @param {string} name
 */
async function checkRow(page, list, name) {
  const row = list.locator("tr", { hasText: name }).first();
  await expect(row).toBeVisible({ timeout: 20_000 });
  await row.locator('input[type="checkbox"]').check();
  void page;
}

test.describe("Explorer multi-select recycle (#4857)", () => {
  test(
    "UI: one confirm recycles pages, names skipped folders, partial failure stays visible",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });

      /** @type {string[]} */
      const deleteItemUrls = [];
      /** @type {string[]} */
      const deleteFolderUrls = [];
      page.on("request", (req) => {
        const url = req.url();
        if (req.method() === "OPTIONS") return;
        if (req.method() === "DELETE" && isFoldersDeleteItemUrl(url)) {
          deleteItemUrls.push(url);
        }
        if (
          req.method() === "POST" &&
          isPathmanagementDeleteFolderUrl(url)
        ) {
          deleteFolderUrls.push(url);
        }
      });

      const stamp = Date.now();
      const sourceName = uniqueCopyItemName("qa4857src", stamp);
      const folderName = uniqueCopyItemName("qa4857fld", stamp);
      const okName = uniqueCopyItemName("qa4857ok", stamp);
      const missName = uniqueCopyItemName("qa4857miss", stamp);
      /** @type {{ path?: string } | null} */
      let sourceFolder = null;
      /** @type {{ path?: string } | null} */
      let nestedFolder = null;

      try {
        expect(await getStatus(request, assetsFolderUrl(BASE_URL))).toBe(200);
        sourceFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: sourceName },
        );
        const sourcePath = String(sourceFolder.path || `/Assets/${sourceName}`);
        nestedFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: sourcePath, name: folderName },
        );
        await seedDisposableAsset(request, BASE_URL, adminBasicAuthHeaders(), {
          parentPath: sourcePath,
          name: okName,
        });
        await seedDisposableAsset(request, BASE_URL, adminBasicAuthHeaders(), {
          parentPath: sourcePath,
          name: missName,
        });

        let itemDeletes = 0;
        await page.route(/\/folders\/item\//, async (route) => {
          if (route.request().method() !== "DELETE") {
            await route.continue();
            return;
          }
          itemDeletes += 1;
          const url = route.request().url();
          if (itemDeletes >= 2 || url.includes(missName)) {
            await route.fulfill({
              status: 404,
              contentType: "application/json",
              body: "{}",
            });
            return;
          }
          await route.continue();
        });

        await loginAsAdmin(page);
        const explorerUrl = explorerProductCopyItemUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);
        await page.goto(explorerUrl, { waitUntil: "networkidle" });
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

        await checkRow(page, list, okName);
        await checkRow(page, list, missName);
        await checkRow(page, list, folderName);

        await page.locator('[data-testid="explorer-menu-content"]').click();
        await page.locator('[data-testid="explorer-multi-recycle"]').click();
        await page.locator('[data-testid="explorer-recycle-ok"]').click();

        const result = page.locator(
          '[data-testid="explorer-multi-recycle-result"]',
        );
        await expect(result).toBeVisible({ timeout: 30_000 });
        await expect(result).toHaveAttribute("data-outcome", "partial");
        await expect(result).toContainText(folderName);
        await expect(result).toContainText(missName);
        expect(
          deleteFolderUrls,
          "folders must not be posted to path deleteFolder",
        ).toEqual([]);
        expect(deleteItemUrls.length).toBeGreaterThanOrEqual(1);

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${COPY_TEST_IDS.shell}"]`,
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
        void nestedFolder;
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
});
