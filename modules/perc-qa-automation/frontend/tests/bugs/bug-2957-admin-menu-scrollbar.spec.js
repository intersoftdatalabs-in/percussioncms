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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression: Admin menu links bar must not show a vertical scrollbar (GH-2957).
 *
 * When four primary Admin tabs fit the tablist, overflow-y must be hidden so
 * browsers do not paint a spurious vertical scrollbar from overflow-x: auto.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

/** Desktop-ish width where four Admin tabs fit without wrapping. */
const DESKTOP = { width: 1280, height: 800 };

/**
 * @param {import("@playwright/test").Locator} tablist
 */
async function assertNoVerticalTablistScrollbar(tablist) {
  await expect(tablist).toBeVisible();

  const metrics = await tablist.evaluate((el) => {
    const style = getComputedStyle(el);
    return {
      overflowX: style.overflowX,
      overflowY: style.overflowY,
      scrollHeight: el.scrollHeight,
      clientHeight: el.clientHeight,
      scrollWidth: el.scrollWidth,
      clientWidth: el.clientWidth,
    };
  });

  // CSS contract: pair overflow-x auto with overflow-y hidden (GH-2957).
  expect(
    metrics.overflowY,
    "tablist overflow-y must be hidden to avoid vertical scrollbar",
  ).toBe("hidden");

  // When content fits, no vertical overflow (allow 1px subpixel).
  expect(
    metrics.scrollHeight,
    "tablist should not vertically overflow when tabs fit",
  ).toBeLessThanOrEqual(metrics.clientHeight + 1);
}

test.describe("Admin menu tablist scrollbar (GH-2957)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await page.setViewportSize(DESKTOP);
    await loginAsAdmin(page);
  });

  test("Admin tablist has no vertical scrollbar when four tabs visible", async ({
    page,
  }) => {
    // Issue #2957 surface: Admin tools (Scheduled Tasks / Logs / Notifications / Tools)
    await page.goto(`${BASE_URL}/cm/app/admin`);

    const shell = page.locator("[data-testid='perc-admin-shell']");
    await expect(shell).toBeVisible({ timeout: 20_000 });

    const tablist = page.locator("[data-testid='perc-admin-tablist']");
    for (const id of [
      "tab-tasks",
      "tab-logs",
      "tab-notifications",
      "tab-tools",
    ]) {
      await expect(page.locator(`[data-testid='${id}']`)).toBeVisible();
    }

    await assertNoVerticalTablistScrollbar(tablist);
  });
});
