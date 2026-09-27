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
 * React Content Editor — open the item folder in Explorer (#4941).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-open-folder.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { editorSpaUrl } = require("./helpers/editor-host-workflow");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "",
    fields: [{ name: "sys_title", value: "Home" }],
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
      /Failed to load resource: the server responded with a status of (403|404|409)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
}

async function stubEditor(page, pathStatus, pathBody) {
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
      body: JSON.stringify({
        ContentTypeDetail: { name: "percPage", fields: [] },
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
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: pathStatus,
      contentType: "application/json",
      body: JSON.stringify(pathBody),
    }),
  );
  await page.route("**/pathmanagement/path/paginatedFolder**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PagedItemList: { childrenInPage: [], childrenCount: 0, startIndex: 0 },
      }),
    }),
  );
}

test.describe("React Content Editor open folder (#4941)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "Open folder lands on the explorer route for that folder",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditor(page, 200, {
        PathItem: {
          id: "42",
          name: "Home",
          path: "//Folders/Lab/Home",
          type: "page",
        },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.getByTestId("editor-open-folder")).toBeVisible();
      await page.getByTestId("editor-open-folder").click();
      await expect(page).toHaveURL(/\/explorer\?path=%2FFolders%2FLab/);
      await expect(page.getByTestId("editor-open-folder-error")).toHaveCount(0);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "missing folder and 403 or 404 stay on the editor",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditor(page, 200, {
        PathItem: { id: "42", name: "Home", path: "/", type: "page" },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await page.getByTestId("editor-open-folder").click();
      await expect(page.getByTestId("editor-open-folder-error")).toContainText(/not in a folder/i);
      await expect(page).toHaveURL(/\/editor\?contentId=42/);
      await expect(page).not.toHaveURL(/\/explorer/);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);

      await page.unroute("**/pathmanagement/path/item/id/**");
      await page.route("**/pathmanagement/path/item/id/**", (route) =>
        route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "forbidden" }),
        }),
      );
      await page.getByTestId("editor-open-folder").click();
      await expect(page.getByTestId("editor-open-folder-error")).toContainText(/not allowed/i);
      await expect(page).toHaveURL(/\/editor\?contentId=42/);
      await expect(page).not.toHaveURL(/\/explorer/);

      await page.unroute("**/pathmanagement/path/item/id/**");
      await page.route("**/pathmanagement/path/item/id/**", (route) =>
        route.fulfill({
          status: 404,
          contentType: "application/json",
          body: JSON.stringify({ message: "missing" }),
        }),
      );
      await page.getByTestId("editor-open-folder").click();
      await expect(page.getByTestId("editor-open-folder-error")).toContainText(/not found/i);
      await expect(page).toHaveURL(/\/editor\?contentId=42/);
      await expect(page).not.toHaveURL(/\/explorer/);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );
});
