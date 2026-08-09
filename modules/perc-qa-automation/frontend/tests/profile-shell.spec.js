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
 * Profile hub shell smoke + axe WCAG gate
 * (#2393 / #2425 / #2427 / #2497 / #2498 / parent #2374).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-shell.spec.js
 *
 * Axe-only subset (serious/critical zero on hub after deep link + menu entry):
 *   npm run test:surface -- --path tests/profile-shell.spec.js --grep "axe-core"
 *
 * Non-admin menu click only (#2497 residual):
 *   npm run test:surface -- --path tests/profile-shell.spec.js --grep "menu entry"
 *
 * Extended golden multi-path (baseline + folder-recycle + this surface) — #2498:
 *   npm run test:golden-extended
 *   npm run test:golden-extended:list
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* / EDITOR_* / CONTRIBUTOR_*
 * → test:surface or test:golden-extended → qa-down.
 *
 * Covers Admin deep link + UserMenu entry, non-admin (Editor, Contributor)
 * deep-link access, and non-admin UserMenu → My profile click path (#2497)
 * so profile is not admin-only (#2374 acceptance).
 * Inventory: helpers/golden-unattended-smoke-set.js (id profile-shell, tier extended).
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  loginAsEditor,
  loginAsContributor,
  BASE_URL,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

/** Stable CSS scope for axe — matches data-testid on ProfileShell root. */
const PROFILE_SHELL_SCOPE = '[data-testid="perc-profile-shell"]';

function profileDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`;
}

function homeUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

/**
 * Wait until the profile hub shell and section landmarks are mounted.
 * Shared by smoke and axe tests so a11y scans run only after React paint.
 *
 * @param {import('@playwright/test').Page} page
 */
async function expectProfileShellMounted(page) {
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
}

test.describe("Profile shell @profile @smoke", () => {
  test("deep link opens profile shell with section landmarks", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);
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

    await expectProfileShellMounted(page);
  });

  /**
   * #2427 — axe WCAG 2.1 A/AA serious/critical zero on hub after deep link.
   * Reuses tests/helpers/a11y.js (expectNoSeriousA11yViolations).
   */
  test("axe-core a11y gate — profile shell via deep link (#2427)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);

    await expectNoSeriousA11yViolations(page, {
      scope: PROFILE_SHELL_SCOPE,
    });
  });

  /**
   * #2427 — same axe gate after opening My profile from the user menu so
   * menu-driven navigation does not leave a different a11y tree than deep link.
   */
  test("axe-core a11y gate — profile shell via My profile menu (#2427)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(homeUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-spa-user-menu")).toBeVisible({
      timeout: 30_000,
    });

    const menuLink = page.getByTestId("perc-spa-my-profile");
    await expect(menuLink).toBeVisible();
    await menuLink.click();
    await expectProfileShellMounted(page);

    await expectNoSeriousA11yViolations(page, {
      scope: PROFILE_SHELL_SCOPE,
    });
  });

  test("Editor deep link opens profile shell (non-admin)", async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsEditor(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);
  });

  test("Contributor deep link opens profile shell (non-admin)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsContributor(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);
  });

  /**
   * #2497 residual of #2425 — non-admin UserMenu → My profile click path.
   * Deep link alone is not enough; menu entry must navigate for Editor/Contributor.
   */
  test("Editor My profile menu entry navigates to profile shell (#2497)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsEditor(page);

    await page.goto(homeUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-spa-user-menu")).toBeVisible({
      timeout: 30_000,
    });

    const menuLink = page.getByTestId("perc-spa-my-profile");
    await expect(menuLink).toBeVisible();
    await expect(menuLink).toBeEnabled();
    await menuLink.click();

    await expectProfileShellMounted(page);
  });

  test("Contributor My profile menu entry navigates to profile shell (#2497)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsContributor(page);

    await page.goto(homeUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-spa-user-menu")).toBeVisible({
      timeout: 30_000,
    });

    const menuLink = page.getByTestId("perc-spa-my-profile");
    await expect(menuLink).toBeVisible();
    await expect(menuLink).toBeEnabled();
    await menuLink.click();

    await expectProfileShellMounted(page);
  });
});
