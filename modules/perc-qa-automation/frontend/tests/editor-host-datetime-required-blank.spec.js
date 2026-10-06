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
 * React Content Editor refuses a blank required datetime field (#5253).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-datetime-required-blank.spec.js}</p>
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

test.describe("React Content Editor blank required datetime", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a blank required datetime; cancel does not write; a non-blank datetime still saves",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let eventAt = "2026-01-01 09:00:00";
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
          const eventMatch = body.match(
            /"name"\s*:\s*"event_at"\s*,\s*"value"\s*:\s*"([^"]*)"/,
          );
          if (eventMatch) {
            eventAt = eventMatch[1];
          }
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorFields: {
              contentId: "42",
              contentType: "percEvent",
              name: "Event",
              checkoutUser: "admin",
              revision: 4,
              fields: [{ name: "event_at", value: eventAt }],
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
              name: "percEvent",
              fields: [
                {
                  name: "event_at",
                  label: "Event at",
                  control: "sys_CalendarSimple",
                  dataType: "datetime",
                  required: true,
                },
              ],
            },
          }),
        }),
      );

      const eventField = () => page.locator('[data-testid="editor-field-event_at"]');
      const eventRow = () => page.locator('[data-testid="editor-field-row-event_at"]');
      const clearEvent = () => page.locator('[data-testid="editor-date-clear-event_at"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(eventField()).toHaveAttribute("data-editor-kind", "datetime", {
        timeout: 20_000,
      });
      await expect(eventField()).toHaveValue("2026-01-01T09:00");
      await expect(eventField()).toHaveAttribute("aria-required", "true");
      await expect(eventRow()).toHaveAttribute("data-required", "true");

      await clearEvent().click();
      await expect(eventField()).toHaveValue("");
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      await expect(eventField()).toHaveValue("");
      await expect(eventRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(eventField()).toHaveValue("2026-01-01T09:00", { timeout: 20_000 });
      await clearEvent().click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-event_at"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      await expect(eventField()).toBeFocused();
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(eventRow()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(eventField()).toHaveValue("2026-01-01T09:00", { timeout: 20_000 });
      await eventField().fill("");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-event_at"]')).toContainText(
        /required/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(eventField()).toHaveValue("2026-01-01T09:00", { timeout: 20_000 });
      await eventField().fill("2026-09-18T14:30");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(
        /"name"\s*:\s*"event_at"\s*,\s*"value"\s*:\s*"2026-09-18 14:30:00"/,
      );
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(eventField()).toHaveValue("2026-09-18T14:30");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(eventField()).toHaveValue("2026-09-18T14:30", { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
