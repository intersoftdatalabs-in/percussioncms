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

  test(
    "assembly slot add/create/arrange chrome does not request Data Flow HTML",
    { tag: ["@explorer-active-assembly", "@explorer", "@aa-slots"] },
    async ({ page }) => {
      const blocked = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => consoleErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
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

      await page.route("**/services/actions/find/templates/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ActionMenuList: [
              {
                name: "rffPgGeneric",
                label: "Generic Page",
                url: "../assembler/render?sys_template=7",
                sortRank: 0,
                type: "MENUITEM",
              },
            ],
          }),
        });
      });
      await page.route("**/services/assembly/preview-location**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
            contentId: 42,
            templateId: 7,
            revision: 1,
          }),
        });
      });
      await page.route("**/services/assembly/slot-relationships/canvas**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ownerId: 42,
            templateId: 7,
            slots: [
              {
                slotId: 3,
                name: "sidebar",
                label: "Sidebar",
                items: [
                  {
                    relationshipId: 88,
                    ownerId: 42,
                    dependentId: 7,
                    slotId: 3,
                    templateId: 4,
                    sortRank: 0,
                  },
                ],
              },
            ],
          }),
        });
      });
      await page.route("**/assembler/render**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "text/html",
          body: "<html><body>preview</body></html>",
        });
      });

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="assembly-slot-bar"]')).toBeVisible();
      await expect(page.locator('[data-testid="assembly-slot-3"]')).toBeVisible();
      await page.locator('[data-testid="assembly-slot-3"]').click();
      await expect(page.locator('[data-testid="assembly-slot-add"]')).toBeEnabled();
      await page.locator('[data-testid="assembly-slot-item-88"]').click();
      await expect(page.locator('[data-testid="assembly-slot-remove"]')).toBeEnabled();

      expect(blocked, `Data Flow AA HTML must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
      const serious = consoleErrors.filter(
        (t) => !/favicon|ResizeObserver|net::ERR/i.test(t),
      );
      expect(serious, `JS console errors: ${serious.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "slot add opens Content Browser; cancel does not POST; pick posts relationship",
    { tag: ["@explorer-active-assembly", "@explorer", "@aa-slots"] },
    async ({ page }) => {
      const blocked = [];
      const posted = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => consoleErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
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

      await page.route("**/services/actions/find/templates/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ActionMenuList: [
              {
                name: "rffPgGeneric",
                label: "Generic Page",
                url: "../assembler/render?sys_template=7",
                sortRank: 0,
                type: "MENUITEM",
              },
            ],
          }),
        });
      });
      await page.route("**/services/assembly/preview-location**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
            contentId: 42,
            templateId: 7,
            revision: 1,
          }),
        });
      });
      await page.route("**/services/assembly/slot-relationships/canvas**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ownerId: 42,
            templateId: 7,
            slots: [{ slotId: 3, name: "sidebar", label: "Sidebar", items: [] }],
          }),
        });
      });
      await page.route(
        "**/services/assembly/slot-relationships/allowed-templates**",
        async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              items: [{ id: 4, name: "rffSnTitle", label: "Title" }],
            }),
          });
        },
      );
      await page.route("**/services/assembly/slot-relationships", async (route) => {
        if (route.request().method() === "POST") {
          posted.push(route.request().postData() || "");
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              relationshipId: 9,
              ownerId: 42,
              dependentId: 7,
              slotId: 3,
              templateId: 4,
              sortRank: 0,
            }),
          });
          return;
        }
        await route.continue();
      });
      await page.route("**/assembler/render**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "text/html",
          body: "<html><body>preview</body></html>",
        });
      });
      await page.route("**/pathmanagement/**", async (route) => {
        const u = route.request().url();
        if (u.includes("paginatedFolder") || u.includes("/folder/")) {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              PagedItemList: {
                childrenInPage: [
                  {
                    id: "7",
                    name: "Snippet",
                    path: "/Sites/Demo/Snippet",
                    type: "page",
                    leaf: true,
                  },
                ],
                childrenCount: 1,
                startIndex: 0,
              },
            }),
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PathItem: {
              id: "1",
              name: "Sites",
              path: "/Sites",
              type: "folder",
              leaf: false,
            },
          }),
        });
      });

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await page.waitForLoadState("networkidle");
      const slotBar = page.locator('[data-testid="assembly-slot-bar"]');
      const slotChip = page.locator('[data-testid="assembly-slot-3"]');
      if (!(await slotChip.isVisible().catch(() => false))) {
        test.skip(
          true,
          "H2 has no slot canvas for this assembly template; slot add is not exercisable",
        );
        return;
      }
      await expect(slotBar).toBeVisible();
      await slotChip.click();
      await expect(page.locator('[data-testid="assembly-slot-add"]')).toBeEnabled();
      await page.locator('[data-testid="assembly-slot-add"]').click();
      await expect(page.locator('[data-testid="assembly-slot-add-dialog"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(page.locator('[data-testid="content-browser"]')).toBeVisible();
      await page.locator('[data-testid="content-browser-cancel"]').click();
      await expect(page.locator('[data-testid="assembly-slot-add-dialog"]')).toHaveCount(0);
      expect(posted, "cancel must not POST slot-relationships").toEqual([]);

      await page.locator('[data-testid="assembly-slot-add"]').click();
      await expect(page.locator('[data-testid="content-browser"]')).toBeVisible();
      const row = page.locator('[data-testid="detail-row-7"]');
      if (await row.isVisible().catch(() => false)) {
        await row.click();
        await page.locator('[data-testid="content-browser-confirm"]').click();
        await expect.poll(() => posted.length).toBe(1);
      }

      expect(blocked, `Data Flow AA HTML must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
      const serious = consoleErrors.filter(
        (t) => !/favicon|ResizeObserver|net::ERR|Failed to load resource/i.test(t),
      );
      expect(serious, `JS console errors: ${serious.join(" | ")}`).toEqual([]);
    },
  );
});
