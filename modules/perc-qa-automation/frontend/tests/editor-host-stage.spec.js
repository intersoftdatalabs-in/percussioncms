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
 * React Content Editor host — Stage the open page/asset (#4915).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @publish}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-stage.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isPageStageUrl,
  isResourceStageUrl,
  parseStageUrl,
} = require("./helpers/editor-host-stage");

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

const TEMPLATE_FIELDS = {
  ItemEditorFields: {
    contentId: "7",
    contentType: "percTemplate",
    name: "Base",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Base" }],
  },
};

const PAGE_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
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
  const stageStatus = opts.stageStatus || 200;
  const stageBody = opts.stageBody || "{}";
  const stageCalls = opts.stageCalls;
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
      body: JSON.stringify(fields),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(type),
    }),
  );
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
  );
  await page.route("**/services/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ path: "/Sites/Home" }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ workflows: [] }),
    }),
  );
  await page.route("**/services/assembly/slot-relationships/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
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
  await page.route("**/services/sitemanage/publish/**/staging/**", async (route) => {
    stageCalls.push({ url: route.request().url(), method: route.request().method() });
    await route.fulfill({
      status: stageStatus,
      contentType: "application/json",
      body: stageBody,
    });
  });
}

test.describe("React Content Editor Stage", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode confirms then stages a percPage",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const stageCalls = [];
      const leftover = [];
      const pageErrors = [];
      leftoverOn(page, leftover);
      consoleOn(page, pageErrors);
      await stubEditorApis(page, { stageCalls });
      page.on("dialog", async (dialog) => {
        expect(dialog.message()).toMatch(/Stage this item/i);
        await dialog.accept();
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.stage}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.stage}"]`).click();
      await expect.poll(() => stageCalls.length).toBe(1);
      expect(stageCalls[0].method).toBe("GET");
      expect(isPageStageUrl(stageCalls[0].url)).toBe(true);
      expect(parseStageUrl(stageCalls[0].url)).toEqual({ kind: "page", itemId: "42" });
      await expect(page.locator(`[data-testid="${TEST_IDS.stageDone}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.stageError}"]`)).toHaveCount(0);
      expect(leftover, `leftover CE requested: ${leftover.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-overlay"]',
      });
    },
  );

  test(
    "cancel confirm does not stage",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const stageCalls = [];
      await stubEditorApis(page, { stageCalls });
      page.on("dialog", async (dialog) => {
        await dialog.dismiss();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.stage}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.stage}"]`).click();
      expect(stageCalls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.stageDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "HTTP 403 stays on the host and is not success",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const stageCalls = [];
      await stubEditorApis(page, { stageCalls, stageStatus: 403, stageBody: "no" });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.stage}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.stage}"]`).click();
      await expect.poll(() => stageCalls.length).toBe(1);
      await expect(page.locator(`[data-testid="${TEST_IDS.stageError}"]`)).toContainText(
        /Could not stage this item/i,
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.stageDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "HTTP 400 stays on the host and is not success",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const stageCalls = [];
      await stubEditorApis(page, { stageCalls, stageStatus: 400, stageBody: "bad" });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.stage}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.stage}"]`).click();
      await expect.poll(() => stageCalls.length).toBe(1);
      await expect(page.locator(`[data-testid="${TEST_IDS.stageError}"]`)).toContainText(
        /Could not stage this item/i,
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.stageDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "a template does not offer Stage",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const stageCalls = [];
      await stubEditorApis(page, { stageCalls, fields: TEMPLATE_FIELDS });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=7&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.stage}"]`)).toHaveCount(0);
      expect(stageCalls).toEqual([]);
    },
  );

  test(
    "percRichText stages via resource/staging",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const stageCalls = [];
      await stubEditorApis(page, { stageCalls, fields: ASSET_FIELDS });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=99&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.stage}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.stage}"]`).click();
      await expect.poll(() => stageCalls.length).toBe(1);
      expect(isResourceStageUrl(stageCalls[0].url)).toBe(true);
      expect(parseStageUrl(stageCalls[0].url)).toEqual({
        kind: "resource",
        itemId: "99",
      });
    },
  );
});
