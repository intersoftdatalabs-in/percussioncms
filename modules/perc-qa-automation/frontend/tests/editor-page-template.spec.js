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
 * EditorHost page template change (#4839).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-page-template.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function editorSpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const params = new URLSearchParams(query.startsWith("?") ? query.slice(1) : query);
  params.set("entry", "editor");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

const BASE_ID = "16777215-101-1";
const PLAIN_ID = "16777215-101-2";

function fields(templateId) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 3,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "templateid", value: templateId },
      ],
    },
  };
}

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
    allowedTemplates: [
      { name: "perc.base.page", label: "Base", guid: { stringValue: BASE_ID } },
      { name: "perc.base.plain", label: "Plain", guid: { stringValue: PLAIN_ID } },
    ],
  },
};

test.describe("React Content Editor page template", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "saves another allowed page template and shows it on reopen",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const fieldPuts = [];
      const templatePuts = [];
      const pageErrors = [];
      let current = BASE_ID;
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      await page.route("**/rest/editor/items/**/checkout", (route) =>
        route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          fieldPuts.push(route.request().postData() || "");
          const body = route.request().postData() || "";
          if (body.includes(PLAIN_ID)) {
            current = PLAIN_ID;
          }
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(fields(current)),
        });
      });
      await page.route("**/pagemanagement/page/changeTemplate/**", async (route) => {
        templatePuts.push(route.request().url());
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: "{}",
        });
      });
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(TYPE),
        }),
      );
      await page.route("**/pathmanagement/path/item/id/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PathItem: { path: "//Sites/Demo/Home", name: "Home" },
          }),
        }),
      );
      await page.route("**/itemmanagement/workflow/getTransitions/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ stateName: "Draft", transitionTriggers: [] }),
        }),
      );
      await page.route("**/assembly/slot-relationships/canvas**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SlotCanvas: { ownerId: 42, templateId: null, slots: [] },
          }),
        }),
      );
      await page.route("**/content-explorer/relationships/**/local", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ count: 0, links: [] }),
        }),
      );
      await page.route("**/content-explorer/translations/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            itemId: 42,
            locale: "en-us",
            variants: [{ contentId: 42, locale: "en-us", role: "source" }],
          }),
        }),
      );

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const select = page.locator('[data-testid="editor-page-template"]');
      await expect(select).toBeVisible({ timeout: 20_000 });
      await expect(select).toHaveValue(BASE_ID);
      await expect(page.locator('[data-testid="editor-field-templateid"]')).toHaveCount(0);
      await select.selectOption(PLAIN_ID);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toContain(PLAIN_ID);
      expect(fieldPuts[0]).toContain('"name":"templateid"');
      await expect.poll(() => templatePuts.length).toBeGreaterThan(0);
      expect(templatePuts[0]).toContain(`/changeTemplate/42/${encodeURIComponent(PLAIN_ID)}`);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-page-template"]')).toHaveValue(PLAIN_ID, {
        timeout: 20_000,
      });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
