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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Playwright surface: Explorer Revisions / Audit Trail panel.
 *
 * <p>Invokes the Revisions server action (not a Data Flow contenteditorurls
 * page). Asserts the panel or a select-item / needs-item message, and that
 * {@code contenteditorurls.html} / {@code sys_cxSupport} are not requested.</p>
 *
 * <p>Tags: {@code @explorer-revisions} {@code @explorer-action-dispatch} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-revisions.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

async function listWaitReady(page) {
  await page.locator('[data-testid="detail-list"]').waitFor({ timeout: 15_000 });
}

test.describe("modern React Content Explorer — revisions / audit", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "Revisions action opens the panel or a select-item message without Data Flow HTML",
    { tag: ["@explorer-revisions", "@explorer-action-dispatch", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("sys_cxSupport/") ||
          u.includes("contenteditorurls.html") ||
          u.includes("flushcache.html") ||
          u.includes("navreset.html")
        ) {
          blocked.push(u);
        }
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
        timeout: 20_000,
      });

      const tree = page.locator('[data-testid="explorer-tree"]');
      if ((await tree.count()) > 0) {
        const sitesNode = page
          .locator(
            '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
          )
          .first();
        if ((await sitesNode.count()) > 0) {
          await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
          await listWaitReady(page).catch(() => {});
        }
      }

      const list = page.locator('[data-testid="detail-list"]');
      if ((await list.count()) > 0) {
        const row = list.locator('tbody tr[data-testid^="detail-row-"]').first();
        if ((await row.count()) > 0) {
          await row.click({ force: true, timeout: 10_000 }).catch(() => {});
        }
      }

      const workflowMenu = page.locator(
        '[data-testid="action-toolbar-item-Workflow"]',
      );
      await expect(workflowMenu).toBeVisible({ timeout: 20_000 });
      await workflowMenu.click();
      const revisions = page.locator(
        '[data-testid="action-toolbar-item-Workflow_Revisions"]',
      );
      await expect(revisions).toBeVisible({ timeout: 10_000 });
      await revisions.click();
      await expect(
        page.locator(
          '[data-testid="explorer-revisions-panel"], [data-testid="revisions-panel"], [data-testid="explorer-revisions-hint"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      expect(blocked, `Data Flow HTML must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );
});
