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
 * Smoke test: post-logout screen uses the modern UI chrome (same styling
 * contract as login) and offers a sign-in control back to the front door.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Logout screen", () => {
  test("shows modern signed-out chrome after /logout", async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);

    await page.goto(`${BASE_URL}/Rhythmyx/logout`, {
      waitUntil: "domcontentloaded",
    });

    // Modern host mounts React into #perc-logout-root
    await expect(page.getByTestId("perc-logout-root")).toBeAttached({
      timeout: 15_000,
    });
    await expect(page.getByTestId("perc-logout-page")).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.getByTestId("perc-logout-title")).toContainText(
      /signed out/i,
    );
    await expect(page.getByTestId("perc-logout-message")).toContainText(
      /logged out/i,
    );
    await expect(page.getByTestId("perc-brand-bar")).toBeVisible();
    await expect(page.getByTestId("perc-logout-logo")).toBeVisible();

    const signIn = page.getByTestId("perc-logout-sign-in");
    await expect(signIn).toBeVisible();
    await expect(signIn).toHaveAttribute("href", /login|rxlogin/i);

    // No legacy jQuery logout chrome
    await expect(page.locator("table.perc-form")).toHaveCount(0);
  });

  test("sign in again returns to the login front door", async ({ page }) => {
    test.setTimeout(45_000);
    await page.goto(`${BASE_URL}/Rhythmyx/logout`, {
      waitUntil: "domcontentloaded",
    });

    await expect(page.getByTestId("perc-logout-sign-in")).toBeVisible({
      timeout: 15_000,
    });
    await page.getByTestId("perc-logout-sign-in").click();

    await expect(page).toHaveURL(/login|rxlogin/i, { timeout: 15_000 });
    // Login modern root (or form) should appear
    const loginRoot = page.getByTestId("perc-login-root");
    const loginPage = page.getByTestId("perc-login-page");
    await expect(loginRoot.or(loginPage)).toBeVisible({ timeout: 15_000 });
  });
});
