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
 * React Content Editor TipTap spike validation (#5650).
 *
 * <p>Proves the bundled TipTap hello-world ({@code ?engine=tiptap}) renders
 * stored HTML, accepts typed edits, and persists through the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} path with the same
 * HTML the TinyMCE widget would send. Default engine stays TinyMCE; this
 * spec is the only {@code engine=tiptap} coverage until parity lands
 * (#5651).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-html-tiptap.spec.js}</p>
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
    contentType: "percRichText",
    name: "Intro",
    checkoutUser: "admin",
    revision: 2,
    fields: [
      { name: "sys_title", value: "Intro" },
      { name: "text", value: "<p>Hi</p>" },
    ],
  },
};

const SAVED = {
  ItemEditorFields: {
    ...FIELDS.ItemEditorFields,
    revision: 3,
    fields: [
      { name: "sys_title", value: "Intro" },
      { name: "text", value: "<p>Bye</p>" },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percRichText",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "text", label: "Body", control: "sys_tinymce" },
    ],
  },
};

async function stubEditorApis(page, { onPut } = {}) {
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
  // Unstubbed, these hit the fake id 42 for real: translations renders an
  // "Item not found" alert (axe contrast failure) and allowedWorkflows logs
  // a server ERROR that fails qa-health. Empty shapes keep the host quiet.
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, variants: [] }),
    }),
  );
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
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(SAVED),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    });
  });
}

test.describe("React Content Editor TipTap spike", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "tiptap engine renders, edits, and saves HTML",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = [];
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
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit&engine=tiptap"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible();
      const tiptapHost = page.locator('[data-testid="editor-tiptap-text"]');
      await expect(tiptapHost).toBeVisible();
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toHaveAttribute("data-editor-kind", "html");
      await expect(editable).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(editable).toHaveAttribute("contenteditable", "true");
      await expect(editable).toContainText("Hi");

      // Real-user path: a mouse click must focus the surface without
      // clearing it. The field row renders inside an implicit <label>, and
      // the widget cancels the click default so the label cannot forward
      // the click to the Clear button (which used to wipe the field).
      await editable.click();
      await expect(editable).toContainText("Hi");
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Bye");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const text = (payload.fields || []).find((f) => f.name === "text");
      expect(text && text.value).toBe("<p>Bye</p>");
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      // Scoped to the editor form: the host-wide scope also covers the
      // pre-existing translations panel, whose theme contrast fails on this
      // stack for the default-engine spec too (not spike-introduced).
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.form}"]`,
      });
    },
  );

  test(
    "tiptap view mode is read-only",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      await stubEditorApis(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view&engine=tiptap"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const viewHtml = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(viewHtml).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(viewHtml).toHaveAttribute("contenteditable", "false");
      await expect(viewHtml).toHaveAttribute("readonly", "");
      await expect(page.locator(`[data-testid="${TEST_IDS.save}"]`)).toHaveCount(0);
    },
  );
});
