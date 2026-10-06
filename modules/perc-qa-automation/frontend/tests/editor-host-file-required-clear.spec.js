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
 * React Content Editor refuses clearing a required file field (#5279).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @file}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-file-required-clear.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-file-upload");

test.describe("React Content Editor required file clear", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses an empty required file and a clear of a stored file; a chosen file still saves",
    { tag: ["@explorer-content-editor", "@editor", "@file"] },
    async ({ page }) => {
      const fieldPuts = [];
      const binaryWrites = [];
      const pageErrors = [];
      let present = true;
      let filename = "brief.pdf";
      let fieldValue = "brief.pdf";
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
              name: "percFile",
              fields: [
                { name: "sys_title", label: "Title", control: "sys_EditBox" },
                {
                  name: "item_file_attachment",
                  label: "File",
                  control: "sys_File",
                  required: true,
                },
              ],
            },
          }),
        }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          fieldPuts.push(route.request().postData() || "");
        }
        const fields = [{ name: "sys_title", value: "Brief" }];
        if (fieldValue != null) {
          fields.push({ name: "item_file_attachment", value: fieldValue });
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorFields: {
              contentId: "42",
              contentType: "percFile",
              name: "Brief",
              checkoutUser: "admin",
              revision: 4,
              fields,
            },
          }),
        });
      });
      await page.route("**/services/itemmanagement/item/binary/**", async (route) => {
        const method = route.request().method();
        if (method === "PUT" || method === "DELETE") {
          binaryWrites.push(method + " " + route.request().url());
          if (method === "PUT") {
            filename = "notes.txt";
            present = true;
            fieldValue = "notes.txt";
          }
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorBinaryMeta: {
              contentId: "42",
              field: "item_file_attachment",
              filename: present ? filename : "",
              contentType: present ? "application/pdf" : "",
              present,
            },
          }),
        });
      });

      const fileName = () => page.locator('[data-testid="editor-file-name-item_file_attachment"]');
      const fileRow = () => page.locator('[data-testid="editor-field-row-item_file_attachment"]');
      const fileInput = () => page.locator('[data-testid="editor-file-item_file_attachment"]');
      const clearFile = () => page.locator('[data-testid="editor-file-clear-item_file_attachment"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(fileName()).toHaveText("brief.pdf", { timeout: 20_000 });
      await expect(page.locator('[data-testid="editor-field-item_file_attachment"]')).toHaveAttribute(
        "data-editor-kind",
        "file",
      );
      await expect(fileRow()).toHaveAttribute("data-required", "true");

      await clearFile().click();
      await expect(fileName()).toHaveText(/no file attached/i);
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      expect(binaryWrites).toEqual([]);
      await expect(fileRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(fileName()).toHaveText("brief.pdf", { timeout: 20_000 });
      await clearFile().click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-item_file_attachment"]')).toHaveText(
        "This field is required.",
      );
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      expect(fieldPuts).toEqual([]);
      expect(binaryWrites).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(fileName()).toHaveText(/no file attached/i);
      await expect(fileRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(fileName()).toHaveText("brief.pdf", { timeout: 20_000 });

      present = false;
      filename = "";
      fieldValue = null;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(fileName()).toHaveText(/no file attached/i, { timeout: 20_000 });
      await expect(clearFile()).toHaveCount(0);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-item_file_attachment"]')).toHaveText(
        "This field is required.",
      );
      expect(fieldPuts).toEqual([]);
      expect(binaryWrites).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await fileInput().setInputFiles({
        name: "notes.txt",
        mimeType: "text/plain",
        buffer: Buffer.from("hello"),
      });
      await expect(fileName()).toHaveText("notes.txt");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => binaryWrites.length).toBe(1);
      expect(binaryWrites[0]).toMatch(/^PUT /);
      expect(binaryWrites[0]).toContain("item_file_attachment");
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(fileName()).toHaveText("notes.txt");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(fileName()).toHaveText("notes.txt", { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
