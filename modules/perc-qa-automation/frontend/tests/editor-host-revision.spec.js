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
 * EditorHost current revision badge (#4964 / #4532).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run: {@code npm run test:surface -- --path tests/editor-host-revision.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { TEST_IDS, editorSpaUrl } = require("./helpers/editor-host-checkout");

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

function fieldsBody(revision) {
  const body = {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: "Home" }],
    },
  };
  if (revision != null) {
    body.ItemEditorFields.revision = revision;
  }
  return body;
}

function trackErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (403|404|409)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

async function stubCommon(page) {
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: "admin",
          currentUser: "admin",
          assignmentType: "Assignee",
        },
      }),
    }),
  );
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: { id: "42", name: "Home", path: "//Folders/Lab/Home", type: "page" },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ choices: [] }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: { itemId: "42", stateName: "Draft", transitionTriggers: [] },
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
}

test.describe("EditorHost current revision", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "shows the loaded revision and updates it after save and check-in",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      let gets = 0;
      await page.addInitScript(() => {
        window.close = () => undefined;
      });
      await stubCommon(page);
      await page.route("**/services/itemmanagement/item/fields/**", (route) => {
        const method = route.request().method();
        if (method === "PUT") {
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(fieldsBody(5)),
          });
        }
        gets += 1;
        const revision = gets === 1 ? 4 : 8;
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(fieldsBody(revision)),
        });
      });
      await page.route("**/rest/editor/items/**/checkin**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            EditorItemLockInfo: {
              itemName: "Home",
              checkOutUser: "",
              currentUser: "admin",
              assignmentType: "Assignee",
            },
          }),
        }),
      );
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await expect(page.getByTestId("editor-revision")).toContainText("Revision 4");
      await page.getByTestId(TEST_IDS.save).click();
      await expect(page.getByTestId("editor-revision")).toContainText("Revision 5");
      await page.getByTestId(TEST_IDS.checkin).click();
      await page.getByTestId(TEST_IDS.checkinConfirm).click();
      await expect(page.getByTestId("editor-revision")).toContainText("Revision 8");
      expect(pageErrors).toEqual([]);
    },
  );

  test(
    "hides the revision badge when load is 404 or the payload omits revision",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      await stubCommon(page);
      await page.route("**/services/itemmanagement/item/fields/**", (route) =>
        route.fulfill({
          status: 404,
          contentType: "application/json",
          body: JSON.stringify({ message: "missing" }),
        }),
      );
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.getByTestId("editor-error")).toBeVisible();
      await expect(page.getByTestId("editor-revision")).toHaveCount(0);

      await page.unroute("**/services/itemmanagement/item/fields/**");
      await page.route("**/services/itemmanagement/item/fields/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(fieldsBody(null)),
        }),
      );
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await expect(page.getByTestId("editor-revision")).toHaveCount(0);
      expect(pageErrors).toEqual([]);
    },
  );
});
