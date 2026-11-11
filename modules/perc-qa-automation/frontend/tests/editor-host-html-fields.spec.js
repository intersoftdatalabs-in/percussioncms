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
 * React Content Editor HTML field save (#4680). TipTap-only since #5902
 * (the {@code ?engine=tinymce} escape hatch is removed): every HTML field
 * renders the bundled TipTap editor, so no {@code engine} param is passed.
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-html-fields.spec.js}</p>
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

async function stubEditorApis(page, { putStatus, putBody, onPut } = {}) {
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
        status: putStatus ?? 200,
        contentType: "application/json",
        body: JSON.stringify(
          putBody ??
            (putStatus === 400
              ? { Error: { message: "HTML rejected" } }
              : SAVED),
        ),
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

test.describe("React Content Editor HTML field save", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "saves HTML body, maps 400, view is read-only",
    { tag: ["@explorer-content-editor", "@editor", "@html"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = [];
      // TipTap-only (#5902): the React editor must never fetch the legacy
      // TinyMCE script under any param.
      const tinymceRequests = [];
      page.on("request", (req) => {
        if (req.url().includes("/sys_resources/tinymce/")) {
          tinymceRequests.push(req.url());
        }
      });
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

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible();
      const htmlField = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(htmlField).toHaveAttribute("data-editor-kind", "html");
      await expect(htmlField).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(htmlField).toHaveAttribute("contenteditable", "true");
      await expect(htmlField).toContainText("Hi");
      await htmlField.click();
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
      expect(tinymceRequests, `tinymce fetched: ${tinymceRequests.join(" | ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "save HTTP 400 maps onto the HTML field",
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
      await stubEditorApis(page, { putStatus: 400 });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const editField = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editField).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(editField).toContainText("Hi");
      await editField.click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Updated");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.fieldErrorText}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
      await expect(page.getByText(/^Saved$/)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "view mode HTML is read-only",
    { tag: ["@explorer-content-editor", "@editor", "@html"] },
    async ({ page }) => {
      await stubEditorApis(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const viewHtml = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(viewHtml).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(viewHtml).toHaveAttribute("readonly", "");
      await expect(page.locator(`[data-testid="${TEST_IDS.save}"]`)).toHaveCount(0);
    },
  );
});
