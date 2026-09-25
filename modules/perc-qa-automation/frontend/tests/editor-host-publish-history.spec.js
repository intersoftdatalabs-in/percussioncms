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
 * React Content Editor host — publish history for the open item (#4863).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @publish}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-publish-history.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isPubHistoryUrl,
} = require("./helpers/editor-host-publish-history");

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
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    // Browser reports stubbed and unrelated CMS resource failures as console
    // errors. Uncaught page errors still fail the test.
    if (text.startsWith("Failed to load resource:")) {
      return;
    }
    pageErrors.push(text);
  });
}

async function stubEditorApis(page, opts) {
  const historyCalls = opts.historyCalls;
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
      status: 200,
      contentType: "application/json",
      body: "{}",
    });
  });
  await page.route("**/services/itemmanagement/item/pubhistory/**", async (route) => {
    historyCalls.push(route.request().url());
    await route.fulfill(opts.history);
  });
}

test.describe("React Content Editor publish history", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "lists server rows for the open content id and does not publish",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const historyCalls = [];
      const publishCalls = [];
      const leftover = [];
      const pageErrors = [];
      leftoverOn(page, leftover);
      consoleOn(page, pageErrors);
      await stubEditorApis(page, {
        historyCalls,
        publishCalls,
        history: {
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemPublishingHistory: [
              { server: "prod", operation: "publish", status: "SUCCESS" },
            ],
          }),
        },
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.history}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.history}"]`).click();
      await expect.poll(() => historyCalls.length).toBe(1);
      expect(isPubHistoryUrl(historyCalls[0])).toBe(true);
      expect(historyCalls[0]).toMatch(/\/pubhistory\/42(?:$|[?#])/);
      expect(publishCalls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.row}"]`)).toContainText("prod");
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toHaveCount(0);
      expect(leftover, `leftover CE requested: ${leftover.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.dialog}"]`,
      });
    },
  );

  test(
    "shows empty history and an HTTP error without calling publish",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const historyCalls = [];
      const publishCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, {
        historyCalls,
        publishCalls,
        history: {
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ ItemPublishingHistory: [] }),
        },
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.history}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.publishNow}"]`)).toHaveCount(0);
      await page.locator(`[data-testid="${TEST_IDS.history}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.empty}"]`)).toBeVisible();
      expect(publishCalls).toEqual([]);

      await page.unroute("**/services/itemmanagement/item/pubhistory/**");
      await page.route("**/services/itemmanagement/item/pubhistory/**", async (route) => {
        historyCalls.push(route.request().url());
        await route.fulfill({
          status: 404,
          contentType: "application/json",
          body: JSON.stringify({ message: "Not Found" }),
        });
      });
      await page.locator(`[data-testid="${TEST_IDS.close}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.history}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.error}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.empty}"]`)).toHaveCount(0);
      expect(publishCalls).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
