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
 * Server-provided bootstrap for the React login page (XSS-safe JSON text node).
 */
export interface LoginLocaleOption {
  name: string;
  displayName: string;
}

/**
 * Server-resolved locale format profile (RXLOCALEFORMAT + inheritance).
 * Keyed by language string; used for dir, date/currency patterns in the UI.
 */
export interface LocaleFormatBootstrap {
  languageString: string;
  textDir?: "ltr" | "rtl" | string;
  datePattern?: string;
  timePattern?: string;
  dateTimePattern?: string;
  decimalSep?: string;
  groupingSep?: string;
  currencyCode?: string;
  currencyPattern?: string;
  firstDayOfWeek?: number;
  measurementSystem?: string;
  defaultTz?: string;
  numberingSystem?: string;
  calendar?: string;
}

export interface LoginBootstrap {
  /** Available CMS locales for the login form */
  locales: LoginLocaleOption[];
  /** Selected locale name (e.g. en-us) */
  selectedLocale?: string;
  /** Prefill username when re-rendering after error */
  username?: string;
  /** Server error message; already intended for display (client still escapes in React) */
  error?: string | null;
  /** HTML autocomplete attribute value for username/password */
  autocomplete?: "on" | "off";
  /** Post-login path-absolute redirect (query contract) */
  defaultRedirect: string;
  /** CSRF token field name (OWASP CSRFGuard) */
  csrfTokenName: string;
  /** CSRF token value */
  csrfTokenValue: string;
  /** Form action path (default /login) */
  formAction?: string;
  /** Resolved format for selected locale (dir, date, currency, …) */
  localeFormat?: LocaleFormatBootstrap | null;
}

/** Minimal authenticated SPA landing bootstrap */
export interface SpaLandingBootstrap {
  userName?: string;
  locale?: string;
  entry?: string;
  localeFormat?: LocaleFormatBootstrap | null;
}
