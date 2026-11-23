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
 * Playwright surface: #6871 (residual from #6838, parent #4530) —
 * checkout-owner panel Retry on a failed lookup.
 *
 * Sibling of {@code explorer-generic-checkout-owner.spec.js} (#6838):
 * that spec proves the owner line on the happy path. This spec stubs the
 * read-only lookup (`GET /rest/editor/items/{id}/checkout-owner`) to fail
 * twice with HTTP 403 and then succeed, proving the error state offers
 * Retry, a keyboard (Enter) retry that fails again keeps the error and
 * returns focus to Retry, and a final Retry names the holder.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-checkout-owner-retry.spec.js
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

const TAGS = ["@explorer-checkout-owner-retry", "@explorer", "@item"];

/**
 * Untyped generic row for the seeded id, same shape as the #6838 spec:
 * no type name and no category. The owner panel is itemId-driven, so the
 * Retry arc below holds for typed rows too.
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
  const seeded = await seedDisposableAsset(request, BASE_URL, adminBasicAuthHeaders(), {
    parentPath: sourcePath,
    name: itemName,
    contentTypes: ["rffFile", "percFile"],
  });
  return { sourceFolder, itemName, id: String(seeded.guid) };
}

test.describe("Explorer checkout-owner Retry on failed lookup (#6871)", () => {
  test(
    "403 shows Retry; keyboard retry fails again with focus back; Retry names the holder",
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
        const seeded = await seedGenericItem(request, stamp, "qa6871rty");
        sourceFolder = seeded.sourceFolder;
        const { itemName, id } = seeded;
        await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: untypedGenericListBody(id, itemName),
          });
        });

        // Fail the owner lookup twice (403), then name Admin.
        let ownerCalls = 0;
        await page.route("**/rest/editor/items/*/checkout-owner*", async (route) => {
          ownerCalls += 1;
          if (ownerCalls <= 2) {
            await route.fulfill({ status: 403, contentType: "application/json", body: "{}" });
            return;
          }
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              itemName,
              checkOutUser: "Admin",
              currentUser: "Admin",
            }),
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

        // First lookup fails: error line plus Retry, no user line.
        const retry = page.locator('[data-testid="explorer-checkout-owner-retry"]');
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-error"]'),
        ).toBeVisible({ timeout: 15_000 });
        await expect(retry).toBeVisible();
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-user"]'),
        ).toHaveCount(0);

        // Keyboard activation: focus Retry, press Enter. The lookup fails
        // again, so the error stays and focus returns to Retry.
        await retry.focus();
        await page.keyboard.press("Enter");
        await expect
          .poll(async () => ownerCalls, { timeout: 15_000 })
          .toBeGreaterThanOrEqual(2);
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-error"]'),
        ).toBeVisible({ timeout: 15_000 });
        await expect(retry).toBeFocused({ timeout: 15_000 });
        await page.screenshot({
          path: `test-results/live-proof-6871-checkout-owner-retry-error.png`,
        });

        // Pointer Retry against the now-healthy lookup names the holder.
        await retry.click();
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-user"]'),
        ).toHaveText("Admin", { timeout: 15_000 });
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-error"]'),
        ).toHaveCount(0);
        await page.screenshot({
          path: `test-results/live-proof-6871-checkout-owner-retry-fixed.png`,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/rest/editor/items/*/checkout-owner*").catch(() => undefined);
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

  test(
    "successful retry disarms focus; later fresh failure does not steal focus (#7146)",
    { tag: [...TAGS, "@explorer-checkout-owner-retry-focus"] },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });
      const stamp = Date.now();
      /** @type {{ path?: string }[]} */
      const foldersToRecycle = [];
      try {
        const seededA = await seedGenericItem(request, stamp, "qa7146a");
        foldersToRecycle.push(seededA.sourceFolder);
        const seededB = await seedGenericItem(request, stamp, "qa7146b");
        foldersToRecycle.push(seededB.sourceFolder);
        const { id: idA, itemName: nameA } = seededA;
        const { id: idB, itemName: nameB } = seededB;
        await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              PagedItemList: {
                childrenInPage: [
                  {
                    id: String(idA),
                    name: nameA,
                    path: `/ContentLib/${nameA}`,
                    accessLevel: "WRITE",
                    leaf: true,
                  },
                  {
                    id: String(idB),
                    name: nameB,
                    path: `/ContentLib/${nameB}`,
                    accessLevel: "WRITE",
                    leaf: true,
                  },
                ],
                childrenCount: 2,
                startIndex: 0,
              },
            }),
          });
        });

        // Item A: fail once, then name Admin. Item B: always fail.
        let ownerCallsA = 0;
        await page.route("**/rest/editor/items/*/checkout-owner*", async (route) => {
          const url = route.request().url();
          if (url.includes(`/items/${idA}/`)) {
            ownerCallsA += 1;
            if (ownerCallsA <= 1) {
              await route.fulfill({ status: 403, contentType: "application/json", body: "{}" });
              return;
            }
            await route.fulfill({
              status: 200,
              contentType: "application/json",
              body: JSON.stringify({
                itemName: nameA,
                checkOutUser: "Admin",
                currentUser: "Admin",
              }),
            });
            return;
          }
          await route.fulfill({ status: 403, contentType: "application/json", body: "{}" });
        });

        await loginAsAdmin(page);
        await page.goto(explorerSpaUrl(BASE_URL));
        await page.waitForLoadState("networkidle");
        await expect(
          page.locator('[data-testid="content-explorer-shell"]'),
        ).toBeVisible({ timeout: 20_000 });

        // Fail A, then Retry into success: the success path must disarm the
        // pending retry-focus request (#7146).
        const rowA = page.locator(
          `[data-testid="detail-row-${idA}"][data-row-kind="item"]`,
        );
        await expect(rowA).toBeVisible({ timeout: 20_000 });
        await rowA.click();
        const retryA = page.locator('[data-testid="explorer-checkout-owner-retry"]');
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-error"]'),
        ).toBeVisible({ timeout: 15_000 });
        await retryA.click();
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-user"]'),
        ).toHaveText("Admin", { timeout: 15_000 });

        // Fresh failure for B in the same mount: a new Retry renders, keeps
        // the visible ring class, but must not take focus unprompted.
        const rowB = page.locator(
          `[data-testid="detail-row-${idB}"][data-row-kind="item"]`,
        );
        await rowB.click();
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-error"]'),
        ).toBeVisible({ timeout: 15_000 });
        const retryB = page.locator('[data-testid="explorer-checkout-owner-retry"]');
        await expect(retryB).toBeVisible();
        await expect(retryB).toHaveClass(/perc-checkout-owner-retry/);
        await expect(retryB).not.toBeFocused();
        await page.screenshot({
          path: `test-results/live-proof-7146-checkout-owner-no-theft.png`,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/rest/editor/items/*/checkout-owner*").catch(() => undefined);
        await page.unroute("**/pathmanagement/path/paginatedFolder**").catch(() => undefined);
        for (const folder of foldersToRecycle) {
          if (folder && folder.path) {
            await recycleFolderWithItems(
              request,
              BASE_URL,
              adminBasicAuthHeaders(),
              folder,
            ).catch(() => undefined);
          }
        }
      }
    },
  );
});
