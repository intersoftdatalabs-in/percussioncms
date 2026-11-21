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
 * Playwright surface: #6874 — a failed server context-menu catalog load
 * shows an error with Retry, never the empty-catalog "No actions" text.
 *
 * Sibling of {@code explorer-context-menu.spec.js} (#3629): that spec
 * proves the happy path (nested MENU on a healthy catalog). This spec
 * stubs the row catalog (`GET /services/actions/find`, not the
 * types/templates sub-loads) to fail twice with HTTP 500 and then pass
 * through to the live CMS, proving the popup error state offers Retry, a
 * keyboard (Enter) retry that fails again keeps the error and returns
 * focus to Retry, and a final Retry swaps in the live actions.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-context-menu-catalog-error.spec.js
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

const TAGS = ["@explorer-context-menu-error", "@explorer", "@item"];

/**
 * Single synthetic item row for the seeded id. Same shape as the
 * #6871 checkout-owner Retry spec: the menu load is itemId-driven, so the
 * error/Retry arc below holds for typed rows too.
 */
function singleItemListBody(id, name) {
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

async function seedMenuItem(request, stamp) {
  expect(await (await request.get(assetsFolderUrl(BASE_URL))).status()).toBe(200);
  const folderName = uniqueCopyItemName("qa6874f", stamp);
  const itemName = uniqueCopyItemName("qa6874m", stamp);
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

test.describe("Explorer context-menu catalog failure shows error with Retry (#6874)", () => {
  test(
    "500 shows error + Retry (not 'No actions'); Enter retry fails again with focus back; Retry loads actions",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") return;
        const text = msg.text();
        // The stubbed 500 is the failure under test; the browser logs the
        // failed resource (same filter as the #3629 context-menu spec).
        if (
          /Failed to load resource: the server responded with a status of (404|400|500)/i.test(
            text,
          )
        ) {
          return;
        }
        jsErrors.push(text);
      });
      const stamp = Date.now();
      /** @type {{ path?: string } | null} */
      let sourceFolder = null;
      try {
        const seeded = await seedMenuItem(request, stamp);
        sourceFolder = seeded.sourceFolder;
        const { itemName, id } = seeded;
        await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: singleItemListBody(id, itemName),
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

        // Fail the row catalog twice (HTTP 500), then pass through live.
        // Installed after the toolbar catalog already loaded, so only the
        // popup loads hit it.
        let catalogCalls = 0;
        await page.route("**/actions/find**", async (route) => {
          const reqUrl = route.request().url();
          if (/\/actions\/find\/(types|templates)/i.test(reqUrl)) {
            return route.continue();
          }
          catalogCalls += 1;
          if (catalogCalls <= 2) {
            await route.fulfill({
              status: 500,
              contentType: "application/json",
              body: "{}",
            });
            return;
          }
          return route.continue();
        });

        await itemRow.click({ button: "right" });
        const menu = page.locator('[data-testid="context-menu"]');
        await expect(menu).toBeVisible({ timeout: 15_000 });

        // First load fails: error + Retry, never the empty-catalog text.
        const error = page.locator('[data-testid="context-menu-error"]');
        const retry = page.locator('[data-testid="context-menu-retry"]');
        await expect(error).toBeVisible({ timeout: 15_000 });
        await expect(error).toContainText(/could not load/i);
        await expect(retry).toBeVisible();
        await expect(
          page.locator('[data-testid="context-menu-empty"]'),
        ).toHaveCount(0);
        await expect(
          page.locator('[data-testid^="context-menu-item-"]'),
        ).toHaveCount(0);
        await page.screenshot({
          path: `test-results/live-proof-6874-context-menu-catalog-error.png`,
        });

        // Keyboard activation: focus Retry, press Enter. The catalog fails
        // again, so the error stays and focus returns to Retry.
        await retry.focus();
        await page.keyboard.press("Enter");
        await expect
          .poll(async () => catalogCalls, { timeout: 15_000 })
          .toBeGreaterThanOrEqual(2);
        await expect(error).toBeVisible({ timeout: 15_000 });
        await expect(retry).toBeFocused({ timeout: 15_000 });

        // Pointer Retry against the now-healthy catalog swaps in actions.
        await retry.click();
        await expect
          .poll(async () => catalogCalls, { timeout: 15_000 })
          .toBeGreaterThanOrEqual(3);
        await expect(
          page.locator('[data-testid^="context-menu-item-"]').first(),
        ).toBeVisible({ timeout: 15_000 });
        await expect(error).toHaveCount(0);
        await expect(
          page.locator('[data-testid="context-menu-empty"]'),
        ).toHaveCount(0);
        await page.screenshot({
          path: `test-results/live-proof-6874-context-menu-catalog-retry.png`,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const relatedConsole = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/actions/find**").catch(() => undefined);
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
