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
 * React Content Editor refuses a whole-number field NUL on save (#5557 / #4532).
 *
 * <p>The client gate in {@code numericField.ts} runs before the invalid-number
 * check. A keyboard cannot type U+0000, so the spec calls the React
 * {@code onChange} handler with a CMS draft that contains NUL. The widget
 * then sets {@code data-editor-has-nul}.</p>
 *
 * <p>Peer: {@code tests/editor-host-date-nul.spec.js} /
 * {@code tests/editor-host-number-clear.spec.js}. Decimal/float NUL stays
 * on a later slice. Required-blank number stays in
 * {@code tests/editor-host-number-required-blank.spec.js}.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @number}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-number-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-link-fields");

const PREVIOUS = "4";
const NEXT = "7";
const NUL_WIDGET = `7${String.fromCharCode(0)}`;

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      {
        name: "qty",
        label: "Quantity",
        control: "sys_Number",
        dataType: "integer",
        controlProperties: [
          { name: "minimum", value: "0" },
          { name: "maximum", value: "10" },
        ],
      },
    ],
  },
};

const REQUIRED_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      {
        name: "qty",
        label: "Quantity",
        control: "sys_Number",
        dataType: "integer",
        required: true,
        controlProperties: [
          { name: "minimum", value: "0" },
          { name: "maximum", value: "10" },
        ],
      },
    ],
  },
};

function fieldsPayload(qty) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 2,
      fields: [{ name: "qty", value: qty }],
    },
  };
}

/**
 * Controlled number update, including U+0000. Code points cross the
 * Playwright bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setNumberValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload);
    const propsKey = Object.keys(el).find((k) => k.startsWith("__reactProps$"));
    if (!propsKey) {
      throw new Error("react props missing");
    }
    const props = el[propsKey];
    if (typeof props.onChange !== "function") {
      throw new Error("react onChange missing");
    }
    props.onChange({ target: { value: next }, currentTarget: { value: next } });
  }, codes);
}

/**
 * @param {import("@playwright/test").Locator} locator
 * @returns {Promise<boolean>}
 */
async function draftHasNul(locator) {
  return locator.evaluate((el) => el.getAttribute("data-editor-has-nul") === "true");
}

test.describe("React Content Editor number NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a number NUL, keeps the previous integer, still saves a whole number, and leaves required-blank unchanged",
    { tag: ["@explorer-content-editor", "@editor", "@number"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let qty = PREVIOUS;
      let rejectNext = 0;
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
          body: JSON.stringify(TYPE),
        }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          const raw = route.request().postData() || "";
          fieldPuts.push(raw);
          if (rejectNext > 0) {
            const status = rejectNext;
            rejectNext = 0;
            await route.fulfill({
              status,
              contentType: "application/json",
              body: JSON.stringify({ Error: { message: "rejected" } }),
            });
            return;
          }
          const body = JSON.parse(raw);
          const payload = body.ItemEditorFields || body;
          const nextQty = (payload.fields || []).find((f) => f.name === "qty");
          if (nextQty) {
            qty = nextQty.value;
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
          body: JSON.stringify(fieldsPayload(qty)),
        });
      });

      const field = () => page.locator('[data-testid="editor-field-qty"]');
      const fieldError = () => page.locator('[data-testid="editor-field-error-qty"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveAttribute("data-editor-kind", "number", {
        timeout: 20_000,
      });
      await expect(field()).toHaveAttribute("data-editor-numeric", "integer");
      await expect(field()).toHaveValue(PREVIOUS);

      await setNumberValue(field(), NUL_WIDGET);
      await expect.poll(() => draftHasNul(field())).toBe(true);
      let dismissed = false;
      page.once("dialog", async (dialog) => {
        dismissed = true;
        await dialog.dismiss();
      });
      await page.locator('[data-testid="editor-close"]').click();
      await expect.poll(() => dismissed).toBe(true);
      expect(fieldPuts).toEqual([]);
      await expect.poll(() => draftHasNul(field())).toBe(true);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await setNumberValue(field(), NUL_WIDGET);
      await expect.poll(() => draftHasNul(field())).toBe(true);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(fieldError()).toContainText(/character that cannot be saved/i);
      await expect(fieldError()).not.toContainText(/required/i);
      await expect(fieldError()).not.toContainText(/valid number/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /number fields before saving/i,
      );
      await expect(field()).toHaveAttribute("aria-invalid", "true");
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await field().fill(NEXT);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"qty"\s*,\s*"value"\s*:\s*"7"/);
      expect(fieldPuts[0].includes("\u0000")).toBe(false);
      expect(qty).toBe(NEXT);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(field()).toHaveValue(NEXT);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(NEXT, { timeout: 20_000 });
      rejectNext = 400;
      await field().fill(PREVIOUS);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(2);
      await expect(fieldError()).toContainText(/could not be saved/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(NEXT, { timeout: 20_000 });
      rejectNext = 403;
      await field().fill(PREVIOUS);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(3);
      await expect(fieldError()).toContainText(/not allowed/i);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(NEXT, { timeout: 20_000 });
      rejectNext = 409;
      await field().fill(PREVIOUS);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(4);
      await expect(page.locator('[data-testid="editor-save-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.unroute("**/services/contenttypes/**");
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(REQUIRED_TYPE),
        }),
      );
      qty = PREVIOUS;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(field()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await expect(page.locator('[data-testid="editor-field-row-qty"]')).toHaveAttribute(
        "data-required",
        "true",
      );
      const putsBeforeRequired = fieldPuts.length;
      await field().fill("");
      await page.locator('[data-testid="editor-save"]').click();
      await expect(fieldError()).toContainText(/required/i);
      await expect(fieldError()).not.toContainText(/character that cannot be saved/i);
      expect(fieldPuts.length).toBe(putsBeforeRequired);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );
});
