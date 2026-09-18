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
 * React Content Editor host — Publish now for the open page/asset (#4540).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @publish}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-publish-now.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isPagePublishUrl,
  isResourcePublishUrl,
  parsePublishUrl,
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

const ASSET_FIELDS = {
  ItemEditorFields: {
    contentId: "99",
    contentType: "percRichText",
    name: "Intro",
    checkoutUser: "admin",
    fields: [{ name: "sys_title", value: "Intro" }],
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
  const fields = opts.fields || PAGE_FIELDS;
  const type = opts.type || PAGE_TYPE;
  const publishBody = opts.publishBody || "{}";
  const publishCalls = opts.publishCalls;
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
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
  await page.route("**/services/sitemanage/publish/**", async (route) => {
    publishCalls.push(route.request().url());
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: publishBody,
    });
  });
}

test.describe("React Content Editor Publish now", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode confirms then demand-publishes a percPage",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      const leftover = [];
      const pageErrors = [];
      leftoverOn(page, leftover);
      consoleOn(page, pageErrors);
      await stubEditorApis(page, { publishCalls });
      page.on("dialog", async (dialog) => {
        expect(dialog.type()).toBe("confirm");
        await dialog.accept();
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.publishNow}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.publishNow}"]`).click();
      await expect.poll(() => publishCalls.length).toBe(1);
      expect(isPagePublishUrl(publishCalls[0])).toBe(true);
      expect(parsePublishUrl(publishCalls[0]).itemId).toBe("42");
      expect(publishCalls[0]).not.toMatch(/demandpublishing/);
      await expect(page.locator(`[data-testid="${TEST_IDS.publishDone}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.publishError}"]`)).toHaveCount(0);
      expect(leftover, `leftover CE requested: ${leftover.join(" ")}`).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "HTTP 200 FORBIDDEN is a failure, not success",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      await stubEditorApis(page, {
        publishCalls,
        publishBody: JSON.stringify({
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
      await expect(page.locator(`[data-testid="${TEST_IDS.publishNow}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.publishNow}"]`).click();
      await expect.poll(() => publishCalls.length).toBe(1);
      await expect(page.locator(`[data-testid="${TEST_IDS.publishError}"]`)).toContainText(
        /Publication stopped|FORBIDDEN/i,
      );
      await expect(page.locator(`[data-testid="${TEST_IDS.publishDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "cancel confirm does not demand-publish",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      await stubEditorApis(page, { publishCalls });
      page.on("dialog", async (dialog) => {
        await dialog.dismiss();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.publishNow}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.publishNow}"]`).click();
      expect(publishCalls).toEqual([]);
      await expect(page.locator(`[data-testid="${TEST_IDS.publishDone}"]`)).toHaveCount(0);
    },
  );

  test(
    "view mode stays read-only and does not show Publish now",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      await stubEditorApis(page, { publishCalls });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.form}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.publishNow}"]`)).toHaveCount(0);
      expect(publishCalls).toEqual([]);
    },
  );

  test(
    "percRichText uses publish/resource",
    { tag: ["@explorer-content-editor", "@editor", "@publish"] },
    async ({ page }) => {
      const publishCalls = [];
      await stubEditorApis(page, {
        publishCalls,
        fields: ASSET_FIELDS,
        type: ASSET_TYPE,
      });
      page.on("dialog", async (dialog) => {
        await dialog.accept();
      });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=99&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.publishNow}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.publishNow}"]`).click();
      await expect.poll(() => publishCalls.length).toBe(1);
      expect(isResourcePublishUrl(publishCalls[0])).toBe(true);
      expect(parsePublishUrl(publishCalls[0])).toEqual({
        kind: "resource",
        itemId: "99",
      });
    },
  );
});
