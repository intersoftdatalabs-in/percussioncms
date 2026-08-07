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
 * Developer B2 — default ACL preferences / template surface (#2282 / #2274).
 *
 * Opens Preferences section and asserts Security default ACL template UI:
 * table (or load/error), add form, save/reset controls.
 *
 * Entry: spa.jsp?entry=developer&section=preferences
 * Refs #2282, #2274, #2262, #1690.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerPreferencesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "preferences",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer default ACL preferences (#2282)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Preferences security surface shows default ACL template editor", async ({
    page,
  }) => {
    await page.goto(developerPreferencesUrl(), {
      waitUntil: "networkidle",
    });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-preferences"]'),
    ).toBeVisible({ timeout: 15_000 });
    await expect(
      page.locator('[data-testid="tab-developer-preferences"]'),
    ).toHaveAttribute("aria-selected", "true");

    const panel = page.locator('[data-testid="developer-prefs-panel"]');
    await expect(panel).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-prefs-intro"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-prefs-security"]'),
    ).toBeVisible();

    const loading = page.locator('[data-testid="developer-prefs-acl-loading"]');
    const table = page.locator('[data-testid="developer-prefs-acl-table"]');
    const empty = page.locator('[data-testid="developer-prefs-acl-empty"]');
    const error = page.locator('[data-testid="developer-prefs-acl-error"]');

    // Wait until load settles to table, empty, or error (not perpetual loading).
    await expect(table.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });
    if (await loading.isVisible().catch(() => false)) {
      await expect(loading).toBeHidden({ timeout: 30_000 });
    }

    if (await error.isVisible()) {
      // Prefs API may be unavailable in some envs — surface still mounted.
      await expect(page.locator('[data-testid="developer-prefs-security"]')).toBeVisible();
      return;
    }

    await expect(page.locator('[data-testid="developer-prefs-acl-source"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-prefs-acl-add-form"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-prefs-acl-save"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-prefs-acl-reset"]')).toBeVisible();

    if (await table.isVisible()) {
      // System default rows (or saved preference with at least one principal input).
      const nameInputs = page.locator(
        '[data-testid^="developer-prefs-acl-name-"]',
      );
      await expect(nameInputs.first()).toBeVisible();
      const count = await nameInputs.count();
      expect(count).toBeGreaterThan(0);
    }
  });
});
