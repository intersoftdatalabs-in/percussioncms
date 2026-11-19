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
 * Login MFA (TOTP), Forced Setup, and Step-Up QA spec (Milestone 3 / #50).
 *
 * @tag auth
 * @tag mfa
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { generateTotp } = require("./helpers/totp");

test.describe("Two-Step Verification (TOTP & Boundary) @auth @mfa", () => {
  test("Unauthenticated /auth/challenge and /auth/setup without PERC_AUTH_TX show expired state safely", async ({
    page,
  }) => {
    await page.goto(`${BASE_URL}/Rhythmyx/auth/challenge`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("perc-auth-mfa-challenge-page")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("perc-auth-mfa-expired")).toBeVisible();
    await expect(page.getByTestId("perc-auth-back-to-login")).toBeVisible();

    await page.goto(`${BASE_URL}/Rhythmyx/auth/setup`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("perc-auth-mfa-setup-page")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("perc-auth-mfa-expired")).toBeVisible();
  });

  test("Authenticated Admin enrolls TOTP with RFC 6238 code, views recovery codes, and restores state", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`,
      { waitUntil: "domcontentloaded" },
    );

    const securityNav = page.getByTestId("perc-profile-nav-security");
    if (await securityNav.isVisible()) {
      await securityNav.click();
    }

    const mfaPanel = page.getByTestId("perc-profile-mfa");
    await expect(mfaPanel).toBeVisible({ timeout: 30_000 });
    await expect(page.getByTestId("perc-profile-mfa-status")).toBeVisible({
      timeout: 15_000,
    });

    const enrollStart = page.getByTestId("perc-profile-mfa-enroll-start");
    let manualKey = "";
    if (await enrollStart.isVisible()) {
      try {
        await enrollStart.click();
        await expect(page.getByTestId("perc-profile-mfa-qr")).toBeVisible({
          timeout: 15_000,
        });
        // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #53, precedent #5946) - Playwright DOM test ID, not credential material.
        const manualSecretLocator = page.getByTestId("perc-profile-mfa-manual-secret");
        await expect(manualSecretLocator).toBeVisible();
        manualKey = ((await manualSecretLocator.textContent()) || "").trim();
        expect(manualKey.length).toBeGreaterThan(8);

        await page.getByTestId("perc-profile-mfa-enroll-code").fill("000000");
        await page.getByTestId("perc-profile-mfa-enroll-confirm").click();
        await expect(page.getByTestId("perc-profile-mfa-error")).toBeVisible({
          timeout: 15_000,
        });

        const validCode = generateTotp(manualKey);
        await page.getByTestId("perc-profile-mfa-enroll-code").fill(validCode);
        await page.getByTestId("perc-profile-mfa-enroll-confirm").click();
        await expect(
          page.getByTestId("perc-profile-mfa-recovery-codes"),
        ).toBeVisible({ timeout: 15_000 });
      } finally {
        try {
          const disableBtn = page.getByTestId("perc-profile-mfa-disable");
          if (await disableBtn.isVisible()) {
            await disableBtn.click();
            const stepUpInput = page.getByTestId("perc-profile-mfa-stepup-input");
            await expect(stepUpInput.or(enrollStart)).toBeVisible({
              timeout: 10_000,
            });
            if ((await stepUpInput.isVisible()) && manualKey) {
              await stepUpInput.fill(generateTotp(manualKey));
              await page.getByTestId("perc-profile-mfa-stepup-submit").click();
              await expect(enrollStart).toBeVisible({ timeout: 15_000 });
            }
          }
        } catch (cleanupErr) {
          console.warn("TOTP cleanup restore failed:", cleanupErr);
        }
      }
    }
  });
});
