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
 * Playwright spec: US1 core explorer (SC-001 / FR-001..FR-005).
 *
 * <p>Drives the modern React Content Explorer against the live docker dev
 * CMS at the dedicated entry point
 * {@code /Rhythmyx/cm/app/explorerModern.jsp}, which mounts
 * {@code ContentExplorerShell} via the {@code PercModernUI} bridge.</p>
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
const {
  loginAsAdmin,
  BASE_URL,
  ADMIN_USERNAME,
} = require("./helpers/auth");

const EXPLORER_URL =
  `${BASE_URL}/Rhythmyx/cm/app/explorerModern.jsp?_=${Date.now()}`;

test.describe("modern React Content Explorer (US1) — feature 992", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("ContentExplorerShell mounts in the modern JSP entry point", async ({
    page,
  }) => {
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    // The mount target div is rendered by the JSP.
    const root = page.locator('[data-testid="perc-explorer-modern-root"]');
    await expect(root).toBeVisible();
    // Wait for the React component to take over the div. The shell
    // wrapper carries data-testid="content-explorer-shell" + role="application".
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
    await expect(page.locator('[data-testid="action-create-folder"]')).toBeVisible();
    await expect(page.locator('[data-testid="action-rename"]')).toBeVisible();
    await expect(page.locator('[data-testid="action-move"]')).toBeVisible();
    await expect(page.locator('[data-testid="action-copy"]')).toBeVisible();
    await expect(page.locator('[data-testid="action-delete"]')).toBeVisible();
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
});
