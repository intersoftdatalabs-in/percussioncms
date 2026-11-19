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

import React, { useEffect } from "react";
import { BrandFooter } from "../ui-themes/components";
import { ThemeProvider, useTheme } from "../ui-themes/ThemeProvider";
import { ForgotUsernamePage } from "./ForgotUsernamePage";
import { VerifyEmailPage } from "./VerifyEmailPage";
import type { AuthBootstrap, AuthView } from "./types";
import styles from "./AuthApp.module.css";

const AUTH_BOOTSTRAP_ID = "perc-auth-bootstrap";

function readStringField(
  rec: Record<string, unknown>,
  key: string,
  fallback = "",
): string {
  const val = rec[key];
  return typeof val === "string" ? val : fallback;
}

function resolveCsrfFromHolder(raw: Record<string, unknown>): {
  name: string;
  value: string;
} {
  let name = readStringField(raw, "csrfTokenName") || "OWASP_CSRFTOKEN";
  let value = readStringField(raw, "csrfTokenValue");
  const holder = document.getElementById("perc-csrf-holder");
  if (!value && holder) {
    name = holder.getAttribute("data-csrf-name") || name;
    value = holder.getAttribute("data-csrf-value") || value;
  }
  return { name, value };
}

export function readAuthBootstrap(): AuthBootstrap {
  let parsed: Record<string, unknown> = {};
  const el = document.getElementById(AUTH_BOOTSTRAP_ID);
  if (el?.textContent) {
    try {
      const obj = JSON.parse(el.textContent);
      if (obj && typeof obj === "object" && !Array.isArray(obj)) {
        parsed = obj as Record<string, unknown>;
      }
    } catch {
      console.error("[PercModernUI] Failed to parse #perc-auth-bootstrap");
    }
  }
  const rawView = readStringField(parsed, "view");
  const view: AuthView =
    rawView === "verify-email" ? "verify-email" : "forgot-username";
  const csrf = resolveCsrfFromHolder(parsed);
  const rawDir = readStringField(parsed, "textDir", "ltr");

  return {
    view,
    token: readStringField(parsed, "token"),
    authCsrfToken: readStringField(parsed, "authCsrfToken"),
    csrfTokenName: csrf.name,
    csrfTokenValue: csrf.value,
    contextPath: readStringField(parsed, "contextPath"),
    locale: readStringField(parsed, "locale", "en-us"),
    textDir: rawDir === "rtl" ? "rtl" : "ltr",
  };
}

export interface AuthAppProps {
  bootstrap: AuthBootstrap;
}

function AuthShell({ bootstrap }: AuthAppProps): React.ReactElement {
  const theme = useTheme();

  useEffect(() => {
    document.documentElement.lang = bootstrap.locale || "en-us";
    document.documentElement.dir = bootstrap.textDir || "ltr";
  }, [bootstrap.locale, bootstrap.textDir]);

  return (
    <div
      className={styles.page}
      data-testid="perc-auth-page"
      data-text-dir={bootstrap.textDir}
    >
      <main className={styles.main}>
        <div className={styles.card}>
          {theme.brand.logoHorizontal ? (
            <div className={styles.logoWrap}>
              <img
                src={theme.brand.logoHorizontal}
                alt={`${theme.brand.publisher} logo`}
                width={220}
                height={48}
                className={styles.logo}
                data-testid="perc-auth-logo"
              />
            </div>
          ) : null}
          {bootstrap.view === "verify-email" ? (
            <VerifyEmailPage bootstrap={bootstrap} />
          ) : (
            <ForgotUsernamePage bootstrap={bootstrap} />
          )}
        </div>
      </main>
      <BrandFooter />
    </div>
  );
}

/** Root component for unauthenticated {@code /auth/*} flows rendered by {@code rxauth.jsp}. */
export function AuthApp({ bootstrap }: AuthAppProps): React.ReactElement {
  return (
    <ThemeProvider data-testid="perc-auth-theme">
      <AuthShell bootstrap={bootstrap} />
    </ThemeProvider>
  );
}
