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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useCallback, useEffect, useId, useState } from "react";
import {
  formatApiError,
  isSessionRedirectError,
} from "../api/client";
import {
  type CurrentUserProfile,
  getCurrentUserProfile,
} from "../api/user/userProfileApi";
import {
  changeMyPassword,
  MIN_PASSWORD_LENGTH,
  validatePasswordChange,
} from "../api/user/userPasswordApi";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { PROFILE_MSG } from "./messages";
import styles from "./SecuritySection.module.css";

export interface SecuritySectionProps {
  /** Optional test double — defaults to live REST. */
  loadProfile?: () => Promise<CurrentUserProfile>;
  changePassword?: (input: {
    name: string;
    password: string;
    email?: string;
    roles?: string[];
  }) => Promise<void>;
}

type LoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; profile: CurrentUserProfile };

/**
 * Profile Security section (#2394 / parent #2374 slice 3).
 *
 * <p>INTERNAL users get a change-password form that reuses PUT
 * {@code /user/user/changepw} (self-only). DIRECTORY/SSO users see a
 * localized non-dead-end explanation — no always-fail form.</p>
 */
export function SecuritySection({
  loadProfile = getCurrentUserProfile,
  changePassword = changeMyPassword,
}: SecuritySectionProps = {}): React.ReactElement {
  const reactId = useId();
  const formId = `perc-profile-security-form-${reactId}`;
  const newPasswordId = `perc-profile-security-new-${reactId}`;
  const confirmPasswordId = `perc-profile-security-confirm-${reactId}`;
  const newErrorId = `perc-profile-security-new-error-${reactId}`;
  const confirmErrorId = `perc-profile-security-confirm-error-${reactId}`;
  const newHelpId = `perc-profile-security-new-help-${reactId}`;
  const statusId = `perc-profile-security-status-${reactId}`;

  const [loadState, setLoadState] = useState<LoadState>({ status: "loading" });
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [newFieldError, setNewFieldError] = useState<string | null>(null);
  const [confirmFieldError, setConfirmFieldError] = useState<string | null>(
    null,
  );
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const load = useCallback(async () => {
    setLoadState({ status: "loading" });
    setNewFieldError(null);
    setConfirmFieldError(null);
    setFormError(null);
    setSuccessMessage(null);
    setNewPassword("");
    setConfirmPassword("");
    try {
      const profile = await loadProfile();
      setLoadState({ status: "ready", profile });
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setLoadState({
        status: "error",
        message: formatApiError(err, message(PROFILE_MSG.SECURITY_LOAD_ERROR)),
      });
    }
  }, [loadProfile]);

  useEffect(() => {
    void load();
  }, [load]);

  const clearFieldMessages = () => {
    setNewFieldError(null);
    setConfirmFieldError(null);
    setFormError(null);
    setSuccessMessage(null);
  };

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (loadState.status !== "ready") {
      return;
    }
    const { profile } = loadState;
    if (profile.providerType !== "INTERNAL") {
      return;
    }

    clearFieldMessages();

    const validation = validatePasswordChange(newPassword, confirmPassword, {
      required: message(PROFILE_MSG.PW_REQUIRED),
      tooShort: message(PROFILE_MSG.PW_TOO_SHORT).replace(
        "{0}",
        String(MIN_PASSWORD_LENGTH),
      ),
      mismatch: message(PROFILE_MSG.PW_MISMATCH),
    });

    if (!validation.ok) {
      setNewFieldError(validation.fields.newPassword ?? null);
      setConfirmFieldError(validation.fields.confirmPassword ?? null);
      return;
    }

    setIsSaving(true);
    try {
      await changePassword({
        name: profile.name,
        password: newPassword,
        email: profile.email,
        roles: profile.roles,
      });
      setNewPassword("");
      setConfirmPassword("");
      setSuccessMessage(message(PROFILE_MSG.PW_SAVE_SUCCESS));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFormError(formatApiError(err, message(PROFILE_MSG.PW_SAVE_ERROR)));
    } finally {
      setIsSaving(false);
    }
  };

  if (loadState.status === "loading") {
    return (
      <div
        className={styles.root}
        data-testid="perc-profile-security"
        aria-busy="true"
      >
        <p
          className={styles.muted}
          data-testid="perc-profile-security-loading"
          {...i18nKeyAttr(PROFILE_MSG.SECURITY_LOADING)}
        >
          {message(PROFILE_MSG.SECURITY_LOADING)}
        </p>
      </div>
    );
  }

  if (loadState.status === "error") {
    return (
      <div className={styles.root} data-testid="perc-profile-security">
        <div
          className={styles.errorBox}
          role="alert"
          data-testid="perc-profile-security-load-error"
        >
          <p {...i18nKeyAttr(PROFILE_MSG.SECURITY_LOAD_ERROR)}>
            {loadState.message || message(PROFILE_MSG.SECURITY_LOAD_ERROR)}
          </p>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => void load()}
            data-testid="perc-profile-security-retry"
            {...i18nKeyAttr(PROFILE_MSG.SECURITY_RETRY)}
          >
            {message(PROFILE_MSG.SECURITY_RETRY)}
          </button>
        </div>
      </div>
    );
  }

  const { profile } = loadState;
  const passwordChangeable = profile.providerType === "INTERNAL";

  if (!passwordChangeable) {
    return (
      <div className={styles.root} data-testid="perc-profile-security">
        <div
          className={styles.externalBox}
          data-testid="perc-profile-security-external"
          role="status"
        >
          <p
            data-testid="perc-profile-security-external-title"
            {...i18nKeyAttr(PROFILE_MSG.PW_EXTERNAL_TITLE)}
          >
            {message(PROFILE_MSG.PW_EXTERNAL_TITLE)}
          </p>
          <p
            data-testid="perc-profile-security-external-body"
            {...i18nKeyAttr(PROFILE_MSG.PW_EXTERNAL_BODY)}
          >
            {message(PROFILE_MSG.PW_EXTERNAL_BODY)}
          </p>
        </div>
      </div>
    );
  }

  const newDescribedBy = [
    newHelpId,
    newFieldError ? newErrorId : null,
  ]
    .filter(Boolean)
    .join(" ");
  const confirmDescribedBy = confirmFieldError ? confirmErrorId : undefined;

  return (
    <div className={styles.root} data-testid="perc-profile-security">
      <form
        id={formId}
        className={styles.form}
        onSubmit={(e) => void onSubmit(e)}
        noValidate
        data-testid="perc-profile-security-form"
      >
        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor={newPasswordId}
            {...i18nKeyAttr(PROFILE_MSG.PW_NEW_LABEL)}
          >
            {message(PROFILE_MSG.PW_NEW_LABEL)}
          </label>
          <input
            id={newPasswordId}
            name="newPassword"
            type="password"
            autoComplete="new-password"
            className={styles.input}
            value={newPassword}
            onChange={(e) => {
              setNewPassword(e.target.value);
              clearFieldMessages();
            }}
            disabled={isSaving}
            aria-invalid={newFieldError ? true : undefined}
            aria-describedby={newDescribedBy || undefined}
            data-testid="perc-profile-security-new-password"
          />
          <p
            id={newHelpId}
            className={styles.hint}
            {...i18nKeyAttr(PROFILE_MSG.PW_LENGTH_HINT)}
          >
            {message(PROFILE_MSG.PW_LENGTH_HINT).replace(
              "{0}",
              String(MIN_PASSWORD_LENGTH),
            )}
          </p>
          {newFieldError && (
            <p
              id={newErrorId}
              className={styles.fieldError}
              role="alert"
              data-testid="perc-profile-security-new-error"
            >
              {newFieldError}
            </p>
          )}
        </div>

        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor={confirmPasswordId}
            {...i18nKeyAttr(PROFILE_MSG.PW_CONFIRM_LABEL)}
          >
            {message(PROFILE_MSG.PW_CONFIRM_LABEL)}
          </label>
          <input
            id={confirmPasswordId}
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            className={styles.input}
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              clearFieldMessages();
            }}
            disabled={isSaving}
            aria-invalid={confirmFieldError ? true : undefined}
            aria-describedby={confirmDescribedBy}
            data-testid="perc-profile-security-confirm-password"
          />
          {confirmFieldError && (
            <p
              id={confirmErrorId}
              className={styles.fieldError}
              role="alert"
              data-testid="perc-profile-security-confirm-error"
            >
              {confirmFieldError}
            </p>
          )}
        </div>

        <div className={styles.actions}>
          <button
            type="submit"
            className={styles.primaryButton}
            disabled={isSaving}
            data-testid="perc-profile-security-submit"
            {...i18nKeyAttr(
              isSaving ? PROFILE_MSG.PW_SAVING : PROFILE_MSG.PW_SUBMIT,
            )}
          >
            {message(isSaving ? PROFILE_MSG.PW_SAVING : PROFILE_MSG.PW_SUBMIT)}
          </button>
        </div>
      </form>

      <div
        id={statusId}
        className={styles.statusRegion}
        role="status"
        aria-live="polite"
        data-testid="perc-profile-security-status"
      >
        {successMessage && (
          <p
            className={styles.success}
            data-testid="perc-profile-security-success"
          >
            {successMessage}
          </p>
        )}
        {formError && (
          <p
            className={styles.formError}
            data-testid="perc-profile-security-form-error"
          >
            {formError}
          </p>
        )}
      </div>
    </div>
  );
}
