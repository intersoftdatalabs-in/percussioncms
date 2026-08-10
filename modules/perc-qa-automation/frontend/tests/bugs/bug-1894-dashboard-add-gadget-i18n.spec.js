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
 * Regression: Dashboard Add Gadget chrome is localized (GH-1894).
 *
 * <p>Follow-up for GH-1840 / PR #1863 which added TMX unit
 * {@code perc.ui.dashboard.modern@Add Gadget}. After Admin login with a
 * non-en-us locale, the Home gadgets toolbar button
 * ({@code data-testid=dashboard-add-gadget}) and Add Gadget modal title must
 * show the ship-matrix string for that locale — not bare English
 * {@code Add Gadget}.</p>
 *
 * <p>Prefers Spanish ({@code es}) — same endonym family as the GH-1608
 * locale suite — with French/German fallbacks when {@code es} is not in the
 * login locale list. Env-only: {@code TEST_CMS_URL} via H2 {@code qa-up};
 * no {@code DEV_PERCUSSION_INSTALL} required.</p>
 */

const { test, expect } = require("@playwright/test");
const { BASE_URL, ADMIN_USERNAME, ADMIN_PASSWORD } = require("../helpers/auth");

/**
 * Expected TMX segments for perc.ui.dashboard.modern@Add Gadget (CmsUi.tmx).
 * Prefer Spanish; fr/de are fall-backs when the install omits es.
 */
const ADD_GADGET_BY_LOCALE = {
  es: "Agregar gadget",
  "es-es": "Agregar gadget",
  fr: "Ajouter un gadget",
  "fr-fr": "Ajouter un gadget",
  de: "Gadget hinzufügen",
  "de-de": "Gadget hinzufügen",
};

const LOCALE_PREFERENCE = ["es", "es-es", "fr", "fr-fr", "de", "de-de"];

/**
 * Pick the first preferred non-en locale that has both a login option and a
 * known ship-matrix string.
 *
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<{ locale: string, label: string }>}
 */
async function pickNonEnUsLocale(page) {
  const trigger = page.getByTestId("perc-login-locale");
  await expect(trigger).toBeVisible({ timeout: 15_000 });
  await trigger.click();

  const list = page.getByTestId("perc-login-locale-list");
  await expect(list).toBeVisible({ timeout: 10_000 });

  const optionValues = await list
    .locator('[role="option"]')
    .evaluateAll((opts) =>
      opts
        .map((o) => o.getAttribute("data-testid") || "")
        .map((id) => id.replace(/^perc-login-locale-option-/, ""))
        .filter(Boolean),
    );

  // Close listbox before returning so credential fills on the same page are
  // not blocked (login reuses this navigation; no second login goto).
  await page.keyboard.press("Escape").catch(() => {});
  await list.waitFor({ state: "hidden", timeout: 5_000 }).catch(() => {});

  for (const locale of LOCALE_PREFERENCE) {
    if (optionValues.includes(locale) && ADD_GADGET_BY_LOCALE[locale]) {
      return { locale, label: ADD_GADGET_BY_LOCALE[locale] };
    }
  }

  throw new Error(
    `install must expose one of [${LOCALE_PREFERENCE.join(", ")}] in the ` +
      `login locale combobox for Add Gadget i18n (found: ${optionValues.join(", ") || "none"})`,
  );
}

/**
 * Admin login posting j_locale via modern LocaleSelect (combobox + hidden).
 *
 * <p>When the page is already on the modern login form (e.g. after locale
 * discovery), skips a second {@code goto} so discovery + login share one load.
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} locale
 * @param {{ alreadyOnLogin?: boolean }} [options]
 */
async function loginAsAdminWithLocale(page, locale, options = {}) {
  if (!ADMIN_PASSWORD) {
    throw new Error(
      "ADMIN_PASSWORD not set. For QA mode set ADMIN_PASSWORD from perc-devctl " +
        "qa-up / docker exec (never commit secrets).",
    );
  }

  const alreadyOnLogin =
    options.alreadyOnLogin === true ||
    /\/Rhythmyx\/login(\?|$)|\/login(\?|$)/.test(page.url());
  if (!alreadyOnLogin) {
    await page.goto(`${BASE_URL}/Rhythmyx/login`);
    await page.waitForLoadState("domcontentloaded");
  }

  const modernRoot = page.locator('[data-testid="perc-login-root"]');
  const modernForm = page.locator('[data-testid="perc-login-form"]');
  await modernForm
    .or(modernRoot)
    .first()
    .waitFor({ state: "visible", timeout: 30_000 });

  await page.getByTestId("perc-login-username").fill(ADMIN_USERNAME);
  await page.getByTestId("perc-login-password").fill(ADMIN_PASSWORD);

  const trigger = page.getByTestId("perc-login-locale");
  await trigger.click();
  const option = page.getByTestId(`perc-login-locale-option-${locale}`);
  await expect(
    option,
    `locale option ${locale} must be present in login combobox`,
  ).toBeVisible({ timeout: 10_000 });
  await option.click();

  const hiddenLocale = page.getByTestId("perc-login-locale-value");
  await expect(hiddenLocale).toHaveValue(locale, { timeout: 5_000 });

  const submit = page.getByTestId("perc-login-submit");
  await Promise.all([
    page
      .waitForFunction(
        () => {
          const p = window.location.pathname;
          return !p.endsWith("/Rhythmyx/login") && !p.endsWith("/login");
        },
        null,
        { timeout: 30_000 },
      )
      .catch(async () => {
        await page.waitForTimeout(1500);
      }),
    submit.click(),
  ]);

  const url = page.url();
  if (url.includes("/Rhythmyx/login") || /\/login(\?|$)/.test(url)) {
    throw new Error(
      `Login with locale=${locale} did not leave login page (still at ${url})`,
    );
  }
}

test.describe("Dashboard Add Gadget i18n (GH-1894 / PR #1863)", () => {
  test("non-en-us locale localizes Add Gadget button and modal title", async ({
    page,
  }) => {
    test.setTimeout(90_000);

    // Discover a supported non-en locale before authenticating (single login load).
    await page.goto(`${BASE_URL}/Rhythmyx/login`);
    await expect(page.getByTestId("perc-login-page")).toBeVisible({
      timeout: 15_000,
    });
    const { locale, label } = await pickNonEnUsLocale(page);

    await loginAsAdminWithLocale(page, locale, { alreadyOnLogin: true });

    // Home → gadgets section (PR-7). Prefer query entry contract; SPA may
    // rewrite to /cm/app/home/gadgets. Bare /cm/app/home is not a valid host.
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&section=gadgets`,
    );
    await page.waitForLoadState("networkidle");
    await expect(page.getByTestId("home-gadgets-section")).toBeVisible({
      timeout: 30_000,
    });

    const addBtn = page.getByTestId("dashboard-add-gadget");
    await expect(addBtn).toBeVisible({ timeout: 15_000 });

    // Dashboard.tsx prefixes "+ " before the TMX segment.
    const btnText = (await addBtn.innerText()).trim();
    expect(
      btnText,
      `button must not stay bare English under locale=${locale}`,
    ).not.toMatch(/^(\+\s*)?Add Gadget$/i);
    expect(
      btnText,
      `expected ship-matrix label for locale=${locale}`,
    ).toContain(label);

    await addBtn.click();

    // Modal title uses the same TMX unit (MSG.MODAL_ADD_GADGET_TITLE).
    const modalTitle = page.getByRole("heading", { name: label, exact: true });
    await expect(modalTitle).toBeVisible({ timeout: 10_000 });
    await expect(modalTitle).not.toHaveText(/^Add Gadget$/i);
  });
});
