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
 * PublishingShell site-workspace publish now (#4579).
 *
 * <p>Tags: {@code @publishing} {@code @publishing-publish-now}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing/itemPublishNow.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function publishSitesUrl(siteId, itemId) {
  const root = String(BASE_URL || "").replace(/\/$/, "");
  const params = new URLSearchParams({
    entry: "publish",
    section: "sites",
    _: String(Date.now()),
  });
  if (siteId) {
    params.set("siteId", siteId);
  }
  if (itemId) {
    params.set("itemId", itemId);
  }
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

async function stubPublishSiteApis(page, opts = {}) {
  const publishStatus = opts.publishStatus ?? 200;
  const publishBody = opts.publishBody ?? { status: "Success" };

  await page.route("**/sitemanage/site**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        SiteSummary: [{ name: "Demo", siteId: "9", id: "9" }],
      }),
    });
  });
  await page.route("**/publishmanagement/servers/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/sitemanage/pubstatus/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemDates: {
          itemId: "42",
          startDate: "",
          endDate: "",
          comments: "",
        },
      }),
    });
  });
  await page.route("**/itemmanagement/item/findLinkedItems/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ArrayList: [] }),
    });
  });
  await page.route("**/sitemanage/publish/takedown/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ status: "Success" }),
    });
  });
  await page.route("**/sitemanage/publish/page/**", async (route) => {
    const url = route.request().url();
    if (url.includes("/staging/")) {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: publishStatus,
      contentType: "application/json",
      body:
        typeof publishBody === "string"
          ? publishBody
          : JSON.stringify(publishBody),
    });
  });
  await page.route("**/sitemanage/publish/resource/**", async (route) => {
    const url = route.request().url();
    if (url.includes("/staging/")) {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: publishStatus,
      contentType: "application/json",
      body:
        typeof publishBody === "string"
          ? publishBody
          : JSON.stringify(publishBody),
    });
  });
}

test.describe("PublishingShell site-workspace publish now", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "site workspace publishes a page after confirm",
    { tag: ["@publishing", "@publishing-publish-now"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page);

      await page.goto(publishSitesUrl("9", "42"));
      await expect(page.locator('[data-testid="publishing-shell"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="publish-site-workspace"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="item-publish-now"]')).toBeVisible();
      await page.locator('[data-testid="item-publish-now-review"]').click();
      await expect(page.locator('[data-testid="item-publish-now-submit"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="item-publish-now-submit"]').click();
      await expect(page.locator('[data-testid="item-publish-now-success"]')).toBeVisible({
        timeout: 10_000,
      });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "site workspace publishes a resource after confirm",
    { tag: ["@publishing", "@publishing-publish-now"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page);

      await page.goto(publishSitesUrl("9", "99"));
      await expect(page.locator('[data-testid="item-publish-now"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="item-publish-now-kind-resource"]').click();
      await page.locator('[data-testid="item-publish-now-review"]').click();
      await expect(page.locator('[data-testid="item-publish-now-submit"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="item-publish-now-submit"]').click();
      await expect(page.locator('[data-testid="item-publish-now-success"]')).toBeVisible({
        timeout: 10_000,
      });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "HTTP 403 400 and 404 are not success",
    { tag: ["@publishing", "@publishing-publish-now"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        publishStatus: 403,
        publishBody: { message: "User demo is editing this page." },
      });

      await page.goto(publishSitesUrl("9", "42"));
      await expect(page.locator('[data-testid="item-publish-now"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="item-publish-now-review"]').click();
      await expect(page.locator('[data-testid="item-publish-now-submit"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="item-publish-now-submit"]').click();
      await expect(page.locator('[data-testid="item-publish-now-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-publish-now-error"]')).toContainText(
        /editing this page|Forbidden|Publish Forbidden/i,
      );
      await expect(page.locator('[data-testid="item-publish-now-success"]')).toHaveCount(0);

      await page.unroute("**/sitemanage/publish/page/**");
      await page.route("**/sitemanage/publish/page/**", async (route) => {
        if (route.request().url().includes("/staging/")) {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ message: "Invalid item" }),
        });
      });
      await page.locator('[data-testid="item-publish-now-submit"]').click();
      await expect(page.locator('[data-testid="item-publish-now-error"]')).toContainText(
        /Invalid item|400|Bad/i,
      );
      await expect(page.locator('[data-testid="item-publish-now-success"]')).toHaveCount(0);

      await page.unroute("**/sitemanage/publish/page/**");
      await page.route("**/sitemanage/publish/page/**", async (route) => {
        if (route.request().url().includes("/staging/")) {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 404,
          contentType: "application/json",
          body: JSON.stringify({ message: "Unknown item" }),
        });
      });
      await page.locator('[data-testid="item-publish-now-submit"]').click();
      await expect(page.locator('[data-testid="item-publish-now-error"]')).toContainText(
        /Unknown item|not found|404/i,
      );
      await expect(page.locator('[data-testid="item-publish-now-success"]')).toHaveCount(0);

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
