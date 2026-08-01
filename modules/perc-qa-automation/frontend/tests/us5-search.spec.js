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
 * Playwright spec: US5 P-Search \u2014 server-backed search panel (FR-017, FR-018; SC-005 \u2014 partial).
 *
 * <p>Asserts the modern React {@code SearchPanel} mounts on
 * <code>searchModern.jsp</code> and the form submit / loading / empty /
 * error wiring is live against the docker dev CMS at
 * <code>http://localhost:9992</code>.</p>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>The dev CMS's search REST endpoint
 *       (<code>/Rhythmyx/services/searchmanagement/search/get/extendedresults</code>)
 *       returns HTTP 500 against the minimal Derby image (no search
 *       index installed). The Playwright tests use the live endpoint
 *       so they exercise the error / loading wiring that real users
 *       see on a system with no populated search index. SC-005
 *       (\u226510 s on a \u2265500-child folder search) and the per-search
 *       acceptance criteria are gated on a system-installed CMS.</li>
 *   <li>The Vitest mapper tests
 *       (<code>WebUI/src/test/ts/contentExplorer/searchApi.test.ts</code>,
 *       <code>SearchPanel.test.tsx</code>) cover the structural surface
 *       with the REST mocked, so this spec is the wiring smoke test.</li>
 * </ul>
 *
 * <p>Run from <code>modules/perc-qa-automation/frontend</code>:</p>
 * <pre>
 *   npm test -- tests/us5-search.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const SEARCH_URL = `${BASE_URL}/Rhythmyx/cm/app/searchModern.jsp?_=${Date.now()}`;

test.describe("US5 P-Search \u2014 search panel (FR-017)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("SearchPanel pilot page mounts with input + submit", async ({
    page,
  }) => {
    await page.goto(SEARCH_URL, { waitUntil: "networkidle" });
    const panel = page.locator('[data-testid="search-panel"]');
    await expect(panel).toBeVisible({ timeout: 15_000 });
    await expect(
      page.locator('[data-testid="search-panel-input"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="search-panel-submit"]'),
    ).toBeVisible();
  });

  test("legacy miller-column Finder chrome is NOT loaded on the search host", async ({
    page,
  }) => {
    await page.goto(SEARCH_URL, { waitUntil: "networkidle" });
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("submitting a query transitions the panel out of idle (loading or error)", async ({
    page,
  }) => {
    await page.goto(SEARCH_URL, { waitUntil: "networkidle" });
    const input = page.locator('[data-testid="search-panel-input"]');
    await input.fill("test query");
    await page.locator('[data-testid="search-panel-submit"]').click();
    // Either the loading indicator appears (mock-wired) or the error
    // panel appears (the real dev-CMS endpoint returns 500). Both are
    // valid post-submit states; we assert that the panel is no longer
    // in idle (i.e. some non-input UI is rendered).
    await expect(
      page
        .locator(
          '[data-testid="search-panel-loading"], [data-testid="search-panel-error"], [data-testid="search-panel-empty"], [data-testid="search-panel-results"]',
        )
        .first(),
    ).toBeVisible({ timeout: 15_000 });
  });

  test("axe-core a11y gate — SearchPanel mounted with no Finder chrome (T082b)", async ({
    page,
  }) => {
    await page.goto(SEARCH_URL, { waitUntil: "networkidle" });
    await expect(
      page.locator('[data-testid="perc-search-root"]').first(),
    ).toBeVisible({ timeout: 15_000 });
    // Confirm legacy Finder chrome is absent (echo of US6 expectation).
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="perc-search-root"], [data-testid="search-panel"]',
    });
  });
});
