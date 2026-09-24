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
 * EditorHost check-in revision comment (#4817).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run: {@code npm run test:surface -- --path tests/editor-host-checkin-comment.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { TEST_IDS, editorSpaUrl } = require("./helpers/editor-host-checkout");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

async function stubEditor(page, checkinUrls) {
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: "admin",
          currentUser: "admin",
          assignmentType: "Assignee",
        },
      }),
    }),
  );
  await page.route("**/rest/editor/items/**/checkin**", (route) => {
    checkinUrls.push(route.request().url());
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: "",
          currentUser: "admin",
          assignmentType: "Assignee",
        },
      }),
    });
  });
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: { itemId: "42", stateName: "Draft", transitionTriggers: [] },
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
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, locale: "en-us", variants: [] }),
    }),
  );
}

test.describe("EditorHost check-in comment", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "cancel does not check in and confirm sends the revision comment",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      const checkinUrls = [];
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
      await stubEditor(page, checkinUrls);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await page.getByTestId(TEST_IDS.checkin).click();
      await expect(page.getByTestId(TEST_IDS.checkinComment)).toBeVisible();
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-checkin-comment"]',
      });
      await page.getByTestId(TEST_IDS.checkinCancel).click();
      await expect(page.getByTestId(TEST_IDS.checkinComment)).toHaveCount(0);
      expect(checkinUrls).toEqual([]);

      await page.getByTestId(TEST_IDS.checkin).click();
      await page.getByTestId(TEST_IDS.checkinCommentInput).fill("shipped copy");
      await page.getByTestId(TEST_IDS.checkinConfirm).click();
      await expect.poll(() => checkinUrls.length).toBe(1);
      expect(decodeURIComponent(checkinUrls[0])).toContain("comment=shipped copy");

      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );
});
