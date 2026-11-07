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
 * React Content Editor refuses an image field NUL on save (#5695 / #4532).
 *
 * <p>A NUL cannot be stored in item field XML / JDBC text, so a stored image
 * name that contains U+0000 is refused before the save. The client gate in
 * {@code ImageFieldWidget.tsx} reads the raw field value and the picked file
 * name; the rendered name is sanitized, so the display still shows a clean
 * name. Mirrors the file-filename NUL slice (#5588); the file gate covers
 * {@code kind === "file"} only, so image rows need their own gate.</p>
 *
 * <p>Peer: {@code tests/editor-host-file-nul.spec.js}. TipTap rich-text NUL
 * stays in slice 111 (#5696).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @image}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-image-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  editorSpaUrl,
  isItemBinaryPutUrl,
} = require("./helpers/editor-host-file-upload");

const NUL = "\u0000";
const STORED_NAME = "old.png";
const NUL_NAME = "ba" + NUL + "d.png";

const FORM = "editor-form";
const SAVE = "editor-save";
const SAVE_ERROR = "editor-save-error";
const SAVED = "editor-saved";
const FILE_INPUT = "editor-file-img";
const FIELD_ERROR = "editor-field-error-img";
const FILE_NAME = "editor-file-name-img";

const TYPE = {
  ContentTypeDetail: {
    name: "percImage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "img", label: "Image", control: "sys_webImageFX" },
    ],
  },
};

const PNG = {
  name: "new.png",
  mimeType: "image/png",
  buffer: Buffer.from([0x89, 0x50, 0x4e, 0x47]),
};

function fieldsPayload(imageName) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percImage",
      name: "Hero",
      checkoutUser: "admin",
      revision: 2,
      fields: [
        { name: "sys_title", value: "Hero" },
        { name: "img", value: imageName },
      ],
    },
  };
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ imageName?: string, binaryStatus?: number }} [opts]
 */
async function stubEditorApis(page, { imageName, binaryStatus } = {}) {
  const stored = imageName ?? STORED_NAME;
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
      body: JSON.stringify(fieldsPayload(stored)),
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
                  field: "img",
                  filename: "new.png",
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
          field: "img",
          filename: replaced ? "new.png" : STORED_NAME,
          contentType: "image/png",
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

test.describe("React Content Editor image field NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a stored image name with a NUL, keeps the previous image, and still saves an ordinary replace",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }) => {
      const pageErrors = trackPageErrors(page);
      const fieldPuts = [];
      const binaryPuts = [];
      await stubEditorApis(page, { imageName: NUL_NAME });
      page.on("request", (req) => {
        if (req.method() === "PUT") {
          if (isItemBinaryPutUrl(req.url())) {
            binaryPuts.push(req.url());
          } else if (/itemmanagement\/item\/fields/.test(req.url())) {
            fieldPuts.push(req.url());
          }
        }
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${FORM}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-field-img"]')).toHaveAttribute(
        "data-editor-kind",
        "image",
      );

      await page.locator(`[data-testid="${SAVE}"]`).click();
      await expect(page.locator(`[data-testid="${FIELD_ERROR}"]`)).toContainText(
        /character that cannot be saved/i,
      );
      await expect(page.locator(`[data-testid="${SAVE_ERROR}"]`)).toContainText(
        /image fields before saving/i,
      );
      expect(fieldPuts).toEqual([]);
      expect(binaryPuts).toEqual([]);
      await expect(page.locator(`[data-testid="${SAVED}"]`)).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${FILE_NAME}"]`)).toHaveText(
        STORED_NAME,
        { timeout: 20_000 },
      );
      expect(fieldPuts).toEqual([]);
      expect(binaryPuts).toEqual([]);

      await page.unrouteAll({ behavior: "ignoreErrors" });
      await stubEditorApis(page, { imageName: STORED_NAME });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${FORM}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${FILE_INPUT}"]`).setInputFiles(PNG);
      await page.locator(`[data-testid="${SAVE}"]`).click();
      await expect.poll(() => binaryPuts.length).toBe(1);
      await expect(page.locator(`[data-testid="${SAVED}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${FILE_NAME}"]`)).toHaveText("new.png");

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-form"]',
      });
    },
  );

  test(
    "cancel on close does not replace the stored image",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
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
      await expect(page.locator(`[data-testid="${FILE_NAME}"]`)).toHaveText(
        STORED_NAME,
        { timeout: 20_000 },
      );
      await page.locator(`[data-testid="${FILE_INPUT}"]`).setInputFiles(PNG);
      await page.locator('[data-testid="editor-close"]').click();
      await expect.poll(() => binaryPuts.length).toBe(0);
      await expect(page.locator(`[data-testid="${SAVED}"]`)).toHaveCount(0);
    },
  );

  test(
    "HTTP 400, 403, and 409 do not claim the image was replaced",
    { tag: ["@explorer-content-editor", "@editor", "@image"] },
    async ({ page }) => {
      for (const status of [400, 403, 409]) {
        await page.unrouteAll({ behavior: "ignoreErrors" });
        await stubEditorApis(page, { binaryStatus: status });
        await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
        await expect(page.locator(`[data-testid="${FORM}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await page.locator(`[data-testid="${FILE_INPUT}"]`).setInputFiles(PNG);
        await page.locator(`[data-testid="${SAVE}"]`).click();
        await expect(page.locator(`[data-testid="${SAVE_ERROR}"]`)).toBeVisible();
        await expect(page.locator(`[data-testid="${SAVED}"]`)).toHaveCount(0);
      }
    },
  );
});
