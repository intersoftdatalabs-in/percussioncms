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
 * EditorHost related-content sub-dialogs: Escape cancels and focus returns
 * to the opener (#4532 keyboard / focus-return polish).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-related-dialog-escape.spec.js}</p>
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

const TYPES = [{ name: "percPage", label: "Page" }];

function canvas() {
  return {
    SlotCanvas: {
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 9,
          name: "content",
          label: "Content",
          items: [
            {
              relationshipId: 3,
              ownerId: 42,
              dependentId: 55,
              slotId: 9,
              templateId: 7,
              sortRank: 0,
            },
          ],
        },
        {
          slotId: 11,
          name: "sidebar",
          label: "Sidebar",
          items: [],
        },
      ],
    },
  };
}

const LOCAL = {
  count: 1,
  links: [{ type: "local", targetId: "55" }],
};

async function stubEditorApis(page) {
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
  await page.route("**/itemmanagement/workflow/allowedWorkflows/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({}),
    });
  });
  await page.route("**/pathmanagement/path/item/id/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({}),
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
  await page.route("**/assembly/slot-relationships**", async (route) => {
    const url = route.request().url();
    if (url.includes("/canvas")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(canvas()),
      });
      return;
    }
    if (url.includes("/allowed-templates")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          SlotAllowedChoiceList: {
            items: [
              { id: 7, name: "brief", label: "Brief" },
              { id: 4, name: "full", label: "Full" },
            ],
          },
        }),
      });
      return;
    }
    await route.fallback();
  });
  await page.route("**/pathmanagement/path/folder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: [{ name: "About", path: "/Sites/About/", type: "folder" }],
      }),
    });
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

async function openEditor(page) {
  await stubEditorApis(page);
  await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.locator('[data-testid="editor-related-panel"]')).toBeVisible();
}

test.describe("EditorHost related sub-dialogs Escape to cancel", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "Escape closes the change-template dialog and focus returns to its opener",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watch(page);
      await openEditor(page);
      await page.getByTestId("editor-related-change-template").click();
      await expect(page.getByTestId("editor-related-template-dialog")).toBeVisible();
      await page
        .getByTestId("editor-related-template-dialog")
        .evaluate((el) => {
          el.scrollIntoView({ block: "end" });
        });
      await page.screenshot({
        path: "test-results/live-proof-4532-related-dialog-escape.png",
      });
      await page.keyboard.press("Escape");
      await expect(page.getByTestId("editor-related-template-dialog")).toHaveCount(0);
      await expect(page.getByTestId("editor-related-change-template")).toBeFocused();
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "Escape closes the slot-move dialog and focus returns to its opener",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watch(page);
      await openEditor(page);
      await page.getByTestId("editor-related-move-slot").click();
      await expect(page.getByTestId("editor-related-slot-move-dialog")).toBeVisible();
      await page.keyboard.press("Escape");
      await expect(page.getByTestId("editor-related-slot-move-dialog")).toHaveCount(0);
      await expect(page.getByTestId("editor-related-move-slot")).toBeFocused();
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "Escape closes the folder picker and focus returns to its opener",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watch(page);
      await openEditor(page);
      await page.getByTestId("editor-related-pick").click();
      await expect(page.getByTestId("editor-related-pick-dialog")).toBeVisible();
      // Escape is ignored while the picker is busy (peer hook contract);
      // wait out the folder load like a real keypress would.
      await expect(page.getByTestId("editor-related-pick-loading")).toHaveCount(0);
      await page.keyboard.press("Escape");
      await expect(page.getByTestId("editor-related-pick-dialog")).toHaveCount(0);
      await expect(page.getByTestId("editor-related-pick")).toBeFocused();
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "Escape closes the quick-add dialog and focus returns to its opener",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watch(page);
      await openEditor(page);
      await page.getByTestId("editor-related-quick-add").click();
      await expect(page.getByTestId("editor-related-quick-add-dialog")).toBeVisible();
      await page.keyboard.press("Escape");
      await expect(page.getByTestId("editor-related-quick-add-dialog")).toHaveCount(0);
      await expect(page.getByTestId("editor-related-quick-add")).toBeFocused();
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );
});
