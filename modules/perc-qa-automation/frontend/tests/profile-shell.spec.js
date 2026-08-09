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
 * Profile hub shell smoke + axe WCAG gate + locale title residual
 * (#2393 / #2425 / #2427 / #2497 / #2498 / #2499 / #2501 / parent #2374).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-shell.spec.js
 *
 * Locale title residual only (#2499):
 *   npm run test:surface -- --path tests/profile-shell.spec.js --grep "locale"
 *
 * Axe-only subset (serious/critical zero on hub after deep link + menu entry):
 *   npm run test:surface -- --path tests/profile-shell.spec.js --grep "axe-core"
 *
 * Non-admin menu click only (#2497 residual):
 *   npm run test:surface -- --path tests/profile-shell.spec.js --grep "menu entry"
 *
 * Non-admin axe only (#2501 residual of #2427):
 *   npm run test:surface -- --path tests/profile-shell.spec.js --grep "Editor|Contributor"
 *
 * Extended golden multi-path (baseline + folder-recycle + this surface) — #2498:
 *   npm run test:golden-extended
 *   npm run test:golden-extended:list
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* / EDITOR_* / CONTRIBUTOR_*
 * → test:surface or test:golden-extended → qa-down.
 *
 * Covers Admin deep link + UserMenu entry, non-admin (Editor, Contributor)
 * deep-link smoke + axe, non-admin UserMenu → My profile click path (#2497),
 * and non-admin UserMenu → My profile axe (#2501) so profile is not admin-only
 * (#2374 acceptance / #2427 residual).
 * Locale residual: login with de or es and assert perc-profile-title via TMX
 * string — not English-only /my profile/i.
 * Inventory: helpers/golden-unattended-smoke-set.js (id profile-shell, tier extended).
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  loginAsEditor,
  loginAsContributor,
  listModernLoginLocales,
  BASE_URL,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  pickPreferredLocaleTag,
  localeLanguageFamily,
} = require("./helpers/pick-locale-tag");
const {
  expectedProfileTitle,
  profileTitleMatcher,
  PROFILE_TITLE_PREFERRED_LOCALES,
  ENGLISH_PROFILE_TITLE_MATCHER,
} = require("./helpers/profile-shell-title");

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
 * @param {object} [opts]
 * @param {RegExp|string} [opts.titleMatcher] title text match; default English smoke
 */
async function expectProfileShellMounted(page, opts = {}) {
  const titleMatcher = opts.titleMatcher || ENGLISH_PROFILE_TITLE_MATCHER;
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-title")).toContainText(
    titleMatcher,
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

/**
 * Pick de-de/de/es from the live login locale list (modern or legacy).
 *
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<{ tag: string, family: string, title: string, titleRe: RegExp }>}
 */
async function resolveProfileTitleLocale(page) {
  await page.goto(`${BASE_URL}/Rhythmyx/login`);
  await page.waitForLoadState("domcontentloaded");
  // Both root + page may be present; .first() avoids strict-mode dual match.
  await expect(
    page
      .getByTestId("perc-login-page")
      .or(page.getByTestId("perc-login-root"))
      .first(),
  ).toBeVisible({ timeout: 20_000 });

  let available = await listModernLoginLocales(page);
  if (available.length === 0) {
    const native = page.locator('select[name="j_locale"]');
    if ((await native.count()) > 0) {
      available = await native
        .locator("option")
        .evaluateAll((opts) => opts.map((o) => o.value).filter(Boolean));
    }
  }

  const tag = pickPreferredLocaleTag(
    available,
    PROFILE_TITLE_PREFERRED_LOCALES,
  );
  expect(
    tag,
    `install must expose de-de, de, or es for profile title locale residual (available=${JSON.stringify(available)})`,
  ).toBeTruthy();

  const family = localeLanguageFamily(tag);
  const title = expectedProfileTitle(family);
  const titleRe = profileTitleMatcher(family);
  expect(
    title && titleRe,
    `no profile title map for language family ${family} (tag=${tag})`,
  ).toBeTruthy();

  return { tag, family, title, titleRe };
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

  /**
   * #2501 residual of #2427 — axe serious/critical zero for Editor after deep link.
   * Admin-only axe is insufficient; same shell must pass for non-admin roles.
   */
  test("axe-core a11y gate — Editor profile shell via deep link (#2501)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsEditor(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);

    await expectNoSeriousA11yViolations(page, {
      scope: PROFILE_SHELL_SCOPE,
    });
  });

  /**
   * #2501 — axe after Editor UserMenu → My profile (menu tree vs deep link).
   * Menu smoke for non-admin is #2497; this gates a11y on that path.
   */
  test("axe-core a11y gate — Editor profile shell via My profile menu (#2501)", async ({
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

    await expectNoSeriousA11yViolations(page, {
      scope: PROFILE_SHELL_SCOPE,
    });
  });

  /**
   * #2501 residual of #2427 — axe serious/critical zero for Contributor after deep link.
   */
  test("axe-core a11y gate — Contributor profile shell via deep link (#2501)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsContributor(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);

    await expectNoSeriousA11yViolations(page, {
      scope: PROFILE_SHELL_SCOPE,
    });
  });

  /**
   * #2501 — axe after Contributor UserMenu → My profile.
   */
  test("axe-core a11y gate — Contributor profile shell via My profile menu (#2501)", async ({
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

    await expectNoSeriousA11yViolations(page, {
      scope: PROFILE_SHELL_SCOPE,
    });
  });

  /**
   * #2499 / parent #2374 — after modern-locale TMX (#2426), non-English
   * login must render perc-profile-title from CmsUi.tmx (de: Mein Profil,
   * es: mi perfil), not English-only /my profile/i.
   *
   * Surface filter:
   *   npm run test:surface -- --path tests/profile-shell.spec.js --grep "locale"
   */
  test("non-English locale shows localized profile title (#2499)", async ({
    page,
  }) => {
    test.setTimeout(120_000);

    const { tag, family, title, titleRe } =
      await resolveProfileTitleLocale(page);

    await loginAsAdmin(page, { locale: tag });
    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page, { titleMatcher: titleRe });

    const titleEl = page.getByTestId("perc-profile-title");
    await expect(titleEl).toHaveText(titleRe, { timeout: 15_000 });
    // Guard against English fallback when session locale is de/es.
    if (family !== "en") {
      await expect(titleEl).not.toHaveText(/^\s*My profile\s*$/i);
      await expect(titleEl).toContainText(title, { ignoreCase: true });
    }
  });
});
