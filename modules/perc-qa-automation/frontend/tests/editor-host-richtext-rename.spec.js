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
 * React Content Editor renamed RICH_TEXT surface (#6834 slice 3/3, #7237).
 *
 * <p>Widget-builder {@code RICH_TEXT} fields emit
 * {@code sys_default_richtext_editor} since #7238 (owner rename decision
 * 2026-11-24, supersedes the #5874 freeze); the installer upgrade task in
 * #7249 migrates stored {@code sys_tinymce} ControlRefs. This spec proves a
 * fresh widget-builder content type carrying the renamed control renders the
 * bundled TipTap HTML surface in the React Content Editor with zero console
 * errors and zero {@code sys_resources/tinymce} fetches, and that bold
 * formatting round-trips through the fields API with zero drift.</p>
 *
 * <p>Legacy {@code sys_tinymce} stored types keep rendering through the same
 * TipTap mapping (dual-accept in {@code PSAssemblyItemBridge} +
 * {@code classifyEditorControl}); that alias path stays covered by
 * {@code editor-tiptap-template-content-parity.spec.js} and the
 * {@code controlKinds} unit tests.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-richtext-rename.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
} = require("./helpers/editor-host-html-fields");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percWidgetRichArticle",
    name: "Widget Article",
    checkoutUser: "admin",
    revision: 2,
    fields: [
      { name: "sys_title", value: "Widget Article" },
      { name: "text", value: "<p>Hi</p>" },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    // Fresh widget-builder type: RICH_TEXT fields emit the renamed control
    // since #7238 (PSContentTypeFileTransformer.RTE_CONTROL).
    name: "percWidgetRichArticle",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "text", label: "Body", control: "sys_default_richtext_editor" },
    ],
  },
};

async function stubEditorApis(page, { onPut } = {}) {
  // Stateful fake persistence (same shape as
  // editor-host-tiptap-authoring.spec.js): GET serves the last-saved payload
  // so the post-save reload renders what was sent — proving zero drift
  // through the round-trip, not just the request.
  const state = { current: FIELDS };
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkin", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
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
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, variants: [] }),
    }),
  );
  // Quiet the fake id 42 against the live cell: untranslated and
  // workflow-allowance lookups would otherwise log server ERRORs
  // (same empty shapes as editor-host-html-fields.spec.js).
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ choices: [] }),
    }),
  );
  await page.route("**/services/assembly/slot-relationships/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ SlotCanvas: { ownerId: 42, slots: [] } }),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
      if (typeof onPut === "function") {
        onPut(route.request());
      }
      try {
        const sent = JSON.parse(route.request().postData() || "{}");
        const payload = sent.ItemEditorFields || sent;
        state.current = {
          ItemEditorFields: { ...payload, revision: 3 },
        };
      } catch {
        state.current = FIELDS;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(state.current),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(state.current),
    });
  });
}

function watchErrors(page, tinymceRequests) {
  const pageErrors = [];
  // TipTap-only (#5902, #5734): the React editor must never fetch the legacy
  // TinyMCE script under any param.
  page.on("request", (req) => {
    if (req.url().includes("/sys_resources/tinymce/")) {
      tinymceRequests.push(req.url());
    }
  });
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (/Failed to load resource/.test(text)) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

function savedBody(putReq) {
  const body = JSON.parse(putReq.postData() || "{}");
  const payload = body.ItemEditorFields || body;
  const field = (payload.fields || []).find((f) => f.name === "text");
  return (field && field.value) || "";
}

test.describe("React Content Editor renamed RICH_TEXT surface", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "renamed control renders TipTap and saves with zero console errors",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const tinymceRequests = [];
      const pageErrors = watchErrors(page, tinymceRequests);
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible();
      // The renamed widget-builder control must classify as html/TipTap —
      // the same surface stored sys_tinymce types render through.
      const htmlField = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(htmlField).toHaveAttribute("data-editor-kind", "html");
      await expect(htmlField).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(htmlField).toHaveAttribute("contenteditable", "true");
      await expect(htmlField).toContainText("Hi");
      await htmlField.click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Bye");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      expect(savedBody(putReq)).toBe("<p>Bye</p>");
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(tinymceRequests, `tinymce fetched: ${tinymceRequests.join(" | ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
      await page.screenshot({ path: "test-results/live-proof-7237-richtext-rename.png" });
    },
  );

  test(
    "bold formatting round-trips through the fields API with zero drift",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const tinymceRequests = [];
      const pageErrors = watchErrors(page, tinymceRequests);
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toContainText("Hi");
      const toolbar = page.locator('[data-testid="editor-tiptap-toolbar-text"]');
      await expect(toolbar).toBeVisible();

      // Bold the stored paragraph: click the stored text so the caret lands
      // in its paragraph, select it with Home / Shift+End, drive bold.
      await editable.locator("text=Hi").click();
      await page.keyboard.press("Home");
      await page.keyboard.press("Shift+End");
      await page.locator('[data-testid="tiptap-bold-text"]').click();
      await expect(editable.locator("strong").first()).toContainText("Hi");

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const saved = savedBody(putReq);
      expect(saved).toContain("<strong>");
      expect(saved).toContain("Hi");
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      // The stateful stub echoes the PUT back: the reload renders the saved
      // formatting, proving zero drift (not just the request).
      await expect(editable.locator("strong").first()).toContainText("Hi");
      expect(tinymceRequests, `tinymce fetched: ${tinymceRequests.join(" | ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await page.screenshot({ path: "test-results/live-proof-7237-richtext-bold.png" });
    },
  );
});
