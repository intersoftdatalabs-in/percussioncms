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
 * React Content Editor host — ad-hoc assignees on a workflow transition (#5163).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-workflow-assignees.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
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

function consoleOn(page, pageErrors) {
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (400|403|404|409|500)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ assigneeRequiredTriggers?: string[], failStatus?: number }} [opts]
 */
async function stubEditorApis(page, opts = {}) {
  const transitionCalls = [];
  let transitioned = false;
  const required = opts.assigneeRequiredTriggers ?? [];
  const failStatus = opts.failStatus ?? 0;
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
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) => {
    const stateName = transitioned ? "Review" : "Draft";
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName,
          transitionTriggers: transitioned ? ["Approve"] : ["Submit", "Reject"],
          assigneeRequiredTriggers: transitioned ? [] : required,
        },
      }),
    });
  });
  await page.route(
    "**/services/itemmanagement/workflow/transitionWithComments/**",
    async (route) => {
      transitionCalls.push(route.request().url());
      if (failStatus) {
        await route.fulfill({
          status: failStatus,
          contentType: "application/json",
          body: JSON.stringify({ message: "rejected" }),
        });
        return;
      }
      transitioned = true;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ itemId: "42" }),
      });
    },
  );
  return transitionCalls;
}

async function openEditor(page) {
  await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
  await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.workflow}"]`)).toBeVisible();
}

test.describe("React Content Editor ad-hoc assignees", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "required assignees: cancel and empty confirm do not transition; names are sent",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const calls = await stubEditorApis(page, { assigneeRequiredTriggers: ["Submit"] });
      await openEditor(page);
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Draft",
      );
      await expect(page.locator(`[data-testid="${triggerTestId("Submit")}"]`)).toHaveAttribute(
        "data-assignees-required",
        "true",
      );

      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toBeVisible();
      await page.getByTestId("editor-workflow-assignee-confirm").click();
      await expect(page.getByTestId("editor-workflow-assignee-error")).toContainText(
        /at least one assignee/i,
      );
      expect(calls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Draft",
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toHaveCount(0);

      await page.getByTestId("editor-workflow-assignee-cancel").click();
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toHaveCount(0);
      expect(calls).toEqual([]);

      await page.getByTestId("editor-workflow-assignee-input").fill("alice");
      await page.getByTestId("editor-workflow-assignee-add").click();
      await page.locator(`[data-testid="${TEST_IDS.comment}"]`).fill("ready");
      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toBeVisible();
      expect(calls).toEqual([]);
      await page.getByTestId("editor-workflow-assignee-confirm").click();
      await expect.poll(() => calls.length).toBe(1);
      const parsed = parseTransitionWithCommentsUrl(calls[0]);
      expect(parsed.trigger).toBe("Submit");
      expect(parsed.comment).toBe("ready");
      expect(parsed.adhocAssignees).toBe("alice");
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Review",
      );
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "a transition that does not need assignees sends the comment only unless names were chosen",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const calls = await stubEditorApis(page, { assigneeRequiredTriggers: [] });
      await openEditor(page);
      await page.locator(`[data-testid="${TEST_IDS.comment}"]`).fill("note");
      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect.poll(() => calls.length).toBe(1);
      const commentOnly = parseTransitionWithCommentsUrl(calls[0]);
      expect(commentOnly.comment).toBe("note");
      expect(commentOnly.adhocAssignees).toBe("");
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "HTTP 400, 403, and 409 do not change the state label",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      for (const status of [400, 403, 409]) {
        const pageErrors = [];
        consoleOn(page, pageErrors);
        const calls = await stubEditorApis(page, {
          assigneeRequiredTriggers: ["Submit"],
          failStatus: status,
        });
        await openEditor(page);
        await page.getByTestId("editor-workflow-assignee-input").fill("cara");
        await page.getByTestId("editor-workflow-assignee-add").click();
        await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
        await page.getByTestId("editor-workflow-assignee-confirm").click();
        await expect.poll(() => calls.length).toBe(1);
        await expect(page.getByTestId("editor-workflow-assignee-dialog")).toBeVisible();
        await expect(page.getByTestId("editor-workflow-assignee-error")).toBeVisible();
        await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
          "Draft",
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toHaveCount(0);
        expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      }
    },
  );
});
