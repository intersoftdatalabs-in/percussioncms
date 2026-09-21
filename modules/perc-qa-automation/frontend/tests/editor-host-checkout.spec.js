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
 * React Content Editor host — check-out / check-in (#4603).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-checkout.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isWorkflowCheckoutUrl,
  isWorkflowCheckinUrl,
} = require("./helpers/editor-host-checkout");

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

function leftoverOn(page, leftover) {
  page.on("request", (req) => {
    const u = req.url();
    if (
      u.includes("checkoutedit.xml") ||
      u.includes("contenteditorurls.html") ||
      /view=editor/.test(u)
    ) {
      leftover.push(u);
    }
  });
}

function consoleOn(page, pageErrors) {
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
}

async function stubEditorApis(page, { checkoutStatus, checkoutBody, checkinStatus } = {}) {
  const checkoutJson = JSON.stringify(
    checkoutBody ?? {
      EditorItemLockInfo: {
        itemName: "Home",
        checkOutUser: "admin",
        currentUser: "admin",
        assignmentType: "Assignee",
      },
    },
  );
  const fulfillCheckout = (route) =>
    route.fulfill({
      status: checkoutStatus ?? 200,
      contentType: "application/json",
      body: checkoutJson,
    });
  const fulfillCheckin = (route) =>
    route.fulfill({
      status: checkinStatus ?? 200,
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
  await page.route("**/services/itemmanagement/workflow/checkOut/**", fulfillCheckout);
  await page.route("**/rest/editor/items/**/checkout", fulfillCheckout);
  await page.route("**/services/itemmanagement/workflow/checkIn/**", fulfillCheckin);
  await page.route("**/rest/editor/items/**/checkin", fulfillCheckin);
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
}

test.describe("React Content Editor check-out and check-in", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "checked out to self shows Check In; 403 on check-in is not success",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const leftover = [];
      const pageErrors = [];
      leftoverOn(page, leftover);
      consoleOn(page, pageErrors);
      await stubEditorApis(page, { checkinStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.checkin)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.save)).toBeVisible();
      await page.getByTestId(TEST_IDS.checkin).click();
      await expect(page.getByTestId(TEST_IDS.lockError)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.lockError)).toContainText(/not allowed to check in/i);
      expect(leftover).toEqual([]);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      await expectNoSeriousA11yViolations(page);
    },
  );

  test(
    "409 on check-out stays view-only and is not success",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const leftover = [];
      const pageErrors = [];
      leftoverOn(page, leftover);
      consoleOn(page, pageErrors);
      await stubEditorApis(page, {
        checkoutStatus: 409,
        checkoutBody: { message: "checked out" },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.locked)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.checkout)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.save)).toHaveCount(0);
      await expect(page.getByTestId(TEST_IDS.lockError)).toContainText(
        /checked out to another user/i,
      );
      expect(leftover).toEqual([]);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "Check Out is available when the item is not held by the session user",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const leftover = [];
      leftoverOn(page, leftover);
      await stubEditorApis(page, {
        checkoutBody: {
          ItemUserInfo: {
            itemName: "Home",
            checkOutUser: "editor",
            currentUser: "admin",
            assignmentType: "Reader",
          },
        },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.checkout)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.save)).toHaveCount(0);
      expect(leftover).toEqual([]);
    },
  );
});
