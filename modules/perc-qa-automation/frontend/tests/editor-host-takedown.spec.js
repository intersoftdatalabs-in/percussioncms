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
 * React Content Editor host — Take down the open page/asset (#4862).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @publish}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-takedown.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  editorSpaUrl,
  isPageTakedownUrl,
  isResourceTakedownUrl,
  parseTakedownUrl,
} = require("./helpers/editor-host-takedown");

const PAGE_FIELDS = {
  ItemEditorFields: {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Home" }],
  },
};

const ASSET_FIELDS = {
  ItemEditorFields: {
    contentId: "99",
    contentType: "percRichText",
    name: "Intro",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Intro" }],
  },
};

const FOLDER_FIELDS = {
  ItemEditorFields: {
    contentId: "8",
    contentType: "Folder",
    name: "News",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "News" }],
  },
};

const PAGE_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

const ASSET_TYPE = {
  ContentTypeDetail: {
    name: "percRichText",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

function consoleOn(page, pageErrors) {
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      pageErrors.push(msg.text());
    }
  });
}

async function stubEditorApis(page, opts) {
  const fields = opts.fields || PAGE_FIELDS;
  const type = opts.type || PAGE_TYPE;
  const takedownBody = opts.takedownBody || "{}";
  const takedownCalls = opts.takedownCalls;
  const linked = opts.linked || [];
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
      body: JSON.stringify(fields),
    }),
  );
  await page.route("**/services/itemmanagement/item/findLinkedItems/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(linked),
    }),
  );

  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(type),
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
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, variants: [] }),
    }),
  );
  await page.route("**/services/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ path: "/Sites/Demo/Home" }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ choices: [] }),
    }),
  );
  await page.route("**/services/assembly/slot-relationships/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({}),
    }),
  );
  await page.route("**/services/sitemanage/publish/takedown/**", async (route) => {
    takedownCalls.push({
      url: route.request().url(),
      method: route.request().method(),
    });
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: takedownBody,
    });
  });
}

test.describe("React Content Editor Take down", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode confirms then takes down a percPage (#4862)",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const takedownCalls = [];
      const pageErrors = [];
      consoleOn(page, pageErrors);
      await stubEditorApis(page, {
        takedownCalls,
        linked: [
          {
            PageLinkedToItem: {
              id: "7",
              pagePath: "/Sites/Demo/Home",
              relationshipId: "rel-1",
            },
          },
        ],
      });
      page.on("dialog", async (dialog) => {
        expect(dialog.message()).toContain("/Sites/Demo/Home");
        await dialog.accept();
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.takedown}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.takedown}"]`).click();
      await expect.poll(() => takedownCalls.length).toBe(1);
      expect(isPageTakedownUrl(takedownCalls[0].url)).toBe(true);
      expect(takedownCalls[0].method).toBe("PUT");
      expect(parseTakedownUrl(takedownCalls[0].url).itemId).toBe("42");
      await expect(page.locator(`[data-testid="${TEST_IDS.takedownDone}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.takedownError}"]`)).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "HTTP 200 FORBIDDEN is a failure, not success",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const takedownCalls = [];
      await stubEditorApis(page, {
        takedownCalls,
        takedownBody: JSON.stringify({
          SitePublishResponse: {
            status: "FORBIDDEN",
            warningMessage: "Publication stopped",
          },
        }),
      });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.takedown}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.takedown}"]`).click();
      await expect.poll(() => takedownCalls.length).toBe(1);
      expect(takedownCalls[0].method).toBe("GET");
      await expect(page.locator(`[data-testid="${TEST_IDS.takedownError}"]`)).toContainText(
        /Publication stopped|FORBIDDEN/i,
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.takedownDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "cancel confirm does not take down",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const takedownCalls = [];
      await stubEditorApis(page, { takedownCalls });
      page.on("dialog", async (dialog) => {
        await dialog.dismiss();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.takedown}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.takedown}"]`).click();
      expect(takedownCalls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.takedownDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "view mode, folders, and a new unsaved item hide Take down",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const takedownCalls = [];
      await stubEditorApis(page, { takedownCalls });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.takedown}"]`)).toHaveCount(0);

      await stubEditorApis(page, { takedownCalls, fields: FOLDER_FIELDS });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=8&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.takedown}"]`)).toHaveCount(0);

      await page.goto(editorSpaUrl(BASE_URL, "mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.takedown}"]`)).toHaveCount(0);
      expect(takedownCalls).toEqual([]);
    },
  );

  test(
    "percRichText uses takedown/resource",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const takedownCalls = [];
      await stubEditorApis(page, {
        takedownCalls,
        fields: ASSET_FIELDS,
        type: ASSET_TYPE,
      });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=99&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.takedown}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.takedown}"]`).click();
      await expect.poll(() => takedownCalls.length).toBe(1);
      expect(isResourceTakedownUrl(takedownCalls[0].url)).toBe(true);
      expect(parseTakedownUrl(takedownCalls[0].url)).toEqual({
        kind: "resource",
        itemId: "99",
      });
    },
  );
});
