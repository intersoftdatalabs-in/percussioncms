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
 * React Content Editor link field save (#4753).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @link}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-link-fields.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
} = require("./helpers/editor-host-link-fields");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    revision: 2,
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "page", value: "" },
    ],
  },
};

function savedWith(value) {
  return {
    ItemEditorFields: {
      ...FIELDS.ItemEditorFields,
      revision: 3,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "page", value },
      ],
    },
  };
}

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox", dataType: "text" },
      {
        name: "page",
        label: "Page link",
        control: "sys_PageLink",
        dataType: "text",
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
      const status = putStatus ?? 200;
      let body = savedWith("594");
      if (status === 400) {
        body = { Error: { message: "Field \"page\" is not a valid page or managed link." } };
      } else if (status === 404) {
        body = { Error: { message: "Field \"page\" target was not found." } };
      } else if (status === 403) {
        body = { Error: { message: "You are not allowed to use the target of field \"page\"." } };
      } else {
        try {
          const posted = JSON.parse(route.request().postData() || "{}");
          const payload = posted.ItemEditorFields || posted;
          const pageField = (payload.fields || []).find((field) => field.name === "page");
          body = savedWith(pageField ? pageField.value : "");
        } catch {
          body = savedWith("594");
        }
      }
      await route.fulfill({
        status,
        contentType: "application/json",
        body: JSON.stringify(body),
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

test.describe("React Content Editor link field save", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "saves a content id and clears the link",
    { tag: ["@explorer-content-editor", "@editor", "@link"] },
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
      const field = page.locator(`[data-testid="${TEST_IDS.fieldPage}"]`);
      await expect(field).toHaveAttribute("data-editor-kind", "link");
      await field.fill("594");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const pageField = (payload.fields || []).find((f) => f.name === "page");
      expect(pageField && pageField.value).toBe("594");
      expect(pageField && pageField.dataType).toBe("link");
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.clearPage}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(1);
      const clearedReq = puts.filter((r) => isItemFieldsPutUrl(r.url())).at(-1);
      const clearedBody = JSON.parse(clearedReq.postData() || "{}");
      const clearedPayload = clearedBody.ItemEditorFields || clearedBody;
      const cleared = (clearedPayload.fields || []).find((f) => f.name === "page");
      expect(cleared && cleared.value).toBe("");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "an invalid link shape is not saved",
    { tag: ["@explorer-content-editor", "@editor", "@link"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchPageErrors(page);
      await stubEditorApis(page, { onPut: (req) => puts.push(req) });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.fieldPage}"]`).fill("javascript:alert(1)");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldErrorPage}"]`)).toBeVisible();
      expect(puts).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "save HTTP 404 and 403 map onto the link field",
    { tag: ["@explorer-content-editor", "@editor", "@link"] },
    async ({ page }) => {
      const pageErrors = watchPageErrors(page);
      await stubEditorApis(page, { putStatus: 404 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.fieldPage}"]`).fill("999");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldErrorPage}"]`)).toContainText(
        /not found/i,
      );
      await expect(page.getByText(/^Saved$/)).toHaveCount(0);

      await page.unroute("**/services/itemmanagement/item/fields/**");
      await stubEditorApis(page, { putStatus: 403 });
      await page.locator(`[data-testid="${TEST_IDS.fieldPage}"]`).fill("//Sites/Hidden/index");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(
        /not allowed/i,
      );
      await expect(page.getByText(/^Saved$/)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "view mode link field is read-only",
    { tag: ["@explorer-content-editor", "@editor", "@link"] },
    async ({ page }) => {
      await stubEditorApis(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const viewField = page.locator(`[data-testid="${TEST_IDS.fieldPage}"]`);
      await expect(viewField).toHaveAttribute("readonly", "");
      await expect(page.locator(`[data-testid="${TEST_IDS.clearPage}"]`)).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.save}"]`)).toHaveCount(0);
    },
  );
});
