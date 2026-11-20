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
 * React Content Editor host — copy to folder names HTTP 403/409 failures and
 * stays on the dialog (#6772).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-copy-to-folder-conflict.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isFoldersCopyItemUrl,
} = require("./helpers/editor-host-copy-folder");

const TAGS = ["@explorer-content-editor", "@editor"];

const ITEM_PATH = "//Assets/edcf-item";
const ITEM_NAME = "edcf-item";
const DEST_PATH = "//Assets/edcf-dst";

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ status: number }} copyStatus
 * @param {string[]} copyPosts
 */
async function stubCopyConflictApis(page, copyStatus, copyPosts) {
  const fields = {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name: ITEM_NAME,
      checkoutUser: "Admin",
      fields: [{ name: "sys_title", value: ITEM_NAME }],
    },
  };
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fields),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ContentTypeDetail: {
          name: "percPage",
          fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
        },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName: "Draft",
          transitionTriggers: [],
        },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ SlotCanvas: { ownerId: 42, slots: [] } }),
    }),
  );
  await page.route("**/content-explorer/relationships/**/local**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ count: 0, links: [] }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemWorkflowChoices: {
          itemId: "42",
          currentWorkflowId: "1",
          choices: [{ id: "1", name: "Standard" }],
        },
      }),
    }),
  );
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, locale: "en-us", variants: [] }),
    }),
  );
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: {
          id: "",
          name: ITEM_NAME,
          path: ITEM_PATH,
          type: "asset",
        },
      }),
    }),
  );
  await page.route("**/rest/folders/copy/item**", async (route) => {
    copyPosts.push(route.request().url());
    // Empty body so the dialog shows the mapped editor message
    // (COPY_FORBIDDEN / COPY_CONFLICT): a non-blank plain-text body would
    // be surfaced verbatim instead.
    await route.fulfill({
      status: copyStatus.status,
      contentType: "text/plain",
      body: "",
    });
  });
}

function consoleOn(page, pageErrors) {
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (403|409)/.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
}

test.describe("React Content Editor copy to folder 403/409 (#6772)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "HTTP 403 and 409 stay on the dialog with named text, then cancel",
    { tag: TAGS },
    async ({ page }) => {
      const copyStatus = { status: 403 };
      /** @type {string[]} */
      const copyPosts = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubCopyConflictApis(page, copyStatus, copyPosts);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.copyToFolder}"]`)).toBeVisible({
        timeout: 30_000,
      });

      await page.locator(`[data-testid="${TEST_IDS.copyToFolder}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.destInput}"]`)).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.destInput}"]`).fill(DEST_PATH);
      await page.locator(`[data-testid="${TEST_IDS.destOk}"]`).click();

      // 403 keeps the picker open with the named error + destination folder.
      const serverError = page.locator('[data-testid="explorer-copy-dest-server-error"]');
      await expect(serverError).toContainText(/not allowed to copy/i, { timeout: 20_000 });
      await expect(serverError).toContainText(DEST_PATH);
      await expect(page.locator('[data-testid="explorer-copy-dest-picker"]')).toBeVisible();
      await expect(serverError).toBeFocused();
      await expect(page.locator(`[data-testid="${TEST_IDS.copyDone}"]`)).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText("42");

      // 409 keeps the same picker open with the conflict message.
      copyStatus.status = 409;
      await page.locator(`[data-testid="${TEST_IDS.destOk}"]`).click();
      await expect(serverError).toContainText(/already holds an item/i, {
        timeout: 20_000,
      });
      await expect(serverError).toContainText(DEST_PATH);
      await expect(page.locator('[data-testid="explorer-copy-dest-picker"]')).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.copyDone}"]`)).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText("42");

      // Cancel copies nothing new and closes the picker.
      await page.locator(`[data-testid="${TEST_IDS.destCancel}"]`).click();
      await expect(
        page.locator('[data-testid="explorer-copy-dest-picker"]'),
      ).toHaveCount(0);
      expect(copyPosts.length).toBe(2);
      expect(copyPosts.every((url) => isFoldersCopyItemUrl(url))).toBe(true);
      await expect(page.locator(`[data-testid="${TEST_IDS.copyDone}"]`)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
      await page.screenshot({ path: "test-results/live-proof-6772-copy-conflict.png" });
    },
  );
});
