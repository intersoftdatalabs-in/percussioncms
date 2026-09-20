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
 * Playwright surface: #4601 / parent #4530 — Explorer Move selected
 * non-folder item on the product route ({@code spa.jsp?entry=explorer})
 * without {@code rxFolderMutations=1}.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: Assets parent exists on H2 (no skip)</li>
 *   <li>UI: ReducedActions Move of a disposable asset →
 *       {@code POST /rest/folders/move/item} HTTP 200</li>
 *   <li>UI: moved item appears in destination detail-list without View→Refresh</li>
 *   <li>Must not POST {@code /folders/move/folder} (move folder is for folders)</li>
 *   <li>Must not POST pathmanagement moveItem (slice 6 legacy)</li>
 *   <li>Must not POST content-explorer/folders (flag off)</li>
 *   <li>Does not move golden sample pages</li>
 * </ul>
 *
 * <p>Out of scope: folder Move (#3655), Copy (#3656), Subfolder Copy wizard,
 * clipboard paste, tree-refresh residuals (#3652 / #3653).</p>
 *
 * <p>Tags: {@code @explorer-move-item} {@code @explorer} {@code @item}
 * {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-move-item.spec.js}
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
  MOVE_ITEM_TEST_IDS,
  explorerProductMoveItemUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersMoveFolderUrl,
  isFoldersMoveItemUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isMoveItemSuccessStatus,
  uniqueCopyItemName,
  isMoveFolderItemEnvelope,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-move-item");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-move-item", "@explorer", "@item", "@smoke"];

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} url
 * @returns {Promise<number>}
 */
async function getStatus(request, url) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(url, { headers });
  return res.status();
}

test.describe("Explorer Move Item on product route (#4601 / #4530)", () => {
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
    "UI: Move selected item on spa.jsp?entry=explorer without rxFolderMutations",
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
          (isFoldersMoveFolderUrl(url) ||
            isFoldersMoveItemUrl(url) ||
            isPathmanagementMoveItemUrl(url) ||
            isRxContentExplorerFoldersUrl(url)) &&
          req.method() !== "OPTIONS"
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const stamp = Date.now();
      const sourceFolderName = uniqueCopyItemName("qa4601src", stamp);
      const destFolderName = uniqueCopyItemName("qa4601dst", stamp);
      const itemName = uniqueCopyItemName("qa4601itm", stamp);
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let sourceFolder = null;
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let destFolder = null;
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let sourceItem = null;
      let movedViaUi = false;

      try {
        const assetsStatus = await getStatus(
          request,
          assetsFolderUrl(BASE_URL),
        );
        expect(
          assetsStatus,
          `H2 Explorer Assets parent must exist; Assets=${assetsStatus}`,
        ).toBe(200);

        destFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: destFolderName },
        );
        sourceFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: sourceFolderName },
        );
        sourceItem = await seedDisposableAsset(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          {
            parentPath: String(sourceFolder.path || `/Assets/${sourceFolderName}`),
            name: itemName,
          },
        );
        expect(
          String(sourceItem.name || itemName).startsWith("qa4601"),
          `source item must be this test's qa4601* name, got ${sourceItem.name}`,
        ).toBe(true);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductMoveItemUrl(BASE_URL);
        expect(
          hasRxFolderMutationsQuery(explorerUrl),
          "product route must not set rxFolderMutations=1",
        ).toBe(false);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(`[data-testid="${MOVE_ITEM_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator(`[data-testid="${MOVE_ITEM_TEST_IDS.tree}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(
          assetsNode,
          "H2 Explorer tree must show Assets for Move Item",
        ).toBeVisible({ timeout: 20_000 });

        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${MOVE_ITEM_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const sourceFolderRow = list.getByText(sourceFolderName, {
          exact: true,
        });
        await expect(
          sourceFolderRow,
          `seeded ${sourceFolderName} must appear under Assets before Move`,
        ).toBeVisible({ timeout: 20_000 });
        await sourceFolderRow.click();
        const openBtn = page.locator('[data-testid="action-open"]');
        await expect(openBtn).toBeEnabled();
        await openBtn.click();
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const itemRow = list.getByText(sourceItem.name, { exact: true });
        await expect(
          itemRow,
          `seeded item ${sourceItem.name} must appear under ${sourceFolderName}`,
        ).toBeVisible({ timeout: 20_000 });
        await itemRow.click();

        const moveBtn = page.locator(
          `[data-testid="${MOVE_ITEM_TEST_IDS.actionMove}"]`,
        );
        await expect(moveBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          moveBtn,
          `Move must be enabled for selected item ${sourceItem.name}`,
        ).toBeEnabled();

        const destPath = String(
          destFolder.path || `/Assets/${destFolderName}`,
        );
        const moveRespPromise = page.waitForResponse(
          (res) =>
            isFoldersMoveItemUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );

        await moveBtn.click();
        const destInput = page.locator('[data-testid="explorer-move-dest-input"]');
        await expect(destInput).toBeVisible({ timeout: 10_000 });
        await destInput.fill(destPath);
        await page.locator('[data-testid="explorer-move-dest-ok"]').click();
        const moveResp = await moveRespPromise;
        expect(
          isMoveItemSuccessStatus(moveResp.status()),
          `move/item expected 200, got ${moveResp.status()} ${moveResp.url()}`,
        ).toBe(true);
        const moveBody = moveResp.request().postDataJSON();
        expect(
          isMoveFolderItemEnvelope(moveBody),
          `move must wrap MoveFolderItem: ${JSON.stringify(moveBody)}`,
        ).toBe(true);
        movedViaUi = true;

        const folderMoveHits = mutations.filter((m) =>
          isFoldersMoveFolderUrl(m.url),
        );
        expect(
          folderMoveHits,
          `non-folder Move must not POST move/folder: ${JSON.stringify(folderMoveHits)}`,
        ).toEqual([]);

        const rxHits = mutations.filter((m) =>
          isRxContentExplorerFoldersUrl(m.url),
        );
        expect(
          rxHits,
          `product route must not call content-explorer/folders (flag off): ${JSON.stringify(rxHits)}`,
        ).toEqual([]);

        const pathmanagementHits = mutations.filter((m) =>
          isPathmanagementMoveItemUrl(m.url),
        );
        expect(
          pathmanagementHits,
          `Move must not POST pathmanagement/path/moveItem (#4601 REST contract): ${JSON.stringify(pathmanagementHits)}`,
        ).toEqual([]);

        const tree = page.locator(`[data-testid="${MOVE_ITEM_TEST_IDS.tree}"]`);
        const destNode = tree.locator(`[data-testid*="${destFolderName}"]`).first();
        await expect(
          destNode,
          `destination folder ${destFolderName} must be visible after Move`,
        ).toBeVisible({ timeout: 15_000 });
        const destTreeItem = destNode.locator('[role="treeitem"]').first();
        if ((await destTreeItem.getAttribute("aria-selected")) !== "true") {
          await destNode.click({ force: true });
        }
        await expect(
          list.getByText(sourceItem.name, { exact: true }),
          `dest list must show moved item ${sourceItem.name} without View→Refresh`,
        ).toBeVisible({ timeout: 20_000 });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${MOVE_ITEM_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
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
        void movedViaUi;
      }
    },
  );
});
