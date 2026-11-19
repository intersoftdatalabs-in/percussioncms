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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * TipTap editor keyboard + screen-reader evidence (#6588, epic slice 5).
 *
 * <p>Proves the WCAG 2.2 AA keyboard bar for the bundled TipTap editor on a
 * live H2 CMS: the parity toolbar keeps a single tab stop (roving tabindex),
 * Left/Right/Home/End move across enabled controls and skip disabled ones,
 * Enter activates the focused control, the link editor behaves as a labelled
 * dialog (focus-in on open, focus-return on Escape), and Tab/Shift+Tab
 * traverse table cells. The axe gate pins zero serious/critical violations
 * on the full widget surface; Vitest
 * ({@code TipTapToolbar.a11y.test.tsx}) carries the same contract
 * headlessly.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap} {@code @a11y}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-tiptap-a11y.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
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

const TYPE = {
  ContentTypeDetail: {
    name: "percRichText",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "text", label: "Body", control: "sys_tinymce" },
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
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
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
}

async function openEditor(page) {
  await stubEditorApis(page);
  await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
  await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
    timeout: 20_000,
  });
  const tiptapHost = page.locator('[data-testid="editor-tiptap-text"]');
  await expect(tiptapHost).toBeVisible();
  const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
  await expect(editable).toHaveAttribute("data-editor-engine", "tiptap");
  await expect(editable).toContainText("Hi");
  return { tiptapHost, editable };
}

function toolbar(page) {
  return page.locator('[data-testid="editor-tiptap-toolbar-text"]');
}

function percBar(page) {
  return page.locator('[data-testid="editor-tiptap-perc-text"]');
}

async function activeTestId(page) {
  return page.evaluate(
    () => document.activeElement?.getAttribute("data-testid") ?? null,
  );
}

test.describe("React Content Editor TipTap a11y evidence", () => {
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
    "toolbar keeps one tab stop and the widget passes the axe gate",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@a11y"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      const { tiptapHost } = await openEditor(page);
      // Exactly one tab stop in the parity toolbar (roving tabindex starts
      // on undo; the plugin bar below is a separate toolbar).
      const zeroStops = await toolbar(page).evaluate((root) =>
        Array.from(root.querySelectorAll("button, select"))
          .filter((el) => el.getAttribute("tabindex") === "0")
          .map((el) => el.getAttribute("data-testid")),
      );
      expect(zeroStops).toEqual(["tiptap-undo-text"]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-tiptap-text"]',
      });
      expect(tiptapHost).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "arrow keys move across enabled controls and skip disabled ones",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@a11y"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      await openEditor(page);
      const bar = toolbar(page);
      await bar.getByTestId("tiptap-bold-text").focus();
      expect(await activeTestId(page)).toBe("tiptap-bold-text");
      await page.keyboard.press("ArrowRight");
      expect(await activeTestId(page)).toBe("tiptap-italic-text");
      await page.keyboard.press("ArrowLeft");
      expect(await activeTestId(page)).toBe("tiptap-bold-text");
      // End lands on the last enabled control (table row/col controls are
      // disabled outside a table); Home returns to the first enabled one.
      await page.keyboard.press("End");
      expect(await activeTestId(page)).toBe("tiptap-table-insert-text");
      await page.keyboard.press("Home");
      expect(await activeTestId(page)).toBe("tiptap-bold-text");
      // Outdent/indent are disabled outside a list: the arrow skips them.
      await bar.getByTestId("tiptap-numlist-text").focus();
      await page.keyboard.press("ArrowRight");
      expect(await activeTestId(page)).toBe("tiptap-link-text");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "Enter activates the focused control",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@a11y"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      const { editable } = await openEditor(page);
      await editable.click();
      await page.keyboard.press("ControlOrMeta+a");
      await toolbar(page).getByTestId("tiptap-bold-text").focus();
      await page.keyboard.press("Enter");
      await expect(
        toolbar(page).getByTestId("tiptap-bold-text"),
      ).toHaveAttribute("aria-pressed", "true");
      // The field testid sits on the ProseMirror surface itself.
      await expect(editable).toContainText("Hi");
      const html = await editable.evaluate((el) => el.innerHTML);
      expect(html).toContain("<strong>");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "link editor is a dialog with focus-in and Escape focus-return",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@a11y"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      await openEditor(page);
      const bar = toolbar(page);
      const linkButton = bar.getByTestId("tiptap-link-text");
      await linkButton.focus();
      await page.keyboard.press("Enter");
      const dialog = bar.getByRole("dialog");
      await expect(dialog).toBeVisible();
      await expect(dialog).toHaveAttribute("id", "tiptap-link-editor-text");
      await expect(linkButton).toHaveAttribute("aria-expanded", "true");
      await expect(linkButton).toHaveAttribute(
        "aria-controls",
        "tiptap-link-editor-text",
      );
      const input = bar.getByTestId("tiptap-link-url-text");
      expect(await activeTestId(page)).toBe("tiptap-link-url-text");
      await input.fill("https://example.com");
      await page.keyboard.press("Escape");
      await expect(dialog).toBeHidden({ timeout: 5_000 });
      expect(await bar.getByRole("dialog").count()).toBe(0);
      expect(await activeTestId(page)).toBe("tiptap-link-text");
      await expect(linkButton).toHaveAttribute("aria-expanded", "false");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "Tab and Shift+Tab traverse table cells",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@a11y"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      const { editable } = await openEditor(page);
      await toolbar(page).getByTestId("tiptap-table-insert-text").click();
      // The field testid sits on the ProseMirror surface itself.
      const content = editable;
      await expect(content.locator("table")).toBeVisible({ timeout: 10_000 });
      await content.locator("table td p").first().click();
      await page.keyboard.type("A1");
      await page.keyboard.press("Tab");
      const cellIndex = () =>
        page.evaluate(() => {
          const sel = window.getSelection();
          if (!sel || sel.rangeCount === 0) {
            return -1;
          }
          const node = sel.anchorNode;
          const el =
            node.nodeType === 3 ? node.parentElement : node;
          const td = el?.closest("td, th") ?? null;
          if (!td || !td.parentElement) {
            return -1;
          }
          return Array.from(td.parentElement.children).indexOf(td);
        });
      expect(await cellIndex()).toBe(1);
      await page.keyboard.press("Shift+Tab");
      expect(await cellIndex()).toBe(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "parity toolbar groups carry translated accessible names",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@a11y"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      await openEditor(page);
      const names = await toolbar(page).evaluate((root) =>
        Array.from(root.children)
          .filter((el) => el.getAttribute("role") === "group")
          .map((el) => el.getAttribute("aria-label")),
      );
      expect(names).toEqual([
        "History",
        "Text formatting",
        "Style",
        "Text alignment",
        "Lists",
        "Links",
        "Code",
        "Tables",
      ]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "plugin bar keeps one tab stop with arrow-key movement",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@a11y"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      await openEditor(page);
      const bar = percBar(page);
      // The plugin bar is a second toolbar with its own roving slot: Tab
      // enters each editor toolbar exactly once.
      const zeroStops = await bar.evaluate((root) =>
        Array.from(root.querySelectorAll("button"))
          .filter((el) => el.getAttribute("tabindex") === "0")
          .map((el) => el.getAttribute("data-testid")),
      );
      expect(zeroStops).toEqual(["tiptap-perc-inline-link-text"]);
      await bar.getByTestId("tiptap-perc-inline-link-text").focus();
      expect(await activeTestId(page)).toBe("tiptap-perc-inline-link-text");
      await page.keyboard.press("ArrowRight");
      expect(await activeTestId(page)).toBe("tiptap-perc-inline-image-text");
      await page.keyboard.press("End");
      expect(await activeTestId(page)).toBe("tiptap-perc-source-text");
      await page.keyboard.press("Home");
      expect(await activeTestId(page)).toBe("tiptap-perc-inline-link-text");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "plugin inline-link dialog is a dialog with focus-in and Escape focus-return",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@a11y"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      await openEditor(page);
      const bar = percBar(page);
      const linkButton = bar.getByTestId("tiptap-perc-inline-link-text");
      await linkButton.focus();
      await page.keyboard.press("Enter");
      const dialog = bar.getByRole("dialog");
      await expect(dialog).toBeVisible();
      await expect(dialog).toHaveAttribute(
        "id",
        "tiptap-perc-dialog-inline-link-text",
      );
      await expect(dialog).toHaveAttribute(
        "aria-label",
        "Insert inline link",
      );
      await expect(linkButton).toHaveAttribute("aria-expanded", "true");
      await expect(linkButton).toHaveAttribute(
        "aria-controls",
        "tiptap-perc-dialog-inline-link-text",
      );
      expect(await activeTestId(page)).toBe("tiptap-perc-link-url-text");
      await page.keyboard.press("Escape");
      await expect(dialog).toBeHidden({ timeout: 5_000 });
      expect(await bar.getByRole("dialog").count()).toBe(0);
      expect(await activeTestId(page)).toBe("tiptap-perc-inline-link-text");
      await expect(linkButton).toHaveAttribute("aria-expanded", "false");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
