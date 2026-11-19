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
import {
  startMyPasskeyStepUp,
  verifyMfaStepUp,
  verifyMyPasskeyStepUp,
  type MfaStepUpResponse,
  type PasskeyOptionsResponse,
} from "../api/auth";
import { formatApiError, isSessionRedirectError } from "../api/client";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { PROFILE_MSG } from "./messages";
import styles from "./MfaSecurityPanel.module.css";

export interface InlineStepUpPromptProps {
  testIdPrefix: string;
  passkeyAvailable?: boolean;
  supportsWebAuthn?: boolean;
  verifyStepUp?: (
    code: string,
    recoveryCode?: boolean,
  ) => Promise<MfaStepUpResponse>;
  startPasskeyStepUp?: () => Promise<PasskeyOptionsResponse>;
  verifyPasskeyStepUp?: (
    challengeTxId: string,
    credentialJson: string,
  ) => Promise<MfaStepUpResponse>;
  runPasskeyAuth?: (
    optionsJSON: PublicKeyCredentialRequestOptionsJSON,
  ) => Promise<AuthenticationResponseJSON>;
  onVerified: () => Promise<void> | void;
  onCancel: () => void;
}

/**
 * Reusable inline step-up MFA prompt triggered on HTTP 428 {@code STEP_UP_REQUIRED}.
 */
export function InlineStepUpPrompt({
  testIdPrefix,
  passkeyAvailable = false,
  supportsWebAuthn = browserSupportsWebAuthn(),
  verifyStepUp = verifyMfaStepUp,
  startPasskeyStepUp = startMyPasskeyStepUp,
  verifyPasskeyStepUp = verifyMyPasskeyStepUp,
  runPasskeyAuth = (optionsJSON) => startAuthentication({ optionsJSON }),
  onVerified,
  onCancel,
}: InlineStepUpPromptProps): React.ReactElement {
  const [code, setCode] = useState("");
  const [useRecovery, setUseRecovery] = useState(false);
  const [busy, setBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const onSubmit = async (): Promise<void> => {
    const clean = code.trim();
    setErrorMsg(null);
    if (!clean) {
      setErrorMsg(message(PROFILE_MSG.STEP_UP_CODE_REQUIRED));
      return;
    }
    setBusy(true);
    try {
      const res = await verifyStepUp(clean, useRecovery);
      if (!res.verified) {
        setErrorMsg(res.message || message(PROFILE_MSG.STEP_UP_ERROR));
        return;
      }
      await onVerified();
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setErrorMsg(formatApiError(err, message(PROFILE_MSG.STEP_UP_ERROR)));
    } finally {
      setBusy(false);
    }
  };

  const onPasskeyVerify = async (): Promise<void> => {
    setErrorMsg(null);
    setBusy(true);
    try {
      const opts = await startPasskeyStepUp();
      const parsedOptions = JSON.parse(
        opts.optionsJson,
      ) as PublicKeyCredentialRequestOptionsJSON;
      const assertion = await runPasskeyAuth(parsedOptions);
      const res = await verifyPasskeyStepUp(
        opts.challengeTxId,
        JSON.stringify(assertion),
      );
      if (!res.verified) {
        setErrorMsg(res.message || message(PROFILE_MSG.STEP_UP_ERROR));
        return;
      }
      await onVerified();
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setErrorMsg(formatApiError(err, message(PROFILE_MSG.STEP_UP_ERROR)));
    } finally {
      setBusy(false);
    }
  };

  const subtitleKey = useRecovery
    ? PROFILE_MSG.STEP_UP_SUBTITLE_RECOVERY
    : PROFILE_MSG.STEP_UP_SUBTITLE_TOTP;
  const toggleKey = useRecovery
    ? PROFILE_MSG.STEP_UP_USE_TOTP
    : PROFILE_MSG.STEP_UP_USE_RECOVERY;

  return (
    <div
      className={styles.stepUpBox}
      data-testid={testIdPrefix}
      role="region"
      aria-label={message(PROFILE_MSG.STEP_UP_TITLE)}
    >
      <p
        className={styles.stepUpTitle}
        {...i18nKeyAttr(PROFILE_MSG.STEP_UP_TITLE)}
      >
        {message(PROFILE_MSG.STEP_UP_TITLE)}
      </p>
      <p className={styles.hint} {...i18nKeyAttr(subtitleKey)}>
        {message(subtitleKey)}
      </p>
      <div className={styles.stepUpRow}>
        <input
          type="text"
          className={styles.input}
          inputMode={useRecovery ? "text" : "numeric"}
          autoComplete={useRecovery ? "off" : "one-time-code"}
          placeholder={useRecovery ? "XXXXX-XXXXX" : "000000"}
          value={code}
          disabled={busy}
          onChange={(e) => {
            setCode(e.target.value);
            setErrorMsg(null);
          }}
          data-testid={`${testIdPrefix}-input`}
        />
        <button
          type="button"
          className={styles.primaryButton}
          disabled={busy}
          onClick={() => void onSubmit()}
          data-testid={`${testIdPrefix}-submit`}
          {...i18nKeyAttr(PROFILE_MSG.STEP_UP_SUBMIT)}
        >
          {message(PROFILE_MSG.STEP_UP_SUBMIT)}
        </button>
        <button
          type="button"
          className={styles.secondaryButton}
          disabled={busy}
          onClick={onCancel}
          data-testid={`${testIdPrefix}-cancel`}
          {...i18nKeyAttr(PROFILE_MSG.STEP_UP_CANCEL)}
        >
          {message(PROFILE_MSG.STEP_UP_CANCEL)}
        </button>
      </div>
      <div>
        {passkeyAvailable && supportsWebAuthn && (
          <button
            type="button"
            className={styles.secondaryButton}
            disabled={busy}
            onClick={() => void onPasskeyVerify()}
            data-testid={`${testIdPrefix}-passkey`}
          >
            {message(PROFILE_MSG.STEP_UP_USE_PASSKEY)}
          </button>
        )}
        <button
          type="button"
          className={styles.linkButton}
          disabled={busy}
          onClick={() => {
            setUseRecovery((prev) => !prev);
            setCode("");
            setErrorMsg(null);
          }}
          data-testid={`${testIdPrefix}-toggle`}
          {...i18nKeyAttr(toggleKey)}
        >
          {message(toggleKey)}
        </button>
      </div>
      {errorMsg && (
        <p
          className={styles.errorText}
          role="alert"
          data-testid={`${testIdPrefix}-error`}
        >
          {errorMsg}
        </p>
      )}
    </div>
  );
}
