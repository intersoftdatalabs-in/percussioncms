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
 * Developer SE-05 community visibility navigator (#2273 / #2262 / #1690).
 *
 * Opens the dedicated Community Visibility section and asserts navigator IA:
 * communities as expandable groups, type groups under a community, object rows.
 *
 * Entry: spa.jsp?entry=developer&section=community-visibility
 * Refs #2273, #2262, #1690.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerCommunityVisibilityUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "community-visibility",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer SE-05 community visibility navigator (#2273)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("navigator groups design objects by community visibility", async ({ page }) => {
    await page.goto(developerCommunityVisibilityUrl(), {
      waitUntil: "networkidle",
    });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-community-visibility"]'),
    ).toBeVisible({ timeout: 15_000 });

    const error = page.locator('[data-testid="developer-cvn-error"]');
    const panel = page.locator('[data-testid="developer-cvn-panel"]');
    const empty = page.locator('[data-testid="developer-cvn-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Community visibility navigator error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No communities in CMS — cannot exercise SE-05 navigator");
      return;
    }

    await expect(page.locator('[data-testid="developer-cvn-tree"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-cvn-intro"]')).toBeVisible();

    // First community toggle in the tree (keys are guid/name/id-based).
    const firstToggle = page
      .locator('[data-testid^="developer-cvn-community-toggle-"]')
      .first();
    await expect(firstToggle).toBeVisible({ timeout: 15_000 });
    await firstToggle.click();

    const firstCommunityItem = page
      .locator('[data-testid^="developer-cvn-community-"]')
      .filter({ has: firstToggle })
      .first();
    // Resolve key from toggle test id suffix.
    const toggleTestId = await firstToggle.getAttribute("data-testid");
    const key = (toggleTestId || "").replace("developer-cvn-community-toggle-", "");
    expect(key.length).toBeGreaterThan(0);

    const loading = page.locator(
      `[data-testid="developer-cvn-community-loading-${key}"]`,
    );
    const typeGroups = page.locator(
      `[data-testid="developer-cvn-type-groups-${key}"]`,
    );
    const commEmpty = page.locator(
      `[data-testid="developer-cvn-community-empty-${key}"]`,
    );
    const commError = page.locator(
      `[data-testid="developer-cvn-community-error-${key}"]`,
    );

    await expect(loading).toBeHidden({ timeout: 30_000 }).catch(() => {
      // loading may never appear if response was instant
    });
    await expect(typeGroups.or(commEmpty).or(commError).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await commError.isVisible()) {
      const msg = (await commError.innerText()).trim();
      throw new Error(`Community visibility load error: ${msg}`);
    }

    if (await commEmpty.isVisible()) {
      // Empty community is a valid navigator state on sparse H2 fixtures.
      await expect(commEmpty).toBeVisible();
      return;
    }

    await expect(typeGroups).toBeVisible();
    // At least one type group under the community.
    const typeToggle = page
      .locator(`[data-testid^="developer-cvn-type-toggle-${key}-"]`)
      .first();
    await expect(typeToggle).toBeVisible({ timeout: 10_000 });
    await typeToggle.click();

    const typeTestId = await typeToggle.getAttribute("data-testid");
    const typeKey = (typeTestId || "").replace(
      `developer-cvn-type-toggle-${key}-`,
      "",
    );
    expect(typeKey.length).toBeGreaterThan(0);

    const objects = page.locator(
      `[data-testid="developer-cvn-objects-${key}-${typeKey}"]`,
    );
    await expect(objects).toBeVisible({ timeout: 10_000 });
    // Object rows use developer-cvn-object-* test ids.
    await expect(
      objects.locator('[data-testid^="developer-cvn-object-"]').first(),
    ).toBeVisible({ timeout: 5_000 });

    // Collapse community again — navigator chrome remains.
    await firstToggle.click();
    await expect(typeGroups).toBeHidden({ timeout: 5_000 });
    await expect(page.locator('[data-testid="developer-cvn-tree"]')).toBeVisible();

    // Silence unused variable when filter path unused.
    void firstCommunityItem;
  });
});
