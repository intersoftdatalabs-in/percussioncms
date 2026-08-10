/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Regression: Home / dashboard.modern body + modal keys under non-English
 * locale (GH-1876 residual of #1852 / PR #1875).
 *
 * <p>Logs in with a non-English locale (prefer {@code de-de}/{@code de}, else
 * {@code hi-in}/{@code hi}, else {@code es}) and asserts a sample of residual
 * Home gadget chrome/body/modal strings from {@code CmsUi.tmx} are localized —
 * not English fallback after {@code @}.</p>
 *
 * <p>Requires live CMS (QA mode via {@code perc-devctl qa-up} + {@code TEST_CMS_URL}
 * preferred). Surface-filterable path — do not require full suite.</p>
 *
 * <pre>
 *   # From repo root
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/bugs/bug-1876-home-gadget-locale.spec.js
 * </pre>
 *
 * <p>Tags: {@code @locale} {@code @home} {@code @dashboard}</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  listModernLoginLocales,
} = require("../helpers/auth");
const {
  pickPreferredLocaleTag,
  localeLanguageFamily,
} = require("../helpers/pick-locale-tag");

/**
 * Expected localized samples for residual dashboard.modern / welcome /
 * activity keys shipped in #1852 / PR #1875 (CmsUi.tmx).
 *
 * Values must match TMX segs for the language family (not en-us fallback).
 */
const CHROME_BY_FAMILY = {
  de: {
    addGadget: "Gadget hinzufügen",
    searchPlaceholder: "Gadgets durchsuchen...",
    noGadgetsFound: "Keine Gadgets gefunden",
    activityTitle: "Aktivität",
    welcomeTitle: "WILLKOMMEN",
    welcomeBlurb: "Verwendung von Percussion CMS",
    greetings: ["Guten Morgen", "Guten Tag", "Guten Abend"],
    // English fallbacks that must not appear for these surfaces
    englishForbidden: [
      "Add Gadget",
      "Search gadgets...",
      "No gadgets found",
      "Good morning",
      "Good afternoon",
      "Good evening",
      "Using Percussion CMS",
    ],
  },
  hi: {
    addGadget: "गैजेट जोड़ें",
    searchPlaceholder: "गैजेट खोजें...",
    noGadgetsFound: "कोई गैजेट नहीं मिला",
    activityTitle: "गतिविधि",
    welcomeTitle: "स्वागत",
    welcomeBlurb: "पर्कशन सीएमएस का उपयोग करना",
    greetings: ["शुभ प्रभात", "शुभ दोपहर", "शुभ संध्या"],
    englishForbidden: [
      "Add Gadget",
      "Search gadgets...",
      "No gadgets found",
      "Good morning",
      "Good afternoon",
      "Good evening",
      "Using Percussion CMS",
    ],
  },
  es: {
    addGadget: "Agregar gadget",
    searchPlaceholder: "Buscar gadgets...",
    noGadgetsFound: "No se encontraron dispositivos",
    activityTitle: "Actividad",
    welcomeTitle: "BIENVENIDO",
    // Sample only — do not expand Spanish/#961 residual matrix here.
    welcomeBlurb: null,
    greetings: ["Buen día", "Buenas tardes", "Buenas noches"],
    englishForbidden: [
      "Add Gadget",
      "Search gadgets...",
      "No gadgets found",
      "Good morning",
      "Good afternoon",
      "Good evening",
    ],
  },
};

function homeUrl() {
  // Prefer SPA entry (post-login default). Cache-bust so TMX/session locale is fresh.
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

/**
 * Open login page and pick a preferred non-English locale without submitting.
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<{ tag: string, family: string, chrome: object }>}
 */
async function resolveNonEnglishLocale(page) {
  await page.goto(`${BASE_URL}/Rhythmyx/login`);
  await page.waitForLoadState("domcontentloaded");
  await expect(
    page.getByTestId("perc-login-page").or(page.getByTestId("perc-login-root")),
  ).toBeVisible({
    timeout: 20_000,
  });

  let available = await listModernLoginLocales(page);
  if (available.length === 0) {
    // Legacy select fallback
    const native = page.locator('select[name="j_locale"]');
    if ((await native.count()) > 0) {
      available = await native
        .locator("option")
        .evaluateAll((opts) => opts.map((o) => o.value).filter(Boolean));
    }
  }

  const tag = pickPreferredLocaleTag(available);
  expect(
    tag,
    `install must expose a non-English login locale (available=${JSON.stringify(available)})`,
  ).toBeTruthy();

  const family = localeLanguageFamily(tag);
  const chrome = CHROME_BY_FAMILY[family];
  expect(
    chrome,
    `no sample chrome map for language family ${family} (tag=${tag})`,
  ).toBeTruthy();

  return { tag, family, chrome };
}

test.describe("Home gadget locale residual keys (GH-1876) @locale @home @dashboard", () => {
  test("non-English login localizes Add Gadget modal + sample body keys", async ({
    page,
  }) => {
    test.setTimeout(120_000);

    const { tag, family, chrome } = await resolveNonEnglishLocale(page);

    // Full login with the chosen locale (session j_locale → TMX sys_lang).
    await loginAsAdmin(page, { locale: tag });

    await page.goto(homeUrl(), { waitUntil: "domcontentloaded" });
    await page.waitForLoadState("networkidle").catch(() => {});

    const gadgets = page.getByTestId("home-gadgets-section");
    const dashboard = page.getByTestId("dashboard-root");
    await expect(gadgets.or(dashboard).first()).toBeVisible({
      timeout: 45_000,
    });

    // --- Toolbar: Add Gadget button (DASHBOARD_ADD_GADGET / MODAL title key) ---
    const addBtn = page.getByTestId("dashboard-add-gadget");
    await expect(addBtn).toBeVisible({ timeout: 20_000 });
    await expect(addBtn).toHaveText(new RegExp(escapeRegExp(chrome.addGadget)));
    await expect(addBtn).not.toHaveText(/^\s*\+?\s*Add Gadget\s*$/i);

    // --- Add Gadget modal chrome (search placeholder + title) ---
    await addBtn.click();
    const modalTitle = page.getByRole("heading", { level: 2 }).filter({
      hasText: chrome.addGadget,
    });
    await expect(modalTitle.first()).toBeVisible({ timeout: 10_000 });

    const search = page.locator(
      `input[placeholder="${cssEscapeAttr(chrome.searchPlaceholder)}"]`,
    );
    // Placeholder may also match via getByPlaceholder if exact.
    const searchAlt = page.getByPlaceholder(chrome.searchPlaceholder);
    await expect(search.or(searchAlt).first()).toBeVisible({ timeout: 10_000 });

    // Type nonsense so empty state uses MODAL_NO_RESULTS (residual key).
    await search.or(searchAlt).first().fill("__no_such_gadget_xyz__");
    const empty = page.getByTestId("add-gadget-empty");
    await expect(empty).toBeVisible({ timeout: 10_000 });
    await expect(empty).toHaveText(chrome.noGadgetsFound);
    await expect(empty).not.toHaveText(/No gadgets found/i);

    // Close modal (click overlay backdrop or press Escape).
    await page.keyboard.press("Escape");
    await expect(empty)
      .toBeHidden({ timeout: 5_000 })
      .catch(async () => {
        // Backdrop click fallback
        await page.locator("body").click({ position: { x: 4, y: 4 } });
      });

    // --- Sample body keys: Welcome greeting/blurb + Activity title ---
    // Welcome is in DEFAULT_GADGET_IDS; Activity too. Titles/body use message(MSG.*).
    const body = await page.locator("body").innerText();

    // Greetings are time-of-day dependent — any one of the three is fine.
    const greetingHit = chrome.greetings.some((g) => body.includes(g));
    expect(
      greetingHit,
      `expected one of welcome greetings ${JSON.stringify(chrome.greetings)} for family=${family}`,
    ).toBe(true);

    if (chrome.welcomeBlurb) {
      expect(
        body.includes(chrome.welcomeBlurb),
        `expected welcome blurb "${chrome.welcomeBlurb}" for family=${family}`,
      ).toBe(true);
    }

    // Activity gadget title (residual title key from parent #1852 matrix).
    const activity = page.getByTestId("activity-widget");
    if (await activity.isVisible().catch(() => false)) {
      await expect(activity).toContainText(chrome.activityTitle);
      await expect(activity).not.toHaveText(/^Activity$/);
    } else {
      // Layout may omit activity if session prefs differ — still require
      // welcome chrome already checked, and title string somewhere on Home.
      expect(
        body.includes(chrome.activityTitle) ||
          body.includes(chrome.welcomeTitle),
        `expected activity title or welcome title on Home for family=${family}`,
      ).toBe(true);
    }

    // Hard ban: English fallback for residual sample keys must not remain.
    for (const en of chrome.englishForbidden) {
      expect(
        body.includes(en),
        `English fallback "${en}" must not appear after ${tag} login (family=${family})`,
      ).toBe(false);
    }
  });
});

/**
 * Escape a string for use inside a RegExp.
 * @param {string} s
 * @returns {string}
 */
function escapeRegExp(s) {
  return String(s).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Escape a value for use inside a double-quoted CSS attribute selector.
 * @param {string} s
 * @returns {string}
 */
function cssEscapeAttr(s) {
  return String(s).replace(/\\/g, "\\\\").replace(/"/g, '\\"');
}
