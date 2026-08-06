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

/**
 * Thin adapter for third-party {@code @mkd/language} (crowdsource translation UX).
 *
 * <p>Opt-in via query/localStorage. Submissions POST to
 * {@link MKD_LANGUAGE_CORRECTIONS_URL} (server holds GCM PAT). Catalog keys come
 * from tracked {@link message}. Server gates: {@code perc.mkd.language.enabled}
 * + roles. Library design lives outside this monorepo.</p>
 *
 * <p>Enable UX:</p>
 * <ul>
 *   <li>Query: {@code ?mkdLang=1} (or {@code 0} to force off)</li>
 *   <li>localStorage: {@code perc-mkd-lang} = {@code 1} | {@code 0}</li>
 * </ul>
 */

import { init, type MkdLanguageHandle } from "@mkd/language";
import { getCsrfToken } from "../api/csrf";
import { I18N_KEY_ATTR } from "./i18nDom";
import { getTrackedMessageId } from "./message";

/** localStorage key for the opt-in experiment flag. */
export const MKD_LANG_STORAGE_KEY = "perc-mkd-lang";

/** Query parameter for the opt-in experiment flag. */
export const MKD_LANG_QUERY_PARAM = "mkdLang";

/** Same-origin BFF for language corrections (session + CSRF). */
export const MKD_LANGUAGE_CORRECTIONS_URL = "/Rhythmyx/rest/i18n/corrections";

export interface EnsureMkdLanguageOptions {
  /**
   * Content locale being corrected (BCP-47 style product codes, e.g. {@code en-us}).
   * String or getter re-read when the popover opens.
   */
  locale: string | (() => string | undefined);
  /** Library chrome locale; defaults to content locale. */
  uiLocale?: string | (() => string | undefined);
  /** Optional default email for the correction form. */
  getUserEmail?: () => string | undefined;
  /**
   * When true, no-op submissions are logged with {@code console.debug}.
   * Defaults to the same enablement path when query has {@code mkdLangDebug=1}.
   */
  debug?: boolean;
}

let handle: MkdLanguageHandle | null = null;
let lastLocaleRef: EnsureMkdLanguageOptions["locale"] | undefined;

const MKD_THEME_STYLE_ID = "perc-mkd-lang-theme";

/**
 * Light product theme tokens for library chrome (accent / icon opacity).
 * Does not fork library CSS — only CSS variables the client already reads.
 */
function ensureMkdThemeTokens(): void {
  if (typeof document === "undefined") {
    return;
  }
  if (document.getElementById(MKD_THEME_STYLE_ID)) {
    return;
  }
  const style = document.createElement("style");
  style.id = MKD_THEME_STYLE_ID;
  style.textContent = `
:root {
  --mkd-lang-accent: var(--perc-color-primary, #007ea8);
  --mkd-lang-icon-opacity: 0.5;
}
`.trim();
  document.head.appendChild(style);
}

/**
 * Resolve experiment enablement. Query wins over localStorage; default is off.
 *
 * @param search - optional override of {@code window.location.search} (tests)
 * @param storage - optional storage (tests); defaults to {@code localStorage}
 */
export function isMkdLanguageEnabled(
  search?: string,
  storage?: Pick<Storage, "getItem"> | null,
): boolean {
  const q = parseQueryFlag(
    search ??
      (typeof window !== "undefined" ? window.location.search : undefined),
    MKD_LANG_QUERY_PARAM,
  );
  if (q !== null) {
    return q;
  }
  const store =
    storage === undefined
      ? typeof localStorage !== "undefined"
        ? localStorage
        : null
      : storage;
  if (!store) {
    return false;
  }
  try {
    const raw = store.getItem(MKD_LANG_STORAGE_KEY);
    return raw === "1" || raw === "true";
  } catch {
    return false;
  }
}

/**
 * Whether no-op submissions should log to the console.
 */
export function isMkdLanguageDebug(
  search?: string,
  storage?: Pick<Storage, "getItem"> | null,
): boolean {
  const q = parseQueryFlag(
    search ??
      (typeof window !== "undefined" ? window.location.search : undefined),
    "mkdLangDebug",
  );
  if (q !== null) {
    return q;
  }
  const store =
    storage === undefined
      ? typeof localStorage !== "undefined"
        ? localStorage
        : null
      : storage;
  if (!store) {
    return false;
  }
  try {
    const raw = store.getItem("perc-mkd-lang-debug");
    return raw === "1" || raw === "true";
  } catch {
    return false;
  }
}

/**
 * Start or reconfigure the client when the experiment is enabled.
 * Safe to call repeatedly; never throws into product boot.
 *
 * @returns handle when active, otherwise {@code null}
 */
export function ensureMkdLanguage(
  options: EnsureMkdLanguageOptions,
): MkdLanguageHandle | null {
  if (typeof document === "undefined") {
    return null;
  }
  if (!isMkdLanguageEnabled()) {
    destroyMkdLanguage();
    return null;
  }

  const debug = options.debug ?? isMkdLanguageDebug();
  lastLocaleRef = options.locale;
  ensureMkdThemeTokens();

  const postHeaders = (): Record<string, string> => {
    const csrf = getCsrfToken();
    if (!csrf?.token) {
      return {};
    }
    return { [csrf.headerName]: csrf.token };
  };

  try {
    if (handle) {
      handle.configure({
        locale: options.locale,
        uiLocale: options.uiLocale ?? options.locale,
        getUserEmail: options.getUserEmail,
        debug,
        postUrl: MKD_LANGUAGE_CORRECTIONS_URL,
        postHeaders,
        messageIdAttr: I18N_KEY_ATTR,
        getMessageId: getTrackedMessageId,
        scanMessageIdAttr: true,
        includeChromeSelectors: true,
        zIndex: 20000,
        respectIgnore: true,
      });
      handle.rescan();
      return handle;
    }

    handle = init({
      locale: options.locale,
      uiLocale: options.uiLocale ?? options.locale,
      getUserEmail: options.getUserEmail,
      messageIdAttr: I18N_KEY_ATTR,
      messageIdAncestorWalk: true,
      getMessageId: getTrackedMessageId,
      scanMessageIdAttr: true,
      includeChromeSelectors: true,
      // Above SPA modals / dialogs (typical product chrome ~1000–5000).
      zIndex: 20000,
      respectIgnore: true,
      // Host BFF → GCM; token never in the browser
      postUrl: MKD_LANGUAGE_CORRECTIONS_URL,
      postHeaders,
      debug,
    });
    return handle;
  } catch (err) {
    console.warn("[PercModernUI] @mkd/language init failed", err);
    handle = null;
    return null;
  }
}

/**
 * Update locale (and optional email) without re-checking enablement when already
 * active. No-op when the experiment is off or never started.
 */
export function configureMkdLanguage(
  partial: Partial<EnsureMkdLanguageOptions>,
): void {
  if (!handle) {
    return;
  }
  try {
    const locale = partial.locale ?? lastLocaleRef;
    if (partial.locale !== undefined) {
      lastLocaleRef = partial.locale;
    }
    const debug = partial.debug ?? isMkdLanguageDebug();
    handle.configure({
      locale,
      uiLocale: partial.uiLocale ?? locale,
      getUserEmail: partial.getUserEmail,
      debug,
      postUrl: MKD_LANGUAGE_CORRECTIONS_URL,
      messageIdAttr: I18N_KEY_ATTR,
      getMessageId: getTrackedMessageId,
    });
  } catch (err) {
    console.warn("[PercModernUI] @mkd/language configure failed", err);
  }
}

/** Tear down triggers and observers (tests / disable). */
export function destroyMkdLanguage(): void {
  if (!handle) {
    return;
  }
  try {
    handle.destroy();
  } catch {
    // ignore
  }
  handle = null;
  lastLocaleRef = undefined;
}

/** Test helper — clear module state. */
export function __resetMkdLanguageForTests(): void {
  destroyMkdLanguage();
}

function parseQueryFlag(search: string | undefined, name: string): boolean | null {
  if (!search || search === "?") {
    return null;
  }
  try {
    const params = new URLSearchParams(
      search.startsWith("?") ? search.slice(1) : search,
    );
    if (!params.has(name)) {
      return null;
    }
    const raw = (params.get(name) ?? "").trim().toLowerCase();
    if (raw === "" || raw === "1" || raw === "true" || raw === "yes") {
      return true;
    }
    if (raw === "0" || raw === "false" || raw === "no") {
      return false;
    }
    return true;
  } catch {
    return null;
  }
}
