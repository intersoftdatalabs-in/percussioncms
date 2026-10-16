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
 * EditorHost refuses a workflow transition comment that contains a NUL (#5443 / #4532).
 *
 * <p>A keyboard cannot type U+0000, so the spec sets the comment through the
 * textarea value setter and an {@code input} event (the same controlled
 * {@code onChange} path EditorHost already uses).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-workflow-comment-nul.spec.js}</p>
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

const NUL_COMMENT = `bad${String.fromCharCode(0)}value`;

/**
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setTextareaValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload.codes);
    const proto = Object.getOwnPropertyDescriptor(
      window.HTMLTextAreaElement.prototype,
      "value",
    );
    if (!proto || typeof proto.set !== "function") {
      throw new Error("textarea value setter missing");
    }
    const previous = el.value;
    proto.set.call(el, next);
    const tracker = el._valueTracker;
    if (tracker) {
      tracker.setValue(previous);
    }
    el.dispatchEvent(new Event("input", { bubbles: true }));
  }, { codes });
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {string[]} pageErrors
 */
function trackPageErrors(page, pageErrors) {
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
  return transitionCalls;
}

async function openEditor(page) {
  await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
  await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.workflow}"]`)).toBeVisible();
  await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
    "Draft",
  );
}

test.describe("EditorHost workflow comment NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a NUL comment, reload stays Draft, cancel does not transition, and an ordinary comment still runs",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      trackPageErrors(page, pageErrors);
      const calls = await stubEditorApis(page, { assigneeRequiredTriggers: ["Submit"] });
      await openEditor(page);

      const comment = page.locator(`[data-testid="${TEST_IDS.comment}"]`);
      await setTextareaValue(comment, NUL_COMMENT);
      await expect
        .poll(() => comment.evaluate((el) => el.value.includes("\u0000")))
        .toBe(true);
      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toContainText(
        /transition comment contains a character that cannot be saved/i,
      );
      expect(calls).toEqual([]);
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Draft",
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Draft",
      );
      expect(calls).toEqual([]);

      await comment.fill("ready");
      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toBeVisible();
      expect(calls).toEqual([]);
      await page.getByTestId("editor-workflow-assignee-cancel").click();
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toHaveCount(0);
      expect(calls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Draft",
      );

      await page.getByTestId("editor-workflow-assignee-input").fill("alice");
      await page.getByTestId("editor-workflow-assignee-add").click();
      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect(page.getByTestId("editor-workflow-assignee-dialog")).toBeVisible();
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
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.workflow}"]`,
      });
    },
  );

  test(
    "a blank optional comment still transitions and a blank required comment stays blocked",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      trackPageErrors(page, pageErrors);
      const calls = await stubEditorApis(page);
      await openEditor(page);

      await page.locator(`[data-testid="${triggerTestId("Reject")}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toContainText(
        /Enter a comment/i,
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).not.toContainText(
        /character that cannot be saved/i,
      );
      expect(calls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Draft",
      );

      await setTextareaValue(
        page.locator(`[data-testid="${TEST_IDS.comment}"]`),
        `needs${String.fromCharCode(0)}work`,
      );
      await page.locator(`[data-testid="${triggerTestId("Reject")}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toContainText(
        /transition comment contains a character that cannot be saved/i,
      );
      expect(calls).toEqual([]);

      await page.locator(`[data-testid="${TEST_IDS.comment}"]`).fill("   ");
      await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
      await expect.poll(() => calls.length).toBe(1);
      const parsed = parseTransitionWithCommentsUrl(calls[0]);
      expect(parsed.trigger).toBe("Submit");
      expect(parsed.comment).toBe("");
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
        "Review",
      );
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "HTTP 400, 403, and 409 are not a completed transition",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      for (const status of [400, 403, 409]) {
        const pageErrors = [];
        trackPageErrors(page, pageErrors);
        const calls = await stubEditorApis(page, { failStatus: status });
        await openEditor(page);
        await page.locator(`[data-testid="${TEST_IDS.comment}"]`).fill("ready");
        await page.locator(`[data-testid="${triggerTestId("Submit")}"]`).click();
        await expect.poll(() => calls.length).toBe(1);
        await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toBeVisible();
        await expect(page.locator(`[data-testid="${TEST_IDS.workflowState}"]`)).toContainText(
          "Draft",
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toHaveCount(0);
        expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      }
    },
  );
});
