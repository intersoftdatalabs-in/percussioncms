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
 * React Content Editor refuses a blank required link field (#5226).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-link-required-blank.spec.js}</p>
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

test.describe("React Content Editor blank required link", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a blank required link; cancel does not write; a non-blank link still saves",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let pageLink = "594";
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
          const linkMatch = body.match(/"name"\s*:\s*"page"\s*,\s*"value"\s*:\s*"([^"]*)"/);
          if (linkMatch) {
            pageLink = linkMatch[1];
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
              fields: [{ name: "page", value: pageLink }],
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
                  name: "page",
                  label: "Page link",
                  control: "sys_PageLink",
                  dataType: "text",
                  required: true,
                },
              ],
            },
          }),
        }),
      );

      const linkField = () => page.locator('[data-testid="editor-field-page"]');
      const linkRow = () => page.locator('[data-testid="editor-field-row-page"]');
      const clearLink = () => page.locator('[data-testid="editor-link-clear-page"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveAttribute("data-editor-kind", "link", {
        timeout: 20_000,
      });
      await expect(linkField()).toHaveValue("594");
      await expect(linkField()).toHaveAttribute("aria-required", "true");
      await expect(linkRow()).toHaveAttribute("data-required", "true");

      await clearLink().click();
      await expect(linkField()).toHaveValue("");
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      await expect(linkField()).toHaveValue("");
      await expect(linkRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue("594", { timeout: 20_000 });
      await clearLink().click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-page"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      await expect(linkField()).toBeFocused();
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(linkRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue("594", { timeout: 20_000 });
      await linkField().fill("");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-page"]')).toContainText(
        /required/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue("594", { timeout: 20_000 });
      await linkField().fill("   ");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-page"]')).toContainText(
        /required/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue("594", { timeout: 20_000 });
      await linkField().fill("//Sites/Example/index");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(
        /"name"\s*:\s*"page"\s*,\s*"value"\s*:\s*"\/\/Sites\/Example\/index"/,
      );
      expect(fieldPuts[0]).toMatch(/"dataType"\s*:\s*"link"/);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(linkField()).toHaveValue("//Sites/Example/index");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue("//Sites/Example/index", { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
