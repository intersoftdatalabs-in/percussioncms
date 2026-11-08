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
 * React Content Editor TipTap strings follow the session locale (#5732,
 * epic #5622 slice 4).
 *
 * <p>Stands in for a German CMS session by intercepting the TMX JS catalog
 * ({@code tmx.jsp?mode=js&prefix=perc.ui.}) with German TipTap strings, then
 * proves the toolbar and Percussion plugin dialogs render the catalog
 * strings — including the dialog option labels that used to be hardcoded
 * English — while untranslated keys keep the English fallback.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap} {@code @i18n}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-html-tiptap-i18n.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
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

// German session-locale stand-in for tmx.jsp?mode=js&prefix=perc.ui.
// Keys not listed here echo back, exactly like an untranslated TMX key,
// so the UI must keep the English fallback for them.
const DE_TIPTAP = {
  "perc.ui.editor@Formatting toolbar": "Formatierungsleiste",
  "perc.ui.editor@Bold": "Fett",
  "perc.ui.editor@Insert inline link": "Inline-Link einfügen",
  "perc.ui.editor@Target": "Ziel",
  "perc.ui.editor@Same window": "Gleiches Fenster",
  "perc.ui.editor@New window": "Neues Fenster",
  "perc.ui.editor@Parent window": "Übergeordnetes Fenster",
  "perc.ui.editor@Top window": "Oberstes Fenster",
  "perc.ui.editor@Width": "Breite",
  "perc.ui.editor@Height": "Höhe",
  "perc.ui.editor@Alignment": "Ausrichtung",
  "perc.ui.editor@No alignment": "Keine Ausrichtung",
  "perc.ui.editor@Left": "Links",
  "perc.ui.editor@Right": "Rechts",
  "perc.ui.editor@Top": "Oben",
  "perc.ui.editor@Middle": "Mitte",
  "perc.ui.editor@Bottom": "Unten",
};

async function stubEditorApis(page) {
  await page.route("**/tmx/tmx.jsp*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/javascript; charset=UTF-8",
      body: `window.I18N = { message: function (k) { var d = ${JSON.stringify(DE_TIPTAP)}; return Object.prototype.hasOwnProperty.call(d, k) ? d[k] : k; } };`,
    }),
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
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    }),
  );
}

test.describe("React Content Editor TipTap session locale", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "tiptap toolbar and dialogs render the session-locale catalog",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap", "@i18n"] },
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

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit&engine=tiptap"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const toolbar = page.locator('[data-testid="editor-tiptap-toolbar-text"]');
      await expect(toolbar).toBeVisible();
      // Toolbar chrome follows the catalog; untranslated keys stay English.
      await expect(toolbar).toHaveAttribute("aria-label", "Formatierungsleiste");
      await expect(page.locator('[data-testid="tiptap-bold-text"]')).toContainText("Fett");
      await expect(page.locator('[data-testid="tiptap-italic-text"]')).toContainText("Italic");

      // Percussion inline-link dialog: target options from the catalog.
      await page.locator('[data-testid="tiptap-perc-inline-link-text"]').click();
      const target = page.locator('[data-testid="tiptap-perc-link-target-text"]');
      await expect(target).toBeVisible();
      await expect(target).toHaveAttribute("aria-label", "Ziel");
      await expect(target.locator("option")).toHaveText([
        "Gleiches Fenster",
        "Neues Fenster",
        "Übergeordnetes Fenster",
        "Oberstes Fenster",
      ]);
      await page.keyboard.press("Escape");

      // Percussion inline-image dialog: dimension/alignment from the catalog.
      await page.locator('[data-testid="tiptap-perc-inline-image-text"]').click();
      await expect(page.locator('[data-testid="tiptap-perc-image-width-text"]')).toHaveAttribute(
        "aria-label",
        "Breite",
      );
      await expect(page.locator('[data-testid="tiptap-perc-image-height-text"]')).toHaveAttribute(
        "aria-label",
        "Höhe",
      );
      const align = page.locator('[data-testid="tiptap-perc-image-align-text"]');
      await expect(align).toHaveAttribute("aria-label", "Ausrichtung");
      await expect(align.locator("option")).toHaveText([
        "Keine Ausrichtung",
        "Links",
        "Rechts",
        "Oben",
        "Mitte",
        "Unten",
      ]);

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
