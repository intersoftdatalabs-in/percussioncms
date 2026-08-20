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
 * Playwright surface: #3655 / parent #3102 — Explorer Move selected folder
 * on the product route ({@code spa.jsp?entry=explorer}) without
 * {@code rxFolderMutations=1}.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: Sites or Assets parent exists on H2 (no skip)</li>
 *   <li>UI: ReducedActions Move → {@code POST pathmanagement/path/moveItem} HTTP 200</li>
 *   <li>UI: folder gone from source list/tree; visible under dest without View → Refresh</li>
 *   <li>Must not POST content-explorer/folders (flag off)</li>
 *   <li>Must not POST rest/folders/copy/folder (move is not copy)</li>
 * </ul>
 *
 * <p>Out of scope: rxFolderMutations=1, item copy, rename/delete tree-refresh
 * residuals (#3652 / #3653).</p>
 *
 * <p><strong>No soft-skip</strong> when a Sites/Assets parent exists.
 * Do not claim gap-matrix Present from this surface.</p>
 *
 * <p>Tags: {@code @explorer-move-folder} {@code @explorer} {@code @folder}
 * {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-move-folder.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { recycleFolder } = require("./helpers/folder-recycle-smoke");
const {
  MOVE_TEST_IDS,
  explorerProductMoveFolderUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isFoldersCopyFolderUrl,
  isMoveFolderSuccessStatus,
  uniqueMoveFolderName,
  isMoveFolderItemEnvelope,
  seedDisposableEmptyFolder,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-move-folder");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-move-folder", "@explorer", "@folder", "@smoke"];

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

test.describe("Explorer Move Folder on product route (#3655 / #3102)", () => {
  test(
    "REST: Sites or Assets parent exists (no skip)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(30_000);
      const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
      const sitesStatus = await getStatus(request, sitesFolderUrl(BASE_URL));
      const restParentOk = assetsStatus === 200 || sitesStatus === 200;
      expect(
        restParentOk,
        `H2 demo-sites should expose /Assets or /Sites; got Assets=${assetsStatus} Sites=${sitesStatus}`,
      ).toBe(true);
    },
  );

  test(
    "UI: Move selected folder on spa.jsp?entry=explorer without rxFolderMutations",
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
          (isPathmanagementMoveItemUrl(url) ||
            isFoldersCopyFolderUrl(url) ||
            isRxContentExplorerFoldersUrl(url)) &&
          req.method() !== "OPTIONS"
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const stamp = Date.now();
      const sourceName = uniqueMoveFolderName("qa3655src", stamp);
      const destName = uniqueMoveFolderName("qa3655dst", stamp);
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let sourceFolder = null;
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let destFolder = null;
      let movedViaUi = false;

      try {
        const assetsStatus = await getStatus(
          request,
          assetsFolderUrl(BASE_URL),
        );
        const sitesStatus = await getStatus(request, sitesFolderUrl(BASE_URL));
        const useAssets = assetsStatus === 200;
        expect(
          useAssets || sitesStatus === 200,
          `H2 Explorer parent must exist; Assets=${assetsStatus} Sites=${sitesStatus}`,
        ).toBe(true);

        const parentLabel = useAssets ? "Assets" : "Sites";
        const parentPath = useAssets ? "Assets" : "Sites";
        destFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath, name: destName },
        );
        sourceFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath, name: sourceName },
        );
        expect(
          String(sourceFolder.name || sourceName).startsWith("qa3655"),
          `source folder must be this test's qa3655* name, got ${sourceFolder.name}`,
        ).toBe(true);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductMoveFolderUrl(BASE_URL);
        expect(
          hasRxFolderMutationsQuery(explorerUrl),
          "product route must not set rxFolderMutations=1",
        ).toBe(false);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(`[data-testid="${MOVE_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator(`[data-testid="${MOVE_TEST_IDS.tree}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const parentNode = useAssets
          ? treeRootLocator(page, "Assets").first()
          : sitesTreeRootLocator(page).first();
        await expect(
          parentNode,
          `H2 Explorer tree must show ${parentLabel} for Move Folder`,
        ).toBeVisible({ timeout: 20_000 });

        await parentNode.click({ force: true });
        await expandExplorerTreeNode(parentNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${MOVE_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const sourceRow = list.getByText(sourceName, { exact: true });
        await expect(
          sourceRow,
          `seeded ${sourceName} must appear under ${parentLabel} before Move`,
        ).toBeVisible({ timeout: 20_000 });
        await sourceRow.click();

        const moveBtn = page.locator(
          `[data-testid="${MOVE_TEST_IDS.actionMove}"]`,
        );
        await expect(moveBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          moveBtn,
          `Move must be enabled for selected folder ${sourceName}`,
        ).toBeEnabled();

        const destPath = useAssets
          ? `/Assets/${destName}`
          : String(destFolder.path || `/${parentPath}/${destName}`);
        page.once("dialog", async (dialog) => {
          await dialog.accept(destPath);
        });

        const moveRespPromise = page.waitForResponse(
          (res) =>
            isPathmanagementMoveItemUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );

        await moveBtn.click();
        const moveResp = await moveRespPromise;
        expect(
          isMoveFolderSuccessStatus(moveResp.status()),
          `moveItem expected 200, got ${moveResp.status()} ${moveResp.url()}`,
        ).toBe(true);
        const moveBody = moveResp.request().postDataJSON();
        expect(
          isMoveFolderItemEnvelope(moveBody),
          `move must wrap MoveFolderItem: ${JSON.stringify(moveBody)}`,
        ).toBe(true);
        movedViaUi = true;

        const rxHits = mutations.filter((m) =>
          isRxContentExplorerFoldersUrl(m.url),
        );
        expect(
          rxHits,
          `product route must not call content-explorer/folders (flag off): ${JSON.stringify(rxHits)}`,
        ).toEqual([]);

        const copyHits = mutations.filter((m) => isFoldersCopyFolderUrl(m.url));
        expect(
          copyHits,
          `Move must not POST copy/folder: ${JSON.stringify(copyHits)}`,
        ).toEqual([]);

        const tree = page.locator(`[data-testid="${MOVE_TEST_IDS.tree}"]`);
        await expect(
          parentNode.getByText(sourceName, { exact: true }),
        ).toHaveCount(0, { timeout: 20_000 });

        const destNode = tree.locator(`[data-testid*="${destName}"]`).first();
        await expect(destNode).toBeVisible({ timeout: 15_000 });
        const destToggle = destNode.locator('[aria-hidden="true"]').first();
        if (await destToggle.isVisible().catch(() => false)) {
          await destToggle.click();
        }
        await expect(
          destNode.getByText(sourceName, { exact: true }),
        ).toBeVisible({ timeout: 15_000 });
        await expect(list.getByText(sourceName, { exact: true })).toBeVisible({
          timeout: 20_000,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${MOVE_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (movedViaUi && destFolder && destFolder.path) {
          const movedPath = `${String(destFolder.path).replace(/\/+$/, "")}/${sourceName}`;
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: movedPath,
          }).catch(() => undefined);
        }
        if (!movedViaUi && sourceFolder && sourceFolder.path) {
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: sourceFolder.path,
            guid: sourceFolder.guid,
          }).catch(() => undefined);
        }
        if (destFolder && destFolder.path) {
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: destFolder.path,
            guid: destFolder.guid,
          }).catch(() => undefined);
        }
      }
    },
  );
});
