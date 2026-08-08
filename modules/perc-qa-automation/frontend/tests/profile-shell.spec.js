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
 * Profile hub shell smoke (#2393 / parent #2374 slice 1).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-shell.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function profileDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`;
}

function homeUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

test.describe("Profile shell @profile @smoke", () => {
  test("deep link opens profile shell with section landmarks", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-spa-app")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-profile-title")).toContainText(
      /my profile/i,
    );
    await expect(page.getByTestId("perc-profile-section-account")).toBeVisible();
    await expect(page.getByTestId("perc-profile-section-security")).toBeVisible();
    await expect(
      page.getByTestId("perc-profile-section-preferences"),
    ).toBeVisible();
    await expect(page.getByTestId("perc-profile-section-avatar")).toBeVisible();

    // Client path after entry handoff (or still query — accept either)
    await expect(page).toHaveURL(/profile/i, { timeout: 15_000 });
  });

  test("My profile menu entry navigates to profile shell", async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(homeUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-spa-user-menu")).toBeVisible({
      timeout: 30_000,
    });

    const menuLink = page.getByTestId("perc-spa-my-profile");
    await expect(menuLink).toBeVisible();
    await expect(menuLink).toBeEnabled();
    await menuLink.click();

    await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-profile-title")).toContainText(
      /my profile/i,
    );
    await expect(page).toHaveURL(/profile/i, { timeout: 15_000 });
  });
});
