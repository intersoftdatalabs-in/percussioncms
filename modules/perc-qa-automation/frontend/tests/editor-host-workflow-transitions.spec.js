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
 * React Content Editor host — allowed workflow transitions with comments (#4539).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-workflow-transitions.spec.js}</p>
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
    transitionTriggers: ["Submit", "Reject"],
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
  // Isolation stubs: unrelated panels on the same host hit these for the
  // fictional item id in a fresh H2 cell (404/400/500 console noise that
  // fails the zero-console-error gate even though the surface is green;
  // peer: editor-host-approve-reject.spec.js).
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({}),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({}),
    }),
  );
  await page.route("**/services/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({}),
    }),
  );
  await page.route("**/services/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ slots: [] }),
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
}

test.describe("React Content Editor workflow transitions", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode lists allowed triggers, blocks Reject without comment, then transitions",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
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
      await expect(page.locator(`[data-testid="${triggerTestId("Submit")}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${triggerTestId("Reject")}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${triggerTestId("Approve")}"]`)).toHaveCount(0);
      // Visible comment-required affordance (#6632): a sighted keyboard
      // user sees which trigger needs a comment before clicking it, and the
      // button exposes the comment label via aria-describedby.
      await expect(
        page.locator(`[data-testid="${triggerTestId("Reject")}-requires-comment"]`),
      ).toContainText(/requires comment/i);
      await expect(
        page.locator(`[data-testid="${triggerTestId("Reject")}"]`),
      ).toHaveAttribute("aria-describedby", "editor-workflow-comment-label");
      await expect(
        page.locator(`[data-testid="${triggerTestId("Submit")}-requires-comment"]`),
      ).toHaveCount(0);
      await page
        .locator('[data-testid="editor-workflow-actions"]')
        .screenshot({
          path: `test-results/live-proof-6632-workflow-comment-affordance.png`,
        });
      // Hermetic load path: the transitions + choices loads both succeed off
      // stubs, so no panel load notice may appear.
      await expect(
        page.locator(`[data-testid="${TEST_IDS.workflowLoadError}"]`),
      ).toHaveCount(0);

      await page.locator(`[data-testid="${triggerTestId("Reject")}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toContainText(
        /Enter a comment/i,
      );
      expect(transitionCalls).toEqual([]);
      // Keyboard path (#4554): the comment-required block moves focus to the
      // comment box so keyboard users land where the fix happens.
      await expect(
        page.locator(`[data-testid="${TEST_IDS.comment}"]`),
      ).toBeFocused();

      await page.locator(`[data-testid="${TEST_IDS.comment}"]`).fill("needs work");
      await page.locator(`[data-testid="${triggerTestId("Reject")}"]`).click();
      await expect.poll(() => transitionCalls.length).toBe(1);
      const parsed = parseTransitionWithCommentsUrl(transitionCalls[0]);
      expect(parsed.trigger).toBe("Reject");
      expect(parsed.comment).toBe("needs work");
      expect(isTransitionWithCommentsUrl(transitionCalls[0])).toBe(true);
      await expect(page.locator(`[data-testid="${TEST_IDS.done}"]`)).toBeVisible();
      await page.screenshot({
        path: `test-results/live-proof-4554-workflow-transitions.png`,
      });
      expect(leftover, `leftover CE requested: ${leftover.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "view mode stays read-only and does not request transitions",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
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
