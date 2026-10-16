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
 * EditorHost refuses a check-in comment that contains a NUL (#5442 / #4532).
 *
 * <p>A keyboard cannot type U+0000, so the spec sets the comment through the
 * textarea value setter and an {@code input} event (the same controlled
 * {@code onChange} path EditorHost already uses).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-checkin-comment-nul.spec.js}</p>
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

const NUL_COMMENT = `bad${String.fromCharCode(0)}value`;

/**
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setTextareaValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload.codes);
    const proto = Object.getOwnPropertyDescriptor(
      window.HTMLTextAreaElement.prototype,
      "value",
    );
    if (!proto || typeof proto.set !== "function") {
      throw new Error("textarea value setter missing");
    }
    const previous = el.value;
    proto.set.call(el, next);
    const tracker = el._valueTracker;
    if (tracker) {
      tracker.setValue(previous);
    }
    el.dispatchEvent(new Event("input", { bubbles: true }));
  }, { codes });
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {string[]} checkinUrls
 * @param {() => number} statusForCheckin
 */
async function stubEditor(page, checkinUrls, statusForCheckin) {
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
    const status = statusForCheckin();
    return route.fulfill({
      status,
      contentType: "application/json",
      body: JSON.stringify({
        EditorItemLockInfo: {
          itemName: "Home",
          checkOutUser: status === 200 ? "" : "admin",
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

/**
 * @param {import("@playwright/test").Page} page
 * @returns {string[]}
 */
function trackPageErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (/Failed to load resource/.test(text)) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("EditorHost check-in comment NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a NUL check-in comment, cancel does not check in, and a normal comment still checks in",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackPageErrors(page);
      const checkinUrls = [];
      await stubEditor(page, checkinUrls, () => 200);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.checkoutUser)).toContainText(/admin/i);

      await page.getByTestId(TEST_IDS.checkin).click();
      await expect(page.getByTestId(TEST_IDS.checkinComment)).toBeVisible();
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-checkin-comment"]',
      });
      await page.getByTestId(TEST_IDS.checkinCancel).click();
      await expect(page.getByTestId(TEST_IDS.checkinComment)).toHaveCount(0);
      expect(checkinUrls).toEqual([]);
      await expect(page.getByTestId(TEST_IDS.checkoutUser)).toContainText(/admin/i);

      await page.getByTestId(TEST_IDS.checkin).click();
      await setTextareaValue(page.getByTestId(TEST_IDS.checkinCommentInput), NUL_COMMENT);
      await expect
        .poll(() =>
          page.getByTestId(TEST_IDS.checkinCommentInput).evaluate((el) =>
            el.value.includes("\u0000"),
          ),
        )
        .toBe(true);
      await page.getByTestId(TEST_IDS.checkinConfirm).click();
      await expect(page.getByTestId(TEST_IDS.lockError)).toContainText(
        /character that cannot be saved/i,
      );
      expect(checkinUrls).toEqual([]);
      await expect(page.getByTestId(TEST_IDS.checkinComment)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.checkoutUser)).toContainText(/admin/i);

      await page.getByTestId(TEST_IDS.checkinCommentInput).fill("shipped copy");
      await page.getByTestId(TEST_IDS.checkinConfirm).click();
      await expect.poll(() => checkinUrls.length).toBe(1);
      expect(decodeURIComponent(checkinUrls[0])).toContain("comment=shipped copy");

      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "a blank check-in comment checks in with no comment parameter",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackPageErrors(page);
      const checkinUrls = [];
      await stubEditor(page, checkinUrls, () => 200);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await page.getByTestId(TEST_IDS.checkin).click();
      await page.getByTestId(TEST_IDS.checkinCommentInput).fill("   ");
      await page.getByTestId(TEST_IDS.checkinConfirm).click();
      await expect.poll(() => checkinUrls.length).toBe(1);
      expect(decodeURIComponent(checkinUrls[0])).not.toContain("comment=");
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "HTTP 400, 403, and 409 do not check the item in",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackPageErrors(page);
      const checkinUrls = [];
      const statuses = [400, 403, 409];
      let attempt = 0;
      await stubEditor(page, checkinUrls, () => statuses[attempt] ?? 500);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await page.getByTestId(TEST_IDS.checkin).click();
      await page.getByTestId(TEST_IDS.checkinCommentInput).fill("shipped copy");

      const patterns = [
        /could not check in/i,
        /not allowed to check in/i,
        /not checked out to you/i,
      ];
      for (let i = 0; i < patterns.length; i += 1) {
        attempt = i;
        await page.getByTestId(TEST_IDS.checkinConfirm).click();
        await expect(page.getByTestId(TEST_IDS.lockError)).toContainText(patterns[i]);
        await expect(page.getByTestId(TEST_IDS.checkinComment)).toBeVisible();
        await expect(page.getByTestId(TEST_IDS.checkoutUser)).toContainText(/admin/i);
      }
      expect(checkinUrls).toHaveLength(3);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );
});
