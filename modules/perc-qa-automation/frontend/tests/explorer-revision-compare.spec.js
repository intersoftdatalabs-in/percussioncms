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
 * Playwright surface: Explorer compare two revisions (#4560).
 *
 * <p>Tags: {@code @explorer-revision-compare} {@code @explorer-revisions}
 * {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-revision-compare.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

async function stubExplorerItem(page) {
  await page.route("**/actions/find**", async (route) => {
    const reqUrl = route.request().url();
    if (/\/actions\/find\/(types|templates)/i.test(reqUrl)) {
      return route.continue();
    }
    const response = await route.fetch();
    const contentType = response.headers()["content-type"] || "";
    if (!contentType.includes("application/json")) {
      return route.fulfill({ response });
    }
    let body;
    try {
      body = await response.json();
    } catch {
      return route.fulfill({ response });
    }
    const revisions = {
      name: "workflow_revisions",
      label: "Revisions",
      sortRank: 1,
      menuType: "MENUITEM",
    };
    if (Array.isArray(body?.ActionMenu)) {
      body = { ...body, ActionMenu: [...body.ActionMenu, revisions] };
    } else if (Array.isArray(body)) {
      body = [...body, revisions];
    }
    return route.fulfill({
      status: response.status(),
      headers: {
        ...response.headers(),
        "content-type": "application/json",
      },
      body: JSON.stringify(body),
    });
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
}

async function stubRevisions(page) {
  await page.route("**/itemmanagement/item/revisions/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        RevisionsSummary: {
          restorable: true,
          revisions: [
            {
              revId: 1,
              lastModifiedDate: "2026-01-01",
              lastModifier: "Admin",
              status: "Draft",
            },
            {
              revId: 2,
              lastModifiedDate: "2026-01-02",
              lastModifier: "Editor",
              status: "Live",
            },
          ],
          comments: [],
        },
      }),
    });
  });
}

async function openRevisionsPanel(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const itemRow = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(itemRow).toBeVisible({ timeout: 20_000 });
  await itemRow.click();
  const revisions = page.locator(
    [
      '[data-testid="action-toolbar-item-workflow_revisions"]',
      '[data-testid="action-toolbar-item-Workflow_Revisions"]',
      '[data-testid="action-toolbar-item-Revisions"]',
    ].join(", "),
  );
  if ((await revisions.count()) === 0) {
    const workflow = page.locator('[data-testid="action-toolbar-item-Workflow"]');
    if ((await workflow.count()) > 0) {
      await workflow.first().click();
    }
  }
  await expect(revisions.first()).toBeVisible({ timeout: 15_000 });
  await revisions.first().click();
  await expect(page.locator('[data-testid="revisions-panel"]')).toBeVisible({
    timeout: 10_000,
  });
}

test.describe("modern React Content Explorer — compare two revisions", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "operator compares two revisions without Data Flow sys_compare",
    { tag: ["@explorer-revision-compare", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      const blocked = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("request", (req) => {
        const u = req.url().toLowerCase();
        if (
          u.includes("sys_compare") ||
          u.includes("compare.html") ||
          u.includes("sys_cxsupport/")
        ) {
          blocked.push(req.url());
        }
      });
      await stubExplorerItem(page);
      await stubRevisions(page);
      await page.route("**/itemmanagement/item/compare/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemRevisionCompare: {
              itemId: "1-101-42",
              rev1: 1,
              rev2: 2,
              fields: [
                {
                  name: "displaytitle",
                  leftValue: "old",
                  rightValue: "new",
                  changed: true,
                },
              ],
            },
          }),
        });
      });

      await openRevisionsPanel(page);
      await expect(page.locator('[data-testid="revisions-compare-run"]')).toBeVisible();
      await page.locator('[data-testid="revisions-compare-run"]').click();
      await expect(
        page.locator('[data-testid="revisions-compare-row-displaytitle"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="revisions-compare-row-displaytitle"]'),
      ).toHaveAttribute("data-testid-changed", "true");

      expect(blocked, `Data Flow compare must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );

  test(
    "missing revision 404 and forbidden 403 are errors not empty compare",
    { tag: ["@explorer-revision-compare", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubExplorerItem(page);
      await stubRevisions(page);
      await page.route("**/itemmanagement/item/compare/**", async (route) => {
        await route.fulfill({
          status: 404,
          contentType: "application/json",
          body: JSON.stringify({ message: "Revision not found." }),
        });
      });

      await openRevisionsPanel(page);
      await page.locator('[data-testid="revisions-compare-run"]').click();
      await expect(page.locator('[data-testid="revisions-compare-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="revisions-compare-error"]')).toContainText(
        /404/,
      );
      await expect(page.locator('[data-testid="revisions-compare-table"]')).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "forbidden compare is HTTP 403 error chrome",
    { tag: ["@explorer-revision-compare", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubExplorerItem(page);
      await stubRevisions(page);
      await page.route("**/itemmanagement/item/compare/**", async (route) => {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "Not authorized to compare this item." }),
        });
      });

      await openRevisionsPanel(page);
      await page.locator('[data-testid="revisions-compare-run"]').click();
      await expect(page.locator('[data-testid="revisions-compare-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="revisions-compare-error"]')).toContainText(
        /403/,
      );
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
