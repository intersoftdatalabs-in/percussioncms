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
 * Login MFA Passkeys (WebAuthn / CDP Virtual Authenticator) QA spec (Milestone 4 / #51).
 *
 * @tag auth
 * @tag mfa
 * @tag passkey
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("WebAuthn Passkeys Management @auth @mfa @passkey", () => {
  test("Authenticated Admin registers, renames, and deletes a passkey with virtual authenticator", async ({
    page,
    browserName,
  }) => {
    test.skip(
      browserName !== "chromium",
      "WebAuthn CDP virtual authenticator requires Chromium",
    );

    const cdp = await page.context().newCDPSession(page);
    await cdp.send("WebAuthn.enable");
    const { authenticatorId } = await cdp.send(
      "WebAuthn.addVirtualAuthenticator",
      {
        options: {
          protocol: "ctap2",
          transport: "internal",
          hasResidentKey: true,
          hasUserVerification: true,
          isUserVerified: true,
        },
      },
    );

    try {
      await loginAsAdmin(page);
      await page.goto(
        `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`,
        { waitUntil: "domcontentloaded" },
      );

      const securityNav = page.getByTestId("perc-profile-nav-security");
      if (await securityNav.isVisible()) {
        await securityNav.click();
      }

      const passkeysSection = page.getByTestId("perc-profile-passkeys");
      await expect(passkeysSection).toBeVisible({ timeout: 30_000 });

      const uniqueLabel = `Playwright Key ${Date.now()}`;
      const renamedLabel = `Renamed Key ${Date.now()}`;

      await page
        .getByTestId("perc-profile-passkey-label-input")
        .fill(uniqueLabel);
      await page.getByTestId("perc-profile-passkey-add").click();

      const stepUpBtn = page.getByTestId("perc-profile-passkeys-stepup-passkey");
      const addedRow = page
        .locator('[data-testid^="perc-profile-passkey-item-"]')
        .filter({ hasText: uniqueLabel });

      await expect(addedRow.or(stepUpBtn)).toBeVisible({ timeout: 15_000 });
      if (await stepUpBtn.isVisible()) {
        await stepUpBtn.click();
      }
      await expect(addedRow).toBeVisible({ timeout: 20_000 });

      await addedRow
        .locator('[data-testid^="perc-profile-passkey-rename-"]')
        .click();
      await page
        .getByTestId("perc-profile-passkey-rename-input")
        .fill(renamedLabel);
      await page.getByTestId("perc-profile-passkey-rename-save").click();

      const renamedRow = page
        .locator('[data-testid^="perc-profile-passkey-item-"]')
        .filter({ hasText: renamedLabel });
      await expect(renamedRow).toBeVisible({ timeout: 20_000 });

      await renamedRow
        .locator('[data-testid^="perc-profile-passkey-delete-"]')
        .click();

      try {
        await expect(renamedRow).toHaveCount(0, { timeout: 3_000 });
      } catch (deleteErr) {
        if (await stepUpBtn.isVisible()) {
          await stepUpBtn.click();
        } else {
          throw deleteErr;
        }
      }

      await expect(renamedRow).toHaveCount(0, { timeout: 20_000 });
      await expect(page.getByTestId("perc-profile-passkeys-success")).toBeVisible();
    } finally {
      try {
        if (authenticatorId) {
          await cdp.send("WebAuthn.removeVirtualAuthenticator", {
            authenticatorId,
          });
        }
        await cdp.send("WebAuthn.disable");
      } catch (cleanupErr) {
        console.warn(
          "WebAuthn virtual authenticator cleanup failed:",
          cleanupErr,
        );
      }
    }
  });
});
