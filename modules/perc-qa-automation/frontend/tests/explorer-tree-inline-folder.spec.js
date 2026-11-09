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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Playwright surface: #5882 / parent #4530 — Explorer tree inline
 * new-folder row on {@code spa.jsp?entry=explorer} without
 * {@code rxFolderMutations=1}.
 *
 * <p>The tree toolbar {@code New folder} button opens an inline row at the
 * selected location: typing a name and pressing Enter posts one create and
 * shows the new folder in the tree and detail list; Escape closes the row
 * with no POST; a blank name is refused client-side with no POST; a forced
 * HTTP 409 keeps the row open with the conflict named and does not refresh
 * the tree.</p>
 *
 * <p>Peer: {@code tests/explorer-create-folder-nul.spec.js} (dialog
 * validation) and {@code tests/explorer-create-folder.spec.js}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-tree-inline-folder.spec.js}
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
  CREATE_TEST_IDS,
  explorerProductCreateFolderUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersCreateUrl,
  isPathmanagementAddNewFolderUrl,
  isRxContentExplorerFoldersUrl,
  isCreateFolderSuccessStatus,
  uniqueQaFolderName,
  isCreateFolderMutationForName,
  unwrapCreatedPathItem,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-create-folder");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-tree-inline-folder", "@explorer", "@folder"];

const TREE_NEW_FOLDER_BTN = "tree-new-folder";
const INLINE_ROW = "tree-inline-new-folder";
const INLINE_INPUT = "tree-inline-new-folder-input";
const INLINE_ERROR = "tree-inline-new-folder-error";

/**
 * @param {string} url
 * @returns {boolean}
 */
function isCreateFolderMutationUrl(url) {
  return (
    isFoldersCreateUrl(url) ||
    isPathmanagementAddNewFolderUrl(url) ||
    isRxContentExplorerFoldersUrl(url)
  );
}

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} url
 * @returns {Promise<number>}
 */
async function getStatus(request, url) {
  const res = await request.get(url, { headers: adminBasicAuthHeaders() });
  return res.status();
}

test.describe("Explorer tree inline new-folder (#5882)", () => {
  test(
    "Enter commits, Escape cancels, blank is refused, 409 stays open",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(240_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      let createPosts = 0;
      page.on("request", (req) => {
        if (
          req.method() !== "OPTIONS" &&
          isCreateFolderMutationUrl(req.url())
        ) {
          createPosts += 1;
        }
      });

      /** @type {object | null} */
      let createdItem = null;

      try {
        const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
        const sitesStatus = await getStatus(request, sitesFolderUrl(BASE_URL));
        expect(
          assetsStatus === 200 || sitesStatus === 200,
          `H2 Explorer parent missing; Assets=${assetsStatus} Sites=${sitesStatus}`,
        ).toBe(true);
        const parentName = assetsStatus === 200 ? "Assets" : "Sites";

        await loginAsAdmin(page);
        const explorerUrl = explorerProductCreateFolderUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);
        await page.goto(explorerUrl, { waitUntil: "networkidle" });

        const shell = page.locator(`[data-testid="${CREATE_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        const parentNode =
          parentName === "Assets"
            ? treeRootLocator(page, "Assets").first()
            : sitesTreeRootLocator(page).first();
        await expect(parentNode).toBeVisible({ timeout: 20_000 });
        await parentNode.click({ force: true });
        await expandExplorerTreeNode(parentNode);
        const list = page.locator(
          `[data-testid="${CREATE_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });
        const tree = page.locator(`[data-testid="${CREATE_TEST_IDS.tree}"]`);

        const newFolderBtn = page.getByTestId(TREE_NEW_FOLDER_BTN);
        await expect(newFolderBtn).toBeVisible({ timeout: 10_000 });
        const inlineRow = page.getByTestId(INLINE_ROW);
        const inlineInput = page.getByTestId(INLINE_INPUT);

        // Escape cancels with no POST and no tree change.
        const postsBeforeEscape = createPosts;
        await newFolderBtn.click();
        await expect(inlineInput).toBeVisible({ timeout: 10_000 });
        await inlineInput.fill("qa5882abandoned");
        await inlineInput.press("Escape");
        await expect(inlineRow).toHaveCount(0);
        expect(createPosts, "Escape must not POST").toBe(postsBeforeEscape);
        await expect(tree.getByText("qa5882abandoned", { exact: true })).toHaveCount(0);

        // Blank is refused client-side with no POST; the row stays open.
        const postsBeforeBlank = createPosts;
        await newFolderBtn.click();
        await expect(inlineInput).toBeVisible({ timeout: 10_000 });
        await inlineInput.fill("");
        await inlineInput.press("Enter");
        await expect(page.getByTestId(INLINE_ERROR)).toContainText(
          /Enter a folder name/i,
          { timeout: 10_000 },
        );
        expect(createPosts, "blank name must not POST").toBe(postsBeforeBlank);
        await inlineInput.press("Escape");
        await expect(inlineRow).toHaveCount(0);

        // Forced 409 keeps the row open, names the folder, refreshes nothing.
        const createFolderRoute =
          /\/(?:rest\/folders\/create|pathmanagement\/path\/addNewFolder)/i;
        let forcedStatus = 409;
        await page.route(createFolderRoute, async (route) => {
          if (route.request().method() === "OPTIONS") {
            await route.continue();
            return;
          }
          if (forcedStatus) {
            await route.fulfill({
              status: forcedStatus,
              contentType: "text/plain",
              body: "name in use",
            });
            return;
          }
          await route.continue();
        });
        const conflictName = uniqueQaFolderName("qa5882taken");
        const postsBeforeConflict = createPosts;
        await newFolderBtn.click();
        await expect(inlineInput).toBeVisible({ timeout: 10_000 });
        await inlineInput.fill(conflictName);
        await inlineInput.press("Enter");
        const conflictError = page.getByTestId(INLINE_ERROR);
        await expect(conflictError).toContainText(conflictName, {
          timeout: 15_000,
        });
        expect(createPosts, "409 must still POST once").toBeGreaterThan(
          postsBeforeConflict,
        );
        await expect(inlineInput).toBeVisible();
        await expect(
          tree.getByText(conflictName, { exact: true }),
        ).toHaveCount(0);
        await expect(list.getByText(conflictName, { exact: true })).toHaveCount(0);
        forcedStatus = 0;
        await page.unroute(createFolderRoute);
        await inlineInput.press("Escape");
        await expect(inlineRow).toHaveCount(0);

        // Enter commits: one POST, row closes, tree + list show the name.
        const folderName = uniqueQaFolderName("qa5882");
        await parentNode.click({ force: true });
        await newFolderBtn.click();
        await expect(inlineInput).toBeVisible({ timeout: 10_000 });
        await inlineInput.fill(folderName);
        const createRespPromise = page.waitForResponse(
          (res) => isCreateFolderMutationForName(res, folderName),
          { timeout: 30_000 },
        );
        await inlineInput.press("Enter");
        const createResp = await createRespPromise;
        const createBody = await createResp.text();
        expect(
          isCreateFolderSuccessStatus(createResp.status()),
          `inline create expected 200, got ${createResp.status()} ${createResp.url()} body=${createBody.slice(0, 240)}`,
        ).toBeTruthy();
        createdItem = unwrapCreatedPathItem(
          (() => {
            try {
              return JSON.parse(createBody);
            } catch {
              return {};
            }
          })(),
        );
        await expect(inlineRow).toHaveCount(0);
        await expect(list.getByText(folderName, { exact: true })).toBeVisible({
          timeout: 20_000,
        });
        await expect(tree.getByText(folderName, { exact: true })).toBeVisible({
          timeout: 15_000,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${CREATE_TEST_IDS.tree}"]`,
        });
        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (createdItem && createdItem.path) {
          await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
            path: createdItem.path,
          });
        }
      }
    },
  );
});
