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
 * React Content Editor host — field-level restore from a revision compare (#6080).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-compare-field-restore.spec.js}</p>
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
      /Failed to load resource: the server responded with a status of (403|404)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
}

async function stubEditor(page) {
  // Registered first so later editor stubs win. Keeps fake id 42 off the
  // QA server and forces lock/ancillary lookups (checkout-owner, site
  // impact, translations) to empty instead of real cell state, so the
  // host treats the stub checkout as self-held and editable.
  await page.route("**/services/**", (route) =>
    route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/**", (route) =>
    route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
  );
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
      body: JSON.stringify(REVISIONS),
    }),
  );
  await page.route("**/services/itemmanagement/item/compare/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(COMPARE),
    }),
  );
}

test.describe("React Content Editor field-level restore from compare", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "copies an older compare value into the field without calling restore",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      let restoreHits = 0;
      await page.route("**/services/itemmanagement/item/restoreRevision/**", (route) => {
        restoreHits += 1;
        return route.fulfill({ status: 500, contentType: "application/json", body: "{}" });
      });
      let fieldSavePuts = 0;
      page.on("request", (request) => {
        if (
          request.method() === "PUT" &&
          /\/services\/itemmanagement\/item\/fields\//.test(request.url())
        ) {
          fieldSavePuts += 1;
        }
      });
      await stubEditor(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-form")).toBeVisible();
      await page.getByTestId("editor-restore-toggle").click();
      await expect(page.getByTestId("editor-compare-left")).toBeVisible();
      await page.getByTestId("editor-compare-run").click();
      await expect(
        page.getByTestId("editor-compare-row-sys_title"),
      ).toContainText("Changed");
      await page.getByTestId("editor-compare-use-left-sys_title").click();
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        "Old title",
      );
      await expect(
        page.getByTestId("editor-compare-field-restored"),
      ).toContainText("sys_title");
      expect(restoreHits).toBe(0);
      expect(fieldSavePuts).toBe(0);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      await expectNoSeriousA11yViolations(page);
    },
  );

  test(
    "unchanged rows offer no field restore buttons",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditor(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-form")).toBeVisible();
      await page.getByTestId("editor-restore-toggle").click();
      await expect(page.getByTestId("editor-compare-left")).toBeVisible();
      await page.getByTestId("editor-compare-run").click();
      await expect(
        page.getByTestId("editor-compare-row-displaytitle"),
      ).toContainText("Unchanged");
      await expect(
        page.getByTestId("editor-compare-use-left-displaytitle"),
      ).toHaveCount(0);
      await expect(
        page.getByTestId("editor-compare-use-right-displaytitle"),
      ).toHaveCount(0);
      await expect(
        page.getByTestId("editor-compare-use-right-sys_title"),
      ).toBeVisible();
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );
});
