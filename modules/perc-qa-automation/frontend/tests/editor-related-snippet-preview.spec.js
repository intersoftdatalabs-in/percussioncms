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
 * EditorHost per-row snippet preview on a related slot row (#6224).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-related-snippet-preview.spec.js}</p>
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
      ],
    },
  };
}

const LOCAL = {
  count: 1,
  links: [{ type: "local", targetId: "88" }],
};

async function stubEditorApis(page, { previewStatus = 200 } = {}) {
  // Routes live on the browser context (not the page) so the snippet
  // preview popup — a separate page — is intercepted the same way.
  const ctx = page.context();
  await ctx.route("**/itemmanagement/item/fields/**", async (route) => {
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
  await ctx.route("**/itemmanagement/workflow/checkOut/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ checkOutUser: "admin", currentUser: "admin" }),
    });
  });
  await ctx.route("**/rest/editor/items/**/checkout", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ checkOutUser: "admin", currentUser: "admin" }),
    });
  });
  await ctx.route("**/itemmanagement/workflow/getTransitions/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ stateName: "Draft", transitionTriggers: [] }),
    });
  });
  await ctx.route("**/services/contenttypes/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    });
  });
  await ctx.route("**/assembly/slot-relationships**", async (route) => {
    const url = route.request().url();
    if (url.includes("/canvas")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(canvas()),
      });
      return;
    }
    await route.fallback();
  });
  await ctx.route("**/assembly/preview-location**", async (route) => {
    if (previewStatus !== 200) {
      await route.fulfill({
        status: previewStatus,
        contentType: "text/plain",
        body: "Missing",
      });
      return;
    }
    const url = new URL(route.request().url());
    const contentId = url.searchParams.get("contentId") || "55";
    const templateId = url.searchParams.get("templateId") || "7";
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        previewUrl: `/assembler/render?sys_contentid=${contentId}&sys_template=${templateId}&sys_context=0&sys_itemfilter=preview`,
        contentId: Number(contentId),
        templateId: Number(templateId),
        revision: 1,
      }),
    });
  });
  await ctx.route("**/assembler/render**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "text/html",
      body: "<!DOCTYPE html><html><body>snippet-preview</body></html>",
    });
  });
  await ctx.route("**/content-explorer/translations/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, variants: [] }),
    });
  });
  await ctx.route("**/content-explorer/relationships/**/local", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LOCAL),
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

test.describe("EditorHost related snippet preview (#6224)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "slot row opens the assembler snippet preview; inline row has none",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watch(page);
      await stubEditorApis(page, {});
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("editor-related-row")).toHaveCount(2);
      // Only the slot row (templateId 7) offers a preview; the inline
      // local link (id 88) has no snippet template to assemble.
      await expect(page.getByTestId("editor-related-preview")).toHaveCount(1);
      const popupPromise = page.waitForEvent("popup", { timeout: 15_000 });
      await page.getByTestId("editor-related-preview").click();
      const popup = await popupPromise;
      await popup.waitForLoadState("domcontentloaded");
      expect(popup.url()).toContain("/assembler/render");
      expect(popup.url()).toContain("sys_contentid=55");
      await popup.close();
      await expect(page.getByTestId("editor-related-preview-error")).toHaveCount(0);
      await expect(page.getByTestId("editor-related-panel")).toBeVisible();
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );

  test(
    "failed preview-location stays on the related panel",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = watch(page);
      await stubEditorApis(page, { previewStatus: 404 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("editor-related-preview")).toHaveCount(1);
      let popupOpened = false;
      page.once("popup", () => {
        popupOpened = true;
      });
      await page.getByTestId("editor-related-preview").click();
      await expect(page.getByTestId("editor-related-preview-error")).toBeVisible();
      expect(popupOpened).toBe(false);
      await expect(page.getByTestId("editor-related-panel")).toBeVisible();
      await expect(page.getByTestId("editor-related-list")).toBeVisible();
      await assertConsoleClean(page, pageErrors, consoleErrors);
    },
  );
});
