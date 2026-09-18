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
 * PublishingShell site-workspace stage / remove-from-staging (#4580).
 *
 * <p>Tags: {@code @publishing} {@code @publishing-stage}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing/itemStage.spec.js}
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
  const stageStatus = opts.stageStatus ?? 200;
  const stageBody = opts.stageBody ?? { status: "Success" };

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
  // Stage endpoint: …/sitemanage/publish/page/staging/{id}
  await page.route("**/sitemanage/publish/page/staging/**", async (route) => {
    await route.fulfill({
      status: stageStatus,
      contentType: "application/json",
      body:
        typeof stageBody === "string"
          ? stageBody
          : JSON.stringify(stageBody),
    });
  });
  await page.route("**/sitemanage/publish/resource/staging/**", async (route) => {
    await route.fulfill({
      status: stageStatus,
      contentType: "application/json",
      body:
        typeof stageBody === "string"
          ? stageBody
          : JSON.stringify(stageBody),
    });
  });
  // Unstage endpoint: …/sitemanage/publish/takedown/page/staging/{id}
  await page.route(
    "**/sitemanage/publish/takedown/page/staging/**",
    async (route) => {
      await route.fulfill({
        status: stageStatus,
        contentType: "application/json",
        body:
          typeof stageBody === "string"
            ? stageBody
            : JSON.stringify(stageBody),
      });
    },
  );
  await page.route(
    "**/sitemanage/publish/takedown/resource/staging/**",
    async (route) => {
      await route.fulfill({
        status: stageStatus,
        contentType: "application/json",
        body:
          typeof stageBody === "string"
            ? stageBody
            : JSON.stringify(stageBody),
      });
    },
  );
}

test.describe("PublishingShell site-workspace stage / unstage", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "site workspace stages a page after confirm",
    { tag: ["@publishing", "@publishing-stage"] },
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
      await expect(page.locator('[data-testid="item-stage"]')).toBeVisible();
      await page.locator('[data-testid="item-stage-review"]').click();
      await expect(page.locator('[data-testid="item-stage-submit"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="item-stage-submit"]').click();
      await expect(page.locator('[data-testid="item-stage-success"]')).toBeVisible({
        timeout: 10_000,
      });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "site workspace removes an asset from staging after confirm",
    { tag: ["@publishing", "@publishing-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page);

      await page.goto(publishSitesUrl("9", "99"));
      await expect(page.locator('[data-testid="item-stage"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="item-stage-kind-resource"]').click();
      await page.locator('[data-testid="item-stage-action-unstage"]').click();
      await page.locator('[data-testid="item-stage-review"]').click();
      await expect(page.locator('[data-testid="item-stage-submit"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="item-stage-submit"]').click();
      await expect(page.locator('[data-testid="item-stage-success"]')).toBeVisible({
        timeout: 10_000,
      });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "HTTP 403 400 and 404 are not success",
    { tag: ["@publishing", "@publishing-stage"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        stageStatus: 403,
        stageBody: { message: "User demo is editing this page." },
      });

      await page.goto(publishSitesUrl("9", "42"));
      await expect(page.locator('[data-testid="item-stage"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="item-stage-review"]').click();
      await expect(page.locator('[data-testid="item-stage-submit"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="item-stage-submit"]').click();
      await expect(page.locator('[data-testid="item-stage-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-stage-error"]')).toContainText(
        /editing this page|Forbidden|Publish Forbidden/i,
      );
      await expect(page.locator('[data-testid="item-stage-success"]')).toHaveCount(0);

      // Re-stub for 400
      await page.unroute("**/sitemanage/publish/page/staging/**");
      await page.route("**/sitemanage/publish/page/staging/**", async (route) => {
        await route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ message: "Invalid item" }),
        });
      });
      await page.locator('[data-testid="item-stage-submit"]').click();
      await expect(page.locator('[data-testid="item-stage-error"]')).toContainText(
        /Invalid item|400|Bad/i,
      );
      await expect(page.locator('[data-testid="item-stage-success"]')).toHaveCount(0);

      // Re-stub for 404
      await page.unroute("**/sitemanage/publish/page/staging/**");
      await page.route("**/sitemanage/publish/page/staging/**", async (route) => {
        await route.fulfill({
          status: 404,
          contentType: "application/json",
          body: JSON.stringify({ message: "Unknown item" }),
        });
      });
      await page.locator('[data-testid="item-stage-submit"]').click();
      await expect(page.locator('[data-testid="item-stage-error"]')).toContainText(
        /Unknown item|not found|404/i,
      );
      await expect(page.locator('[data-testid="item-stage-success"]')).toHaveCount(0);

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
