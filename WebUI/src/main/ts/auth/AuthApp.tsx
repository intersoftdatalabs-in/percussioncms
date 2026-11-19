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
import { AdminRecoveryPage } from "./AdminRecoveryPage";
import { ForgotPasswordPage } from "./ForgotPasswordPage";
import { ForgotUsernamePage } from "./ForgotUsernamePage";
import { MfaChallengePage } from "./MfaChallengePage";
import { MfaSetupPage } from "./MfaSetupPage";
import { ResetPasswordPage } from "./ResetPasswordPage";
import { VerifyEmailPage } from "./VerifyEmailPage";
import type { AuthBootstrap, AuthView, MfaBootstrapState } from "./types";
import styles from "./AuthApp.module.css";

const AUTH_BOOTSTRAP_ID = "perc-auth-bootstrap";

const VALID_VIEWS: ReadonlySet<string> = new Set([
  "verify-email",
  "forgot-username",
  "forgot-password",
  "reset-password",
  "admin-recovery",
  "mfa-challenge",
  "mfa-setup",
]);

function resolveAuthView(raw: string): AuthView {
  return VALID_VIEWS.has(raw) ? (raw as AuthView) : "forgot-username";
}

function parseAuxBootstrapState(rawJson: string): {
  mfaState?: MfaBootstrapState;
  passwordMinLength?: number;
  passwordMaxLength?: number;
} {
  if (!rawJson.trim()) {
    return {};
  }
  try {
    const parsed = JSON.parse(rawJson);
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
      const rec = parsed as Record<string, unknown>;
      const mfaState: MfaBootstrapState | undefined =
        "hasValidTx" in rec || "txType" in rec
          ? {
              hasValidTx: Boolean(rec.hasValidTx),
              txType: typeof rec.txType === "string" ? rec.txType : "",
              totpAvailable: Boolean(rec.totpAvailable),
              passkeyAvailable: Boolean(rec.passkeyAvailable),
              recoveryCodeAvailable: Boolean(rec.recoveryCodeAvailable),
            }
          : undefined;
      const passwordMinLength =
        typeof rec.passwordMinLength === "number" &&
        Number.isFinite(rec.passwordMinLength)
          ? rec.passwordMinLength
          : undefined;
      const passwordMaxLength =
        typeof rec.passwordMaxLength === "number" &&
        Number.isFinite(rec.passwordMaxLength)
          ? rec.passwordMaxLength
          : undefined;
      return { mfaState, passwordMinLength, passwordMaxLength };
    }
  } catch {
    console.error("[PercModernUI] Failed to parse #perc-auth-bootstrap mfaStateJson");
  }
  return {};
}

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
  const view: AuthView = resolveAuthView(rawView);
  const csrf = resolveCsrfFromHolder(parsed);
  const rawDir = readStringField(parsed, "textDir", "ltr");
  const aux = parseAuxBootstrapState(readStringField(parsed, "mfaStateJson"));
  const passwordMinLength =
    typeof parsed.passwordMinLength === "number" &&
    Number.isFinite(parsed.passwordMinLength)
      ? parsed.passwordMinLength
      : aux.passwordMinLength;
  const passwordMaxLength =
    typeof parsed.passwordMaxLength === "number" &&
    Number.isFinite(parsed.passwordMaxLength)
      ? parsed.passwordMaxLength
      : aux.passwordMaxLength;

  return {
    view,
    token: readStringField(parsed, "token"),
    authCsrfToken: readStringField(parsed, "authCsrfToken"),
    csrfTokenName: csrf.name,
    csrfTokenValue: csrf.value,
    contextPath: readStringField(parsed, "contextPath"),
    locale: readStringField(parsed, "locale", "en-us"),
    textDir: rawDir === "rtl" ? "rtl" : "ltr",
    mfaState: aux.mfaState,
    passwordMinLength,
    passwordMaxLength,
  };
}

export interface AuthAppProps {
  bootstrap: AuthBootstrap;
}

function renderAuthView(bootstrap: AuthBootstrap): React.ReactElement {
  switch (bootstrap.view) {
    case "verify-email":
      return <VerifyEmailPage bootstrap={bootstrap} />;
    case "forgot-password":
      return <ForgotPasswordPage bootstrap={bootstrap} />;
    case "reset-password":
      return <ResetPasswordPage bootstrap={bootstrap} />;
    case "admin-recovery":
      return <AdminRecoveryPage bootstrap={bootstrap} />;
    case "mfa-challenge":
      return <MfaChallengePage bootstrap={bootstrap} />;
    case "mfa-setup":
      return <MfaSetupPage bootstrap={bootstrap} />;
    default:
      return <ForgotUsernamePage bootstrap={bootstrap} />;
  }
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
          {renderAuthView(bootstrap)}
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
