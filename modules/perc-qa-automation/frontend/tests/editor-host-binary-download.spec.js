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
 * React Content Editor host — download a stored file or image field (#4939).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @binary}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-binary-download.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { editorSpaUrl } = require("./helpers/editor-host-image-upload");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percFile",
    name: "Brief",
    checkoutUser: "",
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

async function stubEditorApis(page, { present = true, contentStatus = 200 } = {}) {
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
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    }),
  );
  await page.route("**/services/itemmanagement/item/binary/**", async (route) => {
    const url = route.request().url();
    if (url.includes("/content")) {
      if (contentStatus !== 200) {
        await route.fulfill({
          status: contentStatus,
          contentType: "text/plain",
          body:
            contentStatus === 403
              ? "Not authorized to download field item_file_attachment."
              : "Field item_file_attachment has no file.",
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/pdf",
        headers: {
          "Content-Disposition": 'attachment; filename="brief.pdf"',
        },
        body: Buffer.from("%PDF-1.4 stored"),
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
          filename: present ? "brief.pdf" : "",
          contentType: present ? "application/pdf" : "",
          present,
        },
      }),
    });
  });
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
      /Failed to load resource: the server responded with a status of (403|404|409|500)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("React Content Editor binary download", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "downloads the stored file name in view mode",
    { tag: ["@explorer-content-editor", "@editor", "@binary"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      const contentGets = [];
      await stubEditorApis(page);
      page.on("request", (req) => {
        if (req.method() === "GET" && req.url().includes("/binary/") && req.url().includes("/content")) {
          contentGets.push(req.url());
        }
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      const button = page.locator('[data-testid="editor-file-download-item_file_attachment"]');
      await expect(button).toBeEnabled();
      const downloadPromise = page.waitForEvent("download");
      await button.click();
      const download = await downloadPromise;
      expect(download.suggestedFilename()).toBe("brief.pdf");
      await expect.poll(() => contentGets.length).toBeGreaterThan(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "names a field with no binary instead of an empty file",
    { tag: ["@explorer-content-editor", "@editor", "@binary"] },
    async ({ page }) => {
      const contentGets = [];
      await stubEditorApis(page, { present: false });
      page.on("request", (req) => {
        if (req.url().includes("/item/binary/") && req.url().includes("/content")) {
          contentGets.push(req.url());
        }
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-file-download-item_file_attachment"]').click();
      const error = page.locator(
        '[data-testid="editor-file-download-error-item_file_attachment"]',
      );
      await expect(error).toBeVisible();
      await expect(error).toContainText("item_file_attachment");
      expect(contentGets).toEqual([]);
    },
  );

  test(
    "keeps HTTP 403 on the field",
    { tag: ["@explorer-content-editor", "@editor", "@binary"] },
    async ({ page }) => {
      const pageErrors = trackErrors(page);
      await stubEditorApis(page, { contentStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator('[data-testid="editor-form"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-file-download-item_file_attachment"]').click();
      const error = page.locator(
        '[data-testid="editor-file-download-error-item_file_attachment"]',
      );
      await expect(error).toBeVisible();
      await expect(error).toContainText("item_file_attachment");
      await expect(error).toContainText(/not allowed/i);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
