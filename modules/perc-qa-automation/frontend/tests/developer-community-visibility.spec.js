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
 * Developer communities — object visibility polish (#2250 / #1690).
 *
 * Opens community detail and asserts the visibility lens filters, summary,
 * and empty/table states. Read-only lens (no assign/unassign from this panel).
 *
 * Entry: spa.jsp?entry=developer&section=communities
 * Refs #2250, #1690.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerCommunitiesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "communities",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer community visibility polish (#2250)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("community detail shows visibility filters and empty-or-table state", async ({
    page,
  }) => {
    await page.goto(developerCommunitiesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-communities"]'),
    ).toBeVisible({ timeout: 15_000 });

    const error = page.locator('[data-testid="developer-comm-error"]');
    const panel = page.locator('[data-testid="developer-comm-panel"]');
    const empty = page.locator('[data-testid="developer-comm-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Communities catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No communities in CMS — cannot open visibility lens");
      return;
    }

    const firstRow = page.locator(catalogRowSelector("developer-comm-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(
      openBtn,
      "first community row should expose Open control when selectionKey is set",
    ).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(
      page.locator('[data-testid="developer-comm-detail"]'),
    ).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-comm-visibility"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-comm-visibility-filters"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-comm-visibility-type-filter"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-comm-visibility-name-filter"]'),
    ).toBeVisible();

    // Wait for loading to settle: table, unfiltered empty, or error.
    const visTable = page.locator(
      '[data-testid="developer-comm-visibility-table"]',
    );
    const visEmpty = page.locator(
      '[data-testid="developer-comm-visibility-empty"]',
    );
    const visErr = page.locator(
      '[data-testid="developer-comm-visibility-error"]',
    );
    const visLoading = page.locator(
      '[data-testid="developer-comm-visibility-loading"]',
    );

    await expect(visLoading)
      .toBeHidden({ timeout: 30_000 })
      .catch(() => {
        // loading may never appear if response was instant
      });
    await expect(visTable.or(visEmpty).or(visErr).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await visErr.isVisible()) {
      const msg = (await visErr.innerText()).trim();
      throw new Error(`Community visibility error: ${msg}`);
    }

    // Type filter: pick WORKFLOW (often empty on H2) and assert empty-type or table.
    const typeSelect = page.locator(
      '[data-testid="developer-comm-visibility-type-filter"]',
    );
    await typeSelect.selectOption("WORKFLOW");

    const emptyType = page.locator(
      '[data-testid="developer-comm-visibility-empty-type"]',
    );
    await expect(visLoading)
      .toBeHidden({ timeout: 30_000 })
      .catch(() => {});
    await expect(
      visTable.or(emptyType).or(visEmpty).or(visErr).first(),
    ).toBeVisible({
      timeout: 30_000,
    });

    if (await visErr.isVisible()) {
      const msg = (await visErr.innerText()).trim();
      throw new Error(`Community visibility error after type filter: ${msg}`);
    }

    // Name filter control remains interactive
    const nameInput = page.locator(
      '[data-testid="developer-comm-visibility-name-filter"]',
    );
    await nameInput.fill("unlikely-object-name-zzz");
    if (await visTable.isVisible()) {
      // If rows remained after type filter, name filter should empty them client-side
      await expect(
        page.locator('[data-testid="developer-comm-visibility-empty-name"]'),
      ).toBeVisible({ timeout: 5_000 });
    }

    // Roles section still present for assignment feedback surface
    await expect(
      page.locator('[data-testid="developer-comm-roles"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-comm-roles-save"]'),
    ).toBeVisible();
  });
});
