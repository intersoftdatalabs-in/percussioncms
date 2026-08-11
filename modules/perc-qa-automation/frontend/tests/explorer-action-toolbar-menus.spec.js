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
 * Playwright surface: #2730 residual — server action toolbar nested menus.
 *
 * <p>Coordinates with #2731 ExplorerMenuBar: this surface asserts the
 * server-driven {@code action-toolbar} mounts under the menubar and, when
 * the live CMS returns MENU parents with children, those parents render as
 * nested dropdowns ({@code aria-haspopup=menu}) rather than dumping every
 * child as a top-level button while closed.</p>
 *
 * <p>Tags: {@code @explorer-action-toolbar} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-action-toolbar-menus.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
} = require("./helpers/explorer-menu-bar");

test.describe("modern React Content Explorer — nested ActionToolbar menus (#2730)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
  });

  test(
    "server actions region + action toolbar mount under menu bar",
    { tag: ["@explorer-action-toolbar", "@explorer", "@smoke"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuBar}"]`),
      ).toBeVisible();
      // #2972: labeled Server actions chrome is always visible product chrome.
      await expect(
        page.locator('[data-testid="explorer-server-actions"]'),
      ).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-label"]'),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible();
      // Either at least one server action button OR the empty-state placeholder.
      const items = page.locator(
        `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid^="action-toolbar-item-"]`,
      );
      const empty = page.locator('[data-testid="action-toolbar-empty"]');
      const loadError = page.locator(
        '[data-testid="explorer-server-actions-error"]',
      );
      await expect
        .poll(async () => {
          const n = await items.count();
          const e = await empty.count();
          const err = await loadError.count();
          return n > 0 || e > 0 || err > 0;
        })
        .toBe(true);
    },
  );

  test(
    "MENU parents with children open nested dropdowns (soft when no nested menus)",
    { tag: ["@explorer-action-toolbar", "@explorer"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible({ timeout: 15_000 });

      const parents = page.locator(
        `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid^="action-toolbar-item-"][aria-haspopup="menu"]`,
      );
      const parentCount = await parents.count();
      if (parentCount === 0) {
        // Live CMS may still return only leaf MENUITEMs for the current
        // selection; nested dropdown chrome is covered by WebUI Vitest.
        test.info().annotations.push({
          type: "note",
          description:
            "No aria-haspopup=menu parents on live CMS; nested chrome covered by Vitest ActionToolbar tests.",
        });
        return;
      }

      const parent = parents.first();
      const parentTestId = await parent.getAttribute("data-testid");
      const menuName = String(parentTestId || "").replace(
        /^action-toolbar-item-/,
        "",
      );
      await parent.click();
      await expect(
        page.locator(`[data-testid="action-toolbar-menu-${menuName}"]`),
      ).toBeVisible({ timeout: 5_000 });

      // While open, at least one menuitem is present under the dropdown.
      const items = page.locator(
        `[data-testid="action-toolbar-menu-${menuName}"] [role="menuitem"]`,
      );
      await expect(items.first()).toBeVisible();
    },
  );
});
