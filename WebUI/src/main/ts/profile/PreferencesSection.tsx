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
import { getAllUserPreferences } from "../api/preferences/preferencesApi";
import {
  getMyHomepageOverride,
  setMyHomepageOverride,
} from "../api/user/userHomepageApi";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { i18nKeyAttr } from "../i18n/i18nDom";
import { message } from "../i18n/message";
import { profileLandingOptions } from "./landingOptions";
import { PROFILE_MSG } from "./messages";
import styles from "./PreferencesSection.module.css";

export interface PreferencesSectionProps {
  /** Optional test doubles — default to live REST. */
  loadLanding?: () => Promise<string>;
  saveLanding?: (landing: string) => Promise<string>;
  loadPreferenceCount?: () => Promise<number>;
  isAdmin?: boolean;
  isDesigner?: boolean;
}

type LoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; landing: string; preferenceCount: number };

/**
 * Profile Preferences section (#2396 / parent #2374 slice 4).
 *
 * <p>Default CMS landing page uses existing self homepage REST (product-backed).
 * PreferenceResource is loaded via {@code getAllUserPreferences} so this section
 * binds the public preference stack without inventing a parallel store or keys
 * the product ignores (language/density deferred until product persists them).</p>
 */
export function PreferencesSection({
  loadLanding = getMyHomepageOverride,
  saveLanding = setMyHomepageOverride,
  loadPreferenceCount = defaultPreferenceCount,
  isAdmin: isAdminProp,
  isDesigner: isDesignerProp,
}: PreferencesSectionProps = {}): React.ReactElement {
  const reactId = useId();
  const formId = `perc-profile-preferences-form-${reactId}`;
  const landingId = `perc-profile-preferences-landing-${reactId}`;
  const landingHelpId = `perc-profile-preferences-landing-help-${reactId}`;
  const statusId = `perc-profile-preferences-status-${reactId}`;

  const bootstrap = useSpaBootstrap();
  const isAdmin = isAdminProp ?? bootstrap.isAdmin;
  const isDesigner = isDesignerProp ?? bootstrap.isDesigner;

  const [loadState, setLoadState] = useState<LoadState>({ status: "loading" });
  const [landingDraft, setLandingDraft] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const load = useCallback(async () => {
    setLoadState({ status: "loading" });
    setFormError(null);
    setSuccessMessage(null);
    try {
      const [landing, preferenceCount] = await Promise.all([
        loadLanding(),
        loadPreferenceCount(),
      ]);
      const normalized = landing == null ? "" : String(landing).trim();
      setLandingDraft(normalized);
      setLoadState({
        status: "ready",
        landing: normalized,
        preferenceCount:
          typeof preferenceCount === "number" && preferenceCount >= 0
            ? preferenceCount
            : 0,
      });
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setLoadState({
        status: "error",
        message: formatApiError(err, message(PROFILE_MSG.PREF_LOAD_ERROR)),
      });
    }
  }, [loadLanding, loadPreferenceCount]);

  useEffect(() => {
    void load();
  }, [load]);

  const options = useMemo(
    () =>
      profileLandingOptions(
        { isAdmin, isDesigner },
        loadState.status === "ready" ? loadState.landing : landingDraft,
      ),
    [isAdmin, isDesigner, loadState, landingDraft],
  );

  const dirty =
    loadState.status === "ready" && landingDraft !== loadState.landing;

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (loadState.status !== "ready") {
      return;
    }
    setFormError(null);
    setSuccessMessage(null);
    setIsSaving(true);
    try {
      const saved = await saveLanding(landingDraft);
      const normalized = saved == null ? "" : String(saved).trim();
      // Blank clear may return ""; treat empty draft as clear.
      const effective =
        landingDraft === "" ? "" : normalized || landingDraft.trim();
      setLandingDraft(effective);
      setLoadState({
        status: "ready",
        landing: effective,
        preferenceCount: loadState.preferenceCount,
      });
      setSuccessMessage(message(PROFILE_MSG.PREF_SAVE_SUCCESS));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setFormError(
        formatApiError(err, message(PROFILE_MSG.PREF_SAVE_ERROR)),
      );
    } finally {
      setIsSaving(false);
    }
  };

  if (loadState.status === "loading") {
    return (
      <div
        className={styles.root}
        data-testid="perc-profile-preferences"
        aria-busy="true"
      >
        <p className={styles.muted} {...i18nKeyAttr(PROFILE_MSG.PREF_LOADING)}>
          {message(PROFILE_MSG.PREF_LOADING)}
        </p>
      </div>
    );
  }

  if (loadState.status === "error") {
    return (
      <div
        className={styles.root}
        data-testid="perc-profile-preferences"
        role="alert"
      >
        <div className={styles.errorBox}>
          <p data-testid="perc-profile-preferences-load-error">
            {loadState.message}
          </p>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => void load()}
            data-testid="perc-profile-preferences-retry"
            {...i18nKeyAttr(PROFILE_MSG.PREF_RETRY)}
          >
            {message(PROFILE_MSG.PREF_RETRY)}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.root} data-testid="perc-profile-preferences">
      <form
        id={formId}
        className={styles.form}
        onSubmit={(e) => void onSubmit(e)}
        noValidate
        aria-describedby={statusId}
      >
        <div className={styles.field}>
          <label
            className={styles.label}
            htmlFor={landingId}
            {...i18nKeyAttr(PROFILE_MSG.PREF_LANDING_LABEL)}
          >
            {message(PROFILE_MSG.PREF_LANDING_LABEL)}
          </label>
          <p
            id={landingHelpId}
            className={styles.hint}
            {...i18nKeyAttr(PROFILE_MSG.PREF_LANDING_HINT)}
          >
            {message(PROFILE_MSG.PREF_LANDING_HINT)}
          </p>
          <select
            id={landingId}
            className={styles.select}
            value={landingDraft}
            onChange={(e) => {
              setLandingDraft(e.target.value);
              setSuccessMessage(null);
              setFormError(null);
            }}
            disabled={isSaving}
            aria-describedby={landingHelpId}
            data-testid="perc-profile-preferences-landing"
          >
            {options.map((opt) => (
              <option key={opt.value || "__role_default__"} value={opt.value}>
                {message(opt.labelKey)}
              </option>
            ))}
          </select>
        </div>

        <p
          className={styles.hint}
          data-testid="perc-profile-preferences-stack-note"
          {...i18nKeyAttr(PROFILE_MSG.PREF_STACK_NOTE)}
        >
          {message(PROFILE_MSG.PREF_STACK_NOTE)}
        </p>
        <p
          className={styles.muted}
          data-testid="perc-profile-preferences-count"
          aria-live="polite"
        >
          {message(PROFILE_MSG.PREF_STORED_COUNT).replace(
            "{0}",
            String(loadState.preferenceCount),
          )}
        </p>

        <div className={styles.actions}>
          <button
            type="submit"
            className={styles.primaryButton}
            disabled={isSaving || !dirty}
            data-testid="perc-profile-preferences-save"
            {...i18nKeyAttr(PROFILE_MSG.PREF_SAVE)}
          >
            {message(
              isSaving ? PROFILE_MSG.PREF_SAVING : PROFILE_MSG.PREF_SAVE,
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
              className={styles.formError}
              data-testid="perc-profile-preferences-error"
            >
              {formError}
            </p>
          ) : null}
          {successMessage ? (
            <p
              className={styles.success}
              data-testid="perc-profile-preferences-success"
            >
              {successMessage}
            </p>
          ) : null}
        </div>
      </form>
    </div>
  );
}

async function defaultPreferenceCount(): Promise<number> {
  const prefs = await getAllUserPreferences();
  return prefs.length;
}
