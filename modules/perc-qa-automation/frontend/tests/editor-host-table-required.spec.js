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
 * React Content Editor refuses saving an empty required sys_Table (#5281).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @table}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-table-required.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-workflow");

const STORED_GRID = JSON.stringify({ columns: ["day"], rows: [["Mon"]] });

test.describe("React Content Editor required table", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses an empty required table, keeps the previous grid, and still saves cell text",
    { tag: ["@explorer-content-editor", "@editor", "@table"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let hoursValue = STORED_GRID;
      let required = true;
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.route("**/services/**", (route) =>
        route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
      );
      await page.route("**/rest/**", (route) =>
        route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
      );
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          const text = msg.text();
          if (text.includes("Failed to load resource")) {
            return;
          }
          pageErrors.push(text);
        }
      });
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
          const body = route.request().postData() || "";
          fieldPuts.push(body);
          try {
            const parsed = JSON.parse(body);
            const fields = Array.isArray(parsed.fields) ? parsed.fields : [];
            const hours = fields.find((row) => row && row.name === "hours");
            if (hours && typeof hours.value === "string") {
              hoursValue = hours.value;
            }
          } catch {
            /* leave the previous grid */
          }
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

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Mon", { timeout: 20_000 });
      await expect(grid()).toHaveAttribute("data-editor-kind", "table");
      await expect(row()).toHaveAttribute("data-required", "true");

      await page.locator('[data-testid="editor-table-remove-hours-0"]').click();
      await expect(cell()).toHaveCount(0);
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      await expect(row()).toHaveAttribute("data-required", "true");
      await expect(grid()).toHaveAttribute("data-editor-kind", "table");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Mon", { timeout: 20_000 });
      await page.locator('[data-testid="editor-table-remove-hours-0"]').click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-hours"]')).toHaveText(
        "This field is required.",
      );
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(grid()).toHaveAttribute("data-editor-kind", "table");
      await expect(page.getByRole("columnheader", { name: "day" })).toBeVisible();
      await expect(row()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Mon", { timeout: 20_000 });

      hoursValue = "";
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(grid()).toHaveAttribute("data-editor-kind", "table", { timeout: 20_000 });
      await expect(cell()).toHaveCount(0);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-hours"]')).toHaveText(
        "This field is required.",
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.locator('[data-testid="editor-table-add-hours"]').click();
      await cell().fill("Tue");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      expect(fieldPuts[0]).toContain("Tue");
      expect(fieldPuts[0]).toContain('"name":"sys_title"');
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(cell()).toHaveValue("Tue");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(cell()).toHaveValue("Tue", { timeout: 20_000 });

      required = false;
      hoursValue = "";
      fieldPuts.length = 0;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(row()).toHaveAttribute("data-required", "false", { timeout: 20_000 });
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      expect(fieldPuts[0]).toContain('"name":"hours"');
      await expect(page.locator('[data-testid="editor-field-error-hours"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
