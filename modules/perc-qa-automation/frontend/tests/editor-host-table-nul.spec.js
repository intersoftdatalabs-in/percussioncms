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
 * React Content Editor refuses a table-cell NUL on save (#5417 / #4532).
 *
 * <p>The client gate in {@code tableField.ts} runs after the required-empty
 * table check. A keyboard cannot type U+0000, so the spec sets the cell
 * through the DOM setter and an {@code input} event. JSON wire text escapes
 * the NUL; the gate reads the cell text.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @table}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-table-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-workflow");

const STORED_GRID = JSON.stringify({ columns: ["day"], rows: [["Mon"]] });
const TUE_GRID = JSON.stringify({ columns: ["day"], rows: [["Tue"]] });
const NUL_CELL = `Mon${String.fromCharCode(0)}`;

/**
 * Controlled input update, including U+0000. Code points cross the Playwright
 * bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setInputValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload);
    const proto = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value");
    if (!proto || typeof proto.set !== "function") {
      throw new Error("input value setter missing");
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
async function controlHasNul(locator) {
  return locator.evaluate((el) => el.value.includes("\u0000"));
}

test.describe("React Content Editor table-cell NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a table-cell NUL, keeps the previous cell, still saves a normal cell, and leaves the required-empty-table gate in place",
    { tag: ["@explorer-content-editor", "@editor", "@table"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let hoursValue = STORED_GRID;
      let required = true;
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
      await page.route("**/services/assembly/**", (route) =>
        route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
      );
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ContentTypeDetail: {
              name: "percPage",
              fields: [
                { name: "sys_title", label: "Title", control: "sys_EditBox" },
                {
                  name: "hours",
                  label: "Hours",
                  control: "sys_Table",
                  required,
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
          try {
            const parsed = JSON.parse(raw);
            const payload = parsed.ItemEditorFields || parsed;
            const fields = Array.isArray(payload.fields) ? payload.fields : [];
            const hours = fields.find((row) => row && row.name === "hours");
            if (hours && typeof hours.value === "string") {
              hoursValue = hours.value;
            }
          } catch {
            /* leave the previous grid */
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
                fields: [
                  { name: "sys_title", value: "Home" },
                  { name: "hours", value: hoursValue },
                ],
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
              revision: 3,
              fields: [
                { name: "sys_title", value: "Home" },
                { name: "hours", value: hoursValue },
              ],
            },
          }),
        });
      });

      const cell = () => page.locator('[data-testid="editor-table-cell-hours-0-0"]');
      const grid = () => page.locator('[data-testid="editor-field-hours"]');
      const row = () => page.locator('[data-testid="editor-field-row-hours"]');
      const hoursError = () => page.locator('[data-testid="editor-field-error-hours"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Mon", { timeout: 20_000 });
      await expect(grid()).toHaveAttribute("data-editor-kind", "table");
      await expect(row()).toHaveAttribute("data-required", "true");

      await setInputValue(cell(), NUL_CELL);
      await expect.poll(() => controlHasNul(cell())).toBe(true);
      let dismissed = false;
      page.once("dialog", async (dialog) => {
        dismissed = true;
        await dialog.dismiss();
      });
      await page.locator('[data-testid="editor-close"]').click();
      await expect.poll(() => dismissed).toBe(true);
      expect(fieldPuts).toEqual([]);
      expect(await controlHasNul(cell())).toBe(true);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Mon", { timeout: 20_000 });
      await setInputValue(cell(), NUL_CELL);
      await expect.poll(() => controlHasNul(cell())).toBe(true);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(hoursError()).toContainText(/table cell contains a character that cannot be saved/i);
      await expect(hoursError()).not.toHaveText("This field is required.");
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /table fields before saving/i,
      );
      await expect(grid()).toHaveAttribute("aria-invalid", "true");
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Mon", { timeout: 20_000 });
      await page.locator('[data-testid="editor-table-remove-hours-0"]').click();
      await expect(cell()).toHaveCount(0);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(hoursError()).toHaveText("This field is required.");
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(row()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Mon", { timeout: 20_000 });
      await cell().fill("Tue");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      const savedBody = JSON.parse(fieldPuts[0] || "{}");
      const savedPayload = savedBody.ItemEditorFields || savedBody;
      const savedHours = (savedPayload.fields || []).find((row) => row.name === "hours");
      expect(savedHours && savedHours.value).toBe(TUE_GRID);
      expect(String(savedHours.value).includes("\u0000")).toBe(false);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(cell()).toHaveValue("Tue");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Tue", { timeout: 20_000 });
      rejectNext = true;
      await cell().fill("Wed");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(2);
      await expect(page.locator('[data-testid="editor-save-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Tue", { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
