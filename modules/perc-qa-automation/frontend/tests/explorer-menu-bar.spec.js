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
 * Playwright surface: #2731 / parent #2400 — DCE-style Explorer top menu bar.
 *
 * <p>Verifies Content / View / Help menubar chrome on the modern SPA Explorer
 * (not multi-row flat view-tool buttons), nested View dropdown toggles, and
 * server action toolbar mount.</p>
 *
 * <p>Tags: {@code @explorer-menu-bar} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-menu-bar.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
  openViewMenu,
} = require("./helpers/explorer-menu-bar");

test.describe("modern React Content Explorer — DCE menu bar chrome (#2731)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts menubar with Content / View / Help",
    { tag: ["@explorer-menu-bar", "@explorer", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      const bar = page.locator(`[data-testid="${TEST_IDS.menuBar}"]`);
      await expect(bar).toBeVisible();
      await expect(
        page.locator('[data-testid="explorer-menu-bar-menubar"]'),
      ).toHaveAttribute("role", "menubar");

      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuContent}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuView}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuHelp}"]`),
      ).toBeVisible();

      // Nested view tools are not multi-row flat chrome when menus closed.
      await expect(
        page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`),
      ).toHaveCount(0);
    },
  );

  test(
    "View menu exposes search toggle and opens search panel",
    { tag: ["@explorer-menu-bar", "@explorer"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 15_000 });

      await openViewMenu(page);
      const toggle = page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`);
      await expect(toggle).toBeVisible();
      await expect(toggle).toHaveAttribute("aria-expanded", "false");

      await toggle.click();
      await expect(toggle).toHaveAttribute("aria-expanded", "true");
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchPanel}"]`),
      ).toBeVisible({ timeout: 5_000 });
    },
  );

  test(
    "server action toolbar still mounts under menu bar",
    { tag: ["@explorer-menu-bar", "@explorer"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible({ timeout: 15_000 });
    },
  );
});
