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
 * Verified Email Lifecycle & Forgot Username QA spec (Milestone 2 / #49).
 *
 * @tag auth
 * @tag recovery
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Verified Email & Forgot Username @auth @recovery", () => {
  test("Login shows Forgot username link and Forgot Username returns generic acknowledgment", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/Rhythmyx/login`, {
      waitUntil: "domcontentloaded",
    });

    const forgotLink = page.getByTestId("perc-login-forgot-username");
    await expect(forgotLink).toBeVisible({ timeout: 20_000 });
    await forgotLink.click();

    await expect(
      page.getByTestId("perc-auth-forgot-username-page"),
    ).toBeVisible({ timeout: 20_000 });

    await page
      .getByTestId("perc-auth-forgot-username-email")
      .fill("nonexistent.qa@example.com");
    await page.getByTestId("perc-auth-forgot-username-submit").click();

    await expect(
      page.getByTestId("perc-auth-forgot-username-status"),
    ).toContainText(/verified account/i, { timeout: 15_000 });
  });

  test("Verify email GET does not auto-submit token; POST confirm shows result only on click", async ({
    page,
  }) => {
    let postCount = 0;
    page.on("request", (req) => {
      if (
        req.method() === "POST" &&
        req.url().includes("/auth/verify-email")
      ) {
        postCount += 1;
      }
    });

    await page.goto(
      `${BASE_URL}/Rhythmyx/auth/verify-email?token=qa-invalid-preview-token`,
      { waitUntil: "networkidle" },
    );

    const confirmBtn = page.getByTestId("perc-auth-verify-submit");
    await expect(confirmBtn).toBeVisible({ timeout: 20_000 });
    expect(postCount).toBe(0);

    await confirmBtn.click();
    await expect(page.getByTestId("perc-auth-verify-error")).toBeVisible({
      timeout: 15_000,
    });
    expect(postCount).toBe(1);
  });

  test("Authenticated user profile displays email verification status badge", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(page.getByTestId("perc-profile-account")).toBeVisible({
      timeout: 30_000,
    });
    await expect(
      page.getByTestId("perc-profile-email-verification"),
    ).toBeVisible({ timeout: 15_000 });
  });

  test("Authenticated Admin sees email edit and verification controls with zero uncaught page errors", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(err && err.message ? err.message : String(err));
    });

    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`,
      { waitUntil: "domcontentloaded" },
    );

    await expect(page.getByTestId("perc-profile-account")).toBeVisible({
      timeout: 30_000,
    });
    await expect(
      page.getByTestId("perc-profile-email-verification"),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page.getByTestId("perc-profile-account-email")).toBeVisible();
    await expect(page.getByTestId("perc-profile-account-save")).toBeVisible();
    expect(pageErrors).toEqual([]);
  });
});
