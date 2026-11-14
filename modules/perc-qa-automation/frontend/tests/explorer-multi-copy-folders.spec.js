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
 * Playwright surface: #6255 / parent #4530 — Content → Copy selected to
 * folder deep-copies checked folders.
 *
 * One confirm posts each checked folder to POST /folders/copy/folder (the
 * server deep-copies the tree and uniquifies conflicts with
 * getUniqueFolderName), names the created copy in the result, and cancel
 * copies nothing.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-multi-copy-folders.spec.js
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
  isFoldersCopyFolderUrl,
  isFoldersCopyItemUrl,
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
const { listFolderChildren } = require("./helpers/empty-recycling");

const TAGS = ["@explorer-multi-copy-folders", "@explorer", "@item", "@smoke"];

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

/**
 * @param {Array<{ name?: string }>} items
 */
function childNames(items) {
  return items.map((item) => String(item.name || ""));
}

test.describe("Explorer multi-select folder deep-copy (#6255)", () => {
  test(
    "UI: one confirm deep-copies folder trees, uniquifies conflicts, cancel copies nothing",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(240_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });

      /** @type {string[]} */
      const copyFolderUrls = [];
      /** @type {string[]} */
      const copyItemUrls = [];
      page.on("request", (req) => {
        const url = req.url();
        if (req.method() === "OPTIONS") return;
        if (isFoldersCopyFolderUrl(url)) copyFolderUrls.push(url);
        if (isFoldersCopyItemUrl(url)) copyItemUrls.push(url);
      });

      const stamp = Date.now();
      const sourceName = uniqueCopyItemName("qa6255src", stamp);
      const destName = uniqueCopyItemName("qa6255dst", stamp);
      const folderName = uniqueCopyItemName("qa6255fld", stamp);
      const nestedName = uniqueCopyItemName("qa6255nest", stamp);
      /** @type {{ path?: string } | null} */
      let sourceFolder = null;
      /** @type {{ path?: string } | null} */
      let destFolder = null;

      try {
        expect(await getStatus(request, assetsFolderUrl(BASE_URL))).toBe(200);
        destFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: destName },
        );
        sourceFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: sourceName },
        );
        const sourcePath = String(
          sourceFolder.path || `/Assets/${sourceName}`,
        ).replace(/\/+$/, "");
        const destPath = String(destFolder.path || `/Assets/${destName}`).replace(
          /\/+$/,
          "",
        );
        const nestedFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: sourcePath, name: folderName },
        );
        await seedDisposableAsset(request, BASE_URL, adminBasicAuthHeaders(), {
          parentPath: String(nestedFolder.path || `${sourcePath}/${folderName}`),
          name: nestedName,
        });

        await loginAsAdmin(page);
        const explorerUrl = explorerProductCopyItemUrl(BASE_URL);
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

        await checkRow(page, list, folderName);

        await page.locator('[data-testid="explorer-menu-content"]').click();
        await page.locator('[data-testid="explorer-multi-copy"]').click();
        const destInput = page.locator('[data-testid="explorer-copy-dest-input"]');
        await expect(destInput).toBeVisible();
        await destInput.fill(destPath);
        await page.locator('[data-testid="explorer-copy-dest-ok"]').click();

        const result = page.locator('[data-testid="explorer-multi-copy-result"]');
        await expect(result).toBeVisible({ timeout: 30_000 });
        await expect(result).toHaveAttribute("data-outcome", "success");
        await expect(result).toContainText(folderName);
        expect(
          copyFolderUrls.length,
          "checked folders are deep-copied via copy/folder",
        ).toBeGreaterThanOrEqual(1);

        const destChildren = await listFolderChildren(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          destPath,
        );
        expect(childNames(destChildren)).toContain(folderName);
        const copiedTree = await listFolderChildren(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          `${destPath}/${folderName}`,
        );
        expect(childNames(copiedTree)).toContain(nestedName);

        await page.locator('[data-testid="explorer-menu-content"]').click();
        await page.locator('[data-testid="explorer-multi-copy"]').click();
        const destInputAgain = page.locator(
          '[data-testid="explorer-copy-dest-input"]',
        );
        await expect(destInputAgain).toBeVisible();
        await destInputAgain.fill(destPath);
        await page.locator('[data-testid="explorer-copy-dest-ok"]').click();
        await expect(result).toBeVisible({ timeout: 30_000 });
        await expect(result).toHaveAttribute("data-outcome", "success");
        await expect(result).toContainText(folderName);
        const destChildrenAgain = await listFolderChildren(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          destPath,
        );
        const sameRoot = childNames(destChildrenAgain).filter((name) =>
          name.startsWith(folderName),
        );
        expect(
          sameRoot.length,
          "conflict destination keeps the server-uniquified copy",
        ).toBeGreaterThanOrEqual(2);
        const uniquified = sameRoot.find((name) => name !== folderName);
        expect(uniquified).toBeTruthy();
        await expect(result).toContainText(String(uniquified));

        const folderPostsBefore = copyFolderUrls.length;
        const itemPostsBefore = copyItemUrls.length;
        await page.locator('[data-testid="explorer-menu-content"]').click();
        await page.locator('[data-testid="explorer-multi-copy"]').click();
        await expect(
          page.locator('[data-testid="explorer-copy-dest-input"]'),
        ).toBeVisible();
        await page.locator('[data-testid="explorer-copy-dest-cancel"]').click();
        await expect(
          page.locator('[data-testid="explorer-copy-dest-picker"]'),
        ).toBeHidden({ timeout: 10_000 });
        expect(copyFolderUrls.length).toBe(folderPostsBefore);
        expect(copyItemUrls.length).toBe(itemPostsBefore);

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${COPY_TEST_IDS.shell}"]`,
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (destFolder && destFolder.path) {
          await recycleFolderWithItems(
            request,
            BASE_URL,
            adminBasicAuthHeaders(),
            destFolder,
          ).catch(() => undefined);
        }
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
