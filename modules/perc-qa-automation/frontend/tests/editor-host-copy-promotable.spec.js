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
 * React Content Editor host — new copy / promotable version (#4570).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-copy-promotable.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isNewCopyUrl,
  isPromotableUrl,
} = require("./helpers/editor-host-copy");

const PAGE_FIELDS_42 = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const PAGE_FIELDS_99 = {
  ItemEditorFields: {
    contentId: "99",
    contentType: "percPage",
    name: "Home copy",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home copy" }],
  },
};

const PAGE_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

function leftoverOn(page, leftover) {
  page.on("request", (req) => {
    const u = req.url();
    if (
      u.includes("checkoutedit.xml") ||
      u.includes("contenteditorurls.html") ||
      /view=editor/.test(u)
    ) {
      leftover.push(u);
    }
  });
}

function consoleOn(page, pageErrors) {
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      pageErrors.push(msg.text());
    }
  });
}

async function stubEditorApis(page, opts) {
  const copyCalls = opts.copyCalls;
  const copyStatus = opts.copyStatus == null ? 200 : opts.copyStatus;
  const copyBody =
    opts.copyBody ||
    JSON.stringify({
      ItemCopyResult: {
        itemId: "99",
        folderPath: "//Sites/Demo",
        promotable: false,
      },
    });
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) => {
    const url = route.request().url();
    const fields = /\/fields\/99(?:[/?#]|$)/.test(url) ? PAGE_FIELDS_99 : PAGE_FIELDS_42;
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fields),
    });
  });
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
  await page.route("**/services/itemmanagement/item/newCopy/**", async (route) => {
    copyCalls.push(route.request().url());
    await route.fulfill({
      status: copyStatus,
      contentType: "application/json",
      body: copyStatus === 200 ? copyBody : "{}",
    });
  });
  await page.route("**/services/itemmanagement/item/promotableVersion/**", async (route) => {
    copyCalls.push(route.request().url());
    await route.fulfill({
      status: copyStatus,
      contentType: "application/json",
      body:
        copyStatus === 200
          ? JSON.stringify({
              ItemCopyResult: {
                itemId: "99",
                folderPath: "//Sites/Demo",
                promotable: true,
              },
            })
          : "{}",
    });
  });
}

test.describe("React Content Editor new copy / promotable", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode confirms then opens the new copy",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const copyCalls = [];
      const leftover = [];
      const pageErrors = [];
      leftoverOn(page, leftover);
      consoleOn(page, pageErrors);
      await stubEditorApis(page, { copyCalls });
      page.on("dialog", async (dialog) => {
        expect(dialog.type()).toBe("confirm");
        await dialog.accept();
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.newCopy}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.newCopy}"]`).click();
      await expect.poll(() => copyCalls.length).toBe(1);
      expect(isNewCopyUrl(copyCalls[0])).toBe(true);
      await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText(
        "99",
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.copyError}"]`)).toHaveCount(0);
      expect(leftover, `leftover CE requested: ${leftover.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "promotable version POSTs then lands on the new id",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const copyCalls = [];
      await stubEditorApis(page, { copyCalls });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.promotable}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.promotable}"]`).click();
      await expect.poll(() => copyCalls.length).toBe(1);
      expect(isPromotableUrl(copyCalls[0])).toBe(true);
      await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText(
        "99",
      );
    },
  );

  test(
    "HTTP 403 is a failure, not success",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const copyCalls = [];
      await stubEditorApis(page, { copyCalls, copyStatus: 403 });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.newCopy}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.newCopy}"]`).click();
      await expect.poll(() => copyCalls.length).toBe(1);
      await expect(page.locator(`[data-testid="${TEST_IDS.copyError}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText(
        "42",
      );
    },
  );

  test(
    "HTTP 404 is a failure, not success",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const copyCalls = [];
      await stubEditorApis(page, { copyCalls, copyStatus: 404 });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.newCopy}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.newCopy}"]`).click();
      await expect.poll(() => copyCalls.length).toBe(1);
      await expect(page.locator(`[data-testid="${TEST_IDS.copyError}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText(
        "42",
      );
    },
  );

  test(
    "cancel confirm does not copy",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const copyCalls = [];
      await stubEditorApis(page, { copyCalls });
      page.on("dialog", async (dialog) => {
        await dialog.dismiss();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.newCopy}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.newCopy}"]`).click();
      expect(copyCalls).toEqual([]);
    },
  );

  test(
    "view mode does not show copy actions",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const copyCalls = [];
      await stubEditorApis(page, { copyCalls });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.newCopy}"]`)).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.promotable}"]`)).toHaveCount(0);
      expect(copyCalls).toEqual([]);
    },
  );
});
