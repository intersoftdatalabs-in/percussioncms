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
 * Playwright surface: #6515 Explorer action-toolbar icon+label affordances.
 *
 * <p>Asserts the DCE-familiar icon+label chrome on the server-driven
 * {@code action-toolbar}: mapped buttons render exactly one decorative
 * {@code svg[aria-hidden=true]} next to a non-empty visible label, the
 * accessible name still matches the visible label (i18n/a11y unchanged),
 * unmapped actions stay label-only, and the shell passes
 * {@code expectNoSeriousA11yViolations} scoped to
 * {@code content-explorer-shell}.</p>
 *
 * <p>Tags: {@code @explorer-action-toolbar} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-action-toolbar-icons.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
} = require("./helpers/explorer-menu-bar");
const {
  expectNoSeriousA11yViolations,
} = require("./helpers/a11y");

test.describe("modern React Content Explorer — toolbar icon+label affordances (#6515)", () => {
  /** Uncaught page errors + unexpected console errors for C5. */
  let pageErrors = [];

  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
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
    "toolbar buttons render icon+label with accessible names intact",
    { tag: ["@explorer-action-toolbar", "@explorer", "@smoke"] },
    async ({ page }) => {
      const toolbar = page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`);
      await expect(toolbar).toBeVisible({ timeout: 15_000 });

      // Select a row so the server catalog returns item actions (icon
      // coverage is richest with a selection); fall back to folder-level
      // actions when no rows exist.
      const rows = page.locator(
        '[data-testid="content-explorer-shell"] [role="row"]',
      );
      if ((await rows.count()) > 0) {
        await rows.first().click();
      }

      const buttons = toolbar.locator(
        'button[data-testid^="action-toolbar-item-"]',
      );
      await expect
        .poll(async () => buttons.count(), { timeout: 15_000 })
        .toBeGreaterThan(0);

      const summary = await buttons.evaluateAll((els) =>
        els.map((el) => {
          const name =
            el.getAttribute("aria-label") ||
            (el.textContent || "").trim();
          const visible = (el.textContent || "").trim();
          const icons = el.querySelectorAll(
            '[data-testid="action-toolbar-icon"] > svg',
          );
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
      );
      test.info().annotations.push({
        type: "note",
        description: `toolbar buttons=${summary.length} withIcon=${summary.filter((s) => s.iconCount > 0).length}`,
      });
      expect(
        summary.length,
        "toolbar must render at least one action button",
      ).toBeGreaterThan(0);
      // At least one mapped action (Open/Preview/Checkout/...) must show
      // an icon — otherwise the icon pipeline is dead on the live catalog.
      expect(
        summary.filter((s) => s.iconCount > 0).length,
        "at least one toolbar button renders an icon",
      ).toBeGreaterThan(0);
      for (const row of summary) {
        // Labels stay: every button keeps a non-empty accessible name that
        // matches its visible text (icons are pure decoration).
        expect(row.name, `${row.testid} keeps its accessible name`).not.toBe("");
        expect(
          row.visible,
          `${row.testid} keeps its visible label`,
        ).toContain(row.name);
        // At most one icon per button; every icon is aria-hidden.
        expect(
          row.iconCount,
          `${row.testid} renders at most one icon`,
        ).toBeLessThanOrEqual(1);
        expect(row.iconHidden, `${row.testid} icon is aria-hidden`).toBe(true);
      }

      // Live Proof (#6515): pass-run capture of the icon+label toolbar;
      // committed copy under docs/ai-generated/proofs/.
      await page.screenshot({
        path: "test-results/live-proof-6515-toolbar-icons.png",
      });
    },
  );

  test(
    "axe-core a11y gate — shell with icon toolbar",
    { tag: ["@explorer-action-toolbar", "@explorer", "@a11y"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible({ timeout: 15_000 });
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.shell}"]`,
      });
    },
  );
});
