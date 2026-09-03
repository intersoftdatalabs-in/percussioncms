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
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

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
    // #3306: classic Design list JSP hard-redirects to SPA Design (not explorer mount)
    path: "/Rhythmyx/cm/app/admin.jsp",
    asserted: true,
    expectModernShell: false,
    expectDesignSpa: true,
  },
  {
    name: "editAsset",
    // #3473: leftover JSP 301s to SPA editor (not explorer mount)
    path: "/Rhythmyx/cm/app/editAsset.jsp",
    asserted: true,
    expectModernShell: false,
    expectEditorSpa: true,
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
    // #3099 / #3587: retired JSP URL 301s to SPA Architecture (not explorer mount)
    path: "/Rhythmyx/cm/app/siteArchitecture.jsp",
    asserted: true,
    expectModernShell: false,
    expectArchitectureSpa: true,
  },
  {
    name: "SPA explorer (spa.jsp?entry=explorer)",
    path: "/Rhythmyx/cm/app/spa.jsp?entry=explorer",
    asserted: true, // T024 complete on 2026-07-19; PR-8 product host removed
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
      // Paths may already include query (e.g. spa.jsp?entry=explorer).
      const sep = shell.path.includes("?") ? "&" : "?";
      await page.goto(`${BASE_URL}${shell.path}${sep}_=${Date.now()}`, {
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
        const shellEl = page.locator('[data-testid="content-explorer-shell"]');
        await expect(shellEl).toBeVisible({ timeout: 15_000 });
      }
      if (shell.expectArchitectureSpa) {
        // #3099 / #3587: siteArchitecture.jsp bookmark → SPA Architecture shell
        await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
          timeout: 30_000,
        });
      }
      if (shell.expectDesignSpa) {
        // #3306: admin.jsp → SPA Design template library (multi-hop redirect)
        await expect(page.getByTestId("perc-design-shell")).toBeVisible({
          timeout: 30_000,
        });
      }
      if (shell.expectEditorSpa) {
        // #3473: editAsset.jsp bookmark → React Content Editor host
        await expect(page.getByTestId("editor-host")).toBeVisible({
          timeout: 30_000,
        });
      }
    });
  }
});

test.describe("US6 hard cut — cutover inventory evidence (FR-022)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("primary-nav entry points are modern-only after US6", async ({
    page,
  }) => {
    // After T031 (webmgt.jsp rewired to mount ContentExplorerShell,
    // STOP including miller Finder for primary exploration), the
    // hard-cut evidence is the absence of any Finder widget in the
    // primary nav region. For webmgt, the page still has an editor
    // iframe (that's T031's intentional split), so the assertion is
    // scoped to the Finder chrome.
    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/webmgt.jsp?_=${Date.now()}`, {
      waitUntil: "networkidle",
    });

    // Do not treat a login snapshot as a pass: wait for the signed-in
    // explorer mount, then assert miller Finder chrome is gone.
    await expect(
      page.locator('[data-testid="content-explorer-shell"]'),
    ).toBeVisible({ timeout: 15_000 });

    // The legacy Finder exposes a specific DOM signature (`.perc-mcol`).
    // After T031, only the legacy Finder chrome is gone — the modern
    // ContentExplorerShell mounts in its place. The `#perc-web-management`
    // wrapper div is intentionally retained (it hosts the modern explorer
    // in webmgt), so the per-shell loop no longer asserts on it; the
    // cutover-inventory block uses the same scoped assertion.
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("axe-core a11y gate — hard-cut explorer mounts (T082b)", async ({
    page,
  }) => {
    // Spot-check the first modern entry point for serious/critical
    // a11y regressions. A failing test here is a release-blocker for
    // SC-009 (a11y) and SC-012 (FR-029 parity).
    //
    // Cycle-verify residual (#3613): this describe used to goto spa.jsp
    // without loginAsAdmin, so axe include ran on Sign in and threw
    // "No elements found for include". Login snapshot is not a pass.
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
      { waitUntil: "networkidle" },
    );
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 15_000 });
    // Single include: comma-OR include is fragile (axe requires every
    // listed selector to match). explorer-tree is nested in the shell.
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="content-explorer-shell"]',
    });
    const unexpected = consoleErrors.filter(
      (t) =>
        !/ResizeObserver/i.test(t) &&
        !/Download the React DevTools/i.test(t),
    );
    expect(unexpected, `console/page errors:\n${unexpected.join("\n")}`).toEqual(
      [],
    );
  });
});
