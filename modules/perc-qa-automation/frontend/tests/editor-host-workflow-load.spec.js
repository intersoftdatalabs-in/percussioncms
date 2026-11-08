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
 * React Content Editor workflow-transition panel load notice (#5769,
 * parent #4532 slice 3: workflow-transition increment).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-workflow-load.spec.js}</p>
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
} = require("./helpers/editor-host-save-fields");

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
    ],
  },
};

function fieldsPayload(title, revision) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "displaytitle", value: title },
      ],
    },
  };
}

async function stubEditorApis(page, { failTransitionsTimes = 0 } = {}) {
  const calls = { transitions: 0, transitionsFails: 0 };
  // Registered first so later field/workflow stubs win. Keeps fake id 42
  // off the QA server log; the browser still ignores failed-resource noise.
  await page.route("**/services/**", (route) =>
    route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/**", (route) =>
    route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route(
    "**/services/itemmanagement/workflow/getTransitions/**",
    async (route) => {
      calls.transitions += 1;
      if (calls.transitionsFails < failTransitionsTimes) {
        calls.transitionsFails += 1;
        await route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({ Error: { message: "transitions boom" } }),
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemStateTransition: {
            itemId: "42",
            stateName: "Draft",
            transitionTriggers: ["Approve"],
          },
        }),
      });
    },
  );
  await page.route(
    "**/services/itemmanagement/workflow/allowedWorkflows/**",
    (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemWorkflowChoices: {
            itemId: "42",
            currentWorkflowId: "4",
            choices: [{ id: "4", name: "Local" }],
          },
        }),
      }),
  );
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, locale: "en-us", variants: [] }),
    }),
  );
  await page.route("**/services/assembly/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsPayload("Welcome", 2)),
    }),
  );
  return calls;
}

function trackPageProblems(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (text.includes("Failed to load resource")) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("React Content Editor workflow-transition panel load notice", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "a failed transition load shows a panel notice, and retry renders the triggers",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackPageProblems(page);
      const calls = await stubEditorApis(page, { failTransitionsTimes: 1 });
      const titleField = () => page.locator('[data-testid="editor-field-displaytitle"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(titleField()).toHaveValue("Welcome", { timeout: 20_000 });
      // The transition list failed to load: the workflow panel shows a
      // dedicated alert with retry — not the empty-allowlist hint and no
      // trigger buttons.
      await expect(page.locator('[data-testid="editor-workflow"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-workflow-load-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-workflow-load-error"]')).toContainText(
        /could not load the workflow/i,
      );
      await expect(page.locator('[data-testid="editor-retry-workflow-load"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-workflow-empty"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-workflow-trigger-Approve"]')).toHaveCount(0);

      // Retry re-runs the transition load for the same item: the Approve
      // trigger renders and the notice clears.
      await page.locator('[data-testid="editor-retry-workflow-load"]').click();
      await expect(page.locator('[data-testid="editor-workflow-trigger-Approve"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-workflow-load-error"]')).toHaveCount(0);
      expect(calls.transitions).toBeGreaterThanOrEqual(2);
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );

  test(
    "a repeated transition load failure keeps the notice without claiming triggers",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackPageProblems(page);
      const calls = await stubEditorApis(page, { failTransitionsTimes: 99 });
      const titleField = () => page.locator('[data-testid="editor-field-displaytitle"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(titleField()).toHaveValue("Welcome", { timeout: 20_000 });
      await expect(page.locator('[data-testid="editor-workflow-load-error"]')).toBeVisible();
      await page.locator('[data-testid="editor-retry-workflow-load"]').click();
      await expect.poll(() => calls.transitions).toBeGreaterThanOrEqual(2);
      await expect(page.locator('[data-testid="editor-workflow-load-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-workflow-trigger-Approve"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-workflow-empty"]')).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
