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
  browserSupportsWebAuthn,
  startAuthentication,
  type AuthenticationResponseJSON,
  type PublicKeyCredentialRequestOptionsJSON,
} from "@simplewebauthn/browser";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import {
  AuthClientError,
  defaultNavigate,
  postAuthJson,
  resolveAuthEndpoint,
  resolveDefaultAppUrl,
  resolveLoginUrl,
  safeRedirectUrl,
} from "./authClient";
import { AUTH_MSG } from "./messages";
import type {
  AuthBootstrap,
  AuthCancelResponse,
  MfaChallengeResponse,
} from "./types";
import styles from "./AuthApp.module.css";

interface PasskeyChallengeOptions {
  challengeTxId: string;
  optionsJson: string;
}

export interface MfaChallengePageProps {
  bootstrap: AuthBootstrap;
  onNavigate?: (url: string) => void;
  runPasskeyAuth?: (
    optionsJSON: PublicKeyCredentialRequestOptionsJSON,
  ) => Promise<AuthenticationResponseJSON>;
  supportsWebAuthn?: boolean;
}

/**
 * Unauthenticated second-factor verification page ({@code GET /auth/challenge}).
 * Supports WebAuthn passkeys, authenticator 6-digit TOTP codes, and single-use recovery codes.
 */
export function MfaChallengePage({
  bootstrap,
  onNavigate = defaultNavigate,
  runPasskeyAuth = (optionsJSON) => startAuthentication({ optionsJSON }),
  supportsWebAuthn = browserSupportsWebAuthn(),
}: MfaChallengePageProps): React.ReactElement {
  const mfaState = bootstrap.mfaState;
  const hasValidTx = Boolean(mfaState?.hasValidTx);
  const recoveryAvailable = mfaState?.recoveryCodeAvailable ?? true;
  const showPasskeyOption = Boolean(mfaState?.passkeyAvailable) && supportsWebAuthn;
  const defaultUseRecovery =
    mfaState?.totpAvailable === false && Boolean(mfaState?.recoveryCodeAvailable);

  const [useRecovery, setUseRecovery] = useState(defaultUseRecovery);
  const [code, setCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [passkeyBusy, setPasskeyBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const loginHref = resolveLoginUrl(bootstrap);
  const defaultAppHref = resolveDefaultAppUrl(bootstrap);

  const onPasskeySignIn = async (): Promise<void> => {
    setErrorMsg(null);
    setPasskeyBusy(true);
    try {
      const opts = await postAuthJson<PasskeyChallengeOptions>(
        resolveAuthEndpoint(bootstrap, "/challenge/passkey/options"),
        {},
        bootstrap,
      );
      const parsedOptions = JSON.parse(
        opts.optionsJson,
      ) as PublicKeyCredentialRequestOptionsJSON;
      const assertion = await runPasskeyAuth(parsedOptions);
      const res = await postAuthJson<MfaChallengeResponse>(
        resolveAuthEndpoint(bootstrap, "/challenge/passkey/verify"),
        {
          challengeTxId: opts.challengeTxId,
          credentialJson: JSON.stringify(assertion),
        },
        bootstrap,
      );
      if (res.authenticated) {
        onNavigate(safeRedirectUrl(res.redirectUrl, defaultAppHref));
      } else {
        setErrorMsg(res.message || message(AUTH_MSG.MFA_CHALLENGE_GENERIC_ERROR));
      }
    } catch (err) {
      const msg =
        err instanceof AuthClientError
          ? err.message
          : message(AUTH_MSG.MFA_PASSKEY_ABORTED);
      setErrorMsg(msg);
    } finally {
      setPasskeyBusy(false);
    }
  };

  const onSubmit = async (event: React.FormEvent): Promise<void> => {
    event.preventDefault();
    const cleanCode = code.trim();
    setErrorMsg(null);
    if (!cleanCode) {
      setErrorMsg(message(AUTH_MSG.MFA_CHALLENGE_CODE_REQUIRED));
      return;
    }

    setSubmitting(true);
    try {
      const subPath = useRecovery ? "/challenge/recovery-code" : "/challenge/totp";
      const res = await postAuthJson<MfaChallengeResponse>(
        resolveAuthEndpoint(bootstrap, subPath),
        { code: cleanCode },
        bootstrap,
      );
      if (res.authenticated) {
        onNavigate(safeRedirectUrl(res.redirectUrl, defaultAppHref));
      } else {
        setErrorMsg(res.message || message(AUTH_MSG.MFA_CHALLENGE_GENERIC_ERROR));
      }
    } catch (err) {
      const msg =
        err instanceof AuthClientError
          ? err.message
          : message(AUTH_MSG.MFA_CHALLENGE_GENERIC_ERROR);
      setErrorMsg(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const onCancel = async (): Promise<void> => {
    setSubmitting(true);
    try {
      const res = await postAuthJson<AuthCancelResponse>(
        resolveAuthEndpoint(bootstrap, "/cancel"),
        {},
        bootstrap,
      );
      onNavigate(safeRedirectUrl(res.loginUrl, loginHref));
    } catch {
      onNavigate(loginHref);
    } finally {
      setSubmitting(false);
    }
  };

  if (!hasValidTx) {
    return (
      <div data-testid="perc-auth-mfa-challenge-page">
        <h1
          className={styles.title}
          {...i18nKeyAttr(AUTH_MSG.MFA_CHALLENGE_TITLE)}
        >
          {message(AUTH_MSG.MFA_CHALLENGE_TITLE)}
        </h1>
        <div
          className={styles.error}
          role="alert"
          data-testid="perc-auth-mfa-expired"
          {...i18nKeyAttr(AUTH_MSG.MFA_CHALLENGE_EXPIRED)}
        >
          {message(AUTH_MSG.MFA_CHALLENGE_EXPIRED)}
        </div>
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

  const subtitleKey = useRecovery
    ? AUTH_MSG.MFA_CHALLENGE_RECOVERY_SUBTITLE
    : AUTH_MSG.MFA_CHALLENGE_SUBTITLE;
  const labelKey = useRecovery
    ? AUTH_MSG.MFA_CHALLENGE_RECOVERY_LABEL
    : AUTH_MSG.MFA_CHALLENGE_TOTP_LABEL;
  const toggleKey = useRecovery
    ? AUTH_MSG.MFA_CHALLENGE_USE_TOTP
    : AUTH_MSG.MFA_CHALLENGE_USE_RECOVERY;

  return (
    <div data-testid="perc-auth-mfa-challenge-page">
      <h1
        className={styles.title}
        {...i18nKeyAttr(AUTH_MSG.MFA_CHALLENGE_TITLE)}
      >
        {message(AUTH_MSG.MFA_CHALLENGE_TITLE)}
      </h1>
      <p className={styles.subtitle} {...i18nKeyAttr(subtitleKey)}>
        {message(subtitleKey)}
      </p>

      {showPasskeyOption && (
        <div className={styles.passkeyWrap}>
          <button
            type="button"
            className={styles.submit}
            disabled={submitting || passkeyBusy}
            onClick={() => void onPasskeySignIn()}
            data-testid="perc-auth-mfa-passkey-btn"
            {...i18nKeyAttr(
              passkeyBusy
                ? AUTH_MSG.MFA_PASSKEY_WAITING
                : AUTH_MSG.MFA_PASSKEY_VERIFY_BTN,
            )}
          >
            {message(
              passkeyBusy
                ? AUTH_MSG.MFA_PASSKEY_WAITING
                : AUTH_MSG.MFA_PASSKEY_VERIFY_BTN,
            )}
          </button>
          <p
            className={styles.divider}
            {...i18nKeyAttr(AUTH_MSG.MFA_PASSKEY_OR_CODE)}
          >
            {message(AUTH_MSG.MFA_PASSKEY_OR_CODE)}
          </p>
        </div>
      )}

      <form onSubmit={(e) => void onSubmit(e)} noValidate>
        <div className={styles.formGroup}>
          <label
            className={styles.label}
            htmlFor="perc-auth-mfa-code-input"
            {...i18nKeyAttr(labelKey)}
          >
            {message(labelKey)}
          </label>
          <input
            id="perc-auth-mfa-code-input"
            type="text"
            name="code"
            inputMode={useRecovery ? "text" : "numeric"}
            autoComplete={useRecovery ? "off" : "one-time-code"}
            placeholder={useRecovery ? "XXXXX-XXXXX" : "000000"}
            className={styles.input}
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              setErrorMsg(null);
            }}
            disabled={submitting || passkeyBusy}
            data-testid="perc-auth-mfa-code-input"
          />
        </div>

        <button
          type="submit"
          className={styles.submit}
          disabled={submitting || passkeyBusy}
          data-testid="perc-auth-mfa-submit"
          {...i18nKeyAttr(
            submitting
              ? AUTH_MSG.MFA_CHALLENGE_SUBMITTING
              : AUTH_MSG.MFA_CHALLENGE_SUBMIT,
          )}
        >
          {message(
            submitting
              ? AUTH_MSG.MFA_CHALLENGE_SUBMITTING
              : AUTH_MSG.MFA_CHALLENGE_SUBMIT,
          )}
        </button>
      </form>

      {errorMsg && (
        <div
          className={styles.error}
          role="alert"
          data-testid="perc-auth-mfa-error"
        >
          {errorMsg}
        </div>
      )}

      <div className={styles.actionRow}>
        {(recoveryAvailable || useRecovery) && (
          <button
            type="button"
            className={styles.linkButton}
            disabled={submitting}
            onClick={() => {
              setUseRecovery((prev) => !prev);
              setCode("");
              setErrorMsg(null);
            }}
            data-testid="perc-auth-mfa-toggle-recovery"
            {...i18nKeyAttr(toggleKey)}
          >
            {message(toggleKey)}
          </button>
        )}
        <button
          type="button"
          className={styles.linkButton}
          disabled={submitting}
          onClick={() => void onCancel()}
          data-testid="perc-auth-mfa-cancel"
          {...i18nKeyAttr(AUTH_MSG.MFA_CANCEL)}
        >
          {message(AUTH_MSG.MFA_CANCEL)}
        </button>
      </div>
    </div>
  );
}
