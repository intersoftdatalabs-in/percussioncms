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
 * React Content Editor host — remove the open item from the incremental queue (#5162).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @publish}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-incremental-remove.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { editorSpaUrl } = require("./helpers/editor-host-workflow");

function pageFields(contentType) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType,
      name: "Home",
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: "Home" }],
    },
  };
}

function pageType(contentType) {
  return {
    ContentTypeDetail: {
      name: contentType,
      fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
    },
  };
}

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
      body: JSON.stringify(pageFields(store.contentType)),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(pageType(store.contentType)),
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
  await page.route("**/incremental/explorer/**/remove", async (route) => {
    store.removePosts.push(route.request().url());
    if (store.holdRemove) {
      await store.removeGate;
    }
    await route.fulfill({
      status: store.removeStatus || 204,
      contentType: "text/plain",
      body: "",
    });
  });
  await page.route("**/incremental/explorer/**/approve", async (route) => {
    store.approvePosts.push(route.request().url());
    await route.fulfill({
      status: 204,
      contentType: "text/plain",
      body: "",
    });
  });
}

async function approveUntilBadge(page) {
  await page.getByTestId("editor-incremental-approve").click();
  await page.getByTestId("editor-incremental-approve-confirm").click();
  await expect(page.getByTestId("editor-incremental-approved")).toBeVisible();
}

test.describe("React Content Editor incremental remove", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "confirm clears approved only after the server accepts; cancel does not post",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      let release = () => {};
      const store = {
        contentType: "percPage",
        approvePosts: [],
        removePosts: [],
        removeStatus: 204,
        holdRemove: true,
        removeGate: new Promise((resolve) => {
          release = resolve;
        }),
      };
      await stubEditorApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-incremental-remove")).toBeVisible({
        timeout: 20_000,
      });
      await approveUntilBadge(page);
      await page.getByTestId("editor-incremental-remove").click();
      await page.getByTestId("editor-incremental-remove-cancel").click();
      expect(store.removePosts).toEqual([]);
      await expect(page.getByTestId("editor-incremental-approved")).toBeVisible();
      await expect(page.getByTestId("editor-incremental-removed")).toHaveCount(0);

      await page.getByTestId("editor-incremental-remove").click();
      await page.getByTestId("editor-incremental-remove-confirm").click();
      await expect.poll(() => store.removePosts.length).toBe(1);
      await expect(page.getByTestId("editor-incremental-approved")).toBeVisible();
      await expect(page.getByTestId("editor-incremental-removed")).toHaveCount(0);
      await expect(page.getByTestId("editor-incremental-remove-dialog")).toBeVisible();
      store.holdRemove = false;
      release();
      await expect(page.getByTestId("editor-incremental-removed")).toBeVisible();
      await expect(page.getByTestId("editor-incremental-approved")).toHaveCount(0);
      await expect(page.getByTestId("editor-incremental-remove-dialog")).toHaveCount(0);
      expect(store.removePosts[0]).toMatch(/\/incremental\/explorer\/42\/remove/);
      expect(store.approvePosts[0]).toMatch(/\/incremental\/explorer\/42\/approve/);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "a template does not post or claim removed",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = {
        contentType: "percTemplate",
        approvePosts: [],
        removePosts: [],
        removeStatus: 204,
        holdRemove: false,
        removeGate: Promise.resolve(),
      };
      await stubEditorApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-incremental-remove")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("editor-incremental-remove").click();
      await page.getByTestId("editor-incremental-remove-confirm").click();
      await expect(page.getByTestId("editor-incremental-remove-error")).toContainText(
        /only a page or asset/i,
      );
      expect(store.removePosts).toEqual([]);
      await expect(page.getByTestId("editor-incremental-removed")).toHaveCount(0);
      await expect(page.getByTestId("editor-incremental-approved")).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "HTTP 400, 403, and 409 do not claim removed",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = {
        contentType: "percPage",
        approvePosts: [],
        removePosts: [],
        removeStatus: 400,
        holdRemove: false,
        removeGate: Promise.resolve(),
      };
      await stubEditorApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId("editor-incremental-remove")).toBeVisible({
        timeout: 20_000,
      });
      await approveUntilBadge(page);
      await page.getByTestId("editor-incremental-remove").click();
      await page.getByTestId("editor-incremental-remove-confirm").click();
      await expect(page.getByTestId("editor-incremental-remove-error")).toContainText(
        /could not be removed/i,
      );
      await expect(page.getByTestId("editor-incremental-approved")).toBeVisible();
      await expect(page.getByTestId("editor-incremental-removed")).toHaveCount(0);

      store.removeStatus = 403;
      await page.getByTestId("editor-incremental-remove-confirm").click();
      await expect(page.getByTestId("editor-incremental-remove-error")).toContainText(
        /not allowed/i,
      );
      await expect(page.getByTestId("editor-incremental-approved")).toBeVisible();

      store.removeStatus = 409;
      await page.getByTestId("editor-incremental-remove-confirm").click();
      await expect(page.getByTestId("editor-incremental-remove-error")).toContainText(
        /blocked/i,
      );
      await expect(page.getByTestId("editor-incremental-remove-dialog")).toBeVisible();
      await expect(page.getByTestId("editor-incremental-approved")).toBeVisible();
      await expect(page.getByTestId("editor-incremental-removed")).toHaveCount(0);
      expect(store.removePosts.length).toBe(3);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "view mode does not remove",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = {
        contentType: "percPage",
        approvePosts: [],
        removePosts: [],
        removeStatus: 204,
        holdRemove: false,
        removeGate: Promise.resolve(),
      };
      await stubEditorApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.getByTestId("editor-host")).toBeVisible({ timeout: 20_000 });
      await expect(page.getByTestId("editor-incremental-remove")).toHaveCount(0);
      expect(store.removePosts).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
