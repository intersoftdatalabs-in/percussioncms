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
 * React Content Editor host — create new content item (#4646).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @create}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-create-item.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemCreateUrl,
} = require("./helpers/editor-host-create-item");

const TYPES = {
  ContentTypeList: {
    ContentType: [{ name: "percImageAsset", label: "Image" }],
  },
};

const CREATED = {
  ItemCreateResult: {
    itemId: "77",
    folderPath: "/Assets",
    name: "Shot",
    contentType: "percImageAsset",
  },
};

const FIELDS = {
  ItemEditorFields: {
    contentId: "77",
    contentType: "percImageAsset",
    name: "Shot",
    checkoutUser: "admin",
    revision: 1,
    fields: [{ name: "sys_title", value: "Shot" }],
  },
};

const TYPE_DETAIL = {
  ContentTypeDetail: {
    name: "percImageAsset",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox", required: true },
    ],
  },
};

async function stubApis(page, { createStatus, createBody, onCreate } = {}) {
  await page.route("**/services/contenttypes", (route) => {
    if (route.request().method() === "GET") {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(TYPES),
      });
    }
    return route.continue();
  });
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE_DETAIL),
    }),
  );
  await page.route("**/services/itemmanagement/item/create**", (route) => {
    if (onCreate) {
      onCreate(route.request());
    }
    return route.fulfill({
      status: createStatus ?? 200,
      contentType: "application/json",
      body:
        typeof createBody === "string"
          ? createBody
          : JSON.stringify(createBody ?? CREATED),
    });
  });
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ItemStateTransition: { transitionTriggers: [] } }),
    }),
  );
}

test.describe("EditorHost create item @explorer-content-editor @editor @create", () => {
  test("creates selected type in folder and opens the new item", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => consoleErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });
    let createHit = false;
    await loginAsAdmin(page);
    await stubApis(page, {
      onCreate: (req) => {
        createHit = isItemCreateUrl(req.url());
      },
    });
    await page.goto(editorSpaUrl(BASE_URL));
    await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
    await page.getByTestId(TEST_IDS.type).selectOption("percImageAsset");
    await page.getByTestId(TEST_IDS.folder).fill("/Assets");
    await page.getByTestId(TEST_IDS.submit).click();
    await expect(page.getByTestId(TEST_IDS.contentId)).toContainText("77");
    expect(createHit).toBe(true);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
    await expectNoSeriousA11yViolations(page);
  });

  test("403 create is not success", async ({ page }) => {
    await loginAsAdmin(page);
    await stubApis(page, {
      createStatus: 403,
      createBody: { Error: { message: "FORBIDDEN" } },
    });
    await page.goto(editorSpaUrl(BASE_URL));
    await page.getByTestId(TEST_IDS.type).selectOption("percImageAsset");
    await page.getByTestId(TEST_IDS.folder).fill("/Assets");
    await page.getByTestId(TEST_IDS.submit).click();
    await expect(page.getByTestId(TEST_IDS.error)).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.contentId)).toHaveCount(0);
  });
});
