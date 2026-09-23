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
 * EditorHost reorder of a related-content slot association.
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-related-reorder.spec.js}</p>
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

function item(relationshipId, dependentId, sortRank) {
  return {
    relationshipId,
    ownerId: 42,
    dependentId,
    slotId: 9,
    templateId: 7,
    sortRank,
  };
}

function canvas(order) {
  const items =
    order === "swapped"
      ? [item(4, 66, 0), item(3, 55, 1)]
      : [item(3, 55, 0), item(4, 66, 1)];
  return {
    SlotCanvas: {
      ownerId: 42,
      templateId: 7,
      slots: [{ slotId: 9, name: "content", label: "Content", items }],
    },
  };
}

async function stubEditorApis(page, { moveStatus } = {}) {
  let swapped = false;
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
  await page.route("**/assembly/slot-relationships**", async (route) => {
    const url = route.request().url();
    const method = route.request().method();
    if (url.includes("/canvas")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(canvas(swapped ? "swapped" : "original")),
      });
      return;
    }
    if (method === "POST" && url.includes("/move")) {
      const status = moveStatus ?? 204;
      if (status === 204) {
        swapped = true;
        await route.fulfill({ status: 204, body: "" });
        return;
      }
      await route.fulfill({
        status,
        contentType: "text/plain",
        body: "move failed",
      });
      return;
    }
    await route.fallback();
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
    (t) => !/Failed to load resource/i.test(t) && !/40[0349]/i.test(t),
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

async function itemIds(page) {
  return page.locator('[data-testid="editor-related-item-id"]').allTextContents();
}

test.describe("EditorHost related content reorder", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "moves a slot association down and reloads that order",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      await stubEditorApis(page, {});
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-move-down"]')).toBeVisible({
        timeout: 20_000,
      });
      expect(await itemIds(page)).toEqual(["55", "66"]);
      await page.locator('[data-testid="editor-related-move-down"]').click();
      await expect
        .poll(async () => itemIds(page), { timeout: 20_000 })
        .toEqual(["66", "55"]);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "maps reorder 403 and keeps the order",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      await stubEditorApis(page, { moveStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-move-down"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-related-move-down"]').click();
      await expect(page.locator('[data-testid="editor-related-move-error"]')).toContainText(
        /not allowed to reorder/i,
        { timeout: 20_000 },
      );
      expect(await itemIds(page)).toEqual(["55", "66"]);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "maps reorder 404 and keeps the order",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      await stubEditorApis(page, { moveStatus: 404 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await page.locator('[data-testid="editor-related-move-down"]').click();
      await expect(page.locator('[data-testid="editor-related-move-error"]')).toContainText(
        /not found/i,
        { timeout: 20_000 },
      );
      expect(await itemIds(page)).toEqual(["55", "66"]);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "maps reorder 409 and keeps the order",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watchErrors(page);
      await stubEditorApis(page, { moveStatus: 409 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await page.locator('[data-testid="editor-related-move-down"]').click();
      await expect(page.locator('[data-testid="editor-related-move-error"]')).toContainText(
        /not checked out/i,
        { timeout: 20_000 },
      );
      expect(await itemIds(page)).toEqual(["55", "66"]);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "hides reorder in view mode",
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
      expect(await itemIds(page)).toEqual(["55", "66"]);
      await expect(page.locator('[data-testid="editor-related-move-up"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-related-move-down"]')).toHaveCount(0);
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );
});
