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
 * Playwright surface: #5553 / parent #4530 — Explorer copy destination
 * refuses a blank or whitespace-only path on {@code spa.jsp?entry=explorer}
 * without {@code rxFolderMutations=1}. The copy-destination dialog stays
 * open and the copy request is not sent. A non-blank destination still
 * copies only after HTTP 200. Cancel and HTTP 400/403/409 are not success.
 *
 * <p>Peers: {@code tests/explorer-copy-item.spec.js},
 * {@code tests/explorer-copy-dest-nul.spec.js}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-copy-dest-blank.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { listFolderChildren } = require("./helpers/empty-recycling");
const {
  COPY_TEST_IDS,
  explorerProductCopyItemUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersCopyFolderUrl,
  isFoldersCopyItemUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isCopyFolderSuccessStatus,
  uniqueCopyItemName,
  expectedCopiedItemNames,
  isCopyFolderItemRequestEnvelope,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-item");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-copy-dest-blank", "@explorer", "@item"];
const COPY_ITEM_ROUTE = "**/rest/folders/copy/item";

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
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} destPath
 * @param {string} itemName
 * @returns {Promise<string[]>}
 */
async function destChildNames(request, destPath, itemName) {
  const listPath = String(destPath || "")
    .replace(/^\/+/, "")
    .replace(/\/+$/, "");
  const kids = await listFolderChildren(
    request,
    BASE_URL,
    adminBasicAuthHeaders(),
    listPath,
  );
  const wanted = new Set(expectedCopiedItemNames(itemName));
  return kids
    .map((row) => String(row.name || row.Name || "").trim())
    .filter((n) => wanted.has(n));
}

test.describe("Explorer copy destination refuses a blank path (#5553)", () => {
  test(
    "a blank dest does not copy; a non-blank dest copies only after success",
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

      let copyItemPosts = 0;
      let copyFolderPosts = 0;
      page.on("request", (req) => {
        if (req.method() === "OPTIONS") {
          return;
        }
        if (isFoldersCopyItemUrl(req.url())) {
          copyItemPosts += 1;
        }
        if (isFoldersCopyFolderUrl(req.url())) {
          copyFolderPosts += 1;
        }
        if (
          isPathmanagementMoveItemUrl(req.url()) ||
          isRxContentExplorerFoldersUrl(req.url())
        ) {
          copyFolderPosts += 1;
        }
      });

      const stamp = Date.now();
      const sourceFolderName = uniqueCopyItemName("qa5553src", stamp);
      const destFolderName = uniqueCopyItemName("qa5553dst", stamp);
      const itemName = uniqueCopyItemName("qa5553itm", stamp);
      /** @type {{ path?: string, name?: string } | null} */
      let sourceFolder = null;
      /** @type {{ path?: string, name?: string } | null} */
      let destFolder = null;
      /** @type {{ path?: string, name?: string } | null} */
      let sourceItem = null;

      try {
        const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
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
        const listedName = String(sourceItem.name || itemName);
        const destPath = String(destFolder.path || `/Assets/${destFolderName}`);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductCopyItemUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);
        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        expect(hasRxFolderMutationsQuery(page.url())).toBe(false);

        const shell = page.locator(`[data-testid="${COPY_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(assetsNode).toBeVisible({ timeout: 20_000 });
        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${COPY_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });

        const sourceFolderRow = list.getByText(sourceFolderName, {
          exact: true,
        });
        await expect(sourceFolderRow).toBeVisible({ timeout: 20_000 });
        await sourceFolderRow.click();
        const openBtn = page.locator('[data-testid="action-open"]');
        await expect(openBtn).toBeEnabled();
        await openBtn.click();
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const itemRow = list.getByText(listedName, { exact: true });
        await expect(itemRow).toBeVisible({ timeout: 20_000 });
        await itemRow.click();

        const copyBtn = page.locator(
          `[data-testid="${COPY_TEST_IDS.actionCopy}"]`,
        );
        await expect(copyBtn).toBeEnabled({ timeout: 10_000 });
        const postsBeforeUi = copyItemPosts;

        await copyBtn.click();
        const destInput = page.locator('[data-testid="explorer-copy-dest-input"]');
        await expect(destInput).toBeVisible({ timeout: 10_000 });
        await destInput.fill("");
        await page.locator('[data-testid="explorer-copy-dest-ok"]').click();
        await expect(page.getByTestId("explorer-copy-dest-picker")).toBeVisible();
        await expect(page.getByTestId("explorer-copy-dest-error")).toContainText(
          /enter a destination folder path/i,
        );
        expect(copyItemPosts, "blank dest must not POST copy/item").toBe(
          postsBeforeUi,
        );
        expect(copyFolderPosts, "blank dest must not POST copy/folder").toBe(0);
        expect(await destChildNames(request, destPath, listedName)).toEqual([]);

        await destInput.fill("   ");
        await page.locator('[data-testid="explorer-copy-dest-ok"]').click();
        await expect(page.getByTestId("explorer-copy-dest-picker")).toBeVisible();
        await expect(page.getByTestId("explorer-copy-dest-error")).toContainText(
          /enter a destination folder path/i,
        );
        expect(copyItemPosts, "whitespace dest must not POST copy/item").toBe(
          postsBeforeUi,
        );
        expect(await destChildNames(request, destPath, listedName)).toEqual([]);

        await page.locator('[data-testid="explorer-copy-dest-cancel"]').click();
        await expect(page.getByTestId("explorer-copy-dest-picker")).toHaveCount(
          0,
        );
        expect(copyItemPosts, "cancel must not POST").toBe(postsBeforeUi);
        expect(await destChildNames(request, destPath, listedName)).toEqual([]);

        let forcedStatus = 0;
        await page.route(COPY_ITEM_ROUTE, async (route) => {
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
          const before = copyItemPosts;
          await itemRow.click();
          await copyBtn.click();
          await expect(destInput).toBeVisible({ timeout: 10_000 });
          await destInput.fill(destPath);
          await page.locator('[data-testid="explorer-copy-dest-ok"]').click();
          await expect(
            page.getByTestId("explorer-server-actions-error"),
          ).toBeVisible({ timeout: 15_000 });
          expect(
            copyItemPosts,
            `HTTP ${status} must not be treated as success`,
          ).toBeGreaterThan(before);
          expect(await destChildNames(request, destPath, listedName)).toEqual(
            [],
          );
        }

        forcedStatus = 0;
        await page.unroute(COPY_ITEM_ROUTE);
        await itemRow.click();
        await copyBtn.click();
        await expect(destInput).toBeVisible({ timeout: 10_000 });
        await destInput.fill(destPath);
        const copyRespPromise = page.waitForResponse(
          (res) =>
            isFoldersCopyItemUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );
        await page.locator('[data-testid="explorer-copy-dest-ok"]').click();
        const copyResp = await copyRespPromise;
        expect(
          isCopyFolderSuccessStatus(copyResp.status()),
          `copy/item expected 200, got ${copyResp.status()} ${copyResp.url()}`,
        ).toBe(true);
        const copyBody = copyResp.request().postDataJSON();
        expect(
          isCopyFolderItemRequestEnvelope(copyBody),
          `copy must wrap CopyFolderItemRequest: ${JSON.stringify(copyBody)}`,
        ).toBe(true);

        const destNames = expectedCopiedItemNames(listedName);
        const destNameMatcher = new RegExp(
          `^(${destNames.map((n) => n.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("|")})$`,
        );
        const tree = page.locator(`[data-testid="${COPY_TEST_IDS.tree}"]`);
        const destNode = tree.locator(`[data-testid*="${destFolderName}"]`).first();
        await expect(
          destNode,
          `destination folder ${destFolderName} must be visible after Copy`,
        ).toBeVisible({ timeout: 15_000 });
        const destTreeItem = destNode.locator('[role="treeitem"]').first();
        if ((await destTreeItem.getAttribute("aria-selected")) !== "true") {
          await destNode.click({ force: true });
        }
        await expect(
          list.getByText(destNameMatcher),
          `dest list must show copied item ${destNames.join(" or ")} without View→Refresh`,
        ).toBeVisible({ timeout: 20_000 });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${COPY_TEST_IDS.shell}"]`,
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
      }
    },
  );
});
