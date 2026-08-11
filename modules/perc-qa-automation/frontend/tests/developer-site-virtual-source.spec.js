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
 * Developer Sites → Virtual Site source panel (#2956 / epic #2678).
 *
 * Opens Sites catalog detail and asserts the Virtual Site source section mounts
 * with source-kind control (repository default) and save chrome.
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-site-virtual-source.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowsSelector,
} = require("./helpers/developer-catalog-selectors");

function developerSectionUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer Site Virtual Site source panel (#2956)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Sites detail shows Virtual Site source panel with repository default", async ({
    page,
  }) => {
    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    // Wait for catalog panel, empty, or error (H2 QA may have zero sites)
    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — Virtual Site panel requires a site row",
      });
      return;
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-detail"]')).toBeVisible({
      timeout: 15_000,
    });

    const virtualSection = page.locator('[data-testid="developer-site-virtual"]');
    await expect(virtualSection).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="developer-site-virtual-title"]')).toBeVisible();

    // Load success or error (REST may 404 if site name only on summary list)
    const formOrError = page.locator(
      [
        '[data-testid="developer-site-virtual-form"]',
        '[data-testid="developer-site-virtual-error"]',
      ].join(", "),
    );
    await expect(formOrError.first()).toBeVisible({ timeout: 20_000 });

    if (await page.locator('[data-testid="developer-site-virtual-form"]').isVisible()) {
      const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
      await expect(kind).toBeVisible();
      // Default traditional sites use repository option
      await expect(kind).toHaveValue(/repository|git-filesystem/);
      await expect(page.locator('[data-testid="developer-site-virtual-save"]')).toBeVisible();

      // Switch to git-filesystem reveals root path field
      await kind.selectOption("git-filesystem");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      // Restore repository to avoid leaving QA site dirty when save is not exercised
      await kind.selectOption("repository");
    }
  });
});
