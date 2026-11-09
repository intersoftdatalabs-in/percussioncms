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
 * React Content Editor host — Publish now confirm dialog with HTTP
 * 400/403/409 error mapping (#5939, parent #4532 slice 126).
 *
 * <p>The host opens a confirm dialog (not {@code window.confirm}). Confirm
 * demand-publishes; cancel calls nothing. HTTP 400/403/409 stay on the
 * dialog — they are not success and the item stays open.</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @publish}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-publish-now.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isPagePublishUrl,
} = require("./helpers/editor-host-publish");

const PAGE_FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const PAGE_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

function consoleOn(page, pageErrors) {
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
}

async function stubEditorApis(page, opts) {
  const publishStatus = opts.publishStatus || 200;
  const publishBody = opts.publishBody !== undefined ? opts.publishBody : "{}";
  const publishContentType =
    publishStatus === 200 ? "application/json" : "text/plain";
  const publishCalls = opts.publishCalls;
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_FIELDS),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_TYPE),
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
          transitionTriggers: ["Submit"],
        },
      }),
    }),
  );
  await page.route("**/services/sitemanage/publish/**", async (route) => {
    publishCalls.push(route.request().url());
    await route.fulfill({
      status: publishStatus,
      contentType: publishContentType,
      body: publishBody,
    });
  });
}

async function openPublishDialog(page) {
  await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
  await expect(page.locator(`[data-testid="${TEST_IDS.publishNow}"]`)).toBeVisible({
    timeout: 20_000,
  });
  await page.locator(`[data-testid="${TEST_IDS.publishNow}"]`).click();
  await expect(
    page.locator(`[data-testid="${TEST_IDS.publishDialog}"]`),
  ).toBeVisible();
}

test.describe("React Content Editor Publish now dialog (#5939)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "confirm demand-publishes and closes the dialog",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, { publishCalls });
      await openPublishDialog(page);
      expect(publishCalls).toEqual([]);
      await page.locator(`[data-testid="${TEST_IDS.publishConfirm}"]`).click();
      await expect.poll(() => publishCalls.length).toBe(1);
      expect(isPagePublishUrl(publishCalls[0])).toBe(true);
      await expect(page.locator(`[data-testid="${TEST_IDS.publishDone}"]`)).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.publishDialog}"]`),
      ).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "cancel does not demand-publish",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      await stubEditorApis(page, { publishCalls });
      await openPublishDialog(page);
      await page.locator(`[data-testid="${TEST_IDS.publishCancel}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.publishDialog}"]`),
      ).toHaveCount(0);
      expect(publishCalls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.publishDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "HTTP 403 stays on the dialog and is not success",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, {
        publishCalls,
        publishStatus: 403,
        publishBody: "",
      });
      await openPublishDialog(page);
      await page.locator(`[data-testid="${TEST_IDS.publishConfirm}"]`).click();
      await expect.poll(() => publishCalls.length).toBe(1);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.publishDialogError}"]`),
      ).toContainText(/not allowed/i);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.publishDialog}"]`),
      ).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.publishDone}"]`)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "HTTP 409 stays on the dialog and is not success",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, {
        publishCalls,
        publishStatus: 409,
        publishBody: "",
      });
      await openPublishDialog(page);
      await page.locator(`[data-testid="${TEST_IDS.publishConfirm}"]`).click();
      await expect.poll(() => publishCalls.length).toBe(1);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.publishDialogError}"]`),
      ).toContainText(/cannot be published right now/i);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.publishDialog}"]`),
      ).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.publishDone}"]`)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "HTTP 400 stays on the dialog and is not success",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, {
        publishCalls,
        publishStatus: 400,
        publishBody: "",
      });
      await openPublishDialog(page);
      await page.locator(`[data-testid="${TEST_IDS.publishConfirm}"]`).click();
      await expect.poll(() => publishCalls.length).toBe(1);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.publishDialogError}"]`),
      ).toContainText(/rejected/i);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.publishDialog}"]`),
      ).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.publishDone}"]`)).toHaveCount(0);
    },
  );
});
