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
 * Template-content TipTap render parity on live H2 (#7048, #5622 slice 3/3).
 *
 * <p>Fields configured with the legacy {@code sys_tinymce} control name (as
 * the audited template content editors still declare them — FastForward
 * shared {@code callout}/{@code body}, press-release {@code pr_summary})
 * render through the bundled TipTap pipeline in the React Content Editor.
 * Proves the stored template shapes (marks, managed inline links, inline
 * images, global variables, more-link splits, inline-variant links) display
 * identically to the TipTap editing serialization with no live editor
 * mounted in view mode, zero console errors, and a clean host axe gate.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-tiptap-template-content-parity.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
} = require("./helpers/editor-host-html-fields");

// Stored values shaped after the audited template configs: TinyMCE-era
// markup plus the Percussion stored constructs those fields carry (managed
// inline link CMS attributes, inline image, global-variable marker,
// more-link marker, inline-variant wrapper with marker images).
const CALLOUT =
  "<p><strong>Callout:</strong> Read <em>the release notes</em> and see " +
  '<a href="https://example.com/notes">what changed</a>.</p>';

const BODY =
  "<h2>Highlights</h2>" +
  "<p>Ship <strong>TipTap</strong> with <em>zero drift</em>.</p>" +
  "<ul><li>First</li><li>Second</li></ul>" +
  '<p>See <a href="/Rhythmyx/Sites/a.html" title="About" rxinlineslot="103" ' +
  'inlinetype="rxhyperlink" sys_dependentid="101" sys_dependentvariantid="201" ' +
  'rxselectedtext="About">About</a> and ' +
  '<span class="perc-global-variable-marker" title="company_name">Acme</span>.</p>' +
  '<p><img src="https://example.com/i.png" alt="Pic" title="T" width="320" ' +
  'height="200" rxinlineslot="104" inlinetype="rximage" ' +
  'sys_dependentid="9" sys_dependentvariantid="8"></p>';

const SUMMARY =
  "<p>Teaser<span " +
  'class="perc-blog-more-link"></span> continued after the fold.</p>' +
  '<p><span class="rx_inlinevariant" rxinlineslot="105" ' +
  'inlinetype="rxvariant" sys_dependentid="77">' +
  '<img alt="@inlinemarker@" width="9" height="8" src="../sys_resources/images/inlinestart.png"/>' +
  '<a href="/Sites/Enterprise/about">About</a>' +
  '<img alt="@inlinemarker@" width="9" height="8" src="../sys_resources/images/inlineend.png"/>' +
  "</span> follows.</p>";

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percRichText",
    name: "Intro",
    checkoutUser: "admin",
    revision: 2,
    fields: [
      { name: "sys_title", value: "Intro" },
      { name: "callout", value: CALLOUT },
      { name: "body", value: BODY },
      { name: "pr_summary", value: SUMMARY },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percRichText",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "callout", label: "Callout", control: "sys_tinymce" },
      { name: "body", label: "Body", control: "sys_tinymce" },
      { name: "pr_summary", label: "Summary", control: "sys_tinymce" },
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

test.describe("template-content TipTap render parity", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "view mode renders template-configured html fields through TipTap",
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

      for (const name of ["callout", "body", "pr_summary"]) {
        const row = page.locator(`[data-testid="editor-field-${name}"]`);
        await expect(row).toHaveAttribute("data-editor-engine", "tiptap");
        await expect(row).toHaveAttribute("contenteditable", "false");
        // No interactive editor mounted for read-only template rows.
        await expect(
          page.locator(`[data-testid="editor-tiptap-${name}"] .ProseMirror`),
        ).toHaveCount(0);
      }

      // Callout: marks and ordinary link render in TipTap form.
      const callout = page.locator('[data-testid="editor-field-callout"]');
      await expect(callout.locator("strong")).toContainText("Callout:");
      await expect(callout.locator("em")).toContainText("the release notes");
      await expect(callout.locator("b")).toHaveCount(0);
      await expect(callout.locator('a[href="https://example.com/notes"]')).toContainText(
        "what changed",
      );

      // Body: heading, list, managed link text, variable value, and the
      // inline image render; no raw managed-link bookkeeping leaks.
      const body = page.locator('[data-testid="editor-field-body"]');
      await expect(body.locator("h2")).toContainText("Highlights");
      await expect(body.locator("ul > li")).toHaveCount(2);
      await expect(body).toContainText("About");
      await expect(body).toContainText("Acme");
      await expect(body.locator('img[alt="Pic"]')).toHaveCount(1);

      // Summary: more-link continuation and the inline-variant link render.
      // The wrapper carries the inline-template relationship
      // (rxinlineslot/inlinetype/sys_dependentid): dropping it would drop
      // the relationship, so it must survive the render.
      const summary = page.locator('[data-testid="editor-field-pr_summary"]');
      await expect(summary).toContainText("continued after the fold");
      await expect(summary.locator('a[href="/Sites/Enterprise/about"]')).toContainText(
        "About",
      );
      await expect(summary.locator('img[alt="@inlinemarker@"]')).toHaveCount(2);
      const variant = summary.locator("span.rx_inlinevariant");
      await expect(variant).toHaveCount(1);
      await expect(variant).toHaveAttribute("rxinlineslot", "105");
      await expect(variant).toHaveAttribute("inlinetype", "rxvariant");
      await expect(variant).toHaveAttribute("sys_dependentid", "77");

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );
});
