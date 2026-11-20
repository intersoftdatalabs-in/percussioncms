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
 * Explorer batch visibility identity (#6783 / parent #4530).
 *
 * Selecting a page/asset row resolves checkout status + numeric ids via
 * {@code POST /rest/explorer/identity/batch} and feeds the shell
 * visibility evaluator. A failed lookup names the error with Retry and
 * keeps menus fully visible (permissive default).
 *
 * {@code npm run test:surface -- --path tests/explorer-visibility-identity.spec.js}
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const PAGE_LIST = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "7",
        name: "Demo",
        path: "/Sites/Demo",
        type: "Folder",
        category: "folder",
        leaf: false,
      },
      {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 2,
    startIndex: 0,
  },
};

function identityBody() {
  return JSON.stringify([
    {
      id: "42",
      checkoutStatus: "Checked Out",
      contentTypeId: 123,
      objectType: 2,
      workflowId: 5,
      communityId: 1001,
    },
  ]);
}

async function stubList(page) {
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_LIST),
    });
  });
  await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: "editor",
          currentUser: "Admin",
          assignmentType: "Reader",
        },
      }),
    });
  });
}

test.describe("modern React Content Explorer — visibility identity", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "selected row resolves identity; menus stay mounted",
    { tag: ["@explorer-visibility-identity", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });
      let batches = 0;
      let batchBody = "";
      await stubList(page);
      await page.route("**/rest/explorer/identity/batch", async (route) => {
        batches += 1;
        batchBody = route.request().postData() ?? "";
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: identityBody(),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await page.locator('[data-testid="detail-row-42"][data-row-kind="item"]').click();
      await expect(page.locator('[data-testid="explorer-checkout-owner-user"]')).toHaveText(
        "editor",
        { timeout: 10_000 },
      );
      expect(batches, "identity batch must fire for the selected row").toBeGreaterThan(0);
      expect(batchBody).toContain("42");
      await expect(page.locator('[data-testid="explorer-identity-error"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible();
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );

  test(
    "failed identity lookup names the error with Retry; menus stay visible",
    { tag: ["@explorer-visibility-identity", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await stubList(page);
      let calls = 0;
      await page.route("**/rest/explorer/identity/batch", async (route) => {
        calls += 1;
        if (calls === 1) {
          await route.fulfill({
            status: 500,
            contentType: "application/json",
            body: JSON.stringify({ message: "identity down" }),
          });
        } else {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: identityBody(),
          });
        }
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await page.locator('[data-testid="detail-row-42"][data-row-kind="item"]').click();
      const alert = page.locator('[data-testid="explorer-identity-error"]');
      await expect(alert).toBeVisible({ timeout: 10_000 });
      await expect(alert).toContainText(/visibility details/i);
      // Permissive default: menus stay fully visible despite the failure.
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible();
      // Retry re-resolves and clears the error.
      await page.locator('[data-testid="explorer-identity-retry"]').click();
      await expect(alert).toHaveCount(0, { timeout: 10_000 });
      expect(calls).toBeGreaterThanOrEqual(2);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "folders do not request identity",
    { tag: ["@explorer-visibility-identity", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await stubList(page);
      let batches = 0;
      await page.route("**/rest/explorer/identity/batch", async (route) => {
        batches += 1;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([]),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await page.locator('[data-testid="detail-row-7"]').click();
      await expect(page.locator('[data-testid="explorer-checkout-owner"]')).toHaveCount(0);
      await page.waitForTimeout(1_500);
      expect(batches, "folders carry no identity; no batch expected").toBe(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "live batch endpoint is wired (real 200 for unknown ids)",
    { tag: ["@explorer-visibility-identity", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
        timeout: 20_000,
      });
      // No route stubs: this hits the deployed ItemIdentityResource +
      // sitemanage ItemIdentityAdaptor. Unknown ids are omitted (200 []),
      // which also proves the adaptor bean is wired (not a 500).
      const res = await page.request.post(
        `${BASE_URL}/Rhythmyx/rest/explorer/identity/batch`,
        {
          data: { ItemIdentityBatchRequest: { ids: ["no-such-row"] } },
        },
      );
      expect(res.status(), "live batch endpoint must be registered").toBe(200);
      // CXF wraps the list under the element root name; the SPA unwrap
      // accepts both the envelope and a bare array.
      expect(await res.json()).toEqual({ ItemIdentity: [] });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
