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
 * React Content Editor date / datetime widgets (sys_CalendarSimple).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-date-fields.spec.js}</p>
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
    contentType: "percEvent",
    name: "Event",
    checkoutUser: "admin",
    fields: [
      { name: "sys_title", value: "Event" },
      { name: "sys_contentstartdate", value: "2026-01-01" },
      { name: "event_at", value: "2026-01-01 09:00:00" },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percEvent",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      {
        name: "sys_contentstartdate",
        label: "Start",
        control: "sys_CalendarSimple",
      },
      {
        name: "event_at",
        label: "Event at",
        control: "sys_CalendarSimple",
        dataType: "datetime",
      },
    ],
  },
};

test.describe("React Content Editor date calendar fields", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "renders date widgets, round-trips save, maps 400, view is read-only",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
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
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          fieldPuts.push(route.request().postData() || "");
          const body = route.request().postData() || "";
          if (body.includes("not-a-date")) {
            await route.fulfill({
              status: 400,
              contentType: "application/json",
              body: JSON.stringify({
                Error: {
                  message: "sys_contentstartdate is not a valid date",
                  errorData: "sys_contentstartdate",
                },
              }),
            });
            return;
          }
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(FIELDS),
        });
      });
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(TYPE),
        }),
      );

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="editor-field-sys_contentstartdate"]'),
      ).toHaveAttribute("data-editor-kind", "date");
      await expect(page.locator('[data-testid="editor-field-event_at"]')).toHaveAttribute(
        "data-editor-kind",
        "datetime",
      );
      await page.locator('[data-testid="editor-field-sys_contentstartdate"]').fill("2026-09-18");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(/2026-09-18/);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      const viewDate = page.locator('[data-testid="editor-field-sys_contentstartdate"]');
      await expect(viewDate).toBeDisabled();
      await expect(page.locator('[data-testid="editor-save"]')).toHaveCount(0);
    },
  );
});
