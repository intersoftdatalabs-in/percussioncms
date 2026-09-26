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
 * React Content Editor host — clear a stored file or image field (#4940).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @binary}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-binary-clear.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { editorSpaUrl } = require("./helpers/editor-host-image-upload");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percFile",
    name: "Brief",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Brief" }],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percFile",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "item_file_attachment", label: "File", control: "sys_File" },
    ],
  },
};

async function stubEditorApis(page, { deleteStatus = 200 } = {}) {
  const deletes = [];
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) => {
    if (route.request().method() === "PUT") {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(FIELDS),
      });
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    });
  });
  await page.route("**/services/itemmanagement/item/binary/**", async (route) => {
    const method = route.request().method();
    if (method === "DELETE") {
      deletes.push(route.request().url());
      if (deleteStatus !== 200) {
        await route.fulfill({
          status: deleteStatus,
          contentType: "text/plain",
          body:
            deleteStatus === 403
              ? "Not authorized to clear field item_file_attachment."
              : "Invalid binary field name.",
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemEditorBinaryMeta: {
            contentId: "42",
            field: "item_file_attachment",
            filename: "",
            contentType: "",
            present: false,
          },
        }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemEditorBinaryMeta: {
          contentId: "42",
          field: "item_file_attachment",
          filename: "brief.pdf",
          contentType: "application/pdf",
          present: true,
        },
      }),
    });
  });
  return deletes;
}

function trackErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (400|403|404|409|500)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("React Content Editor binary clear", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "clear without save does not delete; save does",
    { tag: ["@explorer-content-editor", "@editor", "@binary"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      const deletes = await stubEditorApis(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-file-clear-item_file_attachment"]').click();
      expect(deletes).toEqual([]);
      await page.locator('[data-testid="editor-save"]').click();
      await expect.poll(() => deletes.length).toBe(1);
      expect(deletes[0]).toContain("/binary/");
      expect(deletes[0]).toContain("item_file_attachment");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "view mode has no clear control",
    { tag: ["@explorer-content-editor", "@editor", "@binary"] },
    async ({ page }) => {
      await stubEditorApis(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-file-download-item_file_attachment"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-file-clear-item_file_attachment"]')).toHaveCount(0);
    },
  );

  test(
    "keeps HTTP 403 on the field",
    { tag: ["@explorer-content-editor", "@editor", "@binary"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      await stubEditorApis(page, { deleteStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-file-clear-item_file_attachment"]').click();
      await page.locator('[data-testid="editor-save"]').click();
      const error = page.locator('[data-testid="editor-field-error-item_file_attachment"]');
      await expect(error).toBeVisible();
      await expect(error).toContainText("item_file_attachment");
      await expect(error).toContainText(/not allowed/i);
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
