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
 * React Content Editor long-text field save (#4726).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @longtext}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-longtext-fields.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
} = require("./helpers/editor-host-longtext-fields");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    revision: 2,
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "description", value: "line one" },
    ],
  },
};

const SAVED = {
  ItemEditorFields: {
    ...FIELDS.ItemEditorFields,
    revision: 3,
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "description", value: "line one\nline two" },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox", dataType: "text" },
      {
        name: "description",
        label: "Description",
        control: "sys_EditBox",
        dataType: "maxtext",
      },
    ],
  },
};

async function stubEditorApis(page, { putStatus, putBody, onPut } = {}) {
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
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        itemId: 42,
        locale: "en-us",
        variants: [{ contentId: 42, locale: "en-us", role: "source" }],
      }),
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
          putBody ??
            (putStatus === 400 ? { Error: { message: "Long text rejected" } } : SAVED),
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

test.describe("React Content Editor long-text field save", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "saves multiline long text, maps 400, view is read-only",
    { tag: ["@explorer-content-editor", "@editor", "@longtext"] },
    async ({ page }) => {
      const puts = [];
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
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible();
      const field = page.locator(`[data-testid="${TEST_IDS.fieldDescription}"]`);
      await expect(field).toHaveAttribute("data-editor-kind", "longtext");
      await field.fill("line one\nline two");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const description = (payload.fields || []).find((f) => f.name === "description");
      expect(description && description.value).toBe("line one\nline two");
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );

  test(
    "save HTTP 400 maps onto the long-text field",
    { tag: ["@explorer-content-editor", "@editor", "@longtext"] },
    async ({ page }) => {
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
      await stubEditorApis(page, { putStatus: 400 });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.fieldDescription}"]`).fill("updated\nbody");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.fieldErrorDescription}"]`),
      ).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
      await expect(page.getByText(/^Saved$/)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "view mode long text is read-only",
    { tag: ["@explorer-content-editor", "@editor", "@longtext"] },
    async ({ page }) => {
      await stubEditorApis(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const viewField = page.locator(`[data-testid="${TEST_IDS.fieldDescription}"]`);
      await expect(viewField).toHaveAttribute("readonly", "");
      await expect(page.locator('[data-testid="editor-longtext-clear-description"]')).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.save}"]`)).toHaveCount(0);
    },
  );

  test(
    "clears optional long text, cancel does not save, required and 400/403/409 are not success",
    { tag: ["@explorer-content-editor", "@editor", "@longtext"] },
    async ({ page }) => {
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
      let description = "line one";
      let required = false;
      let failStatus = 0;
      const fieldPuts = [];
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
            ItemStateTransition: { itemId: "42", stateName: "Draft", transitionTriggers: [] },
          }),
        }),
      );
      await page.route("**/rest/content-explorer/translations/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            itemId: 42,
            locale: "en-us",
            variants: [{ contentId: 42, locale: "en-us", role: "source" }],
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
              fields: [
                { name: "sys_title", label: "Title", control: "sys_EditBox", dataType: "text" },
                {
                  name: "description",
                  label: "Description",
                  control: "sys_TextArea",
                  required,
                },
              ],
            },
          }),
        }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          fieldPuts.push(route.request().postData() || "");
          if (failStatus === 400 || failStatus === 403 || failStatus === 409) {
            await route.fulfill({
              status: failStatus,
              contentType: "application/json",
              body: JSON.stringify({ Error: { message: "rejected" } }),
            });
            return;
          }
          const body = JSON.parse(route.request().postData() || "{}");
          const payload = body.ItemEditorFields || body;
          const next = (payload.fields || []).find((f) => f.name === "description");
          description = next ? next.value : description;
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              ItemEditorFields: {
                contentId: "42",
                contentType: "percPage",
                name: "Home",
                checkoutUser: "admin",
                revision: 3,
                fields: payload.fields,
              },
            }),
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorFields: {
              contentId: "42",
              contentType: "percPage",
              name: "Home",
              checkoutUser: "admin",
              revision: 2,
              fields: [
                { name: "sys_title", value: "Home" },
                { name: "description", value: description },
              ],
            },
          }),
        });
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-longtext-clear-description"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-longtext-clear-description"]').click();
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldDescription}"]`)).toHaveValue(
        "line one",
        { timeout: 20_000 },
      );
      await page.locator('[data-testid="editor-longtext-clear-description"]').click();
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"description"\s*,\s*"value"\s*:\s*""/);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldDescription}"]`)).toHaveValue("");
      await expect(page.locator('[data-testid="editor-longtext-clear-description"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldDescription}"]`)).toHaveValue("", {
        timeout: 20_000,
      });

      description = "line one";
      required = true;
      fieldPuts.length = 0;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-longtext-clear-description"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-longtext-clear-description"]').click();
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldErrorDescription}"]`)).toBeVisible();
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      required = false;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-longtext-clear-description"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-longtext-clear-description"]').click();
      const putsBeforeErrors = fieldPuts.length;
      failStatus = 400;
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(putsBeforeErrors);
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      failStatus = 403;
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(putsBeforeErrors + 1);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      failStatus = 409;
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(/revision/i);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
