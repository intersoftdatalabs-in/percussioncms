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
 * React Content Editor community field save and view lock.
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-community-field.spec.js}</p>
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
      { name: "sys_communityid", label: "Community", control: "sys_DropDownSingle" },
    ],
  },
};

function fieldsPayload(communityId) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percEvent",
      name: "Home",
      checkoutUser: "admin",
      revision: 1,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "sys_communityid", value: communityId },
      ],
    },
  };
}

test.describe("React Content Editor community field", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "saves the selected community and shows it again; view mode is read-only",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      let communityId = "10";
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
      await page.route("**/services/communities/find**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            CommunityList: [
              { id: 10, name: "Default", label: "Default" },
              { id: 20, name: "Enterprise", label: "Enterprise" },
            ],
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
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          const body = route.request().postData() || "";
          fieldPuts.push(body);
          const match = body.match(/"name"\s*:\s*"sys_communityid"\s*,\s*"value"\s*:\s*"(\d+)"/);
          if (match) {
            communityId = match[1];
          }
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(fieldsPayload(communityId)),
        });
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const community = page.locator('[data-testid="editor-field-sys_communityid"]');
      await expect(community).toBeVisible({ timeout: 20_000 });
      await expect(community).toHaveAttribute("data-editor-kind", "community");
      await expect(community).toHaveValue("10");
      await community.selectOption("20");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"sys_communityid"\s*,\s*"value"\s*:\s*"20"/);
      await expect(community).toHaveValue("20");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const reopened = page.locator('[data-testid="editor-field-sys_communityid"]');
      await expect(reopened).toBeVisible({ timeout: 20_000 });
      await expect(reopened).toHaveValue("20");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-form"]',
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      const viewCommunity = page.locator('[data-testid="editor-field-sys_communityid"]');
      await expect(viewCommunity).toBeVisible({ timeout: 20_000 });
      await expect(viewCommunity).toBeDisabled();
      await expect(page.locator('[data-testid="editor-save"]')).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
