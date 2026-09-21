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
 * React Content Editor host — restore prior revision (#4604).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-restore-revision.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isRevisionsUrl,
  isRestoreRevisionUrl,
} = require("./helpers/editor-host-restore-revision");

const ITEM_FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const REVISIONS = {
  RevisionsSummary: {
    restorable: true,
    Revision: [
      {
        revId: 5,
        status: "Quick Edit",
        lastModifier: "admin",
        lastModifiedDate: "2026-04-12",
      },
      {
        revId: 4,
        status: "Live",
        lastModifier: "admin",
        lastModifiedDate: "2026-04-10",
      },
    ],
    Comment: [
      {
        comment: "Published as v3",
        commenter: "admin",
        commentType: "Workflow",
        commentDate: "2026-04-10",
      },
    ],
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

async function stubCommonApis(page) {
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
}

async function stubEditorApisWithRevisions(
  page,
  {
    revisionsStatus = 200,
    revisionsBody = REVISIONS,
    restoreStatus = 200,
    restoreBody = { NoContent: { operation: "restored" } },
    refreshFieldsBody = {
      ItemEditorFields: {
        ...ITEM_FIELDS.ItemEditorFields,
        name: "Home (restored)",
        fields: [{ name: "sys_title", value: "Home (restored)" }],
      },
    },
  } = {},
) {
  await stubCommonApis(page);
  await page.route("**/services/itemmanagement/item/revisions/**", (route) =>
    route.fulfill({
      status: revisionsStatus,
      contentType: "application/json",
      body: JSON.stringify(revisionsBody),
    }),
  );
  await page.route("**/services/itemmanagement/item/restoreRevision/**", (route) =>
    route.fulfill({
      status: restoreStatus,
      contentType: "application/json",
      body: restoreStatus >= 400
        ? JSON.stringify({ message: "server failure" })
        : JSON.stringify(restoreBody),
    }),
  );
  await page.unroute("**/services/itemmanagement/item/fields/**");
  let fieldsCount = 0;
  await page.route("**/services/itemmanagement/item/fields/**", (route) => {
    fieldsCount += 1;
    const body = fieldsCount === 1 ? ITEM_FIELDS : refreshFieldsBody;
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
}

test.describe("React Content Editor restore prior revision", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "confirm + restore refreshes the open editor fields",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApisWithRevisions(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.toggle)).toBeVisible();
      await page.getByTestId(TEST_IDS.toggle).click();
      await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.select)).toBeVisible();
      await page.getByTestId(TEST_IDS.select).selectOption("5");
      page.once("dialog", (d) => d.accept());
      await page.getByTestId(TEST_IDS.confirm).click();
      await expect(page.getByTestId(TEST_IDS.done)).toBeVisible();
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        "Home (restored)",
      );
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      await expectNoSeriousA11yViolations(page);
    },
  );

  test(
    "403 on restore is not silent success and shows the forbidden copy",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApisWithRevisions(page, { restoreStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await page.getByTestId(TEST_IDS.toggle).click();
      await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
      await page.getByTestId(TEST_IDS.select).selectOption("5");
      page.once("dialog", (d) => d.accept());
      await page.getByTestId(TEST_IDS.confirm).click();
      await expect(page.getByTestId(TEST_IDS.error)).toContainText(
        /not allowed to restore/i,
      );
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        "Home",
      );
    },
  );

  test(
    "404 on restore is not silent success and shows the not-found copy",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApisWithRevisions(page, { restoreStatus: 404 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await page.getByTestId(TEST_IDS.toggle).click();
      await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
      await page.getByTestId(TEST_IDS.select).selectOption("5");
      page.once("dialog", (d) => d.accept());
      await page.getByTestId(TEST_IDS.confirm).click();
      await expect(page.getByTestId(TEST_IDS.error)).toContainText(
        /was not found/i,
      );
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "empty revisions list shows the empty-state copy and disables the confirm",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApisWithRevisions(page, {
        revisionsBody: { RevisionsSummary: { restorable: false, Revision: [], Comment: [] } },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await page.getByTestId(TEST_IDS.toggle).click();
      await expect(page.getByTestId(TEST_IDS.empty)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.confirm)).toBeDisabled();
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "revisions load error surfaces the load-error message",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApisWithRevisions(page, {
        revisionsStatus: 500,
        revisionsBody: { message: "down" },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await page.getByTestId(TEST_IDS.toggle).click();
      await expect(page.getByTestId(TEST_IDS.loadError)).toBeVisible();
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );
});

test("isRevisionsUrl + isRestoreRevisionUrl regex helpers accept CMS URLs", () => {
  expect(isRevisionsUrl("/Rhythmyx/services/itemmanagement/item/revisions/42")).toBe(true);
  expect(isRevisionsUrl("/Rhythmyx/services/itemmanagement/item/fields/42")).toBe(false);
  expect(
    isRestoreRevisionUrl(
      "/Rhythmyx/services/itemmanagement/item/restoreRevision/5-101-42",
    ),
  ).toBe(true);
});
