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

import React, { useCallback, useEffect, useId, useMemo, useState } from "react";
import {
  formatApiError,
  isSessionRedirectError,
} from "../api/client";
import {
  getCurrentUserBasic,
  isValidEmailAddress,
} from "../api/user/userCurrentApi";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import {
  loadGravatarEmailOverride,
  saveGravatarEmailOverride,
} from "./avatarPrefs";
import { resolveAvatarPresentation } from "./gravatar";
import { PROFILE_MSG } from "./messages";
import { UserAvatar } from "./UserAvatar";
import styles from "./AvatarSection.module.css";

export interface AvatarSectionProps {
  loadPrimaryEmail?: () => Promise<string>;
  loadOverride?: (userName: string) => Promise<string>;
  saveOverride?: (userName: string, email: string) => Promise<string>;
  /** Test override for kill-switch (defaults to bootstrap). */
  allowExternalAvatarFetch?: boolean;
  userName?: string;
  displayName?: string;
}

type LoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | {
      status: "ready";
      primaryEmail: string;
      overrideEmail: string;
    };

/**
 * Profile Avatar section (#2397 / parent #2374 slice 5).
 *
 * <p>Gravatar email override is stored via PreferenceResource. Primary account
 * email comes from GET {@code /user/user/current}. Live preview uses client
 * SHA-256 Gravatar URLs when external fetch is allowed; otherwise initials.</p>
 */
export function AvatarSection({
  loadPrimaryEmail = defaultLoadPrimaryEmail,
  loadOverride = loadGravatarEmailOverride,
  saveOverride = saveGravatarEmailOverride,
  allowExternalAvatarFetch: allowExternalProp,
  userName: userNameProp,
  displayName: displayNameProp,
}: AvatarSectionProps = {}): React.ReactElement {
  const reactId = useId();
  const formId = `perc-profile-avatar-form-${reactId}`;
  const emailId = `perc-profile-avatar-email-${reactId}`;
  const emailHelpId = `perc-profile-avatar-email-help-${reactId}`;
  const emailErrorId = `perc-profile-avatar-email-error-${reactId}`;
  const usePrimaryId = `perc-profile-avatar-use-primary-${reactId}`;
  const previewId = `perc-profile-avatar-preview-${reactId}`;
  const statusId = `perc-profile-avatar-status-${reactId}`;

  const bootstrap = useSpaBootstrap();
  const userName = (userNameProp ?? bootstrap.userName ?? "").trim();
  const displayName =
    (displayNameProp ?? bootstrap.userName ?? "").trim() ||
    message(PROFILE_MSG.AVATAR_DEFAULT_NAME);
  const allowExternal =
    allowExternalProp ?? bootstrap.allowExternalAvatarFetch !== false;

  const [loadState, setLoadState] = useState<LoadState>({ status: "loading" });
  const [emailDraft, setEmailDraft] = useState("");
  const [usePrimary, setUsePrimary] = useState(true);
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoadState({ status: "loading" });
    setFormError(null);
    setSuccessMessage(null);
    try {
      const [primaryEmail, overrideEmail] = await Promise.all([
        loadPrimaryEmail(),
        loadOverride(userName),
      ]);
      const primary = (primaryEmail ?? "").trim();
      const override = (overrideEmail ?? "").trim();
      setEmailDraft(override || primary);
      setUsePrimary(!override);
      setLoadState({
        status: "ready",
        primaryEmail: primary,
        overrideEmail: override,
      });
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setLoadState({
        status: "error",
        message: formatApiError(err, message(PROFILE_MSG.AVATAR_LOAD_ERROR)),
      });
    }
  }, [loadPrimaryEmail, loadOverride, userName]);

  useEffect(() => {
    void load();
  }, [load]);

  const effectiveEmail = useMemo(() => {
    if (usePrimary) {
      return loadState.status === "ready" ? loadState.primaryEmail : "";
    }
    return emailDraft.trim();
  }, [usePrimary, emailDraft, loadState]);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      const presentation = await resolveAvatarPresentation({
        displayName,
        overrideEmail: usePrimary ? "" : emailDraft,
        primaryEmail:
          loadState.status === "ready" ? loadState.primaryEmail : "",
        allowExternalAvatarFetch: allowExternal,
        size: 96,
      });
      if (!cancelled) {
        setPreviewUrl(presentation.imageUrl);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [displayName, usePrimary, emailDraft, loadState, allowExternal]);

  const dirty =
    loadState.status === "ready" &&
    (() => {
      const storedOverride = loadState.overrideEmail;
      if (usePrimary) {
        return storedOverride !== "";
      }
      return emailDraft.trim() !== storedOverride;
    })();

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (loadState.status !== "ready") {
      return;
    }
    setFormError(null);
    setSuccessMessage(null);

    const toSave = usePrimary ? "" : emailDraft.trim();
    if (!usePrimary && !isValidEmailAddress(toSave)) {
      setFormError(message(PROFILE_MSG.AVATAR_EMAIL_INVALID));
      return;
    }
    if (!usePrimary && !toSave) {
      setFormError(message(PROFILE_MSG.AVATAR_EMAIL_INVALID));
      return;
    }

    setIsSaving(true);
    try {
      const saved = await saveOverride(userName, toSave);
      const normalized = saved == null ? "" : String(saved).trim();
      setLoadState({
        status: "ready",
        primaryEmail: loadState.primaryEmail,
        overrideEmail: normalized,
      });
      setUsePrimary(!normalized);
      setEmailDraft(normalized || loadState.primaryEmail);
      setSuccessMessage(message(PROFILE_MSG.AVATAR_SAVE_SUCCESS));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFormError(
        formatApiError(err, message(PROFILE_MSG.AVATAR_SAVE_ERROR)),
      );
    } finally {
      setIsSaving(false);
    }
  };

  if (loadState.status === "loading") {
    return (
      <div
        className={styles.root}
        data-testid="perc-profile-avatar"
        aria-busy="true"
      >
        <p className={styles.muted} {...i18nKeyAttr(PROFILE_MSG.AVATAR_LOADING)}>
          {message(PROFILE_MSG.AVATAR_LOADING)}
        </p>
      </div>
    );
  }

  if (loadState.status === "error") {
    return (
      <div
        className={styles.root}
        data-testid="perc-profile-avatar"
        role="alert"
      >
        <div className={styles.errorBox}>
          <p data-testid="perc-profile-avatar-load-error">
            {loadState.message}
          </p>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => void load()}
            data-testid="perc-profile-avatar-retry"
            {...i18nKeyAttr(PROFILE_MSG.AVATAR_RETRY)}
          >
            {message(PROFILE_MSG.AVATAR_RETRY)}
          </button>
        </div>
      </div>
    );
  }

  const emailDescribedBy = [
    emailHelpId,
    formError ? emailErrorId : null,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={styles.root} data-testid="perc-profile-avatar">
      <form
        id={formId}
        className={styles.form}
        onSubmit={(e) => void onSubmit(e)}
        noValidate
        aria-describedby={statusId}
      >
        <div className={styles.previewRow}>
          <div
            id={previewId}
            className={styles.preview}
            data-testid="perc-profile-avatar-preview"
          >
            <UserAvatar
              displayName={displayName}
              imageUrl={previewUrl}
              size={72}
              testId="perc-profile-avatar-chip"
            />
          </div>
          <div className={styles.previewMeta}>
            <p
              className={styles.previewLabel}
              {...i18nKeyAttr(PROFILE_MSG.AVATAR_PREVIEW_LABEL)}
            >
              {message(PROFILE_MSG.AVATAR_PREVIEW_LABEL)}
            </p>
            <p
              className={styles.hint}
              data-testid="perc-profile-avatar-effective-email"
              data-mkd-lang-ignore="1"
            >
              {effectiveEmail || message(PROFILE_MSG.AVATAR_NO_EMAIL)}
            </p>
          </div>
        </div>

        <div className={styles.field}>
          <div className={styles.checkboxRow}>
            <input
              id={usePrimaryId}
              type="checkbox"
              className={styles.checkbox}
              checked={usePrimary}
              disabled={isSaving}
              onChange={(e) => {
                const checked = e.target.checked;
                setUsePrimary(checked);
                setFormError(null);
                setSuccessMessage(null);
                if (checked) {
                  setEmailDraft(loadState.primaryEmail);
                }
              }}
              data-testid="perc-profile-avatar-use-primary"
            />
            <label
              className={styles.checkboxLabel}
              htmlFor={usePrimaryId}
              {...i18nKeyAttr(PROFILE_MSG.AVATAR_USE_PRIMARY)}
            >
              {message(PROFILE_MSG.AVATAR_USE_PRIMARY)}
            </label>
          </div>
          {loadState.primaryEmail ? (
            <p
              className={styles.hint}
              data-testid="perc-profile-avatar-primary-email"
              data-mkd-lang-ignore="1"
            >
              {message(PROFILE_MSG.AVATAR_PRIMARY_EMAIL).replace(
                "{0}",
                loadState.primaryEmail,
              )}
            </p>
          ) : (
            <p
              className={styles.hint}
              data-testid="perc-profile-avatar-primary-missing"
              {...i18nKeyAttr(PROFILE_MSG.AVATAR_PRIMARY_MISSING)}
            >
              {message(PROFILE_MSG.AVATAR_PRIMARY_MISSING)}
            </p>
          )}
        </div>

        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor={emailId}
            {...i18nKeyAttr(PROFILE_MSG.AVATAR_EMAIL_LABEL)}
          >
            {message(PROFILE_MSG.AVATAR_EMAIL_LABEL)}
          </label>
          <p
            id={emailHelpId}
            className={styles.hint}
            {...i18nKeyAttr(PROFILE_MSG.AVATAR_EMAIL_HINT)}
          >
            {message(PROFILE_MSG.AVATAR_EMAIL_HINT)}
          </p>
          <input
            id={emailId}
            name="gravatarEmail"
            type="email"
            autoComplete="email"
            className={styles.input}
            value={emailDraft}
            disabled={isSaving || usePrimary}
            aria-invalid={formError ? true : undefined}
            aria-describedby={emailDescribedBy}
            onChange={(e) => {
              setEmailDraft(e.target.value);
              setFormError(null);
              setSuccessMessage(null);
            }}
            data-testid="perc-profile-avatar-email"
          />
        </div>

        <p
          className={styles.hint}
          data-testid="perc-profile-avatar-privacy"
          {...i18nKeyAttr(
            allowExternal
              ? PROFILE_MSG.AVATAR_PRIVACY_NOTE
              : PROFILE_MSG.AVATAR_EXTERNAL_DISABLED,
          )}
        >
          {message(
            allowExternal
              ? PROFILE_MSG.AVATAR_PRIVACY_NOTE
              : PROFILE_MSG.AVATAR_EXTERNAL_DISABLED,
          )}
        </p>
        <p
          className={styles.hint}
          data-testid="perc-profile-avatar-sso-note"
          {...i18nKeyAttr(PROFILE_MSG.AVATAR_SSO_NOTE)}
        >
          {message(PROFILE_MSG.AVATAR_SSO_NOTE)}
        </p>

        <div className={styles.actions}>
          <button
            type="submit"
            className={styles.primaryButton}
            disabled={isSaving || !dirty}
            data-testid="perc-profile-avatar-save"
            {...i18nKeyAttr(PROFILE_MSG.AVATAR_SAVE)}
          >
            {message(
              isSaving ? PROFILE_MSG.AVATAR_SAVING : PROFILE_MSG.AVATAR_SAVE,
            )}
          </button>
        </div>

        <div
          id={statusId}
          className={styles.statusRegion}
          role="status"
          aria-live="polite"
          aria-atomic="true"
        >
          {formError ? (
            <p
              id={emailErrorId}
              className={styles.formError}
              data-testid="perc-profile-avatar-error"
            >
              {formError}
            </p>
          ) : null}
          {successMessage ? (
            <p
              className={styles.success}
              data-testid="perc-profile-avatar-success"
            >
              {successMessage}
            </p>
          ) : null}
        </div>
      </form>
    </div>
  );
}

async function defaultLoadPrimaryEmail(): Promise<string> {
  const user = await getCurrentUserBasic();
  return user.email ?? "";
}
