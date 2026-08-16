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
 * React Content Editor rich controls (TinyMCE, file, image, keyword, community).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-rich-controls.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

function editorSpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const params = new URLSearchParams(query.startsWith("?") ? query.slice(1) : query);
  params.set("entry", "editor");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percRichText",
    name: "Intro",
    checkoutUser: "admin",
    fields: [
      { name: "sys_title", value: "Intro" },
      { name: "text", value: "<p>Hi</p>" },
      { name: "keywords", value: "news" },
      { name: "sys_communityid", value: "10" },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percRichText",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "text", label: "Body", control: "sys_tinymce" },
      { name: "img", label: "Image", control: "sys_webImageFX" },
      { name: "item_file_attachment", label: "File", control: "sys_File" },
      { name: "keywords", label: "Keywords", control: "sys_DropDownSingle" },
      { name: "sys_communityid", label: "Community", control: "sys_DropDownSingle" },
    ],
  },
};

test.describe("React Content Editor rich controls", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "renders TinyMCE, file, image, keyword, and community widgets without leftover CE HTML",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const blocked = [];
      const fieldPuts = [];
      const binaryPuts = [];
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("checkoutedit.xml") ||
          u.includes("contenteditorurls.html") ||
          u.includes("sys_ceSupport") ||
          /view=editor/.test(u)
        ) {
          blocked.push(u);
        }
      });
      await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
        route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
      );
      await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
        if (route.request().method() === "PUT") {
          fieldPuts.push(route.request().postData() || "");
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(FIELDS),
        });
      });
      await page.route("**/services/itemmanagement/item/binary/**", async (route) => {
        const url = route.request().url();
        if (route.request().method() === "PUT") {
          binaryPuts.push(url);
        }
        const fieldMatch = url.match(/\/item\/binary\/[^/]+\/([^/?#]+)/);
        const field = fieldMatch ? decodeURIComponent(fieldMatch[1]) : "";
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorBinaryMeta: {
              contentId: "42",
              field,
              filename: field === "img" ? "hero.png" : "spec.pdf",
              present: true,
            },
          }),
        });
      });
      await page.route("**/services/contenttypes/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(TYPE),
        }),
      );
      await page.route("**/services/keywords**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([
            {
              value: "keywords",
              label: "Keywords",
              choices: [
                { value: "news", label: "News" },
                { value: "events", label: "Events" },
              ],
            },
          ]),
        }),
      );
      await page.route("**/services/communities/find**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify([
            { id: 10, name: "Default", label: "Default" },
            { id: 20, name: "Enterprise", label: "Enterprise" },
          ]),
        }),
      );
      await page.route("**/sys_resources/tinymce/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/javascript",
          body: "window.tinymce=undefined;",
        }),
      );

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-field-text"]')).toHaveAttribute(
        "data-editor-kind",
        "html",
      );
      await expect(page.locator('[data-testid="editor-field-img"]')).toHaveAttribute(
        "data-editor-kind",
        "image",
      );
      await expect(
        page.locator('[data-testid="editor-field-item_file_attachment"]'),
      ).toHaveAttribute("data-editor-kind", "file");
      await expect(page.locator('[data-testid="editor-field-keywords"]')).toHaveAttribute(
        "data-editor-kind",
        "keyword",
      );
      await expect(page.locator('[data-testid="editor-field-sys_communityid"]')).toHaveAttribute(
        "data-editor-kind",
        "community",
      );
      await page.locator('[data-testid="editor-field-keywords"]').selectOption("events");
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => fieldPuts.length).toBeGreaterThan(0);
      expect(fieldPuts[0]).toMatch(/"name"\s*:\s*"keywords"\s*,\s*"value"\s*:\s*"events"/);
      expect(blocked, `leftover CE requested: ${blocked.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
      });
    },
  );

  test(
    "promote mode shows the revision form and does not request leftover CE",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        if (/view=editor/.test(req.url()) || req.url().includes("checkoutedit.xml")) {
          blocked.push(req.url());
        }
      });
      await page.route("**/services/itemmanagement/item/revisions/**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            RevisionsSummary: {
              restorable: true,
              revisions: [
                { revId: 2, lastModifier: "admin", status: "Draft", lastModifiedDate: "2026-01-01" },
              ],
            },
          }),
        }),
      );
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=promote"));
      await expect(page.locator('[data-testid="editor-promote-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-save"]')).toHaveCount(0);
      expect(blocked, `leftover CE requested: ${blocked.join(" ")}`).toEqual([]);
    },
  );
});
