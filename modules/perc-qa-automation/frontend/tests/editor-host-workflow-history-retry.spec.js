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
 * EditorHost workflow-history panel load failure with retry (#6896, parent
 * #4532).
 *
 * <p>The workflow-history panel is read-only browse surface: populated rows
 * and the explicit empty state are covered by
 * {@code editor-host-workflow-history.spec.js}. This spec proves the failure
 * path — a 500 from
 * {@code GET /services/itemmanagement/item/revisions/{id}} renders the
 * named error alert with a retry control, retry re-runs the load for the
 * same item, success renders the recorded rows, and a repeated failure
 * keeps the alert with retry still offered. Peer: the site-impact retry
 * panel ({@code editor-host-site-impact-retry.spec.js}, #6771).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-workflow-history-retry.spec.js}</p>
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
      /Failed to load resource: the server responded with a status of (403|404|409|500)/.test(
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

test.describe("EditorHost workflow history retry (#6896)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "500 shows the named error with retry; retry loads the recorded rows",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      await stubCommon(page);
      let calls = 0;
      await page.route("**/services/itemmanagement/item/revisions/**", (route) => {
        calls += 1;
        if (calls === 1) {
          return route.fulfill({
            status: 500,
            contentType: "application/json",
            body: "{}",
          });
        }
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(HISTORY),
        });
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible({ timeout: 20_000 });
      await page.getByTestId("editor-workflow-history-toggle").click();
      const error = page.getByTestId("editor-workflow-history-load-error");
      await expect(error).toBeVisible({ timeout: 20_000 });
      await expect(error).toContainText(/could not load the workflow history/i);
      const retry = page.getByTestId("editor-retry-workflow-history-load");
      await expect(retry).toBeVisible();
      await expect(retry).toContainText(/retry/i);
      await retry.click();
      await expect(
        page.getByTestId("editor-workflow-history-row-0"),
      ).toContainText("Looks good", { timeout: 20_000 });
      await expect(
        page.getByTestId("editor-workflow-history-load-error"),
      ).toHaveCount(0);
      expect(calls).toBeGreaterThanOrEqual(2);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-workflow-history"]',
      });
      await page
        .getByTestId("editor-workflow-history")
        .screenshot({ path: "test-results/live-proof-6896-workflow-history-retry.png" });
      expect(pageErrors).toEqual([]);
    },
  );

  test(
    "a repeated failure keeps the alert and offers retry again",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      await stubCommon(page);
      await page.route("**/services/itemmanagement/item/revisions/**", (route) =>
        route.fulfill({
          status: 500,
          contentType: "application/json",
          body: "{}",
        }),
      );

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible({ timeout: 20_000 });
      await page.getByTestId("editor-workflow-history-toggle").click();
      await expect(
        page.getByTestId("editor-workflow-history-load-error"),
      ).toBeVisible({ timeout: 20_000 });
      await page.getByTestId("editor-retry-workflow-history-load").click();
      await expect(
        page.getByTestId("editor-workflow-history-load-error"),
      ).toBeVisible({ timeout: 20_000 });
      await expect(
        page.getByTestId("editor-retry-workflow-history-load"),
      ).toBeVisible();
      await expect(
        page.getByTestId("editor-workflow-history-table"),
      ).toHaveCount(0);
      expect(pageErrors).toEqual([]);
    },
  );
});
