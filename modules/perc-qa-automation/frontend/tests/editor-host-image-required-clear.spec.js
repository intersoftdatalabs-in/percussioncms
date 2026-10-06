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
 * React Content Editor refuses clearing a required image field (#5280).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @image}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-image-required-clear.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-image-upload");

test.describe("React Content Editor required image clear", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses an empty required image and a clear of a stored image; a chosen image still saves",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }) => {
      const fieldPuts = [];
      const binaryWrites = [];
      const pageErrors = [];
      let present = true;
      let filename = "hero.png";
      let fieldValue = "hero.png";
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
              name: "percImage",
              fields: [
                { name: "sys_title", label: "Title", control: "sys_EditBox" },
                {
                  name: "img",
                  label: "Image",
                  control: "sys_webImageFX",
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
        const fields = [{ name: "sys_title", value: "Hero" }];
        if (fieldValue != null) {
          fields.push({ name: "img", value: fieldValue });
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorFields: {
              contentId: "42",
              contentType: "percImage",
              name: "Hero",
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
            filename = "photo.png";
            present = true;
            fieldValue = "photo.png";
          }
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorBinaryMeta: {
              contentId: "42",
              field: "img",
              filename: present ? filename : "",
              contentType: present ? "image/png" : "",
              present,
            },
          }),
        });
      });

      const imageName = () => page.locator('[data-testid="editor-file-name-img"]');
      const imageRow = () => page.locator('[data-testid="editor-field-row-img"]');
      const imageInput = () => page.locator('[data-testid="editor-file-img"]');
      const clearImage = () => page.locator('[data-testid="editor-file-clear-img"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(imageName()).toHaveText("hero.png", { timeout: 20_000 });
      await expect(page.locator('[data-testid="editor-field-img"]')).toHaveAttribute(
        "data-editor-kind",
        "image",
      );
      await expect(imageRow()).toHaveAttribute("data-required", "true");

      await clearImage().click();
      await expect(imageName()).toHaveText(/no image attached/i);
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      expect(binaryWrites).toEqual([]);
      await expect(imageRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(imageName()).toHaveText("hero.png", { timeout: 20_000 });
      await clearImage().click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-img"]')).toHaveText(
        "This field is required.",
      );
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      expect(fieldPuts).toEqual([]);
      expect(binaryWrites).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(imageName()).toHaveText(/no image attached/i);
      await expect(imageRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(imageName()).toHaveText("hero.png", { timeout: 20_000 });

      present = false;
      filename = "";
      fieldValue = null;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(imageName()).toHaveText(/no image attached/i, { timeout: 20_000 });
      await expect(clearImage()).toHaveCount(0);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-img"]')).toHaveText(
        "This field is required.",
      );
      expect(fieldPuts).toEqual([]);
      expect(binaryWrites).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await imageInput().setInputFiles({
        name: "photo.png",
        mimeType: "image/png",
        buffer: Buffer.from("png"),
      });
      await expect(imageName()).toHaveText("photo.png");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => binaryWrites.length).toBe(1);
      expect(binaryWrites[0]).toMatch(/^PUT /);
      expect(binaryWrites[0]).toContain("/img");
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(imageName()).toHaveText("photo.png");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(imageName()).toHaveText("photo.png", { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
