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
 * React Content Editor host — clear the publish schedule (#5123).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @publish}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-clear-schedule.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
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
  comments: "keep",
};

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
    if (store.setStatus && store.setStatus !== 200) {
      await route.fulfill({
        status: store.setStatus,
        contentType: "application/json",
        body: "{}",
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

test.describe("React Content Editor clear publish schedule", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "confirm clears both dates only after refresh; cancel does not post",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = {
        dates: { ...INITIAL_DATES },
        posts: [],
      };
      await stubEditorApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-clear-schedule")).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.getByTestId("editor-schedule")).toBeVisible();
      await page.getByTestId("editor-clear-schedule").click();
      await expect(page.getByTestId("editor-clear-schedule-current")).toContainText(
        "09/18/2026 09:00 am",
      );
      await page.getByTestId("editor-clear-schedule-cancel").click();
      expect(store.posts).toEqual([]);
      await expect(page.getByTestId("editor-clear-schedule-done")).toHaveCount(0);

      await page.getByTestId("editor-clear-schedule").click();
      await page.getByTestId("editor-clear-schedule-confirm").click();
      await expect(page.getByTestId("editor-clear-schedule-done")).toBeVisible();
      expect(store.posts.length).toBe(1);
      const posted = JSON.parse(store.posts[0]);
      expect(posted.ItemDates.startDate).toBe("");
      expect(posted.ItemDates.endDate).toBe("");
      await page.getByTestId("editor-schedule").click();
      await expect(page.getByTestId("explorer-schedule-start")).toHaveValue("");
      await expect(page.getByTestId("explorer-schedule-end")).toHaveValue("");
      await page.getByTestId("explorer-schedule-cancel").click();
      await page.getByTestId("editor-clear-schedule").click();
      await expect(page.getByTestId("editor-clear-schedule-current")).toContainText(
        "no scheduled dates",
      );
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "HTTP 400, 403, and 409 do not claim the schedule was cleared",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = {
        dates: { ...INITIAL_DATES },
        posts: [],
        setStatus: 400,
      };
      await stubEditorApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-clear-schedule")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("editor-clear-schedule").click();
      await page.getByTestId("editor-clear-schedule-confirm").click();
      await expect(page.getByTestId("editor-clear-schedule-error")).toBeVisible();
      await expect(page.getByTestId("editor-clear-schedule-current")).toContainText(
        "09/18/2026 09:00 am",
      );
      await expect(page.getByTestId("editor-clear-schedule-done")).toHaveCount(0);
      expect(store.dates.startDate).toBe(INITIAL_DATES.startDate);

      store.setStatus = 403;
      await page.getByTestId("editor-clear-schedule-confirm").click();
      await expect(page.getByTestId("editor-clear-schedule-error")).toContainText(
        /not allowed to clear/i,
      );
      await expect(page.getByTestId("editor-clear-schedule-done")).toHaveCount(0);

      store.setStatus = 409;
      await page.getByTestId("editor-clear-schedule-confirm").click();
      await expect(page.getByTestId("editor-clear-schedule-error")).toContainText(
        /was not cleared/i,
      );
      await expect(page.getByTestId("editor-clear-schedule-dialog")).toBeVisible();
      await expect(page.getByTestId("editor-clear-schedule-current")).toContainText(
        "09/19/2026 10:00 am",
      );
      await expect(page.getByTestId("editor-clear-schedule-done")).toHaveCount(0);
      expect(store.posts.length).toBe(3);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
