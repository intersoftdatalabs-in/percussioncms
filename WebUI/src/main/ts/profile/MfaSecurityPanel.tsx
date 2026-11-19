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
import {
  confirmMyTotpEnrollment,
  disableMyTotp,
  getMyMfaStatus,
  isStepUpRequiredError,
  regenerateMyRecoveryCodes,
  startMyTotpEnrollment,
  verifyMfaStepUp,
  type MfaActionResult,
  type MfaStepUpResponse,
  type ProfileMfaStatus,
  type RecoveryCodesRegenerateResponse,
  type TotpEnrollConfirmResponse,
  type TotpEnrollStartResponse,
} from "../api/auth";
import { formatApiError, isSessionRedirectError } from "../api/client";
import {
  copyTextToClipboard,
  downloadRecoveryCodesTxt,
} from "../auth/recoveryCodeHelpers";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { InlineStepUpPrompt } from "./InlineStepUpPrompt";
import { PROFILE_MSG } from "./messages";
import styles from "./MfaSecurityPanel.module.css";

type PendingStepUpAction = "start" | "disable" | "regen" | null;

type PanelLoadState =
  | { kind: "loading" }
  | { kind: "unavailable" }
  | { kind: "ready"; mfaStatus: ProfileMfaStatus };

export interface MfaSecurityPanelProps {
  loadMfaStatus?: () => Promise<ProfileMfaStatus>;
  verifyStepUp?: (
    code: string,
    recoveryCode?: boolean,
  ) => Promise<MfaStepUpResponse>;
  startTotpEnrollment?: (label?: string) => Promise<TotpEnrollStartResponse>;
  confirmTotpEnrollment?: (
    totpId: string,
    code: string,
  ) => Promise<TotpEnrollConfirmResponse>;
  disableTotp?: () => Promise<MfaActionResult>;
  regenerateRecoveryCodes?: () => Promise<RecoveryCodesRegenerateResponse>;
}

interface TotpEnrollSectionProps {
  enrollStart: TotpEnrollStartResponse;
  busy: boolean;
  onConfirmed: (res: TotpEnrollConfirmResponse) => Promise<void>;
  onCancel: () => void;
  confirmTotpEnrollment: (
    totpId: string,
    code: string,
  ) => Promise<TotpEnrollConfirmResponse>;
  onError: (msg: string) => void;
}

function TotpEnrollSection({
  enrollStart,
  busy,
  onConfirmed,
  onCancel,
  confirmTotpEnrollment,
  onError,
}: TotpEnrollSectionProps): React.ReactElement {
  const [code, setCode] = useState("");
  const [copiedKey, setCopiedKey] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleConfirm = async (): Promise<void> => {
    const clean = code.trim();
    if (!clean) {
      onError(message(PROFILE_MSG.STEP_UP_CODE_REQUIRED));
      return;
    }
    setSubmitting(true);
    try {
      const res = await confirmTotpEnrollment(enrollStart.totpId, clean);
      await onConfirmed(res);
    } catch (err) {
      if (!isSessionRedirectError(err)) {
        onError(formatApiError(err, message(PROFILE_MSG.STEP_UP_ERROR)));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.enrollCard} data-testid="perc-profile-mfa-enroll-card">
      <div className={styles.qrWrap} data-testid="perc-profile-mfa-qr">
        <QRCodeSVG value={enrollStart.otpauthUri} size={176} />
      </div>
      <div className={styles.secretRow}>
        <span className={styles.hint}>
          {message(PROFILE_MSG.MFA_MANUAL_KEY_LABEL)}:
        </span>
        <code
          className={styles.secretCode}
          data-testid="perc-profile-mfa-manual-secret"
        >
          {enrollStart.manualSecret}
        </code>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => {
            void copyTextToClipboard(enrollStart.manualSecret).then((ok) =>
              setCopiedKey(ok),
            );
          }}
        >
          {copiedKey
            ? message(PROFILE_MSG.MFA_COPIED)
            : message(PROFILE_MSG.MFA_COPY_KEY)}
        </button>
      </div>
      <div className={styles.stepUpRow}>
        <input
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          placeholder="000000"
          className={styles.input}
          aria-label={message(PROFILE_MSG.MFA_CODE_LABEL)}
          value={code}
          disabled={busy || submitting}
          onChange={(e) => setCode(e.target.value)}
          data-testid="perc-profile-mfa-enroll-code"
        />
        <button
          type="button"
          className={styles.primaryButton}
          disabled={busy || submitting}
          onClick={() => void handleConfirm()}
          data-testid="perc-profile-mfa-enroll-confirm"
        >
          {message(PROFILE_MSG.MFA_ENROLL_CONFIRM)}
        </button>
        <button
          type="button"
          className={styles.secondaryButton}
          disabled={busy || submitting}
          onClick={onCancel}
        >
          {message(PROFILE_MSG.MFA_ENROLL_CANCEL)}
        </button>
      </div>
    </div>
  );
}

interface RecoveryCodesCardProps {
  codes: readonly string[];
  onDismiss: () => void;
}

function RecoveryCodesCard({
  codes,
  onDismiss,
}: RecoveryCodesCardProps): React.ReactElement {
  const [copied, setCopied] = useState(false);

  return (
    <div className={styles.codesCard}>
      <p className={styles.stepUpTitle}>{message(PROFILE_MSG.MFA_RECOVERY_TITLE)}</p>
      <p className={styles.hint}>{message(PROFILE_MSG.MFA_RECOVERY_BODY)}</p>
      <ul
        className={styles.recoveryGrid}
        data-testid="perc-profile-mfa-recovery-codes"
      >
        {codes.map((entry) => (
          <li key={entry} className={styles.recoveryCode}>
            {entry}
          </li>
        ))}
      </ul>
      <div className={styles.actions}>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => {
            void copyTextToClipboard(codes.join("\n")).then((ok) => setCopied(ok));
          }}
        >
          {copied
            ? message(PROFILE_MSG.MFA_COPIED)
            : message(PROFILE_MSG.MFA_COPY_CODES)}
        </button>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={() => downloadRecoveryCodesTxt(codes)}
        >
          {message(PROFILE_MSG.MFA_DOWNLOAD_CODES)}
        </button>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={onDismiss}
        >
          {message(PROFILE_MSG.MFA_DISMISS_CODES)}
        </button>
      </div>
    </div>
  );
}

/**
 * Self-service TOTP enrollment, recovery code management, and step-up MFA panel.
 */
export function MfaSecurityPanel({
  loadMfaStatus = getMyMfaStatus,
  verifyStepUp = verifyMfaStepUp,
  startTotpEnrollment = startMyTotpEnrollment,
  confirmTotpEnrollment = confirmMyTotpEnrollment,
  disableTotp = disableMyTotp,
  regenerateRecoveryCodes = regenerateMyRecoveryCodes,
}: MfaSecurityPanelProps = {}): React.ReactElement {
  const [loadState, setLoadState] = useState<PanelLoadState>({ kind: "loading" });
  const [enrollStart, setEnrollStart] = useState<TotpEnrollStartResponse | null>(
    null,
  );
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [pendingStepUp, setPendingStepUp] =
    useState<PendingStepUpAction>(null);
  const [busy, setBusy] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const refreshStatus = useCallback(async (): Promise<void> => {
    try {
      const mfaStatus = await loadMfaStatus();
      setLoadState({ kind: "ready", mfaStatus });
    } catch (err) {
      if (!isSessionRedirectError(err)) {
        setLoadState({ kind: "unavailable" });
      }
    }
  }, [loadMfaStatus]);

  useEffect(() => {
    void refreshStatus();
  }, [refreshStatus]);

  const runAction = async (
    actionKind: Exclude<PendingStepUpAction, null>,
    fn: () => Promise<void>,
  ): Promise<void> => {
    setBusy(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await fn();
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      if (isStepUpRequiredError(err)) {
        setPendingStepUp(actionKind);
      } else {
        setErrorMsg(formatApiError(err, message(PROFILE_MSG.MFA_UNAVAILABLE)));
      }
    } finally {
      setBusy(false);
    }
  };

  const executeStartEnroll = (): Promise<void> =>
    runAction("start", async () => {
      const res = await startTotpEnrollment("Authenticator");
      setEnrollStart(res);
      setRecoveryCodes(null);
    });

  const executeDisableTotp = (): Promise<void> =>
    runAction("disable", async () => {
      const res = await disableTotp();
      setSuccessMsg(res.message || message(PROFILE_MSG.MFA_DISABLED_SUCCESS));
      setEnrollStart(null);
      await refreshStatus();
    });

  const executeRegenerate = (): Promise<void> =>
    runAction("regen", async () => {
      const res = await regenerateRecoveryCodes();
      setRecoveryCodes(res.recoveryCodes);
      setSuccessMsg(res.message || null);
      await refreshStatus();
    });

  const onStepUpVerified = async (): Promise<void> => {
    const action = pendingStepUp;
    setPendingStepUp(null);
    if (action === "start") {
      await executeStartEnroll();
    } else if (action === "disable") {
      await executeDisableTotp();
    } else if (action === "regen") {
      await executeRegenerate();
    }
  };

  return (
    <section
      className={styles.panel}
      data-testid="perc-profile-mfa"
      aria-labelledby="perc-profile-mfa-heading"
    >
      <h3
        id="perc-profile-mfa-heading"
        className={styles.heading}
        {...i18nKeyAttr(PROFILE_MSG.MFA_TITLE)}
      >
        {message(PROFILE_MSG.MFA_TITLE)}
      </h3>
      <p className={styles.subtitle} {...i18nKeyAttr(PROFILE_MSG.MFA_SUBTITLE)}>
        {message(PROFILE_MSG.MFA_SUBTITLE)}
      </p>

      {loadState.kind === "loading" && (
        <p className={styles.muted}>{message(PROFILE_MSG.MFA_LOADING)}</p>
      )}

      {loadState.kind === "unavailable" && (
        <div role="status" data-testid="perc-profile-mfa-unavailable">
          <p className={styles.muted}>{message(PROFILE_MSG.MFA_UNAVAILABLE)}</p>
        </div>
      )}

      {loadState.kind === "ready" && (
        <>
          <div className={styles.statusCard} data-testid="perc-profile-mfa-status">
            <ul className={styles.statusList}>
              <li>
                {message(
                  loadState.mfaStatus.totpEnabled
                    ? PROFILE_MSG.MFA_TOTP_ENABLED
                    : PROFILE_MSG.MFA_TOTP_DISABLED,
                )}
              </li>
              <li>
                {message(PROFILE_MSG.MFA_PASSKEYS_COUNT).replace(
                  "{0}",
                  String(loadState.mfaStatus.passkeyCount),
                )}
              </li>
              <li>
                {message(PROFILE_MSG.MFA_CODES_REMAINING).replace(
                  "{0}",
                  String(loadState.mfaStatus.recoveryCodesRemaining),
                )}
              </li>
            </ul>
          </div>

          <div className={styles.actions}>
            {!loadState.mfaStatus.totpEnabled && !enrollStart && (
              <button
                type="button"
                className={styles.primaryButton}
                disabled={busy}
                onClick={() => void executeStartEnroll()}
                data-testid="perc-profile-mfa-enroll-start"
              >
                {message(PROFILE_MSG.MFA_ENROLL_START)}
              </button>
            )}

            {loadState.mfaStatus.totpEnabled && (
              <>
                <button
                  type="button"
                  className={styles.dangerButton}
                  disabled={busy || !loadState.mfaStatus.canDisableTotp}
                  onClick={() => void executeDisableTotp()}
                  data-testid="perc-profile-mfa-disable"
                >
                  {message(PROFILE_MSG.MFA_DISABLE)}
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  disabled={busy}
                  onClick={() => void executeRegenerate()}
                  data-testid="perc-profile-mfa-regen-codes"
                >
                  {message(PROFILE_MSG.MFA_REGEN_CODES)}
                </button>
              </>
            )}
          </div>

          {loadState.mfaStatus.totpEnabled &&
            !loadState.mfaStatus.canDisableTotp && (
              <p
                className={styles.hint}
                data-testid="perc-profile-mfa-last-factor-hint"
              >
                {message(PROFILE_MSG.MFA_LAST_FACTOR_HINT)}
              </p>
            )}

          {pendingStepUp && (
            <InlineStepUpPrompt
              testIdPrefix="perc-profile-mfa-stepup"
              verifyStepUp={verifyStepUp}
              onVerified={onStepUpVerified}
              onCancel={() => setPendingStepUp(null)}
            />
          )}

          {enrollStart && (
            <TotpEnrollSection
              enrollStart={enrollStart}
              busy={busy}
              confirmTotpEnrollment={confirmTotpEnrollment}
              onConfirmed={async (res) => {
                setEnrollStart(null);
                setRecoveryCodes(
                  res.recoveryCodes.length > 0 ? res.recoveryCodes : null,
                );
                setSuccessMsg(
                  res.message || message(PROFILE_MSG.MFA_ENROLLED_SUCCESS),
                );
                await refreshStatus();
              }}
              onCancel={() => setEnrollStart(null)}
              onError={(msg) => setErrorMsg(msg)}
            />
          )}

          {recoveryCodes && recoveryCodes.length > 0 && (
            <RecoveryCodesCard
              codes={recoveryCodes}
              onDismiss={() => setRecoveryCodes(null)}
            />
          )}

          {successMsg && (
            <p
              className={styles.successText}
              role="status"
              data-testid="perc-profile-mfa-success"
            >
              {successMsg}
            </p>
          )}
          {errorMsg && (
            <p
              className={styles.errorText}
              role="alert"
              data-testid="perc-profile-mfa-error"
            >
              {errorMsg}
            </p>
          )}
        </>
      )}
    </section>
  );
}
