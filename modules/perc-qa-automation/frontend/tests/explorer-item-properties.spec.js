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
 * Playwright surface: #4701 / parent #4530 — Explorer item properties save.
 *
 * Tags: @explorer-item-properties @explorer @item @smoke
 *
 * npm run test:surface -- --path tests/explorer-item-properties.spec.js
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  ITEM_PROPS_TEST_IDS,
  explorerProductItemPropertiesUrl,
  assetsFolderUrl,
  itemPropertiesUrl,
  isItemPropertiesSaveUrl,
  wrapItemPropertiesRequest,
  hasRxFolderMutationsQuery,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-item-properties");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = [
  "@explorer-item-properties",
  "@explorer",
  "@item",
  "@smoke",
];

async function getStatus(request, url) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(url, { headers });
  return res.status();
}

test.describe("Explorer item properties save (#4701 / #4530)", () => {
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
    "REST: blank name is HTTP 400 (#4701)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(30_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        "Content-Type": "application/json",
      };
      const res = await request.post(itemPropertiesUrl(BASE_URL), {
        headers,
        data: wrapItemPropertiesRequest("/Assets/missing", "  ", "t"),
      });
      expect(res.status()).toBe(400);
    },
  );

  test(
    "UI: save name and display title on spa.jsp?entry=explorer",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(120_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          const text = msg.text();
          if (isKnownExplorerSitesConsoleNoise(text)) {
            return;
          }
          jsErrors.push(text);
        }
      });

      const stamp = Date.now();
      const folderName = uniqueCopyItemName("qa4701fld", stamp);
      const itemName = uniqueCopyItemName("qa4701itm", stamp);
      const newName = uniqueCopyItemName("qa4701nm", stamp);
      /** @type {{ path?: string, name?: string } | null} */
      let liveFolder = null;
      /** @type {{ path?: string, name?: string } | null} */
      let sourceItem = null;

      try {
        const assetsStatus = await getStatus(
          request,
          assetsFolderUrl(BASE_URL),
        );
        expect(assetsStatus).toBe(200);

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

        await loginAsAdmin(page);
        const explorerUrl = explorerProductItemPropertiesUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);
        await page.goto(explorerUrl, { waitUntil: "networkidle" });

        const shell = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.shell}"]`,
        );
        await expect(shell).toBeVisible({ timeout: 20_000 });

        const assetsNode = treeRootLocator(page, "Assets").first();
        await expect(assetsNode).toBeVisible({ timeout: 20_000 });
        await assetsNode.click({ force: true });
        await expandExplorerTreeNode(assetsNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });

        const folderRow = list.getByText(folderName, { exact: true });
        await expect(folderRow).toBeVisible({ timeout: 20_000 });
        await folderRow.click();
        await page.locator('[data-testid="action-open"]').click();
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const itemRow = list.getByText(sourceItem.name, { exact: true });
        await expect(itemRow).toBeVisible({ timeout: 20_000 });
        await itemRow.click();

        await page
          .locator(`[data-testid="${ITEM_PROPS_TEST_IDS.toggle}"]`)
          .click();
        const nameInput = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.name}"]`,
        );
        await expect(nameInput).toBeVisible({ timeout: 20_000 });
        await nameInput.fill(newName);
        await page
          .locator(`[data-testid="${ITEM_PROPS_TEST_IDS.displayTitle}"]`)
          .fill(`${newName} title`);

        const saveRespPromise = page.waitForResponse(
          (res) =>
            isItemPropertiesSaveUrl(res.url()) &&
            res.request().method() === "POST",
          { timeout: 30_000 },
        );
        await page
          .locator(`[data-testid="${ITEM_PROPS_TEST_IDS.save}"]`)
          .click();
        const saveResp = await saveRespPromise;
        expect(saveResp.status(), "properties save HTTP").toBe(200);

        await expect(
          list.getByText(newName, { exact: true }),
        ).toBeVisible({ timeout: 20_000 });

        await expectNoSeriousA11yViolations(page, {
          include: '[data-testid="content-explorer-shell"]',
        });
        expect(
          jsErrors,
          `console/pageerror must stay empty: ${jsErrors.join(" | ")}`,
        ).toEqual([]);
      } finally {
        if (liveFolder && liveFolder.path) {
          await recycleFolderWithItems(
            request,
            BASE_URL,
            adminBasicAuthHeaders(),
            liveFolder.path,
          ).catch(() => undefined);
        }
      }
    },
  );
});
