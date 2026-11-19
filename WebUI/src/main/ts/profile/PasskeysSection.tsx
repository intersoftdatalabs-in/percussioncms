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
import {
  browserSupportsWebAuthn,
  startAuthentication,
  startRegistration,
  type AuthenticationResponseJSON,
  type PublicKeyCredentialCreationOptionsJSON,
  type PublicKeyCredentialRequestOptionsJSON,
  type RegistrationResponseJSON,
} from "@simplewebauthn/browser";
import {
  deleteMyPasskey,
  finishMyPasskeyRegistration,
  isStepUpRequiredError,
  listMyPasskeys,
  renameMyPasskey,
  startMyPasskeyRegistration,
  startMyPasskeyStepUp,
  verifyMfaStepUp,
  verifyMyPasskeyStepUp,
  type MfaActionResult,
  type MfaStepUpResponse,
  type PasskeyListResponse,
  type PasskeyOptionsResponse,
  type PasskeyRegisterFinishResponse,
  type PasskeySummary,
} from "../api/auth";
import { formatApiError, isSessionRedirectError } from "../api/client";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { InlineStepUpPrompt } from "./InlineStepUpPrompt";
import { PROFILE_MSG } from "./messages";
import styles from "./MfaSecurityPanel.module.css";

type PendingPasskeyAction =
  | { kind: "add"; label: string }
  | { kind: "delete"; passkeyId: string }
  | null;

type LoadState =
  | { kind: "loading" }
  | { kind: "unavailable" }
  | { kind: "ready"; data: PasskeyListResponse };

export interface PasskeysSectionProps {
  listPasskeys?: () => Promise<PasskeyListResponse>;
  startPasskeyRegistration?: (label?: string) => Promise<PasskeyOptionsResponse>;
  finishPasskeyRegistration?: (
    challengeTxId: string,
    label: string,
    credentialJson: string,
  ) => Promise<PasskeyRegisterFinishResponse>;
  renamePasskey?: (passkeyId: string, label: string) => Promise<MfaActionResult>;
  deletePasskey?: (passkeyId: string) => Promise<MfaActionResult>;
  verifyStepUp?: (
    code: string,
    recoveryCode?: boolean,
  ) => Promise<MfaStepUpResponse>;
  startPasskeyStepUp?: () => Promise<PasskeyOptionsResponse>;
  verifyPasskeyStepUp?: (
    challengeTxId: string,
    credentialJson: string,
  ) => Promise<MfaStepUpResponse>;
  runPasskeyRegister?: (
    optionsJSON: PublicKeyCredentialCreationOptionsJSON,
  ) => Promise<RegistrationResponseJSON>;
  runPasskeyAuth?: (
    optionsJSON: PublicKeyCredentialRequestOptionsJSON,
  ) => Promise<AuthenticationResponseJSON>;
  supportsWebAuthn?: boolean;
  onMutated?: (recoveryCodes: string[]) => Promise<void> | void;
}

interface PasskeyRowProps {
  item: PasskeySummary;
  busy: boolean;
  disableDelete: boolean;
  onRename: (passkeyId: string, label: string) => Promise<void>;
  onDelete: (passkeyId: string) => Promise<void>;
}

function PasskeyRow({
  item,
  busy,
  disableDelete,
  onRename,
  onDelete,
}: PasskeyRowProps): React.ReactElement {
  const [editing, setEditing] = useState(false);
  const [draftLabel, setDraftLabel] = useState(item.label);

  const saveRename = async (): Promise<void> => {
    await onRename(item.passkeyId, draftLabel);
    setEditing(false);
  };

  return (
    <li
      className={styles.passkeyItem}
      data-testid={`perc-profile-passkey-item-${item.passkeyId}`}
    >
      {editing ? (
        <div className={styles.stepUpRow}>
          <input
            type="text"
            className={styles.input}
            value={draftLabel}
            disabled={busy}
            onChange={(e) => setDraftLabel(e.target.value)}
            data-testid="perc-profile-passkey-rename-input"
          />
          <button
            type="button"
            className={styles.primaryButton}
            disabled={busy}
            onClick={() => void saveRename()}
            data-testid="perc-profile-passkey-rename-save"
          >
            {message(PROFILE_MSG.PASSKEYS_SAVE_NAME)}
          </button>
          <button
            type="button"
            className={styles.secondaryButton}
            disabled={busy}
            onClick={() => {
              setDraftLabel(item.label);
              setEditing(false);
            }}
          >
            {message(PROFILE_MSG.PASSKEYS_CANCEL)}
          </button>
        </div>
      ) : (
        <>
          <div className={styles.passkeyMeta}>
            <span className={styles.passkeyLabel}>{item.label}</span>
            {item.backupState && (
              <span className={styles.badge}>
                {message(PROFILE_MSG.PASSKEYS_SYNCED_BADGE)}
              </span>
            )}
          </div>
          <div className={styles.stepUpRow}>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={busy}
              onClick={() => {
                setDraftLabel(item.label);
                setEditing(true);
              }}
              data-testid={`perc-profile-passkey-rename-${item.passkeyId}`}
            >
              {message(PROFILE_MSG.PASSKEYS_RENAME)}
            </button>
            <button
              type="button"
              className={styles.dangerButton}
              disabled={busy || disableDelete}
              onClick={() => void onDelete(item.passkeyId)}
              data-testid={`perc-profile-passkey-delete-${item.passkeyId}`}
            >
              {message(PROFILE_MSG.PASSKEYS_DELETE)}
            </button>
          </div>
        </>
      )}
    </li>
  );
}

/** Self-service Passkeys inventory, registration, rename, and removal section. */
export function PasskeysSection({
  listPasskeys = listMyPasskeys,
  startPasskeyRegistration = startMyPasskeyRegistration,
  finishPasskeyRegistration = finishMyPasskeyRegistration,
  renamePasskey = renameMyPasskey,
  deletePasskey = deleteMyPasskey,
  verifyStepUp = verifyMfaStepUp,
  startPasskeyStepUp = startMyPasskeyStepUp,
  verifyPasskeyStepUp = verifyMyPasskeyStepUp,
  runPasskeyRegister = (optionsJSON) => startRegistration({ optionsJSON }),
  runPasskeyAuth = (optionsJSON) => startAuthentication({ optionsJSON }),
  supportsWebAuthn = browserSupportsWebAuthn(),
  onMutated,
}: PasskeysSectionProps = {}): React.ReactElement | null {
  const [loadState, setLoadState] = useState<LoadState>({ kind: "loading" });
  const [newLabel, setNewLabel] = useState("");
  const [busy, setBusy] = useState(false);
  const [pendingStepUp, setPendingStepUp] =
    useState<PendingPasskeyAction>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const refreshPasskeys = useCallback(async (): Promise<void> => {
    try {
      const data = await listPasskeys();
      setLoadState({ kind: "ready", data });
    } catch (err) {
      if (!isSessionRedirectError(err)) {
        setLoadState({ kind: "unavailable" });
      }
    }
  }, [listPasskeys]);

  useEffect(() => {
    void refreshPasskeys();
  }, [refreshPasskeys]);

  const executeAddPasskey = async (labelInput: string): Promise<void> => {
    const cleanLabel = labelInput.trim();
    setBusy(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const opts = await startPasskeyRegistration(cleanLabel);
      const parsedOptions = JSON.parse(
        opts.optionsJson,
      ) as PublicKeyCredentialCreationOptionsJSON;
      const registration = await runPasskeyRegister(parsedOptions);
      const res = await finishPasskeyRegistration(
        opts.challengeTxId,
        cleanLabel,
        JSON.stringify(registration),
      );
      setNewLabel("");
      setSuccessMsg(res.message || message(PROFILE_MSG.PASSKEYS_ADDED_SUCCESS));
      await refreshPasskeys();
      if (onMutated) {
        await onMutated(res.recoveryCodes);
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      if (isStepUpRequiredError(err)) {
        setPendingStepUp({ kind: "add", label: cleanLabel });
      } else {
        setErrorMsg(formatApiError(err, message(PROFILE_MSG.PASSKEYS_ABORTED)));
      }
    } finally {
      setBusy(false);
    }
  };

  const executeDeletePasskey = async (passkeyId: string): Promise<void> => {
    setBusy(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await deletePasskey(passkeyId);
      setSuccessMsg(
        res.message || message(PROFILE_MSG.PASSKEYS_DELETED_SUCCESS),
      );
      await refreshPasskeys();
      if (onMutated) {
        await onMutated([]);
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      if (isStepUpRequiredError(err)) {
        setPendingStepUp({ kind: "delete", passkeyId });
      } else {
        setErrorMsg(formatApiError(err, message(PROFILE_MSG.PASSKEYS_ABORTED)));
      }
    } finally {
      setBusy(false);
    }
  };

  const executeRenamePasskey = async (
    passkeyId: string,
    label: string,
  ): Promise<void> => {
    setBusy(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await renamePasskey(passkeyId, label);
      setSuccessMsg(
        res.message || message(PROFILE_MSG.PASSKEYS_RENAMED_SUCCESS),
      );
      await refreshPasskeys();
    } catch (err) {
      if (!isSessionRedirectError(err)) {
        setErrorMsg(formatApiError(err, message(PROFILE_MSG.PASSKEYS_ABORTED)));
      }
    } finally {
      setBusy(false);
    }
  };

  const onStepUpVerified = async (): Promise<void> => {
    const action = pendingStepUp;
    setPendingStepUp(null);
    if (!action) {
      return;
    }
    if (action.kind === "add") {
      await executeAddPasskey(action.label);
    } else if (action.kind === "delete") {
      await executeDeletePasskey(action.passkeyId);
    }
  };

  if (loadState.kind !== "ready") {
    return null;
  }

  const { passkeys, canDeleteLastPasskey } = loadState.data;
  const disableDeleteLast = !canDeleteLastPasskey && passkeys.length <= 1;

  return (
    <div
      className={styles.passkeysSection}
      data-testid="perc-profile-passkeys"
    >
      <h4
        className={styles.passkeysHeading}
        {...i18nKeyAttr(PROFILE_MSG.PASSKEYS_TITLE)}
      >
        {message(PROFILE_MSG.PASSKEYS_TITLE)}
      </h4>
      <p className={styles.hint} {...i18nKeyAttr(PROFILE_MSG.PASSKEYS_SUBTITLE)}>
        {message(PROFILE_MSG.PASSKEYS_SUBTITLE)}
      </p>

      {passkeys.length === 0 ? (
        <p className={styles.muted} data-testid="perc-profile-passkeys-empty">
          {message(PROFILE_MSG.PASSKEYS_EMPTY)}
        </p>
      ) : (
        <ul
          className={styles.passkeysList}
          data-testid="perc-profile-passkeys-list"
        >
          {passkeys.map((item) => (
            <PasskeyRow
              key={item.passkeyId}
              item={item}
              busy={busy}
              disableDelete={disableDeleteLast}
              onRename={executeRenamePasskey}
              onDelete={executeDeletePasskey}
            />
          ))}
        </ul>
      )}

      {passkeys.length > 0 && disableDeleteLast && (
        <p
          className={styles.hint}
          data-testid="perc-profile-passkeys-last-factor-hint"
        >
          {message(PROFILE_MSG.PASSKEYS_LAST_FACTOR_HINT)}
        </p>
      )}

      {pendingStepUp && (
        <InlineStepUpPrompt
          testIdPrefix="perc-profile-passkeys-stepup"
          passkeyAvailable={passkeys.length > 0}
          supportsWebAuthn={supportsWebAuthn}
          verifyStepUp={verifyStepUp}
          startPasskeyStepUp={startPasskeyStepUp}
          verifyPasskeyStepUp={verifyPasskeyStepUp}
          runPasskeyAuth={runPasskeyAuth}
          onVerified={onStepUpVerified}
          onCancel={() => setPendingStepUp(null)}
        />
      )}

      {supportsWebAuthn ? (
        <div className={styles.addPasskeyRow}>
          <input
            type="text"
            className={styles.input}
            placeholder={message(PROFILE_MSG.PASSKEYS_LABEL_PLACEHOLDER)}
            value={newLabel}
            disabled={busy}
            onChange={(e) => setNewLabel(e.target.value)}
            data-testid="perc-profile-passkey-label-input"
          />
          <button
            type="button"
            className={styles.primaryButton}
            disabled={busy}
            onClick={() => void executeAddPasskey(newLabel)}
            data-testid="perc-profile-passkey-add"
          >
            {message(busy ? PROFILE_MSG.PASSKEYS_ADDING : PROFILE_MSG.PASSKEYS_ADD)}
          </button>
        </div>
      ) : (
        <p className={styles.muted}>{message(PROFILE_MSG.PASSKEYS_UNSUPPORTED)}</p>
      )}

      {successMsg && (
        <p
          className={styles.successText}
          role="status"
          data-testid="perc-profile-passkeys-success"
        >
          {successMsg}
        </p>
      )}
      {errorMsg && (
        <p
          className={styles.errorText}
          role="alert"
          data-testid="perc-profile-passkeys-error"
        >
          {errorMsg}
        </p>
      )}
    </div>
  );
}
