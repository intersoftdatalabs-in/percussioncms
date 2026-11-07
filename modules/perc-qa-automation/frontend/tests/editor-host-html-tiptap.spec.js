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
 * React Content Editor TipTap toolbar parity (#5651).
 *
 * <p>Proves the bundled TipTap editor ({@code ?engine=tiptap}) matches the
 * {@code rx_default_config.json} toolbar contract — lists, link, code,
 * tables (plus undo/redo, inline styles, alignment) — and reduces Word
 * pastes to the {@code paste_word_valid_elements} contract. Toolbar edits
 * persist through the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} path. Default
 * engine stays TinyMCE; spike coverage (#5650) lives in the tests below
 * alongside the parity tests.</p>
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

test.describe("React Content Editor TipTap editor", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  function watchErrors(page) {
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
    return pageErrors;
  }

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
      // Host scope covers the translations panel too: its light-surface
      // text colors meet WCAG 2.2 AA on the panel's white background (#5657).
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
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

  test(
    "tiptap toolbar parity: lists, link, code, tables persist via PUT",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchErrors(page);
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit&engine=tiptap"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toContainText("Hi");
      const toolbar = page.locator('[data-testid="editor-tiptap-toolbar-text"]');
      await expect(toolbar).toBeVisible();

      // Tables: insert at the (start) caret, table lands above the paragraph.
      await page.locator('[data-testid="tiptap-table-insert-text"]').click();
      await expect(editable.locator("table").first()).toBeVisible();

      // Bold + link + bullet list on the stored paragraph. Click the stored
      // text first so the caret lands in its paragraph, then select it with
      // Home / Shift+End (caret-exact, no whole-document select that would
      // also catch the table above).
      await editable.locator("text=Hi").click();
      await page.keyboard.press("Home");
      await page.keyboard.press("Shift+End");
      await page.locator('[data-testid="tiptap-bold-text"]').click();
      await page.locator('[data-testid="tiptap-link-text"]').click();
      const linkInput = page.locator('[data-testid="tiptap-link-url-text"]');
      await linkInput.fill("https://example.com/x");
      // Await the React state round-trip: Apply reads the input state, so
      // clicking before the render lands would apply a stale address.
      await expect(linkInput).toHaveValue("https://example.com/x");
      await page.locator('[data-testid="tiptap-link-apply-text"]').click();
      await page.locator('[data-testid="tiptap-bullist-text"]').click();
      const listHtml = await editable.innerHTML();
      expect(listHtml).toContain("<ul>");
      expect(listHtml).toContain("<strong>");
      expect(listHtml).toContain('href="https://example.com/x"');

      // Code: leave the list, type a line, style it as a code block.
      // Collapse the list-item selection first: Enter on a range selection
      // would delete it (empty list item), so ArrowRight collapses to the
      // trailing edge before moving to the document end.
      await page.keyboard.press("ArrowRight");
      await page.keyboard.press("ControlOrMeta+End");
      await page.keyboard.press("Enter");
      await page.keyboard.press("Enter");
      await page.keyboard.type("var x = 1;");
      await page.keyboard.press("Shift+Home");
      await page.locator('[data-testid="tiptap-code-block-text"]').click();
      await expect(editable.locator("pre").first()).toContainText("var x = 1;");
      // Pin the list item before saving: the PUT below must carry exactly
      // this DOM state, so fail here (not on the PUT) if a keystroke ever
      // disturbs the list again.
      await expect(editable.locator("li").first()).toContainText("Hi");

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const text = (payload.fields || []).find((f) => f.name === "text");
      const saved = (text && text.value) || "";
      // TipTap 3 serializes column bookkeeping on the table element.
      expect(saved).toContain("<table");
      expect(saved).toContain("<ul>");
      expect(saved).toContain("<strong>");
      expect(saved).toContain('href="https://example.com/x"');
      expect(saved).toContain("<pre><code>var x = 1;</code></pre>");
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "tiptap percussion plugins persist via PUT",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchErrors(page);
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit&engine=tiptap"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toContainText("Hi");
      const percBar = page.locator('[data-testid="editor-tiptap-perc-text"]');
      await expect(percBar).toBeVisible();

      // More link: one click inserts the stored marker span.
      await page.locator('[data-testid="tiptap-perc-more-link-text"]').click();
      await expect(editable.locator(".perc-blog-more-link").first()).toBeVisible();

      // Insert HTML fragment dialog (rxinserthtml port).
      await page.locator('[data-testid="tiptap-perc-insert-html-text"]').click();
      const htmlInput = page.locator('[data-testid="tiptap-perc-html-text"]');
      await htmlInput.fill("<p>Fragment <strong>bold</strong></p>");
      await expect(htmlInput).toHaveValue("<p>Fragment <strong>bold</strong></p>");
      await page.locator('[data-testid="tiptap-perc-apply-text"]').click();
      await expect(editable).toContainText("Fragment");

      // Global variable dialog (manual name; the QA host injects no CMS table).
      await page.locator('[data-testid="tiptap-perc-global-variable-text"]').click();
      const varInput = page.locator('[data-testid="tiptap-perc-variable-name-text"]');
      await varInput.fill("company_name");
      await expect(varInput).toHaveValue("company_name");
      await page.locator('[data-testid="tiptap-perc-apply-text"]').click();
      const variableBadge = editable.locator(".perc-global-variable-marker").first();
      await expect(variableBadge).toBeVisible();
      await expect(variableBadge).toContainText("$company_name");

      // Inline link dialog (rxinline/percadvlink port): stored CMS attributes.
      await page.locator('[data-testid="tiptap-perc-inline-link-text"]').click();
      const linkUrl = page.locator('[data-testid="tiptap-perc-link-url-text"]');
      await linkUrl.fill("/Rhythmyx/Sites/a.html");
      await expect(linkUrl).toHaveValue("/Rhythmyx/Sites/a.html");
      const linkText = page.locator('[data-testid="tiptap-perc-link-text-text"]');
      await linkText.fill("About");
      await expect(linkText).toHaveValue("About");
      await page.locator('[data-testid="tiptap-perc-apply-text"]').click();
      const saved = await editable.innerHTML();
      // The live cell's OWASP CSRFGuard appends ?OWASP-CSRFTOKEN= to
      // relative hrefs in the DOM, so match the path prefix, not the exact
      // attribute (exact stored shape is locked by the Vitest suite).
      expect(saved).toContain('href="/Rhythmyx/Sites/a.html');
      expect(saved).toContain('rxinlineslot="103"');
      expect(saved).toContain('inlinetype="rxhyperlink"');

      // Inline image dialog (rxinline/percadvimage port).
      await page.locator('[data-testid="tiptap-perc-inline-image-text"]').click();
      const imgSrc = page.locator('[data-testid="tiptap-perc-image-src-text"]');
      await imgSrc.fill("/Rhythmyx/Assets/i.png");
      await expect(imgSrc).toHaveValue("/Rhythmyx/Assets/i.png");
      const imgAlt = page.locator('[data-testid="tiptap-perc-image-alt-text"]');
      await imgAlt.fill("Pic");
      await page.locator('[data-testid="tiptap-perc-apply-text"]').click();
      const withImage = await editable.innerHTML();
      expect(withImage).toContain('src="/Rhythmyx/Assets/i.png"');
      expect(withImage).toContain('inlinetype="rximage"');

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const text = (payload.fields || []).find((f) => f.name === "text");
      const persisted = (text && text.value) || "";
      expect(persisted).toContain("perc-blog-more-link");
      expect(persisted).toContain("Fragment");
      expect(persisted).toContain("perc-global-variable-marker");
      expect(persisted).toContain('title="company_name"');
      expect(persisted).toContain('rxinlineslot="103"');
      expect(persisted).toContain('inlinetype="rxhyperlink"');
      expect(persisted).toContain('inlinetype="rximage"');
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "tiptap paste from Word sanitizes to the paste contract",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchErrors(page);
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit&engine=tiptap"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toContainText("Hi");

      // Synthetic Word paste: mso classes/styles, o:p, a style block, and
      // an unsafe link. ProseMirror reads clipboardData off the paste event.
      await editable.click();
      await page.keyboard.press("ControlOrMeta+a");
      await editable.evaluate((el) => {
        const word =
          '<p class="MsoNormal" style="mso-x: 1;"><b>Pasted</b><o:p></o:p></p>' +
          '<style>p.MsoNormal { mso-x: 2; }</style>' +
          '<p class="MsoNormal">Bad <a href="javascript:alert(1)">link</a></p>';
        const dt = new DataTransfer();
        dt.setData("text/html", word);
        const event = new Event("paste", { bubbles: true, cancelable: true });
        event.clipboardData = dt;
        el.dispatchEvent(event);
      });
      await expect(editable).toContainText("Pasted");
      const pastedHtml = await editable.innerHTML();
      expect(pastedHtml).not.toContain("o:p");
      expect(pastedHtml).not.toContain("mso-");
      expect(pastedHtml).not.toContain("javascript:");
      expect(pastedHtml).toContain("Pasted");

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const text = (payload.fields || []).find((f) => f.name === "text");
      const saved = (text && text.value) || "";
      expect(saved).toContain("Pasted");
      expect(saved).not.toContain("o:p");
      expect(saved).not.toContain("mso-");
      expect(saved).not.toContain("javascript:");
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
