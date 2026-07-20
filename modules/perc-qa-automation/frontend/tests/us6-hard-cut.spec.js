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
 * Playwright spec: US6 hard cut (SC-006 / FR-019b / FR-020).
 *
 * <p>After US6, production Web Management content entry loads **zero**
 * miller-column Finder chrome for the primary navigation. The modern React
 * Content Explorer is the supported path. This spec asserts that against
 * the live docker dev CMS at {@code http://localhost:9992}.</p>
 *
 * <p>Scope: this spec drives a representative sample of the shells that
 * mounted Finder (webmgt, dashboard, admin, editAsset, editTemplate,
 * adminWorkflow, users, siteArchitecture per the cutover-inventory §A).
 * Each shell that has been hard-cut asserts no miller Finder chrome
 * loads. Shells that haven't been hard-cut yet are marked
 * {@code test.skip} with a {@code BUG:} note; flip to {@code test} when
 * the cutover completes for that shell.</p>
 *
 * <p>Run from {@code modules/perc-qa-automation/frontend}:</p>
 * <pre>
 *   npm test -- tests/us6-hard-cut.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

/**
 * Each entry is a shell that previously mounted miller-column Finder.
 * The {@code asserted} flag is true if the shell has been hard-cut;
 * false (test.skip) if the cutover is still in progress.
 *
 * Hard-cut shells assert:
 *   1. No miller-column Finder chrome loads (no `.perc-mcol` element).
 *   2. Modern ContentExplorerShell mounts (data-testid="content-explorer-shell"
 *      is visible in the page after the PercModernUI bridge calls mount).
 */
const SHELLS = [
  {
    name: "webmgt (primary editor)",
    path: "/Rhythmyx/cm/app/webmgt.jsp",
    asserted: true, // T031 complete on 2026-07-19
    expectModernShell: true, // modern explorer mounts in the main panel
  },
  {
    name: "dashboard",
    path: "/Rhythmyx/cm/app/dashboard.jsp",
    asserted: true, // T031 complete on 2026-07-19
    expectModernShell: true,
  },
  {
    name: "admin",
    path: "/Rhythmyx/cm/app/admin.jsp",
    asserted: true, // T031 complete on 2026-07-19
    expectModernShell: true,
  },
  {
    name: "editAsset",
    path: "/Rhythmyx/cm/app/editAsset.jsp",
    asserted: true, // T031 complete on 2026-07-19
    expectModernShell: true,
  },
  {
    name: "editTemplate",
    path: "/Rhythmyx/cm/app/editTemplate.jsp",
    asserted: true, // T031 complete on 2026-07-19
    expectModernShell: true,
  },
  {
    name: "adminWorkflow",
    path: "/Rhythmyx/cm/app/adminWorkflow.jsp",
    asserted: true, // T031 complete on 2026-07-19
    expectModernShell: true,
  },
  {
    name: "users",
    path: "/Rhythmyx/cm/app/users.jsp",
    asserted: true, // T031 complete on 2026-07-19
    expectModernShell: true,
  },
  {
    name: "siteArchitecture",
    path: "/Rhythmyx/cm/app/siteArchitecture.jsp",
    asserted: true, // T031 complete on 2026-07-19
    expectModernShell: true,
  },
  {
    name: "explorerModern (dedicated modern entry point)",
    path: "/Rhythmyx/cm/app/explorerModern.jsp",
    asserted: true, // T024 complete on 2026-07-19
    expectModernShell: true,
  },
];

test.describe("US6 hard cut — no miller-column Finder chrome (SC-006)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  for (const shell of SHELLS) {
    const testFn = shell.asserted ? test : test.skip;
    const testName = shell.asserted
      ? `${shell.name} (${shell.path}) has no miller-column Finder chrome`
      : `${shell.name} (${shell.path}) [PENDING — cutover in progress]`;

    testFn(testName, async ({ page }) => {
      await page.goto(`${BASE_URL}${shell.path}?_=${Date.now()}`, {
        waitUntil: "networkidle",
      });

      // No miller-column Finder chrome. The legacy Finder renders a
      // `.perc-mcol` element. After hard cut, none is present.
      // Note: `#perc-web-management` may still exist as a wrapper div
      // for the modern explorer mount — that's intentional. The hard cut
      // replaces the INSIDE of the wrapper, not the wrapper itself.
      await expect(page.locator(".perc-mcol")).toHaveCount(0);

      if (shell.expectModernShell) {
        // Dedicated modern entry point: ContentExplorerShell mounts.
        const shellEl = page.locator(
          '[data-testid="content-explorer-shell"]',
        );
        await expect(shellEl).toBeVisible({ timeout: 15_000 });
      }
    });
  }
});

test.describe("US6 hard cut — cutover inventory evidence (FR-022)", () => {
  test("primary-nav entry points are modern-only after US6", async ({ page }) => {
    // After T031 (webmgt.jsp rewired to mount ContentExplorerShell,
    // STOP including miller Finder for primary exploration), the
    // hard-cut evidence is the absence of any Finder widget in the
    // primary nav region. For webmgt, the page still has an editor
    // iframe (that's T031's intentional split), so the assertion is
    // scoped to the Finder chrome.
    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/webmgt.jsp?_=${Date.now()}`, {
      waitUntil: "networkidle",
    });

    // The legacy Finder exposes a specific DOM signature (`.perc-mcol`).
    // After T031, only the legacy Finder chrome is gone — the modern
    // ContentExplorerShell mounts in its place. The `#perc-web-management`
    // wrapper div is intentionally retained (it hosts the modern explorer
    // in webmgt), so the per-shell loop no longer asserts on it; the
    // cutover-inventory block uses the same scoped assertion.
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });
});