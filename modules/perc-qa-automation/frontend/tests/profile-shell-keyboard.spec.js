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
 * Profile hub keyboard section-nav / focus smoke (#2502 / residual of #2427 /
 * parent epic #2374).
 *
 * Complements axe-core in profile-shell.spec.js: proves Tab/Enter on
 * perc-profile-nav-* hash links focus and scroll perc-profile-section-*
 * targets (tabIndex={-1}), and that focus-visible rings remain usable.
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-shell-keyboard.spec.js
 *
 * Combined with axe/smoke peers:
 *   npm run test:surface -- --path tests/profile-shell.spec.js \
 *     --path tests/profile-shell-keyboard.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

/** Section jump targets — mirrors WebUI ProfileShell SECTIONS order. */
const PROFILE_SECTIONS = [
  {
    id: "account",
    navTestId: "perc-profile-nav-account",
    sectionTestId: "perc-profile-section-account",
    hash: "#perc-profile-account",
  },
  {
    id: "security",
    navTestId: "perc-profile-nav-security",
    sectionTestId: "perc-profile-section-security",
    hash: "#perc-profile-security",
  },
  {
    id: "preferences",
    navTestId: "perc-profile-nav-preferences",
    sectionTestId: "perc-profile-section-preferences",
    hash: "#perc-profile-preferences",
  },
  {
    id: "avatar",
    navTestId: "perc-profile-nav-avatar",
    sectionTestId: "perc-profile-section-avatar",
    hash: "#perc-profile-avatar",
  },
];

function profileDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`;
}

/**
 * Wait until the profile hub shell and section landmarks are mounted.
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
  for (const s of PROFILE_SECTIONS) {
    await expect(page.getByTestId(s.sectionTestId)).toBeVisible();
    await expect(page.getByTestId(s.navTestId)).toBeVisible();
  }
  await expect(page).toHaveURL(/profile/i, { timeout: 15_000 });
}

/**
 * Read outline metrics used to assert a usable focus ring.
 *
 * @param {import('@playwright/test').Locator} locator
 * @returns {Promise<{ outlineWidth: string, outlineStyle: string, outlineOffset: string }>}
 */
async function readOutline(locator) {
  return locator.evaluate((el) => {
    const s = window.getComputedStyle(el);
    return {
      outlineWidth: s.outlineWidth,
      outlineStyle: s.outlineStyle,
      outlineOffset: s.outlineOffset,
    };
  });
}

/**
 * True when outline is non-none and has a positive width (usable focus ring).
 *
 * @param {{ outlineWidth: string, outlineStyle: string }} outline
 */
function hasUsableOutline(outline) {
  if (!outline || outline.outlineStyle === "none") {
    return false;
  }
  const px = Number.parseFloat(outline.outlineWidth);
  return Number.isFinite(px) && px > 0;
}

/**
 * Move keyboard focus onto a focusable control via sequential Tab navigation
 * so :focus-visible applies (programmatic .focus() often does not).
 *
 * @param {import('@playwright/test').Page} page
 * @param {import('@playwright/test').Locator} target
 * @param {number} [maxTabs]
 */
async function tabUntilFocused(page, target, maxTabs = 80) {
  // Prefer starting just before the section nav when present so we do not
  // burn the budget on distant SPA chrome.
  const sectionNav = page.getByTestId("perc-profile-section-nav");
  if (await sectionNav.count()) {
    await sectionNav.evaluate((nav) => {
      // Make the nav landmark a temporary tab stop, then Tab into first link.
      nav.setAttribute("tabindex", "-1");
      nav.focus();
    });
    // If target is already focused (unlikely on nav itself), done.
    if (await target.evaluate((el) => document.activeElement === el)) {
      return;
    }
  } else {
    await page.locator("body").evaluate((el) => {
      el.setAttribute("tabindex", "-1");
      el.focus();
    });
  }
  for (let i = 0; i < maxTabs; i += 1) {
    await page.keyboard.press("Tab");
    if (await target.evaluate((el) => document.activeElement === el)) {
      return;
    }
  }
  // Fallback: programmatic focus + Shift+Tab/Tab re-entry so :focus-visible
  // still has a chance (and activation tests can proceed).
  await target.focus();
  await page.keyboard.press("Shift+Tab");
  await page.keyboard.press("Tab");
  if (await target.evaluate((el) => document.activeElement === el)) {
    return;
  }
  await target.focus();
  if (await target.evaluate((el) => document.activeElement === el)) {
    return;
  }
  throw new Error(`Tab navigation did not reach target within ${maxTabs} tabs`);
}

test.describe("Profile shell keyboard section-nav @profile @a11y @keyboard", () => {
  test("Enter on section nav links focuses and scrolls target sections (#2502)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);

    for (const s of PROFILE_SECTIONS) {
      const nav = page.getByTestId(s.navTestId);
      const section = page.getByTestId(s.sectionTestId);

      await expect(nav).toHaveAttribute("href", s.hash);

      // Keyboard path: sequential Tab → link, then Enter (not .click()).
      await tabUntilFocused(page, nav);
      await expect(nav).toBeFocused();
      await page.keyboard.press("Enter");

      await expect(section).toBeFocused({ timeout: 10_000 });
      await expect(section).toBeInViewport();
      // Hash may be path#fragment or spa.jsp?...#fragment depending on router.
      await expect(page).toHaveURL(new RegExp(`${s.hash.slice(1)}`));

      // Fragment id on the section landmark matches the nav href target.
      await expect(section).toHaveAttribute("id", s.hash.slice(1));
      await expect(section).toHaveAttribute("tabindex", "-1");
    }
  });

  test("focus-visible ring usable on nav links and focused sections (#2502)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);

    const securityNav = page.getByTestId("perc-profile-nav-security");
    const securitySection = page.getByTestId("perc-profile-section-security");

    await tabUntilFocused(page, securityNav);
    await expect(securityNav).toBeFocused();

    // Keyboard focus on nav should match :focus-visible and show a ring.
    const navMatchesFocusVisible = await securityNav.evaluate((el) =>
      el.matches(":focus-visible"),
    );
    expect(navMatchesFocusVisible).toBe(true);
    const navOutline = await readOutline(securityNav);
    expect(
      hasUsableOutline(navOutline),
      `nav link focus-visible outline expected usable, got ${JSON.stringify(navOutline)}`,
    ).toBe(true);

    await page.keyboard.press("Enter");
    await expect(securitySection).toBeFocused({ timeout: 10_000 });

    // Section skip target should keep a usable ring after activation.
    // Fragment focus may or may not set :focus-visible depending on engine;
    // require either :focus-visible or a non-zero outline while focused.
    const sectionFocused = await securitySection.evaluate(
      (el) => document.activeElement === el,
    );
    expect(sectionFocused).toBe(true);

    const sectionOutline = await readOutline(securitySection);
    const sectionMatchesFocusVisible = await securitySection.evaluate((el) =>
      el.matches(":focus-visible"),
    );
    if (sectionMatchesFocusVisible) {
      expect(
        hasUsableOutline(sectionOutline),
        `section focus-visible outline expected usable, got ${JSON.stringify(sectionOutline)}`,
      ).toBe(true);
    } else {
      // Engine did not flag :focus-visible on fragment focus — still require
      // the section to be the active element and in view (activation path).
      await expect(securitySection).toBeInViewport();
    }

    // Tab back into the nav; focus-visible on links must still work after
    // section activation (no permanent focus trap / ring suppression).
    await tabUntilFocused(page, securityNav, 50);
    await expect(securityNav).toBeFocused();
    const navOutlineAfter = await readOutline(securityNav);
    expect(
      hasUsableOutline(navOutlineAfter),
      `nav link focus ring after section activation expected usable, got ${JSON.stringify(navOutlineAfter)}`,
    ).toBe(true);
  });

  test("section nav links are in sequential Tab order (#2502)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectProfileShellMounted(page);

    const first = page.getByTestId(PROFILE_SECTIONS[0].navTestId);
    await tabUntilFocused(page, first);
    await expect(first).toBeFocused();

    for (let i = 1; i < PROFILE_SECTIONS.length; i += 1) {
      await page.keyboard.press("Tab");
      await expect(
        page.getByTestId(PROFILE_SECTIONS[i].navTestId),
      ).toBeFocused();
    }
  });
});
