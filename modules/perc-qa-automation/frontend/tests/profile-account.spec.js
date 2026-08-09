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
 * Profile Account section view/edit smoke (#2395 / parent #2374 slice 2).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-account.spec.js
 *
 * Axe form root (#2503 / residual #2427):
 *   npm run test:surface -- --path tests/profile-account.spec.js --grep "axe-core"
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Covers: Account identity fields for Admin (internal), email edit happy path
 * + client validation, and self-only REST (no target user on path).
 * Axe: zero serious/critical on [data-testid="perc-profile-account"].
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

/** Form root for account edit (not the whole profile shell). */
const ACCOUNT_FORM_SCOPE = '[data-testid="perc-profile-account"]';

function profileDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function expectAccountMounted(page) {
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-section-account")).toBeVisible();
  await expect(page.getByTestId("perc-profile-account")).toBeVisible({
    timeout: 30_000,
  });
  // Loading finishes → login id shown
  await expect(page.getByTestId("perc-profile-account-login")).toBeVisible({
    timeout: 30_000,
  });
}

test.describe("Profile account @profile @account @smoke", () => {
  test("deep link shows account identity fields for Admin", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectAccountMounted(page);

    const login = page.getByTestId("perc-profile-account-login");
    await expect(login).not.toHaveText(/^\s*$/);
    await expect(login).not.toHaveText(/not set/i);

    await expect(page.getByTestId("perc-profile-account-provider")).toBeVisible();
    await expect(page.getByTestId("perc-profile-account-roles")).toBeVisible();
    await expect(
      page.getByTestId("perc-profile-account-communities"),
    ).toBeVisible();

    // Admin is an INTERNAL user → email input + save control present
    const email = page.getByTestId("perc-profile-account-email");
    await expect(email).toBeVisible();
    await expect(email).toHaveAttribute("type", "email");
    await expect(page.getByTestId("perc-profile-account-save")).toBeVisible();
  });

  /**
   * #2503 residual of #2427 — axe serious/critical zero on account form root
   * after mount (labels, controls, live region wiring). Scope is the form
   * section, not perc-profile-shell (shell covered by profile-shell.spec.js).
   */
  test("axe-core a11y gate — account form root (#2503)", async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectAccountMounted(page);

    await expectNoSeriousA11yViolations(page, {
      scope: ACCOUNT_FORM_SCOPE,
    });
  });

  /**
   * #2503 — axe after client validation error so aria-invalid / error live
   * region on the email field stay free of serious/critical issues.
   */
  test("axe-core a11y gate — account form with email error (#2503)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectAccountMounted(page);

    const email = page.getByTestId("perc-profile-account-email");
    await email.fill("not-a-valid-email");
    await page.getByTestId("perc-profile-account-save").click();
    await expect(
      page.getByTestId("perc-profile-account-email-error"),
    ).toBeVisible();

    await expectNoSeriousA11yViolations(page, {
      scope: ACCOUNT_FORM_SCOPE,
    });
  });

  test("email validation and save happy path for Admin", async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectAccountMounted(page);

    const email = page.getByTestId("perc-profile-account-email");
    await expect(email).toBeVisible();

    const original = (await email.inputValue()) || "";
    const unique = `profile-account-${Date.now()}@example.com`;

    // Invalid shape → client error, no success toast
    await email.fill("not-a-valid-email");
    await page.getByTestId("perc-profile-account-save").click();
    await expect(
      page.getByTestId("perc-profile-account-email-error"),
    ).toBeVisible();
    await expect(
      page.getByTestId("perc-profile-account-success"),
    ).toHaveCount(0);

    // Valid save
    await email.fill(unique);
    await page.getByTestId("perc-profile-account-save").click();
    await expect(
      page.getByTestId("perc-profile-account-success"),
    ).toBeVisible({ timeout: 30_000 });
    await expect(email).toHaveValue(unique);

    // Best-effort restore original so re-runs stay stable
    if (original && original !== unique) {
      await email.fill(original);
      await page.getByTestId("perc-profile-account-save").click();
      await expect(
        page.getByTestId("perc-profile-account-success"),
      ).toBeVisible({ timeout: 30_000 });
    }
  });
});
