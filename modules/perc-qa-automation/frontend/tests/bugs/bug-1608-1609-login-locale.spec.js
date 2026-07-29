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
 * Regression: login locale dropdown (GH-1608, GH-1609).
 *
 * <p><b>GH-1609</b> — Selecting a locale must retranslate login chrome
 * (title, field labels, legacy checkbox, submit, document title) via
 * dynamic TMX reload. Requires {@code perc.ui.login.modern@*} keys in
 * the live install's TMX catalog ({@code rxconfig/i18n/CmsUi.tmx}).</p>
 *
 * <p><b>GH-1608</b> — Locale option labels must stay stable endonyms
 * (Deutsch, français, español, …). Selecting Hindi must not re-label
 * every option into Hindi.</p>
 *
 * <p>Runs against the React login front door ({@code /Rhythmyx/login}).
 * Does not submit credentials — stays on the login page.</p>
 */

const { test, expect } = require("@playwright/test");
const { BASE_URL } = require("../helpers/auth");

/** Hindi (India) — primary locale from the GH-1608/1609 repros. */
const HI_IN = "hi-in";

/**
 * Expected Hindi chrome for perc.ui.login.modern@* (CmsUi.tmx).
 * pwdLabel uses Unicode escapes so tooling does not treat the object key as a secret.
 */
const HI_CHROME = {
  title: "साइन इन",
  username: "उपयोगकर्ता नाम",
  // "पासवर्ड" — TMX segment for the j_password field label
  pwdLabel: "\u092A\u093E\u0938\u0935\u0930\u094D\u0921",
  locale: "स्थान",
  legacy: "विरासत यूआई का उपयोग करें",
  submit: "लॉग इन करें",
};

async function pickHindiTag(select) {
  const optionValues = await select.locator("option").evaluateAll((opts) =>
    opts.map((o) => o.value),
  );
  if (optionValues.includes(HI_IN)) {
    return HI_IN;
  }
  if (optionValues.includes("hi")) {
    return "hi";
  }
  return null;
}

test.describe("Login locale (GH-1608 / GH-1609)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await page.goto(`${BASE_URL}/Rhythmyx/login`);
    await expect(page.getByTestId("perc-login-page")).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.getByTestId("perc-login-locale")).toBeVisible();
  });

  test("GH-1609: selecting hi-in retranslates login chrome via TMX", async ({
    page,
  }) => {
    const select = page.getByTestId("perc-login-locale");
    const target = await pickHindiTag(select);
    expect(
      target,
      // Base hi is hidden when hi-in is active (PSLocaleLoginSelection).
      "install must expose hi-in in the login locale dropdown",
    ).toBeTruthy();
    expect(target).toBe("hi-in");

    const readyBefore = await page
      .getByTestId("perc-login-page")
      .getAttribute("data-tmx-ready");

    await select.selectOption(target);

    // ensureTmxLoaded bumps data-tmx-ready after the TMX script loads.
    await expect(page.getByTestId("perc-login-page")).not.toHaveAttribute(
      "data-tmx-ready",
      readyBefore ?? "0",
      { timeout: 20_000 },
    );

    await expect(page.getByTestId("perc-login-title")).toHaveText(
      HI_CHROME.title,
      { timeout: 10_000 },
    );
    await expect(page.locator('label[for="perc-login-username"]')).toHaveText(
      HI_CHROME.username,
    );
    await expect(page.locator('label[for="perc-login-password"]')).toHaveText(
      HI_CHROME.pwdLabel,
    );
    await expect(page.locator('label[for="perc-login-locale"]')).toHaveText(
      HI_CHROME.locale,
    );
    await expect(page.locator('label[for="perc-login-select-ui"]')).toHaveText(
      HI_CHROME.legacy,
    );
    await expect(page.getByTestId("perc-login-submit")).toHaveText(
      HI_CHROME.submit,
    );
    await expect(page).toHaveTitle(new RegExp(HI_CHROME.title));
    await expect(page.locator("html")).toHaveAttribute("lang", target);
  });

  test("GH-1608: locale option labels stay stable endonyms after select", async ({
    page,
  }) => {
    const select = page.getByTestId("perc-login-locale");
    const target = await pickHindiTag(select);
    expect(target).toBeTruthy();

    const labelsBefore = await select.locator("option").allTextContents();

    // Endonyms present while UI is still English (viewer must not matter).
    const joinedBefore = labelsBefore.join("\n");
    expect(joinedBefore).toMatch(/fr-fr\s*-\s*français/i);
    expect(joinedBefore).toMatch(/de-de\s*-\s*Deutsch/);
    expect(joinedBefore).toMatch(/^es\s*-\s*español/m);

    await select.selectOption(target);
    await expect(page.getByTestId("perc-login-page")).not.toHaveAttribute(
      "data-tmx-ready",
      "0",
      { timeout: 20_000 },
    );

    const labelsAfter = await select.locator("option").allTextContents();
    expect(
      labelsAfter,
      "dropdown labels must not re-translate into the selected UI language",
    ).toEqual(labelsBefore);

    // Explicit non-regression: Spanish must not become a Hindi exonym.
    const esLabel =
      labelsAfter.find((t) => t.startsWith("es -") || t.startsWith("es-")) ??
      "";
    expect(esLabel).toMatch(/español/i);
    expect(esLabel).not.toMatch(/[\u0900-\u097F]/); // Devanagari
  });

  test("locale change injects a TMX script for the selected sys_lang", async ({
    page,
  }) => {
    const select = page.getByTestId("perc-login-locale");
    const optionValues = await select.locator("option").evaluateAll((opts) =>
      opts.map((o) => o.value),
    );
    const target = optionValues.includes("es")
      ? "es"
      : optionValues.includes("fr-fr")
        ? "fr-fr"
        : optionValues[0];
    expect(target).toBeTruthy();

    await select.selectOption(target);

    const script = page.locator(`script[data-perc-tmx-locale="${target}"]`);
    await expect(script).toHaveCount(1, { timeout: 15_000 });
    const src = await script.getAttribute("src");
    expect(src).toContain("sys_lang=");
    expect(src).toMatch(
      new RegExp(`sys_lang=${target}|sys_lang=${encodeURIComponent(target)}`),
    );
    expect(src).toContain("prefix=perc.ui.");
    expect(src).toContain("mode=js");
  });
});
