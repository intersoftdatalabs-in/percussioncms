/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * <p>Alpha: opt-in via query or localStorage; submissions use the library
 * no-op client (optional debug log). Library design and GCM live outside this
 * monorepo — do not expand this module into product-owned correction UI.</p>
 *
 * <p>Enable (developer / experiment):</p>
 * <ul>
 *   <li>Query: {@code ?mkdLang=1} (or {@code 0} to force off)</li>
 *   <li>localStorage: {@code perc-mkd-lang} = {@code 1} | {@code 0}</li>
 * </ul>
 */

import {
  init,
  NoopSubmissionClient,
  type MkdLanguageHandle,
} from "@mkd/language";
import { I18N_KEY_ATTR } from "./i18nDom";

/** localStorage key for the opt-in experiment flag. */
export const MKD_LANG_STORAGE_KEY = "perc-mkd-lang";

/** Query parameter for the opt-in experiment flag. */
export const MKD_LANG_QUERY_PARAM = "mkdLang";

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

  try {
    if (handle) {
      handle.configure({
        locale: options.locale,
        uiLocale: options.uiLocale ?? options.locale,
        getUserEmail: options.getUserEmail,
        debug,
        client: new NoopSubmissionClient(debug),
        messageIdAttr: I18N_KEY_ATTR,
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
      client: new NoopSubmissionClient(debug),
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
      client: new NoopSubmissionClient(debug),
      messageIdAttr: I18N_KEY_ATTR,
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
