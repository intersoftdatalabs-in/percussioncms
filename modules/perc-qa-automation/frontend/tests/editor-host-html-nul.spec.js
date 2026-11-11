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
 * React Content Editor refuses an HTML NUL on save (#5389 / #4532).
 * TipTap-only since #5902 (the {@code ?engine=tinymce} escape hatch is
 * removed): the NUL reaches the row through the TipTap insert-HTML-fragment
 * dialog (a keyboard cannot type U+0000, so the spec sets the real
 * fragment textarea through the DOM setter and an {@code input} event),
 * while unsafe payloads seed the fields GET stub (parsed markup would
 * strip them from the surface but the draft still carries them). No
 * {@code engine} param is passed.
 *
 * <p>The client gate in {@code htmlField.ts} stays beside the unsafe-HTML
 * heuristic and the long-text gate in {@code longTextField.ts}.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-html-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-longtext-fields");

const PREVIOUS = "<p>Hi</p>";
const ORDINARY = "<p>Bye</p>";
const NUL_VALUE = `<p>bad${String.fromCharCode(0)}value</p>`;
// Plain-text twin for the TipTap insert-HTML dialog: parsed markup would
// strip the NUL in the ProseMirror schema, while plain text carries it
// into the row value the save gate reads.
const NUL_FRAGMENT = `bad${String.fromCharCode(0)}value`;
const LONG_PREVIOUS = "line one";

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

/**
 * @param {import("@playwright/test").Locator} locator
 * @returns {Promise<boolean>}
 */
async function controlHasNul(locator) {
  return locator.evaluate((el) => el.value.includes("\u0000"));
}

/**
 * Insert a raw fragment through the TipTap insert-HTML dialog. Parsed
 * markup would strip control characters, but a plain-text fragment carries
 * them into the row value the save gate reads.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} value
 */
async function insertTipTapFragment(page, value) {
  await page.locator('[data-testid="tiptap-perc-insert-html-text"]').click();
  const fragment = page.locator('[data-testid="tiptap-perc-html-text"]');
  await setTextareaValue(fragment, value);
  await expect(fragment).toHaveValue(value);
  await page.locator('[data-testid="tiptap-perc-apply-text"]').click();
}

test.describe("React Content Editor HTML NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses an HTML NUL, keeps the previous HTML, still saves ordinary HTML, and leaves the other gates in place",
    { tag: ["@explorer-content-editor", "@editor", "@html"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      // Single dialog listener with an explicit mode: competing
      // page.once() handlers race (a pending dismiss wins over a later
      // accept and aborts the navigation). The close-cancel phase keeps
      // the default dismiss; gotos after a refused (still-dirty) save
      // switch to accept first.
      let dialogMode = "dismiss";
      page.on("dialog", (dialog) => dialog[dialogMode]());
      let html = PREVIOUS;
      let description = LONG_PREVIOUS;
      let rejectNext = false;
      let includeLongText = false;
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
      // TipTap-only (#5902): the React editor must never fetch the legacy
      // TinyMCE script under any param.
      const tinymceRequests = [];
      page.on("request", (req) => {
        if (req.url().includes("/sys_resources/tinymce/")) {
          tinymceRequests.push(req.url());
        }
      });
      await page.route("**/sys_resources/**", (route) =>
        route.fulfill({ status: 404, body: "" }),
      );
      await page.route("**/services/**", (route) =>
        route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
      );
      await page.route("**/rest/**", (route) =>
        route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
      );
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
      // Quiet the fake id 42 against the live cell: untranslated and
      // workflow-allowance lookups would otherwise log server ERRORs
      // (same empty shapes as editor-host-html-tiptap.spec.js).
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
      await page.route("**/rest/content-explorer/translations/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            itemId: 42,
            locale: "en-us",
            variants: [{ contentId: 42, locale: "en-us", role: "source" }],
          }),
        }),
      );
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ContentTypeDetail: {
              name: "percRichText",
              fields: includeLongText
                ? [
                    { name: "text", label: "Body", control: "sys_tinymce" },
                    {
                      name: "description",
                      label: "Description",
                      control: "sys_TextArea",
                    },
                  ]
                : [{ name: "text", label: "Body", control: "sys_tinymce" }],
            },
          }),
        }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          const raw = route.request().postData() || "";
          fieldPuts.push(raw);
          if (rejectNext) {
            rejectNext = false;
            await route.fulfill({
              status: 400,
              contentType: "application/json",
              body: JSON.stringify({ Error: { message: "rejected" } }),
            });
            return;
          }
          const body = JSON.parse(raw);
          const payload = body.ItemEditorFields || body;
          const nextHtml = (payload.fields || []).find((f) => f.name === "text");
          const nextDescription = (payload.fields || []).find(
            (f) => f.name === "description",
          );
          if (nextHtml) {
            html = nextHtml.value;
          }
          if (nextDescription) {
            description = nextDescription.value;
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
              fields: includeLongText
                ? [
                    { name: "text", value: html },
                    { name: "description", value: description },
                  ]
                : [{ name: "text", value: html }],
            },
          }),
        });
      });

      const htmlField = () => page.locator('[data-testid="editor-field-text"]');
      const htmlError = () => page.locator('[data-testid="editor-field-error-text"]');
      const descriptionField = () =>
        page.locator('[data-testid="editor-field-description"]');
      const descriptionError = () =>
        page.locator('[data-testid="editor-field-error-description"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveAttribute("data-editor-kind", "html", {
        timeout: 20_000,
      });
      await expect(htmlField()).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(htmlField()).toContainText("Hi");
      await expect(descriptionField()).toHaveCount(0);

      // The fragment insert carries the NUL into the row value; closing
      // asks first and cancelling writes nothing, and the NUL is still
      // refused on the subsequent save.
      await insertTipTapFragment(page, NUL_FRAGMENT);
      await page.locator('[data-testid="editor-close"]').click();
      await page.waitForTimeout(300);
      expect(fieldPuts).toEqual([]);
      dialogMode = "accept";
      await page.locator('[data-testid="editor-save"]').click();
      await expect(htmlError()).toContainText(/character that cannot be saved/i);
      expect(fieldPuts).toEqual([]);

      // The refused save keeps the draft dirty, so leaving asks
      // first; accepting is the operator path under test here.
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Hi", { timeout: 20_000 });
      await insertTipTapFragment(page, NUL_FRAGMENT);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(htmlError()).toContainText(/character that cannot be saved/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /HTML fields before saving/i,
      );
      await expect(htmlField()).toHaveAttribute("aria-invalid", "true");
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      // Seeded script markup: parsed markup would strip it from the
      // surface, but the draft still carries it to the save-side gate.
      // The refused save keeps the draft dirty, so leaving asks
      // first; accepting is the operator path under test here.
      html = "<p><script>alert(1)</script></p>";
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toHaveAttribute("data-editor-engine", "tiptap", {
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-save"]').click();
      await expect(htmlError()).toContainText(/script or event markup/i);
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      // The refused save keeps the draft dirty, so leaving asks
      // first; accepting is the operator path under test here.
      html = PREVIOUS;

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Hi", { timeout: 20_000 });
      await htmlField().click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Bye");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      const savedBody = JSON.parse(fieldPuts[0] || "{}");
      const savedPayload = savedBody.ItemEditorFields || savedBody;
      const savedHtml = (savedPayload.fields || []).find((f) => f.name === "text");
      expect(savedHtml && savedHtml.value).toBe(ORDINARY);
      expect(String(savedHtml.value).includes("\u0000")).toBe(false);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(htmlField()).toContainText("Bye");

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Bye", { timeout: 20_000 });
      rejectNext = true;
      await htmlField().click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Rejected");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(2);
      await expect(htmlError()).toContainText(/could not be saved/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Bye", { timeout: 20_000 });

      // The refused save keeps the draft dirty, so leaving asks
      // first; accepting is the operator path under test here.
      includeLongText = true;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(htmlField()).toContainText("Bye", { timeout: 20_000 });
      await expect(descriptionField()).toHaveAttribute("data-editor-kind", "longtext");
      await expect(descriptionField()).toHaveValue(LONG_PREVIOUS);
      const putsBeforeLong = fieldPuts.length;
      await setTextareaValue(descriptionField(), `x${String.fromCharCode(0)}y`);
      await expect.poll(() => controlHasNul(descriptionField())).toBe(true);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(descriptionError()).toContainText(/character that cannot be saved/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /long text fields before saving/i,
      );
      expect(fieldPuts.length).toBe(putsBeforeLong);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(htmlError()).toHaveCount(0);

      // The refused save keeps the draft dirty, so leaving asks
      // first; accepting is the operator path under test here.
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(descriptionField()).toHaveValue(LONG_PREVIOUS, { timeout: 20_000 });
      await expect(htmlField()).toContainText("Bye");
      expect(tinymceRequests, `tinymce fetched: ${tinymceRequests.join(" | ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
