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
 * EditorHost move a related slot row to another slot (#6225).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-related-slot-move.spec.js}</p>
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

function canvas(slotId, templateId) {
  return {
    SlotCanvas: {
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 9,
          name: "content",
          label: "Content",
          items:
            slotId === 9
              ? [
                  {
                    relationshipId: 3,
                    ownerId: 42,
                    dependentId: 55,
                    slotId: 9,
                    templateId,
                    sortRank: 0,
                  },
                ]
              : [],
        },
        {
          slotId: 11,
          name: "sidebar",
          label: "Sidebar",
          items:
            slotId === 11
              ? [
                  {
                    relationshipId: 3,
                    ownerId: 42,
                    dependentId: 55,
                    slotId: 11,
                    templateId,
                    sortRank: 0,
                  },
                ]
              : [],
        },
      ],
    },
  };
}

const LOCAL = {
  count: 1,
  links: [{ type: "local", targetId: "88" }],
};

async function stubEditorApis(page, { writeStatus } = {}) {
  let slotId = 9;
  const templateId = 7;
  const posts = [];
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
        body: JSON.stringify(canvas(slotId, templateId)),
      });
      return;
    }
    if (method === "POST" && url.includes("/template-slot")) {
      const status = writeStatus ?? 200;
      posts.push(route.request().postData() || "");
      if (status !== 200) {
        await route.fulfill({
          status,
          contentType: "text/plain",
          body: status === 403 ? "Forbidden" : status === 400 ? "Bad" : "Missing",
        });
        return;
      }
      slotId = 11;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          SlotRelationship: {
            relationshipId: 3,
            ownerId: 42,
            dependentId: 55,
            slotId: 11,
            templateId: 7,
            sortRank: 0,
          },
        }),
      });
      return;
    }
    await route.fallback();
  });
  await page.route("**/content-explorer/translations/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, variants: [] }),
    });
  });
  await page.route("**/content-explorer/relationships/**/local", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LOCAL),
    });
  });
  return posts;
}

async function assertConsoleClean(page, pageErrors, consoleErrors) {
  expect(pageErrors, pageErrors.join("\n")).toEqual([]);
  const relatedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/40[034]/i.test(t),
  );
  expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
  await expectNoSeriousA11yViolations(page, {
    scope: '[data-testid="editor-related-panel"]',
  });
}

function watch(page) {
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

test.describe("EditorHost related slot move", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "apply posts template-slot with the destination slot and cancel does not",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watch(page);
      const posts = await stubEditorApis(page, {});
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator('[data-testid="editor-related-move-slot"]')).toHaveCount(1);
      await expect(page.locator('[data-testid="editor-related-row"]')).toHaveCount(2);
      await page.getByTestId("editor-related-move-slot").click();
      await expect(page.getByTestId("editor-related-slot-move-dialog")).toBeVisible();
      await expect(page.getByTestId("editor-related-slot-move-select")).toContainText("Sidebar");
      await page.getByTestId("editor-related-slot-move-cancel").click();
      await expect(page.getByTestId("editor-related-slot-move-dialog")).toHaveCount(0);
      expect(posts).toEqual([]);
      await page.getByTestId("editor-related-move-slot").click();
      await page.getByTestId("editor-related-slot-move-select").selectOption("11");
      await page.getByTestId("editor-related-slot-move-apply").click();
      await expect(page.getByTestId("editor-related-slot-move-saved")).toBeVisible();
      expect(posts.length).toBe(1);
      expect(posts[0]).toContain('"slotId":11');
      expect(posts[0]).toContain('"templateId":7');
      await expect(page.getByTestId("editor-related-panel")).toBeVisible();
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "forbidden slot move stays on the related panel",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watch(page);
      await stubEditorApis(page, { writeStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await page.getByTestId("editor-related-move-slot").click();
      await page.getByTestId("editor-related-slot-move-select").selectOption("11");
      await page.getByTestId("editor-related-slot-move-apply").click();
      await expect(page.getByTestId("editor-related-slot-move-error")).toContainText(
        /not allowed to move/i,
      );
      await expect(page.getByTestId("editor-related-panel")).toBeVisible();
      await expect(page.getByTestId("editor-related-list")).toBeVisible();
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );
});
