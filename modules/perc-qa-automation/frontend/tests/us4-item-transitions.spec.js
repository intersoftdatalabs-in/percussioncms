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
 * Workflow Administration - In-Context Item Transitions (993 US5 / P1).
 *
 * <p>Discoverable E2E for "Perform In-Context Item Workflow Transitions":
 * the React content-editor workflow panel ({@code EditorWorkflowPanel},
 * the mounted successor of the removed {@code workflowactions.jsp} +
 * Dojo flow) shows the current state and allowed triggers, enforces the
 * comment requirement, fires the transition with the comment, and supports
 * ad-hoc assignees. View mode stays read-only and issues no transition
 * requests.</p>
 *
 * <p>The backend is stubbed at the route layer (same pattern as
 * {@code editor-host-workflow-transitions.spec.js}) so the spec exercises
 * the real React panel without depending on seed content; the legacy
 * content-editor JSP flow must see zero requests.</p>
 *
 * <p>Tags: {@code @workflow-admin} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/us4-item-transitions.spec.js}</p>
 *
 * <p>Coverage map: {@code specs/993-workflow-admin-react-ui/e2e-coverage-map.md}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isGetTransitionsUrl,
  isTransitionWithCommentsUrl,
  parseTransitionWithCommentsUrl,
  triggerTestId,
} = require("./helpers/editor-host-workflow");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

const TRANSITIONS = {
  ItemStateTransition: {
    itemId: "42",
    stateName: "Draft",
    workflowId: "wf-1",
    transitionTriggers: ["Submit", "Reject"],
    commentRequiredTriggers: ["Reject"],
  },
};

async function stubEditorApis(page, transitionCalls) {
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TRANSITIONS),
    }),
  );
  await page.route(
    "**/services/itemmanagement/workflow/transitionWithComments/**",
    async (route) => {
      transitionCalls.push(route.request().url());
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ itemId: "42" }),
      });
    },
  );
  // Auxiliary editor-host endpoints outside US5 scope: the fictitious
  // contentId=42 has no translations, workflow catalog, site path, or
  // related-content canvas in QA H2. Benign 200 payloads keep the spec
  // hermetic so the zero-JS-error assertion stays meaningful for US5.
  await page.route(
    "**/services/itemmanagement/workflow/allowedWorkflows/**",
    (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: '{"choices":[]}',
      }),
  );
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: '{"itemId":42,"variants":[]}',
    }),
  );
  await page.route("**/services/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: '{"PathItem":{}}',
    }),
  );
  await page.route("**/services/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: '{"ownerId":42,"templateId":null,"slots":[]}',
    }),
  );
  // Page site-impact panel for the fictitious content id (defensive
  // parser accepts the bare {pages, templates} shape).
  await page.route("**/services/itemmanagement/item/siteimpact/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: '{"pages":[],"templates":[]}',
    }),
  );
}

test.describe("Workflow Administration - In-Context Item Transitions (993 US5)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode shows state and triggers, enforces comments, then transitions",
    { tag: ["@workflow-admin", "@editor", "@workflow"] },
    async ({ page }) => {
      const transitionCalls = [];
      const pageErrors = [];
      const leftover = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("checkoutedit.xml") ||
          u.includes("contenteditorurls.html") ||
          // #6139: retired AA workflow dialog panes must also see zero requests
          // (EditorWorkflowPanel transitions via REST, never these JSPs).
          u.includes("workflowactions.jsp") ||
          u.includes("adhocsearch.jsp") ||
          u.includes("adhocresults.jsp") ||
          /view=editor/.test(u)
        ) {
          leftover.push(u);
        }
      });
      await stubEditorApis(page, transitionCalls);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.workflow}"]`)).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.workflowState}"]`),
      ).toContainText(/Draft/);
      await expect(
        page.locator(`[data-testid="${triggerTestId("Submit")}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${triggerTestId("Reject")}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${triggerTestId("Approve")}"]`),
      ).toHaveCount(0);

      // Reject without a comment must be blocked before any backend call.
      await page.locator(`[data-testid="${triggerTestId("Reject")}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toContainText(
        /Enter a comment/i,
      );
      expect(transitionCalls).toEqual([]);

      // With a comment the transition fires exactly once, carrying the comment.
      await page.locator(`[data-testid="${TEST_IDS.comment}"]`).fill("needs work");
      await page.locator(`[data-testid="${triggerTestId("Reject")}"]`).click();
      await expect.poll(() => transitionCalls.length).toBe(1);
      const parsed = parseTransitionWithCommentsUrl(transitionCalls[0]);
      expect(parsed.trigger).toBe("Reject");
      expect(parsed.comment).toBe("needs work");
      expect(isTransitionWithCommentsUrl(transitionCalls[0])).toBe(true);
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toBeVisible();

      // No legacy content-editor flow and no JS errors.
      expect(leftover, `leftover CE requested: ${leftover.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "ad-hoc assignee can be added to and removed from the transition",
    { tag: ["@workflow-admin", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      await stubEditorApis(page, []);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.workflow}"]`)).toBeVisible({
        timeout: 20_000,
      });

      const assignee = "us4adhoc";
      await page
        .locator('[data-testid="editor-workflow-assignee-input"]')
        .fill(assignee);
      await page.locator('[data-testid="editor-workflow-assignee-add"]').click();
      await expect(
        page.locator(`[data-testid="editor-workflow-assignee-${assignee}"]`),
      ).toBeVisible();

      await page
        .locator(`[data-testid="editor-workflow-assignee-remove-${assignee}"]`)
        .click();
      await expect(
        page.locator(`[data-testid="editor-workflow-assignee-${assignee}"]`),
      ).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "view mode stays read-only and issues no transition requests",
    { tag: ["@workflow-admin", "@editor", "@workflow"] },
    async ({ page }) => {
      const transitionGets = [];
      await stubEditorApis(page, []);
      page.on("request", (req) => {
        if (isGetTransitionsUrl(req.url()) || isTransitionWithCommentsUrl(req.url())) {
          transitionGets.push(req.url());
        }
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.workflow}"]`)).toHaveCount(0);
      expect(transitionGets).toEqual([]);
    },
  );
});
