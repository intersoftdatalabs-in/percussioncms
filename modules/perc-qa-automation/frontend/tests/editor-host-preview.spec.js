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
 * React Content Editor host — assembled Preview for the open page/asset (#4568).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @preview}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-preview.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isPageRenderPreviewUrl,
  isAssetViewUrlRequest,
  isEditorHostPreviewUrl,
  parseEditorPreviewUrl,
} = require("./helpers/editor-host-preview");

const PAGE_FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const ASSET_FIELDS = {
  ItemEditorFields: {
    contentId: "99",
    contentType: "percRichText",
    name: "Intro",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Intro" }],
  },
};

const PAGE_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

const ASSET_TYPE = {
  ContentTypeDetail: {
    name: "percRichText",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

function leftoverOn(page, leftover) {
  page.on("request", (req) => {
    const u = req.url();
    if (
      u.includes("checkoutedit.xml") ||
      u.includes("contenteditorurls.html") ||
      /view=editor/.test(u)
    ) {
      leftover.push(u);
    }
  });
}

function consoleOn(page, pageErrors) {
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      pageErrors.push(msg.text());
    }
  });
}

async function stubEditorApis(page, opts) {
  const fields = opts.fields || PAGE_FIELDS;
  const type = opts.type || PAGE_TYPE;
  const renderStatus = opts.renderStatus == null ? 200 : opts.renderStatus;
  const renderBody = opts.renderBody == null ? "<html><body>assembled</body></html>" : opts.renderBody;
  const assetViewStatus = opts.assetViewStatus == null ? 200 : opts.assetViewStatus;
  const assetViewBody =
    opts.assetViewBody == null
      ? "/Rhythmyx/assembler/render?sys_contentid=99"
      : opts.assetViewBody;
  const previewCalls = opts.previewCalls;
  const ctx = page.context();
  await ctx.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await ctx.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await ctx.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fields),
    }),
  );
  await ctx.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ path: "/Sites/Example/Home" }),
    }),
  );
  await ctx.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(type),
    }),
  );
  await ctx.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, variants: [] }),
    }),
  );
  await ctx.route("**/rest/content-explorer/relationships/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ items: [] }),
    }),
  );
  await ctx.route("**/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ SlotCanvas: { ownerId: 42, slots: [] } }),
    }),
  );
  await ctx.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName: "Draft",
          transitionTriggers: ["Submit"],
        },
      }),
    }),
  );
  await ctx.route("**/services/pagemanagement/render/page/**", async (route) => {
    previewCalls.push(route.request().url());
    await route.fulfill({
      status: renderStatus,
      contentType: "text/html",
      body: renderBody,
    });
  });
  await ctx.route("**/services/assetmanagement/asset/assetViewUrl/**", async (route) => {
    previewCalls.push(route.request().url());
    await route.fulfill({
      status: assetViewStatus,
      contentType: "text/plain",
      body: assetViewBody,
    });
  });
  await ctx.route("**/assembler/render**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "text/html",
      body: "<html><body>assembled-asset</body></html>",
    });
  });
}

test.describe("React Content Editor Preview", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "view mode opens assembled page render, not the editor host",
    { tag: ["@explorer-content-editor", "@editor", "@preview"] },
    async ({ page }) => {
      const previewCalls = [];
      const leftover = [];
      const pageErrors = [];
      leftoverOn(page, leftover);
      consoleOn(page, pageErrors);
      await stubEditorApis(page, { previewCalls });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.preview}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const popupPromise = page.waitForEvent("popup");
      await page.locator(`[data-testid="${TEST_IDS.preview}"]`).click();
      const popup = await popupPromise;
      await expect.poll(() => previewCalls.some(isPageRenderPreviewUrl)).toBe(true);
      expect(parseEditorPreviewUrl(previewCalls.find(isPageRenderPreviewUrl)).itemId).toBe(
        "42",
      );
      expect(isEditorHostPreviewUrl(popup.url())).toBe(false);
      expect(isPageRenderPreviewUrl(popup.url())).toBe(true);
      await expect(page.locator(`[data-testid="${TEST_IDS.previewDone}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.previewError}"]`)).toHaveCount(0);
      expect(leftover, `leftover CE requested: ${leftover.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );

  test(
    "edit mode previews the last saved revision after unsaved confirm",
    { tag: ["@explorer-content-editor", "@editor", "@preview"] },
    async ({ page }) => {
      const previewCalls = [];
      await stubEditorApis(page, { previewCalls });
      page.on("dialog", async (dialog) => {
        expect(dialog.type()).toBe("confirm");
        expect(dialog.message()).toMatch(/last saved revision|unsaved/i);
        await dialog.accept();
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.fieldTitle}"]`).fill("Draft title");
      const popupPromise = page.waitForEvent("popup");
      await page.locator(`[data-testid="${TEST_IDS.preview}"]`).click();
      const popup = await popupPromise;
      expect(isPageRenderPreviewUrl(popup.url())).toBe(true);
      await expect(page.locator(`[data-testid="${TEST_IDS.previewDone}"]`)).toBeVisible();
    },
  );

  test(
    "HTTP 403 is a failure, not success",
    { tag: ["@explorer-content-editor", "@editor", "@preview"] },
    async ({ page }) => {
      const previewCalls = [];
      await stubEditorApis(page, {
        previewCalls,
        renderStatus: 403,
        renderBody: JSON.stringify({ Error: { message: "FORBIDDEN" } }),
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.preview}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.preview}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.previewError}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.previewError}"]`)).toContainText(
        /FORBIDDEN|403|Forbidden/i,
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.previewDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "unknown id HTTP 404 is a failure, not success",
    { tag: ["@explorer-content-editor", "@editor", "@preview"] },
    async ({ page }) => {
      const previewCalls = [];
      await stubEditorApis(page, {
        previewCalls,
        renderStatus: 404,
        renderBody: "{}",
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.preview}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.preview}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.previewError}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.previewDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "cancel unsaved confirm does not open preview",
    { tag: ["@explorer-content-editor", "@editor", "@preview"] },
    async ({ page }) => {
      const previewCalls = [];
      await stubEditorApis(page, { previewCalls });
      page.on("dialog", async (dialog) => {
        await dialog.dismiss();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.fieldTitle}"]`).fill("Draft title");
      await page.locator(`[data-testid="${TEST_IDS.preview}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.previewDone}"]`)).toHaveCount(0);
      expect(previewCalls.filter(isPageRenderPreviewUrl)).toEqual([]);
    },
  );

  test(
    "percRichText uses asset view URL",
    { tag: ["@explorer-content-editor", "@editor", "@preview"] },
    async ({ page }) => {
      const previewCalls = [];
      await stubEditorApis(page, {
        previewCalls,
        fields: ASSET_FIELDS,
        type: ASSET_TYPE,
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=99&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.preview}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const popupPromise = page.waitForEvent("popup");
      await page.locator(`[data-testid="${TEST_IDS.preview}"]`).click();
      const popup = await popupPromise;
      await expect.poll(() => previewCalls.some(isAssetViewUrlRequest)).toBe(true);
      expect(parseEditorPreviewUrl(previewCalls.find(isAssetViewUrlRequest)).itemId).toBe(
        "99",
      );
      expect(isEditorHostPreviewUrl(popup.url())).toBe(false);
      await expect(page.locator(`[data-testid="${TEST_IDS.previewDone}"]`)).toBeVisible();
    },
  );

  test(
    "preview panel reloads the iframe for a chosen template",
    { tag: ["@explorer-content-editor", "@editor", "@preview"] },
    async ({ page }) => {
      const previewCalls = [];
      const locationCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const type = {
        ContentTypeDetail: {
          name: "percPage",
          fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
          allowedTemplates: [
            { name: "Home", label: "Home page", guid: { stringValue: "7" } },
            { name: "Blog", label: "Blog", guid: { stringValue: "8" } },
          ],
        },
      };
      await stubEditorApis(page, { previewCalls, type });
      await page.context().route("**/services/assembly/preview-location**", async (route) => {
        locationCalls.push(route.request().url());
        const url = new URL(route.request().url());
        const templateId = url.searchParams.get("templateId") || "";
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PreviewLocation: {
              previewUrl: `/assembler/render?sys_contentid=42&sys_template=${templateId}&sys_revision=1&sys_context=0&sys_itemfilter=preview`,
              contentId: 42,
              templateId: Number(templateId),
              revision: 1,
            },
          }),
        });
      });
      await page.context().route("**/assembler/render**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "text/html",
          body: "<html><body data-preview-marker=\"chosen\">assembled</body></html>",
        });
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      const select = page.locator(`[data-testid="${TEST_IDS.previewTemplate}"]`);
      await expect(select).toBeVisible({ timeout: 20_000 });
      await expect(select).toHaveValue("");
      await expect(page.locator(`[data-testid="${TEST_IDS.previewFrame}"]`)).toHaveCount(0);
      await select.selectOption("8");
      await expect.poll(() => locationCalls.some((u) => u.includes("templateId=8"))).toBe(true);
      const frame = page.locator(`[data-testid="${TEST_IDS.previewFrame}"]`);
      await expect(frame).toHaveAttribute("data-preview-template", "8");
      await expect(frame).toHaveAttribute("src", /sys_template=8/);
      await select.selectOption("");
      await expect(frame).toHaveAttribute("data-preview-template", "current");
      await expect(frame).toHaveAttribute("src", /\/pagemanagement\/render\/page\/42/);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
