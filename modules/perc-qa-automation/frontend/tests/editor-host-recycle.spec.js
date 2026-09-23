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
 * React Content Editor recycle of the open item (#4773).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-recycle.spec.js}</p>
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

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    revision: 1,
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

const PATH_ITEM = {
  PathItem: {
    id: "42",
    name: "Home",
    path: "//Sites/Demo/Home",
    type: "percPage",
    category: "page",
    leaf: true,
  },
};

test.describe("React Content Editor recycle open item", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "confirms recycle, maps 403, hides the control in view mode",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      const deletes = [];
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
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(TYPE),
        }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(FIELDS),
        }),
      );
      await page.route("**/pathmanagement/path/item/id/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(PATH_ITEM),
        }),
      );
      let deleteStatus = 200;
      await page.route("**/rest/folders/item/**", async (route) => {
        if (route.request().method() !== "DELETE") {
          await route.continue();
          return;
        }
        deletes.push(route.request().url());
        await route.fulfill({
          status: deleteStatus,
          contentType: "application/json",
          body: deleteStatus === 200 ? "{}" : JSON.stringify({ message: "no" }),
        });
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-recycle"]')).toBeVisible({
        timeout: 20_000,
      });

      page.once("dialog", (dialog) => dialog.dismiss());
      await page.locator('[data-testid="editor-recycle"]').click();
      await expect(page.locator('[data-testid="editor-content-id"]')).toContainText("42");
      expect(deletes).toEqual([]);

      deleteStatus = 403;
      page.once("dialog", (dialog) => dialog.accept());
      await page.locator('[data-testid="editor-recycle"]').click();
      await expect(page.locator('[data-testid="editor-recycle-error"]')).toContainText(
        /not allowed/i,
      );
      await expect(page.locator('[data-testid="editor-content-id"]')).toContainText("42");
      await expect(page.locator('[data-testid="editor-recycle-done"]')).toHaveCount(0);
      expect(deletes.length).toBe(1);
      expect(deletes[0]).toContain("/rest/folders/item/");
      expect(deletes[0]).toContain("Home");

      deleteStatus = 200;
      page.once("dialog", (dialog) => dialog.accept());
      await page.locator('[data-testid="editor-recycle"]').click();
      await expect(page.locator('[data-testid="editor-recycle-done"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-content-id"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-recycle"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-form"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-recycle"]')).toHaveCount(0);

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
      });
    },
  );
});
