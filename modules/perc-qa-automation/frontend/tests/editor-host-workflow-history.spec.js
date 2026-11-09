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
 * React Content Editor host — read-only workflow history/audit panel (#5829,
 * parent #4532).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run: {@code npm run test:surface -- --path tests/editor-host-workflow-history.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { TEST_IDS, editorSpaUrl } = require("./helpers/editor-host-checkout");

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const HISTORY = {
  RevisionsSummary: {
    restorable: false,
    currentRevision: 3,
    revisions: [],
    comments: [
      {
        comment: "Looks good",
        commenter: "admin",
        commentType: "Approve",
        commentDate: "2026-04-12",
      },
      {
        comment: "",
        commenter: "editor2",
        commentType: "Submit",
        commentDate: "2026-04-10",
      },
      { comment: "", commenter: "", commentType: "", commentDate: "" },
    ],
  },
};

function trackErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (403|404|409)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

async function stubCommon(page) {
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: "admin",
          currentUser: "admin",
          assignmentType: "Assignee",
        },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    }),
  );
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: { id: "42", name: "Home", path: "//Folders/Lab/Home", type: "page" },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ choices: [] }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: { itemId: "42", stateName: "Draft", transitionTriggers: [] },
      }),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    }),
  );
  await page.route("**/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ SlotCanvas: { slots: [] } }),
    }),
  );
  await page.route("**/content-explorer/relationships/**/local", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ PSLocalDependencySummary: { links: [] } }),
    }),
  );
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, locale: "en-us", variants: [] }),
    }),
  );
  await page.route("**/itemmanagement/item/siteimpact/page/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ pages: [], templates: [] }),
    }),
  );
}

test.describe("EditorHost workflow history", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "toggle lists recorded transitions and drops blank rows",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      await page.addInitScript(() => {
        window.close = () => undefined;
      });
      await stubCommon(page);
      let historyGets = 0;
      await page.route("**/services/itemmanagement/item/revisions/**", (route) => {
        historyGets += 1;
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(HISTORY),
        });
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      // Collapsed by default: no history fetch fires and no panel renders.
      await expect(page.getByTestId("editor-workflow-history-toggle")).toBeVisible();
      await expect(page.getByTestId("editor-workflow-history")).toHaveCount(0);
      expect(historyGets).toBe(0);
      await page.getByTestId("editor-workflow-history-toggle").click();
      await expect(page.getByTestId("editor-workflow-history-table")).toBeVisible();
      expect(historyGets).toBe(1);
      await expect(page.getByTestId("editor-workflow-history-row-0")).toBeVisible();
      await expect(page.getByTestId("editor-workflow-history-date-0")).toContainText(
        "2026-04-12",
      );
      await expect(page.getByTestId("editor-workflow-history-user-0")).toContainText(
        "admin",
      );
      await expect(page.getByTestId("editor-workflow-history-type-0")).toContainText(
        "Approve",
      );
      await expect(page.getByTestId("editor-workflow-history-comment-0")).toContainText(
        "Looks good",
      );
      await expect(page.getByTestId("editor-workflow-history-user-1")).toContainText(
        "editor2",
      );
      // The fully blank comment object is not a row.
      await expect(page.getByTestId("editor-workflow-history-row-2")).toHaveCount(0);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-workflow-history"]',
      });
      expect(pageErrors).toEqual([]);
    },
  );

  test(
    "empty history shows an empty state and 403 shows a load error",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      await stubCommon(page);
      await page.route("**/services/itemmanagement/item/revisions/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            RevisionsSummary: { restorable: false, revisions: [], comments: [] },
          }),
        }),
      );
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      // Read-only history is available in view mode too.
      await page.getByTestId("editor-workflow-history-toggle").click();
      await expect(page.getByTestId("editor-workflow-history-empty")).toBeVisible();
      await expect(page.getByTestId("editor-workflow-history-empty")).toContainText(
        /no workflow comments/i,
      );

      await page.unroute("**/services/itemmanagement/item/revisions/**");
      await page.route("**/services/itemmanagement/item/revisions/**", (route) =>
        route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "forbidden" }),
        }),
      );
      await page.getByTestId("editor-workflow-history-toggle").click();
      await page.getByTestId("editor-workflow-history-toggle").click();
      await expect(
        page.getByTestId("editor-workflow-history-load-error"),
      ).toBeVisible();
      await expect(
        page.getByTestId("editor-workflow-history-load-error"),
      ).toContainText(/could not load the workflow history/i);
      await expect(page.getByTestId("editor-workflow-history-empty")).toHaveCount(0);
      expect(pageErrors).toEqual([]);
    },
  );
});
