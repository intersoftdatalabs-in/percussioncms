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
 * React Content Editor lists every invalid field in one error summary (#5680).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-field-error-summary.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

function editorSpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const params = new URLSearchParams(query.startsWith("?") ? query.slice(1) : query);
  params.set("entry", "editor");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

test.describe("React Content Editor field error summary", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "blocked save lists both invalid fields; entries focus rows; success clears",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let headline = "Hello";
      let qty = "4";
      let rejectNextPut = null;
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      // Registered first so later field/workflow stubs win. Keeps fake id 42
      // off the QA server log; the browser still ignores failed-resource noise.
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
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          const body = route.request().postData() || "";
          fieldPuts.push(body);
          const headlineMatch = body.match(/"name"\s*:\s*"headline"\s*,\s*"value"\s*:\s*"([^"]*)"/);
          if (headlineMatch) {
            headline = headlineMatch[1];
          }
          const qtyMatch = body.match(/"name"\s*:\s*"qty"\s*,\s*"value"\s*:\s*"([^"]*)"/);
          if (qtyMatch) {
            qty = qtyMatch[1];
          }
          if (rejectNextPut) {
            const failure = rejectNextPut;
            rejectNextPut = null;
            await route.fulfill({
              status: failure.status,
              contentType: "application/json",
              body: JSON.stringify(failure.body),
            });
            return;
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
              revision: 4,
              fields: [
                { name: "headline", value: headline },
                { name: "qty", value: qty },
              ],
            },
          }),
        });
      });
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ContentTypeDetail: {
              name: "percPage",
              fields: [
                {
                  name: "headline",
                  label: "Headline",
                  control: "sys_EditBox",
                  dataType: "text",
                  required: true,
                },
                {
                  name: "qty",
                  label: "Quantity",
                  control: "sys_Number",
                  dataType: "integer",
                  required: true,
                  controlProperties: [
                    { name: "minimum", value: "0" },
                    { name: "maximum", value: "10" },
                  ],
                },
              ],
            },
          }),
        }),
      );

      const headlineField = () => page.locator('[data-testid="editor-field-headline"]');
      const qtyField = () => page.locator('[data-testid="editor-field-qty"]');
      const summary = () => page.locator('[data-testid="editor-field-error-summary"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(headlineField()).toHaveValue("Hello", { timeout: 20_000 });
      await headlineField().fill("");
      await qtyField().fill("");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(summary()).toBeVisible();
      await expect(summary()).toHaveAttribute("role", "alert");
      await expect(summary()).toContainText(/headline/i);
      await expect(summary()).toContainText(/quantity/i);
      await expect(page.locator('[data-testid="editor-field-error-headline"]')).toContainText(
        /required/i,
      );
      await expect(page.locator('[data-testid="editor-field-error-qty"]')).toContainText(
        /required/i,
      );
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      await expect(headlineField()).toBeFocused();
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.locator('[data-testid="editor-field-error-summary-qty"]').click();
      await expect(qtyField()).toBeFocused();
      await page.locator('[data-testid="editor-field-error-summary-headline"]').click();
      await expect(headlineField()).toBeFocused();

      await headlineField().fill("Updated headline");
      await qtyField().fill("7");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(summary()).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(headlineField()).toHaveValue("Updated headline", { timeout: 20_000 });
      await headlineField().fill("");
      const putsBeforeClose = fieldPuts.length;
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts.length).toEqual(putsBeforeClose);
      await expect(headlineField()).toHaveValue("");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(headlineField()).toHaveValue("Updated headline", { timeout: 20_000 });
      rejectNextPut = {
        status: 400,
        body: {
          fieldErrors: {
            headline: "Headline was rejected",
            qty: "Quantity was rejected",
          },
        },
      };
      await headlineField().fill("Another headline");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(summary()).toBeVisible();
      await expect(summary()).toContainText(/headline was rejected/i);
      await expect(summary()).toContainText(/quantity was rejected/i);
      await expect(page.locator('[data-testid="editor-field-error-headline"]')).toContainText(
        /headline was rejected/i,
      );
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
