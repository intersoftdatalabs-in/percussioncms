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
 * Playwright surface: #3645 / parent #3102 — Explorer Rename on the
 * product route ({@code spa.jsp?entry=explorer}) without
 * {@code rxFolderMutations=1}.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: Sites or Assets parent exists on H2 (no skip)</li>
 *   <li>UI: ReducedActions Rename → pathmanagement renameFolder HTTP 200</li>
 *   <li>UI: new name appears in detail-list (and tree when expanded)</li>
 *   <li>Must not POST content-explorer/folders (flag off)</li>
 * </ul>
 *
 * <p><strong>No soft-skip</strong> when a Sites/Assets parent exists.
 * Do not claim gap-matrix Present from this surface.</p>
 *
 * <p>Tags: {@code @explorer-rename-folder} {@code @explorer} {@code @folder}
 * {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-rename-folder.spec.js}
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
  RENAME_TEST_IDS,
  explorerProductRenameFolderUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementRenameFolderUrl,
  isRxContentExplorerFoldersUrl,
  isRenameFolderSuccessStatus,
  uniqueRenameFolderName,
  unwrapPathItem,
  wrapRenameFolderItem,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-rename-folder");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-rename-folder", "@explorer", "@folder", "@smoke"];

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

/**
 * Seed a disposable folder under Assets (preferred) or Sites via
 * pathmanagement addNewFolder. Unique name via wrap-rename when the server
 * ignores {@code ?name=}.
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} parentName
 * @param {string} seedName
 * @returns {Promise<{ path: string, name: string, parentName: string }>}
 */
async function seedDisposableFolder(request, parentName, seedName) {
  const headers = {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
  };
  const parent = String(parentName).replace(/^\/+|\/+$/g, "");
  const addUrl = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/addNewFolder/${parent}?name=${encodeURIComponent(seedName)}`;
  const addRes = await request.get(addUrl, { headers });
  expect(
    addRes.ok(),
    `addNewFolder under /${parent} must succeed (HTTP ${addRes.status()})`,
  ).toBeTruthy();
  const created = unwrapPathItem(await addRes.json().catch(() => ({})));
  let name = String(created.name || seedName);
  let path = String(created.path || `/${parent}/${name}`);
  if (name !== seedName) {
    const renameRes = await request.post(
      `${BASE_URL}/Rhythmyx/services/pathmanagement/path/renameFolder`,
      {
        headers: { ...headers, "Content-Type": "application/json" },
        data: wrapRenameFolderItem(path, seedName),
      },
    );
    expect(
      isRenameFolderSuccessStatus(renameRes.status()),
      `seed renameFolder expected 200, got ${renameRes.status()}`,
    ).toBeTruthy();
    const renamed = unwrapPathItem(await renameRes.json().catch(() => ({})));
    name = String(renamed.name || seedName);
    path = String(renamed.path || `/${parent}/${name}`);
  }
  return { path, name, parentName: parent };
}

test.describe("Explorer Rename folder on product route (#3645 / #3102)", () => {
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
    "UI: Rename on spa.jsp?entry=explorer without rxFolderMutations",
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
          (isPathmanagementRenameFolderUrl(url) ||
            isRxContentExplorerFoldersUrl(url)) &&
          req.method() !== "OPTIONS"
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const seedName = uniqueRenameFolderName("qa3645s");
      const newName = uniqueRenameFolderName("qa3645r");
      /** @type {{ path: string, name: string, parentName: string } | null} */
      let liveFolder = null;

      try {
        const assetsStatus = await getStatus(
          request,
          assetsFolderUrl(BASE_URL),
        );
        const sitesStatus = await getStatus(request, sitesFolderUrl(BASE_URL));
        expect(
          assetsStatus === 200 || sitesStatus === 200,
          `H2 Explorer parent missing; Assets=${assetsStatus} Sites=${sitesStatus}`,
        ).toBe(true);
        const parentName = assetsStatus === 200 ? "Assets" : "Sites";
        liveFolder = await seedDisposableFolder(request, parentName, seedName);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductRenameFolderUrl(BASE_URL);
        expect(
          hasRxFolderMutationsQuery(explorerUrl),
          "product route must not set rxFolderMutations=1",
        ).toBe(false);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(
          `[data-testid="${RENAME_TEST_IDS.shell}"]`,
        );
        await expect(shell).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator(`[data-testid="${RENAME_TEST_IDS.tree}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const parentNode =
          parentName === "Assets"
            ? treeRootLocator(page, "Assets").first()
            : sitesTreeRootLocator(page).first();
        await expect(
          parentNode,
          `H2 Explorer tree must show ${parentName} for Rename`,
        ).toBeVisible({ timeout: 20_000 });

        await parentNode.click({ force: true });
        await expandExplorerTreeNode(parentNode).catch(() => undefined);
        await page
          .locator(`[data-testid="${RENAME_TEST_IDS.detailList}"]`)
          .waitFor({ timeout: 15_000 });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const seedRow = page
          .locator(`[data-testid="${RENAME_TEST_IDS.detailList}"]`)
          .getByText(liveFolder.name, { exact: true });
        await expect(seedRow).toBeVisible({ timeout: 20_000 });
        await seedRow.click();

        const renameBtn = page.locator(
          `[data-testid="${RENAME_TEST_IDS.actionRename}"]`,
        );
        await expect(renameBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          renameBtn,
          `Rename must be enabled for ${liveFolder.name} on the product route`,
        ).toBeEnabled();

        page.once("dialog", async (dialog) => {
          await dialog.accept(newName);
        });

        const renameRespPromise = page.waitForResponse(
          (res) =>
            isPathmanagementRenameFolderUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );

        await renameBtn.click();
        const renameResp = await renameRespPromise;
        expect(
          isRenameFolderSuccessStatus(renameResp.status()),
          `renameFolder expected 200, got ${renameResp.status()} ${renameResp.url()}`,
        ).toBeTruthy();
        const renamedItem = unwrapPathItem(
          await renameResp.json().catch(() => ({})),
        );
        if (renamedItem.path) {
          liveFolder.path = String(renamedItem.path);
          liveFolder.name = String(renamedItem.name || newName);
        } else {
          liveFolder.name = newName;
        }

        const rxHits = mutations.filter((m) =>
          isRxContentExplorerFoldersUrl(m.url),
        );
        expect(
          rxHits,
          `product route must not POST content-explorer/folders (flag off): ${JSON.stringify(rxHits)}`,
        ).toEqual([]);

        const list = page.locator(
          `[data-testid="${RENAME_TEST_IDS.detailList}"]`,
        );
        await expect(list.getByText(newName, { exact: true })).toBeVisible({
          timeout: 20_000,
        });

        const tree = page.locator(
          `[data-testid="${RENAME_TEST_IDS.tree}"]`,
        );
        await expect(tree.getByText(newName, { exact: true })).toBeVisible({
          timeout: 15_000,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${RENAME_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (liveFolder && liveFolder.path) {
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: liveFolder.path,
          }).catch(() => undefined);
        }
      }
    },
  );
});
