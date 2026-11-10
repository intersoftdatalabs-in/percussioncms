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
 * PublishingShell schedule publish dates on Status/Logs (#5988).
 *
 * <p>Tags: {@code @publishing} {@code @publishing-schedule}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing/itemPublishingScheduleDates.spec.js}
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

function storedDates() {
  return {
    ItemDates: {
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "",
    },
  };
}

async function stubShellScheduleApis(page, opts = {}) {
  let cleared = false;
  const setPosts = [];
  let setStatus = opts.setStatus ?? 200;
  let setBody = opts.setBody ?? { operation: "Success" };

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
  await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
    const body = cleared
      ? { ItemDates: { itemId: "42", startDate: "", endDate: "", comments: "" } }
      : storedDates();
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
  await page.route("**/itemmanagement/item/setitemdates**", async (route) => {
    try {
      setPosts.push(route.request().postDataJSON());
    } catch {
      setPosts.push(route.request().postData());
    }
    const posted = setPosts[setPosts.length - 1] || {};
    const empty =
      (posted.startDate ?? posted?.ItemDates?.startDate ?? "") === "" &&
      (posted.endDate ?? posted?.ItemDates?.endDate ?? "") === "";
    if (empty && setStatus === 200) {
      cleared = true;
    }
    await route.fulfill({
      status: setStatus,
      contentType: "application/json",
      body: typeof setBody === "string" ? setBody : JSON.stringify(setBody),
    });
  });
  return {
    setPosts,
    setState: (next) => {
      setStatus = next.setStatus ?? 200;
      setBody = next.setBody ?? { operation: "Success" };
    },
  };
}

test.describe("PublishingShell schedule publish dates", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "Status and Logs show the schedule panel; deep-link loads dates",
    { tag: ["@publishing", "@publishing-schedule"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubShellScheduleApis(page);

      await page.goto(publishSpaUrl("status", "42"));
      await expect(page.locator('[data-testid="publishing-shell"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="item-schedule-dates"]')).toBeVisible();
      await expect(page.locator('[data-testid="item-schedule-start"]')).toHaveValue(
        "2026-09-18T09:00",
        { timeout: 10_000 },
      );

      await page.goto(publishSpaUrl("logs", "42"));
      await expect(page.locator('[data-testid="publish-section-logs"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="item-schedule-dates"]')).toBeVisible();
      await expect(page.locator('[data-testid="item-schedule-end"]')).toHaveValue(
        "2026-09-19T10:00",
        { timeout: 10_000 },
      );

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "save success; HTTP 400, 403, and 409 stay errors",
    { tag: ["@publishing", "@publishing-schedule"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      const state = await stubShellScheduleApis(page);

      await page.goto(publishSpaUrl("status", "42"));
      await expect(page.locator('[data-testid="item-schedule-dates"]')).toBeVisible({
        timeout: 20_000,
      });

      await page.locator('[data-testid="item-schedule-save"]').click();
      await expect(page.locator('[data-testid="item-schedule-success"]')).toBeVisible({
        timeout: 10_000,
      });

      state.setState({ setStatus: 400, setBody: { message: "Invalid date range" } });
      await page.locator('[data-testid="item-schedule-save"]').click();
      await expect(page.locator('[data-testid="item-schedule-error"]')).toContainText(
        /Invalid date range|Invalid schedule dates|HTTP 400/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="item-schedule-success"]')).toHaveCount(0);

      state.setState({ setStatus: 403, setBody: { message: "FORBIDDEN" } });
      await page.locator('[data-testid="item-schedule-save"]').click();
      await expect(page.locator('[data-testid="item-schedule-error"]')).toContainText(
        /FORBIDDEN|Publish Forbidden|HTTP 403/i,
      );
      await expect(page.locator('[data-testid="item-schedule-success"]')).toHaveCount(0);

      state.setState({
        setStatus: 409,
        setBody: { message: "User other is editing this page." },
      });
      await page.locator('[data-testid="item-schedule-save"]').click();
      await expect(page.locator('[data-testid="item-schedule-error"]')).toContainText(
        /editing this page|HTTP 409/i,
      );
      await expect(page.locator('[data-testid="item-schedule-success"]')).toHaveCount(0);

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "clear confirm removes dates; cancel writes nothing",
    { tag: ["@publishing", "@publishing-schedule"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      const { setPosts } = await stubShellScheduleApis(page);

      await page.goto(publishSpaUrl("logs", "42"));
      await expect(page.locator('[data-testid="item-schedule-dates"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="item-schedule-start"]')).toHaveValue(
        "2026-09-18T09:00",
        { timeout: 10_000 },
      );

      await page.locator('[data-testid="item-schedule-clear-submit"]').click();
      await expect(
        page.locator('[data-testid="item-schedule-clear-confirm"]'),
      ).toBeVisible();
      await page.locator('[data-testid="item-schedule-clear-cancel"]').click();
      await expect(
        page.locator('[data-testid="item-schedule-clear-confirm"]'),
      ).toHaveCount(0);
      expect(setPosts).toEqual([]);
      await expect(page.locator('[data-testid="item-schedule-start"]')).toHaveValue(
        "2026-09-18T09:00",
      );

      await page.locator('[data-testid="item-schedule-clear-submit"]').click();
      await page.locator('[data-testid="item-schedule-clear-yes"]').click();
      await expect(page.locator('[data-testid="item-schedule-success"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-schedule-start"]')).toHaveValue("");

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
