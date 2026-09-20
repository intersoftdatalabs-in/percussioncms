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
 * Playwright surface: #4636 / parent #4530 — Explorer Rename selected
 * non-folder item on the product route ({@code spa.jsp?entry=explorer})
 * without {@code rxFolderMutations=1}.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: Assets parent exists on H2 (no skip)</li>
 *   <li>UI: ReducedActions Rename of a disposable asset →
 *       {@code POST /rest/folders/rename/item} HTTP 200</li>
 *   <li>UI: new name appears in detail-list without View→Refresh</li>
 *   <li>Must not POST pathmanagement renameFolder (folder rename)</li>
 *   <li>Must not POST content-explorer/folders (flag off)</li>
 * </ul>
 *
 * <p>Tags: {@code @explorer-rename-item} {@code @explorer} {@code @item}
 * {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-rename-item.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  RENAME_ITEM_TEST_IDS,
  explorerProductRenameItemUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersRenameItemUrl,
  isPathmanagementRenameFolderUrl,
  isRxContentExplorerFoldersUrl,
  isRenameItemSuccessStatus,
  isRenameFolderItemRequestEnvelope,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-rename-item");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-rename-item", "@explorer", "@item", "@smoke"];

async function getStatus(request, url) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(url, { headers });
  return res.status();
}

test.describe("Explorer Rename Item on product route (#4636 / #4530)", () => {
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
    "UI: Rename selected item on spa.jsp?entry=explorer without rxFolderMutations",
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
          (isFoldersRenameItemUrl(url) ||
            isPathmanagementRenameFolderUrl(url) ||
            isRxContentExplorerFoldersUrl(url)) &&
          req.method() !== "OPTIONS"
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const stamp = Date.now();
      const folderName = uniqueCopyItemName("qa4636fld", stamp);
      const itemName = uniqueCopyItemName("qa4636itm", stamp);
      const newName = uniqueCopyItemName("qa4636ren", stamp);
      /** @type {{ path?: string, name?: string } | null} */
      let liveFolder = null;
      /** @type {{ path?: string, name?: string } | null} */
      let sourceItem = null;

      try {
        const assetsStatus = await getStatus(
          request,
          assetsFolderUrl(BASE_URL),
        );
        expect(
          assetsStatus,
          `H2 Explorer Assets parent must exist; Assets=${assetsStatus}`,
        ).toBe(200);

        liveFolder = await seedDisposableEmptyFolder(
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
            parentPath: String(liveFolder.path || `/Assets/${folderName}`),
            name: itemName,
          },
        );
        expect(
          String(sourceItem.name || itemName).startsWith("qa4636"),
          `source item must be this test's qa4636* name, got ${sourceItem.name}`,
        ).toBe(true);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductRenameItemUrl(BASE_URL);
        expect(
          hasRxFolderMutationsQuery(explorerUrl),
          "product route must not set rxFolderMutations=1",
        ).toBe(false);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(
          `[data-testid="${RENAME_ITEM_TEST_IDS.shell}"]`,
        );
        await expect(shell).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator(`[data-testid="${RENAME_ITEM_TEST_IDS.tree}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(
          assetsNode,
          "H2 Explorer tree must show Assets for Rename Item",
        ).toBeVisible({ timeout: 20_000 });

        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${RENAME_ITEM_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const folderRow = list.getByText(folderName, { exact: true });
        await expect(
          folderRow,
          `seeded ${folderName} must appear under Assets before Rename`,
        ).toBeVisible({ timeout: 20_000 });
        await folderRow.click();
        const openBtn = page.locator('[data-testid="action-open"]');
        await expect(openBtn).toBeEnabled();
        await openBtn.click();
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const itemRow = list.getByText(sourceItem.name, { exact: true });
        await expect(
          itemRow,
          `seeded item ${sourceItem.name} must appear under ${folderName}`,
        ).toBeVisible({ timeout: 20_000 });
        await itemRow.click();

        const renameBtn = page.locator(
          `[data-testid="${RENAME_ITEM_TEST_IDS.actionRename}"]`,
        );
        await expect(renameBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          renameBtn,
          `Rename must be enabled for selected item ${sourceItem.name}`,
        ).toBeEnabled();

        page.once("dialog", async (dialog) => {
          await dialog.accept(newName);
        });

        const renameRespPromise = page.waitForResponse(
          (res) =>
            isFoldersRenameItemUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );

        await renameBtn.click();
        const renameResp = await renameRespPromise;
        expect(
          isRenameItemSuccessStatus(renameResp.status()),
          `rename/item expected 200, got ${renameResp.status()} ${renameResp.url()}`,
        ).toBe(true);
        const renameBody = renameResp.request().postDataJSON();
        expect(
          isRenameFolderItemRequestEnvelope(renameBody),
          `rename must wrap RenameFolderItemRequest: ${JSON.stringify(renameBody)}`,
        ).toBe(true);

        const folderRenameHits = mutations.filter((m) =>
          isPathmanagementRenameFolderUrl(m.url),
        );
        expect(
          folderRenameHits,
          `non-folder Rename must not POST pathmanagement renameFolder: ${JSON.stringify(folderRenameHits)}`,
        ).toEqual([]);

        const rxHits = mutations.filter((m) =>
          isRxContentExplorerFoldersUrl(m.url),
        );
        expect(
          rxHits,
          `product route must not call content-explorer/folders (flag off): ${JSON.stringify(rxHits)}`,
        ).toEqual([]);

        await expect(
          list.getByText(newName, { exact: true }),
          `list must show renamed item ${newName} without View→Refresh`,
        ).toBeVisible({ timeout: 20_000 });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${RENAME_ITEM_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (liveFolder && liveFolder.path) {
          await recycleFolderWithItems(
            request,
            BASE_URL,
            adminBasicAuthHeaders(),
            liveFolder,
          ).catch(() => undefined);
        }
      }
    },
  );
});
