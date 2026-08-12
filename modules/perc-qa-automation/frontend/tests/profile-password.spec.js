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
 * Profile Security password section smoke (#2394 / parent #2374 slice 3).
 * Also covers residual #2647 axe form gate once password form is on the page.
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-password.spec.js
 *
 * Axe form root (#2647):
 *   npm run test:surface -- --path tests/profile-password.spec.js --grep "axe-core"
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Covers: INTERNAL Admin change-password form mount, client validation
 * (agent-safe), optional success+restore when ADMIN_PASSWORD is available,
 * and axe serious/critical zero on [data-testid="perc-profile-security"].
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

/** Form root for password / security section (not the whole profile shell). */
const SECURITY_FORM_SCOPE = '[data-testid="perc-profile-security"]';

function profileDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function expectSecurityMounted(page) {
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-section-security")).toBeVisible();
  await expect(page.getByTestId("perc-profile-security")).toBeVisible({
    timeout: 30_000,
  });
  // Loading finishes → form (Admin is INTERNAL) or external box
  await expect(
    page
      .getByTestId("perc-profile-security-form")
      .or(page.getByTestId("perc-profile-security-external")),
  ).toBeVisible({ timeout: 30_000 });
}

test.describe("Profile security password @profile @password @security @smoke", () => {
  test("deep link shows change-password form for Admin (INTERNAL)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectSecurityMounted(page);

    await expect(page.getByTestId("perc-profile-security-form")).toBeVisible();
    await expect(
      page.getByTestId("perc-profile-security-new-password"),
    ).toHaveAttribute("type", "password");
    await expect(
      page.getByTestId("perc-profile-security-confirm-password"),
    ).toHaveAttribute("type", "password");
    await expect(
      page.getByTestId("perc-profile-security-submit"),
    ).toBeVisible();
    await expect(
      page.getByTestId("perc-profile-security-external"),
    ).toHaveCount(0);
  });

  /**
   * #2647 residual of #2503 — axe serious/critical zero on security form root
   * after mount (labels, controls, live region wiring).
   */
  test("axe-core a11y gate — security form root (#2647)", async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectSecurityMounted(page);
    await expect(page.getByTestId("perc-profile-security-form")).toBeVisible();

    await expectNoSeriousA11yViolations(page, {
      scope: SECURITY_FORM_SCOPE,
    });
  });

  /**
   * #2647 — axe after client validation error so aria-invalid / error live
   * region on password fields stay free of serious/critical issues.
   */
  test("axe-core a11y gate — security form with validation error (#2647)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectSecurityMounted(page);

    await page.getByTestId("perc-profile-security-new-password").fill("ab");
    await page
      .getByTestId("perc-profile-security-confirm-password")
      .fill("ab");
    await page.getByTestId("perc-profile-security-submit").click();
    await expect(
      page.getByTestId("perc-profile-security-new-error"),
    ).toBeVisible();

    await expectNoSeriousA11yViolations(page, {
      scope: SECURITY_FORM_SCOPE,
    });
  });

  test("client validation: too short and mismatch (agent-safe)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectSecurityMounted(page);

    const newPw = page.getByTestId("perc-profile-security-new-password");
    const confirm = page.getByTestId("perc-profile-security-confirm-password");
    const submit = page.getByTestId("perc-profile-security-submit");

    // Too short
    await newPw.fill("ab");
    await confirm.fill("ab");
    await submit.click();
    await expect(
      page.getByTestId("perc-profile-security-new-error"),
    ).toBeVisible();
    await expect(newPw).toHaveAttribute("aria-invalid", "true");
    await expect(page.getByTestId("perc-profile-security-success")).toHaveCount(
      0,
    );

    // Mismatch
    await newPw.fill("abcdef");
    await confirm.fill("abcdeg");
    await submit.click();
    await expect(
      page.getByTestId("perc-profile-security-confirm-error"),
    ).toBeVisible();
    await expect(confirm).toHaveAttribute("aria-invalid", "true");
    await expect(page.getByTestId("perc-profile-security-success")).toHaveCount(
      0,
    );
  });

  /**
   * Success path: change password then restore original ADMIN_PASSWORD so the
   * suite remains stable. Skipped when ADMIN_PASSWORD is unset (agent-safe
   * validation tests above still run).
   */
  test("password change success and restore when ADMIN_PASSWORD set", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const original = process.env.ADMIN_PASSWORD;
    test.skip(
      !original,
      "ADMIN_PASSWORD required for success+restore path",
    );

    await loginAsAdmin(page);
    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectSecurityMounted(page);

    const tempPassword = `TmpPw${Date.now()}a`;
    const newPw = page.getByTestId("perc-profile-security-new-password");
    const confirm = page.getByTestId("perc-profile-security-confirm-password");
    const submit = page.getByTestId("perc-profile-security-submit");

    await newPw.fill(tempPassword);
    await confirm.fill(tempPassword);
    await submit.click();
    await expect(
      page.getByTestId("perc-profile-security-success"),
    ).toBeVisible({ timeout: 30_000 });

    // Restore original so subsequent tests / operators keep working
    await newPw.fill(original);
    await confirm.fill(original);
    await submit.click();
    await expect(
      page.getByTestId("perc-profile-security-success"),
    ).toBeVisible({ timeout: 30_000 });
  });
});
