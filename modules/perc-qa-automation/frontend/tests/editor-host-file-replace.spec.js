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
 * React Content Editor host — replace a stored non-image file field (#5090).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @file}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-file-replace.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemBinaryPutUrl,
} = require("./helpers/editor-host-file-upload");

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
      {
        name: "item_file_attachment",
        label: "File",
        control: "sys_File",
      },
    ],
  },
};

const PDF = {
  name: "next.pdf",
  mimeType: "application/pdf",
  buffer: Buffer.from("%PDF-1.4 next"),
};

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ binaryStatus?: number }} [opts]
 */
async function stubEditorApis(page, { binaryStatus } = {}) {
  let replaced = false;
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: { transitionTriggers: [] },
      }),
    }),
  );
  await page.route(
    "**/services/itemmanagement/workflow/allowedWorkflows/**",
    (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemWorkflowChoices: { choices: [] },
        }),
      }),
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
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, locale: "en-us", variants: [] }),
    }),
  );
  await page.route("**/sys_resources/tinymce/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/javascript",
      body: "window.tinymce=undefined;",
    }),
  );
  await page.route("**/services/itemmanagement/item/binary/**", async (route) => {
    const method = route.request().method();
    if (method === "PUT") {
      const status = binaryStatus ?? 200;
      if (status === 200) {
        replaced = true;
      }
      await route.fulfill({
        status,
        contentType: "application/json",
        body:
          status === 200
            ? JSON.stringify({
                ItemEditorBinaryMeta: {
                  contentId: "42",
                  field: "item_file_attachment",
                  filename: "next.pdf",
                  present: true,
                },
              })
            : JSON.stringify({ Error: { message: "replace failed" } }),
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
          filename: replaced ? "next.pdf" : "brief.pdf",
          contentType: "application/pdf",
          present: true,
        },
      }),
    });
  });
}

function trackPageErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (400|403|404|409|413|500)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("React Content Editor file field replace", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "replaces a stored file on save and shows the new name",
    { tag: ["@explorer-content-editor", "@editor", "@file"] },
    async ({ page }) => {
      const pageErrors = trackPageErrors(page);
      const binaryPuts = [];
      await stubEditorApis(page);
      page.on("request", (req) => {
        if (req.method() === "PUT" && isItemBinaryPutUrl(req.url())) {
          binaryPuts.push(req.url());
        }
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.fileField}"]`)).toHaveAttribute(
        "data-editor-kind",
        "file",
      );
      await expect(page.locator('[data-testid="editor-file-name-item_file_attachment"]')).toHaveText(
        "brief.pdf",
      );
      await page.locator(`[data-testid="${TEST_IDS.fileInput}"]`).setInputFiles(PDF);
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => binaryPuts.length).toBe(1);
      await expect(page.locator(`[data-testid="${TEST_IDS.saved}"]`)).toBeVisible();
      await expect(page.locator('[data-testid="editor-file-name-item_file_attachment"]')).toHaveText(
        "next.pdf",
      );
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-form"]',
      });
    },
  );

  test(
    "cancel on close does not replace the stored file",
    { tag: ["@explorer-content-editor", "@editor", "@file"] },
    async ({ page }) => {
      const binaryPuts = [];
      await stubEditorApis(page);
      page.on("request", (req) => {
        if (req.method() === "PUT" && isItemBinaryPutUrl(req.url())) {
          binaryPuts.push(req.url());
        }
      });
      page.on("dialog", (dialog) => dialog.dismiss());
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(
        page.locator('[data-testid="editor-file-name-item_file_attachment"]'),
      ).toHaveText("brief.pdf", { timeout: 20_000 });
      await page.locator(`[data-testid="${TEST_IDS.fileInput}"]`).setInputFiles(PDF);
      await page.locator('[data-testid="editor-close"]').click();
      await expect.poll(() => binaryPuts.length).toBe(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.saved}"]`)).toHaveCount(0);
    },
  );

  test(
    "an empty selection does not replace the stored file",
    { tag: ["@explorer-content-editor", "@editor", "@file"] },
    async ({ page }) => {
      const binaryPuts = [];
      await stubEditorApis(page);
      page.on("request", (req) => {
        if (req.method() === "PUT" && isItemBinaryPutUrl(req.url())) {
          binaryPuts.push(req.url());
        }
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(
        page.locator('[data-testid="editor-file-name-item_file_attachment"]'),
      ).toHaveText("brief.pdf", { timeout: 20_000 });
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.saved}"]`)).toBeVisible();
      expect(binaryPuts).toEqual([]);
      await expect(
        page.locator('[data-testid="editor-file-name-item_file_attachment"]'),
      ).toHaveText("brief.pdf");
    },
  );

  test(
    "HTTP 400, 403, and 409 do not claim the file was replaced",
    { tag: ["@explorer-content-editor", "@editor", "@file"] },
    async ({ page }) => {
      const expected = {
        400: /could not be uploaded/i,
        403: /not allowed to upload a file/i,
        409: /was not replaced/i,
      };
      for (const status of [400, 403, 409]) {
        await page.unrouteAll({ behavior: "ignoreErrors" });
        await stubEditorApis(page, { binaryStatus: status });
        await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
        await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await page.locator(`[data-testid="${TEST_IDS.fileInput}"]`).setInputFiles(PDF);
        await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
        await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(
          expected[status],
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.saved}"]`)).toHaveCount(0);
      }
    },
  );
});
