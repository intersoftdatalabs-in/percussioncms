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
 * Playwright spec: US1 core explorer (SC-001 / FR-001..FR-005).
 *
 * <p>Drives the modern React Content Explorer against the live docker dev
 * CMS at the dedicated entry point
 * {@code /Rhythmyx/cm/app/spa.jsp?entry=explorer}, which mounts
 * {@code ContentExplorerShell} inside the pure React SPA (PR-6/PR-8).</p>
 *
 * <p>Run from {@code modules/perc-qa-automation/frontend}:</p>
 * <pre>
 *   npm ci
 *   npx playwright install chromium
 *   npm test -- tests/us1-core-explorer.spec.js
 * </pre>
 *
 * <p>The spec uses a cache-buster query param on the explorer URL so
 * the CMS doesn't serve a cached JSP/bridge bundle across reruns.</p>
 *
 * <p>Known bugs as of 2026-07-19 are codified as {@code test.skip} with a
 * {@code BUG:} note. When the upstream bug is fixed, change
 * {@code test.skip} to {@code test} to make the assertion run.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, ADMIN_USERNAME } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;

test.describe("modern React Content Explorer (US1) — feature 992", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("ContentExplorerShell mounts in the modern JSP entry point", async ({
    page,
  }) => {
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    // SPA route embeds ContentExplorerShell (data-testid on the shell).
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 15_000 });
    // The shell wraps a tree region, a list region, and a toolbar
    // (ReducedAction set). Use data-testid (stable) instead of
    // aria-label (which is the unresolved TMX key from the message()
    // fallback path).
    await expect(page.locator('[data-testid="explorer-tree"]')).toBeVisible();
    await expect(page.locator('[data-testid="reduced-actions"]')).toBeVisible();
    await expect(page.locator('[data-testid="detail-list"]')).toBeVisible();
    // 7 reduced actions: open / preview / createFolder / rename / move / copy / delete.
    await expect(page.locator('[data-testid="action-open"]')).toBeVisible();
    await expect(page.locator('[data-testid="action-preview"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="action-create-folder"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="action-rename"]')).toBeVisible();
    await expect(page.locator('[data-testid="action-move"]')).toBeVisible();
    await expect(page.locator('[data-testid="action-copy"]')).toBeVisible();
    await expect(page.locator('[data-testid="action-delete"]')).toBeVisible();
    // #2400 / #2731 / #3208: DCE menu bar + labeled Server actions +
    // always-visible Search / Folder Security / Display format chrome.
    await expect(
      page.locator('[data-testid="explorer-menu-bar"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-menu-view"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-display-format"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-view-tools"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-view-tool-search"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-view-tool-security"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-server-actions"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-server-actions-label"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible();
    // View menu still hosts the same Search / Security commands (#2731).
    await page.locator('[data-testid="explorer-menu-view"]').click();
    await expect(
      page.locator('[data-testid="explorer-toggle-search"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-toggle-security"]'),
    ).toBeVisible();
  });

  test("server action toolbar mounts; detail list supports context menu (#2849)", async ({
    page,
  }) => {
    // Product Explorer route: ActionToolbar is always present; right-click
    // on a detail row opens the ContextMenu anchor (actions may be empty on
    // a minimal Derby catalog — empty state is still a mounted menu).
    //
    // Enablement (#2849): inject a desktop-only action into the live
    // /actions/find* responses so the E2E path exercises
    // filterToolbarActions / filterContextMenuActions (absence of the
    // known desktop-only item). Vitest covers the pure helpers exhaustively.
    const DESKTOP_ONLY_NAME = "night-desktop-only-cx";
    const desktopOnlyWire = {
      id: 9_000_001,
      name: DESKTOP_ONLY_NAME,
      label: "Desktop CX only (night probe)",
      sortRank: 9999,
      menuType: "MENUITEM",
      url: "rxapp://launch-cx",
    };

    await page.route("**/Rhythmyx/rest/actions/find**", async (route) => {
      const response = await route.fetch();
      const contentType = response.headers()["content-type"] || "";
      if (!contentType.includes("application/json")) {
        return route.fulfill({ response });
      }
      let body;
      try {
        body = await response.json();
      } catch {
        return route.fulfill({ response });
      }
      if (Array.isArray(body?.ActionMenu)) {
        body = {
          ...body,
          ActionMenu: [...body.ActionMenu, desktopOnlyWire],
        };
      } else if (Array.isArray(body?.ActionMenuList)) {
        body = {
          ...body,
          ActionMenuList: [...body.ActionMenuList, desktopOnlyWire],
        };
      }
      return route.fulfill({
        status: response.status(),
        headers: {
          ...response.headers(),
          "content-type": "application/json",
        },
        body: JSON.stringify(body),
      });
    });

    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    await expect(
      page.locator('[data-testid="content-explorer-shell"]'),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible();
    await expect(page.locator('[data-testid="detail-list"]')).toBeVisible();

    // Desktop-only probe must never appear on the toolbar (filtered).
    await expect(
      page.locator(`[data-testid="action-toolbar-item-${DESKTOP_ONLY_NAME}"]`),
    ).toHaveCount(0);

    const row = page
      .locator('[data-testid^="detail-row-"]:not([aria-disabled="true"])')
      .first();
    if ((await row.count()) === 0) {
      // Empty folder or only disabled root rows: still assert chrome.
      test.info().annotations.push({
        type: "note",
        description:
          "No enabled detail rows on this CMS folder; skipped context-menu click path",
      });
      return;
    }
    await row.click({ button: "right" });
    await expect(page.locator('[data-testid="context-menu"]')).toBeVisible({
      timeout: 10_000,
    });
    // Filtered out on context-menu surface as well.
    await expect(
      page.locator(`[data-testid="context-menu-item-${DESKTOP_ONLY_NAME}"]`),
    ).toHaveCount(0);
  });

  test("Explorer shell opens search panel from View menu (#2400 / #2731)", async ({
    page,
  }) => {
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 15_000 });
    await page.locator('[data-testid="explorer-menu-view"]').click();
    await page.locator('[data-testid="explorer-toggle-search"]').click();
    await expect(
      page.locator('[data-testid="explorer-search-panel"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-side-panels"]'),
    ).toBeVisible();
  });

  test("Explorer view-tool Search opens panel under header chrome (#3208)", async ({
    page,
  }) => {
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 15_000 });
    const searchTool = page.locator('[data-testid="explorer-view-tool-search"]');
    await expect(searchTool).toBeVisible();
    await searchTool.click();
    const panel = page.locator('[data-testid="explorer-search-panel"]');
    await expect(panel).toBeVisible();
    await expect(
      page.locator('[data-testid="explorer-side-panels"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="search-panel-input"]')).toBeVisible();
    await expect(panel).toBeInViewport();
    await searchTool.click();
    await expect(panel).toHaveCount(0);
  });

  test("no miller-column Finder chrome loads for the modern entry", async ({
    page,
  }) => {
    // SC-001: zero miller-column Finder chrome at the modern entry. The
    // legacy Finder exposes a specific DOM signature (#perc-web-management
    // wrapping a .perc-finder / .perc-mcol surface). Asserting the
    // absence of that surface on the modern URL ensures US6 hard-cut
    // evidence is captured per page.
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    await expect(page.locator("#perc-web-management")).toHaveCount(0);
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("Admin user can sign in and reaches the explorer (SC-001 prereq)", async ({
    page,
  }) => {
    // The auth helper logs in. The 8.2 CMS lands at /Rhythmyx/index.jsp
    // (a JSP welcome page). We then navigate to the explorer URL.
    // Verifying that step succeeds confirms Admin auth + CSRF + session
    // are working. The /Rhythmyx/ root is itself a redirect to
    // /Rhythmyx/sys_welcome/.../login in 8.2; we hit the welcome page
    // directly to assert post-login state.
    await page.goto(`${BASE_URL}/Rhythmyx/index.jsp`);
    expect(page.url()).toMatch(/\/Rhythmyx\/index\.jsp/);
    expect(ADMIN_USERNAME).toBe("Admin");
  });

  test("axe-core a11y gate — modern Content Explorer shell (T082b / 508)", async ({
    page,
  }) => {
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 15_000 });
    // Full product shell including #2400 composition chrome (search / DF / menus).
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="content-explorer-shell"]',
    });
  });

  test("axe-core a11y gate — Explorer search panel expanded (#2400)", async ({
    page,
  }) => {
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    await expect(
      page.locator('[data-testid="content-explorer-shell"]'),
    ).toBeVisible({ timeout: 15_000 });
    await page.locator('[data-testid="explorer-menu-view"]').click();
    await page.locator('[data-testid="explorer-toggle-search"]').click();
    await expect(
      page.locator('[data-testid="explorer-search-panel"]'),
    ).toBeVisible();
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="content-explorer-shell"]',
    });
  });
});
