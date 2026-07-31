/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Developer catalog smoke (post-P0 / #1690).
 *
 * Loads each primary Developer SPA section against a live CMS and asserts the
 * catalog reaches a non-error state (panel, empty, or loading resolved to data).
 * Content-types also asserts table rows carry real labels/names (not only "—"
 * placeholders from empty DTOs).
 *
 * Entry: spa.jsp?entry=developer&section=<slug>
 * Refs #1690 (design-WS retargets #1700–#1704).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

/**
 * @type {{
 *   section: string,
 *   successTestIds: string[],
 *   errorTestId: string,
 * }[]}
 */
const CATALOGS = [
  {
    section: "content-types",
    successTestIds: ["developer-ct-panel", "developer-ct-empty"],
    errorTestId: "developer-ct-error",
  },
  {
    section: "keywords",
    successTestIds: ["developer-kw-panel", "developer-kw-empty"],
    errorTestId: "developer-kw-error",
  },
  {
    section: "locales",
    successTestIds: ["developer-loc-panel", "developer-loc-empty"],
    errorTestId: "developer-loc-error",
  },
  {
    section: "slots",
    successTestIds: ["developer-slot-panel", "developer-slot-empty"],
    errorTestId: "developer-slot-error",
  },
  {
    section: "shared-fields",
    successTestIds: ["developer-sf-panel", "developer-sf-empty"],
    errorTestId: "developer-sf-error",
  },
  {
    section: "system-def",
    successTestIds: ["developer-sys-panel", "developer-sys-empty"],
    errorTestId: "developer-sys-error",
  },
];

function developerUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer catalog smoke (#1690)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  for (const cat of CATALOGS) {
    test(`${cat.section}: catalog loads without API error`, async ({ page }) => {
      await page.goto(developerUrl(cat.section), { waitUntil: "networkidle" });

      await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator(`[data-testid="tab-developer-${cat.section}"]`),
      ).toBeVisible({ timeout: 15_000 });

      const error = page.locator(`[data-testid="${cat.errorTestId}"]`);
      const success = page.locator(
        cat.successTestIds.map((id) => `[data-testid="${id}"]`).join(", "),
      );

      // Wait until loading finishes: either success surface or error alert
      await expect(success.or(error).first()).toBeVisible({ timeout: 30_000 });

      if (await error.isVisible()) {
        const msg = (await error.innerText()).trim();
        throw new Error(
          `Developer section "${cat.section}" showed catalog error: ${msg}`,
        );
      }

      await expect(success.first()).toBeVisible();

      // Content-types: panel with rows must expose real labels/names, not only
      // "—" placeholders (empty DTOs when list JSON only carries hideFromMenu).
      // Empty catalog (developer-ct-empty) is a valid success surface.
      if (cat.section === "content-types") {
        await assertContentTypesRowsUsable(page);
      }
    });
  }
});

/**
 * When the content-types panel is shown (not the empty state), require at least
 * one data row with alphabetic label/name content.
 *
 * @param {import('@playwright/test').Page} page
 */
async function assertContentTypesRowsUsable(page) {
  const panel = page.locator('[data-testid="developer-ct-panel"]');
  if (!(await panel.isVisible())) {
    return;
  }

  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 10_000 });

  const rowCount = await table.locator('[data-testid="developer-ct-row"]').count();
  expect(
    rowCount,
    "content type table should have at least one row when panel is shown",
  ).toBeGreaterThan(0);

  const bodyText = await table.innerText();
  // Strip column headers so we only judge cell content.
  const onlyPlaceholders = !/[A-Za-z]{2,}/.test(
    bodyText.replace(/Label|Name|Id|Description|Select/gi, ""),
  );
  expect(
    onlyPlaceholders,
    "content type rows look empty (labels/names missing from API/DTO) — redeploy rest/WebUI or fix ContentType list mapping",
  ).toBe(false);
}
