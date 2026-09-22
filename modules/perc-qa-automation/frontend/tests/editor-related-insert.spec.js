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
 * EditorHost insert of an existing item into a related-content slot.
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-related-insert.spec.js}</p>
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
                  dependentId: 55,
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
  await page.route("**/assembly/slot-relationships", async (route) => {
    if (route.request().method() !== "POST") {
      await route.fallback();
      return;
    }
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
            dependentId: 55,
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

test.describe("EditorHost related content insert", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "inserts an existing item into a slot",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      await stubEditorApis(page, {});
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-insert"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-related-item"]').fill("55");
      await page.locator('[data-testid="editor-related-insert-submit"]').click();
      await expect(page.locator('[data-testid="editor-related-item-id"]')).toHaveText("55", {
        timeout: 20_000,
      });
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "maps insert 403 and does not list a new row",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      await stubEditorApis(page, { postStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-insert"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-related-item"]').fill("55");
      await page.locator('[data-testid="editor-related-insert-submit"]').click();
      await expect(page.locator('[data-testid="editor-related-insert-error"]')).toContainText(
        /not allowed to insert/i,
        { timeout: 20_000 },
      );
      await expect(page.locator('[data-testid="editor-related-item-id"]')).toHaveCount(0);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "hides insert in view mode",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      await stubEditorApis(page, {});
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-panel"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-related-insert"]')).toHaveCount(0);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );
});
