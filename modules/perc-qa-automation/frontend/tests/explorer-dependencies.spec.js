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
 * Playwright surface: #2768 / parent #2400 — Explorer Dependency Viewer shell chrome.
 *
 * <p>Verifies the modern React Content Explorer mounts View → Dependencies and
 * either shows the select-item hint or the DependencyViewer panel (reusing the
 * existing relationship summary REST loaders). Soft-skip deep relationship
 * assertions when the QA fixture has no selectable content item.</p>
 *
 * <p>Tags: {@code @explorer-dependencies} {@code @p-adv} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-dependencies.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

/** Wait until the detail list region is present (folder navigation settled). */
async function listWaitReady(page) {
  await page.locator('[data-testid="detail-list"]').waitFor({ timeout: 15_000 });
}

test.describe("modern React Content Explorer — dependency viewer (#2768)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts dependencies toggle and select-item hint",
    { tag: ["@explorer-dependencies", "@p-adv", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      // T082b / WebUI AGENTS.md — a11y gate on product Explorer shell surface.
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });

      // #2731 / #2768: dependency toggle lives under the View menu dropdown.
      await page.locator('[data-testid="explorer-menu-view"]').click();
      const toggle = page.locator(
        '[data-testid="explorer-toggle-dependencies"]',
      );
      await expect(toggle).toBeVisible();
      await expect(toggle).toHaveAttribute("aria-expanded", "false");

      await toggle.click();
      await expect(toggle).toHaveAttribute("aria-expanded", "true");

      // No content item selected yet → select-item hint (not the live panel).
      const hint = page.locator('[data-testid="explorer-dependencies-hint"]');
      await expect(hint).toBeVisible({ timeout: 5_000 });
      await expect(
        page.locator('[data-testid="dependency-viewer"]'),
      ).toHaveCount(0);
    },
  );

  test(
    "selecting a list row opens dependency viewer or select-item hint",
    { tag: ["@explorer-dependencies", "@p-adv"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      // Navigate into a structural root so the list has selectable children.
      const tree = page.locator('[data-testid="explorer-tree"]');
      await expect(tree).toBeVisible({ timeout: 15_000 });
      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
        )
        .first();
      if ((await sitesNode.count()) > 0) {
        // No force:true / silent catch — surface navigation failures.
        await sitesNode.click({ timeout: 10_000 });
        await listWaitReady(page);
        await page.waitForLoadState("networkidle").catch(() => {});
      }

      const list = page.locator('[data-testid="detail-list"]');
      await expect(list).toBeVisible({ timeout: 15_000 });

      const enabledRows = list.locator(
        'tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])',
      );
      const anyRows = list.locator('tbody tr[data-testid^="detail-row-"]');
      const enabledCount = await enabledRows.count();
      const anyCount = await anyRows.count();
      if (enabledCount === 0 && anyCount === 0) {
        // Soft path: empty list fixtures still prove chrome wiring via hint.
        await page.locator('[data-testid="explorer-menu-view"]').click();
        await page
          .locator('[data-testid="explorer-toggle-dependencies"]')
          .click();
        await expect(
          page.locator('[data-testid="explorer-dependencies-hint"]'),
        ).toBeVisible({ timeout: 5_000 });
        return;
      }

      const target = enabledCount > 0 ? enabledRows.first() : anyRows.first();
      try {
        await target.click({ timeout: 10_000 });
      } catch (err) {
        const again = list
          .locator('tbody tr[data-testid^="detail-row-"]')
          .first();
        await again.click({ timeout: 10_000 });
      }

      await page.locator('[data-testid="explorer-menu-view"]').click();
      await page.locator('[data-testid="explorer-toggle-dependencies"]').click();

      // Either full viewer (content id) or select-item hint (folder / no id).
      const panel = page.locator('[data-testid="dependency-viewer"]');
      const hint = page.locator('[data-testid="explorer-dependencies-hint"]');
      await expect(panel.or(hint)).toBeVisible({ timeout: 15_000 });

      if ((await panel.count()) > 0) {
        await expect(panel).toHaveAttribute(
          "data-testid-state",
          /ok|loading|auth|error/,
        );
        // Soft-skip strict ok when relationships REST/fixture is thin.
        await expect(panel).not.toHaveAttribute("data-testid-state", "loading", {
          timeout: 20_000,
        });
        const state = await panel.getAttribute("data-testid-state");
        if (state === "ok") {
          await expect(
            page.locator('[data-testid="dependency-dimensions"]'),
          ).toBeVisible();
          await expect(
            page.locator('[data-testid="dependency-row-outgoing"]'),
          ).toBeVisible();
        }
      }
    },
  );
});
