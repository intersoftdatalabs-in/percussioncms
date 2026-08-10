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
 * Playwright surface: #2732 / parent #2400 — Workflow transitions in Explorer menus.
 *
 * <p>Verifies the modern React Content Explorer toolbar can surface workflow
 * transitions for a selected content item (itemmanagement getTransitions) and
 * that a transition control is invokable. When the H2 QA fixture has no
 * transition-eligible content, soft-skips with a documented reason (not suite red).</p>
 *
 * <p>Tags: {@code @explorer-workflow} {@code @workflow} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-workflow-transitions.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

async function listWaitReady(page) {
  await page.locator('[data-testid="detail-list"]').waitFor({ timeout: 15_000 });
}

/**
 * Soft-skip reason when the stack has no transition-capable content selection.
 * Issue #2732 acceptance allows documented soft-skip when fixture is thin.
 */
const FIXTURE_SKIP =
  "H2 QA fixture has no selectable content item with workflow transitions " +
  "(issue #2732 soft-skip; chrome/mapping covered by Vitest)";

test.describe("modern React Content Explorer - workflow transitions (#2732)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts server action toolbar (workflow surface host)",
    { tag: ["@explorer-workflow", "@workflow", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(
        page.locator('[data-testid="explorer-server-actions"]'),
      ).toBeVisible();
      // T082b / WebUI AGENTS.md — a11y gate on product Explorer shell surface.
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );

  test(
    "selecting a content item shows Workflow transition controls when available",
    { tag: ["@explorer-workflow", "@workflow"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      const tree = page.locator('[data-testid="explorer-tree"]');
      await expect(tree).toBeVisible({ timeout: 15_000 });
      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
        )
        .first();
      if ((await sitesNode.count()) > 0) {
        await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
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
        test.skip(true, FIXTURE_SKIP);
        return;
      }

      // Probe a few rows for a Workflow group (folder rows won't get transitions).
      const probeLimit = Math.min(
        Math.max(enabledCount, anyCount),
        8,
      );
      let foundWorkflow = false;
      for (let i = 0; i < probeLimit; i++) {
        const target =
          enabledCount > 0
            ? enabledRows.nth(i)
            : anyRows.nth(i);
        if ((await target.count()) === 0) break;
        await target.click({ force: true, timeout: 10_000 }).catch(() => {});
        await page.waitForTimeout(400);
        const group = page.locator(
          '[data-testid="action-toolbar-group-workflow"]',
        );
        if ((await group.count()) > 0 && (await group.isVisible())) {
          foundWorkflow = true;
          // Prefer first transition child button.
          const transitionBtn = page
            .locator('[data-testid^="action-toolbar-item-workflow-transition:"]')
            .first();
          if ((await transitionBtn.count()) === 0) {
            test.skip(true, FIXTURE_SKIP);
            return;
          }
          await expect(transitionBtn).toBeVisible();
          // Happy path: click once; do not assert state change hard (may need checkout).
          // If the CMS rejects, still prove the control was invokable (no throw on click).
          await transitionBtn.click({ force: true, timeout: 10_000 });
          // Toolbar host remains mounted after invoke.
          await expect(
            page.locator('[data-testid="action-toolbar"]'),
          ).toBeVisible();
          break;
        }
      }

      if (!foundWorkflow) {
        test.skip(true, FIXTURE_SKIP);
      }
    },
  );
});
