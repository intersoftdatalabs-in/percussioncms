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
 * Playwright surface: #3647 / parent #3102 — Explorer Copy selected folder
 * on the product route ({@code spa.jsp?entry=explorer}) without
 * {@code rxFolderMutations=1}.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: Sites or Assets parent exists on H2 (no skip)</li>
 *   <li>UI: ReducedActions Copy → {@code POST /rest/folders/copy/folder} HTTP 200</li>
 *   <li>UI: copy appears in destination detail-list and tree</li>
 *   <li>Must not POST content-explorer/folders (flag off)</li>
 *   <li>Must not POST pathmanagement moveItem (copy is not move)</li>
 * </ul>
 *
 * <p>Out of scope: Subfolder Copy wizard (#2792), clipboard paste (#2408).</p>
 *
 * <p><strong>No soft-skip</strong> when a Sites/Assets parent exists.
 * Do not claim gap-matrix Present from this surface.</p>
 *
 * <p>Tags: {@code @explorer-copy-folder} {@code @explorer} {@code @folder}
 * {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-copy-folder.spec.js}
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
  COPY_TEST_IDS,
  explorerProductCopyFolderUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersCopyFolderUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isCopyFolderSuccessStatus,
  uniqueCopyFolderName,
  isCopyFolderItemRequestEnvelope,
  seedDisposableEmptyFolder,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-folder");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-copy-folder", "@explorer", "@folder", "@smoke"];

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

test.describe("Explorer Copy Folder on product route (#3647 / #3102)", () => {
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
    "UI: Copy selected folder on spa.jsp?entry=explorer without rxFolderMutations",
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
          (isFoldersCopyFolderUrl(url) ||
            isPathmanagementMoveItemUrl(url) ||
            isRxContentExplorerFoldersUrl(url)) &&
          req.method() !== "OPTIONS"
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const stamp = Date.now();
      const sourceName = uniqueCopyFolderName("qa3647src", stamp);
      const destName = uniqueCopyFolderName("qa3647dst", stamp);
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let sourceFolder = null;
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let destFolder = null;
      let copiedViaUi = false;

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
          String(sourceFolder.name || sourceName).startsWith("qa3647"),
          `source folder must be this test's qa3647* name, got ${sourceFolder.name}`,
        ).toBe(true);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductCopyFolderUrl(BASE_URL);
        expect(
          hasRxFolderMutationsQuery(explorerUrl),
          "product route must not set rxFolderMutations=1",
        ).toBe(false);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(`[data-testid="${COPY_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator(`[data-testid="${COPY_TEST_IDS.tree}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const parentNode = useAssets
          ? treeRootLocator(page, "Assets").first()
          : sitesTreeRootLocator(page).first();
        await expect(
          parentNode,
          `H2 Explorer tree must show ${parentLabel} for Copy Folder`,
        ).toBeVisible({ timeout: 20_000 });

        await parentNode.click({ force: true });
        await expandExplorerTreeNode(parentNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${COPY_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const sourceRow = list.getByText(sourceName, { exact: true });
        await expect(
          sourceRow,
          `seeded ${sourceName} must appear under ${parentLabel} before Copy`,
        ).toBeVisible({ timeout: 20_000 });
        await sourceRow.click();

        const copyBtn = page.locator(
          `[data-testid="${COPY_TEST_IDS.actionCopy}"]`,
        );
        await expect(copyBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          copyBtn,
          `Copy must be enabled for selected folder ${sourceName}`,
        ).toBeEnabled();

        const destPath = String(destFolder.path || `/${parentPath}/${destName}`);
        page.once("dialog", async (dialog) => {
          await dialog.accept(destPath);
        });

        const copyRespPromise = page.waitForResponse(
          (res) =>
            isFoldersCopyFolderUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );

        await copyBtn.click();
        const copyResp = await copyRespPromise;
        expect(
          isCopyFolderSuccessStatus(copyResp.status()),
          `copy/folder expected 200, got ${copyResp.status()} ${copyResp.url()}`,
        ).toBe(true);
        const copyBody = copyResp.request().postDataJSON();
        expect(
          isCopyFolderItemRequestEnvelope(copyBody),
          `copy must wrap CopyFolderItemRequest: ${JSON.stringify(copyBody)}`,
        ).toBe(true);
        copiedViaUi = true;

        const rxHits = mutations.filter((m) =>
          isRxContentExplorerFoldersUrl(m.url),
        );
        expect(
          rxHits,
          `product route must not call content-explorer/folders (flag off): ${JSON.stringify(rxHits)}`,
        ).toEqual([]);

        const moveHits = mutations.filter((m) =>
          isPathmanagementMoveItemUrl(m.url),
        );
        expect(
          moveHits,
          `Copy must not POST moveItem: ${JSON.stringify(moveHits)}`,
        ).toEqual([]);

        await expect(list.getByText(sourceName, { exact: true })).toBeVisible({
          timeout: 20_000,
        });

        const tree = page.locator(`[data-testid="${COPY_TEST_IDS.tree}"]`);
        const destNode = tree.locator(`[data-testid*="${destName}"]`).first();
        await expect(destNode).toBeVisible({ timeout: 15_000 });
        const destToggle = destNode.locator('[aria-hidden="true"]').first();
        if (await destToggle.isVisible().catch(() => false)) {
          await destToggle.click();
        }
        await expect(
          destNode.getByText(sourceName, { exact: true }),
        ).toBeVisible({ timeout: 15_000 });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${COPY_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (sourceFolder && sourceFolder.path) {
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: sourceFolder.path,
            guid: sourceFolder.guid,
          }).catch(() => undefined);
        }
        if (copiedViaUi && destFolder && destFolder.path) {
          const copiedPath = `${String(destFolder.path).replace(/\/+$/, "")}/${sourceName}`;
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: copiedPath,
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
