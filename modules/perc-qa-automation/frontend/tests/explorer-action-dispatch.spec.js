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
 * Explorer action dispatcher — no Data Flow HTML navigation.
 *
 * <p>Tags: {@code @explorer-action-dispatch} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-action-dispatch.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

test.describe("modern React Content Explorer — action dispatch", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "server actions region mounts and Edit does not open CM1 editor",
    { tag: ["@explorer-action-dispatch", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("sys_cxSupport/") ||
          u.includes("/cm/sys_cxSupport") ||
          u.includes("checkoutedit.xml") ||
          u.includes("contenteditorurls.html") ||
          u.includes("flushcache.html") ||
          u.includes("navreset.html") ||
          u.includes("demandpublishing")
        ) {
          blocked.push(u);
        }
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="explorer-server-actions"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible();

      const edit = page.locator('[data-testid="action-toolbar-item-Edit"]');
      if (await edit.isVisible()) {
        await edit.click();
        await expect(page).not.toHaveURL(/view=editor/);
      }

      expect(blocked, `Data Flow HTML must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );

  test(
    "Publish Now HTTP 200 FORBIDDEN shows an error and does not treat it as published",
    { tag: ["@explorer-action-dispatch", "@explorer"] },
    async ({ page }) => {
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await page.route("**/services/sitemanage/publish/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            status: "FORBIDDEN",
            warningMessage: "Publication stopped because of licensing issues",
          }),
        });
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
        }
      }

      const list = page.locator('[data-testid="detail-list"]');
      if ((await list.count()) > 0) {
        const row = list.locator('tbody tr[data-testid^="detail-row-"]').first();
        if ((await row.count()) > 0) {
          await row.click({ force: true, timeout: 10_000 }).catch(() => {});
        }
      }

      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      if ((await publishNow.count()) === 0 || !(await publishNow.first().isVisible())) {
        test.skip(true, "Publish Now action not visible for the current selection");
        return;
      }

      await publishNow.first().click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/FORBIDDEN|licensing|Publication stopped/i);
    },
  );
});
