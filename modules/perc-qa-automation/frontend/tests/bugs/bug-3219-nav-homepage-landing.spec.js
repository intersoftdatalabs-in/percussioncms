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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Issue #3219 / parent #3197 slice 3 — homepage Navigation landing.
 *
 * Profile preference Architecture (product: Navigation) must open the
 * Navigation SPA after a fresh login, not Home.
 *
 * Surface-filtered:
 *   npm run test:surface -- --path tests/bugs/bug-3219-nav-homepage-landing.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

async function openPreferences(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`,
    { waitUntil: "domcontentloaded" },
  );
  await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
    timeout: 30_000,
  });
  const prefsNav = page.getByTestId("perc-profile-nav-preferences");
  await expect(prefsNav).toBeVisible({ timeout: 20_000 });
  await expect(prefsNav).toBeEnabled();
  await prefsNav.click();
  const landing = page.getByTestId("perc-profile-preferences-landing");
  await expect(landing).toBeVisible({ timeout: 20_000 });
  return landing;
}

async function saveLanding(page, value) {
  const landing = await openPreferences(page);
  const optionValues = await landing.locator("option").evaluateAll((els) =>
    els.map((o) => o.value),
  );
  const target = optionValues.includes(value)
    ? value
    : optionValues.includes("Home")
      ? "Home"
      : optionValues[0];
  if (target == null) {
    throw new Error("Landing select has no options to restore");
  }
  await landing.selectOption(target);
  const save = page.getByTestId("perc-profile-preferences-save");
  if (await save.isEnabled()) {
    await save.click();
    await expect(
      page.getByTestId("perc-profile-preferences-success"),
    ).toBeVisible({ timeout: 15_000 });
  } else {
    await expect(landing).toHaveValue(target);
  }
}

test.describe("Homepage Navigation landing (#3219)", () => {
  test("login default is dispatcher; Architecture pref opens Navigation @smoke @ui", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await loginAsAdmin(page);

    let restoreTo = "";
    try {
      const landing = await openPreferences(page);
      restoreTo = await landing.inputValue();
      const optionValues = await landing.locator("option").evaluateAll((els) =>
        els.map((o) => o.value),
      );
      expect(optionValues).toContain("Architecture");

      await saveLanding(page, "Architecture");

      await page.goto(`${BASE_URL}/Rhythmyx/logout`, {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("perc-logout-page")).toBeVisible({
        timeout: 20_000,
      });

      await page.goto(`${BASE_URL}/Rhythmyx/login`, {
        waitUntil: "domcontentloaded",
      });
      const redirect = page.getByTestId("perc-login-redirect");
      await expect(redirect).toBeAttached({ timeout: 20_000 });
      await expect(redirect).toHaveValue("/cm/app/");

      await loginAsAdmin(page);

      await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
        timeout: 40_000,
      });
      await expect(page.getByTestId("architecture-action-new-site")).toBeVisible();
      const url = page.url();
      expect(url).toMatch(/architecture|entry=architecture|view=arch/i);
      expect(url).not.toMatch(/\/home(\/|$|\?)/);
    } finally {
      try {
        await loginAsAdmin(page);
        await saveLanding(page, restoreTo);
      } catch {
        // Best-effort restore; do not mask the original assertion.
      }
    }

    expect(pageErrors, pageErrors.join("\n")).toEqual([]);
  });
});
