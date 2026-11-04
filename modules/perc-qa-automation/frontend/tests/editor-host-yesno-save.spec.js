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
 * React Content Editor save of one yes-no field (#5586 / #4532).
 *
 * <p>Peer: {@code tests/editor-host-text-save.spec.js} /
 * {@code tests/editor-host-yesno-nul.spec.js}. Yes-no NUL stays in the NUL
 * spec. Required-blank yes-no stays in slice 104.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @yesno}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-yesno-save.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-link-fields");

const PREVIOUS = "yes";
const NEXT = "no";
const SUMMARY = "Hello";

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      {
        name: "no_propagate",
        label: "Propagate",
        control: "sys_SingleCheckBox",
        dataType: "boolean",
      },
      {
        name: "summary",
        label: "Summary",
        control: "sys_EditBox",
        dataType: "text",
      },
    ],
  },
};

function fieldsPayload(flag, summary) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 2,
      fields: [
        { name: "no_propagate", value: flag },
        { name: "summary", value: summary },
      ],
    },
  };
}

test.describe("React Content Editor save yes-no", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "saves one yes-no field with boolean dataType; cancel, view, and HTTP 400/403/409 do not succeed",
    { tag: ["@explorer-content-editor", "@editor", "@yesno"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let flag = PREVIOUS;
      let summary = SUMMARY;
      let rejectNext = 0;
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
      await page.route("**/sys_resources/**", (route) =>
        route.fulfill({ status: 404, body: "" }),
      );
      await page.route("**/services/**", (route) =>
        route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
      );
      await page.route("**/rest/**", (route) =>
        route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
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
          body: JSON.stringify({
            ItemStateTransition: {
              itemId: "42",
              stateName: "Draft",
              transitionTriggers: [],
            },
          }),
        }),
      );
      await page.route("**/rest/content-explorer/translations/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ itemId: 42, locale: "en-us", variants: [] }),
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
          const raw = route.request().postData() || "";
          fieldPuts.push(raw);
          if (rejectNext > 0) {
            const status = rejectNext;
            rejectNext = 0;
            await route.fulfill({
              status,
              contentType: "application/json",
              body: JSON.stringify({ Error: { message: "rejected" } }),
            });
            return;
          }
          const body = JSON.parse(raw);
          const payload = body.ItemEditorFields || body;
          const nextFlag = (payload.fields || []).find((f) => f.name === "no_propagate");
          if (nextFlag) {
            flag = nextFlag.value;
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
          body: JSON.stringify(fieldsPayload(flag, summary)),
        });
      });

      const field = () => page.locator('[data-testid="editor-field-no_propagate"]');
      const summaryField = () => page.locator('[data-testid="editor-field-summary"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(field()).toHaveAttribute("data-editor-kind", "yesno", {
        timeout: 20_000,
      });
      await expect(field()).toBeDisabled();
      await expect(page.locator('[data-testid="editor-save"]')).toHaveCount(0);
      expect(fieldPuts).toEqual([]);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await expect(summaryField()).toHaveValue(SUMMARY);
      await field().selectOption(NEXT);
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await field().selectOption(NEXT);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      const firstPut = JSON.parse(fieldPuts[0]);
      const firstFields = (firstPut.ItemEditorFields || firstPut).fields || [];
      expect(firstFields.find((f) => f.name === "no_propagate")).toMatchObject({
        name: "no_propagate",
        value: NEXT,
        dataType: "boolean",
      });
      expect(firstFields.find((f) => f.name === "summary")?.value).toBe(SUMMARY);
      expect(flag).toBe(NEXT);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(field()).toHaveValue(NEXT);
      await expect(summaryField()).toHaveValue(SUMMARY);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(NEXT, { timeout: 20_000 });
      await expect(summaryField()).toHaveValue(SUMMARY);

      flag = PREVIOUS;
      fieldPuts.length = 0;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await field().selectOption(NEXT);
      rejectNext = 400;
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      await expect(page.locator('[data-testid="editor-field-error-no_propagate"]')).toContainText(
        /could not be saved/i,
      );
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      rejectNext = 403;
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(2);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      rejectNext = 409;
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(3);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(/revision/i);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
