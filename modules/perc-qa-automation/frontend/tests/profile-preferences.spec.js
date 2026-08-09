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
 * Profile Preferences section (#2396 / parent #2374 slice 4).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-preferences.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Covers: Preferences form mounts; change default landing; reload persists.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function profileDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function expectPreferencesMounted(page) {
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
    timeout: 30_000,
  });
  await expect(
    page.getByTestId("perc-profile-section-preferences"),
  ).toBeVisible();
  await expect(page.getByTestId("perc-profile-preferences")).toBeVisible({
    timeout: 30_000,
  });
  await expect(
    page.getByTestId("perc-profile-preferences-landing"),
  ).toBeVisible({ timeout: 30_000 });
}

test.describe("Profile preferences @profile @preferences @smoke", () => {
  test("deep link shows preferences landing control for Admin", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectPreferencesMounted(page);

    const landing = page.getByTestId("perc-profile-preferences-landing");
    await expect(landing).toBeEnabled();
    await expect(
      page.getByTestId("perc-profile-preferences-save"),
    ).toBeVisible();
    await expect(
      page.getByTestId("perc-profile-preferences-count"),
    ).toBeVisible();
  });

  test("change landing preference, reload, value persists", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectPreferencesMounted(page);

    const landing = page.getByTestId("perc-profile-preferences-landing");
    await expect(landing).toBeVisible();

    const original = await landing.inputValue();
    // Prefer Home ↔ Editor so both are allowed for Admin and product-backed.
    const next = original === "Editor" ? "Home" : "Editor";

    await landing.selectOption(next);
    await page.getByTestId("perc-profile-preferences-save").click();
    await expect(
      page.getByTestId("perc-profile-preferences-success"),
    ).toBeVisible({ timeout: 30_000 });
    await expect(landing).toHaveValue(next);

    // Reload profile hub — value must come back from server
    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectPreferencesMounted(page);
    await expect(
      page.getByTestId("perc-profile-preferences-landing"),
    ).toHaveValue(next, { timeout: 30_000 });

    // Best-effort restore original for stable re-runs
    if (original !== next) {
      const landing2 = page.getByTestId("perc-profile-preferences-landing");
      await landing2.selectOption(original);
      await page.getByTestId("perc-profile-preferences-save").click();
      await expect(
        page.getByTestId("perc-profile-preferences-success"),
      ).toBeVisible({ timeout: 30_000 });
    }
  });
});
