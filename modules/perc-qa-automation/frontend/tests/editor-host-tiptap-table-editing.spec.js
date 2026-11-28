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
 * React Content Editor TipTap table structure editing round-trip (#7393).
 *
 * <p>Proves the toolbar table controls (insert, add/remove row, add/remove
 * column, delete) persist through the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} path: structure edits
 * plus header/body cell content save through the PUT, and a reload serves the
 * same structure and content back (zero drift). The Vitest suite
 * ({@code tiptapRoundTrip.test.ts}, {@code TipTapToolbar.test.tsx}) proves the
 * same cycle at the schema/normalizer level; this spec proves it live against
 * the served SPA bundle.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-tiptap-table-editing.spec.js}</p>
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

const TYPE = {
  ContentTypeDetail: {
    name: "percRichText",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "text", label: "Body", control: "sys_default_richtext_editor" },
    ],
  },
};

async function stubEditorApis(page, state, { onPut } = {}) {
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
  await page.route("**/Assets/i.png*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "image/png",
      body: Buffer.from(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
        "base64",
      ),
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
        body: JSON.stringify({
          ItemEditorFields: {
            ...FIELDS.ItemEditorFields,
            revision: 3,
            fields: [
              { name: "sys_title", value: "Intro" },
              { name: "text", value: state.savedText },
            ],
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
          ...FIELDS.ItemEditorFields,
          fields: [
            { name: "sys_title", value: "Intro" },
            { name: "text", value: state.savedText },
          ],
        },
      }),
    });
  });
}

test.describe("TipTap table structure editing round-trip", () => {
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
    "table structure edits and cell content persist via PUT with zero drift on reload (#7393)",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const state = { savedText: "<p>Hi</p>" };
      const puts = [];
      const pageErrors = watchErrors(page);
      await stubEditorApis(page, state, {
        onPut: (req) => {
          puts.push(req);
          try {
            const body = JSON.parse(req.postData() || "{}");
            const payload = body.ItemEditorFields || body;
            const text = (payload.fields || []).find((f) => f.name === "text");
            if (text && typeof text.value === "string") {
              state.savedText = text.value;
            }
          } catch {
            // Keep serving the last good value on reload.
          }
        },
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(editable).toContainText("Hi");
      await expect(
        page.locator('[data-testid="editor-tiptap-toolbar-text"]'),
      ).toBeVisible();

      // Structure edits first on the pristine table (no content to lose):
      // insert 3x3, grow to 4x4, shrink back to 3x3.
      await page.locator('[data-testid="tiptap-table-insert-text"]').click();
      const table = editable.locator("table").first();
      await expect(table).toBeVisible();
      await expect(editable.locator("tr")).toHaveCount(3);
      await page.locator('[data-testid="tiptap-table-add-row-text"]').click();
      await expect(editable.locator("tr")).toHaveCount(4);
      await page.locator('[data-testid="tiptap-table-add-col-text"]').click();
      await expect(editable.locator("th")).toHaveCount(4);
      // Steer into the added column so remove-col drops that column, then
      // into a body cell so remove-row drops a body row (header survives).
      await editable.locator("tr").first().locator("th").nth(1).click();
      await page.locator('[data-testid="tiptap-table-remove-col-text"]').click();
      await expect(editable.locator("th")).toHaveCount(3);
      await editable.locator("td").first().click();
      await page.locator('[data-testid="tiptap-table-remove-row-text"]').click();
      await expect(editable.locator("tr")).toHaveCount(3);
      await expect(editable.locator("th")).toHaveCount(3);

      // Cell content: header row plus one body cell. Tab walks table cells
      // like the toolbar keyboard contract.
      await editable.locator("th").first().click();
      await page.keyboard.type("Name");
      await page.keyboard.press("Tab");
      await page.keyboard.type("Value");
      await page.keyboard.press("Tab");
      await page.keyboard.type("Note");
      await page.keyboard.press("Tab");
      await page.keyboard.type("Alpha");
      await expect(editable.locator("th").nth(0)).toContainText("Name");
      await expect(editable.locator("th").nth(1)).toContainText("Value");
      await expect(editable.locator("th").nth(2)).toContainText("Note");
      await expect(editable).toContainText("Alpha");
      await page.screenshot({
        path: "test-results/live-proof-7393-table-editing.png",
      });

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
      expect(putReq).toBeTruthy();
      const body = JSON.parse(putReq.postData() || "{}");
      const payload = body.ItemEditorFields || body;
      const text = (payload.fields || []).find((f) => f.name === "text");
      const saved = (text && text.value) || "";
      expect(saved).toContain("<table");
      expect(saved).toContain("<th");
      expect(saved).toContain("Name");
      expect(saved).toContain("Value");
      expect(saved).toContain("Note");
      expect(saved).toContain("Alpha");
      await expect(page.getByText(/^Saved$/)).toBeVisible();

      // Reload: the GET now serves the last PUT value — the same structure
      // (3 rows, header row) and cell content must come back (zero drift).
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const reloaded = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
      await expect(reloaded.locator("table").first()).toBeVisible();
      await expect(reloaded.locator("tr")).toHaveCount(3);
      await expect(reloaded.locator("th")).toHaveCount(3);
      await expect(reloaded.locator("th").nth(0)).toContainText("Name");
      await expect(reloaded.locator("th").nth(1)).toContainText("Value");
      await expect(reloaded.locator("th").nth(2)).toContainText("Note");
      await expect(reloaded).toContainText("Alpha");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );
});
