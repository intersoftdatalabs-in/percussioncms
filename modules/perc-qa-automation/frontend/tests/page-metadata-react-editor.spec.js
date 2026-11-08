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
      body: JSON.stringify(metadataFields()),
    });
  });
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
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
      const sysTitle = page.locator('[data-testid="editor-field-sys_title"]');
      await expect(sysTitle).toBeVisible({ timeout: 20_000 });
      await expect(sysTitle).toHaveValue("Home");
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
