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
 * React Content Editor refuses a TipTap rich-text NUL on save (#5696 / #4532).
 *
 * <p>The TipTap editor reports ProseMirror HTML
 * through the same {@code onChange} contract as the TinyMCE textarea, so the
 * save-side HTML NUL gate ({@code htmlField.ts}, shipped for TinyMCE in
 * #5389) also covers TipTap rows. Two widget details make that true: the
 * mount-time content/editable sync stays silent (TipTap v3 emits updates on
 * {@code setContent}/{@code setEditable} by default), so a stored NUL still
 * reaches the gate instead of being normalized away; ProseMirror strips a
 * NUL out of parsed markup, while a plain-text insert (for example the
 * insert-HTML-fragment dialog) carries it into the row value the gate reads.
 * A keyboard cannot type U+0000, so the spec sets the fragment textarea
 * through the DOM setter and an {@code input} event.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-html-tiptap-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
} = require("./helpers/editor-host-html-fields");

const PREVIOUS = "<p>Hi</p>";
const ORDINARY = "<p>Bye</p>";
const NUL = String.fromCharCode(0);
const NUL_VALUE = `<p>bad${NUL}value</p>`;

const TYPE = {
  ContentTypeDetail: {
    name: "percRichText",
    fields: [{ name: "text", label: "Body", control: "sys_tinymce" }],
  },
};

/**
 * Controlled textarea update, including U+0000. Code points cross the
 * Playwright bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setTextareaValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload);
    const proto = Object.getOwnPropertyDescriptor(
      window.HTMLTextAreaElement.prototype,
      "value",
    );
    if (!proto || typeof proto.set !== "function") {
      throw new Error("textarea value setter missing");
    }
    const previous = el.value;
    proto.set.call(el, next);
    const tracker = el._valueTracker;
    if (tracker) {
      tracker.setValue(previous);
    }
    el.dispatchEvent(new Event("input", { bubbles: true }));
  }, codes);
}

async function stubEditorApis(page, state) {
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
          transitionTriggers: [],
        },
      }),
    }),
  );
  // Unstubbed, these hit the fake id 42 for real: translations renders an
  // "Item not found" alert (axe contrast failure) and allowedWorkflows logs a
  // server ERROR that fails qa-health. Empty shapes keep the host quiet.
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
      const raw = route.request().postData() || "";
      state.fieldPuts.push(raw);
      if (state.rejectNext > 0) {
        const status = state.rejectNext;
        state.rejectNext = 0;
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({ Error: { message: "rejected" } }),
        });
        return;
      }
      const body = JSON.parse(raw);
      const payload = body.ItemEditorFields || body;
      const nextHtml = (payload.fields || []).find((f) => f.name === "text");
      if (nextHtml) {
        state.html = nextHtml.value;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemEditorFields: {
            contentId: "42",
            contentType: "percRichText",
            name: "Intro",
            checkoutUser: "admin",
            revision: 3,
            fields: payload.fields,
          },
        }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemEditorFields: {
          contentId: "42",
          contentType: "percRichText",
          name: "Intro",
          checkoutUser: "admin",
          revision: 2,
          fields: [{ name: "text", value: state.html }],
        },
      }),
    });
  });
}

test.describe("React Content Editor TipTap rich-text NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a TipTap NUL, keeps the previous HTML, still saves ordinary TipTap edits, and leaves the HTTP error mapping in place",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const state = { fieldPuts: [], rejectNext: 0, html: NUL_VALUE };
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
      await stubEditorApis(page, state);

      const editable = () => page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      const fieldError = () => page.locator(`[data-testid="${TEST_IDS.fieldErrorText}"]`);
      const tiptapUrl = () => editorSpaUrl(BASE_URL, "contentId=42&mode=edit");

      // Stored NUL: ProseMirror shows the stripped text, but the row value
      // still carries the NUL, so save refuses with a per-field message and
      // no PUT. Reload keeps serving the previous (NUL-bearing) HTML.
      await page.goto(tiptapUrl());
      await expect(editable()).toHaveAttribute("data-editor-kind", "html", {
        timeout: 20_000,
      });
      await expect(editable()).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(editable()).toContainText("badvalue");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(fieldError()).toContainText(/character that cannot be saved/i);
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(
        /HTML fields before saving/i,
      );
      await expect(editable()).toHaveAttribute("aria-invalid", "true");
      expect(state.fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(tiptapUrl());
      await expect(editable()).toContainText("badvalue", { timeout: 20_000 });
      expect(state.fieldPuts).toEqual([]);

      // Genuine TipTap edit path: a plain-text fragment insert carries the
      // NUL into the ProseMirror document (parsed markup would strip it), so
      // this save refuses the same way.
      state.html = PREVIOUS;
      await page.goto(tiptapUrl());
      await expect(editable()).toContainText("Hi", { timeout: 20_000 });
      await page.locator('[data-testid="tiptap-perc-insert-html-text"]').click();
      const fragment = page.locator('[data-testid="tiptap-perc-html-text"]');
      await setTextareaValue(fragment, `xa${NUL}yb`);
      await expect(fragment).toHaveValue(`xa${NUL}yb`);
      await page.locator('[data-testid="tiptap-perc-apply-text"]').click();
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(fieldError()).toContainText(/character that cannot be saved/i);
      expect(state.fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      // Ordinary TipTap save still writes through the same fields PUT.
      await page.goto(tiptapUrl());
      await expect(editable()).toContainText("Hi", { timeout: 20_000 });
      await editable().click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Bye");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => state.fieldPuts.length).toBe(1);
      const savedBody = JSON.parse(state.fieldPuts[0] || "{}");
      const savedPayload = savedBody.ItemEditorFields || savedBody;
      const savedHtml = (savedPayload.fields || []).find((f) => f.name === "text");
      expect(savedHtml && savedHtml.value).toBe(ORDINARY);
      expect(String(savedHtml.value).includes(NUL)).toBe(false);
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      await expect(editable()).toContainText("Bye");

      // Close with an unsaved TipTap edit asks first; cancelling writes nothing.
      await editable().click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Unsaved");
      let dismissed = false;
      page.once("dialog", async (dialog) => {
        dismissed = true;
        await dialog.dismiss();
      });
      await page.locator('[data-testid="editor-close"]').click();
      await expect.poll(() => dismissed).toBe(true);
      expect(state.fieldPuts.length).toBe(1);

      // HTTP 400, 403, and 409 do not claim success.
      await page.goto(tiptapUrl());
      await expect(editable()).toContainText("Bye", { timeout: 20_000 });
      state.rejectNext = 400;
      await editable().click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Rejected");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => state.fieldPuts.length).toBe(2);
      await expect(fieldError()).toContainText(/could not be saved/i);
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(tiptapUrl());
      await expect(editable()).toContainText("Bye", { timeout: 20_000 });
      state.rejectNext = 403;
      await editable().click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Rejected");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => state.fieldPuts.length).toBe(3);
      await expect(fieldError()).toContainText(/not allowed/i);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(tiptapUrl());
      await expect(editable()).toContainText("Bye", { timeout: 20_000 });
      state.rejectNext = 409;
      await editable().click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Rejected");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => state.fieldPuts.length).toBe(4);
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(
        /newer revision/i,
      );
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      // Rejected saves keep the previous HTML on reload.
      await page.goto(tiptapUrl());
      await expect(editable()).toContainText("Bye", { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );
});
