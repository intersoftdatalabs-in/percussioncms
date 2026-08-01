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

import React, { useEffect } from "react";
import { BrandBar, BrandFooter } from "../ui-themes/components";
import { ThemeProvider } from "../ui-themes/ThemeProvider";
import { useTheme } from "../ui-themes/ThemeProvider";
// Reuse login card chrome so logout stays pixel-aligned with the front door.
import loginStyles from "../login/LoginPage.module.css";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { ensureMkdLanguage } from "../i18n/mkdLanguage";
import { LOGOUT_KEYS, t } from "./i18n";
import type { LogoutBootstrap } from "./types";
import styles from "./LogoutPage.module.css";

export interface LogoutPageProps {
  bootstrap: LogoutBootstrap;
}

function LogoutBody({ bootstrap }: LogoutPageProps): React.ReactElement {
  const theme = useTheme();

  useEffect(() => {
    if (bootstrap.locale) {
      document.documentElement.lang = bootstrap.locale;
    }
    document.title = `${t(LOGOUT_KEYS.TITLE)} — Percussion CMS`;
  }, [bootstrap.locale]);

  useEffect(() => {
    ensureMkdLanguage({
      locale: () => bootstrap.locale || "en-us",
    });
  }, [bootstrap.locale]);

  return (
    <div className={loginStyles.page} data-testid="perc-logout-page">
      <BrandBar />
      <main className={loginStyles.main}>
        <div className={loginStyles.card}>
          <div className={loginStyles.logoWrap}>
            <img
              className={loginStyles.logo}
              src={theme.brand.logoHorizontal}
              alt={`${theme.brand.publisher} logo`}
              width={220}
              height={48}
              data-testid="perc-logout-logo"
            />
          </div>
          <h1
            className={`${loginStyles.title} mkd-lang-target`}
            data-testid="perc-logout-title"
            {...i18nKeyAttr(LOGOUT_KEYS.TITLE)}
          >
            {t(LOGOUT_KEYS.TITLE)}
          </h1>
          <p className={loginStyles.subtitle}>{theme.brand.productName}</p>
          <p
            className={`${styles.message} mkd-lang-target`}
            data-testid="perc-logout-message"
            {...i18nKeyAttr(LOGOUT_KEYS.MESSAGE)}
          >
            {t(LOGOUT_KEYS.MESSAGE)}
          </p>
          <div className={styles.actions}>
            <a
              className={styles.loginLink}
              href={bootstrap.loginHref}
              data-testid="perc-logout-sign-in"
              {...i18nKeyAttr(LOGOUT_KEYS.SIGN_IN)}
            >
              {t(LOGOUT_KEYS.SIGN_IN)}
            </a>
          </div>
        </div>
      </main>
      <BrandFooter />
    </div>
  );
}

/**
 * Product logout confirmation page (React). Matches login card chrome;
 * server logout endpoint is unchanged — this page is the post-logout UI only.
 */
export function LogoutPage({ bootstrap }: LogoutPageProps): React.ReactElement {
  return (
    <ThemeProvider data-testid="perc-logout-theme">
      <LogoutBody bootstrap={bootstrap} />
    </ThemeProvider>
  );
}
