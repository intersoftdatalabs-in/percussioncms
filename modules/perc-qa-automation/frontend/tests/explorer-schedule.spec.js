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
 * Explorer single-item Schedule publish dates (#5632 / parent #4530).
 *
 * <p>One selected page row + Schedule opens the dates dialog; saving persists
 * the start/end dates and refreshes the row. An invalid range (removal before
 * publish) stays on the dialog and posts nothing. Cancel changes nothing.
 * HTTP 400/403/404/409 names the item and leaves the prior dates intact.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-schedule.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const PRIOR_START = "09/18/2026 09:00 am";

const LISTING = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "43",
        name: "About",
        path: "/Sites/Demo/About",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 2,
    startIndex: 0,
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ homeStatus?: number }} [opts]
 */
async function installScheduleRoutes(page, opts = {}) {
  const homeStatus = opts.homeStatus ?? 200;
  /** @type {any[]} */
  const posted = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemDates: {
          itemId: "42",
          startDate: PRIOR_START,
          endDate: "",
          comments: "",
        },
      }),
    });
  });
  await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
    const body = route.request().postDataJSON();
    posted.push(body);
    if (homeStatus === 200) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ ItemDates: body?.ItemDates ?? {} }),
      });
      return;
    }
    await route.fulfill({
      status: homeStatus,
      contentType: "application/json",
      body: "denied",
    });
  });
  return posted;
}

async function openListing(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(home).toBeVisible({ timeout: 20_000 });
  await home.click();
  await expect(
    page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
  ).toBeVisible({ timeout: 10_000 });
}

async function openScheduleDialog(page) {
  const button = page.locator('[data-testid="action-toolbar-item-Schedule"]');
  await expect(button).toBeVisible({ timeout: 15_000 });
  await button.click();
  await expect(page.locator('[data-testid="explorer-schedule-dialog"]')).toBeVisible({
    timeout: 10_000,
  });
}

test.describe("Explorer single-item Schedule publish dates", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "one dialog saves the selected page schedule and refreshes the row",
    {
      tag: ["@explorer", "@explorer-schedule", "@explorer-single-schedule"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/Save schedule publish dates/i);
        void dialog.accept();
      });
      const posted = await installScheduleRoutes(page);
      await openListing(page);
      await openScheduleDialog(page);
      await expect(page.locator('[data-testid="explorer-schedule-start"]')).toHaveValue(
        "2026-09-18T09:00",
      );
      await page.locator('[data-testid="explorer-schedule-start"]').fill("2026-09-20T09:00");
      await page.locator('[data-testid="explorer-schedule-end"]').fill("2026-09-25T09:00");
      await page.locator('[data-testid="explorer-schedule-save"]').click();
      await expect.poll(() => posted.length).toBe(1);
      expect(posted[0]?.ItemDates?.itemId).toBe("42");
      expect(posted[0]?.ItemDates?.startDate).toBe("09/20/2026 09:00 am");
      expect(posted[0]?.ItemDates?.endDate).toBe("09/25/2026 09:00 am");
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "invalid range stays on the dialog and posts nothing",
    {
      tag: ["@explorer", "@explorer-schedule", "@explorer-single-schedule"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const posted = await installScheduleRoutes(page);
      await openListing(page);
      await openScheduleDialog(page);
      await page.locator('[data-testid="explorer-schedule-start"]').fill("2026-09-20T09:00");
      await page.locator('[data-testid="explorer-schedule-end"]').fill("2026-09-18T09:00");
      await page.locator('[data-testid="explorer-schedule-save"]').click();
      await expect(
        page.locator('[data-testid="explorer-schedule-dialog-error"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="explorer-schedule-dialog-error"]'),
      ).toContainText(/Removal must be after publish/i);
      await expect(page.locator('[data-testid="explorer-schedule-dialog"]')).toBeVisible();
      expect(posted).toEqual([]);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "cancel changes nothing",
    {
      tag: ["@explorer", "@explorer-schedule", "@explorer-single-schedule"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const posted = await installScheduleRoutes(page);
      await openListing(page);
      await openScheduleDialog(page);
      await page.locator('[data-testid="explorer-schedule-start"]').fill("2026-09-20T09:00");
      await page.locator('[data-testid="explorer-schedule-cancel"]').click();
      await expect(
        page.locator('[data-testid="explorer-schedule-dialog"]'),
      ).toHaveCount(0);
      expect(posted).toEqual([]);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 403 names the item and leaves the prior dates intact",
    {
      tag: ["@explorer", "@explorer-schedule", "@explorer-single-schedule"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const posted = await installScheduleRoutes(page, { homeStatus: 403 });
      await openListing(page);
      await openScheduleDialog(page);
      await page.locator('[data-testid="explorer-schedule-start"]').fill("2026-09-20T09:00");
      await page.locator('[data-testid="explorer-schedule-save"]').click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Home \(HTTP 403\)/i);
      await expect(error).toContainText(/Schedule failed/i);
      await expect.poll(() => posted.length).toBe(1);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="42"]'),
      ).toBeVisible({ timeout: 10_000 });
      await openScheduleDialog(page);
      await expect(page.locator('[data-testid="explorer-schedule-start"]')).toHaveValue(
        "2026-09-18T09:00",
      );
      await page.locator('[data-testid="explorer-schedule-cancel"]').click();
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
