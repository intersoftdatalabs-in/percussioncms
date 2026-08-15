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
 * Preview-first Active Assembly host — chrome-less assembled preview.
 *
 * <p>Tags: {@code @explorer-active-assembly} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-active-assembly.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

test.describe("modern React Active Assembly — preview host", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "assembly entry is chrome-less and does not request Data Flow AA HTML",
    { tag: ["@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("sys_cxItemAssembly") ||
          u.includes("itemassembly.html") ||
          u.includes("variantlistwithslots.html") ||
          u.includes("sys_cxSupport/")
        ) {
          blocked.push(u);
        }
      });

      await page.goto(assemblySpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="assembly-overlay"]')).toBeVisible();
      await expect(page.locator('[data-testid="perc-spa-app"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="assembly-error"]')).toBeVisible();

      expect(blocked, `Data Flow AA HTML must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="assembly-host"]',
      });
    },
  );

  test(
    "unmatched templateId shows an error instead of silently using another template",
    { tag: ["@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      await page.route("**/services/actions/find/templates/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ActionMenuList: [
              {
                name: "rffSnTitle",
                label: "Title snippet",
                url: "../assembler/render?sys_template=3",
                sortRank: 0,
                type: "MENUITEM",
              },
            ],
          }),
        });
      });

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=99"));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="assembly-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="assembly-error"]')).toContainText(
        /not in the available list|No page or snippet template/i,
      );
      await expect(page.locator('[data-testid="assembly-preview-frame"]')).toHaveCount(
        0,
      );
    },
  );

  test(
    "Explorer Active Assembly does not navigate leftover AA HTML",
    { tag: ["@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (u.includes("sys_cxItemAssembly") || u.includes("itemassembly.html")) {
          blocked.push(u);
        }
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 20_000,
      });

      const aa = page.locator('[data-testid="action-toolbar-item-Item_ActiveAssembly"]');
      if (await aa.isVisible()) {
        await aa.click();
      }

      expect(blocked, `Data Flow AA HTML must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
    },
  );
});
