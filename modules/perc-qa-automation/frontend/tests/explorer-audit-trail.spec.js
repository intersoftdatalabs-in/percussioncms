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
 * Playwright surface: Explorer audit trail of the selected item (#5245).
 *
 * <p>Tags: {@code @explorer-audit-trail} {@code @explorer-revisions}
 * {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-audit-trail.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const PAGE = {
  id: "42",
  name: "Home",
  path: "/Sites/Demo/Home",
  type: "percPage",
  category: "page",
  accessLevel: "WRITE",
  leaf: true,
};

const FOLDER = {
  id: "fold-1",
  name: "News",
  path: "/Sites/Demo/News/",
  type: "folder",
  category: "folder",
  accessLevel: "WRITE",
  leaf: false,
};

const RECORDED = {
  comment: "Looks good",
  commenter: "Admin",
  commentType: "Approve",
  commentDate: "2026-01-02",
};

/**
 * Page errors and console errors, plus same-origin HTTP failures.
 * Chromium logs "Failed to load resource" for any non-OK fetch; those lines
 * are counted via the response listener so an allowed revisions 403/404 is
 * not a second failure. Favicon and ResizeObserver stay out of both lists.
 *
 * @param {import("@playwright/test").Page} page
 * @param {(url: string, status: number) => boolean} [allowHttp]
 */
function watchBrowser(page, allowHttp) {
  const jsErrors = [];
  const httpFailures = [];
  page.on("pageerror", (err) => {
    jsErrors.push(String(err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (/Failed to load resource|favicon|ResizeObserver|Download the React DevTools/i.test(text)) {
      return;
    }
    jsErrors.push(text);
  });
  page.on("response", (res) => {
    const status = res.status();
    if (status < 400) {
      return;
    }
    const url = res.url();
    if (/favicon/i.test(url)) {
      return;
    }
    if (allowHttp && allowHttp(url, status)) {
      return;
    }
    httpFailures.push(`${status} ${res.request().method()} ${url}`);
  });
  return { jsErrors, httpFailures };
}

function expectCleanBrowser(watched) {
  expect(
    watched.httpFailures,
    `unexpected HTTP failure:\n${watched.httpFailures.join("\n")}`,
  ).toEqual([]);
  expect(watched.jsErrors, `uncaught browser error: ${watched.jsErrors.join(" | ")}`).toEqual(
    [],
  );
}

function watchBlocked(page) {
  const blocked = [];
  page.on("request", (req) => {
    const u = req.url().toLowerCase();
    if (
      u.includes("contenteditorurls") ||
      u.includes("sys_audittrail") ||
      u.includes("sys_cxsupport/") ||
      u.includes("sys_compare") ||
      u.includes("item/restorerevision") ||
      u.includes("item/compare/")
    ) {
      blocked.push(req.url());
    }
  });
  return blocked;
}

const EMPTY_MENUS = JSON.stringify({ ActionMenu: [] });

async function stubExplorer(page) {
  // Fake id 42 is not in H2. Live find/templates and getTransitions throw
  // (ActionMenuAdaptor ERROR, PSComponentSummary 500). Fulfill empty catalogs
  // so the audit proof does not depend on a real content row.
  await page.route("**/actions/find/templates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: EMPTY_MENUS,
    });
  });
  await page.route("**/actions/find/types**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: EMPTY_MENUS,
    });
  });
  await page.route("**/itemmanagement/workflow/getTransitions/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: { transitionTriggers: [] },
      }),
    });
  });
  await page.route("**/actions/find**", async (route) => {
    const reqUrl = route.request().url();
    if (/\/actions\/find\/(types|templates)/i.test(reqUrl)) {
      return route.fallback();
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
    const audit = {
      name: "Workflow_AuditTrail",
      label: "Audit Trail",
      sortRank: 1,
      menuType: "MENUITEM",
      url: "../sys_cxSupport/contenteditorurls.html?sys_userview=sys_audittrail&sys_command=preview",
    };
    if (Array.isArray(body?.ActionMenu)) {
      body = { ...body, ActionMenu: [...body.ActionMenu, audit] };
    } else if (Array.isArray(body)) {
      body = [...body, audit];
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
  await page.route("**/explorer/list-columns**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ExplorerListColumns: { folderPath: "/", columns: [] },
      }),
    });
  });
  await page.route("**/editor/items/**/checkout-owner**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: "",
          currentUser: "Admin",
        },
      }),
    });
  });
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PagedItemList: {
          childrenInPage: [PAGE, FOLDER],
          childrenCount: 2,
          startIndex: 0,
        },
      }),
    });
  });
}

async function stubHistory(page, status, body, gate) {
  await page.route("**/itemmanagement/item/revisions/**", async (route) => {
    if (gate) {
      await gate.promise;
    }
    await route.fulfill({
      status,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
}

function holdGate() {
  let open = () => {};
  const promise = new Promise((resolve) => {
    open = resolve;
  });
  return { promise, open };
}

async function openExplorer(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
}

async function clickAudit(page) {
  const audit = page.locator(
    [
      '[data-testid="action-toolbar-item-Workflow_AuditTrail"]',
      '[data-testid="action-toolbar-item-workflow_audittrail"]',
    ].join(", "),
  );
  if ((await audit.count()) === 0) {
    for (const parent of ["View", "Workflow"]) {
      const menu = page.locator(`[data-testid="action-toolbar-item-${parent}"]`);
      if ((await menu.count()) > 0) {
        await menu.first().click();
        if ((await audit.count()) > 0) {
          break;
        }
      }
    }
  }
  await expect(audit.first()).toBeVisible({ timeout: 15_000 });
  await audit.first().click();
}

test.describe("modern React Content Explorer — audit trail", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "audit columns appear only after history loads",
    { tag: ["@explorer-audit-trail", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const watched = watchBrowser(page);
      const blocked = watchBlocked(page);
      const gate = holdGate();
      await stubExplorer(page);
      await stubHistory(
        page,
        200,
        {
          RevisionsSummary: {
            restorable: true,
            currentRevision: 1,
            revisions: [
              {
                revId: 1,
                lastModifiedDate: "2026-01-01",
                lastModifier: "Admin",
                status: "Draft",
              },
            ],
            comments: [RECORDED],
          },
        },
        gate,
      );
      await openExplorer(page);
      await page.locator('[data-testid="detail-row-42"][data-row-kind="item"]').click();
      await clickAudit(page);
      const panel = page.locator('[data-testid="revisions-panel"]');
      await expect(panel).toBeVisible({ timeout: 10_000 });
      await expect(panel).toHaveAttribute("data-testid-state", "loading");
      await expect(page.locator('[data-testid="audit-row-0"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="revisions-audit-empty"]')).toHaveCount(0);
      gate.open();
      await expect(page.locator('[data-testid="audit-date-0"]')).toHaveText("2026-01-02");
      await expect(page.locator('[data-testid="audit-user-0"]')).toHaveText("Admin");
      await expect(page.locator('[data-testid="audit-type-0"]')).toHaveText("Approve");
      await expect(page.locator('[data-testid="audit-comment-0"]')).toHaveText(
        "Looks good",
      );
      await expect(page.locator('[data-testid="revisions-restore-1"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="revisions-compare-run"]')).toHaveCount(0);
      expect(blocked, `Data Flow or restore/compare must not run: ${blocked.join(" ")}`).toEqual(
        [],
      );
      expectCleanBrowser(watched);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );

  test(
    "empty history shows the empty message, not a fake row",
    { tag: ["@explorer-audit-trail", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const watched = watchBrowser(page);
      await stubExplorer(page);
      await stubHistory(page, 200, {
        RevisionsSummary: {
          restorable: false,
          revisions: [],
          comments: {
            comment: " ",
            commenter: "",
            commentType: "",
            commentDate: "",
          },
        },
      });
      await openExplorer(page);
      await page.locator('[data-testid="detail-row-42"][data-row-kind="item"]').click();
      await clickAudit(page);
      await expect(page.locator('[data-testid="revisions-audit-empty"]')).toContainText(
        /No workflow comments/,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="audit-row-0"]')).toHaveCount(0);
      expectCleanBrowser(watched);
    },
  );

  test(
    "a folder and an empty selection do not claim an audit trail",
    { tag: ["@explorer-audit-trail", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const watched = watchBrowser(page);
      const blocked = watchBlocked(page);
      await stubExplorer(page);
      await stubHistory(page, 200, {
        RevisionsSummary: { restorable: false, revisions: [], comments: [RECORDED] },
      });
      await openExplorer(page);
      await clickAudit(page);
      await expect(page.locator('[data-testid="explorer-revisions-hint"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="revisions-audit-table"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-server-actions-error"]')).toContainText(
        /Select a content item first/,
      );

      await page.locator('[data-testid="detail-row-fold-1"][data-row-kind="folder"]').click();
      await clickAudit(page);
      await expect(page.locator('[data-testid="explorer-revisions-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="audit-row-0"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="revisions-audit-table"]')).toHaveCount(0);
      expect(blocked, `folder audit must not call history HTML: ${blocked.join(" ")}`).toEqual(
        [],
      );
      expectCleanBrowser(watched);
    },
  );

  for (const status of [403, 404]) {
    test(
      `HTTP ${status} stays in the panel and is not an empty trail`,
      { tag: ["@explorer-audit-trail", "@explorer-revisions", "@explorer"] },
      async ({ page }) => {
        const watched = watchBrowser(
          page,
          (url, code) =>
            code === status && /itemmanagement\/item\/revisions\//i.test(url),
        );
        await stubExplorer(page);
        await stubHistory(page, status, {
          message: "No workflow comments are recorded for this item",
        });
        await openExplorer(page);
        await page.locator('[data-testid="detail-row-42"][data-row-kind="item"]').click();
        await clickAudit(page);
        const error = page.locator('[data-testid="revisions-load-error"]');
        await expect(error).toBeVisible({ timeout: 10_000 });
        await expect(error).toContainText(String(status));
        await expect(page.locator('[data-testid="revisions-panel"]')).toHaveAttribute(
          "data-testid-state",
          "error",
        );
        await expect(page.locator('[data-testid="revisions-audit-empty"]')).toHaveCount(0);
        await expect(page.locator('[data-testid="audit-row-0"]')).toHaveCount(0);
        expectCleanBrowser(watched);
      },
    );
  }
});
