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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useState } from "react";
import { BrandBar, BrandFooter } from "../ui-themes/components";
import { ThemeProvider } from "../ui-themes/ThemeProvider";
import { useTheme } from "../ui-themes/ThemeProvider";
import { LOGIN_KEYS, t } from "./i18n";
import { localeLabel } from "./localeLabels";
import { ensureTmxLoaded } from "./tmxLoader";
import type { LocaleFormatBootstrap, LoginBootstrap } from "./types";
import styles from "./LoginPage.module.css";

const SELECT_UI_STORAGE_KEY = "perc-login-select-ui-checked";

/** Well-known RTL primary language subtags (bootstrap may refine for selected locale). */
const RTL_PRIMARY = new Set(["ar", "he", "fa", "ur"]);

function textDirForLocale(
  code: string,
  bootstrapFormat?: LocaleFormatBootstrap | null,
): string {
  if (
    bootstrapFormat?.languageString === code &&
    bootstrapFormat.textDir
  ) {
    return bootstrapFormat.textDir;
  }
  const primary = code.split("-")[0]?.toLowerCase() ?? "";
  return RTL_PRIMARY.has(primary) ? "rtl" : "ltr";
}

function applyDocumentLocale(
  code: string,
  bootstrapFormat?: LocaleFormatBootstrap | null,
): void {
  document.documentElement.lang = code;
  document.documentElement.dir = textDirForLocale(code, bootstrapFormat);
}

export interface LoginPageProps {
  bootstrap: LoginBootstrap;
}

function LoginForm({ bootstrap }: LoginPageProps): React.ReactElement {
  const theme = useTheme();
  const [username, setUsername] = useState(bootstrap.username ?? "");
  const [password, setPassword] = useState("");
  const [locale, setLocale] = useState(
    bootstrap.selectedLocale || bootstrap.locales[0]?.name || "en-us",
  );
  const [selectUi, setSelectUi] = useState(false);
  const [tmxReady, setTmxReady] = useState(0);

  useEffect(() => {
    try {
      setSelectUi(localStorage.getItem(SELECT_UI_STORAGE_KEY) === "true");
    } catch {
      // private mode / blocked storage — ignore
    }
  }, []);

  useEffect(() => {
    const el = document.getElementById("perc-login-username");
    if (el instanceof HTMLInputElement) {
      el.focus();
    }
  }, []);

  const onSelectUiChange = (checked: boolean): void => {
    setSelectUi(checked);
    try {
      localStorage.setItem(SELECT_UI_STORAGE_KEY, String(checked));
    } catch {
      // ignore
    }
  };

  const onLocaleChange = (next: string): void => {
    setLocale(next);
    applyDocumentLocale(next, bootstrap.localeFormat);
    ensureTmxLoaded(next)
      .then(() => {
        document.documentElement.lang = next;
        // Bump so labels re-read window.I18N after the new TMX bundle loads.
        setTmxReady((n) => n + 1);
      })
      .catch(() => {
        // Bundle unavailable; t() resolves to English fallback text after @.
      });
  };

  // Apply server-resolved format (dir, lang) on mount and when locale changes.
  useEffect(() => {
    applyDocumentLocale(locale, bootstrap.localeFormat);
  }, [locale, bootstrap.localeFormat]);

  // Keep the browser tab title in sync with the selected locale chrome.
  useEffect(() => {
    document.title = `${t(LOGIN_KEYS.TITLE)} — Percussion CMS`;
  }, [locale, tmxReady]);

  return (
    <div
      className={styles.page}
      data-testid="perc-login-page"
      data-tmx-ready={tmxReady}
      data-text-dir={
        bootstrap.localeFormat?.languageString === locale
          ? bootstrap.localeFormat?.textDir ?? "ltr"
          : textDirForLocale(locale)
      }
    >
      <BrandBar />
      <main className={styles.main}>
        <div className={styles.card}>
          <div className={styles.logoWrap}>
            <img
              className={styles.logo}
              src={theme.brand.logoHorizontal}
              alt={`${theme.brand.publisher} logo`}
              width={220}
              height={48}
              data-testid="perc-login-logo"
            />
          </div>
          <h1 className={styles.title} data-testid="perc-login-title">
            {t(LOGIN_KEYS.TITLE)}
          </h1>
          <p className={styles.subtitle}>{theme.brand.productName}</p>

          {/*
            Native form POST to existing /login servlet (multipart).
            Do not convert to fetch/XHR — session cookie + redirect flow is server-owned.
          */}
          <form
            id="loginform"
            name="loginform"
            method="post"
            encType="multipart/form-data"
            action={bootstrap.formAction || "/login"}
            autoComplete={bootstrap.autocomplete || "on"}
            data-testid="perc-login-form"
          >
            <input
              type="hidden"
              name={bootstrap.csrfTokenName}
              value={bootstrap.csrfTokenValue}
              data-testid="perc-login-csrf"
            />
            <input
              type="hidden"
              name="sys_redirect"
              value={bootstrap.defaultRedirect}
              data-testid="perc-login-redirect"
            />

            <div className={styles.formGroup}>
              <label className={styles.label} htmlFor="perc-login-username">
                {t(LOGIN_KEYS.USERNAME)}
              </label>
              <input
                className={styles.input}
                type="text"
                id="perc-login-username"
                name="j_username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                tabIndex={1}
                autoComplete={
                  bootstrap.autocomplete === "off" ? "off" : "username"
                }
                data-testid="perc-login-username"
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label} htmlFor="perc-login-password">
                {t(LOGIN_KEYS.PASSWORD)}
              </label>
              <input
                className={styles.input}
                type="password"
                id="perc-login-password"
                name="j_password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                tabIndex={2}
                autoComplete={
                  bootstrap.autocomplete === "off" ? "off" : "current-password"
                }
                data-testid="perc-login-password"
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label} htmlFor="perc-login-locale">
                {t(LOGIN_KEYS.LOCALE)}
              </label>
              <select
                className={styles.select}
                id="perc-login-locale"
                name="j_locale"
                value={locale}
                onChange={(e) => onLocaleChange(e.target.value)}
                data-testid="perc-login-locale"
              >
                {bootstrap.locales.map((loc) => (
                  <option key={loc.name} value={loc.name}>
                    {/*
                      Endonym labels are stable across UI locale changes
                      (GH-1608). Second arg is unused; pass option code.
                    */}
                    {localeLabel(loc.name, loc.name, loc.displayName)}
                  </option>
                ))}
              </select>
            </div>

            <div className={styles.checkboxRow}>
              <input
                type="checkbox"
                id="perc-login-select-ui"
                name="j_selectUI"
                checked={selectUi}
                onChange={(e) => onSelectUiChange(e.target.checked)}
                data-testid="perc-login-select-ui"
              />
              <label htmlFor="perc-login-select-ui">
                {t(LOGIN_KEYS.USE_LEGACY)}
              </label>
            </div>

            <button
              type="submit"
              id="perc-login-button"
              className={styles.submit}
              data-testid="perc-login-submit"
            >
              {t(LOGIN_KEYS.SUBMIT)}
            </button>
          </form>

          {bootstrap.error ? (
            <div
              className={styles.error}
              role="alert"
              data-testid="perc-login-error"
            >
              {bootstrap.error}
            </div>
          ) : null}
        </div>
      </main>
      <BrandFooter />
    </div>
  );
}

/**
 * Product front-door login page (React). Posts credentials to the existing
 * {@code /login} servlet — no parallel auth API.
 */
export function LoginPage({ bootstrap }: LoginPageProps): React.ReactElement {
  return (
    <ThemeProvider data-testid="perc-login-theme">
      <LoginForm bootstrap={bootstrap} />
    </ThemeProvider>
  );
}
