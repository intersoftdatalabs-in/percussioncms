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

import React, { useCallback, useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
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
import {
  copyTextToClipboard,
  downloadRecoveryCodesTxt,
} from "./recoveryCodeHelpers";
import type {
  AuthBootstrap,
  AuthCancelResponse,
  TotpSetupConfirmResponse,
  TotpSetupStartResponse,
} from "./types";
import styles from "./AuthApp.module.css";

export interface MfaSetupPageProps {
  bootstrap: AuthBootstrap;
  onNavigate?: (url: string) => void;
}

interface RecoveryCodesViewProps {
  codes: readonly string[];
  copiedCodes: boolean;
  onCopyCodes: () => void;
  onDownloadCodes: () => void;
  onContinue: () => void;
}

function RecoveryCodesStep({
  codes,
  copiedCodes,
  onCopyCodes,
  onDownloadCodes,
  onContinue,
}: RecoveryCodesViewProps): React.ReactElement {
  return (
    <div>
      <h2
        className={styles.sectionTitle}
        {...i18nKeyAttr(AUTH_MSG.MFA_SETUP_RECOVERY_TITLE)}
      >
        {message(AUTH_MSG.MFA_SETUP_RECOVERY_TITLE)}
      </h2>
      <p
        className={styles.subtitle}
        {...i18nKeyAttr(AUTH_MSG.MFA_SETUP_RECOVERY_BODY)}
      >
        {message(AUTH_MSG.MFA_SETUP_RECOVERY_BODY)}
      </p>
      <ul
        className={styles.recoveryList}
        data-testid="perc-auth-mfa-recovery-codes"
      >
        {codes.map((entry) => (
          <li key={entry} className={styles.recoveryItem}>
            {entry}
          </li>
        ))}
      </ul>
      <div className={styles.secondaryActions}>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={onCopyCodes}
          data-testid="perc-auth-mfa-copy-codes"
        >
          {copiedCodes
            ? message(AUTH_MSG.MFA_SETUP_COPIED)
            : message(AUTH_MSG.MFA_SETUP_COPY_CODES)}
        </button>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={onDownloadCodes}
          data-testid="perc-auth-mfa-download-codes"
        >
          {message(AUTH_MSG.MFA_SETUP_DOWNLOAD_CODES)}
        </button>
      </div>
      <button
        type="button"
        className={styles.submit}
        onClick={onContinue}
        data-testid="perc-auth-mfa-continue"
        {...i18nKeyAttr(AUTH_MSG.MFA_SETUP_CONTINUE)}
      >
        {message(AUTH_MSG.MFA_SETUP_CONTINUE)}
      </button>
    </div>
  );
}

/**
 * Unauthenticated mandatory MFA enrollment page ({@code GET /auth/setup}).
 */
export function MfaSetupPage({
  bootstrap,
  onNavigate = defaultNavigate,
}: MfaSetupPageProps): React.ReactElement {
  const hasValidTx = Boolean(bootstrap.mfaState?.hasValidTx);
  const [startData, setStartData] = useState<TotpSetupStartResponse | null>(null);
  const [confirmData, setConfirmData] =
    useState<TotpSetupConfirmResponse | null>(null);
  const [code, setCode] = useState("");
  const [loadingStart, setLoadingStart] = useState(hasValidTx);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [copiedKey, setCopiedKey] = useState(false);
  const [copiedCodes, setCopiedCodes] = useState(false);

  const loginHref = resolveLoginUrl(bootstrap);
  const defaultAppHref = resolveDefaultAppUrl(bootstrap);

  const startSetup = useCallback(async (): Promise<void> => {
    setLoadingStart(true);
    setErrorMsg(null);
    try {
      const res = await postAuthJson<TotpSetupStartResponse>(
        resolveAuthEndpoint(bootstrap, "/setup/totp/start"),
        { label: "Authenticator" },
        bootstrap,
      );
      setStartData(res);
    } catch (err) {
      const msg =
        err instanceof AuthClientError
          ? err.message
          : message(AUTH_MSG.MFA_SETUP_EXPIRED);
      setErrorMsg(msg);
    } finally {
      setLoadingStart(false);
    }
  }, [bootstrap]);

  useEffect(() => {
    if (hasValidTx) {
      void startSetup();
    }
  }, [hasValidTx, startSetup]);

  const onConfirmSubmit = async (event: React.FormEvent): Promise<void> => {
    event.preventDefault();
    if (!startData) {
      return;
    }
    const cleanCode = code.trim();
    setErrorMsg(null);
    if (!cleanCode) {
      setErrorMsg(message(AUTH_MSG.MFA_CHALLENGE_CODE_REQUIRED));
      return;
    }

    setSubmitting(true);
    try {
      const res = await postAuthJson<TotpSetupConfirmResponse>(
        resolveAuthEndpoint(bootstrap, "/setup/totp/confirm"),
        { totpId: startData.totpId, code: cleanCode },
        bootstrap,
      );
      if (res.enrolled) {
        setConfirmData(res);
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
      <div data-testid="perc-auth-mfa-setup-page">
        <h1 className={styles.title} {...i18nKeyAttr(AUTH_MSG.MFA_SETUP_TITLE)}>
          {message(AUTH_MSG.MFA_SETUP_TITLE)}
        </h1>
        <div
          className={styles.error}
          role="alert"
          data-testid="perc-auth-mfa-expired"
          {...i18nKeyAttr(AUTH_MSG.MFA_SETUP_EXPIRED)}
        >
          {message(AUTH_MSG.MFA_SETUP_EXPIRED)}
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

  return (
    <div data-testid="perc-auth-mfa-setup-page">
      <h1 className={styles.title} {...i18nKeyAttr(AUTH_MSG.MFA_SETUP_TITLE)}>
        {message(AUTH_MSG.MFA_SETUP_TITLE)}
      </h1>

      {confirmData ? (
        <RecoveryCodesStep
          codes={confirmData.recoveryCodes}
          copiedCodes={copiedCodes}
          onCopyCodes={() => {
            void copyTextToClipboard(confirmData.recoveryCodes.join("\n")).then(
              (ok) => setCopiedCodes(ok),
            );
          }}
          onDownloadCodes={() => {
            downloadRecoveryCodesTxt(confirmData.recoveryCodes);
          }}
          onContinue={() =>
            onNavigate(safeRedirectUrl(confirmData.redirectUrl, defaultAppHref))
          }
        />
      ) : (
        <>
          <p
            className={styles.subtitle}
            {...i18nKeyAttr(AUTH_MSG.MFA_SETUP_SUBTITLE)}
          >
            {message(AUTH_MSG.MFA_SETUP_SUBTITLE)}
          </p>

          {loadingStart && (
            <p className={styles.subtitle}>{message(AUTH_MSG.MFA_SETUP_LOADING)}</p>
          )}

          {!loadingStart && !startData && (
            <button
              type="button"
              className={styles.submit}
              onClick={() => void startSetup()}
              data-testid="perc-auth-mfa-setup-retry"
            >
              {message(AUTH_MSG.MFA_SETUP_RETRY)}
            </button>
          )}

          {startData && (
            <>
              <div className={styles.qrBox} data-testid="perc-auth-mfa-qr">
                <QRCodeSVG value={startData.otpauthUri} size={192} />
              </div>

              <div className={styles.secretRow}>
                <span className={styles.label}>
                  {message(AUTH_MSG.MFA_SETUP_MANUAL_LABEL)}
                </span>
                <code
                  className={styles.secretCode}
                  data-testid="perc-auth-mfa-manual-secret"
                >
                  {startData.manualSecret}
                </code>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => {
                    void copyTextToClipboard(startData.manualSecret).then((ok) =>
                      setCopiedKey(ok),
                    );
                  }}
                >
                  {copiedKey
                    ? message(AUTH_MSG.MFA_SETUP_COPIED)
                    : message(AUTH_MSG.MFA_SETUP_COPY_KEY)}
                </button>
              </div>

              <form onSubmit={(e) => void onConfirmSubmit(e)} noValidate>
                <div className={styles.formGroup}>
                  <label
                    className={styles.label}
                    htmlFor="perc-auth-mfa-setup-code"
                    {...i18nKeyAttr(AUTH_MSG.MFA_SETUP_CODE_LABEL)}
                  >
                    {message(AUTH_MSG.MFA_SETUP_CODE_LABEL)}
                  </label>
                  <input
                    id="perc-auth-mfa-setup-code"
                    type="text"
                    name="code"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    placeholder="000000"
                    className={styles.input}
                    value={code}
                    onChange={(e) => {
                      setCode(e.target.value);
                      setErrorMsg(null);
                    }}
                    disabled={submitting}
                    data-testid="perc-auth-mfa-setup-code"
                  />
                </div>
                <button
                  type="submit"
                  className={styles.submit}
                  disabled={submitting}
                  data-testid="perc-auth-mfa-setup-confirm"
                >
                  {message(
                    submitting
                      ? AUTH_MSG.MFA_SETUP_CONFIRMING
                      : AUTH_MSG.MFA_SETUP_CONFIRM,
                  )}
                </button>
              </form>
            </>
          )}

          {errorMsg && (
            <div
              className={styles.error}
              role="alert"
              data-testid="perc-auth-mfa-error"
            >
              {errorMsg}
            </div>
          )}

          <div className={styles.footerLinks}>
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
        </>
      )}
    </div>
  );
}
