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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * EditorHost refuses a schedule comment that contains a NUL (#5444 / #4532).
 *
 * <p>A keyboard cannot type U+0000, so the spec sets the comment through the
 * textarea value setter and an {@code input} event (the same controlled
 * {@code onChange} path the schedule dialog already uses).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @publish}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-schedule-comment-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-workflow");

const PAGE_FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const PAGE_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

const INITIAL_DATES = {
  itemId: "42",
  startDate: "09/18/2026 09:00 am",
  endDate: "09/19/2026 10:00 am",
  comments: "first",
};

const NUL_COMMENT = `keep${String.fromCharCode(0)}me`;

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
 * @param {string[]} pageErrors
 */
function consoleOn(page, pageErrors) {
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (400|403|404|409|500)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ dates: object, posts: string[], fail: { status?: number, body?: object } | null }} store
 */
async function stubEditorApis(page, store) {
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_FIELDS),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName: "Draft",
          transitionTriggers: ["Submit"],
        },
        ItemWorkflowChoices: { workflows: [] },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/item/getitemdates/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ItemDates: store.dates }),
    }),
  );
  await page.route("**/services/itemmanagement/item/setitemdates**", async (route) => {
    store.posts.push(route.request().postData() || "");
    const fail = store.fail;
    if (fail) {
      await route.fulfill({
        status: fail.status ?? 200,
        contentType: "application/json",
        body: JSON.stringify(fail.body ?? { message: "rejected" }),
      });
      return;
    }
    const raw = route.request().postData() || "{}";
    const parsed = JSON.parse(raw);
    const next = parsed.ItemDates || parsed;
    store.dates = {
      itemId: String(next.itemId || "42"),
      startDate: next.startDate || "",
      endDate: next.endDate || "",
      comments: next.comments || "",
    };
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ operation: "Success" }),
    });
  });
}

test.describe("EditorHost schedule comment NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a NUL comment, reopen shows the previous schedule, and a normal comment still saves",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = {
        dates: { ...INITIAL_DATES },
        posts: [],
        fail: null,
      };
      await stubEditorApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-schedule")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("editor-schedule").click();
      await expect(page.getByTestId("explorer-schedule-comments")).toHaveValue("first");
      await page.getByTestId("explorer-schedule-start").fill("2026-09-20T09:00");
      await page.getByTestId("explorer-schedule-end").fill("2026-09-21T10:00");
      const comments = page.getByTestId("explorer-schedule-comments");
      await setTextareaValue(comments, NUL_COMMENT);
      await expect
        .poll(() => comments.evaluate((el) => el.value.includes("\u0000")))
        .toBe(true);
      await page.getByTestId("explorer-schedule-save").click();
      await expect(page.getByTestId("explorer-schedule-dialog-error")).toContainText(
        /schedule comment contains a character that cannot be saved/i,
      );
      expect(store.posts).toEqual([]);
      await expect(page.getByTestId("editor-schedule-done")).toHaveCount(0);
      await expect(page.getByTestId("explorer-schedule-dialog")).toBeVisible();
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-schedule-dialog"]',
      });

      await page.getByTestId("explorer-schedule-cancel").click();
      expect(store.posts).toEqual([]);
      await expect(page.getByTestId("editor-schedule-done")).toHaveCount(0);
      await page.getByTestId("editor-schedule").click();
      await expect(page.getByTestId("explorer-schedule-start")).toHaveValue(
        "2026-09-18T09:00",
      );
      await expect(page.getByTestId("explorer-schedule-end")).toHaveValue(
        "2026-09-19T10:00",
      );
      await expect(page.getByTestId("explorer-schedule-comments")).toHaveValue("first");

      await page.getByTestId("explorer-schedule-comments").fill("ship window");
      await page.getByTestId("explorer-schedule-save").click();
      await expect(page.getByTestId("editor-schedule-done")).toBeVisible();
      expect(store.posts.length).toBe(1);
      expect(store.posts[0]).toContain("ship window");
      expect(store.posts[0]).not.toContain("\u0000");
      await page.getByTestId("editor-schedule").click();
      await expect(page.getByTestId("explorer-schedule-comments")).toHaveValue(
        "ship window",
      );
      await expect(page.getByTestId("explorer-schedule-start")).toHaveValue(
        "2026-09-18T09:00",
      );
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "HTTP 409 and HTTP 200 FORBIDDEN, BADCONFIG, and INVALID are not a saved schedule",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = {
        dates: { ...INITIAL_DATES },
        posts: [],
        fail: null,
      };
      await stubEditorApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-schedule")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("editor-schedule").click();
      await expect(page.getByTestId("explorer-schedule-comments")).toHaveValue("first");

      const failures = [
        { status: 409, body: { message: "editing" }, pattern: /HTTP 409|editing/i },
        { status: 200, body: { status: "FORBIDDEN" }, pattern: /not allowed/i },
        { status: 200, body: { status: "BADCONFIG" }, pattern: /not valid/i },
        { status: 200, body: { status: "INVALID" }, pattern: /not valid/i },
      ];
      for (const failure of failures) {
        store.fail = { status: failure.status, body: failure.body };
        await page.getByTestId("explorer-schedule-save").click();
        await expect(page.getByTestId("explorer-schedule-dialog-error")).toContainText(
          failure.pattern,
        );
        await expect(page.getByTestId("editor-schedule-done")).toHaveCount(0);
        await expect(page.getByTestId("explorer-schedule-dialog")).toBeVisible();
        await expect(page.getByTestId("explorer-schedule-comments")).toHaveValue("first");
      }
      expect(store.posts.length).toBe(failures.length);
      expect(store.dates.comments).toBe("first");
      expect(store.dates.startDate).toBe(INITIAL_DATES.startDate);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
