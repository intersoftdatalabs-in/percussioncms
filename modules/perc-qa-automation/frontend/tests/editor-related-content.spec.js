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
 * EditorHost related / inline related content list (browse).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-related-content.spec.js}</p>
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

const CANVAS = {
  SlotCanvas: {
    ownerId: 42,
    templateId: null,
    slots: [
      {
        slotId: 1,
        name: "content",
        label: "Content",
        items: [
          {
            relationshipId: 9,
            ownerId: 42,
            dependentId: 55,
            slotId: 1,
            templateId: 2,
            sortRank: 0,
          },
        ],
      },
    ],
  },
};

async function stubEditorApis(page, { canvasBody, canvasStatus, localBody, localStatus }) {
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
      status: canvasStatus ?? 200,
      contentType: "application/json",
      body:
        canvasStatus === 403
          ? "Forbidden"
          : JSON.stringify(canvasBody ?? CANVAS),
    });
  });
  await page.route("**/content-explorer/relationships/**/local", async (route) => {
    await route.fulfill({
      status: localStatus ?? 200,
      contentType: "application/json",
      body:
        localStatus === 403
          ? "Forbidden"
          : JSON.stringify(localBody ?? { count: 0, links: [] }),
    });
  });
}

async function assertConsoleClean(page, pageErrors, consoleErrors) {
  expect(pageErrors, pageErrors.join("\n")).toEqual([]);
  const relatedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/403/i.test(t),
  );
  expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
  await expectNoSeriousA11yViolations(page, {
    scope: '[data-testid="editor-host"]',
  });
}

test.describe("EditorHost related content list", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "lists related slot items",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      await stubEditorApis(page, {});
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-list"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-related-row"]')).toHaveCount(1);
      await expect(page.locator('[data-testid="editor-related-item-id"]')).toHaveText(
        "55",
      );
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "shows empty related content",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      await stubEditorApis(page, {
        canvasBody: { SlotCanvas: { ownerId: 42, templateId: null, slots: [] } },
        localBody: { count: 0, links: [] },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-empty"]')).toBeVisible({
        timeout: 20_000,
      });
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "shows forbidden when related APIs return 403",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      await stubEditorApis(page, { canvasStatus: 403, localStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-error"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-related-error"]')).toContainText(
        /not allowed/i,
      );
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );
});
