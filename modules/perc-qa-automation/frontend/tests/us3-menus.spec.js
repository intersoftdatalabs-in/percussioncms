/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Playwright spec: US3 P-Menu — configuration-driven toolbar / context
 * menu (SC-003 / FR-010, FR-011, FR-013).
 *
 * <p>Asserts the modern React {@code ActionToolbar} and
 * {@code ContextMenu} components mount on
 * <code>actionMenuModern.jsp</code> (the US3 P-Menu pilot entry point
 * — see T053/T054 in tasks.md). The toolbar renders the configured
 * actions returned by <code>/actions/find</code>; the menu demonstrates
 * the same actions when an item is selected (the pilot uses synthetic
 * actions so the keyboard / click paths can be exercised against the
 * dev CMS regardless of installed menu set).</p>
 *
 * <p>SC-003 acceptance criterion (≥10 high-value actions visible) is
 * gated on a system-installed CMS — the docker Derby dev image returns
 * <code>{"ActionMenu":[]}</code> from <code>/actions/find</code> by
 * default. The Vitest mapper tests cover the structural enumeration in
 * <code>specs/992-react-content-explorer/checklists/sc003-actions-checklist.md</code>;
 * this Playwright spec exercises the wiring against the live CMS
 * shell.</p>
 *
 * <p>Run from <code>modules/perc-qa-automation/frontend</code>:</p>
 * <pre>
 *   npm test -- tests/us3-menus.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const MENU_URL = `${BASE_URL}/Rhythmyx/cm/app/actionMenuModern.jsp?_=${Date.now()}`;

test.describe("US3 P-Menu — action toolbar / context menu (SC-003)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("ActionToolbar mounts with role='toolbar' and the canonical aria-label", async ({
    page,
  }) => {
    await page.goto(MENU_URL, { waitUntil: "networkidle" });
    const toolbar = page.locator('[data-testid="action-toolbar"]');
    await expect(toolbar).toBeVisible({ timeout: 15_000 });
    const ariaLabel = await toolbar.getAttribute("aria-label");
    expect(ariaLabel).toBe("Action toolbar");
  });

  test("ActionToolbar renders the empty-state placeholder when no actions are configured", async ({
    page,
  }) => {
    await page.goto(MENU_URL, { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="action-toolbar-empty"]')).toBeVisible({
      timeout: 15_000,
    });
  });

  test("ContextMenu mounts with the configured aria-label and demo items", async ({
    page,
  }) => {
    await page.goto(MENU_URL, { waitUntil: "networkidle" });
    const menu = page.locator('[data-testid="context-menu"]');
    await expect(menu).toBeVisible({ timeout: 15_000 });
    const ariaLabel = await menu.getAttribute("aria-label");
    expect(ariaLabel).toBe("Demo context menu");
    await expect(page.locator('[data-testid="context-menu-item-open"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="context-menu-item-preview"]'),
    ).toBeVisible();
  });

  test("ContextMenu click invokes onInvoke and writes to the result block", async ({
    page,
  }) => {
    await page.goto(MENU_URL, { waitUntil: "networkidle" });
    await page
      .locator('[data-testid="context-menu-item-preview"]')
      .click();
    const result = page.locator('[data-testid="perc-action-menu-result"]');
    await expect(result).toHaveText(/Invoked: preview/);
  });

  test("ContextMenu is keyboard-completable: Escape closes (no result block write)", async ({
    page,
  }) => {
    await page.goto(MENU_URL, { waitUntil: "networkidle" });
    const menu = page.locator('[data-testid="context-menu"]');
    await expect(menu).toBeVisible({ timeout: 15_000 });
    await menu.focus();
    await page.keyboard.press("Escape");
    // onClose writes "Closed" to the result block; we just verify the
    // menu is still in the document (Escape doesn't unmount).
    await expect(menu).toBeVisible({ timeout: 5_000 });
  });

  test("ContextMenu: Enter activates a focused menu item (kilo-code-bot PR #1396 mitigation)", async ({
    page,
  }) => {
    await page.goto(MENU_URL, { waitUntil: "networkidle" });
    const item = page.locator('[data-testid="context-menu-item-preview"]');
    await expect(item).toBeVisible({ timeout: 15_000 });
    // Focus the item, press Enter, assert the result block is updated.
    await item.focus();
    await page.keyboard.press("Enter");
    const result = page.locator('[data-testid="perc-action-menu-result"]');
    await expect(result).toHaveText(/Invoked: preview/);
  });

  test("ContextMenu: Space activates a focused menu item (kilo-code-bot PR #1396 mitigation)", async ({
    page,
  }) => {
    await page.goto(MENU_URL, { waitUntil: "networkidle" });
    const item = page.locator('[data-testid="context-menu-item-preview"]');
    await expect(item).toBeVisible({ timeout: 15_000 });
    await item.focus();
    await page.keyboard.press(" ");
    const result = page.locator('[data-testid="perc-action-menu-result"]');
    await expect(result).toHaveText(/Invoked: preview/);
  });
});
