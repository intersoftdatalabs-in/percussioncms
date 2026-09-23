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
 * React Content Editor force check-in of another user's checkout (#4775).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-force-checkin.spec.js}</p>
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

const OTHER_FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "editor",
    revision: 1,
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const SELF_FIELDS = {
  ItemEditorFields: {
    ...OTHER_FIELDS.ItemEditorFields,
    checkoutUser: "admin",
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

test.describe("React Content Editor force check-in", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "shows another user's checkout, confirms force check-in, and maps 403/404/409",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      const forceCalls = [];
      let forceStatus = 403;
      let fieldsBody = OTHER_FIELDS;
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") {
          return;
        }
        const text = msg.text();
        if (
          /Failed to load resource: the server responded with a status of (403|404|409)/.test(
            text,
          )
        ) {
          return;
        }
        pageErrors.push(text);
      });

      await page.route("**/rest/editor/items/**/checkout", (route) =>
        route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "checked out" }),
        }),
      );
      await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
        route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "checked out" }),
        }),
      );
      await page.route("**/services/itemmanagement/workflow/forceCheckIn/**", (route) => {
        forceCalls.push(route.request().url());
        const status = forceStatus;
        if (status === 200) {
          fieldsBody = {
            ItemEditorFields: { ...OTHER_FIELDS.ItemEditorFields, checkoutUser: "" },
          };
        }
        return route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({ PSNoContent: { operation: "forceCheckIn" } }),
        });
      });
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
          body: JSON.stringify(fieldsBody),
        }),
      );
      await page.route("**/assembly/slot-relationships/canvas**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ SlotCanvas: { slots: [] } }),
        }),
      );
      await page.route("**/content-explorer/relationships/**/local", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ PSLocalDependencySummary: { links: [] } }),
        }),
      );

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-force-checkin")).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.getByTestId("editor-checkout-user")).toContainText(/editor/i);
      await expect(page.getByTestId("editor-checkin")).toHaveCount(0);

      page.once("dialog", (dialog) => dialog.dismiss());
      await page.getByTestId("editor-force-checkin").click();
      expect(forceCalls).toEqual([]);
      await expect(page.getByTestId("editor-checkout-user")).toContainText(/editor/i);

      for (const status of [403, 404, 409]) {
        forceStatus = status;
        page.once("dialog", (dialog) => dialog.accept());
        await page.getByTestId("editor-force-checkin").click();
        const pattern =
          status === 403 ? /not allowed/i : status === 404 ? /not found/i : /not checked out/i;
        await expect(page.getByTestId("editor-lock-error")).toContainText(pattern);
        await expect(page.getByTestId("editor-checkout-user")).toContainText(/editor/i);
        await expect(page.getByTestId("editor-save")).toHaveCount(0);
      }
      expect(forceCalls.length).toBe(3);

      forceStatus = 200;
      page.once("dialog", (dialog) => dialog.accept());
      await page.getByTestId("editor-force-checkin").click();
      await expect(page.getByTestId("editor-checkout-user")).toHaveCount(0);
      await expect(page.getByTestId("editor-force-checkin")).toHaveCount(0);
      await expect(page.getByTestId("editor-checkout")).toBeVisible();
      expect(forceCalls.length).toBe(4);

      fieldsBody = SELF_FIELDS;
      await page.unroute("**/rest/editor/items/**/checkout");
      await page.unroute("**/services/itemmanagement/workflow/checkOut/**");
      await page.route("**/rest/editor/items/**/checkout", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            EditorItemLockInfo: {
              checkOutUser: "admin",
              currentUser: "admin",
              itemName: "Home",
              assignmentType: "Admin",
            },
          }),
        }),
      );
      await page.getByTestId("editor-checkout").click();
      await expect(page.getByTestId("editor-save")).toBeVisible();
      await expect(page.getByTestId("editor-checkin")).toBeVisible();
      await expect(page.getByTestId("editor-force-checkin")).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.getByTestId("editor-form")).toBeVisible({ timeout: 20_000 });
      await expect(page.getByTestId("editor-force-checkin")).toHaveCount(0);

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
      });
    },
  );
});
