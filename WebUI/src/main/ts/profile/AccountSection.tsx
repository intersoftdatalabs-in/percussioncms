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
import { allowedSessionCommunities } from "../api/user/communitySwitchApi";
import {
  type CurrentUserProfile,
  getCurrentUserProfile,
  isValidEmailAddress,
  updateMyAccountEmail,
  updateMyDefaultCommunity,
} from "../api/user/userProfileApi";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { PROFILE_MSG } from "./messages";
import {
  loadRememberLastCommunityPrefs,
  saveLastCommunity,
  saveRememberLastCommunityFlag,
  type RememberLastCommunityPrefs,
} from "./rememberLastCommunity";
import styles from "./AccountSection.module.css";

export interface AccountSectionProps {
  /** Optional test double — defaults to live REST. */
  loadProfile?: () => Promise<CurrentUserProfile>;
  saveEmail?: (email: string) => Promise<CurrentUserProfile>;
  saveDefaultCommunity?: (name: string) => Promise<CurrentUserProfile>;
  loadRememberLast?: () => Promise<RememberLastCommunityPrefs>;
  saveRememberLast?: (userName: string, remember: boolean) => Promise<boolean>;
  persistLastCommunity?: (userName: string, community: string) => Promise<string>;
}

type LoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; profile: CurrentUserProfile };

/**
 * Select value for the stored default: membership spelling when still allowed,
 * otherwise empty so the control never holds a stale unmatched option.
 */
export function allowedDefaultCommunityDraft(
  profile: Pick<CurrentUserProfile, "communities" | "defaultCommunity">,
): string {
  const stored = (profile.defaultCommunity ?? "").trim();
  if (!stored) {
    return "";
  }
  const allowed = allowedSessionCommunities(profile.communities);
  return (
    allowed.find((name) => name.toLowerCase() === stored.toLowerCase()) ?? ""
  );
}

/**
 * Account identity view/edit for the signed-in user (slice 2 / #2395).
 * Login id, provider, roles, and communities are read-only. Email is editable
 * only for internal accounts; directory-managed fields show localized hints.
 * Default community (#3508) and remember-last community (#3507) are self-service
 * controls on this form.
 */
export function AccountSection({
  loadProfile = getCurrentUserProfile,
  saveEmail = updateMyAccountEmail,
  saveDefaultCommunity = updateMyDefaultCommunity,
  loadRememberLast = loadRememberLastCommunityPrefs,
  saveRememberLast = saveRememberLastCommunityFlag,
  persistLastCommunity = saveLastCommunity,
}: AccountSectionProps = {}): React.ReactElement {
  const reactId = useId();
  const formId = `perc-profile-account-form-${reactId}`;
  const emailId = `perc-profile-account-email-${reactId}`;
  const emailErrorId = `perc-profile-account-email-error-${reactId}`;
  const emailHelpId = `perc-profile-account-email-help-${reactId}`;
  const defaultCommunityId = `perc-profile-account-default-community-${reactId}`;
  const defaultCommunityHelpId = `perc-profile-account-default-community-help-${reactId}`;
  const defaultCommunityErrorId = `perc-profile-account-default-community-error-${reactId}`;
  const defaultCommunityFormId = `perc-profile-account-default-community-form-${reactId}`;
  const rememberId = `perc-profile-account-remember-last-${reactId}`;
  const rememberHelpId = `perc-profile-account-remember-last-help-${reactId}`;
  const statusId = `perc-profile-account-status-${reactId}`;

  const [loadState, setLoadState] = useState<LoadState>({ status: "loading" });
  const [emailDraft, setEmailDraft] = useState("");
  const [defaultCommunityDraft, setDefaultCommunityDraft] = useState("");
  const [rememberLast, setRememberLast] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [communityFieldError, setCommunityFieldError] = useState<string | null>(
    null,
  );
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isSavingCommunity, setIsSavingCommunity] = useState(false);
  const [isSavingRemember, setIsSavingRemember] = useState(false);

  const load = useCallback(async () => {
    setLoadState({ status: "loading" });
    setFieldError(null);
    setFormError(null);
    setSuccessMessage(null);
    try {
      const [profile, rememberPrefs] = await Promise.all([
        loadProfile(),
        loadRememberLast().catch(() => ({ remember: false, last: "" })),
      ]);
      setEmailDraft(profile.email ?? "");
      setDefaultCommunityDraft(allowedDefaultCommunityDraft(profile));
      setCommunityFieldError(null);
      setRememberLast(rememberPrefs.remember);
      setLoadState({ status: "ready", profile });
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setLoadState({
        status: "error",
        message: formatApiError(err, message(PROFILE_MSG.ACCOUNT_LOAD_ERROR)),
      });
    }
  }, [loadProfile, loadRememberLast]);

  useEffect(() => {
    void load();
  }, [load]);

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (loadState.status !== "ready" || !loadState.profile.emailEditable) {
      return;
    }
    setFieldError(null);
    setFormError(null);
    setSuccessMessage(null);

    if (!isValidEmailAddress(emailDraft)) {
      setFieldError(message(PROFILE_MSG.EMAIL_INVALID));
      return;
    }

    setIsSaving(true);
    try {
      const updated = await saveEmail(emailDraft.trim());
      setLoadState({ status: "ready", profile: updated });
      setEmailDraft(updated.email ?? "");
      setSuccessMessage(message(PROFILE_MSG.SAVE_SUCCESS));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFormError(formatApiError(err, message(PROFILE_MSG.SAVE_ERROR)));
    } finally {
      setIsSaving(false);
    }
  };

  const onSubmitDefaultCommunity = async (event: React.FormEvent) => {
    event.preventDefault();
    if (loadState.status !== "ready") {
      return;
    }
    setCommunityFieldError(null);
    setFormError(null);
    setSuccessMessage(null);

    const allowed = allowedSessionCommunities(loadState.profile.communities);
    const trimmed = defaultCommunityDraft.trim();
    if (
      trimmed &&
      !allowed.some((name) => name.toLowerCase() === trimmed.toLowerCase())
    ) {
      setCommunityFieldError(message(PROFILE_MSG.DEFAULT_COMMUNITY_INVALID));
      return;
    }

    setIsSavingCommunity(true);
    try {
      const updated = await saveDefaultCommunity(trimmed);
      setLoadState({ status: "ready", profile: updated });
      setDefaultCommunityDraft(allowedDefaultCommunityDraft(updated));
      setSuccessMessage(message(PROFILE_MSG.DEFAULT_COMMUNITY_SAVE_SUCCESS));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFormError(
        formatApiError(err, message(PROFILE_MSG.DEFAULT_COMMUNITY_SAVE_ERROR)),
      );
    } finally {
      setIsSavingCommunity(false);
    }
  };

  const onRememberLastChange = async (
    event: React.ChangeEvent<HTMLInputElement>,
  ): Promise<void> => {
    if (loadState.status !== "ready") {
      return;
    }
    const next = event.target.checked;
    const previous = rememberLast;
    setRememberLast(next);
    setFormError(null);
    setSuccessMessage(null);
    setIsSavingRemember(true);
    try {
      await saveRememberLast(loadState.profile.name, next);
      if (next) {
        const current = (loadState.profile.currentCommunity ?? "").trim();
        if (current) {
          await persistLastCommunity(loadState.profile.name, current);
        }
      }
      setSuccessMessage(message(PROFILE_MSG.REMEMBER_LAST_SAVE_SUCCESS));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setRememberLast(previous);
      setFormError(
        formatApiError(err, message(PROFILE_MSG.REMEMBER_LAST_SAVE_ERROR)),
      );
    } finally {
      setIsSavingRemember(false);
    }
  };

  if (loadState.status === "loading") {
    return (
      <div
        className={styles.root}
        data-testid="perc-profile-account"
        aria-busy="true"
      >
        <p
          className={styles.muted}
          data-testid="perc-profile-account-loading"
          {...i18nKeyAttr(PROFILE_MSG.ACCOUNT_LOADING)}
        >
          {message(PROFILE_MSG.ACCOUNT_LOADING)}
        </p>
      </div>
    );
  }

  if (loadState.status === "error") {
    return (
      <div className={styles.root} data-testid="perc-profile-account">
        <div
          className={styles.errorBox}
          role="alert"
          data-testid="perc-profile-account-load-error"
        >
          <p {...i18nKeyAttr(PROFILE_MSG.ACCOUNT_LOAD_ERROR)}>
            {loadState.message || message(PROFILE_MSG.ACCOUNT_LOAD_ERROR)}
          </p>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => void load()}
            data-testid="perc-profile-account-retry"
            {...i18nKeyAttr(PROFILE_MSG.ACCOUNT_RETRY)}
          >
            {message(PROFILE_MSG.ACCOUNT_RETRY)}
          </button>
        </div>
      </div>
    );
  }

  const { profile } = loadState;
  const providerLabel =
    profile.providerType === "DIRECTORY"
      ? message(PROFILE_MSG.PROVIDER_DIRECTORY)
      : message(PROFILE_MSG.PROVIDER_INTERNAL);
  const providerKey =
    profile.providerType === "DIRECTORY"
      ? PROFILE_MSG.PROVIDER_DIRECTORY
      : PROFILE_MSG.PROVIDER_INTERNAL;

  const rolesText =
    profile.roles.length > 0
      ? profile.roles.join(", ")
      : message(PROFILE_MSG.NONE_LISTED);
  const communitiesText =
    profile.communities.length > 0
      ? profile.communities.join(", ")
      : message(PROFILE_MSG.NONE_LISTED);
  const currentCommunityText =
    profile.currentCommunity || message(PROFILE_MSG.EMPTY_VALUE);

  const emailDescribedBy = [
    emailHelpId,
    fieldError ? emailErrorId : null,
  ]
    .filter(Boolean)
    .join(" ");

  const allowedCommunities = allowedSessionCommunities(profile.communities);
  const dirty =
    profile.emailEditable &&
    emailDraft.trim() !== (profile.email ?? "").trim();
  const communityDirty =
    defaultCommunityDraft.trim() !== (profile.defaultCommunity ?? "").trim();

  return (
    <div className={styles.root} data-testid="perc-profile-account">
      <dl className={styles.fieldList}>
        <div className={styles.field}>
          <dt>
            <span {...i18nKeyAttr(PROFILE_MSG.FIELD_LOGIN_ID)}>
              {message(PROFILE_MSG.FIELD_LOGIN_ID)}
            </span>
          </dt>
          <dd>
            <span
              className={styles.readonlyValue}
              data-testid="perc-profile-account-login"
            >
              {profile.name || message(PROFILE_MSG.EMPTY_VALUE)}
            </span>
            <p
              className={styles.hint}
              data-testid="perc-profile-account-login-hint"
              {...i18nKeyAttr(PROFILE_MSG.LOGIN_READONLY_HINT)}
            >
              {message(PROFILE_MSG.LOGIN_READONLY_HINT)}
            </p>
          </dd>
        </div>

        <div className={styles.field}>
          <dt>
            <span {...i18nKeyAttr(PROFILE_MSG.FIELD_PROVIDER)}>
              {message(PROFILE_MSG.FIELD_PROVIDER)}
            </span>
          </dt>
          <dd>
            <span
              className={styles.readonlyValue}
              data-testid="perc-profile-account-provider"
              data-provider={profile.providerType}
              {...i18nKeyAttr(providerKey)}
            >
              {providerLabel}
            </span>
            {profile.providerType === "DIRECTORY" && (
              <p
                className={styles.hint}
                data-testid="perc-profile-account-directory-hint"
                {...i18nKeyAttr(PROFILE_MSG.READONLY_HINT)}
              >
                {message(PROFILE_MSG.READONLY_HINT)}
              </p>
            )}
          </dd>
        </div>

        <div className={styles.field}>
          <dt>
            <label htmlFor={emailId} {...i18nKeyAttr(PROFILE_MSG.FIELD_EMAIL)}>
              {message(PROFILE_MSG.FIELD_EMAIL)}
            </label>
          </dt>
          <dd>
            {profile.emailEditable ? (
              <form
                id={formId}
                className={styles.emailForm}
                onSubmit={(e) => void onSubmit(e)}
                noValidate
                data-testid="perc-profile-account-email-form"
              >
                <input
                  id={emailId}
                  name="email"
                  type="email"
                  autoComplete="email"
                  className={styles.emailInput}
                  value={emailDraft}
                  onChange={(e) => {
                    setEmailDraft(e.target.value);
                    setFieldError(null);
                    setSuccessMessage(null);
                  }}
                  disabled={isSaving}
                  aria-invalid={fieldError ? true : undefined}
                  aria-describedby={emailDescribedBy}
                  data-testid="perc-profile-account-email"
                />
                <p
                  id={emailHelpId}
                  className={styles.hint}
                  {...i18nKeyAttr(PROFILE_MSG.EMAIL_HELP_EDITABLE)}
                >
                  {message(PROFILE_MSG.EMAIL_HELP_EDITABLE)}
                </p>
                {fieldError && (
                  <p
                    id={emailErrorId}
                    className={styles.fieldError}
                    role="alert"
                    data-testid="perc-profile-account-email-error"
                  >
                    {fieldError}
                  </p>
                )}
                <div className={styles.actions}>
                  <button
                    type="submit"
                    className={styles.primaryButton}
                    disabled={isSaving || !dirty}
                    data-testid="perc-profile-account-save"
                    {...i18nKeyAttr(
                      isSaving ? PROFILE_MSG.SAVING : PROFILE_MSG.SAVE_EMAIL,
                    )}
                  >
                    {message(
                      isSaving ? PROFILE_MSG.SAVING : PROFILE_MSG.SAVE_EMAIL,
                    )}
                  </button>
                </div>
              </form>
            ) : (
              <>
                <span
                  className={styles.readonlyValue}
                  data-testid="perc-profile-account-email"
                >
                  {profile.email || message(PROFILE_MSG.EMPTY_VALUE)}
                </span>
                <p
                  id={emailHelpId}
                  className={styles.hint}
                  data-testid="perc-profile-account-email-ro-hint"
                  {...i18nKeyAttr(PROFILE_MSG.EMAIL_HELP_DIRECTORY)}
                >
                  {message(PROFILE_MSG.EMAIL_HELP_DIRECTORY)}
                </p>
              </>
            )}
          </dd>
        </div>

        <div className={styles.field}>
          <dt>
            <span {...i18nKeyAttr(PROFILE_MSG.FIELD_ROLES)}>
              {message(PROFILE_MSG.FIELD_ROLES)}
            </span>
          </dt>
          <dd>
            <span
              className={styles.readonlyValue}
              data-testid="perc-profile-account-roles"
            >
              {rolesText}
            </span>
            <p
              className={styles.hint}
              {...i18nKeyAttr(PROFILE_MSG.ROLES_READONLY_HINT)}
            >
              {message(PROFILE_MSG.ROLES_READONLY_HINT)}
            </p>
          </dd>
        </div>

        <div className={styles.field}>
          <dt>
            <span {...i18nKeyAttr(PROFILE_MSG.FIELD_COMMUNITIES)}>
              {message(PROFILE_MSG.FIELD_COMMUNITIES)}
            </span>
          </dt>
          <dd>
            <span
              className={styles.readonlyValue}
              data-testid="perc-profile-account-communities"
            >
              {communitiesText}
            </span>
            <p
              className={styles.hint}
              {...i18nKeyAttr(PROFILE_MSG.COMMUNITIES_READONLY_HINT)}
            >
              {message(PROFILE_MSG.COMMUNITIES_READONLY_HINT)}
            </p>
          </dd>
        </div>

        <div className={styles.field}>
          <dt>
            <span {...i18nKeyAttr(PROFILE_MSG.FIELD_CURRENT_COMMUNITY)}>
              {message(PROFILE_MSG.FIELD_CURRENT_COMMUNITY)}
            </span>
          </dt>
          <dd>
            <span
              className={styles.readonlyValue}
              data-testid="perc-profile-account-current-community"
            >
              {currentCommunityText}
            </span>
          </dd>
        </div>

        <div className={styles.field}>
          <dt>
            <label
              htmlFor={defaultCommunityId}
              {...i18nKeyAttr(PROFILE_MSG.FIELD_DEFAULT_COMMUNITY)}
            >
              {message(PROFILE_MSG.FIELD_DEFAULT_COMMUNITY)}
            </label>
          </dt>
          <dd>
            {allowedCommunities.length > 0 ? (
              <form
                id={defaultCommunityFormId}
                className={styles.communityForm}
                onSubmit={(e) => void onSubmitDefaultCommunity(e)}
                noValidate
                data-testid="perc-profile-account-default-community-form"
              >
                <select
                  id={defaultCommunityId}
                  name="defaultCommunity"
                  className={styles.select}
                  value={defaultCommunityDraft}
                  onChange={(e) => {
                    setDefaultCommunityDraft(e.target.value);
                    setCommunityFieldError(null);
                    setSuccessMessage(null);
                  }}
                  disabled={isSavingCommunity}
                  aria-invalid={communityFieldError ? true : undefined}
                  aria-describedby={
                    communityFieldError
                      ? `${defaultCommunityHelpId} ${defaultCommunityErrorId}`
                      : defaultCommunityHelpId
                  }
                  data-testid="perc-profile-account-default-community-select"
                >
                  <option
                    value=""
                    {...i18nKeyAttr(PROFILE_MSG.DEFAULT_COMMUNITY_NONE)}
                  >
                    {message(PROFILE_MSG.DEFAULT_COMMUNITY_NONE)}
                  </option>
                  {allowedCommunities.map((name) => (
                    <option key={name} value={name}>
                      {name}
                    </option>
                  ))}
                </select>
                <p
                  id={defaultCommunityHelpId}
                  className={styles.hint}
                  {...i18nKeyAttr(PROFILE_MSG.DEFAULT_COMMUNITY_HINT)}
                >
                  {message(PROFILE_MSG.DEFAULT_COMMUNITY_HINT)}
                </p>
                {communityFieldError && (
                  <p
                    id={defaultCommunityErrorId}
                    className={styles.fieldError}
                    role="alert"
                    data-testid="perc-profile-account-default-community-error"
                  >
                    {communityFieldError}
                  </p>
                )}
                <div className={styles.actions}>
                  <button
                    type="submit"
                    className={styles.primaryButton}
                    disabled={isSavingCommunity || !communityDirty}
                    data-testid="perc-profile-account-default-community-save"
                    {...i18nKeyAttr(
                      isSavingCommunity
                        ? PROFILE_MSG.SAVING
                        : PROFILE_MSG.SAVE_DEFAULT_COMMUNITY,
                    )}
                  >
                    {message(
                      isSavingCommunity
                        ? PROFILE_MSG.SAVING
                        : PROFILE_MSG.SAVE_DEFAULT_COMMUNITY,
                    )}
                  </button>
                </div>
              </form>
            ) : (
              <>
                <span
                  className={styles.readonlyValue}
                  data-testid="perc-profile-account-default-community-unavailable"
                >
                  {message(PROFILE_MSG.DEFAULT_COMMUNITY_UNAVAILABLE)}
                </span>
                <p
                  className={styles.hint}
                  {...i18nKeyAttr(PROFILE_MSG.DEFAULT_COMMUNITY_HINT)}
                >
                  {message(PROFILE_MSG.DEFAULT_COMMUNITY_HINT)}
                </p>
              </>
            )}
          </dd>
        </div>

        <div className={styles.field}>
          <dt>
            <label
              htmlFor={rememberId}
              {...i18nKeyAttr(PROFILE_MSG.REMEMBER_LAST_LABEL)}
            >
              {message(PROFILE_MSG.REMEMBER_LAST_LABEL)}
            </label>
          </dt>
          <dd>
            <input
              id={rememberId}
              type="checkbox"
              className={styles.checkbox}
              checked={rememberLast}
              disabled={isSavingRemember}
              onChange={(e) => {
                void onRememberLastChange(e);
              }}
              aria-describedby={rememberHelpId}
              data-testid="perc-profile-account-remember-last"
            />
            <p
              id={rememberHelpId}
              className={styles.hint}
              data-testid="perc-profile-account-remember-last-hint"
              {...i18nKeyAttr(PROFILE_MSG.REMEMBER_LAST_HINT)}
            >
              {message(PROFILE_MSG.REMEMBER_LAST_HINT)}
            </p>
          </dd>
        </div>
      </dl>

      <div
        id={statusId}
        className={styles.statusRegion}
        role="status"
        aria-live="polite"
        data-testid="perc-profile-account-status"
      >
        {successMessage && (
          <p
            className={styles.success}
            data-testid="perc-profile-account-success"
          >
            {successMessage}
          </p>
        )}
        {formError && (
          <p
            className={styles.formError}
            data-testid="perc-profile-account-form-error"
          >
            {formError}
          </p>
        )}
      </div>
    </div>
  );
}
