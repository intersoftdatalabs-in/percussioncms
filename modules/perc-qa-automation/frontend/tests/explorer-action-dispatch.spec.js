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
          u.includes("demandpublishing") ||
          u.includes("sys_cxItemAssembly") ||
          u.includes("itemassembly.html")
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
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      // H2 QA sites often have no page rows. Inject one leaf so Publish Now
      // can run and the 200 FORBIDDEN intercept can fire (#3451).
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
          }),
        });
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

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="42"]',
        ),
      ).toBeVisible({ timeout: 10_000 });
      await expect(itemRow).toHaveAttribute("aria-selected", "true");

      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/FORBIDDEN|licensing|Publication stopped/i);
      await expect(
        page.getByRole("alert").filter({ hasText: /Select a content item first/i }),
      ).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Take Down HTTP 200 FORBIDDEN shows an error and does not treat it as unpublished",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-takedown"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/findLinkedItems/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ ArrayList: [] }),
        });
      });
      await page.route("**/services/sitemanage/publish/takedown/**", async (route) => {
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

      await expect(
        page.locator('[data-testid="action-toolbar-item-Take_Down"]'),
      ).toHaveCount(0);

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="42"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      const takeDown = page.locator(
        '[data-testid="action-toolbar-item-Take_Down"], [data-testid="action-toolbar-item-take_down"]',
      );
      await expect(takeDown.first()).toBeVisible({ timeout: 15_000 });
      await takeDown.first().click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/FORBIDDEN|licensing|Publication stopped/i);
      await expect(
        page.getByRole("alert").filter({ hasText: /Select a content item first/i }),
      ).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Stage HTTP 200 FORBIDDEN shows an error and does not treat it as staged",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-staging"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route("**/services/sitemanage/publish/page/staging/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            status: "FORBIDDEN",
            warningMessage: "Publication stopped because of licensing issues",
          }),
        });
      });
      await page.route(
        "**/services/sitemanage/publish/resource/staging/**",
        async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              status: "FORBIDDEN",
              warningMessage: "Publication stopped because of licensing issues",
            }),
          });
        },
      );

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
        timeout: 20_000,
      });

      await expect(page.locator('[data-testid="action-toolbar-item-Stage"]')).toHaveCount(
        0,
      );

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="42"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      const stage = page.locator('[data-testid="action-toolbar-item-Stage"]');
      await expect(stage).toBeVisible({ timeout: 15_000 });
      await stage.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/FORBIDDEN|licensing|Publication stopped/i);
      await expect(
        page.getByRole("alert").filter({ hasText: /Select a content item first/i }),
      ).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Remove from Staging HTTP 200 NOSTAGING_SERVERS shows an error and does not treat it as unstaged",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-staging"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route(
        "**/services/sitemanage/publish/takedown/page/staging/**",
        async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              SitePublishResponse: {
                status: "NOSTAGING_SERVERS",
                warningMessage: "No staging servers are configured.",
              },
            }),
          });
        },
      );
      await page.route(
        "**/services/sitemanage/publish/takedown/resource/staging/**",
        async (route) => {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              SitePublishResponse: {
                status: "NOSTAGING_SERVERS",
                warningMessage: "No staging servers are configured.",
              },
            }),
          });
        },
      );

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
        timeout: 20_000,
      });

      await expect(
        page.locator('[data-testid="action-toolbar-item-Remove_from_Staging"]'),
      ).toHaveCount(0);

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="42"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      const unstage = page.locator(
        '[data-testid="action-toolbar-item-Remove_from_Staging"]',
      );
      await expect(unstage).toBeVisible({ timeout: 15_000 });
      await unstage.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/No staging servers|NOSTAGING_SERVERS/i);
      await expect(
        page.getByRole("alert").filter({ hasText: /Select a content item first/i }),
      ).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Schedule HTTP 200 FORBIDDEN shows an error and does not treat it as saved",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-schedule"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemDates: { itemId: "42", startDate: "", endDate: "", comments: "" },
          }),
        });
      });
      await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
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

      await expect(page.locator('[data-testid="action-toolbar-item-Schedule"]')).toHaveCount(
        0,
      );

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="42"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      const schedule = page.locator('[data-testid="action-toolbar-item-Schedule"]');
      await expect(schedule).toBeVisible({ timeout: 15_000 });
      await schedule.click();
      await expect(page.locator('[data-testid="explorer-schedule-dialog"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="explorer-schedule-clear"]').click();
      await page.locator('[data-testid="explorer-schedule-save"]').click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/FORBIDDEN|licensing|Publication stopped/i);
      await expect(
        page.getByRole("alert").filter({ hasText: /Select a content item first/i }),
      ).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Schedule multi-select writes one dialog to each page and shows partial failure",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-schedule"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      const posted = [];
      let confirms = [];
      page.on("dialog", (dialog) => {
        confirms.push(dialog.message());
        void dialog.accept();
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "1",
                  name: "Sites",
                  path: "/Sites",
                  type: "folder",
                  category: "folder",
                  leaf: false,
                },
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
                {
                  id: "43",
                  name: "About",
                  path: "/Sites/Demo/About",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
              ],
              childrenCount: 3,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemDates: { itemId: "42", startDate: "", endDate: "", comments: "" },
          }),
        });
      });
      await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
        const body = route.request().postDataJSON();
        const id = body?.ItemDates?.itemId;
        posted.push(id);
        if (id === "43") {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              status: "FORBIDDEN",
              warningMessage: "Publication stopped because of licensing issues",
            }),
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ ItemDates: body?.ItemDates ?? {} }),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
        timeout: 20_000,
      });
      const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
      await expect(home).toBeVisible({ timeout: 20_000 });
      await home.click();
      await page.locator('[data-testid="detail-select-1"]').check();
      await page.locator('[data-testid="detail-select-42"]').check();
      await page.locator('[data-testid="detail-select-43"]').check();
      const schedule = page.locator('[data-testid="action-toolbar-item-Schedule"]');
      await expect(schedule).toBeVisible({ timeout: 15_000 });
      await schedule.click();
      await expect(page.locator('[data-testid="explorer-schedule-multi"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="explorer-schedule-clear"]').click();
      await page.locator('[data-testid="explorer-schedule-cancel"]').click();
      expect(posted).toEqual([]);
      await schedule.click();
      await expect(page.locator('[data-testid="explorer-schedule-dialog"]')).toBeVisible();
      await page.locator('[data-testid="explorer-schedule-save"]').click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/not saved for every|About|FORBIDDEN|licensing/i, {
        timeout: 10_000,
      });
      expect(posted.sort()).toEqual(["42", "43"]);
      expect(confirms.some((text) => /every selected page and asset/i.test(text))).toBe(
        true,
      );
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "Publishing History shows rows or empty; HTTP 404 and 403 are errors",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-publishing-history"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/pubhistory/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemPublishingHistory: [
              {
                server: "prod",
                location: "/index.html",
                revisionId: 3,
                publishedDate: Date.now(),
                operation: "publish",
                status: "SUCCESS",
                contentId: 42,
              },
            ],
          }),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
        timeout: 20_000,
      });

      await expect(
        page.locator('[data-testid="action-toolbar-item-Publishing_History"]'),
      ).toHaveCount(0);

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="42"]',
        ),
      ).toBeVisible({ timeout: 10_000 });

      const history = page.locator(
        '[data-testid="action-toolbar-item-Publishing_History"]',
      );
      await expect(history).toBeVisible({ timeout: 15_000 });
      await history.click();
      await expect(
        page.locator('[data-testid="explorer-publishing-history-dialog"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(page.locator('[data-testid="item-history-table"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-history-row"]')).toContainText(
        "prod",
      );

      await page.unroute("**/itemmanagement/item/pubhistory/**");
      await page.route("**/itemmanagement/item/pubhistory/**", async (route) => {
        await route.fulfill({
          status: 404,
          contentType: "application/json",
          body: JSON.stringify({ message: "unknown item" }),
        });
      });
      await page.locator('[data-testid="item-history-id"]').fill("99");
      await page.locator('[data-testid="item-history-lookup"]').click();
      await expect(page.locator('[data-testid="item-history-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-history-error"]')).toContainText(
        /unknown item|HTTP 404/i,
      );
      await expect(page.locator('[data-testid="item-history-empty"]')).toHaveCount(0);

      await page.unroute("**/itemmanagement/item/pubhistory/**");
      await page.route("**/itemmanagement/item/pubhistory/**", async (route) => {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "forbidden" }),
        });
      });
      await page.locator('[data-testid="item-history-id"]').fill("7");
      await page.locator('[data-testid="item-history-lookup"]').click();
      await expect(page.locator('[data-testid="item-history-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-history-error"]')).toContainText(
        /forbidden|HTTP 403/i,
      );
      await expect(page.locator('[data-testid="item-history-empty"]')).toHaveCount(0);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-publishing-history-dialog"]',
      });
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Clear scheduled dates confirms empty dates, cancel does not write, folder hides the action",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-clear-schedule"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      const posted = [];
      let cleared = false;
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        const start = cleared ? "" : "09/18/2026 09:00 am";
        const end = cleared ? "" : "09/19/2026 10:00 am";
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "1",
                  name: "Sites",
                  path: "/Sites",
                  type: "folder",
                  category: "folder",
                  leaf: false,
                },
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                  displayProperties: { startDate: start, endDate: end },
                },
              ],
              childrenCount: 2,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemDates: {
              itemId: "42",
              startDate: "09/18/2026 09:00 am",
              endDate: "09/19/2026 10:00 am",
              comments: "",
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
        const body = route.request().postDataJSON();
        posted.push(body);
        cleared = true;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ ItemDates: body?.ItemDates ?? {} }),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const folder = page.locator('[data-testid="detail-row-1"][data-row-kind="folder"]');
      await expect(folder).toBeVisible({ timeout: 20_000 });
      await folder.click();
      await expect(
        page.locator('[data-testid="action-toolbar-item-Clear_Scheduled_Dates"]'),
      ).toHaveCount(0);

      const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
      await expect(home).toBeVisible();
      await expect(page.locator('[data-testid="detail-schedule-dates-42"]')).toContainText(
        "09/18/2026 09:00 am",
      );
      await home.click();
      const clear = page.locator(
        '[data-testid="action-toolbar-item-Clear_Scheduled_Dates"]',
      );
      await expect(clear).toBeVisible({ timeout: 15_000 });
      await clear.click();
      await expect(page.locator('[data-testid="explorer-clear-schedule-dialog"]')).toBeVisible();
      await page.locator('[data-testid="explorer-clear-schedule-cancel"]').click();
      expect(posted).toEqual([]);
      await expect(page.locator('[data-testid="detail-schedule-dates-42"]')).toBeVisible();

      await clear.click();
      await expect(page.locator('[data-testid="explorer-clear-schedule-current"]')).toContainText(
        "09/18/2026 09:00 am",
      );
      await page.locator('[data-testid="explorer-clear-schedule-confirm"]').click();
      await expect(page.locator('[data-testid="explorer-clear-schedule-dialog"]')).toHaveCount(0, {
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="detail-schedule-dates-42"]')).toHaveCount(0, {
        timeout: 10_000,
      });
      expect(posted).toHaveLength(1);
      expect(posted[0]?.ItemDates?.startDate ?? "").toBe("");
      expect(posted[0]?.ItemDates?.endDate ?? "").toBe("");
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "Clear scheduled dates on a multi-selection uses one confirm, skips folders, and keeps HTTP 409",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-clear-schedule"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      const posted = [];
      const confirms = [];
      page.on("dialog", (dialog) => {
        confirms.push(dialog.message());
        if (confirms.length === 1) {
          void dialog.dismiss();
          return;
        }
        void dialog.accept();
      });
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "1",
                  name: "Sites",
                  path: "/Sites",
                  type: "folder",
                  category: "folder",
                  leaf: false,
                },
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
                {
                  id: "43",
                  name: "About",
                  path: "/Sites/Demo/About",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                },
              ],
              childrenCount: 3,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
        const body = route.request().postDataJSON();
        const id = body?.ItemDates?.itemId;
        posted.push(body);
        if (id === "43") {
          await route.fulfill({
            status: 409,
            contentType: "application/json",
            body: JSON.stringify({ message: "checked out" }),
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ ItemDates: body?.ItemDates ?? {} }),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
      await expect(home).toBeVisible({ timeout: 20_000 });
      await home.click();
      await page.locator('[data-testid="detail-select-1"]').check();
      await page.locator('[data-testid="detail-select-42"]').check();
      await page.locator('[data-testid="detail-select-43"]').check();
      const clear = page.locator(
        '[data-testid="action-toolbar-item-Clear_Scheduled_Dates"]',
      );
      await expect(clear).toBeVisible({ timeout: 15_000 });
      await clear.click();
      await expect(page.locator('[data-testid="explorer-clear-schedule-dialog"]')).toHaveCount(0);
      expect(posted).toEqual([]);
      expect(confirms[0] ?? "").toMatch(/2 selected|not cleared/i);

      await clear.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/Sites|not cleared|About|409|checked out/i, { timeout: 10_000 });
      expect(posted.map((body) => body?.ItemDates?.itemId).sort()).toEqual(["42", "43"]);
      expect(posted.every((body) => (body?.ItemDates?.startDate ?? "") === "")).toBe(true);
      expect(posted.every((body) => (body?.ItemDates?.endDate ?? "") === "")).toBe(true);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "Clear scheduled dates keeps HTTP 400, 403, and 409 on the dialog",
    { tag: ["@explorer-action-dispatch", "@explorer", "@explorer-clear-schedule"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      let status = 400;
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PagedItemList: {
              childrenInPage: [
                {
                  id: "42",
                  name: "Home",
                  path: "/Sites/Demo/Home",
                  type: "percPage",
                  category: "page",
                  accessLevel: "WRITE",
                  leaf: true,
                  displayProperties: {
                    startDate: "09/18/2026 09:00 am",
                    endDate: "09/19/2026 10:00 am",
                  },
                },
              ],
              childrenCount: 1,
              startIndex: 0,
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemDates: {
              itemId: "42",
              startDate: "09/18/2026 09:00 am",
              endDate: "09/19/2026 10:00 am",
            },
          }),
        });
      });
      await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({ message: `blocked ${status}` }),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const home = page.locator('[data-testid="detail-row-42"]');
      await expect(home).toBeVisible({ timeout: 20_000 });
      await home.click();
      const clear = page.locator(
        '[data-testid="action-toolbar-item-Clear_Scheduled_Dates"]',
      );
      for (const code of [400, 403, 409]) {
        status = code;
        await clear.click();
        await expect(
          page.locator('[data-testid="explorer-clear-schedule-dialog"]'),
        ).toBeVisible();
        await page.locator('[data-testid="explorer-clear-schedule-confirm"]').click();
        const alert = page.locator('[data-testid="explorer-clear-schedule-error"]');
        await expect(alert).toBeVisible({ timeout: 10_000 });
        await expect(alert).toContainText(new RegExp(`blocked ${code}|HTTP ${code}`, "i"));
        await expect(page.locator('[data-testid="detail-schedule-dates-42"]')).toBeVisible();
        await page.locator('[data-testid="explorer-clear-schedule-cancel"]').click();
        await expect(
          page.locator('[data-testid="explorer-clear-schedule-dialog"]'),
        ).toHaveCount(0);
      }
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
