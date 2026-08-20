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
 * Playwright surface: #3646 / parent #3102 — Explorer Delete selected folder
 * on the product route ({@code spa.jsp?entry=explorer}) without
 * {@code rxFolderMutations=1}.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: Sites or Assets parent exists on H2 (no skip)</li>
 *   <li>UI: ReducedActions Delete → pathmanagement deleteFolder HTTP 200</li>
 *   <li>UI: disposable folder name gone from detail-list and tree</li>
 *   <li>Must not POST/DELETE content-explorer/folders (flag off)</li>
 *   <li>Must not hit missing {@code /path/delete/{path}}</li>
 *   <li>Never deletes shipped sample pages — only {@code qa3646_*} created here</li>
 * </ul>
 *
 * <p><strong>No soft-skip</strong> when a Sites/Assets parent exists.
 * Do not claim gap-matrix Present from this surface.</p>
 *
 * <p>Tags: {@code @explorer-delete-folder} {@code @explorer} {@code @folder}
 * {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-delete-folder.spec.js}
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
  DELETE_TEST_IDS,
  explorerProductDeleteFolderUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementDeleteFolderUrl,
  isLegacyPathDeleteItemUrl,
  isRxContentExplorerFoldersUrl,
  isDeleteFolderSuccessStatus,
  uniqueDeleteFolderName,
  seedDisposableEmptyFolder,
  recycleFolder,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-delete-folder");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-delete-folder", "@explorer", "@folder", "@smoke"];

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

test.describe("Explorer Delete Folder on product route (#3646 / #3102)", () => {
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
    "UI: Delete selected empty folder on spa.jsp?entry=explorer without rxFolderMutations",
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
          (isPathmanagementDeleteFolderUrl(url) ||
            isLegacyPathDeleteItemUrl(url) ||
            isRxContentExplorerFoldersUrl(url)) &&
          req.method() !== "OPTIONS"
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const folderName = uniqueDeleteFolderName();
      /** @type {{ path?: string, name?: string, guid?: string } | null} */
      let seeded = null;
      let deletedViaUi = false;

      try {
        const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
        const sitesStatus = await getStatus(request, sitesFolderUrl(BASE_URL));
        const useAssets = assetsStatus === 200;
        expect(
          useAssets || sitesStatus === 200,
          `H2 Explorer parent must exist; Assets=${assetsStatus} Sites=${sitesStatus}`,
        ).toBe(true);

        const parentLabel = useAssets ? "Assets" : "Sites";
        const parentPath = useAssets ? "Assets" : "Sites";
        seeded = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath, name: folderName },
        );
        expect(
          String(seeded.name || folderName).startsWith("qa3646_"),
          `seed folder must be this test's qa3646_* name, got ${seeded.name}`,
        ).toBe(true);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductDeleteFolderUrl(BASE_URL);
        expect(
          hasRxFolderMutationsQuery(explorerUrl),
          "product route must not set rxFolderMutations=1",
        ).toBe(false);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(`[data-testid="${DELETE_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator(`[data-testid="${DELETE_TEST_IDS.tree}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const parentNode = useAssets
          ? treeRootLocator(page, "Assets").first()
          : sitesTreeRootLocator(page).first();
        await expect(
          parentNode,
          `H2 Explorer tree must show ${parentLabel} for Delete Folder`,
        ).toBeVisible({ timeout: 20_000 });

        await parentNode.click({ force: true });
        await expandExplorerTreeNode(parentNode).catch(() => undefined);
        const list = page.locator(`[data-testid="${DELETE_TEST_IDS.detailList}"]`);
        await list.waitFor({ timeout: 15_000 });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const folderRow = list.getByText(folderName, { exact: true });
        await expect(
          folderRow,
          `seeded ${folderName} must appear under ${parentLabel} before Delete`,
        ).toBeVisible({ timeout: 20_000 });
        await folderRow.click();

        const deleteBtn = page.locator(
          `[data-testid="${DELETE_TEST_IDS.actionDelete}"]`,
        );
        await expect(deleteBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          deleteBtn,
          `Delete must be enabled for selected empty folder ${folderName}`,
        ).toBeEnabled();

        page.once("dialog", async (dialog) => {
          await dialog.accept();
        });

        const deleteRespPromise = page.waitForResponse(
          (res) =>
            isPathmanagementDeleteFolderUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );

        await deleteBtn.click();
        const deleteResp = await deleteRespPromise;
        expect(
          isDeleteFolderSuccessStatus(deleteResp.status()),
          `deleteFolder expected 200, got ${deleteResp.status()} ${deleteResp.url()}`,
        ).toBe(true);
        deletedViaUi = true;

        const rxHits = mutations.filter((m) =>
          isRxContentExplorerFoldersUrl(m.url),
        );
        expect(
          rxHits,
          `product route must not call content-explorer/folders (flag off): ${JSON.stringify(rxHits)}`,
        ).toEqual([]);

        const legacyHits = mutations.filter((m) =>
          isLegacyPathDeleteItemUrl(m.url),
        );
        expect(
          legacyHits,
          `product Delete must not POST missing /path/delete/{path}: ${JSON.stringify(legacyHits)}`,
        ).toEqual([]);

        await expect(list.getByText(folderName, { exact: true })).toHaveCount(0, {
          timeout: 20_000,
        });

        const tree = page.locator(`[data-testid="${DELETE_TEST_IDS.tree}"]`);
        await expect(tree.getByText(folderName, { exact: true })).toHaveCount(0, {
          timeout: 15_000,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${DELETE_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (!deletedViaUi && seeded && seeded.path) {
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: seeded.path,
            guid: seeded.guid,
          }).catch(() => undefined);
        }
      }
    },
  );
});
