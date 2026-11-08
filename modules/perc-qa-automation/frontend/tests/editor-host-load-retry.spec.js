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
 * React Content Editor host — retry the item load after a failure (#5767,
 * parent #4532 slice 1: open + render increment).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @open}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-load-retry.spec.js}</p>
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
} = require("./helpers/editor-host-save-fields");

const FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    revision: 2,
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "displaytitle", value: "Welcome" },
    ],
  },
};

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox", required: true },
      {
        name: "displaytitle",
        label: "Display title",
        control: "sys_EditBox",
        required: true,
      },
    ],
  },
};

async function stubEditorApis(page, { failFieldsTimes = 0 } = {}) {
  let fieldsGets = 0;
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkin", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
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
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(FIELDS),
      });
      return;
    }
    fieldsGets += 1;
    if (fieldsGets <= failFieldsTimes) {
      await route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ Error: { message: "boom" } }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    });
  });
  return () => fieldsGets;
}

function trackPageProblems(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    // Browser-level resource noise (favicon, auxiliary catalog calls for the
    // stubbed fixture, and the stubbed 500 fields GET itself). Real app
    // breakage still surfaces via pageerror and non-resource console errors.
    if (/Failed to load resource/i.test(text)) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("React Content Editor load retry", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "retry after a failed load renders the form",
    { tag: ["@explorer-content-editor", "@editor", "@open"] },
    async ({ page }) => {
      const pageErrors = trackPageProblems(page);
      const fieldsGets = await stubEditorApis(page, { failFieldsTimes: 1 });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-retry-load"]')).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toHaveCount(0);
      await page.locator('[data-testid="editor-retry-load"]').click();
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toHaveCount(0);
      expect(fieldsGets()).toBeGreaterThanOrEqual(2);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "a failed retry keeps the alert and is not success",
    { tag: ["@explorer-content-editor", "@editor", "@open"] },
    async ({ page }) => {
      const pageErrors = trackPageProblems(page);
      const fieldsGets = await stubEditorApis(page, { failFieldsTimes: 99 });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator('[data-testid="editor-retry-load"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="editor-retry-load"]').click();
      await expect.poll(() => fieldsGets()).toBeGreaterThanOrEqual(2);
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toHaveCount(0);
      await expect(page.getByText(/^Saved$/)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
