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
 * Playwright surface: #5479 / parent #4530 — Explorer Create Folder refuses
 * a NUL (U+0000) on {@code spa.jsp?entry=explorer} without
 * {@code rxFolderMutations=1}. The create request is not sent. A normal
 * name still creates only after HTTP 200.
 *
 * <p>Peer: {@code tests/explorer-create-folder.spec.js}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-create-folder-nul.spec.js}
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
  isPathmanagementAddNewFolderUrl,
  isFoldersCreateUrl,
  isRxContentExplorerFoldersUrl,
  isCreateFolderSuccessStatus,
  unwrapCreatedPathItem,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-create-folder");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-create-folder-nul", "@explorer", "@folder"];
const NUL_NAME = `bad${String.fromCharCode(0)}name`;

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

test.describe("Explorer create folder refuses a NUL (#5479)", () => {
  test(
    "a NUL does not create; a normal name creates only after success",
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

      let createPosts = 0;
      page.on("request", (req) => {
        if (
          req.method() !== "OPTIONS" &&
          isCreateFolderMutationUrl(req.url())
        ) {
          createPosts += 1;
        }
      });

      const folderName = `qa5479_${Date.now()}`;
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
        const createBtn = page.locator(
          `[data-testid="${CREATE_TEST_IDS.actionCreateFolder}"]`,
        );
        await expect(createBtn).toBeEnabled({ timeout: 10_000 });
        const postsBeforeUi = createPosts;

        await installPromptQueue(page, [NUL_NAME]);
        await createBtn.click();
        await expect(
          page.getByTestId("explorer-server-actions-error"),
        ).toContainText(/cannot be stored/i);
        expect(createPosts, "NUL create-folder must not POST").toBe(
          postsBeforeUi,
        );
        await expect(tree.getByText("badname", { exact: true })).toHaveCount(0);
        await expect(list.getByText("badname", { exact: true })).toHaveCount(0);

        await installPromptQueue(page, ["", null]);
        await createBtn.click();
        await createBtn.click();
        expect(createPosts, "empty or cancel must not POST").toBe(postsBeforeUi);

        let forcedStatus = 0;
        await page.route(
          /\/(?:rest\/folders\/create|pathmanagement\/path\/addNewFolder)/i,
          async (route) => {
            if (forcedStatus) {
              await route.fulfill({
                status: forcedStatus,
                contentType: "text/plain",
                body: "no",
              });
              return;
            }
            await route.continue();
          },
        );

        for (const status of [400, 403, 409]) {
          forcedStatus = status;
          const before = createPosts;
          const failName = `${folderName}x${status}`;
          await installPromptQueue(page, [failName]);
          await createBtn.click();
          await expect(
            page.getByTestId("explorer-server-actions-error"),
          ).toBeVisible({ timeout: 15_000 });
          expect(
            createPosts,
            `HTTP ${status} must not be treated as success`,
          ).toBeGreaterThan(before);
          await expect(tree.getByText(failName, { exact: true })).toHaveCount(0);
          await expect(list.getByText(failName, { exact: true })).toHaveCount(0);
        }

        forcedStatus = 0;
        await page.unroute(
          /\/(?:rest\/folders\/create|pathmanagement\/path\/addNewFolder)/i,
        );
        await installPromptQueue(page, [folderName]);
        const createRespPromise = page.waitForResponse(
          (res) =>
            isFoldersCreateUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );
        await createBtn.click();
        const createResp = await createRespPromise;
        expect(
          isCreateFolderSuccessStatus(createResp.status()),
          `POST /folders/create expected 200, got ${createResp.status()}`,
        ).toBeTruthy();
        createdItem = unwrapCreatedPathItem(await createResp.json());
        await expect(list.getByText(folderName, { exact: true })).toBeVisible({
          timeout: 20_000,
        });
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
          });
        }
      }
    },
  );
});
