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
 * React Content Editor keyword field save, leave-without-PUT, and view lock.
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-keyword-field.spec.js}</p>
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
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "keywords", label: "Keywords", control: "sys_DropDownSingle" },
    ],
  },
};

function fieldsPayload(keyword) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percEvent",
      name: "Home",
      checkoutUser: "admin",
      revision: 1,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "keywords", value: keyword },
      ],
    },
  };
}

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

test.describe("React Content Editor keyword field", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "saves the selected keyword and shows it again; close does not PUT; view is read-only",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      let keyword = "news";
      const fieldPuts = [];
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
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
              transitionTriggers: ["Submit"],
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
          const match = body.match(/"name"\s*:\s*"keywords"\s*,\s*"value"\s*:\s*"([^"]+)"/);
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

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const select = page.locator('[data-testid="editor-field-keywords"]');
      await expect(select).toBeVisible({ timeout: 20_000 });
      await expect(select).toHaveAttribute("data-editor-kind", "keyword");
      await expect(select).toHaveValue("news");
      await select.selectOption("events");
      page.once("dialog", (dialog) => dialog.accept());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const again = page.locator('[data-testid="editor-field-keywords"]');
      await expect(again).toBeVisible({ timeout: 20_000 });
      await again.selectOption("events");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"keywords"\s*,\s*"value"\s*:\s*"events"/);
      await expect(again).toHaveValue("events");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const reopened = page.locator('[data-testid="editor-field-keywords"]');
      await expect(reopened).toBeVisible({ timeout: 20_000 });
      await expect(reopened).toHaveValue("events");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-form"]',
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      const viewKeyword = page.locator('[data-testid="editor-field-keywords"]');
      await expect(viewKeyword).toBeVisible({ timeout: 20_000 });
      await expect(viewKeyword).toBeDisabled();
      await expect(page.locator('[data-testid="editor-save"]')).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "keeps HTTP 400 and 403 on the keyword field",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const statuses = [400, 403];
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
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
            ItemStateTransition: { itemId: "42", stateName: "Draft", transitionTriggers: [] },
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
          const status = statuses.shift() ?? 400;
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message: status === 403 ? "denied" : "rejected" }),
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(fieldsPayload("news")),
        });
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const select = page.locator('[data-testid="editor-field-keywords"]');
      await expect(select).toBeVisible({ timeout: 20_000 });
      await select.selectOption("events");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-keywords"]')).toContainText(
        /could not be saved/i,
      );
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(page.locator('[data-testid="editor-field-error-keywords"]')).toContainText(
        /not allowed/i,
      );
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "clears a saved keyword on save; close without save does not PUT; view has no clear",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      let keyword = "news";
      const fieldPuts = [];
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
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

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const clear = page.locator('[data-testid="editor-keyword-clear-keywords"]');
      await expect(clear).toBeVisible({ timeout: 20_000 });
      await clear.click();
      await expect(page.locator('[data-testid="editor-field-keywords"]')).toHaveValue("");
      page.once("dialog", (dialog) => dialog.accept());
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-field-keywords"]')).toHaveValue("news", {
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-keyword-clear-keywords"]').click();
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"keywords"\s*,\s*"value"\s*:\s*""/);
      await expect(page.locator('[data-testid="editor-field-keywords"]')).toHaveValue("");
      await expect(page.locator('[data-testid="editor-keyword-clear-keywords"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-field-keywords"]')).toHaveValue("", {
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-keyword-clear-keywords"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator('[data-testid="editor-field-keywords"]')).toBeDisabled({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-keyword-clear-keywords"]')).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
