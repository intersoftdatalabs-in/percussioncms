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
 * Playwright surface: #3640 / parent #3102 — Explorer Create Folder on the
 * product route ({@code spa.jsp?entry=explorer}) without
 * {@code rxFolderMutations=1}.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: Sites or Assets parent exists on H2 (no skip)</li>
 *   <li>UI: ReducedActions Create Folder → pathmanagement addNewFolder HTTP 200</li>
 *   <li>UI: new name appears in detail-list (and tree when expanded)</li>
 *   <li>Must not POST content-explorer/folders (flag off)</li>
 * </ul>
 *
 * <p><strong>No soft-skip</strong> when a Sites/Assets parent exists.
 * Do not claim gap-matrix Present from this surface.</p>
 *
 * <p>Tags: {@code @explorer-create-folder} {@code @explorer} {@code @folder}
 * {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-create-folder.spec.js}
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
  recycleFolder,
} = require("./helpers/folder-recycle-smoke");
const {
  CREATE_TEST_IDS,
  explorerProductCreateFolderUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementAddNewFolderUrl,
  isPathmanagementRenameFolderUrl,
  isRxContentExplorerFoldersUrl,
  isCreateFolderSuccessStatus,
  shouldSkipCreateFolder,
  uniqueCreateFolderName,
  unwrapCreatedPathItem,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-create-folder");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-create-folder", "@explorer", "@folder", "@smoke"];

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

test.describe("Explorer Create Folder on product route (#3640 / #3102)", () => {
  test(
    "REST: Sites or Assets parent exists (no skip)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(30_000);
      const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
      const sitesStatus = await getStatus(request, sitesFolderUrl(BASE_URL));
      const restParentOk = assetsStatus === 200 || sitesStatus === 200;
      expect(
        shouldSkipCreateFolder({
          restParentOk,
          testDbType: process.env.TEST_DB_TYPE,
        }),
        "must not soft-skip Create Folder when a Sites/Assets parent exists (#3640)",
      ).toBe(false);
      expect(
        restParentOk,
        `H2 demo-sites should expose /Assets or /Sites; got Assets=${assetsStatus} Sites=${sitesStatus}`,
      ).toBe(true);
    },
  );

  test(
    "UI: Create Folder on spa.jsp?entry=explorer without rxFolderMutations",
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

      /** @type {Array<{ url: string, method: string, status?: number }>} */
      const mutations = [];
      page.on("request", (req) => {
        const url = req.url();
        if (
          (isPathmanagementAddNewFolderUrl(url) ||
            isRxContentExplorerFoldersUrl(url)) &&
          req.method() !== "OPTIONS"
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const folderName = uniqueCreateFolderName();
      /** @type {object | null} */
      let createdItem = null;

      try {
      await loginAsAdmin(page);
      const explorerUrl = explorerProductCreateFolderUrl(BASE_URL);
      expect(
        hasRxFolderMutationsQuery(explorerUrl),
        "product route must not set rxFolderMutations=1",
      ).toBe(false);

      await page.goto(explorerUrl, { waitUntil: "networkidle" });
      expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

      const shell = page.locator(`[data-testid="${CREATE_TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator(`[data-testid="${CREATE_TEST_IDS.tree}"]`),
      ).toBeVisible({ timeout: 20_000 });

      const assetsRoot = treeRootLocator(page, "Assets");
      const sitesRoot = sitesTreeRootLocator(page);
      const assetsVisible = await assetsRoot
        .first()
        .isVisible({ timeout: 20_000 })
        .catch(() => false);
      const sitesVisible = await sitesRoot
        .first()
        .isVisible({ timeout: 8_000 })
        .catch(() => false);

      expect(
        shouldSkipCreateFolder({
          sitesRootVisible: sitesVisible,
          assetsRootVisible: assetsVisible,
          testDbType: process.env.TEST_DB_TYPE,
        }),
        "must not skip Create Folder when Sites/Assets is on the tree",
      ).toBe(false);

      const parentNode = assetsVisible ? assetsRoot.first() : sitesRoot.first();
      const parentLabel = assetsVisible ? "Assets" : "Sites";
      expect(
        assetsVisible || sitesVisible,
        "H2 Explorer tree must show Assets or Sites for Create Folder",
      ).toBe(true);

      await parentNode.click({ force: true });
      await expandExplorerTreeNode(parentNode).catch(() => undefined);
      await page
        .locator(`[data-testid="${CREATE_TEST_IDS.detailList}"]`)
        .waitFor({ timeout: 15_000 });
      await page.waitForLoadState("networkidle").catch(() => undefined);

      const createBtn = page.locator(
        `[data-testid="${CREATE_TEST_IDS.actionCreateFolder}"]`,
      );
      await expect(createBtn).toBeVisible({ timeout: 10_000 });
      await expect(
        createBtn,
        `Create Folder must be enabled under ${parentLabel} on the product route`,
      ).toBeEnabled();

      page.once("dialog", async (dialog) => {
        await dialog.accept(folderName);
      });

      const createRespPromise = page.waitForResponse(
        (res) =>
          isPathmanagementAddNewFolderUrl(res.url()) &&
          res.request().method() !== "OPTIONS",
        { timeout: 30_000 },
      );
      const renameRespPromise = page.waitForResponse(
        (res) =>
          isPathmanagementRenameFolderUrl(res.url()) &&
          res.request().method() !== "OPTIONS",
        { timeout: 30_000 },
      );

      await createBtn.click();
      const createResp = await createRespPromise;
      expect(
        isCreateFolderSuccessStatus(createResp.status()),
        `addNewFolder expected 200, got ${createResp.status()} ${createResp.url()}`,
      ).toBe(true);
      const renameResp = await renameRespPromise;
      expect(
        isCreateFolderSuccessStatus(renameResp.status()),
        `renameFolder expected 200, got ${renameResp.status()} ${renameResp.url()}`,
      ).toBe(true);
      createdItem = unwrapCreatedPathItem(
        await renameResp.json().catch(() => ({})),
      );
      if (!createdItem.path) {
        createdItem = unwrapCreatedPathItem(
          await createResp.json().catch(() => ({})),
        );
      }

      const rxHits = mutations.filter((m) =>
        isRxContentExplorerFoldersUrl(m.url),
      );
      expect(
        rxHits,
        `product route must not POST content-explorer/folders (flag off): ${JSON.stringify(rxHits)}`,
      ).toEqual([]);

      const list = page.locator(`[data-testid="${CREATE_TEST_IDS.detailList}"]`);
      await expect(list.getByText(folderName, { exact: true })).toBeVisible({
        timeout: 20_000,
      });

      const tree = page.locator(`[data-testid="${CREATE_TEST_IDS.tree}"]`);
      await expect(tree.getByText(folderName, { exact: true })).toBeVisible({
        timeout: 15_000,
      });

      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${CREATE_TEST_IDS.shell}"]`,
      });

      const relatedConsole = jsErrors.filter(
        (t) => !isKnownExplorerSitesConsoleNoise(t),
      );
      expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (createdItem && createdItem.path) {
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: createdItem.path,
          }).catch(() => undefined);
        }
      }
    },
  );
});
