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
 * Playwright surface: #4602 / parent #4530 — Explorer Delete selected
 * non-folder item on spa.jsp?entry=explorer without rxFolderMutations=1.
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  DELETE_ITEM_TEST_IDS,
  explorerProductDeleteItemUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersDeleteItemUrl,
  isPathmanagementDeleteFolderUrl,
  isRxContentExplorerFoldersUrl,
  isDeleteItemSuccessStatus,
  uniqueDeleteItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-delete-item");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-delete-item", "@explorer", "@item", "@smoke"];

async function getStatus(request, url) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(url, { headers });
  return res.status();
}

test.describe("Explorer Delete Item on product route (#4602 / #4530)", () => {
  test(
    "REST: Assets parent exists (no skip)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(30_000);
      const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
      expect(
        assetsStatus,
        `H2 demo-sites should expose /Assets; got Assets=${assetsStatus}`,
      ).toBe(200);
    },
  );

  test(
    "UI: Delete selected item on spa.jsp?entry=explorer without rxFolderMutations",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(120_000);

      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      /** @type {Array<{ url: string, method: string }>} */
      const mutations = [];
      page.on("request", (req) => {
        const url = req.url();
        if (
          (isFoldersDeleteItemUrl(url) ||
            isPathmanagementDeleteFolderUrl(url) ||
            isRxContentExplorerFoldersUrl(url)) &&
          req.method() !== "OPTIONS"
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const stamp = Date.now();
      const folderName = uniqueDeleteItemName("qa4602fld", stamp);
      const itemName = uniqueDeleteItemName("qa4602itm", stamp);
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let folder = null;
      /** @type {{ path?: string, name?: string } | null} */
      let sourceItem = null;
      let deletedViaUi = false;

      try {
        const assetsStatus = await getStatus(
          request,
          assetsFolderUrl(BASE_URL),
        );
        expect(
          assetsStatus,
          `H2 Explorer Assets parent must exist; Assets=${assetsStatus}`,
        ).toBe(200);

        folder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: folderName },
        );
        sourceItem = await seedDisposableAsset(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          {
            parentPath: String(folder.path || `/Assets/${folderName}`),
            name: itemName,
          },
        );
        expect(
          String(sourceItem.name || itemName).startsWith("qa4602"),
          `source item must be this test's qa4602* name, got ${sourceItem.name}`,
        ).toBe(true);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductDeleteItemUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(
          `[data-testid="${DELETE_ITEM_TEST_IDS.shell}"]`,
        );
        await expect(shell).toBeVisible({ timeout: 20_000 });
        const list = page.locator(
          `[data-testid="${DELETE_ITEM_TEST_IDS.detailList}"]`,
        );

        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(assetsNode).toBeVisible({ timeout: 20_000 });
        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        await list.waitFor({ timeout: 15_000 });

        const folderRow = list.getByText(folderName, { exact: true });
        await expect(folderRow).toBeVisible({ timeout: 20_000 });
        await folderRow.click();
        const openBtn = page.locator('[data-testid="action-open"]');
        await expect(openBtn).toBeEnabled();
        await openBtn.click();
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const itemRow = list.getByText(sourceItem.name, { exact: true });
        await expect(itemRow).toBeVisible({ timeout: 20_000 });
        await itemRow.click();

        const deleteBtn = page.locator(
          `[data-testid="${DELETE_ITEM_TEST_IDS.actionDelete}"]`,
        );
        await expect(deleteBtn).toBeEnabled();

        page.once("dialog", async (dialog) => {
          await dialog.accept();
        });

        const deleteRespPromise = page.waitForResponse(
          (res) =>
            isFoldersDeleteItemUrl(res.url()) &&
            res.request().method() === "DELETE",
          { timeout: 30_000 },
        );

        await deleteBtn.click();
        const deleteResp = await deleteRespPromise;
        expect(
          isDeleteItemSuccessStatus(deleteResp.status()),
          `folders/item DELETE expected 200, got ${deleteResp.status()} ${deleteResp.url()}`,
        ).toBe(true);
        deletedViaUi = true;

        const folderDelHits = mutations.filter((m) =>
          isPathmanagementDeleteFolderUrl(m.url),
        );
        expect(folderDelHits).toEqual([]);
        const rxHits = mutations.filter((m) =>
          isRxContentExplorerFoldersUrl(m.url),
        );
        expect(rxHits).toEqual([]);

        await expect(
          list.getByText(sourceItem.name, { exact: true }),
        ).toHaveCount(0, { timeout: 20_000 });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${DELETE_ITEM_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (folder && folder.path) {
          await recycleFolderWithItems(
            request,
            BASE_URL,
            adminBasicAuthHeaders(),
            folder,
          ).catch(() => undefined);
        }
        void deletedViaUi;
      }
    },
  );
});
