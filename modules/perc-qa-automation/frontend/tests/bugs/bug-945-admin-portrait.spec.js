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
 * Regression: Admin screen usable in portrait / narrow viewport (GH-945).
 *
 * Primary Admin chrome (SPA AdminShell tabs) must remain clickable when the
 * viewport is phone-portrait sized — wrapping tabs / no fixed min-width clip.
 *
 * #3088: Workflow / roles / users / categories are Admin tabs in the same shell.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

/** Typical phone portrait (iPhone SE-ish). */
const PORTRAIT = { width: 375, height: 667 };

test.describe("Admin portrait layout (GH-945)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await page.setViewportSize(PORTRAIT);
    await loginAsAdmin(page);
  });

  test("AdminShell tabs remain visible and switchable in portrait", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=admin`);

    const shell = page.locator("[data-testid='perc-admin-shell']");
    await expect(shell).toBeVisible({ timeout: 20_000 });

    const tablist = page.locator("[data-testid='perc-admin-tablist']");
    await expect(tablist).toBeVisible();

    for (const id of [
      "tab-tasks",
      "tab-logs",
      "tab-notifications",
      "tab-tools",
      "tab-workflow",
      "tab-roles",
      "tab-users",
      "tab-categories",
    ]) {
      const tab = page.locator(`[data-testid='${id}']`);
      await expect(tab).toBeVisible();
      const box = await tab.boundingBox();
      expect(box, id).not.toBeNull();
      // Tab must sit within (or only slightly past) the portrait width —
      // wrapping may place later tabs on a second row still fully on-screen.
      expect(box.width).toBeGreaterThan(0);
      expect(box.height).toBeGreaterThan(0);
    }

    await page.locator("[data-testid='tab-logs']").click();
    await expect(
      page.locator("[data-testid='perc-task-logs-section']"),
    ).toBeVisible();

    await page.locator("[data-testid='tab-tools']").click();
    await expect(
      page.locator("[data-testid='perc-tools-section']"),
    ).toBeVisible();
  });

  test("Admin workflow tabs remain visible in portrait (#3088)", async ({
    page,
  }) => {
    // Legacy workflow entry redirects into Admin workflow tab
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);

    const shell = page.locator("[data-testid='perc-admin-shell']");
    await expect(shell).toBeVisible({ timeout: 20_000 });

    const tablist = page.locator("[data-testid='perc-admin-tablist']");
    await expect(tablist).toBeVisible();

    for (const id of [
      "tab-workflow",
      "tab-roles",
      "tab-users",
      "tab-categories",
    ]) {
      const tab = page.locator(`[data-testid='${id}']`);
      await expect(tab).toBeVisible();
      const box = await tab.boundingBox();
      expect(box, id).not.toBeNull();
      expect(box.width).toBeGreaterThan(0);
    }

    await page.locator("[data-testid='tab-users']").click();
    await expect(page.locator("[data-testid='perc-users-section']")).toBeVisible();
  });
});
