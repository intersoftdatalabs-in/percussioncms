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
 * React Content Editor host — image field upload (#4679).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @image}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-image-upload.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemBinaryPutUrl,
} = require("./helpers/editor-host-image-upload");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percImage",
    name: "Hero",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Hero" }],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percImage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "img", label: "Image", control: "sys_webImageFX" },
    ],
  },
};

async function stubEditorApis(page, { binaryStatus } = {}) {
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
  await page.route("**/sys_resources/tinymce/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/javascript",
      body: "window.tinymce=undefined;",
    }),
  );
  await page.route("**/services/itemmanagement/item/binary/**", async (route) => {
    if (route.request().method() === "PUT") {
      await route.fulfill({
        status: binaryStatus ?? 200,
        contentType: "application/json",
        body:
          (binaryStatus ?? 200) === 200
            ? JSON.stringify({
                ItemEditorBinaryMeta: {
                  contentId: "42",
                  field: "img",
                  filename: "hero.png",
                  present: true,
                },
              })
            : JSON.stringify({ Error: { message: "upload failed" } }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemEditorBinaryMeta: {
          contentId: "42",
          field: "img",
          filename: "",
          present: false,
        },
      }),
    });
  });
}

test.describe("React Content Editor image field upload", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "uploads an image field on save for a checked-out item",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }) => {
      const pageErrors = [];
      const binaryPuts = [];
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
        "image",
      );
      await page.locator(`[data-testid="${TEST_IDS.fileInput}"]`).setInputFiles({
        name: "hero.png",
        mimeType: "image/png",
        buffer: Buffer.from([0x89, 0x50, 0x4e, 0x47]),
      });
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => binaryPuts.length).toBeGreaterThan(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.saved}"]`)).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
      });
    },
  );

  test(
    "view mode keeps the image input read-only",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }) => {
      await stubEditorApis(page);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.fileInput}"]`)).toBeDisabled();
      await expect(page.locator(`[data-testid="${TEST_IDS.save}"]`)).toHaveCount(0);
    },
  );

  test(
    "maps HTTP 403 image uploads as errors, not success",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }) => {
      await stubEditorApis(page, { binaryStatus: 403 });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.fileInput}"]`).setInputFiles({
        name: "hero.png",
        mimeType: "image/png",
        buffer: Buffer.from([0x89, 0x50, 0x4e, 0x47]),
      });
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toContainText(
        /not allowed to upload an image/i,
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.saved}"]`)).toHaveCount(0);
    },
  );
});
