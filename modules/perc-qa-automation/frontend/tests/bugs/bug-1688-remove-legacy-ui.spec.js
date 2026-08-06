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
 * Regression: legacy Rhythmyx UI removal (GH-1688).
 *
 * <p><b>GH-1688</b> — The product UI has been refactored to React, so the
 * "Use legacy UI" checkbox on the login form and the "Rhythmyx UI" link in
 * the top header are removed. This spec asserts they are gone.</p>
 *
 * <p>Runs against the React login front door ({@code /Rhythmyx/login})
 * and a post-login page that includes the header JSP.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("Legacy UI removal (GH-1688)", () => {
  test("login form has no legacy UI checkbox or j_selectUI field", async ({
    page,
  }) => {
    test.setTimeout(30_000);
    await page.goto(`${BASE_URL}/Rhythmyx/login`);
    await expect(page.getByTestId("perc-login-page")).toBeVisible({
      timeout: 15_000,
    });

    await expect(page.locator("#perc-login-select-ui")).toHaveCount(0);
    await expect(page.locator('input[name="j_selectUI"]')).toHaveCount(0);
  });

  test("header has no Rhythmyx UI link after login", async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);

    // Legacy dashboard still includes header.jsp (React SPA shell / index.jsp does not).
    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`);
    await expect(page.locator("#perc-rhythmyx-ui")).toHaveCount(0);
    await expect(
      page.locator('a[href="/Rhythmyx/sys_cx/mainpage.html"]'),
    ).toHaveCount(0);
  });
});
