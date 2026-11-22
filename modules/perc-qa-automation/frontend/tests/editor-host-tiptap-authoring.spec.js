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
 * React Content Editor TipTap authoring slice (#7047, slice 2/3 of #5622).
 *
 * <p>Proves the authoring-emphasis paths the parity suite (#5651) left
 * uncovered: typing into the surface, <em>italic</em> and ordered-list
 * toolbar commands, the link dialog empty-state (empty address removes the
 * link), and unlink — each persisted through the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} path with zero
 * content drift. The toolbar already covers bold/bullet-list/link-apply
 * ({@code editor-host-html-tiptap.spec.js}); this spec pins the rest of the
 * slice-2/3 acceptance (italic, ordered list, link removal) plus the link
 * dialog hint copy and a Live Proof screenshot.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-tiptap-authoring.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { proofScreenshot } = require("./helpers/proof-shot");
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
  // Fake persistence backing the fields endpoints (see the fields route
  // below): GET serves the last-saved payload so post-save reloads render
  // what was sent.
  const state = { current: FIELDS };
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
      // Stateful fake persistence: the host reloads fields after a save
      // (PUT response or re-GET), so the stub stores what was sent and
      // echoes it back with the next revision — like a real persisting
      // server. This also proves the reload shows the saved formatting
      // (zero drift through the round-trip, not just the request).
      try {
        const sent = JSON.parse(route.request().postData() || "{}");
        const payload = sent.ItemEditorFields || sent;
        state.current = {
          ItemEditorFields: { ...payload, revision: 3 },
        };
      } catch {
        state.current = SAVED;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(state.current),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(state.current),
    });
  });
}

test.describe("React Content Editor TipTap authoring slice", () => {
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

  function savedText(putReq) {
    const body = JSON.parse(putReq.postData() || "{}");
    const payload = body.ItemEditorFields || body;
    const text = (payload.fields || []).find((f) => f.name === "text");
    return (text && text.value) || "";
  }

  test(
    "italic, ordered list, and link edits persist via PUT with zero drift",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchErrors(page);
      await stubEditorApis(page, {
        onPut: (req) => puts.push(req),
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toContainText("Hi");
      const toolbar = page.locator('[data-testid="editor-tiptap-toolbar-text"]');
      await expect(toolbar).toBeVisible();

      // Type into the surface, then italicize the stored word: click the
      // stored text so the caret lands in its paragraph, select it with
      // Home / Shift+End, and drive the italic control.
      await editable.locator("text=Hi").click();
      await page.keyboard.press("End");
      await page.keyboard.type(" there");
      await expect(editable).toContainText("Hi there");
      await page.keyboard.press("Home");
      await page.keyboard.press("Shift+End");
      await page.locator('[data-testid="tiptap-italic-text"]').click();
      await expect(editable.locator("em").first()).toContainText("Hi there");

      // Ordered list on the same paragraph.
      await page.locator('[data-testid="tiptap-numlist-text"]').click();
      const listHtml = await editable.innerHTML();
      expect(listHtml).toContain("<ol>");
      expect(listHtml).toContain("<em>");

      // Link dialog on plain text: no link under the selection, so the
      // remove-hint stays out and only the address field shows (#7075
      // review — the hint reads oddly when there is no link to remove).
      await page.locator('[data-testid="tiptap-link-text"]').click();
      const linkInput = page.locator('[data-testid="tiptap-link-url-text"]');
      await expect(
        page.getByText("Leave the address empty to remove the link."),
      ).toHaveCount(0);
      expect(await linkInput.getAttribute("aria-describedby")).toBeNull();
      await linkInput.fill("https://example.com/authoring");
      await expect(linkInput).toHaveValue("https://example.com/authoring");
      await page.locator('[data-testid="tiptap-link-apply-text"]').click();
      await expect(editable.locator('a[href="https://example.com/authoring"]')).toBeVisible();
      // Unlink arms only inside a link now that one is applied.
      await expect(page.locator('[data-testid="tiptap-unlink-text"]')).toBeEnabled();

      // Live Proof: put the caret inside the applied link, then reopen
      // the link dialog — now the empty-state hint names the
      // empty-removes contract and editing selects the existing address
      // for replacement.
      await editable.locator('a[href="https://example.com/authoring"]').click();
      await page.locator('[data-testid="tiptap-link-text"]').click();
      await expect(
        page.getByText("Leave the address empty to remove the link."),
      ).toBeVisible();
      // The dialog element tightly bounds the address field, the hint,
      // and the apply/close controls (a toolbar-box shot crops the hint).
      const linkDialog = page.locator("#tiptap-link-editor-text");
      await linkDialog.scrollIntoViewIfNeeded();
      await proofScreenshot(linkDialog, "issue-7047-authoring-emphasis.png");

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const saved = savedText(putReq);
      expect(saved).toContain("<em>Hi there</em>");
      expect(saved).toContain("<ol>");
      expect(saved).toContain('href="https://example.com/authoring"');
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      // Post-save reload renders what was sent (stateful stub echoes the
      // PUT): the formatted surface survives the full round-trip.
      await expect(editable.locator("em").first()).toContainText("Hi there");
      await expect(editable.locator("ol").first()).toBeVisible();

      // Link removal round-trip: emptying the address removes the link and
      // the removal persists on the next save.
      await editable.locator("text=Hi there").click();
      await page.keyboard.press("Home");
      await page.keyboard.press("Shift+End");
      await page.locator('[data-testid="tiptap-link-text"]').click();
      const reopened = page.locator('[data-testid="tiptap-link-url-text"]');
      await expect(reopened).toHaveValue("https://example.com/authoring");
      await reopened.fill("");
      await page.locator('[data-testid="tiptap-link-apply-text"]').click();
      await expect(editable.locator("a").first()).toHaveCount(0);
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(1);
      const secondPut = puts.filter((r) => isItemFieldsPutUrl(r.url())).at(-1);
      expect(secondPut).toBeTruthy();
      const resaved = savedText(secondPut);
      expect(resaved).not.toContain("<a");
      expect(resaved).toContain("<em>Hi there</em>");
      expect(resaved).toContain("<ol>");
      await expect(page.getByText(/^Saved$/)).toBeVisible();

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );
});
