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
 * EditorHost bulk revert of unsaved field edits (#5889 / #4532).
 *
 * <p>Editing a field shows the Revert control with a per-field dirty marker;
 * Revert restores the loaded values client-side (no fields PUT) and hides
 * itself; a later edit still saves.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-revert-unsaved.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { editorSpaUrl } = require("./helpers/editor-host-workflow");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    revision: 1,
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

function consoleOn(page, pageErrors) {
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      pageErrors.push(msg.text());
    }
  });
}

async function stubEditor(page, puts) {
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    const method = route.request().method();
    if (method === "PUT" || method === "POST") {
      puts.push(route.request().url());
      const saved = {
        ItemEditorFields: {
          ...FIELDS.ItemEditorFields,
          fields: [{ name: "sys_title", value: "Draft title" }],
        },
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(saved),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    });
  });
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: { checkOutUser: "admin", currentUser: "admin" },
      }),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ContentTypeDetail: {
          name: "percPage",
          fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
        },
      }),
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
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ choices: [] }),
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
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, currentLocale: "en-us", variants: [] }),
    }),
  );
  await page.route("**/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ SlotCanvas: { ownerId: 42, slots: [] } }),
    }),
  );
  await page.route("**/itemmanagement/item/siteimpact/page/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ pages: [], templates: [] }),
    }),
  );
  await page.route("**/rest/content-explorer/relationships/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ items: [] }),
    }),
  );
}

test.describe("EditorHost bulk revert unsaved edits (#5889)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "revert restores loaded values with no PUT, then a later edit still saves",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      const puts = [];
      consoleOn(page, pageErrors);
      await stubEditor(page, puts);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const title = page.getByTestId("editor-field-sys_title");
      await expect(title).toBeVisible();
      await expect(page.getByTestId("editor-revert")).toHaveCount(0);

      await title.fill("Draft title");
      await expect(page.getByTestId("editor-revert")).toBeVisible();
      await expect(page.getByTestId("editor-field-row-sys_title")).toHaveAttribute(
        "data-editor-dirty",
        "true",
      );

      await page.getByTestId("editor-revert").click();
      await expect(title).toHaveValue("Home");
      await expect(page.getByTestId("editor-revert")).toHaveCount(0);
      await expect(page.getByTestId("editor-field-row-sys_title")).not.toHaveAttribute(
        "data-editor-dirty",
        "true",
      );
      expect(puts).toEqual([]);

      await title.fill("Draft title");
      await page.getByTestId("editor-save").click();
      await expect(page.getByTestId("editor-saved")).toBeVisible();
      expect(puts.length).toBeGreaterThan(0);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );
});
