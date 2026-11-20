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
 * Playwright surface: #6651 / parent #4530 — checkout trio for generic
 * custom-type rows.
 *
 * The workflow endpoints are id-based
 * (`GET …/workflow/checkOut|checkIn|forceCheckIn/{id}`), so a generic
 * custom-type row (no page/asset token, outside /Sites and /Assets) keeps
 * Check Out, Check In, and Force Check-in visible and submitting. Hybrid
 * proof: the listed row is mocked into the kind-"none" shape (the H2 seed
 * has no stable generic folder), but the row id is a REAL seeded
 * custom-type item and every checkout call below hits the REAL endpoint —
 * a rejection would surface in the Server actions region and fail the
 * test. The single Check In rides the editor endpoint
 * (`POST /rest/editor/items/{id}/checkin`), like the page/asset flow.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-checkout-generic.spec.js
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

const TAGS = ["@explorer-checkout-generic", "@explorer", "@item"];

/**
 * Kind-"none" row for the seeded id: custom type, outside /Sites and
 * /Assets, so resolvePublishKind cannot see a page/asset affinity.
 */
function genericListBody(id, name) {
  return JSON.stringify({
    PagedItemList: {
      childrenInPage: [
        {
          id: String(id),
          name,
          path: `/ContentLib/${name}`,
          type: "MyCustomArticle",
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

test.describe("Explorer checkout trio for generic custom-type rows (#6651)", () => {
  test(
    "generic row exposes the trio; live checkOut + editor checkIn succeed",
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
        const seeded = await seedGenericItem(request, stamp, "qa6651gen");
        sourceFolder = seeded.sourceFolder;
        const { itemName, id } = seeded;
        await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: genericListBody(id, itemName),
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

        // The trio stays visible for the kind-"none" selection …
        const checkout = page.locator('[data-testid="action-toolbar-item-Check_Out"]');
        const checkin = page.locator('[data-testid="action-toolbar-item-Check_In"]');
        const force = page.locator('[data-testid="action-toolbar-item-Force_Checkin"]');
        await expect(checkout).toBeVisible({ timeout: 15_000 });
        await expect(checkin).toBeVisible({ timeout: 15_000 });
        await expect(force).toBeVisible({ timeout: 15_000 });
        // … and the checkout-owner panel loads for the generic row.
        await expect(page.locator('[data-testid="explorer-checkout-owner"]')).toBeVisible({
          timeout: 15_000,
        });
        await expect(
          page.locator('[data-testid="explorer-checkout-owner-error"]'),
        ).toHaveCount(0);

        // Live checkOut of the generic id (real endpoint, real 200).
        const checkOutResp = page.waitForResponse(
          "**/itemmanagement/workflow/checkOut/**",
          { timeout: 30_000 },
        );
        await checkout.click();
        expect((await checkOutResp).ok()).toBe(true);
        await expect(
          page.locator('[data-testid="explorer-server-actions-error"]'),
        ).toHaveCount(0);
        await page.screenshot({
          path: `test-results/live-proof-6651-checkout-generic.png`,
        });

        // Live editor checkIn of the generic id (comment dialog + POST).
        const checkInResp = page.waitForResponse("**/rest/editor/items/**/checkin**", {
          timeout: 30_000,
        });
        await checkin.click();
        await expect(page.locator('[data-testid="explorer-checkin-comment"]')).toBeVisible({
          timeout: 10_000,
        });
        await page.locator('[data-testid="explorer-checkin-confirm"]').click();
        expect((await checkInResp).ok()).toBe(true);
        await expect(
          page.locator('[data-testid="explorer-server-actions-error"]'),
        ).toHaveCount(0);

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

  test(
    "Admin live forceCheckIn succeeds for the generic id",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const stamp = Date.now();
      /** @type {{ path?: string } | null} */
      let sourceFolder = null;
      try {
        const seeded = await seedGenericItem(request, stamp, "qa6651fce");
        sourceFolder = seeded.sourceFolder;
        const { itemName, id } = seeded;
        await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: genericListBody(id, itemName),
          });
        });

        await loginAsAdmin(page);
        await page.goto(explorerSpaUrl(BASE_URL));
        await page.waitForLoadState("networkidle");
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

        // Check out first so force has a lock to steal (both real calls).
        const checkOutResp = page.waitForResponse(
          "**/itemmanagement/workflow/checkOut/**",
          { timeout: 30_000 },
        );
        await page.locator('[data-testid="action-toolbar-item-Check_Out"]').click();
        expect((await checkOutResp).ok()).toBe(true);

        const forceResp = page.waitForResponse(
          "**/itemmanagement/workflow/forceCheckIn/**",
          { timeout: 30_000 },
        );
        await page.locator('[data-testid="action-toolbar-item-Force_Checkin"]').click();
        expect((await forceResp).ok()).toBe(true);
        await expect(
          page.locator('[data-testid="explorer-server-actions-error"]'),
        ).toHaveCount(0);

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
