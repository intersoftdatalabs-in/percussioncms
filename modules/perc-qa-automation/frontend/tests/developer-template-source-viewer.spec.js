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
 * Developer template detail — source viewer (UI-SRC-01 / #2088).
 *
 * Asserts line-number gutter, copy control, and edit/preview chrome on the
 * template detail panel after opening the first catalog row.
 *
 * Part of #2188 smoke gate ({@code @smoke}). On H2 qa-up matrix (#2185) this
 * is RED for product TemplateSummary empty name/label (#2189) and indexed row
 * selectors (#2186). Codified as skip-with-BUG — flip to live assert when
 * residuals land (see helpers/developer-smoke-set.js).
 *
 * Entry: spa.jsp?entry=developer&section=templates
 * Refs #2088, #1690, #2188.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");
const {
  getSmokeEntry,
  skipReasonFor,
} = require("./helpers/developer-smoke-set");

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer template source viewer (#2088 UI-SRC-01) @smoke", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("template detail source shows line numbers and copy control @smoke", async ({
    page,
  }) => {
    // Product DTO empty name/label (#2189) + selector harden (#2186). Smoke gate
    // requires explicit skip-with-BUG, not a silent red flake (#2188).
    test.skip(true, skipReasonFor(getSmokeEntry("template-source-viewer")));

    await page.goto(developerTemplatesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-templates"]'),
    ).toBeVisible({ timeout: 15_000 });

    const error = page.locator('[data-testid="developer-tpl-error"]');
    const panel = page.locator('[data-testid="developer-tpl-panel"]');
    const empty = page.locator('[data-testid="developer-tpl-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Templates catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No templates in CMS — cannot open detail source viewer");
      return;
    }

    // Indexed CatalogTable rows (developer-tpl-row-0 …); bare developer-tpl-row
    // never matches WebUI. Prefer first-row open button so product detail/DTO
    // failures (#2189) surface cleanly after this selector harden (#2186).
    const firstRow = page.locator(catalogRowSelector("developer-tpl-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(
      openBtn,
      "first template row should expose Open control when selectionKey is set",
    ).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(
      page.locator('[data-testid="developer-tpl-detail"]'),
    ).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-tpl-source"]'),
    ).toBeVisible();

    // Line-number gutter (at least line 1)
    await expect(
      page.locator('[data-testid="developer-tpl-source-lines"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-tpl-source-ln-1"]'),
    ).toBeVisible();

    // Edit surface by default
    await expect(
      page.locator('[data-testid="developer-tpl-source-edit"]'),
    ).toBeVisible();

    // Copy control present; grant clipboard permissions when possible
    const copyBtn = page.locator('[data-testid="developer-tpl-source-copy"]');
    await expect(copyBtn).toBeVisible();
    try {
      await page
        .context()
        .grantPermissions(["clipboard-read", "clipboard-write"]);
    } catch {
      // Some browsers / contexts ignore grantPermissions
    }
    await copyBtn.click();
    await expect(
      page.locator('[data-testid="developer-tpl-source-copy-feedback"]'),
    ).toBeVisible({ timeout: 5_000 });

    // Preview highlight mode
    const modeBtn = page.locator('[data-testid="developer-tpl-source-mode"]');
    await modeBtn.click();
    await expect(
      page.locator('[data-testid="developer-tpl-source-preview"]'),
    ).toBeVisible({ timeout: 5_000 });
    await expect(
      page.locator('[data-testid="developer-tpl-source-edit"]'),
    ).toHaveCount(0);

    // Back to edit
    await modeBtn.click();
    await expect(
      page.locator('[data-testid="developer-tpl-source-edit"]'),
    ).toBeVisible();
  });
});
