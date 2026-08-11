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
 * Developer template detail — long binding expression show-more (UI-SRC-02 / #2901).
 *
 * Opens the first template detail row and, when a long binding expression is
 * present, asserts Show more / Show less expand chrome. When no long
 * expression exists in the CMS catalog, asserts the bindings section and that
 * short expressions do not show expand controls.
 *
 * Live run requires a CMS with at least one template. Prefer QA mode:
 *   perc-devctl qa-up → TEST_CMS_URL → surface-filtered Playwright.
 *
 * Entry: spa.jsp?entry=developer&section=templates
 * Refs #2901, #1690.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer template binding expression show-more (#2901 UI-SRC-02)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("template detail bindings section supports long-expression expand when present", async ({
    page,
  }) => {
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
      test.skip(true, "No templates in CMS — cannot open detail bindings");
      return;
    }

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
      page.locator('[data-testid="developer-tpl-bindings"]'),
    ).toBeVisible();

    // Expression editors use textareas (maxWidth clamp + optional expand).
    const expr0 = page.locator('[data-testid="developer-tpl-binding-expr-0"]');
    const hasBindingsTable = await page
      .locator('[data-testid="developer-tpl-bindings-table"]')
      .isVisible()
      .catch(() => false);

    if (!hasBindingsTable) {
      // Empty bindings is valid product state for some templates.
      await expect(
        page.locator('[data-testid="developer-tpl-bindings"]'),
      ).toContainText(/None/i);
      return;
    }

    await expect(expr0).toBeVisible({ timeout: 10_000 });
    // Clamp is applied as max-width on the textarea.
    await expect(expr0).toHaveCSS("max-width", "320px");

    const expandAny = page.locator(
      '[data-testid^="developer-tpl-binding-expr-expand-"]',
    );
    const expandCount = await expandAny.count();
    if (expandCount === 0) {
      // No long expressions in this catalog row — control correctly hidden.
      return;
    }

    const expandBtn = expandAny.first();
    await expect(expandBtn).toBeVisible();
    await expect(expandBtn).toHaveAttribute("aria-expanded", "false");
    await expandBtn.click();
    await expect(expandBtn).toHaveAttribute("aria-expanded", "true");
    await expect(expandBtn).toContainText(/Show less|show less/i);
    await expandBtn.click();
    await expect(expandBtn).toHaveAttribute("aria-expanded", "false");
  });
});
