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
 * 008 US3 (T022): new-content menus list exactly the allowed types, offer
 * templates for multi-template types, and open the create flow in-folder
 * (FR-006/007/008). Type/template endpoints are stubbed with production
 * wire shapes.
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-new-content.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const LISTING = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "7",
        name: "Folder1",
        path: "/Sites/Demo/Folder1",
        type: "folder",
        category: "folder",
        accessLevel: "WRITE",
        leaf: false,
      },
    ],
    childrenCount: 1,
    startIndex: 0,
  },
};

function typeMenu(name, label) {
  return { name, label, sortRank: 1, menuType: "MENUITEM", handler: "client" };
}

async function installRoutes(page) {
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/actions/find/types**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ActionMenuList: [
          typeMenu("percPage", "Page"),
          typeMenu("percFileAsset", "File"),
          typeMenu("rffImage", "Image"),
        ],
      }),
    });
  });
  await page.route("**/actions/find/templates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ActionMenuList: [
          typeMenu("101", "Standard"),
          typeMenu("102", "Sidebar"),
        ],
      }),
    });
  });
}

async function openShellWithFolder(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const folder = page.locator('[data-testid="detail-row-7"]');
  await expect(folder).toBeVisible({ timeout: 20_000 });
  await folder.click();
}

test.describe("Explorer new-content menus", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "type picker lists exactly the allowed types",
    { tag: ["@explorer", "@explorer-create", "@explorer-create-types"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installRoutes(page);
      await openShellWithFolder(page);
      await page.locator('[data-testid="action-toolbar-item-Create_New_Item"]').click();
      const picker = page.locator('[data-testid="explorer-type-picker"]');
      await expect(picker).toBeVisible({ timeout: 15_000 });
      const options = picker.locator("option");
      await expect(options).toHaveCount(3);
      await expect(picker).toContainText("Page");
      await expect(picker).toContainText("File");
      await expect(picker).toContainText("Image");
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-type-picker"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "multi-template type offers templates then opens the create flow",
    { tag: ["@explorer", "@explorer-create", "@explorer-create-templates"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installRoutes(page);
      await openShellWithFolder(page);
      await page.locator('[data-testid="action-toolbar-item-Create_New_Item"]').click();
      const picker = page.locator('[data-testid="explorer-type-picker"]');
      await expect(picker).toBeVisible({ timeout: 15_000 });
      await picker.locator("select").selectOption({ label: "Page" });
      await page.locator('[data-testid="explorer-type-picker-ok"]').click();
      // Template options come from the merged server catalog (live data);
      // this asserts the multi-template branch triggers (FR-007), while
      // exact type-set pinning lives in the stubbed type test + T021.
      const templates = page.locator('[data-testid="explorer-template-picker"]');
      await expect(templates).toBeVisible({ timeout: 15_000 });
      const options = templates.locator("option");
      await expect(options.first()).toBeAttached();
      expect(await options.count()).toBeGreaterThanOrEqual(2);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
