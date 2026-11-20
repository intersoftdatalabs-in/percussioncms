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
 * Playwright surface: #6651 / parent #4530 — Explorer generic items keep the
 * checkout / check-in / force-check-in trio.
 *
 * The trio is gated on item-ness (non-folder + id), never on publish kind,
 * so a kind-"none" custom-type row outside /Sites and /Assets still shows
 * all three actions and submits the row id to the id-based workflow
 * endpoints. The row shape below is stubbed to classify kind-"none"
 * (path + type carry no page/asset token); the content id is a real H2 id
 * seeded via item/create, and all three workflow calls hit the real
 * backend — that is the server-acceptance proof the issue asks for.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-generic-checkout.spec.js
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  assetsFolderUrl,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-item");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

const TAGS = ["@explorer-generic-checkout", "@explorer", "@item"];

function kindNoneList(itemName, realId) {
  return {
    PagedItemList: {
      childrenInPage: [
        {
          id: String(realId),
          name: itemName,
          path: `/Other/${itemName}`,
          type: "percTemplate",
          category: "template",
          accessLevel: "WRITE",
          leaf: true,
        },
      ],
      childrenCount: 1,
      startIndex: 0,
    },
  };
}

test.describe("Explorer generic items checkout trio (#6651)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "UI: kind-none row shows the trio and all three id-based calls succeed",
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
      const sourceName = uniqueCopyItemName("qa6651src", stamp);
      const itemName = uniqueCopyItemName("qa6651gen", stamp);
      /** @type {{ path?: string } | null} */
      let sourceFolder = null;

      try {
        expect(await (await request.get(assetsFolderUrl(BASE_URL))).status()).toBe(200);
        sourceFolder = await seedDisposableEmptyFolder(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: "Assets", name: sourceName },
        );
        const sourcePath = String(sourceFolder.path || `/Assets/${sourceName}`);
        const seeded = await seedDisposableAsset(
          request,
          BASE_URL,
          adminBasicAuthHeaders(),
          { parentPath: sourcePath, name: itemName },
        );
        const realId = String(seeded.guid || "");
        expect(realId.length).toBeGreaterThan(0);

        await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(kindNoneList(itemName, realId)),
          });
        });

        await page.goto(explorerSpaUrl(BASE_URL), {
          waitForLoadState: "networkidle",
        });
        await expect(
          page.locator('[data-testid="content-explorer-shell"]'),
        ).toBeVisible({ timeout: 20_000 });

        const row = page.locator(
          `[data-testid="detail-row-${realId}"][data-row-kind="item"]`,
        );
        await expect(row).toBeVisible({ timeout: 20_000 });
        await row.click();
        const shell = page.locator(
          `[data-testid="content-explorer-shell"][data-selected-item-id="${realId}"]`,
        );
        await expect(shell).toBeVisible({ timeout: 10_000 });
        // The stubbed custom-type row classifies kind-none (the #6651 case).
        await expect(shell).toHaveAttribute("data-selected-item-kind", "none");

        const checkout = page.locator('[data-testid="action-toolbar-item-Check_Out"]');
        const checkin = page.locator('[data-testid="action-toolbar-item-Check_In"]');
        const force = page.locator('[data-testid="action-toolbar-item-Force_Checkin"]');
        await expect(checkout).toBeVisible({ timeout: 15_000 });
        await expect(checkin).toBeVisible({ timeout: 15_000 });
        await expect(force).toBeVisible({ timeout: 15_000 });

        // 1. Check Out submits the generic id to the id-based endpoint.
        const [checkoutRes] = await Promise.all([
          page.waitForResponse(
            (res) => res.url().includes("/itemmanagement/workflow/checkOut/"),
            { timeout: 20_000 },
          ),
          checkout.click(),
        ]);
        expect(checkoutRes.status()).toBe(200);
        await expect(
          page.locator('[data-testid="explorer-server-actions-error"]'),
        ).toHaveCount(0);

        // 2. Check In with a comment uses the editor check-in for the same id.
        await checkin.click();
        await expect(
          page.locator('[data-testid="explorer-checkin-comment"]'),
        ).toBeVisible({ timeout: 10_000 });
        const [checkinRes] = await Promise.all([
          page.waitForResponse(
            (res) => res.url().includes("/rest/editor/items/"),
            { timeout: 20_000 },
          ),
          page.locator('[data-testid="explorer-checkin-confirm"]').click(),
        ]);
        expect(checkinRes.status()).toBe(200);
        await expect(
          page.locator('[data-testid="explorer-server-actions-error"]'),
        ).toHaveCount(0);

        // 3. Force Check-in (Admin) succeeds once the generic id is checked out.
        const [checkoutRes2] = await Promise.all([
          page.waitForResponse(
            (res) => res.url().includes("/itemmanagement/workflow/checkOut/"),
            { timeout: 20_000 },
          ),
          checkout.click(),
        ]);
        expect(checkoutRes2.status()).toBe(200);
        const [forceRes] = await Promise.all([
          page.waitForResponse(
            (res) => res.url().includes("/itemmanagement/workflow/forceCheckIn/"),
            { timeout: 20_000 },
          ),
          force.click(),
        ]);
        expect(forceRes.status()).toBe(200);
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
