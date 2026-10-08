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
 * React Content Editor refuses a long-text NUL on save (#5347 / #4532).
 *
 * <p>The client gate in {@code longTextField.ts} stays in place. A keyboard
 * cannot type U+0000, so the spec sets the textarea value through the DOM
 * setter and an {@code input} event (the same controlled {@code onChange}
 * path EditorHost already uses).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @longtext}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-longtext-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
} = require("./helpers/editor-host-longtext-fields");

const PREVIOUS = "line one";
const MULTILINE = "line one\nline two";
const NUL_VALUE = `bad${String.fromCharCode(0)}value`;

/**
 * Controlled textarea update, including U+0000. Code points cross the
 * Playwright bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setTextareaValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, codePoints) => {
    const next = String.fromCodePoint(...codePoints);
    const proto = Object.getOwnPropertyDescriptor(
      window.HTMLTextAreaElement.prototype,
      "value",
    );
    if (!proto || typeof proto.set !== "function") {
      throw new Error("textarea value setter missing");
    }
    const previous = el.value;
    proto.set.call(el, next);
    const tracker = el._valueTracker;
    if (tracker) {
      tracker.setValue(previous);
    }
    el.dispatchEvent(new Event("input", { bubbles: true }));
  }, codes);
}

/**
 * @param {import("@playwright/test").Locator} locator
 * @returns {Promise<boolean>}
 */
async function textareaHasNul(locator) {
  return locator.evaluate((el) => el.value.includes("\u0000"));
}

test.describe("React Content Editor long-text NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a long-text NUL, keeps the previous text, still saves ordinary multiline, and does not treat HTTP 400 as success",
    { tag: ["@explorer-content-editor", "@editor", "@longtext"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let description = PREVIOUS;
      let rejectNext = false;
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
              transitionTriggers: [],
            },
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
                {
                  name: "sys_title",
                  label: "Title",
                  control: "sys_EditBox",
                  dataType: "text",
                },
                {
                  name: "description",
                  label: "Description",
                  control: "sys_TextArea",
                },
              ],
            },
          }),
        }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          const raw = route.request().postData() || "";
          fieldPuts.push(raw);
          if (rejectNext) {
            rejectNext = false;
            await route.fulfill({
              status: 400,
              contentType: "application/json",
              body: JSON.stringify({ Error: { message: "rejected" } }),
            });
            return;
          }
          const body = JSON.parse(raw);
          const payload = body.ItemEditorFields || body;
          const next = (payload.fields || []).find((f) => f.name === "description");
          if (next) {
            description = next.value;
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

      const field = () => page.locator(`[data-testid="${TEST_IDS.fieldDescription}"]`);
      const rowError = () =>
        page.locator(`[data-testid="${TEST_IDS.fieldErrorDescription}"]`);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveAttribute("data-editor-kind", "longtext", {
        timeout: 20_000,
      });
      await expect(field()).toHaveValue(PREVIOUS);

      await setTextareaValue(field(), NUL_VALUE);
      await expect.poll(() => textareaHasNul(field())).toBe(true);
      let dismissed = false;
      page.once("dialog", async (dialog) => {
        dismissed = true;
        await dialog.dismiss();
      });
      await page.locator('[data-testid="editor-close"]').click();
      await expect.poll(() => dismissed).toBe(true);
      expect(fieldPuts).toEqual([]);
      expect(await textareaHasNul(field())).toBe(true);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await setTextareaValue(field(), NUL_VALUE);
      await expect.poll(() => textareaHasNul(field())).toBe(true);
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(rowError()).toContainText(/character that cannot be saved/i);
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(
        /long text fields before saving/i,
      );
      await expect(field()).toHaveAttribute("aria-invalid", "true");
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await field().fill(MULTILINE);
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      const savedBody = JSON.parse(fieldPuts[0] || "{}");
      const savedPayload = savedBody.ItemEditorFields || savedBody;
      const savedDescription = (savedPayload.fields || []).find(
        (f) => f.name === "description",
      );
      expect(savedDescription && savedDescription.value).toBe(MULTILINE);
      expect(savedDescription.value.includes("\u0000")).toBe(false);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(field()).toHaveValue(MULTILINE);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(MULTILINE, { timeout: 20_000 });
      rejectNext = true;
      await field().fill("line one\nrejected");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => fieldPuts.length).toBe(2);
      await expect(rowError()).toContainText(/could not be saved/i);
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(MULTILINE, { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
