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
 * Password Reset, Scanner-Safe Recovery & Admin Break-Glass Recovery QA spec (Milestone 5 / #52).
 *
 * @tag auth
 * @tag recovery
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

// mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #5946) - Playwright test suite title / test ID, not credential material.
test.describe("Password Recovery & Admin Break-Glass MFA Recovery @auth @recovery", () => {
  test("Login shows Forgot links and Forgot Password returns generic acknowledgment", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/Rhythmyx/login`, {
      waitUntil: "domcontentloaded",
    });

    await expect(page.getByTestId("perc-login-forgot-username")).toBeVisible({
      timeout: 20_000,
    });
    // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #5946) - Playwright test suite title / test ID, not credential material.
    const forgotPasswordLink = page.getByTestId("perc-login-forgot-password");
    await expect(forgotPasswordLink).toBeVisible({ timeout: 20_000 });
    await forgotPasswordLink.click();

    await expect(
      page.getByTestId("perc-auth-forgot-password-page"),
    ).toBeVisible({ timeout: 20_000 });

    await page
      .getByTestId("perc-auth-forgot-password-identifier")
      .fill("nonexistent.qa@example.com");
    await page.getByTestId("perc-auth-forgot-password-submit").click();

    await expect(
      page.getByTestId("perc-auth-forgot-password-status"),
    ).toBeVisible({ timeout: 15_000 });
  });

  test("Reset Password and Admin Recovery validate missing tokens and stay scanner-safe on GET", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/Rhythmyx/auth/reset-password`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByTestId("perc-auth-reset-password-missing-token"),
    ).toBeVisible({ timeout: 20_000 });

    await page.goto(`${BASE_URL}/Rhythmyx/auth/admin-recovery`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByTestId("perc-auth-admin-recovery-missing-token"),
    ).toBeVisible({ timeout: 20_000 });

    let postCount = 0;
    page.on("request", (req) => {
      if (
        req.method() === "POST" &&
        req.url().includes("/auth/admin-recovery")
      ) {
        postCount += 1;
      }
    });

    await page.goto(
      `${BASE_URL}/Rhythmyx/auth/admin-recovery?token=qa-invalid-preview-token`,
      { waitUntil: "networkidle" },
    );

    const confirmBtn = page.getByTestId("perc-auth-admin-recovery-confirm");
    await expect(confirmBtn).toBeVisible({ timeout: 20_000 });
    expect(postCount).toBe(0);

    await confirmBtn.click();
    await expect(
      page.getByTestId("perc-auth-admin-recovery-error"),
    ).toBeVisible({ timeout: 15_000 });
    expect(postCount).toBe(1);
  });

  test("Admin Auth Readiness panel exposes Admin Break-Glass MFA Recovery controls", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=admin&tab=tools&_=${Date.now()}`,
      { waitUntil: "domcontentloaded" },
    );

    await page.getByTestId("tool-tab-auth-readiness").click();
    await expect(page.getByTestId("perc-auth-readiness")).toBeVisible({
      timeout: 30_000,
    });
    await expect(
      page.getByTestId("auth-admin-recovery-section"),
    ).toBeVisible({ timeout: 20_000 });
    await expect(page.getByTestId("auth-admin-recovery-user")).toBeVisible();
    await expect(page.getByTestId("auth-admin-recovery-note")).toBeVisible();
    await expect(page.getByTestId("auth-admin-recovery-submit")).toBeVisible();
  });
});
