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
import type { LoginBootstrap } from "./types";
import styles from "./LoginPage.module.css";

const SELECT_UI_STORAGE_KEY = "perc-login-select-ui-checked";

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

  return (
    <div className={styles.page} data-testid="perc-login-page">
      <BrandBar />
      <main className={styles.main}>
        <div className={styles.card}>
          <div className={styles.logoWrap}>
            <img
              className={styles.logo}
              src={theme.brand.logoHorizontal}
              alt={`${theme.brand.publisher} logo`}
              data-testid="perc-login-logo"
            />
          </div>
          <h1 className={styles.title} data-testid="perc-login-title">
            Sign in
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
                User name
              </label>
              <input
                className={styles.input}
                type="text"
                id="perc-login-username"
                name="j_username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                tabIndex={1}
                autoComplete={bootstrap.autocomplete === "off" ? "off" : "username"}
                data-testid="perc-login-username"
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label} htmlFor="perc-login-password">
                Password
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
                Locale
              </label>
              <select
                className={styles.select}
                id="perc-login-locale"
                name="j_locale"
                value={locale}
                onChange={(e) => setLocale(e.target.value)}
                data-testid="perc-login-locale"
              >
                {bootstrap.locales.map((loc) => (
                  <option key={loc.name} value={loc.name}>
                    {loc.displayName}
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
              <label htmlFor="perc-login-select-ui">Use legacy UI</label>
            </div>

            <button
              type="submit"
              id="perc-login-button"
              className={styles.submit}
              data-testid="perc-login-submit"
            >
              Login
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
