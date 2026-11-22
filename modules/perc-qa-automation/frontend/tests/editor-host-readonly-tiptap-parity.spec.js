/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * React Content Editor read-only TipTap parity (#7046, #5622 slice 1/3).
 *
 * <p>View-mode HTML rows render a static schema-normalized view instead of
 * mounting an interactive ProseMirror instance. Proves the static readout
 * carries the full row contract (testids, data markers,
 * {@code contenteditable="false"}, {@code readonly}, aria wiring), mounts no
 * live editor view or toolbar, normalizes TinyMCE-era markup exactly like
 * the editing view, and passes the host axe gate with zero console errors.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-readonly-tiptap-parity.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
} = require("./helpers/editor-host-html-fields");

// TinyMCE-era stored markup: aliases, bookkeeping attrs, legacy table
// wrappers, and a Percussion more-link marker shorthand. The static view
// must show the TipTap-normalized form (strong/em, unwrapped rows), proving
// the same schema pipeline runs as in the editing view.
const STORED =
  '<p><b>Bold</b> and <i>italic</i></p>' +
  '<ul><li>one</li><li>two</li></ul>' +
  '<table><tbody><tr><td>cell</td></tr></tbody></table>' +
  '<p>Before</p><span class="perc-blog-more-link"/><p>After</p>';

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percRichText",
    name: "Intro",
    checkoutUser: "admin",
    revision: 2,
    fields: [
      { name: "sys_title", value: "Intro" },
      { name: "text", value: STORED },
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

async function stubEditorApis(page) {
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
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    }),
  );
}

test.describe("React Content Editor read-only TipTap parity", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "view mode renders the static normalized readout with no live editor",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
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
      await stubEditorApis(page);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const viewHtml = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(viewHtml).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(viewHtml).toHaveAttribute("contenteditable", "false");
      await expect(viewHtml).toHaveAttribute("readonly", "");
      await expect(viewHtml).toHaveAttribute("aria-readonly", "true");

      // No interactive editor: no ProseMirror view and no toolbar chrome in
      // the read-only row, so View mode offers no tab stop that swallows
      // arrow keys.
      const host = page.locator('[data-testid="editor-tiptap-text"]');
      await expect(host.locator(".ProseMirror")).toHaveCount(0);
      await expect(
        page.locator('[data-testid="editor-tiptap-toolbar-text"]'),
      ).toHaveCount(0);

      // Schema normalization ran (not raw stored markup): aliases resolve
      // to the StarterKit form, the list and table survive as real
      // structures, and legacy wrappers are gone.
      await expect(viewHtml.locator("strong")).toContainText("Bold");
      await expect(viewHtml.locator("em")).toContainText("italic");
      await expect(viewHtml.locator("b")).toHaveCount(0);
      await expect(viewHtml.locator("ul > li")).toHaveCount(2);
      await expect(viewHtml.locator("table td")).toContainText("cell");
      await expect(viewHtml.locator("tbody")).toHaveCount(0);
      await expect(viewHtml).toContainText("After");
      await expect(page.locator(`[data-testid="${TEST_IDS.save}"]`)).toHaveCount(0);

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );
});
