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
 * React Content Editor host — server-driven comment-required transitions (#5828).
 *
 * <p>The `getTransitions` DTO carries a workflow-designer-configured
 * `commentRequiredTriggers` list. The editor host enforces that list like the
 * classic surface, extending the built-in reject-style names: a listed
 * trigger blocks until the comment is non-empty, while an unlisted,
 * non-reject trigger still runs without a comment.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-workflow-comment-required-server.spec.js}</p>
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
 * @param {{ commentRequiredTriggers?: string[], triggers?: string[] }} [opts]
 */
async function stubEditorApis(page, opts = {}) {
  const transitionCalls = [];
  let transitioned = false;
  const commentRequired = opts.commentRequiredTriggers ?? ["Approve"];
  const triggers = opts.triggers ?? ["Approve", "Submit"];
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
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ItemWorkflowChoices: { choices: [] } }),
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
          transitionTriggers: transitioned ? ["Approve"] : triggers,
          commentRequiredTriggers: transitioned ? [] : commentRequired,
        },
      }),
    });
  });
  await page.route(
    "**/services/itemmanagement/workflow/transitionWithComments/**",
    async (route) => {
      transitionCalls.push(route.request().url());
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

test.describe("React Content Editor server-driven comment-required transitions", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "a server-listed trigger blocks until commented; an unlisted trigger runs",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const calls = await stubEditorApis(page, { commentRequiredTriggers: ["Approve"] });
      await openEditor(page);
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Draft",
      );
      await expect(page.locator(`[data-testid="${triggerTestId("Approve")}"]`)).toHaveAttribute(
        "data-comment-required",
        "true",
      );
      await expect(page.locator(`[data-testid="${triggerTestId("Submit")}"]`)).toHaveAttribute(
        "data-comment-required",
        "false",
      );

      await page.locator(`[data-testid="${triggerTestId("Approve")}"]`).click();
      await expect(page.getByTestId(TEST_IDS.error)).toContainText(/enter a comment/i);
      expect(calls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toHaveCount(0);

      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect.poll(() => calls.length).toBe(1);
      const submitted = parseTransitionWithCommentsUrl(calls[0]);
      expect(submitted.trigger).toBe("Submit");
      expect(submitted.comment).toBe("");
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      // Workflow surface only. The editor also mounts Translations, whose
      // unstubbed "Item not found" alert is outside this slice.
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.workflow}"]`,
      });
    },
  );

  test(
    "an empty server list keeps the built-in reject-style gate",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const calls = await stubEditorApis(page, {
        commentRequiredTriggers: [],
        triggers: ["Submit", "Reject"],
      });
      await openEditor(page);
      await expect(page.locator(`[data-testid="${triggerTestId("Reject")}"]`)).toHaveAttribute(
        "data-comment-required",
        "true",
      );
      await expect(page.locator(`[data-testid="${triggerTestId("Submit")}"]`)).toHaveAttribute(
        "data-comment-required",
        "false",
      );
      await page.locator(`[data-testid="${triggerTestId("Reject")}"]`).click();
      await expect(page.getByTestId(TEST_IDS.error)).toContainText(/enter a comment/i);
      expect(calls).toEqual([]);
      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect.poll(() => calls.length).toBe(1);
      const parsed = parseTransitionWithCommentsUrl(calls[0]);
      expect(parsed.trigger).toBe("Submit");
      expect(parsed.comment).toBe("");
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
