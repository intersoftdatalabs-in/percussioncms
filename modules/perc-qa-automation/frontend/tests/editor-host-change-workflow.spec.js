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
 * EditorHost change the item workflow (#4861).
 *
 * Run: npm run test:surface -- --path tests/editor-host-change-workflow.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin } = require("./helpers/auth");

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

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

async function stubEditor(page, { changeStatus = 200 } = {}) {
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
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName: "Draft",
          workflowId: "4",
          transitionTriggers: ["Submit"],
        },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemWorkflowChoices: {
          itemId: "42",
          currentWorkflowId: "4",
          choices: [
            { id: "4", name: "Local" },
            { id: "7", name: "Review" },
          ],
        },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/changeWorkflow/**", (route) => {
    if (changeStatus !== 200) {
      return route.fulfill({
        status: changeStatus,
        contentType: "application/json",
        body: JSON.stringify({ message: "forbidden" }),
      });
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName: "Pending",
          workflowId: "7",
          transitionTriggers: ["Approve"],
        },
      }),
    });
  });
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
      body: JSON.stringify(FIELDS),
    }),
  );
}

test.describe("EditorHost change workflow", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "saves a new workflow and rejects a forbidden id",
    { tag: ["@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await stubEditor(page);
      await page.goto("/Rhythmyx/cm/app/spa.jsp?entry=editor&contentId=42&mode=edit");
      await expect(page.getByTestId("editor-workflow-picker")).toBeVisible();
      await page.getByTestId("editor-workflow-picker").selectOption("7");
      await page.getByTestId("editor-workflow-save").click();
      await expect(page.getByTestId("editor-workflow-changed")).toBeVisible();
      await expect(page.getByTestId("editor-workflow-state")).toContainText("Pending");
      await expect(page.getByTestId("editor-workflow-trigger-Approve")).toBeVisible();
      expect(pageErrors).toEqual([]);
    },
  );

  test(
    "forbidden workflow is an error",
    { tag: ["@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await stubEditor(page, { changeStatus: 403 });
      await page.goto("/Rhythmyx/cm/app/spa.jsp?entry=editor&contentId=42&mode=edit");
      await expect(page.getByTestId("editor-workflow-picker")).toBeVisible();
      await page.getByTestId("editor-workflow-picker").selectOption("7");
      await page.getByTestId("editor-workflow-save").click();
      await expect(page.getByTestId("editor-workflow-error")).toBeVisible();
      await expect(page.getByTestId("editor-workflow-changed")).toHaveCount(0);
      expect(pageErrors).toEqual([]);
    },
  );
});
