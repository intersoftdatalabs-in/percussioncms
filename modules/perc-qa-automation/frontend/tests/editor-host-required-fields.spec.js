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
 * React Content Editor host — required / invalid field errors on save (#4541).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @validation}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-required-fields.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
  isWorkflowCheckinUrl,
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

const FILLED_FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "displaytitle", value: "Welcome" },
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

async function stubEditorApis(page, { onPut, putStatus, putBody } = {}) {
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/checkIn/**", (route) =>
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
          transitionTriggers: ["Submit"],
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
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
      if (typeof onPut === "function") {
        onPut(route.request().url());
      }
      await route.fulfill({
        status: putStatus ?? 200,
        contentType: "application/json",
        body: JSON.stringify(
          putBody ?? {
            Error: {
              message: "displaytitle is invalid",
              errorData: "displaytitle",
            },
          },
        ),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(putStatus === 400 ? FILLED_FIELDS : FIELDS),
    });
  });
}

test.describe("React Content Editor required field save errors", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "empty required field shows inline error on save and does not PUT",
    { tag: ["@explorer-content-editor", "@editor", "@validation"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = [];
      const leftover = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("checkoutedit.xml") ||
          u.includes("contenteditorurls.html") ||
          /view=editor/.test(u)
        ) {
          leftover.push(u);
        }
      });
      await stubEditorApis(page, { onPut: (u) => puts.push(u), putBody: FIELDS });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.fieldErrorDisplayTitle}"]`),
      ).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(
        /required fields before saving/i,
      );
      expect(puts.filter((u) => isItemFieldsPutUrl(u))).toEqual([]);
      expect(leftover, `leftover CE requested: ${leftover.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "save HTTP 400 maps onto the named field",
    { tag: ["@explorer-content-editor", "@editor", "@validation"] },
    async ({ page }) => {
      const puts = [];
      await stubEditorApis(page, {
        onPut: (u) => puts.push(u),
        putStatus: 400,
        putBody: {
          Error: {
            message: "displaytitle is invalid",
            errorData: "displaytitle",
          },
        },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.fieldDisplayTitle}"]`).fill("Welcome");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBe(1);
      expect(isItemFieldsPutUrl(puts[0])).toBe(true);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.fieldErrorDisplayTitle}"]`),
      ).toContainText(/displaytitle/i);
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible();
    },
  );

  test(
    "check-in is blocked while a required field is empty",
    { tag: ["@explorer-content-editor", "@editor", "@validation"] },
    async ({ page }) => {
      const checkins = [];
      page.on("request", (req) => {
        if (req.method() !== "GET") {
          return;
        }
        if (isWorkflowCheckinUrl(req.url())) {
          checkins.push(req.url());
        }
      });
      await stubEditorApis(page, { putBody: FIELDS });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.checkin}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.checkin}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.fieldErrorDisplayTitle}"]`),
      ).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(
        /required fields before checking in/i,
      );
      expect(checkins).toEqual([]);
    },
  );
});
