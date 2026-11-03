/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
 * Playwright surface: #5480 / parent #4530 — Explorer item Rename refuses
 * a NUL (U+0000) on {@code spa.jsp?entry=explorer} without
 * {@code rxFolderMutations=1}. The rename request is not sent. A normal
 * name still renames only after HTTP 200. Folder rename stays on the
 * folder dialog.
 *
 * <p>Peer: {@code tests/explorer-rename-item.spec.js}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-rename-item-nul.spec.js}
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
  isRenameItemSuccessStatus,
  isRenameFolderItemRequestForName,
  uniqueCopyItemName,
  uniqueRenameItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-rename-item");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-rename-item-nul", "@explorer", "@item"];
const NUL_NAME = `bad${String.fromCharCode(0)}name`;
const RENAME_ITEM_ROUTE = "**/rest/folders/rename/item";

/**
 * Queue of prompt answers. Code points cross the Playwright bridge as
 * numbers so a NUL is not dropped by JSON transport. {@code null} is
 * cancel.
 *
 * @param {import("@playwright/test").Page} page
 * @param {Array<string | null>} answers
 */
async function installPromptQueue(page, answers) {
  const payload = answers.map((value) => {
    if (value == null) {
      return null;
    }
    return Array.from(value, (ch) => ch.codePointAt(0));
  });
  await page.evaluate((items) => {
    const queue = items.map((codes) =>
      codes == null ? null : String.fromCodePoint(...codes),
    );
    let i = 0;
    window.prompt = () => (i < queue.length ? queue[i++] : null);
  }, payload);
}

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
 * @param {import("@playwright/test").Response} res
 * @param {string} name
 * @returns {boolean}
 */
function isRenameItemResponseForName(res, name) {
  if (
    !isFoldersRenameItemUrl(res.url()) ||
    res.request().method() === "OPTIONS"
  ) {
    return false;
  }
  let body;
  try {
    body = res.request().postDataJSON();
  } catch {
    return false;
  }
  return isRenameFolderItemRequestForName(body, name);
}

test.describe("Explorer item rename refuses a NUL (#5480)", () => {
  test(
    "a NUL does not rename; a normal name renames only after success",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      let renamePosts = 0;
      let folderRenamePosts = 0;
      page.on("request", (req) => {
        if (req.method() === "OPTIONS") {
          return;
        }
        if (isFoldersRenameItemUrl(req.url())) {
          renamePosts += 1;
        }
        if (isPathmanagementRenameFolderUrl(req.url())) {
          folderRenamePosts += 1;
        }
      });

      const stamp = Date.now();
      const folderName = uniqueCopyItemName("qa5480fld", stamp);
      const itemName = uniqueCopyItemName("qa5480itm", stamp);
      /** @type {{ path?: string, name?: string } | null} */
      let liveFolder = null;
      /** @type {{ path?: string, name?: string } | null} */
      let sourceItem = null;

      try {
        const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
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
        const listedName = String(sourceItem.name || itemName);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductRenameItemUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);
        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(
          `[data-testid="${RENAME_ITEM_TEST_IDS.shell}"]`,
        );
        await expect(shell).toBeVisible({ timeout: 20_000 });
        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(assetsNode).toBeVisible({ timeout: 20_000 });
        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${RENAME_ITEM_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });

        const folderRow = list.getByText(folderName, { exact: true });
        await expect(folderRow).toBeVisible({ timeout: 20_000 });
        await folderRow.click();
        const openBtn = page.locator('[data-testid="action-open"]');
        await expect(openBtn).toBeEnabled();
        await openBtn.click();
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const itemRow = list.getByText(listedName, { exact: true });
        await expect(itemRow).toBeVisible({ timeout: 20_000 });
        await itemRow.click();

        const renameBtn = page.locator(
          `[data-testid="${RENAME_ITEM_TEST_IDS.actionRename}"]`,
        );
        await expect(renameBtn).toBeEnabled({ timeout: 10_000 });
        const postsBeforeUi = renamePosts;

        await installPromptQueue(page, [NUL_NAME]);
        await renameBtn.click();
        await expect(
          page.getByTestId("explorer-server-actions-error"),
        ).toContainText(/cannot be stored/i);
        await expect(page.getByTestId("explorer-folder-rename")).toHaveCount(0);
        await expect(list.getByText(listedName, { exact: true })).toBeVisible();
        expect(renamePosts, "NUL item rename must not POST").toBe(postsBeforeUi);
        expect(folderRenamePosts, "item rename must not POST folder rename").toBe(
          0,
        );

        await installPromptQueue(page, ["", null, listedName]);
        await renameBtn.click();
        await renameBtn.click();
        await renameBtn.click();
        expect(renamePosts, "empty, cancel, or same name must not POST").toBe(
          postsBeforeUi,
        );

        let forcedStatus = 0;
        await page.route(RENAME_ITEM_ROUTE, async (route) => {
          if (route.request().method() === "OPTIONS") {
            await route.continue();
            return;
          }
          if (forcedStatus) {
            await route.fulfill({
              status: forcedStatus,
              contentType: "text/plain",
              body: "no",
            });
            return;
          }
          await route.continue();
        });

        for (const status of [400, 403, 409]) {
          forcedStatus = status;
          const before = renamePosts;
          const failName = uniqueRenameItemName(`qa5480x${status}`);
          await itemRow.click();
          await installPromptQueue(page, [failName]);
          await renameBtn.click();
          await expect(
            page.getByTestId("explorer-server-actions-error"),
          ).toBeVisible({ timeout: 15_000 });
          expect(
            renamePosts,
            `HTTP ${status} must not be treated as success`,
          ).toBeGreaterThan(before);
          await expect(list.getByText(failName, { exact: true })).toHaveCount(0);
          await expect(list.getByText(listedName, { exact: true })).toBeVisible();
          await expect(page.getByTestId("explorer-folder-rename")).toHaveCount(0);
        }

        forcedStatus = 0;
        await page.unroute(RENAME_ITEM_ROUTE);
        await itemRow.click();
        const successName = uniqueRenameItemName("qa5480ren");
        await installPromptQueue(page, [successName]);
        const renameRespPromise = page.waitForResponse(
          (res) => isRenameItemResponseForName(res, successName),
          { timeout: 30_000 },
        );
        await renameBtn.click();
        const renameResp = await renameRespPromise;
        expect(
          isRenameItemSuccessStatus(renameResp.status()),
          `rename/item expected 200, got ${renameResp.status()} ${renameResp.url()}`,
        ).toBe(true);
        await expect(
          list.getByText(successName, { exact: true }),
          `list must show renamed item ${successName} without View→Refresh`,
        ).toBeVisible({ timeout: 20_000 });
        await expect(list.getByText(listedName, { exact: true })).toHaveCount(0);
        expect(
          folderRenamePosts,
          `non-folder Rename must not POST pathmanagement renameFolder`,
        ).toBe(0);

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
