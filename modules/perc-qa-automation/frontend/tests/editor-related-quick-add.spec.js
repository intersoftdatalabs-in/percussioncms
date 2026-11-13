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
 * EditorHost related-content quick add: create a brand-new item from the
 * panel and attach it as related (#6264 / #4532).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-related-quick-add.spec.js}</p>
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

const TYPES = {
  ContentTypeList: {
    ContentType: [{ name: "percPage", label: "Page" }],
  },
};

const CREATED = {
  ItemCreateResult: {
    itemId: "77",
    folderPath: "/Sites/Enterprise",
    name: "New page",
    contentType: "percPage",
  },
};

function canvas(attached) {
  return {
    SlotCanvas: {
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 9,
          name: "content",
          label: "Content",
          items: attached
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

async function stubEditorShell(page) {
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
  await page.route("**/services/contenttypes", async (route) => {
    if (route.request().method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(TYPES),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/services/contenttypes/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    });
  });
  await page.route("**/content-explorer/relationships/**/local", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ count: 0, links: [] }),
    });
  });
}

async function stubCreateRoute(page, posted, createStatus) {
  await page.route("**/itemmanagement/item/create**", async (route) => {
    posted.create.push(route.request().postData() || "");
    const status = createStatus ?? 200;
    if (status === 200) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(CREATED),
      });
      return;
    }
    await route.fulfill({
      status,
      contentType: "text/plain",
      body: status === 403 ? "Forbidden" : status === 409 ? "Conflict" : "Bad request",
    });
  });
}

async function stubCanvasAndInsertRoutes(page, posted, state, insertStatus) {
  await page.route("**/assembly/slot-relationships/canvas**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(canvas(state.attached)),
    });
  });
  await page.route("**/assembly/slot-relationships", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }
    posted.insert.push(route.request().postData() || "");
    const status = insertStatus ?? 200;
    if (status === 200) {
      state.attached = true;
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
}

async function stubEditorApis(page, { createStatus, insertStatus } = {}) {
  const state = { attached: false };
  const posted = { create: [], insert: [] };
  await stubEditorShell(page);
  await stubCreateRoute(page, posted, createStatus);
  await stubCanvasAndInsertRoutes(page, posted, state, insertStatus);
  return posted;
}

async function assertConsoleClean(page, pageErrors, consoleErrors) {
  expect(pageErrors, pageErrors.join("\n")).toEqual([]);
  const relatedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/40[0349]/i.test(t),
  );
  expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
  await expectNoSeriousA11yViolations(page, {
    scope: '[data-testid="editor-related-panel"]',
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

async function openQuickAdd(page) {
  await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId("editor-related-quick-add")).toBeVisible({ timeout: 20_000 });
  await page.getByTestId("editor-related-quick-add").click();
  await expect(page.getByTestId("editor-related-quick-add-dialog")).toBeVisible();
}

test.describe("EditorHost related quick add", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "creates a new item and attaches it as related",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      const posted = await stubEditorApis(page, {});
      await openQuickAdd(page);
      await page.getByTestId("editor-related-quick-add-type").selectOption("percPage");
      await page.getByTestId("editor-related-quick-add-folder").fill("/Sites/Enterprise");
      await page.getByTestId("editor-related-quick-add-name").fill("New page");
      await page.getByTestId("editor-related-quick-add-create").click();
      await expect(page.getByTestId("editor-related-item-id")).toHaveText("77", {
        timeout: 20_000,
      });
      await expect(page.getByTestId("editor-related-quick-add-dialog")).toHaveCount(0);
      expect(posted.create.some((body) => /"contentType"\s*:\s*"percPage"/.test(body))).toBe(true);
      expect(posted.insert.some((body) => /"dependentId"\s*:\s*77/.test(body))).toBe(true);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "cancel closes without creating or attaching",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      const posted = await stubEditorApis(page, {});
      await openQuickAdd(page);
      await page.getByTestId("editor-related-quick-add-type").selectOption("percPage");
      await page.getByTestId("editor-related-quick-add-folder").fill("/Sites/Enterprise");
      await page.getByTestId("editor-related-quick-add-cancel").click();
      await expect(page.getByTestId("editor-related-quick-add-dialog")).toHaveCount(0);
      expect(posted.create).toEqual([]);
      expect(posted.insert).toEqual([]);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "create 400 stays on the panel and attaches nothing",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      const posted = await stubEditorApis(page, { createStatus: 400 });
      await openQuickAdd(page);
      await page.getByTestId("editor-related-quick-add-type").selectOption("percPage");
      await page.getByTestId("editor-related-quick-add-folder").fill("/Sites/Enterprise");
      await page.getByTestId("editor-related-quick-add-name").fill("New page");
      await page.getByTestId("editor-related-quick-add-create").click();
      await expect(page.getByTestId("editor-related-quick-add-error")).toContainText(
        /could not be created/i,
        { timeout: 20_000 },
      );
      await expect(page.getByTestId("editor-related-panel")).toBeVisible();
      await expect(page.getByTestId("editor-related-item-id")).toHaveCount(0);
      expect(posted.insert).toEqual([]);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );
});
