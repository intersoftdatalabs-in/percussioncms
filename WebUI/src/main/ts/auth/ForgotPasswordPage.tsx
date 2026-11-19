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
} from "./authClient";
import { AUTH_MSG } from "./messages";
import type { AuthBootstrap, ForgotPasswordResponse } from "./types";
import styles from "./AuthApp.module.css";

export interface ForgotPasswordPageProps {
  bootstrap: AuthBootstrap;
}

/**
 * Enumeration-safe Forgot Password request page backed by {@code POST /auth/forgot-password}.
 */
export function ForgotPasswordPage({
  bootstrap,
}: ForgotPasswordPageProps): React.ReactElement {
  const [identifier, setIdentifier] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const loginHref = resolveLoginUrl(bootstrap);

  const onSubmit = async (event: React.FormEvent): Promise<void> => {
    event.preventDefault();
    const clean = identifier.trim();
    setErrorMsg(null);
    setStatusMsg(null);

    if (!clean) {
      setErrorMsg(message(AUTH_MSG.FORGOT_PW_IDENTIFIER_REQUIRED));
      return;
    }

    setSubmitting(true);
    try {
      const res = await postAuthJson<ForgotPasswordResponse>(
        resolveAuthEndpoint(bootstrap, "/forgot-password"),
        { identifier: clean },
        bootstrap,
      );
      setStatusMsg(res.message);
    } catch (err) {
      const msg =
        err instanceof AuthClientError
          ? err.message
          : message(AUTH_MSG.FORGOT_PW_GENERIC_ERROR);
      setErrorMsg(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
      data-testid="perc-auth-forgot-password-page"
    >
      <h1
        className={styles.title}
        // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
        data-testid="perc-auth-forgot-password-title"
        {...i18nKeyAttr(AUTH_MSG.FORGOT_PW_TITLE)}
      >
        {message(AUTH_MSG.FORGOT_PW_TITLE)}
      </h1>
      <p
        className={styles.subtitle}
        {...i18nKeyAttr(AUTH_MSG.FORGOT_PW_SUBTITLE)}
      >
        {message(AUTH_MSG.FORGOT_PW_SUBTITLE)}
      </p>

      <form onSubmit={(e) => void onSubmit(e)} noValidate>
        <div className={styles.formGroup}>
          <label
            className={styles.label}
            // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
            htmlFor="perc-auth-forgot-password-identifier"
            {...i18nKeyAttr(AUTH_MSG.FORGOT_PW_IDENTIFIER_LABEL)}
          >
            {message(AUTH_MSG.FORGOT_PW_IDENTIFIER_LABEL)}
          </label>
          <input
            // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
            id="perc-auth-forgot-password-identifier"
            type="text"
            name="identifier"
            autoComplete="username"
            className={styles.input}
            value={identifier}
            onChange={(e) => {
              setIdentifier(e.target.value);
              setErrorMsg(null);
            }}
            disabled={submitting}
            // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
            data-testid="perc-auth-forgot-password-identifier"
          />
        </div>

        <button
          type="submit"
          className={styles.submit}
          disabled={submitting}
          // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
          data-testid="perc-auth-forgot-password-submit"
          {...i18nKeyAttr(
            submitting
              ? AUTH_MSG.FORGOT_PW_SUBMITTING
              : AUTH_MSG.FORGOT_PW_SUBMIT,
          )}
        >
          {message(
            submitting
              ? AUTH_MSG.FORGOT_PW_SUBMITTING
              : AUTH_MSG.FORGOT_PW_SUBMIT,
          )}
        </button>
      </form>

      {statusMsg && (
        <div
          className={styles.success}
          role="status"
          // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
          data-testid="perc-auth-forgot-password-status"
        >
          {statusMsg}
        </div>
      )}

      {errorMsg && (
        <div
          className={styles.error}
          role="alert"
          // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
          data-testid="perc-auth-forgot-password-error"
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
