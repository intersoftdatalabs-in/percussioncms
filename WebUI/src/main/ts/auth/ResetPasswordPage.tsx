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
import {
  MAX_PASSWORD_LENGTH,
  MIN_PASSWORD_LENGTH,
  validatePasswordChange,
} from "../api/user/userPasswordApi";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import {
  AuthClientError,
  postAuthJson,
  resolveAuthEndpoint,
  resolveLoginUrl,
} from "./authClient";
import { AUTH_MSG } from "./messages";
import type { AuthBootstrap, ResetPasswordResponse } from "./types";
import styles from "./AuthApp.module.css";

export interface ResetPasswordPageProps {
  bootstrap: AuthBootstrap;
}

/**
 * Scanner-safe Reset Password page ({@code GET /auth/reset-password?token=...},
 * submits via explicit {@code POST /auth/reset-password}).
 */
export function ResetPasswordPage({
  bootstrap,
}: ResetPasswordPageProps): React.ReactElement {
  const token = (bootstrap.token ?? "").trim();
  const minLength = Math.max(
    8,
    Math.min(
      MAX_PASSWORD_LENGTH,
      bootstrap.passwordMinLength ?? MIN_PASSWORD_LENGTH,
    ),
  );
  const maxLength = bootstrap.passwordMaxLength ?? MAX_PASSWORD_LENGTH;
  const loginHref = resolveLoginUrl(bootstrap);

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [doneMessage, setDoneMessage] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const onSubmit = async (event: React.FormEvent): Promise<void> => {
    event.preventDefault();
    if (!token) {
      return;
    }
    setErrorMsg(null);

    const validation = validatePasswordChange(
      newPassword,
      confirmPassword,
      {
        required: message(AUTH_MSG.RESET_PW_REQUIRED),
        tooShort: message(AUTH_MSG.RESET_PW_TOO_SHORT).replace(
          "{0}",
          String(minLength),
        ),
        tooLong: message(AUTH_MSG.RESET_PW_TOO_LONG).replace(
          "{0}",
          String(maxLength),
        ),
        mismatch: message(AUTH_MSG.RESET_PW_MISMATCH),
      },
      minLength,
      maxLength,
    );
    if (!validation.ok) {
      setErrorMsg(
        validation.fields.newPassword ||
          validation.fields.confirmPassword ||
          message(AUTH_MSG.RESET_PW_GENERIC_ERROR),
      );
      return;
    }

    setSubmitting(true);
    try {
      const res = await postAuthJson<ResetPasswordResponse>(
        resolveAuthEndpoint(bootstrap, "/reset-password"),
        { token, newPassword },
        bootstrap,
      );
      if (res.reset) {
        setNewPassword("");
        setConfirmPassword("");
        setDoneMessage(res.message);
      } else {
        setErrorMsg(res.message || message(AUTH_MSG.RESET_PW_GENERIC_ERROR));
      }
    } catch (err) {
      const msg =
        err instanceof AuthClientError
          ? err.message
          : message(AUTH_MSG.RESET_PW_GENERIC_ERROR);
      setErrorMsg(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
      data-testid="perc-auth-reset-password-page"
    >
      <h1
        className={styles.title}
        // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
        data-testid="perc-auth-reset-password-title"
        {...i18nKeyAttr(AUTH_MSG.RESET_PW_TITLE)}
      >
        {message(AUTH_MSG.RESET_PW_TITLE)}
      </h1>
      <p
        className={styles.subtitle}
        {...i18nKeyAttr(AUTH_MSG.RESET_PW_SUBTITLE)}
      >
        {message(AUTH_MSG.RESET_PW_SUBTITLE)}
      </p>

      {!token ? (
        <div
          className={styles.error}
          role="alert"
          // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
          data-testid="perc-auth-reset-password-missing-token"
          {...i18nKeyAttr(AUTH_MSG.RESET_PW_MISSING_TOKEN)}
        >
          {message(AUTH_MSG.RESET_PW_MISSING_TOKEN)}
        </div>
      ) : doneMessage ? (
        <div
          className={styles.success}
          role="status"
          // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
          data-testid="perc-auth-reset-password-success"
        >
          {doneMessage}
        </div>
      ) : (
        <form onSubmit={(e) => void onSubmit(e)} noValidate>
          <div className={styles.formGroup}>
            <label
              className={styles.label}
              // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
              htmlFor="perc-auth-reset-password-new"
              {...i18nKeyAttr(AUTH_MSG.RESET_PW_NEW_LABEL)}
            >
              {message(AUTH_MSG.RESET_PW_NEW_LABEL)}
            </label>
            <input
              // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
              id="perc-auth-reset-password-new"
              type="password"
              name="newPassword"
              autoComplete="new-password"
              className={styles.input}
              value={newPassword}
              onChange={(e) => {
                setNewPassword(e.target.value);
                setErrorMsg(null);
              }}
              disabled={submitting}
              // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
              data-testid="perc-auth-reset-password-new"
            />
            <p
              className={styles.hint}
              {...i18nKeyAttr(AUTH_MSG.RESET_PW_LENGTH_HINT)}
            >
              {message(AUTH_MSG.RESET_PW_LENGTH_HINT)
                .replace("{0}", String(minLength))
                .replace("{1}", String(maxLength))}
            </p>
          </div>

          <div className={styles.formGroup}>
            <label
              className={styles.label}
              // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
              htmlFor="perc-auth-reset-password-confirm"
              {...i18nKeyAttr(AUTH_MSG.RESET_PW_CONFIRM_LABEL)}
            >
              {message(AUTH_MSG.RESET_PW_CONFIRM_LABEL)}
            </label>
            <input
              // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
              id="perc-auth-reset-password-confirm"
              type="password"
              name="confirmPassword"
              autoComplete="new-password"
              className={styles.input}
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                setErrorMsg(null);
              }}
              disabled={submitting}
              // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
              data-testid="perc-auth-reset-password-confirm"
            />
          </div>

          <button
            type="submit"
            className={styles.submit}
            disabled={submitting}
            // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
            data-testid="perc-auth-reset-password-submit"
            {...i18nKeyAttr(
              submitting
                ? AUTH_MSG.RESET_PW_SUBMITTING
                : AUTH_MSG.RESET_PW_SUBMIT,
            )}
          >
            {message(
              submitting
                ? AUTH_MSG.RESET_PW_SUBMITTING
                : AUTH_MSG.RESET_PW_SUBMIT,
            )}
          </button>
        </form>
      )}

      {errorMsg && (
        <div
          className={styles.error}
          role="alert"
          // mkd-code-review[secrets.heuristic]: adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material.
          data-testid="perc-auth-reset-password-error"
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
