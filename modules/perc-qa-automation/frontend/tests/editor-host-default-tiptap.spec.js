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
 * React Content Editor default-engine cutover (#5758).
 *
 * <p>TipTap is the default HTML engine: the editor renders the bundled
 * TipTap surface with no opt-in flag. {@code ?engine=tinymce} keeps the
 * legacy script-loaded TinyMCE as a one-release escape hatch. The legacy
 * textarea-fallback specs ({@code editor-host-html-fields},
 * {@code editor-host-html-clear}, {@code editor-host-html-nul},
 * {@code editor-host-html-required-blank}, {@code editor-host-html-unsafe})
 * stay pinned to that escape hatch; this spec proves the default path.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-default-tiptap.spec.js}</p>
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

test.describe("React Content Editor default TipTap engine (#5758)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "renders TipTap with no engine flag, edits, and saves HTML",
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
      // The default path must not fetch the legacy TinyMCE script at all.
      const tinymceRequests = [];
      page.on("request", (req) => {
        if (req.url().includes("/sys_resources/tinymce/")) {
          tinymceRequests.push(req.url());
        }
      });
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toHaveAttribute("data-editor-kind", "html");
      await expect(editable).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(editable).toHaveAttribute("contenteditable", "true");
      await expect(editable).toContainText("Hi");

      await editable.click();
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
      expect(
        tinymceRequests,
        `default engine requested TinyMCE: ${tinymceRequests.join(" ")}`,
      ).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "?engine=tinymce renders the legacy escape hatch",
    { tag: ["@explorer-content-editor", "@editor", "@html"] },
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
      // Keep the hatch hermetic: the shipped script is stubbed to a no-op so
      // the textarea fallback is the surface under test.
      await page.route("**/sys_resources/tinymce/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/javascript",
          body: "window.tinymce=undefined;",
        }),
      );

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit&engine=tinymce"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const area = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(area).toHaveAttribute("data-editor-kind", "html");
      await expect(area).not.toHaveAttribute("data-editor-engine", "tiptap");
      await expect(area).toHaveValue("<p>Hi</p>");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
