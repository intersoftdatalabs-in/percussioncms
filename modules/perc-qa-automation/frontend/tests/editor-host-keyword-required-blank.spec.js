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
 * React Content Editor refuses a required keyword with no choice (#5252).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @keyword}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-keyword-required-blank.spec.js}</p>
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

const TYPE = {
  ContentTypeDetail: {
    name: "percEvent",
    fields: [
      {
        name: "keywords",
        label: "Keywords",
        control: "sys_DropDownSingle",
        required: true,
      },
    ],
  },
};

const KEYWORDS = {
  Keyword: [
    {
      value: "keywords",
      label: "Keywords",
      choices: [
        { value: "news", label: "News" },
        { value: "events", label: "Events" },
      ],
    },
  ],
};

function fieldsPayload(keyword) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percEvent",
      name: "Home",
      checkoutUser: "admin",
      revision: 4,
      fields: [{ name: "keywords", value: keyword }],
    },
  };
}

test.describe("React Content Editor blank required keyword", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a required keyword with no choice; cancel does not write; a catalog choice still saves",
    { tag: ["@explorer-content-editor", "@editor", "@keyword"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let keyword = "news";
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      // Registered first so later field/keyword stubs win. Keeps fake id 42
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
      await page.route("**/services/keywords**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(KEYWORDS),
        }),
      );
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(TYPE),
        }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          const body = route.request().postData() || "";
          fieldPuts.push(body);
          const match = body.match(/"name"\s*:\s*"keywords"\s*,\s*"value"\s*:\s*"([^"]*)"/);
          if (match) {
            keyword = match[1];
          }
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(fieldsPayload(keyword)),
        });
      });

      const select = () => page.locator('[data-testid="editor-field-keywords"]');
      const row = () => page.locator('[data-testid="editor-field-row-keywords"]');
      const clearKeyword = () => page.locator('[data-testid="editor-keyword-clear-keywords"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveAttribute("data-editor-kind", "keyword", {
        timeout: 20_000,
      });
      await expect(select()).toHaveValue("news");
      await expect(select().locator('option[value="events"]')).toHaveCount(1);
      await expect(row()).toHaveAttribute("data-required", "true");

      await clearKeyword().click();
      await expect(select()).toHaveValue("");
      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      await expect(select()).toHaveValue("");
      await expect(row()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue("news", { timeout: 20_000 });
      await clearKeyword().click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-keywords"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /required fields before saving/i,
      );
      await expect(select()).toBeFocused();
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(row()).toHaveAttribute("data-required", "true");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue("news", { timeout: 20_000 });
      await select().selectOption("");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-keywords"]')).toContainText(
        /required/i,
      );
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue("news", { timeout: 20_000 });
      await select().selectOption("events");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"keywords"\s*,\s*"value"\s*:\s*"events"/);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(select()).toHaveValue("events");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue("events", { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
