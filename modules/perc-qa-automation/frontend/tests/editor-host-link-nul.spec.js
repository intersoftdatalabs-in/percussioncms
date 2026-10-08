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
 * React Content Editor refuses a link NUL on save (#5390 / #4532).
 *
 * <p>The client gate in {@code linkField.ts} runs before the existing
 * invalid-link shape check. A keyboard cannot type U+0000, so the spec sets
 * the input through the DOM setter and an {@code input} event.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @link}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-link-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-link-fields");

const PREVIOUS = "594";
const CONTENT_ID = "595";
const CONTENT_GUID = "0-101-594";
const FOLDER_PATH = "/Sites/Example/index";
const NUL_VALUE = `594${String.fromCharCode(0)}`;

/**
 * Controlled input update, including U+0000. Code points cross the Playwright
 * bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setInputValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload);
    const proto = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value");
    if (!proto || typeof proto.set !== "function") {
      throw new Error("input value setter missing");
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

test.describe("React Content Editor link NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a link NUL, keeps the previous link, still saves a content id, GUID, and folder path, and leaves the invalid-link gate in place",
    { tag: ["@explorer-content-editor", "@editor", "@link"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let pageLink = PREVIOUS;
      let rejectNext = false;
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
              name: "percPage",
              fields: [
                {
                  name: "page",
                  label: "Page link",
                  control: "sys_PageLink",
                  dataType: "text",
                },
              ],
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
          const nextLink = (payload.fields || []).find((f) => f.name === "page");
          if (nextLink) {
            pageLink = nextLink.value;
          }
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              ItemEditorFields: {
                contentId: "42",
                contentType: "percPage",
                name: "Home",
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
              contentType: "percPage",
              name: "Home",
              checkoutUser: "admin",
              revision: 2,
              fields: [{ name: "page", value: pageLink }],
            },
          }),
        });
      });

      const linkField = () => page.locator('[data-testid="editor-field-page"]');
      const linkError = () => page.locator('[data-testid="editor-field-error-page"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveAttribute("data-editor-kind", "link", {
        timeout: 20_000,
      });
      await expect(linkField()).toHaveValue(PREVIOUS);

      await setInputValue(linkField(), NUL_VALUE);
      await expect.poll(() => controlHasNul(linkField())).toBe(true);
      let dismissed = false;
      page.once("dialog", async (dialog) => {
        dismissed = true;
        await dialog.dismiss();
      });
      await page.locator('[data-testid="editor-close"]').click();
      await expect.poll(() => dismissed).toBe(true);
      expect(fieldPuts).toEqual([]);
      expect(await controlHasNul(linkField())).toBe(true);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await setInputValue(linkField(), NUL_VALUE);
      await expect.poll(() => controlHasNul(linkField())).toBe(true);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(linkError()).toContainText(/character that cannot be saved/i);
      await expect(linkError()).not.toContainText(/content id or a site path/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /link fields before saving/i,
      );
      await expect(linkField()).toHaveAttribute("aria-invalid", "true");
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await linkField().fill("javascript:alert(1)");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(linkError()).toContainText(/content id or a site path/i);
      await expect(linkError()).not.toContainText(/character that cannot be saved/i);
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      const valid = [CONTENT_ID, CONTENT_GUID, FOLDER_PATH];
      for (const next of valid) {
        await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
        await expect(linkField()).toBeVisible({ timeout: 20_000 });
        const putsBefore = fieldPuts.length;
        await linkField().fill(next);
        await page.locator('[data-testid="editor-save"]').click();
        await expect.poll(() => fieldPuts.length).toBe(putsBefore + 1);
        const savedBody = JSON.parse(fieldPuts[fieldPuts.length - 1] || "{}");
        const savedPayload = savedBody.ItemEditorFields || savedBody;
        const savedLink = (savedPayload.fields || []).find((f) => f.name === "page");
        expect(savedLink && savedLink.value).toBe(next);
        expect(savedLink && savedLink.dataType).toBe("link");
        expect(String(savedLink.value).includes("\u0000")).toBe(false);
        await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
        await expect(linkField()).toHaveValue(next);
      }

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue(FOLDER_PATH, { timeout: 20_000 });
      rejectNext = true;
      await linkField().fill(CONTENT_ID);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(valid.length + 1);
      await expect(linkError()).toContainText(/could not be saved/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(linkField()).toHaveValue(FOLDER_PATH, { timeout: 20_000 });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
