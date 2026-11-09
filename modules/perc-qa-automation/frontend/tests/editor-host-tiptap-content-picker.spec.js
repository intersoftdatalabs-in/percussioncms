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
 * React content-picker lookup for the TipTap adv link/image dialogs (#5900).
 *
 * <p>Proves the picker browse → insert → stored-attribute flow the classic
 * {@code ContentBrowserDialog} provided: folder browse and free-text search
 * resolve through the render-link preview, the adv dialogs persist the
 * classic-identical managed attributes ({@code sys_dependentid},
 * {@code sys_dependentvariantid}, {@code data-jcrpath}), manual URL entry
 * still works as fallback, and edits persist through the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} path.</p>
 *
 * <p>Picker REST is stubbed (pathmanagement folder children, search
 * extended results, render-link preview) so the surface is deterministic;
 * the stored-attribute assertions mirror the classic
 * {@code percadvlink} / {@code percadvimage} serialization.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @html} {@code @tiptap}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-tiptap-content-picker.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
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

const BROWSE_CHILDREN = {
  PathItem: [
    { id: "9", name: "News", path: "/Sites/News/", type: "folder" },
    { id: "77", name: "About", path: "/Sites/About", type: "page" },
  ],
};

const ASSET_CHILDREN = {
  PathItem: [
    {
      id: "88",
      name: "logo.png",
      path: "/Assets/Images/logo.png",
      type: "asset",
    },
  ],
};

const LINK_PREVIEW = {
  InlineRenderLink: {
    sys_dependentid: 77,
    sys_dependentvariantid: 512,
    url: "/Sites/About",
    title: "About us",
  },
};

const IMAGE_PREVIEW = {
  InlineRenderLink: {
    sys_dependentid: 88,
    url: "/Assets/Images/logo.png",
    altText: "Company logo",
    title: "Logo",
  },
};

const SEARCH_HITS = {
  PagedItemPropertiesList: {
    childrenInPage: [
      {
        id: "77",
        title: "About",
        name: "about",
        folderPath: "/Sites",
        type: "page",
      },
    ],
    childrenCount: 1,
    startIndex: 1,
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

// Transparent 1px PNG: the editor canvas really loads an inserted
// <img src>, so the fixture binary is stubbed — otherwise the CMS content
// servlet logs a server ERROR for the missing fixture path and qa-health
// fails the run on server_log_errors.
const ONE_PX_PNG_BASE64 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

async function stubPickerApis(page) {
  await page.route("**/Assets/Images/logo.png", (route) =>
    route.fulfill({
      status: 200,
      contentType: "image/png",
      body: Buffer.from(ONE_PX_PNG_BASE64, "base64"),
    }),
  );
  await page.route("**/services/pathmanagement/path/folder/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(
        route.request().url().includes("/Assets") ? ASSET_CHILDREN : BROWSE_CHILDREN,
      ),
    }),
  );
  await page.route("**/services/searchmanagement/search/get/extendedresults", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(SEARCH_HITS),
    }),
  );
  await page.route("**/services/pagemanagement/renderlink/preview/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(
        route.request().url().includes("/88/") ? IMAGE_PREVIEW : LINK_PREVIEW,
      ),
    }),
  );
}

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

async function openTiptapEditor(page) {
  await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit&engine=tiptap"));
  await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
    timeout: 20_000,
  });
  const editable = page.locator(`[data-testid="${TEST_IDS.fieldText}"]`);
  await expect(editable).toContainText("Hi");
  return editable;
}

function savedTextField(puts) {
  const putReq = puts.find((r) => isItemFieldsPutUrl(r.url()));
  expect(putReq).toBeTruthy();
  const body = JSON.parse(putReq.postData() || "{}");
  const payload = body.ItemEditorFields || body;
  return (payload.fields || []).find((f) => f.name === "text");
}

test.describe("TipTap content-picker lookup", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "link picker browse inserts a managed link with classic attributes",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchErrors(page);
      await stubEditorApis(page, { onPut: (req) => puts.push(req) });
      await stubPickerApis(page);
      const editable = await openTiptapEditor(page);

      await page.locator('[data-testid="tiptap-perc-inline-link-text"]').click();
      await page.locator('[data-testid="tiptap-perc-link-browse-text"]').click();
      await expect(
        page.locator('[data-testid="tiptap-content-picker-text"]'),
      ).toBeVisible();
      await page.locator('[data-path="/Sites/About"]').click();
      await page.locator('[data-testid="tiptap-content-picker-use-text"]').click();
      const urlInput = page.locator('[data-testid="tiptap-perc-link-url-text"]');
      await expect(urlInput).toHaveValue("/Sites/About");
      await page.locator('[data-testid="tiptap-perc-apply-text"]').click();

      const html = await editable.innerHTML();
      // The TipTap Link mark appends the session CSRF token to same-origin
      // hrefs (pre-existing parity-slice behavior, not picker-specific).
      expect(html).toContain('href="/Sites/About?');
      expect(html).toContain('sys_dependentid="77"');
      expect(html).toContain('sys_dependentvariantid="512"');
      expect(html).toContain('data-jcrpath="/Sites/About"');
      expect(html).toContain('rxinlineslot="103"');
      expect(html).toContain('inlinetype="rxhyperlink"');

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const text = savedTextField(puts);
      expect(text && text.value).toContain('sys_dependentid="77"');
      expect(text && text.value).toContain('data-jcrpath="/Sites/About"');
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "image picker search inserts a managed image with classic attributes",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const puts = [];
      const pageErrors = watchErrors(page);
      await stubEditorApis(page, { onPut: (req) => puts.push(req) });
      await stubPickerApis(page);
      // Search path resolves the asset hit instead of folder browse.
      await page.route("**/services/pathmanagement/path/folder/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(ASSET_CHILDREN),
        }),
      );
      const editable = await openTiptapEditor(page);

      await page.locator('[data-testid="tiptap-perc-inline-image-text"]').click();
      await page.locator('[data-testid="tiptap-perc-image-browse-text"]').click();
      await expect(
        page.locator('[data-testid="tiptap-content-picker-text"]'),
      ).toBeVisible();
      await page.locator('[data-testid="tiptap-content-picker-search-text"]').fill("logo");
      await page.locator('[data-testid="tiptap-content-picker-search-button-text"]').click();
      await page.locator('[data-path="/Assets/Images/logo.png"]').click();
      await page.locator('[data-testid="tiptap-content-picker-use-text"]').click();
      const srcInput = page.locator('[data-testid="tiptap-perc-image-src-text"]');
      await expect(srcInput).toHaveValue("/Assets/Images/logo.png");
      await page.locator('[data-testid="tiptap-perc-apply-text"]').click();

      const html = await editable.innerHTML();
      expect(html).toContain('src="/Assets/Images/logo.png"');
      expect(html).toContain('alt="Company logo"');
      expect(html).toContain('sys_dependentid="88"');
      expect(html).toContain('data-jcrpath="/Assets/Images/logo.png"');
      expect(html).toContain('inlinetype="rximage"');

      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => puts.length).toBeGreaterThan(0);
      const text = savedTextField(puts);
      expect(text && text.value).toContain('sys_dependentid="88"');
      await expect(page.getByText(/^Saved$/)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "manual URL entry still works without managed attributes",
    { tag: ["@explorer-content-editor", "@editor", "@html", "@tiptap"] },
    async ({ page }) => {
      const pageErrors = watchErrors(page);
      await stubEditorApis(page);
      await stubPickerApis(page);
      const editable = await openTiptapEditor(page);

      await page.locator('[data-testid="tiptap-perc-inline-link-text"]').click();
      const urlInput = page.locator('[data-testid="tiptap-perc-link-url-text"]');
      await urlInput.fill("https://example.com/manual");
      await expect(urlInput).toHaveValue("https://example.com/manual");
      await page.locator('[data-testid="tiptap-perc-link-text-text"]').fill("Manual");
      await page.locator('[data-testid="tiptap-perc-apply-text"]').click();

      const html = await editable.innerHTML();
      expect(html).toContain('href="https://example.com/manual"');
      expect(html).not.toContain("sys_dependentid");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
