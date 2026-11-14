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
 * React Content Editor image alternative text required-field validation (#6263).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @image}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-image-alt.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-image-upload");

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ fieldPuts: string[], binaryWrites: string[], pageErrors: string[],
 *   required: boolean, altValue: string|null, fieldValue: string|null }} state
 */
async function routeEditor(page, state) {
  page.on("pageerror", (err) => state.pageErrors.push(String(err)));
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
      state.pageErrors.push(text);
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
              required: state.required,
            },
          ],
        },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
      state.fieldPuts.push(route.request().postData() || "");
      const sent = JSON.parse(route.request().postData() || "{}");
      const alt = (sent.fields || []).find((f) => f.name === "img_alt");
      if (alt) {
        state.altValue = alt.value;
      }
    }
    const fields = [{ name: "sys_title", value: "Hero" }];
    if (state.fieldValue != null) {
      fields.push({ name: "img", value: state.fieldValue });
    }
    if (state.altValue != null) {
      fields.push({ name: "img_alt", value: state.altValue });
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
      state.binaryWrites.push(method + " " + route.request().url());
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemEditorBinaryMeta: {
          contentId: "42",
          field: "img",
          filename: "hero.png",
          contentType: "image/png",
          present: true,
        },
      }),
    });
  });
}

test.describe("React Content Editor image alternative text", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "blank alt on a required image refuses save; a typed alt saves via the fields PUT",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }, testInfo) => {
      const state = {
        fieldPuts: [],
        binaryWrites: [],
        pageErrors: [],
        required: true,
        altValue: "Stored alt",
        fieldValue: "hero.png",
      };
      await routeEditor(page, state);

      const altInput = () => page.locator('[data-testid="editor-image-alt-img"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(altInput()).toHaveValue("Stored alt", { timeout: 20_000 });
      await expect(altInput()).toHaveAttribute("aria-label", "Alternative text");
      await expect(page.locator('[data-testid="editor-field-img"]')).toHaveAttribute(
        "data-editor-kind",
        "image",
      );
      await expect(page.locator('[data-testid="editor-field-row-img"]')).toHaveAttribute(
        "data-required",
        "true",
      );
      // The sibling entry hides behind the image row instead of doubling it.
      await expect(page.locator('[data-testid="editor-field-img_alt"]')).toHaveCount(0);

      await altInput().fill("   ");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-img"]')).toHaveText(
        "This field is required.",
      );
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      await expect(
        page.locator('[data-testid="editor-field-error-summary-img"]'),
      ).toContainText("Image");
      expect(state.fieldPuts).toEqual([]);
      expect(state.binaryWrites).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      // The refused save keeps the typed value; storage is untouched.
      await expect(altInput()).toHaveValue("   ");

      await altInput().fill("A hero at dawn");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      expect(state.fieldPuts).toHaveLength(1);
      const sent = JSON.parse(state.fieldPuts[0]);
      expect(
        (sent.fields || []).find((f) => f.name === "img_alt"),
      ).toMatchObject({ name: "img_alt", value: "A hero at dawn" });
      await expect(altInput()).toHaveValue("A hero at dawn");

      await page.screenshot({
        path: `test-results/live-proof-6263-image-alt.png`,
      });
      await testInfo.attach("live-proof-image-alt", {
        path: `test-results/live-proof-6263-image-alt.png`,
        contentType: "image/png",
      });

      page.once("dialog", (dialog) => dialog.dismiss());
      await altInput().fill("Unsaved alt");
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(state.fieldPuts).toHaveLength(1);
      expect(state.binaryWrites).toEqual([]);

      expect(state.pageErrors, `console/page errors: ${state.pageErrors.join(" | ")}`).toEqual(
        [],
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );

  test(
    "an optional image with a blank alt still saves",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }) => {
      const state = {
        fieldPuts: [],
        binaryWrites: [],
        pageErrors: [],
        required: false,
        altValue: "Stored alt",
        fieldValue: "hero.png",
      };
      await routeEditor(page, state);

      const altInput = () => page.locator('[data-testid="editor-image-alt-img"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(altInput()).toHaveValue("Stored alt", { timeout: 20_000 });
      await altInput().fill("");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      expect(state.fieldPuts).toHaveLength(1);
      expect(
        page.locator('[data-testid="editor-field-error-img"]'),
      ).toHaveCount(0);
      expect(state.pageErrors, `console/page errors: ${state.pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "an HTTP 400 on the alt save is not success",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }) => {
      const state = {
        fieldPuts: [],
        binaryWrites: [],
        pageErrors: [],
        required: true,
        altValue: "Stored alt",
        fieldValue: "hero.png",
      };
      await routeEditor(page, state);
      await page.unroute("**/services/itemmanagement/item/fields/**");
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          state.fieldPuts.push(route.request().postData() || "");
          await route.fulfill({
            status: 400,
            contentType: "application/json",
            body: JSON.stringify({ Error: { message: "rejected" } }),
          });
          return;
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
              fields: [
                { name: "sys_title", value: "Hero" },
                { name: "img", value: "hero.png" },
                { name: "img_alt", value: state.altValue },
              ],
            },
          }),
        });
      });

      const altInput = () => page.locator('[data-testid="editor-image-alt-img"]');
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(altInput()).toHaveValue("Stored alt", { timeout: 20_000 });
      await altInput().fill("A hero at dawn");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-save-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      expect(state.fieldPuts).toHaveLength(1);
      // The typed alt is kept so the operator can retry after the failure.
      await expect(altInput()).toHaveValue("A hero at dawn");
      expect(state.pageErrors, `console/page errors: ${state.pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
