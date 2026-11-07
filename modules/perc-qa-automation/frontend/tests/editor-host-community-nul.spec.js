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
 * React Content Editor refuses a community field NUL on save (#5694 / #4532).
 *
 * <p>The client gate in {@code CommunityFieldWidget.tsx} runs before the item
 * fields PUT. A keyboard cannot type U+0000, so the spec injects
 * a select option through the DOM setter and a {@code change} event.</p>
 *
 * <p>Peer: {@code tests/editor-community-field.spec.js}. Required-empty community
 * stays in {@code tests/editor-host-community-required-blank.spec.js}.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @community}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-community-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { editorSpaUrl } = require("./helpers/editor-host-link-fields");

const PREVIOUS = "10";
const CATALOG = "20";
const NUL_VALUE = `10${String.fromCharCode(0)}`;

const TYPE = {
  ContentTypeDetail: {
    name: "percEvent",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "sys_communityid", label: "Community", control: "sys_DropDownSingle" },
    ],
  },
};

const REQUIRED_TYPE = {
  ContentTypeDetail: {
    name: "percEvent",
    fields: [
      {
        name: "sys_communityid",
        label: "Community",
        control: "sys_DropDownSingle",
        required: true,
      },
    ],
  },
};

const COMMUNITIES = {
  CommunityList: [
    { id: 10, name: "Default", label: "Default" },
    { id: 20, name: "Enterprise", label: "Enterprise" },
  ],
};

function fieldsPayload(communityId) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percEvent",
      name: "Home",
      checkoutUser: "admin",
      revision: 2,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "sys_communityid", value: communityId },
      ],
    },
  };
}

/**
 * Controlled select update, including U+0000. Code points cross the Playwright
 * bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setSelectValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload);
    const select = el;
    if (!Array.from(select.options).some((opt) => opt.value === next)) {
      const opt = document.createElement("option");
      opt.value = next;
      opt.textContent = "nul";
      select.appendChild(opt);
    }
    const proto = Object.getOwnPropertyDescriptor(
      window.HTMLSelectElement.prototype,
      "value",
    );
    if (!proto || typeof proto.set !== "function") {
      throw new Error("select value setter missing");
    }
    proto.set.call(select, next);
    select.dispatchEvent(new Event("change", { bubbles: true }));
  }, codes);
}

/**
 * @param {import("@playwright/test").Locator} locator
 * @returns {Promise<boolean>}
 */
async function controlHasNul(locator) {
  return locator.evaluate((el) => el.value.includes("\u0000"));
}

test.describe("React Content Editor community NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a community NUL, keeps the previous community, still saves a listed community, and leaves required-empty unchanged",
    { tag: ["@explorer-content-editor", "@editor", "@community"] },
    async ({ page }) => {
      const fieldPuts = [];
      const pageErrors = [];
      let communityId = PREVIOUS;
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
      await page.route("**/services/communities/find**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(COMMUNITIES),
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
          const nextCommunity = (payload.fields || []).find((f) => f.name === "sys_communityid");
          if (nextCommunity) {
            communityId = nextCommunity.value;
          }
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              ItemEditorFields: {
                contentId: "42",
                contentType: "percEvent",
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
          body: JSON.stringify(fieldsPayload(communityId)),
        });
      });

      const select = () => page.locator('[data-testid="editor-field-sys_communityid"]');
      const fieldError = () => page.locator('[data-testid="editor-field-error-sys_communityid"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveAttribute("data-editor-kind", "community", {
        timeout: 20_000,
      });
      await expect(select()).toHaveValue(PREVIOUS);

      await setSelectValue(select(), NUL_VALUE);
      await expect.poll(() => controlHasNul(select())).toBe(true);
      let dismissed = false;
      page.once("dialog", async (dialog) => {
        dismissed = true;
        await dialog.dismiss();
      });
      await page.locator('[data-testid="editor-close"]').click();
      await expect.poll(() => dismissed).toBe(true);
      expect(fieldPuts).toEqual([]);
      expect(await controlHasNul(select())).toBe(true);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await setSelectValue(select(), NUL_VALUE);
      await expect.poll(() => controlHasNul(select())).toBe(true);
      await page.locator('[data-testid="editor-save"]').click();
      await expect(fieldError()).toContainText(/character that cannot be saved/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toContainText(
        /community fields before saving/i,
      );
      await expect(select()).toHaveAttribute("aria-invalid", "true");
      expect(fieldPuts).toEqual([]);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await select().selectOption(CATALOG);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(1);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"sys_communityid"\s*,\s*"value"\s*:\s*"20"/);
      expect(fieldPuts[0].includes("\u0000")).toBe(false);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(select()).toHaveValue(CATALOG);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue(CATALOG, { timeout: 20_000 });
      rejectNext = 400;
      await select().selectOption(PREVIOUS);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(2);
      await expect(fieldError()).toContainText(/could not be saved/i);
      await expect(page.locator('[data-testid="editor-save-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue(CATALOG, { timeout: 20_000 });
      rejectNext = 403;
      await select().selectOption(PREVIOUS);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBe(3);
      await expect(fieldError()).toContainText(/not allowed/i);
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue(CATALOG, { timeout: 20_000 });
      rejectNext = 409;
      await select().selectOption(PREVIOUS);
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
      communityId = PREVIOUS;
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(select()).toHaveValue(PREVIOUS, { timeout: 20_000 });
      await expect(page.locator('[data-testid="editor-field-row-sys_communityid"]')).toHaveAttribute(
        "data-required",
        "true",
      );
      const putsBeforeRequired = fieldPuts.length;
      await select().selectOption("");
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
