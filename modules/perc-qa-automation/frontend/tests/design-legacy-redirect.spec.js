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
 * Design template-list legacy entry retirement (#3306 / parent #2631).
 *
 * Surface-filtered:
 *   npm run test:surface -- --path tests/design-legacy-redirect.spec.js
 *
 * Proves admin.jsp and ?view=design land on SPA Design template library
 * (not the retired CM1 Design list). Broader Design SPA Playwright is #3307.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Design legacy list entry retirement (#3306)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  function attachPageErrorGate(page) {
    const pageErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });
    return pageErrors;
  }

  test("admin.jsp hard-redirects to SPA Design @smoke @ui", async ({
    page,
  }) => {
    const pageErrors = attachPageErrorGate(page);
    const url = `${BASE_URL}/Rhythmyx/cm/app/admin.jsp?_=${Date.now()}`;
    await page.goto(url, { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-design-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator("#perc-assigned-templates")).toHaveCount(0);
    const finalUrl = page.url();
    expect(finalUrl).toMatch(/design|entry=design|view=design/i);
    expect(pageErrors, "uncaught pageerror on Design redirect").toEqual([]);
  });

  test("?view=design deep link lands on SPA Design @smoke @ui", async ({
    page,
  }) => {
    const pageErrors = attachPageErrorGate(page);
    const url = `${BASE_URL}/Rhythmyx/cm/app/?view=design&_=${Date.now()}`;
    await page.goto(url, { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-design-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("panel-design-templates")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator("#perc-assigned-templates")).toHaveCount(0);
    expect(pageErrors, "uncaught pageerror on view=design").toEqual([]);
  });
});
