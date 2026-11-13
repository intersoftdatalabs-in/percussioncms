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
 * EditorHost rich-text undo/redo parity (#6262 / #4532).
 *
 * <p>Toolbar Undo/Redo and Mod-z / Shift-Mod-z / Mod-y cover user typing
 * only: programmatic syncs (revert-all, restore, reload) reset the TipTap
 * history instead of pushing an undo step, so Ctrl+Z after a revert cannot
 * resurrect discarded edits into the draft.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-tiptap-undo-redo.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  editorSpaUrl,
  isItemFieldsPutUrl,
  TEST_IDS,
} = require("./helpers/editor-host-html-fields");
const {
  expectNoSeriousA11yViolations,
} = require("./helpers/a11y");

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
        body: JSON.stringify(FIELDS),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    });
  });
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: { id: "42", name: "Intro", path: "//Folders/Lab/Intro", type: "page" },
      }),
    }),
  );
}

function consoleOn(page, pageErrors) {
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
}

test.describe("EditorHost TipTap undo/redo parity (#6262)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "typing undoes and redoes through toolbar and keyboard, then saves",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, { onPut: (req) => puts.push(req) });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toHaveAttribute("data-editor-engine", "tiptap");
      await expect(editable).toContainText("Hi");

      await editable.click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Bye");
      await expect(editable).toContainText("Bye");

      // Keyboard undo restores the loaded text; keyboard redo re-applies.
      await page.keyboard.press("ControlOrMeta+z");
      await expect(editable).toContainText("Hi");
      await expect(editable).not.toContainText("Bye");
      await page.keyboard.press("ControlOrMeta+Shift+z");
      await expect(editable).toContainText("Bye");

      // Toolbar undo/redo agree with the keyboard history.
      await page.getByTestId("tiptap-undo-text").click();
      await expect(editable).not.toContainText("Bye");
      await page.getByTestId("tiptap-redo-text").click();
      await expect(editable).toContainText("Bye");

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const text = (payload.fields || []).find((f) => f.name === "text");
      expect(text && text.value).toBe("<p>Bye</p>");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "revert-all resets history: Ctrl+Z cannot resurrect discarded edits",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, { onPut: (req) => puts.push(req) });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toContainText("Hi");

      await editable.click();
      await page.keyboard.press("ControlOrMeta+a");
      await page.keyboard.type("Discarded");
      await expect(editable).toContainText("Discarded");
      await expect(page.getByTestId("editor-revert")).toBeVisible();

      await page.getByTestId("editor-revert").click();
      await expect(editable).toContainText("Hi");
      await expect(editable).not.toContainText("Discarded");
      await expect(page.getByTestId("editor-revert")).toHaveCount(0);

      // The revert is the new undo baseline: Ctrl+Z is a no-op instead of
      // resurrecting "Discarded", and toolbar Undo is disabled (a disabled
      // button cannot be clicked, so assert the disabled state itself).
      await editable.click();
      await page.keyboard.press("ControlOrMeta+z");
      await expect(editable).not.toContainText("Discarded");
      await expect(editable).toContainText("Hi");
      await expect(page.getByTestId("tiptap-undo-text")).toBeDisabled();
      await expect(editable).not.toContainText("Discarded");

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const bodies = puts
        .filter((r) => isItemFieldsPutUrl(r.url()))
        .map((r) => r.postData() || "");
      expect(bodies.length).toBeGreaterThan(0);
      for (const raw of bodies) {
        expect(raw).not.toContain("Discarded");
      }
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );
});
