/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software,
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * React Content Editor host — quick Approve/Reject action bar (#6483).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-approve-reject.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  editorSpaUrl,
  isTransitionWithCommentsUrl,
  parseTransitionWithCommentsUrl,
} = require("./helpers/editor-host-workflow");

const BAR = "editor-approve-reject-bar";
const APPROVE = "editor-approve-quick";
const REJECT = "editor-reject-quick";
const DIALOG = "editor-approve-reject-dialog";
const COMMENT = "editor-approve-reject-comment";
const REQUIRED = "editor-approve-reject-required";
const CONFIRM = "editor-approve-reject-confirm";
const CANCEL = "editor-approve-reject-cancel";
const ERROR = "editor-approve-reject-error";
const DONE = "editor-approve-reject-done";

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

function transitionsBody(triggers) {
  return {
    ItemStateTransition: {
      itemId: "42",
      stateName: "Draft",
      transitionTriggers: triggers,
    },
  };
}

// Browser-automatic "Failed to load resource" logging for HTTP statuses the
// app itself handles and asserts on (e.g. the deliberate 409 below) is not
// app console noise: pageerrors and real console errors still fail the run.
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

async function stubEditorApis(page, transitionCalls, options = {}) {
  const status = options.transitionStatus || 200;
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
  await page.route(
    "**/services/itemmanagement/workflow/getTransitions/**",
    (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          transitionsBody(options.triggers || ["Submit", "Approve", "Reject"]),
        ),
      }),
  );
  // Isolation stubs: unrelated panels on the same host hit these for the
  // fictional item id in a fresh H2 cell (404/400/500 console noise that
  // fails the zero-console-error gate even though the bar is green).
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
        status,
        contentType: "application/json",
        body: JSON.stringify(status === 200 ? { itemId: "42" } : {}),
      });
    },
  );
}

test.describe("React Content Editor quick Approve/Reject bar", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "approve runs from the header bar with a comment and reports success",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const transitionCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, transitionCalls);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${BAR}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${APPROVE}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${REJECT}"]`)).toBeVisible();

      await page.locator(`[data-testid="${APPROVE}"]`).click();
      await expect(page.locator(`[data-testid="${DIALOG}"]`)).toBeVisible();
      await expect(
        page.locator(`[data-testid="${REQUIRED}"]`),
      ).toHaveCount(0);
      await page.locator(`[data-testid="${COMMENT}"]`).fill("looks good");
      await page.locator(`[data-testid="${CONFIRM}"]`).click();

      await expect.poll(() => transitionCalls.length).toBe(1);
      const parsed = parseTransitionWithCommentsUrl(transitionCalls[0]);
      expect(parsed.trigger).toBe("Approve");
      expect(parsed.comment).toBe("looks good");
      expect(isTransitionWithCommentsUrl(transitionCalls[0])).toBe(true);

      await expect(page.locator(`[data-testid="${DONE}"]`)).toContainText(
        /approved/i,
      );
      await expect(page.locator(`[data-testid="${DIALOG}"]`)).toHaveCount(0);
      await page.screenshot({
        path: `test-results/live-proof-6483-approve-reject.png`,
      });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="editor-host"]`,
      });
    },
  );

  test(
    "reject blocks a blank comment, then a 409 stays on the dialog",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const transitionCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, transitionCalls, { transitionStatus: 409 });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${REJECT}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${REJECT}"]`).click();
      await expect(page.locator(`[data-testid="${DIALOG}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${REQUIRED}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${CONFIRM}"]`)).toBeDisabled();
      expect(transitionCalls).toEqual([]);

      await page.locator(`[data-testid="${COMMENT}"]`).fill("needs rework");
      await expect(page.locator(`[data-testid="${CONFIRM}"]`)).toBeEnabled();
      await page.locator(`[data-testid="${CONFIRM}"]`).click();
      await expect.poll(() => transitionCalls.length).toBe(1);
      const parsed = parseTransitionWithCommentsUrl(transitionCalls[0]);
      expect(parsed.trigger).toBe("Reject");
      expect(parsed.comment).toBe("needs rework");

      await expect(page.locator(`[data-testid="${ERROR}"]`)).toContainText(
        /409/,
      );
      await expect(page.locator(`[data-testid="${DIALOG}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${DONE}"]`)).toHaveCount(0);

      await page.locator(`[data-testid="${CANCEL}"]`).click();
      await expect(page.locator(`[data-testid="${DIALOG}"]`)).toHaveCount(0);
      expect(transitionCalls.length).toBe(1);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "bar stays hidden when the allowlist names neither Approve nor Reject",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      await stubEditorApis(page, [], { triggers: ["Submit"] });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-host"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${BAR}"]`)).toHaveCount(0);
      await expect(page.locator(`[data-testid="${APPROVE}"]`)).toHaveCount(0);
      await expect(page.locator(`[data-testid="${REJECT}"]`)).toHaveCount(0);
    },
  );
});
