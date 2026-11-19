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
 * Playwright surface: #6603 Explorer menu-bar + context-menu icon coverage
 * (follow-on to #6515, parent #4530).
 *
 * <p>Asserts the DCE-familiar icon+label chrome beyond the server-driven
 * {@code action-toolbar}: every visible Explorer menu-bar dropdown item
 * (Content / View / Help) renders at most one decorative
 * {@code svg[aria-hidden=true]} next to a non-empty visible label, and
 * every item of the row context menu renders at most one decorative
 * context-menu icon. Accessible names still match visible labels
 * (i18n/a11y unchanged); unmapped commands stay label-only; the empty
 * context-menu state stays text-only. The shell passes
 * {@code expectNoSeriousA11yViolations} scoped to
 * {@code content-explorer-shell}.</p>
 *
 * <p>Tags: {@code @explorer-menu-context-icons} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-menu-context-icons.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
  openContentMenu,
  openViewMenu,
} = require("./helpers/explorer-menu-bar");
const {
  expectNoSeriousA11yViolations,
} = require("./helpers/a11y");

/**
 * Summarize icon+label chrome for every menuitem under a dropdown/menu root.
 * @param {import('@playwright/test').Page} page
 * @param {import('@playwright/test').Locator} root
 * @param {string} iconTestId
 */
async function summarizeMenuItems(page, root, iconTestId) {
  return root
    .locator('[role="menuitem"], [role="menuitemcheckbox"]')
    .evaluateAll((els, testId) =>
      els.map((el) => {
        const name =
          el.getAttribute("aria-label") || (el.textContent || "").trim();
        const visible = (el.textContent || "").trim();
        const icons = el.querySelectorAll(`[data-testid="${testId}"] > svg`);
        return {
          testid: el.getAttribute("data-testid") || "",
          name,
          visible,
          iconCount: icons.length,
          iconHidden: Array.from(icons).every(
            (svg) => svg.getAttribute("aria-hidden") === "true",
          ),
        };
      }),
      iconTestId,
    );
}

/**
 * Assert icon+label invariants shared by both chrome surfaces.
 * @param {import('@playwright/test').TestInfo} info
 * @param {boolean} [matchName] when false, the accessible name and the
 * visible label are asserted non-empty independently (menu-bar toggles
 * carry descriptive aria-labels such as "Toggle search panel" that differ
 * from the visible label by design).
 */
function expectIconLabelChrome(info, summary, surface, matchName = true) {
  expect(summary.length, `${surface} must render at least one item`).toBeGreaterThan(0);
  info.annotations.push({
    type: "note",
    description: `${surface} items=${summary.length} withIcon=${summary.filter((s) => s.iconCount > 0).length}`,
  });
  expect(
    summary.filter((s) => s.iconCount > 0).length,
    `at least one ${surface} item renders an icon — otherwise the pipeline is dead on the live catalog`,
  ).toBeGreaterThan(0);
  for (const row of summary) {
    expect(row.name, `${surface} ${row.testid} keeps its accessible name`).not.toBe("");
    if (matchName) {
      expect(row.visible, `${surface} ${row.testid} keeps its visible label`).toContain(
        row.name,
      );
    } else {
      expect(row.visible, `${surface} ${row.testid} keeps its visible label`).not.toBe("");
    }
    expect(
      row.iconCount,
      `${surface} ${row.testid} renders at most one icon`,
    ).toBeLessThanOrEqual(1);
    expect(row.iconHidden, `${surface} ${row.testid} icon is aria-hidden`).toBe(true);
  }
}

test.describe("modern React Content Explorer — menu-bar + context-menu icons (#6603)", () => {
  /** Uncaught page errors + unexpected console errors for C5. */
  let pageErrors = [];

  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    pageErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const text = msg.text();
      if (
        /Failed to load resource: the server responded with a status of (404|400)/i.test(
          text,
        )
      ) {
        return;
      }
      pageErrors.push(text);
    });
    await loginAsAdmin(page);
    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
  });

  test.afterEach(() => {
    expect(pageErrors, `JS page/console errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });

  test(
    "menu-bar dropdowns render icon+label with accessible names intact",
    { tag: ["@explorer-menu-context-icons", "@explorer", "@smoke"] },
    async ({ page }, info) => {
      const menuBar = page.locator(`[data-testid="${TEST_IDS.menuBar}"]`);
      await expect(menuBar).toBeVisible({ timeout: 15_000 });

      await openContentMenu(page);
      const contentSummary = await summarizeMenuItems(
        page,
        page.locator('[data-testid="explorer-menu-content-dropdown"]'),
        "menu-bar-icon",
      );
      expectIconLabelChrome(info, contentSummary, "Content menu", false);
      await page.locator(`[data-testid="${TEST_IDS.menuContent}"]`).click();

      await openViewMenu(page);
      const viewSummary = await summarizeMenuItems(
        page,
        page.locator(`[data-testid="${TEST_IDS.viewDropdown}"]`),
        "menu-bar-icon",
      );
      expectIconLabelChrome(info, viewSummary, "View menu", false);
      await page.locator(`[data-testid="${TEST_IDS.menuView}"]`).click();

      await page.locator(`[data-testid="${TEST_IDS.menuHelp}"]`).click();
      await page
        .locator(`[data-testid="${TEST_IDS.helpDropdown}"]`)
        .waitFor({ state: "visible", timeout: 10_000 });
      const helpSummary = await summarizeMenuItems(
        page,
        page.locator(`[data-testid="${TEST_IDS.helpDropdown}"]`),
        "menu-bar-icon",
      );
      expectIconLabelChrome(info, helpSummary, "Help menu", false);
    },
  );

  test(
    "row context menu renders icon+label with accessible names intact",
    { tag: ["@explorer-menu-context-icons", "@explorer", "@smoke"] },
    async ({ page }, info) => {
      await page.goto(`${explorerSpaUrl(BASE_URL)}&path=/Sites`, {
        waitUntil: "networkidle",
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
        timeout: 15_000,
      });

      const rows = page.locator(
        '[data-testid="content-explorer-shell"] [role="row"]',
      );
      if ((await rows.count()) > 0) {
        await rows.first().click();
        await rows.first().click({ button: "right" });
      }
      const menu = page.locator(`[data-testid="${TEST_IDS.contextMenu}"]`);
      await expect(menu, "right-click must mount the row context menu").toBeVisible({
        timeout: 15_000,
      });
      // Empty catalog stays text-only (text-first clarity for empty states):
      // either the empty marker (no icons) or ≥1 icon-bearing menuitem.
      if ((await menu.locator('[role="menuitem"]').count()) === 0) {
        await expect(
          page.locator(`[data-testid="${TEST_IDS.contextMenuEmpty}"]`),
        ).toBeVisible();
        expect(
          await page.locator(`[data-testid="${TEST_IDS.contextMenuEmpty}"] [data-testid="context-menu-icon"]`).count(),
          "empty context-menu state stays text-only",
        ).toBe(0);
        return;
      }
      const summary = await summarizeMenuItems(page, menu, "context-menu-icon");
      expectIconLabelChrome(info, summary, "context menu");
    },
  );

  test(
    "axe-core a11y gate — shell with menu-bar and context-menu icons",
    { tag: ["@explorer-menu-context-icons", "@explorer", "@a11y"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });
      await openViewMenu(page);

      // Live Proof (#6603): pass-run capture of the icon-bearing View menu;
      // committed copy under docs/ai-generated/proofs/.
      await page.screenshot({
        path: "test-results/live-proof-6603-menu-context-icons.png",
      });

      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.shell}"]`,
      });
    },
  );
});
