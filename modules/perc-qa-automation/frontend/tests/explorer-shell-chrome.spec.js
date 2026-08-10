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
 * Playwright surface: #2850 / parent #2407 / grandparent #2400 —
 * Explorer product shell chrome composition (search toggle + stable testids).
 *
 * <p>Asserts DCE menu bar, display-format selector, server action toolbar
 * region, and Search panel open/close from View and Content menus. Free-text
 * SearchPanel chrome is hard when the panel is open; toolbar item children
 * are soft-asserted when the action catalog is empty on minimal H2.</p>
 *
 * <p>Tags: {@code @explorer-shell-chrome} {@code @explorer} {@code @smoke}
 * {@code @search}</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-shell-chrome.spec.js
 *   # tags:
 *   npm run test:surface -- --tag explorer-shell-chrome
 *   # list only (no live CMS):
 *   npm run test:surface:list -- --path tests/explorer-shell-chrome.spec.js
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 *
 * <p><strong>Soft-skip policy:</strong> only when a live QA CMS is unavailable
 * (agent documents skip in the PR / issue). Do not soft-skip missing product
 * chrome on a healthy CMS — those are hard fails for this surface.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSpaUrl,
  openViewMenu,
  openContentMenu,
  softVisible,
} = require("./helpers/explorer-shell-chrome");

test.describe("Explorer shell chrome composition (#2850 / #2407)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts menu bar, display format, and action toolbar chrome",
    { tag: ["@explorer-shell-chrome", "@explorer", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });

      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuBar}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuContent}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuView}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuHelp}"]`),
      ).toBeVisible();

      // Always-visible shell chrome (not nested under a closed menu).
      await expect(
        page.locator(`[data-testid="${TEST_IDS.displayFormat}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.serverActions}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.tree}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.detailList}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.reducedActions}"]`),
      ).toBeVisible();

      // Soft-assert: optional toolbar menu items when catalog has entries.
      await softVisible(
        page.locator(
          `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid^="action-toolbar-item-"]`,
        ),
      );
    },
  );

  test(
    "View → Search toggles Search panel free-text chrome",
    { tag: ["@explorer-shell-chrome", "@explorer", "@search", "@smoke"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });

      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchPanelHost}"]`),
      ).toHaveCount(0);

      await openViewMenu(page);
      const toggle = page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`);
      await expect(toggle).toBeVisible();
      await expect(toggle).toHaveAttribute("aria-expanded", "false");
      await expect(toggle).toHaveAttribute(
        "aria-controls",
        "explorer-search-panel",
      );

      await toggle.click();
      await expect(toggle).toHaveAttribute("aria-expanded", "true");
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchPanelHost}"]`),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchPanel}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchInput}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchSubmit}"]`),
      ).toBeVisible();

      // View dropdown stays open for view toggles — flip Search off in place
      // (re-clicking View would collapse the menu and hide the toggle test id).
      await page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchPanelHost}"]`),
      ).toHaveCount(0);
    },
  );

  test(
    "Content → Search opens the same Search panel",
    { tag: ["@explorer-shell-chrome", "@explorer", "@search"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });

      await openContentMenu(page);
      const contentSearch = page.locator(
        `[data-testid="${TEST_IDS.contentSearch}"]`,
      );
      await expect(contentSearch).toBeVisible();
      await expect(contentSearch).toHaveAttribute(
        "role",
        "menuitemcheckbox",
      );
      await expect(contentSearch).toHaveAttribute(
        "aria-controls",
        "explorer-search-panel",
      );

      await contentSearch.click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchPanelHost}"]`),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchInput}"]`),
      ).toBeVisible();
    },
  );

  test(
    "axe-core a11y gate — shell with Search panel expanded",
    { tag: ["@explorer-shell-chrome", "@explorer", "@a11y"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });
      await openViewMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.searchPanelHost}"]`),
      ).toBeVisible({ timeout: 10_000 });
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.shell}"]`,
      });
    },
  );
});
