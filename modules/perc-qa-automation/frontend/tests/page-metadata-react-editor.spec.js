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
 * Page metadata in the React Content Editor (#5733, parent #5622 slice 6
 * batch 1 — classic `perc_page_edit_dialog` React replacement).
 *
 * <p>Tags: {@code @page-metadata-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/page-metadata-react-editor.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
} = require("./helpers/editor-host-save-fields");

const PAGE_ID = "42";

function metadataFields() {
  return {
    ItemEditorFields: {
      contentId: PAGE_ID,
      contentType: "percPage",
      name: "Home",
      checkoutUser: "Admin",
      revision: 3,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "resource_link_title", value: "Home" },
        { name: "page_title", value: "Home page" },
        { name: "page_summary", value: "Welcome to the home page." },
        { name: "page_description", value: "Home page description." },
        { name: "auto_generate_summary", value: "no" },
      ],
    },
  };
}

const BASE_ID = "16777215-101-1";
const PLAIN_ID = "16777215-101-2";

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      { name: "sys_title", label: "Filename", control: "sys_EditBox" },
      {
        name: "resource_link_title",
        label: "Link text",
        control: "sys_EditBox",
      },
      { name: "page_title", label: "Browser title", control: "sys_EditBox" },
      { name: "page_summary", label: "Page summary", control: "sys_EditBox" },
      {
        name: "page_description",
        label: "Page description",
        control: "sys_EditBox",
      },
      {
        name: "auto_generate_summary",
        label: "Auto-generate summary",
        control: "sys_CheckBox",
      },
    ],
    allowedTemplates: [
      { name: "perc.base.page", label: "Base", guid: { stringValue: BASE_ID } },
      { name: "perc.base.plain", label: "Plain", guid: { stringValue: PLAIN_ID } },
    ],
  },
};

async function mockEditorHost(page, { onFieldsPut } = {}) {
  await mockEditorHostRoutes(page, {
    onFieldsPut,
    fields: metadataFields(),
    type: TYPE,
  });
}

async function mockEditorHostRoutes(
  page,
  { onFieldsPut, fields, type, siteImpact } = {},
) {
  const fieldsBody = fields || metadataFields();
  const typeBody = type || TYPE;
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
      if (isItemFieldsPutUrl(route.request().url()) && onFieldsPut) {
        onFieldsPut(route.request().postData() || "");
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody),
    });
  });
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(typeBody),
    }),
  );
  await page.route("**/itemmanagement/item/siteimpact/page/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(
        siteImpact || { pages: [], templates: [] },
      ),
    }),
  );
  await page.route("**/pagemanagement/page/changeTemplate/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "{}",
    }),
  );
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: { path: "//Sites/Demo/Home", name: "Home" },
      }),
    }),
  );
  await page.route("**/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ stateName: "Draft", transitionTriggers: [] }),
    }),
  );
  await page.route("**/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, currentWorkflowId: "1", choices: [] }),
    }),
  );
  await page.route("**/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        SlotCanvas: { ownerId: 42, templateId: null, slots: [] },
      }),
    }),
  );
  await page.route("**/content-explorer/relationships/**/local", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ count: 0, links: [] }),
    }),
  );
  await page.route("**/content-explorer/translations/**", (route) =>
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
}

test.describe("React page metadata editor (classic dialog replacement)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "page metadata renders and saves through the React editor",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const fieldPuts = [];
      const legacyDialogRequests = [];
      const badResponses = [];
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("response", (res) => {
        if (res.status() >= 400) {
          badResponses.push(`${res.status()} ${res.url()}`);
        }
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      page.on("request", (req) => {
        if (/perc_page_edit_dialog|pageEditUrl|PAGE_EDITURL/i.test(req.url())) {
          legacyDialogRequests.push(req.url());
        }
      });
      await mockEditorHost(page, {
        onFieldsPut: (body) => fieldPuts.push(body),
      });

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=edit`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      // Field-level parity with the retired dialog: filename, link text,
      // browser title, summary and description all render for a percPage.
      // Batch-2 groups (#5739): Page Summary starts open; SEO starts
      // collapsed and expands on toggle.
      const sysTitle = page.locator('[data-testid="editor-field-sys_title"]');
      await expect(sysTitle).toBeVisible({ timeout: 20_000 });
      await expect(sysTitle).toHaveValue("Home");
      await page
        .locator('[data-testid="editor-metadata-group-toggle-seo"]')
        .click();
      await expect(
        page.locator('[data-testid="editor-field-page_title"]'),
      ).toHaveValue("Home page");
      await expect(
        page.locator('[data-testid="editor-field-page_summary"]'),
      ).toHaveValue("Welcome to the home page.");
      await expect(
        page.locator('[data-testid="editor-field-page_description"]'),
      ).toHaveValue("Home page description.");

      await sysTitle.fill("Home Renamed");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toContain("Home Renamed");
      expect(fieldPuts[0]).toContain('"name":"sys_title"');
      expect(
        legacyDialogRequests,
        `retired dialog must not be requested: ${legacyDialogRequests.join(" ")}`,
      ).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        badResponses,
        `HTTP 400+ responses: ${badResponses.join(" | ")}`,
      ).toEqual([]);
    },
  );

  test(
    "page metadata view mode is read-only",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      await mockEditorHost(page);

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=view`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const sysTitle = page.locator('[data-testid="editor-field-sys_title"]');
      await expect(sysTitle).toBeVisible({ timeout: 20_000 });
      await expect(sysTitle).not.toBeEditable();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});

function batch2Fields(autoSummaryValue = "no") {
  return {
    ItemEditorFields: {
      contentId: PAGE_ID,
      contentType: "percPage",
      name: "Home",
      checkoutUser: "Admin",
      revision: 3,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "resource_link_title", value: "Home" },
        { name: "page_title", value: "Home page" },
        {
          name: "page_summary",
          value: "<p>Welcome<script>alert(1)</script></p>",
        },
        { name: "sys_contentpostdate", value: "2026-01-02" },
        { name: "sys_contentpostdatetz", value: "America/New_York" },
        { name: "auto_generate_summary", value: autoSummaryValue },
        { name: "page_description", value: "Home page description." },
        { name: "page_noindex", value: "no" },
        { name: "page_tags", value: "" },
        { name: "page_categories_tree", value: "" },
        { name: "page_calendar", value: "" },
        { name: "additional_head_content", value: "" },
        { name: "code_insert_after_body_start", value: "" },
        { name: "code_insert_before_body_close", value: "" },
      ],
    },
  };
}

function batch2Type() {
  const editBox = (name, label) => ({ name, label, control: "sys_EditBox" });
  return {
    ContentTypeDetail: {
      name: "percPage",
      fields: [
        editBox("sys_title", "Filename"),
        editBox("resource_link_title", "Link text"),
        editBox("page_title", "Browser title"),
        editBox("page_summary", "Page summary"),
        {
          name: "sys_contentpostdate",
          label: "Post date",
          control: "sys_Calendar",
        },
        editBox("sys_contentpostdatetz", "Post date time zone"),
        {
          name: "auto_generate_summary",
          label: "Auto-generate summary",
          control: "sys_SingleCheckBox",
        },
        editBox("page_description", "Page description"),
        {
          name: "page_noindex",
          label: "No index",
          control: "sys_SingleCheckBox",
        },
        editBox("page_tags", "Tags"),
        editBox("page_categories_tree", "Categories"),
        {
          name: "page_calendar",
          label: "Calendar",
          control: "sys_Calendar",
        },
        editBox("additional_head_content", "Additional head content"),
        editBox("code_insert_after_body_start", "Code after body start"),
        editBox("code_insert_before_body_close", "Code before body close"),
      ],
      allowedTemplates: [
        { name: "perc.base.page", label: "Base", guid: { stringValue: BASE_ID } },
        { name: "perc.base.plain", label: "Plain", guid: { stringValue: PLAIN_ID } },
      ],
    },
  };
}

function batch2SiteImpact() {
  return {
    pages: [
      {
        id: "7",
        name: "Home",
        path: "/Sites/Demo/Home",
        status: "Live",
        type: "Page",
      },
    ],
    templates: [{ template: { name: "Base" }, site: "Demo" }],
  };
}

function watchPage(page, errors, badResponses) {
  page.on("pageerror", (err) => errors.push(String(err)));
  page.on("response", (res) => {
    if (res.status() >= 400) {
      badResponses.push(`${res.status()} ${res.url()}`);
    }
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      errors.push(msg.text());
    }
  });
}

function expectClean(errors, badResponses) {
  expect(errors, `console/page errors: ${errors.join(" | ")}`).toEqual([]);
  expect(
    badResponses,
    `HTTP 400+ responses: ${badResponses.join(" | ")}`,
  ).toEqual([]);
}

test.describe("React page metadata editor batch-2 chrome (#5739)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "collapsible field groups render with Page Summary open",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const errors = [];
      const badResponses = [];
      watchPage(page, errors, badResponses);
      await mockEditorHostRoutes(page, {
        fields: batch2Fields(),
        type: batch2Type(),
        siteImpact: batch2SiteImpact(),
      });

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=edit`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      for (const id of [
        "page-summary",
        "seo",
        "tags-categories",
        "calendar",
        "additional-code",
      ]) {
        await expect(
          page.locator(`[data-testid="editor-metadata-group-toggle-${id}"]`),
        ).toBeVisible({ timeout: 20_000 });
      }
      // Page Summary starts open; SEO starts collapsed (classic parity).
      await expect(
        page.locator('[data-testid="editor-field-row-sys_title"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="editor-field-row-page_title"]'),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="editor-metadata-group-toggle-seo"]'),
      ).toHaveAttribute("aria-expanded", "false");

      await page
        .locator('[data-testid="editor-metadata-group-toggle-seo"]')
        .click();
      await expect(
        page.locator('[data-testid="editor-field-row-page_title"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="editor-metadata-group-toggle-seo"]'),
      ).toHaveAttribute("aria-expanded", "true");
      // Classic dialog forced aria-required on filename / link text /
      // browser title independent of schema flags.
      await expect(
        page.locator('[data-testid="editor-field-sys_title"]'),
      ).toHaveAttribute("aria-required", "true");
      await expect(
        page.locator('[data-testid="editor-field-page_title"]'),
      ).toHaveAttribute("aria-required", "true");
      // Override-post-date note from the retired dialog.
      await expect(
        page.locator('[data-testid="editor-metadata-postdate-note"]'),
      ).toBeVisible();
      expectClean(errors, badResponses);
    },
  );

  test(
    "auto-generate summary swaps the editor for a sanitized preview",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const errors = [];
      const badResponses = [];
      watchPage(page, errors, badResponses);
      await mockEditorHostRoutes(page, {
        fields: batch2Fields("yes"),
        type: batch2Type(),
        siteImpact: batch2SiteImpact(),
      });

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=edit`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const preview = page.locator(
        '[data-testid="editor-metadata-summary-preview"]',
      );
      await expect(preview).toBeVisible({ timeout: 20_000 });
      await expect(preview).toContainText("Welcome");
      await expect(
        page.locator('[data-testid="editor-field-page_summary"]'),
      ).toHaveCount(0);

      // Toggling back off restores the summary editor.
      await page
        .locator('[data-testid="editor-field-auto_generate_summary"]')
        .selectOption("no");
      await expect(
        page.locator('[data-testid="editor-field-page_summary"]'),
      ).toBeVisible();
      await expect(preview).toHaveCount(0);
      expectClean(errors, badResponses);
    },
  );

  test(
    "filename filter trims trailing dots on save",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const errors = [];
      const badResponses = [];
      const fieldPuts = [];
      watchPage(page, errors, badResponses);
      await mockEditorHostRoutes(page, {
        fields: batch2Fields(),
        type: batch2Type(),
        siteImpact: batch2SiteImpact(),
        onFieldsPut: (body) => fieldPuts.push(body),
      });

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=edit`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const sysTitle = page.locator('[data-testid="editor-field-sys_title"]');
      await expect(sysTitle).toBeVisible({ timeout: 20_000 });
      await sysTitle.fill("Home...   ");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toContain('"name":"sys_title","value":"Home"');
      expectClean(errors, badResponses);
    },
  );

  test(
    "site impact panel lists impacting pages and templates",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const errors = [];
      const badResponses = [];
      watchPage(page, errors, badResponses);
      await mockEditorHostRoutes(page, {
        fields: batch2Fields(),
        type: batch2Type(),
        siteImpact: batch2SiteImpact(),
      });

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=edit`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="editor-site-impact-panel"]'),
      ).toBeVisible({ timeout: 20_000 });
      const pageRow = page.locator(
        '[data-testid="editor-site-impact-page-row"]',
      );
      await expect(pageRow).toContainText("Home");
      await expect(pageRow).toContainText("Demo");
      await expect(pageRow).toContainText("Live");
      await expect(
        page.locator('[data-testid="editor-site-impact-template-row"]'),
      ).toContainText("Base");
      expectClean(errors, badResponses);
    },
  );

  test(
    "site impact panel renders an explicit empty state",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const errors = [];
      const badResponses = [];
      watchPage(page, errors, badResponses);
      await mockEditorHostRoutes(page, {
        fields: batch2Fields(),
        type: batch2Type(),
        siteImpact: { pages: [], templates: [] },
      });

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=edit`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="editor-site-impact-empty"]'),
      ).toBeVisible({ timeout: 20_000 });
      expectClean(errors, badResponses);
    },
  );
});
