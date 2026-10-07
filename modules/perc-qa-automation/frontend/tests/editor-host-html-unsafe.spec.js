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
 * React Content Editor refuses unsafe HTML on save (#5313).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-html-unsafe.spec.js}</p>
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

test.describe("React Content Editor unsafe HTML", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses script, event-handler, and javascript URL HTML; cancel does not write; ordinary HTML still saves; HTTP 400 is not success",
    { tag: ["@explorer-content-editor", "@editor", "@html"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let html = "<p>Hi</p>";
      let rejectNext = false;
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.route("**/sys_resources/tinymce/**", (route) =>
        route.fulfill({ status: 404, body: "" }),
      );
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
          if (rejectNext) {
            rejectNext = false;
            await route.fulfill({
              status: 400,
              contentType: "application/json",
              body: JSON.stringify({
                Error: { message: "Field 'text' contains script that cannot be saved." },
              }),
            });
            return;
          }
          const htmlMatch = body.match(/"name"\s*:\s*"text"\s*,\s*"value"\s*:\s*"([^"]*)"/);
          if (htmlMatch) {
            html = htmlMatch[1]
              .replace(/\\u003c/g, "<")
              .replace(/\\u003e/g, ">")
              .replace(/\\"/g, '"')
              .replace(/\\\\/g, "\\");
          }
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorFields: {
              contentId: "42",
              contentType: "percRichText",
              name: "Intro",
              checkoutUser: "admin",
              revision: 4,
              fields: [{ name: "text", value: html }],
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
              name: "percRichText",
              fields: [{ name: "text", label: "Body", control: "sys_tinymce" }],
            },
          }),
        }),
      );

      const htmlField = () => page.locator('[data-testid="editor-field-text"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveAttribute("data-editor-kind", "html", {
        timeout: 20_000,
      });
      await expect(htmlField()).toHaveValue("<p>Hi</p>");

      await htmlField().fill("<script>x</script>");
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      await expect(htmlField()).toHaveValue("<script>x</script>");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveValue("<p>Hi</p>", { timeout: 20_000 });
      await htmlField().fill("<p><script>alert(1)</script></p>");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-text"]')).toContainText(
        /script or event markup/i,
      );
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /HTML fields before saving/i,
      );
      await expect(htmlField()).toBeFocused();
      await expect(htmlField()).toHaveAttribute("aria-invalid", "true");
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveValue("<p>Hi</p>", { timeout: 20_000 });
      await htmlField().fill('<img src="x" onerror="alert(1)">');
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-text"]')).toContainText(
        /script or event markup/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveValue("<p>Hi</p>", { timeout: 20_000 });
      await htmlField().fill('<a href="javascript:alert(1)">x</a>');
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-text"]')).toContainText(
        /script or event markup/i,
      );
      expect(fieldPuts).toEqual([]);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveValue("<p>Hi</p>", { timeout: 20_000 });
      await htmlField().fill("<p>Bye</p>");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"text"/);
      expect(fieldPuts[0]).toContain("<p>Bye</p>");
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(htmlField()).toHaveValue("<p>Bye</p>");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveValue("<p>Bye</p>", { timeout: 20_000 });
      rejectNext = true;
      await htmlField().fill("<p>Ok</p>");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(2);
      await expect(page.locator('[data-testid="editor-field-error-text"]')).toContainText(
        /script/i,
      );
      await expect(page.locator('[data-testid="editor-save-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveValue("<p>Bye</p>", { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
