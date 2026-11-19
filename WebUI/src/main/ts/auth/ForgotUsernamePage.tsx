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
import { isValidEmailAddress } from "../api/user/userProfileApi";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { AUTH_MSG } from "./messages";
import {
  AuthClientError,
  postAuthJson,
  resolveAuthEndpoint,
  resolveLoginUrl,
} from "./authClient";
import type { AuthBootstrap, ForgotUsernameResponse } from "./types";
import styles from "./AuthApp.module.css";

export interface ForgotUsernamePageProps {
  bootstrap: AuthBootstrap;
}

/**
 * Enumeration-safe Forgot Username screen backed by {@code POST /auth/forgot-username}.
 */
export function ForgotUsernamePage({
  bootstrap,
}: ForgotUsernamePageProps): React.ReactElement {
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const loginHref = resolveLoginUrl(bootstrap);

  const onSubmit = async (event: React.FormEvent): Promise<void> => {
    event.preventDefault();
    const cleanEmail = email.trim();
    setErrorMsg(null);
    setStatusMsg(null);

    if (!cleanEmail || !isValidEmailAddress(cleanEmail)) {
      setErrorMsg(message(AUTH_MSG.FORGOT_USERNAME_EMAIL_INVALID));
      return;
    }

    setSubmitting(true);
    try {
      const endpoint = resolveAuthEndpoint(bootstrap, "/forgot-username");
      const res = await postAuthJson<ForgotUsernameResponse>(
        endpoint,
        { email: cleanEmail },
        bootstrap,
      );
      setStatusMsg(res.message);
    } catch (err) {
      const msg =
        err instanceof AuthClientError
          ? err.message
          : message(AUTH_MSG.FORGOT_USERNAME_GENERIC_ERROR);
      setErrorMsg(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div data-testid="perc-auth-forgot-username-page">
      <h1
        className={styles.title}
        data-testid="perc-auth-forgot-username-title"
        {...i18nKeyAttr(AUTH_MSG.FORGOT_USERNAME_TITLE)}
      >
        {message(AUTH_MSG.FORGOT_USERNAME_TITLE)}
      </h1>
      <p
        className={styles.subtitle}
        {...i18nKeyAttr(AUTH_MSG.FORGOT_USERNAME_SUBTITLE)}
      >
        {message(AUTH_MSG.FORGOT_USERNAME_SUBTITLE)}
      </p>

      <form onSubmit={(e) => void onSubmit(e)} noValidate>
        <div className={styles.formGroup}>
          <label
            className={styles.label}
            htmlFor="perc-auth-forgot-username-email"
            {...i18nKeyAttr(AUTH_MSG.FORGOT_USERNAME_EMAIL_LABEL)}
          >
            {message(AUTH_MSG.FORGOT_USERNAME_EMAIL_LABEL)}
          </label>
          <input
            id="perc-auth-forgot-username-email"
            type="email"
            name="email"
            autoComplete="email"
            className={styles.input}
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              setErrorMsg(null);
            }}
            disabled={submitting}
            data-testid="perc-auth-forgot-username-email"
          />
        </div>

        <button
          type="submit"
          className={styles.submit}
          disabled={submitting}
          data-testid="perc-auth-forgot-username-submit"
          {...i18nKeyAttr(
            submitting
              ? AUTH_MSG.FORGOT_USERNAME_SUBMITTING
              : AUTH_MSG.FORGOT_USERNAME_SUBMIT,
          )}
        >
          {message(
            submitting
              ? AUTH_MSG.FORGOT_USERNAME_SUBMITTING
              : AUTH_MSG.FORGOT_USERNAME_SUBMIT,
          )}
        </button>
      </form>

      {statusMsg && (
        <div
          className={styles.success}
          role="status"
          data-testid="perc-auth-forgot-username-status"
        >
          {statusMsg}
        </div>
      )}

      {errorMsg && (
        <div
          className={styles.error}
          role="alert"
          data-testid="perc-auth-forgot-username-error"
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
