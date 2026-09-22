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
 * PublishingShell site-workspace schedule dates (#4537).
 *
 * <p>Tags: {@code @publishing} {@code @publishing-schedule}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing/itemScheduleDates.spec.js}
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
  const datesBody = opts.datesBody ?? {
    ItemDates: {
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "",
    },
  };
  const datesStatus = opts.datesStatus ?? 200;
  const setStatus = opts.setStatus ?? 200;
  const setBody = opts.setBody ?? { operation: "Success" };

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
      status: datesStatus,
      contentType: "application/json",
      body:
        typeof datesBody === "string" ? datesBody : JSON.stringify(datesBody),
    });
  });
  await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
    await route.fulfill({
      status: setStatus,
      contentType: "application/json",
      body: typeof setBody === "string" ? setBody : JSON.stringify(setBody),
    });
  });
}

test.describe("PublishingShell schedule publish dates", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "site workspace loads and saves schedule dates",
    { tag: ["@publishing", "@publishing-schedule"] },
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
      await expect(page.locator('[data-testid="item-schedule-dates"]')).toBeVisible();
      await expect(page.locator('[data-testid="item-schedule-start"]')).toHaveValue(
        "2026-09-18T09:00",
        { timeout: 10_000 },
      );

      await page.locator('[data-testid="item-schedule-save"]').click();
      await expect(page.locator('[data-testid="item-schedule-success"]')).toBeVisible({
        timeout: 10_000,
      });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "invalid dates 400 and forbidden 403 are not success",
    { tag: ["@publishing", "@publishing-schedule"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        setStatus: 400,
        setBody: { message: "Invalid date range" },
      });

      await page.goto(publishSitesUrl("9", "42"));
      await expect(page.locator('[data-testid="item-schedule-dates"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="item-schedule-save"]').click();
      await expect(page.locator('[data-testid="item-schedule-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-schedule-error"]')).toContainText(
        /Invalid date range|Invalid schedule dates|HTTP 400/i,
      );
      await expect(page.locator('[data-testid="item-schedule-success"]')).toHaveCount(0);

      await page.unroute("**/itemmanagement/item/setitemdates**");
      await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "FORBIDDEN" }),
        });
      });
      await page.locator('[data-testid="item-schedule-save"]').click();
      await expect(page.locator('[data-testid="item-schedule-error"]')).toContainText(
        /FORBIDDEN|Publish Forbidden|HTTP 403/i,
      );
      await expect(page.locator('[data-testid="item-schedule-success"]')).toHaveCount(0);

      await page.unroute("**/itemmanagement/item/setitemdates**");
      await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({
            message: "User other is editing this page. You cannot modify this item.",
          }),
        });
      });
      await page.locator('[data-testid="item-schedule-save"]').click();
      await expect(page.locator('[data-testid="item-schedule-error"]')).toContainText(
        /editing this page|HTTP 409/i,
      );
      await expect(page.locator('[data-testid="item-schedule-success"]')).toHaveCount(0);

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
