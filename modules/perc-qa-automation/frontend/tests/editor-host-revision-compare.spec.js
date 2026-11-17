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
 * React Content Editor host — compare two revisions (#4792).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-revision-compare.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-workflow");

const ITEM_FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const REVISIONS = {
  RevisionsSummary: {
    restorable: true,
    Revisions: {
      Revision: [
        {
          revId: 4,
          status: "Live",
          lastModifier: "admin",
          lastModifiedDate: "2026-04-10",
        },
        {
          revId: 5,
          status: "Quick Edit",
          lastModifier: "admin",
          lastModifiedDate: "2026-04-12",
        },
      ],
    },
    Comments: { Comment: [] },
  },
};

const COMPARE = {
  ItemRevisionCompare: {
    itemId: "42",
    rev1: 4,
    rev2: 5,
    fields: {
      ItemRevisionFieldDiff: [
        {
          name: "sys_title",
          leftValue: "Old title",
          rightValue: "Home",
          changed: true,
        },
        {
          name: "displaytitle",
          leftValue: "Same",
          rightValue: "Same",
          changed: false,
        },
      ],
    },
  },
};

const COMPARE_WORD_DIFF = {
  ItemRevisionCompare: {
    itemId: "42",
    rev1: 4,
    rev2: 5,
    fields: {
      ItemRevisionFieldDiff: [
        {
          name: "sys_title",
          leftValue: "The old title",
          rightValue: "The new title",
          changed: true,
        },
        {
          name: "displaytitle",
          leftValue: "Same",
          rightValue: "Same",
          changed: false,
        },
      ],
    },
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
    // The stubbed cell leaves newer host auxiliary endpoints unstubbed, so
    // the bare H2 CMS answers 400/500 for those background loads. Those
    // resource-load failures are harness noise, not product errors (same
    // filter as the merged field-error-summary jump spec).
    if (/Failed to load resource/.test(text)) {
      return;
    }
    pageErrors.push(text);
  });
}

async function stubEditor(page, { compareStatus = 200, compareBody = COMPARE, revisionsBody = REVISIONS, compareDelayMs = 0 } = {}) {
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName: "Draft",
          transitionTriggers: [],
        },
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
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(ITEM_FIELDS),
    }),
  );
  await page.route("**/services/itemmanagement/item/revisions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(revisionsBody),
    }),
  );
  // The host loads publish schedule dates on mount; stub them so the fake
  // content id never hits the real cell (server.log ERROR, #6533).
  await page.route("**/services/itemmanagement/item/getitemdates/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: "42", startDate: "", endDate: "", comments: "" }),
    }),
  );
  // The host loads the content-type workflow catalog on mount; stub it for
  // the same reason (bare-cell 404 logs server-side otherwise).
  await page.route(
    "**/services/itemmanagement/workflow/allowedWorkflows/**",
    (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ itemId: "42", choices: [] }),
      }),
  );
  await page.route("**/services/itemmanagement/item/compare/**", (route) => {
    const fulfill = () =>
      route.fulfill({
        status: compareStatus,
        contentType: "application/json",
        body: JSON.stringify(
          compareStatus >= 400 ? { message: "compare failed" } : compareBody,
        ),
      });
    if (compareDelayMs > 0) {
      return new Promise((resolve) => {
        setTimeout(() => resolve(fulfill()), compareDelayMs);
      });
    }
    return fulfill();
  });
}

test.describe("React Content Editor compare two revisions", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "shows a field-level diff for two revisions and does not restore",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      let restoreHits = 0;
      await page.route("**/services/itemmanagement/item/restoreRevision/**", (route) => {
        restoreHits += 1;
        return route.fulfill({ status: 500, contentType: "application/json", body: "{}" });
      });
      await stubEditor(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-form")).toBeVisible();
      await page.getByTestId("editor-restore-toggle").click();
      await expect(page.getByTestId("editor-compare-left")).toBeVisible();
      await page.getByTestId("editor-compare-run").click();
      await expect(page.getByTestId("editor-compare-row-sys_title")).toContainText(
        "Old title",
      );
      await expect(page.getByTestId("editor-compare-row-sys_title")).toContainText(
        "Changed",
      );
      expect(restoreHits).toBe(0);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      await expectNoSeriousA11yViolations(page);
    },
  );

  test(
    "same revision is not a diff and a compare 404 is visible",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditor(page, { compareStatus: 404 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-form")).toBeVisible();
      await page.getByTestId("editor-restore-toggle").click();
      await page.getByTestId("editor-compare-right").selectOption("4");
      await expect(page.getByTestId("editor-compare-run")).toBeDisabled();
      await expect(page.getByTestId("editor-compare-table")).toHaveCount(0);
      await page.getByTestId("editor-compare-right").selectOption("5");
      await page.getByTestId("editor-compare-run").click();
      await expect(page.getByTestId("editor-compare-error")).toContainText(
        /not found for compare/i,
      );
      await expect(page.getByTestId("editor-compare-table")).toHaveCount(0);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "empty history does not show a compare table",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditor(page, {
        revisionsBody: {
          RevisionsSummary: {
            restorable: false,
            Revisions: { Revision: [] },
            Comments: { Comment: [] },
          },
        },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-form")).toBeVisible();
      await page.getByTestId("editor-restore-toggle").click();
      await expect(page.getByTestId("editor-compare-need-two")).toBeVisible();
      await expect(page.getByTestId("editor-compare-table")).toHaveCount(0);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "loading status, changed-only filter, and revision-named restore notice",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditor(page, { compareDelayMs: 800 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-form")).toBeVisible();
      await page.getByTestId("editor-restore-toggle").click();
      await expect(page.getByTestId("editor-compare-left")).toBeVisible();
      await page.getByTestId("editor-compare-run").click();
      await expect(page.getByTestId("editor-compare-loading")).toBeVisible();
      await expect(page.getByTestId("editor-compare-table")).toBeVisible();
      await expect(page.getByTestId("editor-compare-loading")).toHaveCount(0);
      await expect(
        page.getByTestId("editor-compare-row-displaytitle"),
      ).toBeVisible();
      await page.getByTestId("editor-compare-changed-only").check();
      await expect(
        page.getByTestId("editor-compare-row-displaytitle"),
      ).toHaveCount(0);
      await expect(
        page.getByTestId("editor-compare-row-sys_title"),
      ).toBeVisible();
      await page.getByTestId("editor-compare-use-left-sys_title").click();
      await expect(
        page.getByTestId("editor-compare-field-restored"),
      ).toContainText(/sys_title.*revision #4/);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      await expectNoSeriousA11yViolations(page);
    },
  );

  test(
    "changed cells mark added and removed words with a keyboard toggle",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditor(page, { compareBody: COMPARE_WORD_DIFF });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-form")).toBeVisible();
      await page.getByTestId("editor-restore-toggle").click();
      await expect(page.getByTestId("editor-compare-left")).toBeVisible();
      await page.getByTestId("editor-compare-run").click();
      const older = page.getByTestId("editor-compare-word-left-sys_title");
      const newer = page.getByTestId("editor-compare-word-right-sys_title");
      await expect(older).toContainText("The old title");
      await expect(older.locator("del")).toContainText("old");
      await expect(older.locator("ins")).toHaveCount(0);
      await expect(newer).toContainText("The new title");
      await expect(newer.locator("ins")).toContainText("new");
      await expect(newer.locator("del")).toHaveCount(0);
      await expect(page.getByTestId("editor-compare-results")).toHaveAttribute(
        "aria-live",
        "polite",
      );
      const toggle = page.getByTestId("editor-compare-word-diff");
      await expect(toggle).toBeChecked();
      await toggle.focus();
      await expect(toggle).toBeFocused();
      await page.keyboard.press("Space");
      await expect(toggle).not.toBeChecked();
      await expect(older).toHaveCount(0);
      await expect(
        page.getByTestId("editor-compare-row-sys_title"),
      ).toContainText("The old title");
      await toggle.check();
      await expect(older.locator("del")).toContainText("old");
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      await expectNoSeriousA11yViolations(page);
    },
  );
});
