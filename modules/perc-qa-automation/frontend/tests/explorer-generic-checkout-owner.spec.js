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
 * Playwright surface: #6838 / parent #4530 — checkout-owner line for
 * untyped generic rows.
 *
 * Sibling of {@code explorer-generic-checkout.spec.js} (#6651): that spec
 * proves the trio on a *typed* custom-type row (which already passed the
 * old page/asset owner gate). This spec covers the #6838 gap — a generic
 * row with *no type name at all* — where the shell passed
 * {@code itemId=null} and the owner line stayed hidden even though
 * Check Out / Check In / Force Check-in were available.
 *
 * Hybrid proof: the listed row is stubbed into the untyped shape (the H2
 * seed has no stable untyped folder), but the row id is a REAL seeded
 * custom-type item and every checkout call below hits the REAL endpoint —
 * a rejection would surface in the Server actions region and fail the
 * test. The single Check Out also bumps the owner reload token, so the
 * owner line names the holder right after checkout; the closing Check In
 * returns the cell to a clean lock state.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-generic-checkout-owner.spec.js
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const {
  uniqueCopyItemName,
  assetsFolderUrl,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-item");

const TAGS = ["@explorer-generic-checkout-owner", "@explorer", "@item"];

/**
 * Untyped generic row for the seeded id: no type name and no category, so
 * the old page/asset owner gate hid the panel (#6838). Still a real
 * workflowed item (leaf, WRITE access, real content id).
 */
function untypedGenericListBody(id, name) {
  return JSON.stringify({
    PagedItemList: {
      childrenInPage: [
        {
          id: String(id),
          name,
          path: `/ContentLib/${name}`,
          accessLevel: "WRITE",
          leaf: true,
        },
      ],
      childrenCount: 1,
      startIndex: 0,
    },
  });
}

async function seedGenericItem(request, stamp, prefix) {
  expect(await (await request.get(assetsFolderUrl(BASE_URL))).status()).toBe(200);
  const folderName = uniqueCopyItemName(`${prefix}f`, stamp);
  const itemName = uniqueCopyItemName(prefix, stamp);
  const sourceFolder = await seedDisposableEmptyFolder(
    request,
    BASE_URL,
    adminBasicAuthHeaders(),
    { parentPath: "Assets", name: folderName },
  );
  const sourcePath = String(sourceFolder.path || `/Assets/${folderName}`);
  // rffFile/percFile are genuine legacy custom types (no page/asset token):
  // the seeded id is custom-type content even though it lives under /Assets.
  const seeded = await seedDisposableAsset(request, BASE_URL, adminBasicAuthHeaders(), {
    parentPath: sourcePath,
    name: itemName,
    contentTypes: ["rffFile", "percFile"],
  });
  return { sourceFolder, itemName, id: String(seeded.guid) };
}

test.describe("Explorer checkout owner for untyped generic rows (#6838)", () => {
  test(
    "untyped row shows the owner line; Check Out names the holder",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });
      const stamp = Date.now();
      /** @type {{ path?: string } | null} */
      let sourceFolder = null;
      try {
        const seeded = await seedGenericItem(request, stamp, "qa6838own");
        sourceFolder = seeded.sourceFolder;
        const { itemName, id } = seeded;
        await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: untypedGenericListBody(id, itemName),
          });
        });

        await loginAsAdmin(page);
        await page.goto(explorerSpaUrl(BASE_URL));
        await page.waitForLoadState("networkidle");
        await expect(
          page.locator('[data-testid="content-explorer-shell"]'),
        ).toBeVisible({ timeout: 20_000 });

        const itemRow = page.locator(
          `[data-testid="detail-row-${id}"][data-row-kind="item"]`,
        );
        await expect(itemRow).toBeVisible({ timeout: 20_000 });
        await itemRow.click();
        await expect(
          page.locator(
            `[data-testid="content-explorer-shell"][data-selected-item-id="${id}"]`,
          ),
        ).toBeVisible({ timeout: 10_000 });

        // The untyped row still loads the owner line from the REAL
        // checkout-owner endpoint (fresh seed: nobody holds the lock).
        await expect(page.locator('[data-testid="explorer-checkout-owner"]')).toBeVisible({
          timeout: 15_000,
        });
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-none"]'),
        ).toBeVisible({ timeout: 15_000 });
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-error"]'),
        ).toHaveCount(0);

        // Live Check Out of the generic id, then the owner line names Admin.
        const checkOutResp = page.waitForResponse(
          "**/itemmanagement/workflow/checkOut/**",
          { timeout: 30_000 },
        );
        await page.locator('[data-testid="action-toolbar-item-Check_Out"]').click();
        expect((await checkOutResp).ok()).toBe(true);
        await expect(
          page.locator('[data-testid="explorer-server-actions-error"]'),
        ).toHaveCount(0);
        await expect(page.locator('[data-testid="explorer-checkout-owner-user"]')).toHaveText(
          "Admin",
          { timeout: 15_000 },
        );
        await page.screenshot({
          path: `test-results/live-proof-6838-checkout-owner-generic.png`,
        });

        // Closing Check In returns the cell to a clean lock state and the
        // owner line back to none (same reload-token path as checkout).
        const checkInResp = page.waitForResponse("**/rest/editor/items/**/checkin**", {
          timeout: 30_000,
        });
        await page.locator('[data-testid="action-toolbar-item-Check_In"]').click();
        await expect(page.locator('[data-testid="explorer-checkin-comment"]')).toBeVisible({
          timeout: 10_000,
        });
        await page.locator('[data-testid="explorer-checkin-confirm"]').click();
        expect((await checkInResp).ok()).toBe(true);
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-none"]'),
        ).toBeVisible({ timeout: 15_000 });

        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/pathmanagement/path/paginatedFolder**").catch(() => undefined);
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
