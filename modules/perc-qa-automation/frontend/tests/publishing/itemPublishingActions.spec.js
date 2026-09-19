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
 * PublishingShell site-workspace item publishing actions menu (#4581).
 *
 * <p>Tags: {@code @publishing} {@code @publishing-actions}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing/itemPublishingActions.spec.js}
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

const DEFAULT_ACTIONS = [
  { name: "Publish", enabled: true },
  { name: "Schedule...", enabled: false },
  { name: "Remove from Site", enabled: true },
  { name: "Stage", enabled: false },
  { name: "Remove from Staging", enabled: true },
];

async function stubPublishSiteApis(page, opts = {}) {
  const actionsStatus = opts.actionsStatus ?? 200;
  const actionsBody = opts.actionsBody ?? DEFAULT_ACTIONS;

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
  // Existing actions endpoint: …/sitemanage/publish/publishingActions/{id}
  await page.route("**/sitemanage/publish/publishingActions/**", async (route) => {
    await route.fulfill({
      status: actionsStatus,
      contentType: "application/json",
      body:
        typeof actionsBody === "string"
          ? actionsBody
          : JSON.stringify(actionsBody),
    });
  });
}

test.describe("PublishingShell site-workspace publishing actions menu", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "menu shows server rows with unavailable actions disabled",
    { tag: ["@publishing", "@publishing-actions"] },
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
      await expect(page.locator('[data-testid="item-publishing-actions"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="item-publishing-actions-menu"]'),
      ).toBeVisible({ timeout: 10_000,
      });

      await expect(page.locator('[data-testid="item-action-publish-now"]')).toBeEnabled();
      await expect(page.locator('[data-testid="item-action-schedule"]')).toBeDisabled();
      await expect(page.locator('[data-testid="item-action-takedown"]')).toBeEnabled();
      await expect(page.locator('[data-testid="item-action-stage"]')).toBeDisabled();
      await expect(page.locator('[data-testid="item-action-unstage"]')).toBeEnabled();

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "enabled action navigates to the shipped panel",
    { tag: ["@publishing", "@publishing-actions"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page);

      await page.goto(publishSitesUrl("9", "42"));
      await expect(
        page.locator('[data-testid="item-publishing-actions-menu"]'),
      ).toBeVisible({ timeout: 20_000 });

      await page.locator('[data-testid="item-action-publish-now"]').click();
      await expect(page.locator('[data-testid="item-publish-now"]')).toBeInViewport({
        timeout: 10_000,
      });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "HTTP 403 and 404 are errors, not empty success",
    { tag: ["@publishing", "@publishing-actions"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        actionsStatus: 403,
        actionsBody: { message: "User demo cannot publish this item." },
      });

      await page.goto(publishSitesUrl("9", "42"));
      await expect(
        page.locator('[data-testid="item-publishing-actions-error"]'),
      ).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator('[data-testid="item-publishing-actions-error"]'),
      ).toContainText(/Forbidden|cannot publish/i);
      await expect(
        page.locator('[data-testid="item-publishing-actions-menu"]'),
      ).toHaveCount(0);

      // Re-stub for 404
      await page.unroute("**/sitemanage/publish/publishingActions/**");
      await page.route(
        "**/sitemanage/publish/publishingActions/**",
        async (route) => {
          await route.fulfill({
            status: 404,
            contentType: "application/json",
            body: JSON.stringify({ message: "Unknown item" }),
          });
        },
      );
      await page.locator('[data-testid="item-publishing-actions-load"]').click();
      await expect(
        page.locator('[data-testid="item-publishing-actions-error"]'),
      ).toContainText(/Unknown item|not found|404/i);
      await expect(
        page.locator('[data-testid="item-publishing-actions-menu"]'),
      ).toHaveCount(0);

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
