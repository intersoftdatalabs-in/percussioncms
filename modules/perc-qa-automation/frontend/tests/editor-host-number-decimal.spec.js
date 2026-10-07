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
 * React Content Editor saves a decimal on a float field (#5346).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-number-decimal.spec.js}</p>
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

function fieldValue(body, name) {
  const parsed = JSON.parse(body || "{}");
  const payload = parsed.ItemEditorFields || parsed;
  const fields = payload.fields || [];
  const row = fields.find((field) => field && field.name === name);
  return row ? row : null;
}

test.describe("React Content Editor decimal number save", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "saves 1.5 on a float field; an integer field refuses that decimal; cancel and HTTP 400, 403, and 409 do not claim success; an in-range integer still saves",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let rate = "1";
      let qty = "4";
      let rejectStatus = 0;
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
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          const body = route.request().postData() || "";
          fieldPuts.push(body);
          if (rejectStatus) {
            const status = rejectStatus;
            rejectStatus = 0;
            const message =
              status === 403 ? "Field 'rate' denied" : status === 409 ? "stale" : "Field 'rate' is not a valid number.";
            await route.fulfill({
              status,
              contentType: "application/json",
              body: JSON.stringify({ Error: { message } }),
            });
            return;
          }
          const nextRate = fieldValue(body, "rate");
          const nextQty = fieldValue(body, "qty");
          if (nextRate) {
            rate = String(nextRate.value ?? rate);
          }
          if (nextQty) {
            qty = String(nextQty.value ?? qty);
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
                { name: "rate", value: rate },
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
                  name: "rate",
                  label: "Rate",
                  control: "sys_Number",
                  dataType: "float",
                },
                {
                  name: "qty",
                  label: "Quantity",
                  control: "sys_Number",
                  dataType: "integer",
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

      const rateField = () => page.locator('[data-testid="editor-field-rate"]');
      const qtyField = () => page.locator('[data-testid="editor-field-qty"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(rateField()).toHaveAttribute("data-editor-kind", "number", {
        timeout: 20_000,
      });
      await expect(rateField()).toHaveAttribute("inputmode", "decimal");
      await expect(qtyField()).toHaveAttribute("inputmode", "numeric");
      await expect(rateField()).toHaveValue("1");
      await expect(qtyField()).toHaveValue("4");

      await rateField().fill("2.5");
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      await expect(rateField()).toHaveValue("2.5");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(rateField()).toHaveValue("1", { timeout: 20_000 });
      await expect(qtyField()).toHaveValue("4");
      await qtyField().fill("1.5");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-qty"]')).toContainText(
        /valid number/i,
      );
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /number fields before saving/i,
      );
      await expect(qtyField()).toBeFocused();
      await expect(qtyField()).toHaveAttribute("aria-invalid", "true");
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(qtyField()).toHaveValue("4", { timeout: 20_000 });
      await rateField().fill("1.5");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      const savedRate = fieldValue(fieldPuts[0], "rate");
      expect(savedRate && savedRate.value).toBe("1.5");
      expect(savedRate && savedRate.dataType).toBe("float");
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(rateField()).toHaveValue("1.5");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(rateField()).toHaveValue("1.5", { timeout: 20_000 });
      await rateField().fill("2.5");
      rejectStatus = 400;
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(2);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /could not be saved/i,
      );
      await expect(page.locator('[data-testid="editor-field-error-rate"]')).toContainText(/rate/i);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      rejectStatus = 403;
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(3);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /not allowed/i,
      );
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      rejectStatus = 409;
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(4);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /newer revision/i,
      );
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(rateField()).toHaveValue("1.5", { timeout: 20_000 });
      await expect(qtyField()).toHaveValue("4");
      await qtyField().fill("7");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(5);
      const savedQty = fieldValue(fieldPuts[4], "qty");
      expect(savedQty && savedQty.value).toBe("7");
      expect(savedQty && savedQty.dataType).toBe("integer");
      expect(savedQty && savedQty.minimum).toBe("0");
      expect(savedQty && savedQty.maximum).toBe("10");
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(qtyField()).toHaveValue("7");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(rateField()).toHaveValue("1.5", { timeout: 20_000 });
      await expect(qtyField()).toHaveValue("7");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
