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
 * Profile Avatar / Gravatar + header avatar chip (#2397 / parent #2374 slice 5).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-avatar.spec.js
 *
 * Axe form root (#2503 / residual #2427):
 *   npm run test:surface -- --path tests/profile-avatar.spec.js --grep "axe-core"
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Axe: zero serious/critical on [data-testid="perc-profile-avatar"].
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

/** Form root for Gravatar / avatar controls (not the whole profile shell). */
const AVATAR_FORM_SCOPE = '[data-testid="perc-profile-avatar"]';
const ENGLISH_PROFILE_TITLE = /my profile/i;

function profileDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`;
}

function homeUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function expectAvatarSectionMounted(page) {
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-title")).toContainText(
    ENGLISH_PROFILE_TITLE,
  );
  await expect(page.getByTestId("perc-profile-section-avatar")).toBeVisible();
  await expect(page.getByTestId("perc-profile-avatar")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-avatar-preview")).toBeVisible();
  await expect(page.getByTestId("perc-profile-avatar-email")).toBeVisible();
  await expect(page.getByTestId("perc-profile-avatar-privacy")).toBeVisible();
}

test.describe("Profile avatar Gravatar @profile @avatar", () => {
  test("Admin deep link shows avatar section, preview, and header chip", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto(profileDeepLink());
    await expectAvatarSectionMounted(page);

    // Header chip with accessible name
    const headerAvatar = page.getByTestId("perc-spa-user-avatar");
    await expect(headerAvatar).toBeVisible();
    const aria = await headerAvatar.getAttribute("aria-label");
    expect(aria && aria.length > 0).toBeTruthy();

    // Use primary is the default path
    const usePrimary = page.getByTestId("perc-profile-avatar-use-primary");
    await expect(usePrimary).toBeVisible();

    // Live section is not "Coming soon"
    await expect(
      page.getByTestId("perc-profile-section-avatar-status"),
    ).toHaveCount(0);
  });

  /**
   * #2503 residual of #2427 — axe serious/critical zero on avatar form root
   * (preview, use-primary, override email, privacy copy, save). Scope is the
   * form section; shell-level axe remains in profile-shell.spec.js.
   * Also fixes a prior call that passed a selector string as opts (scope was
   * never applied — whole page scanned unintentionally).
   */
  test("axe-core a11y gate — avatar form root (#2503)", async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectAvatarSectionMounted(page);

    await expectNoSeriousA11yViolations(page, {
      scope: AVATAR_FORM_SCOPE,
    });
  });

  test("Admin can set Gravatar override email and save", async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto(profileDeepLink());
    await expectAvatarSectionMounted(page);

    const usePrimary = page.getByTestId("perc-profile-avatar-use-primary");
    // Uncheck to enable override field when currently using primary
    if (await usePrimary.isChecked()) {
      await usePrimary.uncheck();
    }

    const email = page.getByTestId("perc-profile-avatar-email");
    await expect(email).toBeEnabled();
    const unique = `night-avatar-${Date.now()}@example.com`;
    await email.fill(unique);

    const save = page.getByTestId("perc-profile-avatar-save");
    await expect(save).toBeEnabled();
    await save.click();

    await expect(page.getByTestId("perc-profile-avatar-success")).toBeVisible({
      timeout: 20_000,
    });

    // Preview chip remains present (image or initials)
    await expect(page.getByTestId("perc-profile-avatar-chip")).toBeVisible();
  });

  test("Header avatar visible from home after login", async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto(homeUrl());
    await expect(page.getByTestId("perc-spa-user-menu")).toBeVisible({
      timeout: 30_000,
    });
    const headerAvatar = page.getByTestId("perc-spa-user-avatar");
    await expect(headerAvatar).toBeVisible();
    await expect(headerAvatar).toHaveAttribute("aria-label", /.+/);
  });
});
