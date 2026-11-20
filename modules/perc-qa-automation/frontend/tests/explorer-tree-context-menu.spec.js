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
 * Playwright surface: #6826 / parent #6824 — Explorer folder tree context menu.
 *
 * <p>Right-clicking a tree folder opens the server catalog context menu
 * scoped to that folder (nav-pane parity with the detail-list row menu,
 * #3629). Keyboard equivalent: focus a tree row and press Shift+F10.
 * The popup mounts immediately with a loading placeholder, moves focus
 * inside (so Escape closes it), and carries the folder label in its
 * accessible name.</p>
 *
 * <p>Tags: {@code @explorer-tree-context-menu} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-tree-context-menu.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSpaUrl,
} = require("./helpers/explorer-menu-bar");

async function openExplorerTree(page) {
  await page.goto(`${explorerSpaUrl(BASE_URL)}&path=/Sites`, {
    waitUntil: "networkidle",
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.locator('[data-testid="explorer-tree"]')).toBeVisible({
    timeout: 15_000,
  });
  const node = page
    .locator(
      '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
    )
    .first();
  await expect(node, "H2 Explorer tree must render a Sites node").toBeVisible({
    timeout: 15_000,
  });
  const row = node.locator('[role="treeitem"]').first();
  await expect(row).toBeVisible({ timeout: 10_000 });
  return { node, row };
}

test.describe("modern React Content Explorer — folder tree context menu (#6826)", () => {
  let pageErrors = [];

  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    pageErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("response", (res) => {
      if (res.status() < 500) return;
      const url = res.url();
      if (/\/actions\/find(\?|$)/i.test(url)) {
        pageErrors.push(`HTTP ${res.status()} ${url}`);
      }
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const text = msg.text();
      if (
        /Failed to load resource: the server responded with a status of (404|400|500)/i.test(
          text,
        )
      ) {
        return;
      }
      pageErrors.push(text);
    });
    await loginAsAdmin(page);
  });

  test.afterEach(() => {
    expect(pageErrors, `JS page/console errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });

  test(
    "right-click on a tree folder opens the folder-scoped server menu",
    { tag: ["@explorer-tree-context-menu", "@explorer", "@smoke"] },
    async ({ page }) => {
      const { row } = await openExplorerTree(page);

      await row.click({ button: "right" });
      const menu = page.locator(`[data-testid="${TEST_IDS.contextMenu}"]`);
      await expect(menu).toBeVisible({ timeout: 10_000 });
      // Accessible name carries the folder label (#6826 polish).
      await expect(menu).toHaveAttribute("aria-label", /Context menu for /);

      // The popup answers before the catalog resolves, then swaps in the
      // loaded MENU pivots (nested, not a flat label dump).
      const pivots = menu.locator(
        '[data-testid^="context-menu-item-"][aria-haspopup="menu"]',
      );
      await expect
        .poll(async () => pivots.count(), { timeout: 15_000 })
        .toBeGreaterThan(0);

      // Focus moved into the popup so Escape closes it from the keyboard.
      await expect(menu).toBeFocused({ timeout: 5_000 });
      await page.keyboard.press("Escape");
      await expect(menu).toHaveCount(0);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-tree"]',
      });
    },
  );

  test(
    "Shift+F10 on a focused tree row opens the same menu",
    { tag: ["@explorer-tree-context-menu", "@explorer"] },
    async ({ page }) => {
      const { row } = await openExplorerTree(page);

      await row.focus();
      await page.keyboard.press("Shift+F10");
      const menu = page.locator(`[data-testid="${TEST_IDS.contextMenu}"]`);
      await expect(menu).toBeVisible({ timeout: 10_000 });
      const pivots = menu.locator(
        '[data-testid^="context-menu-item-"][aria-haspopup="menu"]',
      );
      await expect
        .poll(async () => pivots.count(), { timeout: 15_000 })
        .toBeGreaterThan(0);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-tree"]',
      });
    },
  );
});
