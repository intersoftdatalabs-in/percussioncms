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

import React, { useState } from "react";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import {
  AuthClientError,
  postAuthJson,
  resolveAuthEndpoint,
  resolveLoginUrl,
  safeRedirectUrl,
} from "./authClient";
import { AUTH_MSG } from "./messages";
import type { AdminRecoveryRedeemResponse, AuthBootstrap } from "./types";
import styles from "./AuthApp.module.css";

export interface AdminRecoveryPageProps {
  bootstrap: AuthBootstrap;
}

/**
 * Scanner-safe Administrator break-glass MFA recovery redemption page
 * ({@code GET /auth/admin-recovery?token=...}, executes on explicit {@code POST}).
 */
export function AdminRecoveryPage({
  bootstrap,
}: AdminRecoveryPageProps): React.ReactElement {
  const token = (bootstrap.token ?? "").trim();
  const defaultLoginHref = resolveLoginUrl(bootstrap);

  const [submitting, setSubmitting] = useState(false);
  const [redeemedMsg, setRedeemedMsg] = useState<string | null>(null);
  const [loginHref, setLoginHref] = useState(defaultLoginHref);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const onConfirm = async (): Promise<void> => {
    if (!token) {
      return;
    }
    setSubmitting(true);
    setErrorMsg(null);
    try {
      const res = await postAuthJson<AdminRecoveryRedeemResponse>(
        resolveAuthEndpoint(bootstrap, "/admin-recovery"),
        { token },
        bootstrap,
      );
      if (res.redeemed) {
        setLoginHref(safeRedirectUrl(res.loginUrl, defaultLoginHref));
        setRedeemedMsg(res.message);
      } else {
        setErrorMsg(res.message || message(AUTH_MSG.ADMIN_REC_GENERIC_ERROR));
      }
    } catch (err) {
      const msg =
        err instanceof AuthClientError
          ? err.message
          : message(AUTH_MSG.ADMIN_REC_GENERIC_ERROR);
      setErrorMsg(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div data-testid="perc-auth-admin-recovery-page">
      <h1
        className={styles.title}
        data-testid="perc-auth-admin-recovery-title"
        {...i18nKeyAttr(AUTH_MSG.ADMIN_REC_TITLE)}
      >
        {message(AUTH_MSG.ADMIN_REC_TITLE)}
      </h1>
      <p
        className={styles.subtitle}
        {...i18nKeyAttr(AUTH_MSG.ADMIN_REC_SUBTITLE)}
      >
        {message(AUTH_MSG.ADMIN_REC_SUBTITLE)}
      </p>

      {!token ? (
        <div
          className={styles.error}
          role="alert"
          data-testid="perc-auth-admin-recovery-missing-token"
          {...i18nKeyAttr(AUTH_MSG.ADMIN_REC_MISSING_TOKEN)}
        >
          {message(AUTH_MSG.ADMIN_REC_MISSING_TOKEN)}
        </div>
      ) : redeemedMsg ? (
        <div
          className={styles.success}
          role="status"
          data-testid="perc-auth-admin-recovery-success"
        >
          {redeemedMsg}
        </div>
      ) : (
        <button
          type="button"
          className={styles.submit}
          disabled={submitting}
          onClick={() => void onConfirm()}
          data-testid="perc-auth-admin-recovery-confirm"
          {...i18nKeyAttr(
            submitting
              ? AUTH_MSG.ADMIN_REC_CONFIRMING
              : AUTH_MSG.ADMIN_REC_CONFIRM_BTN,
          )}
        >
          {message(
            submitting
              ? AUTH_MSG.ADMIN_REC_CONFIRMING
              : AUTH_MSG.ADMIN_REC_CONFIRM_BTN,
          )}
        </button>
      )}

      {errorMsg && (
        <div
          className={styles.error}
          role="alert"
          data-testid="perc-auth-admin-recovery-error"
        >
          {errorMsg}
        </div>
      )}

      <div className={styles.footerLinks}>
        <a
          href={loginHref}
          className={styles.link}
          data-testid="perc-auth-back-to-login"
          {...i18nKeyAttr(AUTH_MSG.BACK_TO_SIGN_IN)}
        >
          {message(AUTH_MSG.BACK_TO_SIGN_IN)}
        </a>
      </div>
    </div>
  );
}
