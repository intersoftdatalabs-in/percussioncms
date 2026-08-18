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
 * Playwright spec: #2400 / #2408 Explorer multi-select + clipboard wiring.
 *
 * <p>Verifies that the modern React Content Explorer (feature 992 +
 * parity #2400, slice #2408) renders the multi-select checkbox column
 * in the detail list, accumulates selected items, and feeds them into
 * the {@code ClipboardPanel} for cut / copy / paste.</p>
 *
 * <p>This spec is the live-CMS companion to the Vitest assertions in
 * {@code WebUI/src/test/ts/contentExplorer/DetailList.test.tsx} and
 * {@code ContentExplorerShell.test.tsx}. Vitest covers the pure logic
 * under jsdom; this spec covers the integrated Chrome + SPA shell
 * behavior on a running CMS (dev mode hot copy, or QA mode Docker
 * stack per {@code modules/perc-qa-automation/AGENTS.md}).</p>
 *
 * <p>The CMS is expected to be running on {@link BASE_URL}
 * (`http://localhost:9992` by default; QA mode uses
 * {@code TEST_CMS_URL}). Bring it up via:
 * {@code python docker/scripts/perc-devctl.py qa-up} or
 * {@code ./docker/scripts/perc-devctl.py up} (dev mode).</p>
 *
 * <p>Status as of 2026-08-08: the underlying Explorer shell mounts in
 * the SPA but is not yet wired as the home entry (see
 * {@code WebUI/AGENTS.md} → active focus is Home). When the Explorer
 * entry is live, run this spec via
 * {@code npm run test:surface -- --path tests/explorer-multiselect.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Until then, the
 * spec is committed so CI / dev can execute it once the entry is
 * available.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("modern React Content Explorer — multi-select + clipboard (#2408)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(30_000);
    await loginAsAdmin(page);
    // Cache-buster so the SPA picks up the latest bundle.
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test("renders the multi-select checkbox column in the detail list", async ({ page }) => {
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible();
    // The list mounts as soon as the Explorer opens a folder; assert
    // the checkbox column header is present.
    const header = page.locator('[data-testid="detail-col-header-select"]');
    await expect(header).toBeVisible({ timeout: 10_000 });
  });

  test("selecting two rows surfaces the multi-select count", async ({ page }) => {
    // Read whichever row ids the live list exposes so the spec does
    // not couple to fixture-specific ids (e.g. "p-1") the server
    // may not return on every CMS install. We click the first two
    // row checkboxes regardless of their ids.
    const list = page.locator('[data-testid="detail-list"]');
    await list.waitFor({ timeout: 10_000 });
    const rowCheckboxes = list.locator('tbody tr input[type="checkbox"]');
    await expect(rowCheckboxes.nth(0)).toBeVisible();
    await rowCheckboxes.nth(0).check();
    await rowCheckboxes.nth(1).check();
    const count = page.locator('[data-testid="explorer-multi-select-count"]');
    await expect(count).toContainText("2");
  });

  test("add to clipboard + paste panel flow", async ({ page }) => {
    const list = page.locator('[data-testid="detail-list"]');
    await list.waitFor({ timeout: 10_000 });
    const rowCheckboxes = list.locator('tbody tr input[type="checkbox"]');
    await rowCheckboxes.nth(0).check();
    await rowCheckboxes.nth(1).check();

    // #2731 / #3544 / #3551: Add lives under Content and opens the clipboard
    // panel. View → Clipboard must already be checked — do not click it
    // again here or the toggle would hide the already-open panel.
    await page.locator('[data-testid="explorer-menu-content"]').click();
    await page.locator('[data-testid="explorer-clipboard-add"]').click();

    await page.locator('[data-testid="explorer-menu-view"]').click();
    const toggle = page.locator('[data-testid="explorer-toggle-clipboard"]');
    await expect(toggle).toHaveAttribute("aria-checked", "true");
    const panel = page.locator('[data-testid="explorer-clipboard-panel"]');
    await expect(panel).toBeVisible();

    // Both items are in the clipboard list.
    const rows = page.locator('[data-testid="clipboard-item-row"]');
    await expect(rows).toHaveCount(2);
  });
});
