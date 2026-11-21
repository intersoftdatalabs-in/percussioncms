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
 * Playwright surface: #6863 / parent #6826 — detail-list rows open the
 * server catalog context menu from the keyboard.
 *
 * <p>Focusing a detail row and pressing Shift+F10 (or the context-menu
 * key) opens the same item-scoped server menu as pointer right-click
 * (#3629) — tree folder rows gained the same equivalent in #6826. The
 * popup mounts immediately with a loading placeholder, moves focus
 * inside (so Escape closes it), and carries the row label in its
 * accessible name.</p>
 *
 * <p>Tags: {@code @explorer-detail-keyboard-menu} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-detail-list-keyboard-menu.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSpaUrl,
} = require("./helpers/explorer-menu-bar");

async function openExplorerDetailList(page) {
  await page.goto(`${explorerSpaUrl(BASE_URL)}&path=/Sites`, {
    waitUntil: "networkidle",
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.locator('[data-testid="detail-list"]')).toBeVisible({
    timeout: 15_000,
  });
  const rows = page.locator(
    '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])',
  );
  await expect(
    rows.first(),
    "H2 Explorer must have ≥1 enabled Sites detail row; no soft-skip (#6863)",
  ).toBeVisible({ timeout: 15_000 });
  return rows.first();
}

test.describe("modern React Content Explorer — detail-list keyboard context menu (#6863)", () => {
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
    "Shift+F10 on a focused detail row opens the row-scoped server menu",
    { tag: ["@explorer-detail-keyboard-menu", "@explorer", "@smoke"] },
    async ({ page }) => {
      const row = await openExplorerDetailList(page);

      await row.focus();
      await page.keyboard.press("Shift+F10");
      const menu = page.locator(`[data-testid="${TEST_IDS.contextMenu}"]`);
      await expect(menu).toBeVisible({ timeout: 10_000 });
      // Accessible name carries the row label (#6863 polish).
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
        scope: '[data-testid="detail-list"]',
      });
    },
  );
});
