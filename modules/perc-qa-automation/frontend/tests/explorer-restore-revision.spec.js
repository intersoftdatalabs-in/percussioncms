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
 * Playwright surface: Explorer revisions panel restores one older revision (#5220).
 *
 * <p>This is the Explorer panel, not the editor host restore action.
 * Confirm moves the Current marker only after restore succeeds. Cancel,
 * HTTP 403, and HTTP 409 leave the current revision. Folders and an empty
 * selection do not claim a restore.</p>
 *
 * <p>Tags: {@code @explorer-restore-revision} {@code @explorer-revisions}
 * {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-restore-revision.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const REVISIONS = [
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
];

function summary(currentRevision) {
  return {
    RevisionsSummary: {
      restorable: true,
      currentRevision,
      revisions: REVISIONS,
      comments: [],
    },
  };
}

function unexpectedConsoleErrors(errors) {
  return errors.filter((text) => {
    // Chromium logs the scripted restore 403/409 as resource failures.
    // The panel asserts those statuses; they are not uncaught exceptions.
    return !/Failed to load resource: the server responded with a status of 40[39]/i.test(
      text,
    );
  });
}

async function stubExplorerList(page) {
  // Synthetic row 42 is not a CMS item. Fulfill the selection side-calls
  // so checkout-owner and template menus do not 404/500 on the server.
  await page.route("**/explorer/list-columns**", async (route) => {
    const folderPath =
      new URL(route.request().url()).searchParams.get("folderPath") || "/";
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ExplorerListColumns: { folderPath, columns: [] },
      }),
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
  await page.route("**/actions/find**", async (route) => {
    const reqUrl = route.request().url();
    if (/\/actions\/find\/(types|templates)/i.test(reqUrl)) {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ActionMenu: [] }),
      });
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
              id: "fold-1",
              name: "News",
              path: "/Sites/Demo/News/",
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
          ],
          childrenCount: 2,
          startIndex: 0,
        },
      }),
    });
  });
}

async function openExplorer(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
}

async function openRevisions(page) {
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
}

test.describe("modern React Content Explorer — restore one older revision", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "confirm restores an older revision and current updates only after success",
    { tag: ["@explorer-restore-revision", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      const restoreUrls = [];
      let restored = false;
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      await stubExplorerList(page);
      await page.route("**/itemmanagement/item/revisions/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(summary(restored ? 1 : 2)),
        });
      });
      await page.route("**/itemmanagement/item/restoreRevision/**", async (route) => {
        restoreUrls.push(route.request().url());
        restored = true;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: "{}",
        });
      });

      await openExplorer(page);
      const itemRow = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await openRevisions(page);
      await expect(page.locator('[data-testid="revisions-current"]')).toHaveAttribute(
        "data-current-rev",
        "2",
        { timeout: 10_000 },
      );

      await page.locator('[data-testid="revisions-restore-1"]').click();
      await expect(page.locator('[data-testid="revisions-restore-confirm"]')).toBeVisible();
      expect(restoreUrls).toEqual([]);
      await page.locator('[data-testid="revisions-restore-cancel"]').click();
      await expect(page.locator('[data-testid="revisions-restore-confirm"]')).toHaveCount(0);
      expect(restoreUrls).toEqual([]);
      await expect(page.locator('[data-testid="revisions-current"]')).toHaveAttribute(
        "data-current-rev",
        "2",
      );

      await page.locator('[data-testid="revisions-restore-1"]').click();
      await page.locator('[data-testid="revisions-restore-ok"]').click();
      await expect(page.locator('[data-testid="revisions-current"]')).toHaveAttribute(
        "data-current-rev",
        "1",
        { timeout: 10_000 },
      );
      expect(restoreUrls.some((url) => url.includes("restoreRevision/1-101-42"))).toBe(true);
      await expect(page.locator('[data-testid="revisions-restore-1"]')).toHaveCount(0);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        unexpectedConsoleErrors(consoleErrors),
        `console error: ${consoleErrors.join(" | ")}`,
      ).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );

  test(
    "HTTP 403 and 409 leave the current revision in place",
    { tag: ["@explorer-restore-revision", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      await stubExplorerList(page);
      await page.route("**/itemmanagement/item/revisions/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(summary(2)),
        });
      });
      await page.route("**/itemmanagement/item/restoreRevision/**", async (route) => {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "Not authorized to restore this page." }),
        });
      });

      await openExplorer(page);
      await page.locator('[data-testid="detail-row-42"]').click();
      await openRevisions(page);
      await page.locator('[data-testid="revisions-restore-1"]').click();
      await page.locator('[data-testid="revisions-restore-ok"]').click();
      await expect(page.locator('[data-testid="revisions-restore-error"]')).toContainText(
        /403/,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="revisions-current"]')).toHaveAttribute(
        "data-current-rev",
        "2",
      );

      await page.unroute("**/itemmanagement/item/restoreRevision/**");
      await page.route("**/itemmanagement/item/restoreRevision/**", async (route) => {
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "Checked out to someone else." }),
        });
      });
      await page.locator('[data-testid="revisions-restore-1"]').click();
      await page.locator('[data-testid="revisions-restore-ok"]').click();
      await expect(page.locator('[data-testid="revisions-restore-error"]')).toContainText(
        /409/,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="revisions-current"]')).toHaveAttribute(
        "data-current-rev",
        "2",
      );
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        unexpectedConsoleErrors(consoleErrors),
        `console error: ${consoleErrors.join(" | ")}`,
      ).toEqual([]);
    },
  );

  test(
    "folders and an empty selection do not claim a restore",
    { tag: ["@explorer-restore-revision", "@explorer-revisions", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      const restoreUrls = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubExplorerList(page);
      await page.route("**/itemmanagement/item/restoreRevision/**", async (route) => {
        restoreUrls.push(route.request().url());
        await route.fulfill({ status: 500, body: "{}" });
      });

      await openExplorer(page);
      await openRevisions(page);
      await expect(page.locator('[data-testid="explorer-revisions-hint"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="revisions-restore-1"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-flush-cache-status"]')).toHaveCount(0);

      await page.locator('[data-testid="detail-row-fold-1"]').click();
      await openRevisions(page);
      await expect(page.locator('[data-testid="explorer-revisions-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="revisions-restore-1"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="explorer-flush-cache-status"]')).toHaveCount(0);
      expect(restoreUrls).toEqual([]);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
