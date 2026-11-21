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
 * Invalid editor controls name their inline error text through
 * {@code aria-describedby} (#6933).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @validation}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-field-error-describedby.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
} = require("./helpers/editor-host-required-fields");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "displaytitle", value: "" },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox", required: true },
      {
        name: "displaytitle",
        label: "Display title",
        control: "sys_EditBox",
        required: true,
      },
    ],
  },
};

async function stubEditorApis(page, { onPut } = {}) {
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/checkIn/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkin", (route) =>
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
  const emptyJson = { status: 200, contentType: "application/json", body: "{}" };
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill(emptyJson),
  );
  await page.route("**/services/pathmanagement/path/item/id/**", (route) =>
    route.fulfill(emptyJson),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill(emptyJson),
  );
  await page.route("**/services/assembly/slot-relationships/**", (route) =>
    route.fulfill(emptyJson),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
      if (typeof onPut === "function") {
        onPut(route.request().url());
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(FIELDS),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    });
  });
}

test.describe("React Content Editor field error description", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "blocked save associates the invalid control with its inline error",
    { tag: ["@explorer-content-editor", "@editor", "@validation"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      await stubEditorApis(page, { onPut: (u) => puts.push(u) });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const field = page.locator(`[data-testid="${TEST_IDS.fieldDisplayTitle}"]`);
      const error = page.locator(
        `[data-testid="${TEST_IDS.fieldErrorDisplayTitle}"]`,
      );
      await expect(field).toBeVisible({ timeout: 20_000 });
      // No error yet: the control carries no dangling description.
      await expect(field).not.toHaveAttribute(
        "aria-describedby",
        "editor-field-error-displaytitle",
      );
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(error).toBeVisible();
      // The invalid control names the visible inline error text.
      await expect(field).toHaveAttribute(
        "aria-describedby",
        "editor-field-error-displaytitle",
      );
      const describedId = await field.getAttribute("aria-describedby");
      const describedText = await page
        .locator(`#${describedId}`)
        .textContent();
      const errorText = await error.textContent();
      expect(describedText).toBe(errorText);
      expect(describedText).toMatch(/required/i);
      expect(puts.filter((u) => isItemFieldsPutUrl(u))).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
      await page.screenshot({
        path: "test-results/live-proof-6933-field-error-describedby.png",
      });
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
