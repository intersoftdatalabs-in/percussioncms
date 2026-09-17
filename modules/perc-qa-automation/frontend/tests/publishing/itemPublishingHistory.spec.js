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
 * PublishingShell item publishing history (#4536).
 *
 * <p>Tags: {@code @publishing} {@code @publishing-history}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing/itemPublishingHistory.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function publishSpaUrl(section, itemId) {
  const root = String(BASE_URL || "").replace(/\/$/, "");
  const params = new URLSearchParams({
    entry: "publish",
    section,
    _: String(Date.now()),
  });
  if (itemId) {
    params.set("itemId", itemId);
  }
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

async function stubPublishApis(page, historyBody, historyStatus = 200) {
  await page.route("**/sitemanage/pubstatus/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/sitemanage/site**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/itemmanagement/item/pubhistory/**", async (route) => {
    const body =
      typeof historyBody === "function" ? historyBody(route) : historyBody;
    await route.fulfill({
      status: historyStatus,
      contentType: "application/json",
      body: typeof body === "string" ? body : JSON.stringify(body),
    });
  });
}

test.describe("PublishingShell item publishing history", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "Status and Logs show the item history panel; empty lookup is explicit",
    { tag: ["@publishing", "@publishing-history"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishApis(page, { ItemPublishingHistory: [] });

      await page.goto(publishSpaUrl("status"));
      await expect(page.locator('[data-testid="publishing-shell"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="item-publishing-history"]'),
      ).toBeVisible();
      await expect(page.locator('[data-testid="item-history-idle"]')).toBeVisible();

      await page.locator('[data-testid="item-history-id"]').fill("42");
      await page.locator('[data-testid="item-history-lookup"]').click();
      await expect(page.locator('[data-testid="item-history-empty"]')).toBeVisible({
        timeout: 10_000,
      });

      await page.locator('[data-testid="item-history-open-logs"]').click();
      await expect(
        page.locator('[data-testid="publish-section-logs"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="item-publishing-history"]'),
      ).toBeVisible();

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "deep-link itemId loads rows; error state is not success",
    { tag: ["@publishing", "@publishing-history"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishApis(page, {
        ItemPublishingHistory: [
          {
            server: "prod",
            location: "/index.html",
            revisionId: 3,
            publishedDate: Date.now(),
            operation: "publish",
            status: "SUCCESS",
            contentId: 42,
          },
        ],
      });

      await page.goto(publishSpaUrl("logs", "42"));
      await expect(page.locator('[data-testid="publishing-shell"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="item-history-table"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(page.locator('[data-testid="item-history-row"]')).toContainText(
        "prod",
      );

      await page.unroute("**/itemmanagement/item/pubhistory/**");
      await page.route("**/itemmanagement/item/pubhistory/**", async (route) => {
        await route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({ message: "history lookup failed" }),
        });
      });
      await page.locator('[data-testid="item-history-id"]').fill("99");
      await page.locator('[data-testid="item-history-lookup"]').click();
      await expect(page.locator('[data-testid="item-history-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-history-error"]')).toContainText(
        /history lookup failed|Error|HTTP 500/i,
      );

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
