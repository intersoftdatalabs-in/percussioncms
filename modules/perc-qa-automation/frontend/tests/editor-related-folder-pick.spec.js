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
 * EditorHost pick a related page or asset from a folder (#4891 / #4532).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-related-folder-pick.spec.js}</p>
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

function canvas(withItem) {
  return {
    SlotCanvas: {
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 9,
          name: "content",
          label: "Content",
          items: withItem
            ? [
                {
                  relationshipId: 3,
                  ownerId: 42,
                  dependentId: 77,
                  slotId: 9,
                  templateId: 7,
                  sortRank: 0,
                },
              ]
            : [],
        },
      ],
    },
  };
}

async function stubEditorApis(page, { postStatus } = {}) {
  let inserted = false;
  const posted = [];
  await page.route("**/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(FIELDS),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/itemmanagement/workflow/checkOut/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ checkOutUser: "admin", currentUser: "admin" }),
    });
  });
  await page.route("**/rest/editor/items/**/checkout", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ checkOutUser: "admin", currentUser: "admin" }),
    });
  });
  await page.route("**/itemmanagement/workflow/getTransitions/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ stateName: "Draft", transitionTriggers: [] }),
    });
  });
  await page.route("**/services/contenttypes/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    });
  });
  await page.route("**/assembly/slot-relationships/canvas**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(canvas(inserted)),
    });
  });
  await page.route("**/pathmanagement/path/folder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: [
          { name: "About", path: "/Sites/About/", type: "folder" },
          {
            id: "1-101-77",
            name: "About page",
            path: "/Sites/About/index",
            type: "page",
          },
        ],
      }),
    });
  });
  await page.route("**/assembly/slot-relationships", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }
    posted.push(route.request().postData() || "");
    const status = postStatus ?? 200;
    if (status === 200) {
      inserted = true;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          SlotRelationship: {
            relationshipId: 3,
            ownerId: 42,
            dependentId: 77,
            slotId: 9,
            templateId: 7,
            sortRank: 0,
          },
        }),
      });
      return;
    }
    await route.fulfill({
      status,
      contentType: "text/plain",
      body: status === 403 ? "Forbidden" : status === 404 ? "Not found" : "Bad request",
    });
  });
  await page.route("**/content-explorer/relationships/**/local", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ count: 0, links: [] }),
    });
  });
  return posted;
}

async function assertConsoleClean(page, pageErrors, consoleErrors) {
  expect(pageErrors, pageErrors.join("\n")).toEqual([]);
  const relatedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/40[034]/i.test(t),
  );
  expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
  await expectNoSeriousA11yViolations(page, {
    scope: '[data-testid="editor-host"]',
  });
}

function watchErrors(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

test.describe("EditorHost related folder pick", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "posts the content id chosen from a folder",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      const posted = await stubEditorApis(page, {});
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("editor-related-pick")).toBeVisible({ timeout: 20_000 });
      await page.getByTestId("editor-related-pick").click();
      await expect(page.getByTestId("editor-related-pick-dialog")).toBeVisible();
      await page.getByTestId("editor-related-pick-item").click();
      await page.getByTestId("editor-related-pick-use").click();
      await expect(page.getByTestId("editor-related-pick-dialog")).toHaveCount(0);
      await expect(page.getByTestId("editor-related-item")).toHaveValue("77");
      await page.getByTestId("editor-related-insert-submit").click();
      await expect(page.getByTestId("editor-related-item-id")).toHaveText("77", {
        timeout: 20_000,
      });
      expect(posted.some((body) => /"dependentId"\s*:\s*77/.test(body))).toBe(true);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "cancel does not insert and keeps a typed id",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      const posted = await stubEditorApis(page, {});
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("editor-related-item")).toBeVisible({ timeout: 20_000 });
      await page.getByTestId("editor-related-item").fill("55");
      await page.getByTestId("editor-related-pick").click();
      await expect(page.getByTestId("editor-related-pick-item")).toBeVisible();
      await page.getByTestId("editor-related-pick-cancel").click();
      await expect(page.getByTestId("editor-related-pick-dialog")).toHaveCount(0);
      await expect(page.getByTestId("editor-related-item")).toHaveValue("55");
      expect(posted).toEqual([]);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "insert 400 after a folder pick stays on the related panel",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      await stubEditorApis(page, { postStatus: 400 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("editor-related-pick")).toBeVisible({ timeout: 20_000 });
      await page.getByTestId("editor-related-pick").click();
      await page.getByTestId("editor-related-pick-item").click();
      await page.getByTestId("editor-related-pick-use").click();
      await page.getByTestId("editor-related-insert-submit").click();
      await expect(page.getByTestId("editor-related-insert-error")).toContainText(
        /could not be inserted/i,
        { timeout: 20_000 },
      );
      await expect(page.getByTestId("editor-related-panel")).toBeVisible();
      await expect(page.getByTestId("editor-related-item-id")).toHaveCount(0);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );
});
