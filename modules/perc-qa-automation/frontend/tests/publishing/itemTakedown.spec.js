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
 * PublishingShell site-workspace takedown (#4538).
 *
 * <p>Tags: {@code @publishing} {@code @publishing-takedown}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing/itemTakedown.spec.js}
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
  const linkedBody = opts.linkedBody ?? { ArrayList: [] };
  const takedownStatus = opts.takedownStatus ?? 200;
  const takedownBody = opts.takedownBody ?? { status: "Success" };

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
      body:
        typeof linkedBody === "string"
          ? linkedBody
          : JSON.stringify(linkedBody),
    });
  });
  await page.route("**/sitemanage/publish/takedown/**", async (route) => {
    await route.fulfill({
      status: takedownStatus,
      contentType: "application/json",
      body:
        typeof takedownBody === "string"
          ? takedownBody
          : JSON.stringify(takedownBody),
    });
  });
}

test.describe("PublishingShell site-workspace takedown", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "site workspace takes down a page after linked-item review",
    { tag: ["@publishing", "@publishing-takedown"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        linkedBody: {
          ArrayList: [{ pagePath: "/Sites/Demo/Home", id: "7" }],
        },
      });

      await page.goto(publishSitesUrl("9", "42"));
      await expect(page.locator('[data-testid="publishing-shell"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="publish-site-workspace"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="item-takedown"]')).toBeVisible();
      await expect(page.locator('[data-testid="item-takedown-linked"]')).toContainText(
        "/Sites/Demo/Home",
        { timeout: 10_000 },
      );

      await page.locator('[data-testid="item-takedown-submit"]').click();
      await expect(page.locator('[data-testid="item-takedown-success"]')).toBeVisible({
        timeout: 10_000,
      });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "FORBIDDEN BADCONFIG NOSTAGING_SERVERS and INVALID are not success",
    { tag: ["@publishing", "@publishing-takedown"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        takedownBody: {
          status: "FORBIDDEN",
          warningMessage: "Publication stopped because of licensing issues",
        },
      });

      await page.goto(publishSitesUrl("9", "42"));
      await expect(page.locator('[data-testid="item-takedown"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="item-takedown-submit"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="item-takedown-submit"]').click();
      await expect(page.locator('[data-testid="item-takedown-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-takedown-error"]')).toContainText(
        /FORBIDDEN|licensing|Publish Forbidden/i,
      );
      await expect(page.locator('[data-testid="item-takedown-success"]')).toHaveCount(0);

      await page.unroute("**/sitemanage/publish/takedown/**");
      await page.route("**/sitemanage/publish/takedown/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SitePublishResponse: {
              status: "BADCONFIG",
              warningMessage:
                "Could not connect to publishing server, please check publishing server configuration.",
            },
          }),
        });
      });
      await page.locator('[data-testid="item-takedown-submit"]').click();
      await expect(page.locator('[data-testid="item-takedown-error"]')).toContainText(
        /BADCONFIG|Could not connect to publishing server|Bad Server Configuration/i,
      );
      await expect(page.locator('[data-testid="item-takedown-success"]')).toHaveCount(0);

      await page.unroute("**/sitemanage/publish/takedown/**");
      await page.route("**/sitemanage/publish/takedown/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ status: "NOSTAGING_SERVERS" }),
        });
      });
      await page.locator('[data-testid="item-takedown-submit"]').click();
      await expect(page.locator('[data-testid="item-takedown-error"]')).toContainText(
        /NOSTAGING_SERVERS/i,
      );
      await expect(page.locator('[data-testid="item-takedown-success"]')).toHaveCount(0);

      await page.unroute("**/sitemanage/publish/takedown/**");
      await page.route("**/sitemanage/publish/takedown/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ status: "INVALID" }),
        });
      });
      await page.locator('[data-testid="item-takedown-submit"]').click();
      await expect(page.locator('[data-testid="item-takedown-error"]')).toContainText(
        /INVALID/i,
      );
      await expect(page.locator('[data-testid="item-takedown-success"]')).toHaveCount(0);

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
