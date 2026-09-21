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
 * Playwright: #4638 / parent #4530 — paste clipboard into destination.
 *
 * Tags: @explorer-paste-clipboard @explorer @item @smoke
 *
 * npm run test:surface -- --path tests/explorer-paste-clipboard.spec.js
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  PASTE_TEST_IDS,
  explorerProductPasteUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersCopyFolderUrl,
  isFoldersCopyItemUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isCopyFolderSuccessStatus,
  uniquePasteName,
  isCopyFolderItemRequestEnvelope,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-paste-clipboard");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-paste-clipboard", "@explorer", "@item", "@smoke"];

async function getStatus(request, url) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(url, { headers });
  return res.status();
}

test.describe("Explorer paste clipboard into destination (#4638 / #4530)", () => {
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
    "UI: paste clipboard item into selected dest on spa.jsp?entry=explorer",
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

      const stamp = Date.now();
      const sourceFolderName = uniquePasteName("qa4638src", stamp);
      const destFolderName = uniquePasteName("qa4638dst", stamp);
      const itemName = uniquePasteName("qa4638itm", stamp);
      let sourceFolder = null;
      let destFolder = null;
      let sourceItem = null;

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
            parentPath: String(
              sourceFolder.path || `/Assets/${sourceFolderName}`,
            ),
            name: itemName,
          },
        );

        await loginAsAdmin(page);
        const explorerUrl = explorerProductPasteUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });

        const shell = page.locator(`[data-testid="${PASTE_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator(`[data-testid="${PASTE_TEST_IDS.tree}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(assetsNode).toBeVisible({ timeout: 20_000 });
        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${PASTE_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const sourceFolderRow = list.getByText(sourceFolderName, {
          exact: true,
        });
        await expect(sourceFolderRow).toBeVisible({ timeout: 20_000 });
        await sourceFolderRow.click();
        await page.locator('[data-testid="action-open"]').click();
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const itemRow = list.getByText(sourceItem.name, { exact: true });
        await expect(itemRow).toBeVisible({ timeout: 20_000 });
        await itemRow.click();
        const rowCheckbox = list
          .locator("tbody tr")
          .filter({ hasText: sourceItem.name })
          .locator('input[type="checkbox"]')
          .first();
        if (await rowCheckbox.count()) {
          await rowCheckbox.check();
        }

        await page.locator('[data-testid="explorer-menu-content"]').click();
        await page.locator('[data-testid="explorer-clipboard-add"]').click();
        const panel = page.locator('[data-testid="explorer-clipboard-panel"]');
        await expect(panel).toBeVisible({ timeout: 10_000 });
        await expect(
          page.locator('[data-testid="clipboard-item-row"]').first(),
        ).toBeVisible();

        const tree = page.locator(`[data-testid="${PASTE_TEST_IDS.tree}"]`);
        const destNode = tree.locator(`[data-testid*="${destFolderName}"]`).first();
        if ((await destNode.count()) === 0) {
          await assetsNode.click({ force: true });
          await expandExplorerTreeNode(assetsNode).catch(() => undefined);
          await page.waitForLoadState("networkidle").catch(() => undefined);
        }
        await expect(
          destNode,
          `destination folder ${destFolderName} must be visible in tree`,
        ).toBeVisible({ timeout: 20_000 });
        await destNode.click({ force: true });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const destPath = String(
          destFolder.path || `/Assets/${destFolderName}`,
        );
        await expect(
          page.locator('[data-testid="clipboard-paste-dest"]'),
        ).toContainText(destFolderName);

        const pasteBtn = page.locator('[data-testid="clipboard-paste"]');
        await expect(pasteBtn).toBeEnabled({ timeout: 10_000 });

        const pasteRespPromise = page.waitForResponse(
          (res) =>
            isFoldersCopyItemUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );
        await pasteBtn.click();
        const pasteResp = await pasteRespPromise;
        expect(
          isCopyFolderSuccessStatus(pasteResp.status()),
          `paste copy/item expected 200, got ${pasteResp.status()} ${pasteResp.url()}`,
        ).toBe(true);
        const pasteBody = pasteResp.request().postDataJSON();
        expect(
          isCopyFolderItemRequestEnvelope(pasteBody),
          `paste must wrap CopyFolderItemRequest: ${JSON.stringify(pasteBody)}`,
        ).toBe(true);
        const nested = pasteBody.CopyFolderItemRequest || pasteBody;
        expect(String(nested.targetFolderPath || "")).toContain(destFolderName);
        expect(String(nested.itemPath || "")).not.toBe(
          String(nested.targetFolderPath || ""),
        );

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${PASTE_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
        void destPath;
        void isFoldersCopyFolderUrl;
        void isPathmanagementMoveItemUrl;
        void isRxContentExplorerFoldersUrl;
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
