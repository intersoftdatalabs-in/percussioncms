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
 * React Content Editor numeric field save (#4752).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @numeric}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-numeric-fields.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
} = require("./helpers/editor-host-numeric-fields");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    revision: 2,
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "qty", value: "1" },
    ],
  },
};

const SAVED = {
  ItemEditorFields: {
    ...FIELDS.ItemEditorFields,
    revision: 3,
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "qty", value: "7" },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox", dataType: "text" },
      {
        name: "qty",
        label: "Quantity",
        control: "sys_Number",
        dataType: "integer",
        controlProperties: [
          { name: "minimum", value: "0" },
          { name: "maximum", value: "10" },
        ],
      },
    ],
  },
};

async function stubEditorApis(page, { putStatus, onPut } = {}) {
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
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
        onPut(route.request());
      }
      await route.fulfill({
        status: putStatus ?? 200,
        contentType: "application/json",
        body: JSON.stringify(
          putStatus === 400
            ? { Error: { message: 'Field "qty" is not a valid number.' } }
            : SAVED,
        ),
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

function watchPageErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (/Failed to load resource/.test(text)) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("React Content Editor numeric field save", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "saves an in-range number",
    { tag: ["@explorer-content-editor", "@editor", "@numeric"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchPageErrors(page);
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const field = page.locator(`[data-testid="${TEST_IDS.fieldQty}"]`);
      await expect(field).toHaveAttribute("data-editor-kind", "number");
      await field.fill("7");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const qty = (payload.fields || []).find((f) => f.name === "qty");
      expect(qty && qty.value).toBe("7");
      expect(qty && qty.dataType).toBe("integer");
      expect(qty && qty.minimum).toBe("0");
      expect(qty && qty.maximum).toBe("10");
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "non-numeric and out-of-range values are not saved",
    { tag: ["@explorer-content-editor", "@editor", "@numeric"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchPageErrors(page);
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const field = page.locator(`[data-testid="${TEST_IDS.fieldQty}"]`);
      await field.fill("abc");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldErrorQty}"]`)).toBeVisible();
      await field.fill("11");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldErrorQty}"]`)).toContainText(
        /range/i,
      );
      expect(puts).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "save HTTP 400 maps onto the number field",
    { tag: ["@explorer-content-editor", "@editor", "@numeric"] },
    async ({ page }) => {
      const pageErrors = watchPageErrors(page);
      await stubEditorApis(page, { putStatus: 400 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.fieldQty}"]`).fill("4");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldErrorQty}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
      await expect(page.getByText(/^Saved$/)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "view mode number field is read-only",
    { tag: ["@explorer-content-editor", "@editor", "@numeric"] },
    async ({ page }) => {
      await stubEditorApis(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const viewField = page.locator(`[data-testid="${TEST_IDS.fieldQty}"]`);
      await expect(viewField).toHaveAttribute("readonly", "");
      await expect(page.locator(`[data-testid="${TEST_IDS.save}"]`)).toHaveCount(0);
    },
  );
});
