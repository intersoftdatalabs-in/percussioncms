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
 * React Content Editor refuses a blank required HTML field (#5251).
 * TipTap-only since #5902 (the {@code ?engine=tinymce} escape hatch is
 * removed): every HTML field renders the bundled TipTap editor, so no
 * {@code engine} param is passed.
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-html-required-blank.spec.js}</p>
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

test.describe("React Content Editor blank required HTML", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a blank required HTML field; cancel does not write; a non-blank value still saves",
    { tag: ["@explorer-content-editor", "@editor", "@html"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let bodyHtml = "<p>Hi</p>";
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      // Registered first so later field/workflow stubs win. Keeps fake id 42
      // off the QA server log; the browser still ignores failed-resource noise.
      await page.route("**/services/**", (route) =>
        route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
      );
      await page.route("**/rest/**", (route) =>
        route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
      );
      // TipTap-only (#5902): the React editor must never fetch the legacy
      // TinyMCE script under any param.
      const tinymceRequests = [];
      page.on("request", (req) => {
        if (req.url().includes("/sys_resources/tinymce/")) {
          tinymceRequests.push(req.url());
        }
      });
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
      // Quiet the fake id 42 against the live cell: untranslated and
      // workflow-allowance lookups would otherwise log server ERRORs
      // (same empty shapes as editor-host-html-tiptap.spec.js).
      await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ choices: [] }),
        }),
      );
      await page.route("**/services/assembly/slot-relationships/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ SlotCanvas: { ownerId: 42, slots: [] } }),
        }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          const body = route.request().postData() || "";
          fieldPuts.push(body);
          const htmlMatch = body.match(/"name"\s*:\s*"text"\s*,\s*"value"\s*:\s*"([^"]*)"/);
          if (htmlMatch) {
            bodyHtml = htmlMatch[1];
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
              fields: [{ name: "text", value: bodyHtml }],
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
              fields: [
                {
                  name: "text",
                  label: "Body",
                  control: "sys_tinymce",
                  required: true,
                },
              ],
            },
          }),
        }),
      );

      const htmlField = () => page.locator('[data-testid="editor-field-text"]');
      const htmlRow = () => page.locator('[data-testid="editor-field-row-text"]');
      const clearHtml = () => page.locator('[data-testid="editor-html-clear-text"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveAttribute("data-editor-kind", "html", {
        timeout: 20_000,
      });
      await expect(htmlField()).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(htmlField()).toContainText("Hi");
      await expect(htmlRow()).toHaveAttribute("data-required", "true");

      await clearHtml().click();
      await expect(htmlField()).toBeEmpty();
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      await expect(htmlField()).toBeEmpty();
      await expect(htmlRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Hi", { timeout: 20_000 });
      await clearHtml().click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-text"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(htmlRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Hi", { timeout: 20_000 });
      await clearHtml().click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-text"]')).toContainText(
        /required/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      // A whitespace-only stored value is still blank for the required gate.
      bodyHtml = "   ";
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveAttribute("data-editor-engine", "tiptap", {
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-text"]')).toContainText(
        /required/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      bodyHtml = "<p>Hi</p>";
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Hi", { timeout: 20_000 });
      await htmlField().click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Bye");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"text"\s*,\s*"value"\s*:\s*"<p>Bye<\/p>"/);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(htmlField()).toContainText("Bye");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Bye", { timeout: 20_000 });
      expect(tinymceRequests, `tinymce fetched: ${tinymceRequests.join(" | ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
