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
 * React Content Editor host — scheduled-transition (embargo/expiry)
 * trigger panel (#5830, parent #4532).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @workflow}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-scheduled-transitions.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { TEST_IDS, editorSpaUrl } = require("./helpers/editor-host-checkout");

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

function trackErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (400|403|404|409)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

async function stubCommon(page, store) {
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
      body: JSON.stringify({
        PathItem: { id: "42", name: "Home", path: "//Folders/Lab/Home", type: "page" },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ choices: [] }),
    }),
  );
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
  await page.route("**/itemmanagement/item/siteimpact/page/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ pages: [], templates: [] }),
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

test.describe("EditorHost scheduled transitions", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "toggle lists embargo and expiry; panel Set saves and refreshes the rows",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      await page.addInitScript(() => {
        window.close = () => undefined;
      });
      const store = {
        dates: {
          itemId: "42",
          startDate: "09/18/2026 09:00 am",
          endDate: "09/19/2026 10:00 am",
          comments: "",
        },
        posts: [],
      };
      await stubCommon(page, store);
      let dateGets = 0;
      page.on("request", (request) => {
        if (/\/services\/itemmanagement\/item\/getitemdates\//.test(request.url())) {
          dateGets += 1;
        }
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      // Collapsed by default: no dates fetch fires and no panel renders.
      await expect(
        page.getByTestId("editor-scheduled-transitions-toggle"),
      ).toBeVisible();
      await expect(page.getByTestId("editor-scheduled-transitions")).toHaveCount(0);
      expect(dateGets).toBe(0);
      await page.getByTestId("editor-scheduled-transitions-toggle").click();
      await expect(
        page.getByTestId("editor-scheduled-transitions-list"),
      ).toBeVisible();
      expect(dateGets).toBe(1);
      await expect(
        page.getByTestId("editor-scheduled-transition-embargo-date"),
      ).toContainText("09/18/2026 09:00 am");
      await expect(
        page.getByTestId("editor-scheduled-transition-expiry-date"),
      ).toContainText("09/19/2026 10:00 am");
      // Settable from the panel: Set opens the schedule dialog prefilled.
      await page.getByTestId("editor-scheduled-transitions-set").click();
      await expect(page.getByTestId("explorer-schedule-dialog")).toBeVisible();
      await expect(page.getByTestId("explorer-schedule-start")).toHaveValue(
        "2026-09-18T09:00",
      );
      await page.getByTestId("explorer-schedule-start").fill("2026-09-20T09:00");
      await page.getByTestId("explorer-schedule-end").fill("2026-09-21T10:00");
      await page.getByTestId("explorer-schedule-save").click();
      await expect(page.getByTestId("editor-schedule-done")).toBeVisible();
      expect(store.posts.length).toBe(1);
      // The panel refreshes to the saved embargo without reopening.
      await expect(
        page.getByTestId("editor-scheduled-transition-embargo-date"),
      ).toContainText("09/20/2026 09:00 am");
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-scheduled-transitions"]',
      });
      expect(pageErrors).toEqual([]);
    },
  );

  test(
    "empty schedule shows an empty state and 403 shows a load error",
    { tag: ["@explorer-content-editor", "@editor", "@workflow"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      const store = {
        dates: { itemId: "42", startDate: "", endDate: "", comments: "" },
        posts: [],
      };
      await stubCommon(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.getByTestId(TEST_IDS.form)).toBeVisible();
      // Read-only panel is available in view mode too, without Set/Clear.
      await page.getByTestId("editor-scheduled-transitions-toggle").click();
      await expect(
        page.getByTestId("editor-scheduled-transitions-empty"),
      ).toBeVisible();
      await expect(
        page.getByTestId("editor-scheduled-transitions-empty"),
      ).toContainText(/no embargo or expiry/i);
      await expect(
        page.getByTestId("editor-scheduled-transitions-actions"),
      ).toHaveCount(0);

      await page.unroute("**/services/itemmanagement/item/getitemdates/**");
      await page.route("**/services/itemmanagement/item/getitemdates/**", (route) =>
        route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "forbidden" }),
        }),
      );
      await page.getByTestId("editor-scheduled-transitions-toggle").click();
      await page.getByTestId("editor-scheduled-transitions-toggle").click();
      await expect(
        page.getByTestId("editor-scheduled-transitions-load-error"),
      ).toBeVisible();
      await expect(
        page.getByTestId("editor-scheduled-transitions-load-error"),
      ).toContainText(/could not load the scheduled transitions/i);
      await expect(
        page.getByTestId("editor-scheduled-transitions-empty"),
      ).toHaveCount(0);
      expect(pageErrors).toEqual([]);
    },
  );
});
