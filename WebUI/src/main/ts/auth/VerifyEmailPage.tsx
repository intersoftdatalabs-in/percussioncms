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
import { AUTH_MSG } from "./messages";
import {
  AuthClientError,
  postAuthJson,
  resolveAuthEndpoint,
  resolveLoginUrl,
} from "./authClient";
import type { AuthBootstrap, VerifyEmailResponse } from "./types";
import styles from "./AuthApp.module.css";

export interface VerifyEmailPageProps {
  bootstrap: AuthBootstrap;
}

/**
 * Scanner-safe email verification confirmation screen.
 * Intentionally never auto-POSTs on mount; user confirmation via button click is required.
 */
export function VerifyEmailPage({
  bootstrap,
}: VerifyEmailPageProps): React.ReactElement {
  const [submitting, setSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const hasToken = bootstrap.token.trim().length > 0;
  const loginHref = resolveLoginUrl(bootstrap);

  const onConfirm = async (): Promise<void> => {
    if (!hasToken || submitting) {
      return;
    }
    setSubmitting(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const endpoint = resolveAuthEndpoint(bootstrap, "/verify-email");
      const res = await postAuthJson<VerifyEmailResponse>(
        endpoint,
        { token: bootstrap.token.trim() },
        bootstrap,
      );
      if (res.verified) {
        setSuccessMsg(res.message);
      } else {
        setErrorMsg(res.message || message(AUTH_MSG.VERIFY_GENERIC_ERROR));
      }
    } catch (err) {
      const msg =
        err instanceof AuthClientError
          ? err.message
          : message(AUTH_MSG.VERIFY_GENERIC_ERROR);
      setErrorMsg(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div data-testid="perc-auth-verify-page">
      <h1
        className={styles.title}
        data-testid="perc-auth-verify-title"
        {...i18nKeyAttr(AUTH_MSG.VERIFY_TITLE)}
      >
        {message(AUTH_MSG.VERIFY_TITLE)}
      </h1>
      <p
        className={styles.subtitle}
        {...i18nKeyAttr(AUTH_MSG.VERIFY_SUBTITLE)}
      >
        {message(AUTH_MSG.VERIFY_SUBTITLE)}
      </p>

      {!hasToken && !successMsg && (
        <div
          className={styles.error}
          role="alert"
          data-testid="perc-auth-verify-missing-token"
          {...i18nKeyAttr(AUTH_MSG.VERIFY_MISSING_TOKEN)}
        >
          {message(AUTH_MSG.VERIFY_MISSING_TOKEN)}
        </div>
      )}

      {hasToken && !successMsg && (
        <button
          type="button"
          className={styles.submit}
          disabled={submitting}
          onClick={() => void onConfirm()}
          data-testid="perc-auth-verify-submit"
          {...i18nKeyAttr(
            submitting ? AUTH_MSG.VERIFY_SUBMITTING : AUTH_MSG.VERIFY_SUBMIT,
          )}
        >
          {message(
            submitting ? AUTH_MSG.VERIFY_SUBMITTING : AUTH_MSG.VERIFY_SUBMIT,
          )}
        </button>
      )}

      {successMsg && (
        <div
          className={styles.success}
          role="status"
          data-testid="perc-auth-verify-success"
        >
          {successMsg}
        </div>
      )}

      {errorMsg && (
        <div
          className={styles.error}
          role="alert"
          data-testid="perc-auth-verify-error"
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
