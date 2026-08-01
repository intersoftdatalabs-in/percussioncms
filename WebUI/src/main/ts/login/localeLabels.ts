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
 * Stable locale option labels for the login locale dropdown.
 *
 * <p>Each option is labeled with its own <em>endonym</em> (native-language
 * name), e.g. {@code "fr-fr - français (France)"}, {@code "de-de - Deutsch
 * (Deutschland)"}, {@code "es - español"}. Labels do <strong>not</strong>
 * re-translate into the currently selected UI locale — changing the
 * dropdown only changes application chrome, not the names of every locale
 * option (see GH-1608).</p>
 *
 * <p>Ship locales use a static endonym map so the list looks clean even when
 * {@link Intl.DisplayNames} is missing, incomplete, or returns the English
 * server {@code displayName}. Other / customer locales still prefer
 * {@link Intl.DisplayNames}, then the server fallback.</p>
 */

const viewerCache: Map<string, Intl.DisplayNames | null> = new Map();

/**
 * Endonym text after the {@code "code - "} prefix for product-shipped locales.
 * Keys are normalized BCP-47 (lowercase hyphen). Keep in sync with RXLOCALE
 * seed + login filter matrix.
 */
export const SHIP_LOCALE_ENDONYMS: Readonly<Record<string, string>> = {
  ar: "العربية",
  bn: "বাংলা",
  de: "Deutsch",
  "de-at": "Deutsch (Österreich)",
  "de-ch": "Deutsch (Schweiz)",
  "de-de": "Deutsch (Deutschland)",
  "de-li": "Deutsch (Liechtenstein)",
  "de-lu": "Deutsch (Luxemburg)",
  "en-gb": "English (United Kingdom)",
  "en-us": "English (United States)",
  es: "español",
  "es-ar": "español (Argentina)",
  "es-bo": "español (Bolivia)",
  "es-cl": "español (Chile)",
  "es-co": "español (Colombia)",
  "es-cr": "español (Costa Rica)",
  "es-ec": "español (Ecuador)",
  "es-es": "español (España)",
  "es-hn": "español (Honduras)",
  "es-mx": "español (México)",
  "es-ni": "español (Nicaragua)",
  "es-pa": "español (Panamá)",
  "es-pe": "español (Perú)",
  "es-pr": "español (Puerto Rico)",
  "es-sv": "español (El Salvador)",
  "es-uy": "español (Uruguay)",
  "es-ve": "español (Venezuela)",
  fr: "français",
  "fr-be": "français (Belgique)",
  "fr-ca": "français (Canada)",
  "fr-ch": "français (Suisse)",
  "fr-fr": "français (France)",
  "fr-lu": "français (Luxembourg)",
  "fr-us": "Français cadien",
  lou: "Kréyòl",
  he: "עברית",
  "he-il": "עברית (ישראל)",
  hi: "हिन्दी",
  "hi-in": "हिन्दी (भारत)",
  it: "italiano",
  "it-ch": "italiano (Svizzera)",
  "it-it": "italiano (Italia)",
  "ja-jp": "日本語 (日本)",
  nl: "Nederlands",
  "nl-be": "Nederlands (België)",
  "nl-nl": "Nederlands (Nederland)",
  pl: "polski",
  pt: "português",
  "pt-br": "português (Brasil)",
  "pt-pt": "português (Portugal)",
  ru: "русский",
  sv: "svenska",
  te: "తెలుగు",
  tr: "Türkçe",
  "tr-tr": "Türkçe (Türkiye)",
  "zh-cn": "中文 (简体)",
  "zh-tw": "中文 (繁體)",
};

/**
 * Normalize a locale tag to lowercase BCP-47 with hyphen separator.
 * Mirrors {@code com.percussion.i18n.PSTmxResourceBundle.normalizeLang}.
 */
export function normalizeTag(code: string): string {
  if (!code) {
    return "";
  }
  return code.trim().toLowerCase().replace(/_/g, "-");
}

/**
 * Representative ISO 3166-1 alpha-2 region for language-only (base) tags, and
 * special overrides where the language subtag is not a country code.
 * Used to pick a flag emoji for the login locale dropdown.
 */
const LANGUAGE_DEFAULT_REGION: Readonly<Record<string, string>> = {
  ar: "SA",
  bn: "BD",
  de: "DE",
  en: "US",
  es: "ES",
  fr: "FR",
  he: "IL",
  hi: "IN",
  it: "IT",
  ja: "JP",
  lou: "US", // Louisiana Creole — product uses US flag for fr-us / lou
  nl: "NL",
  pl: "PL",
  pt: "PT",
  ru: "RU",
  sv: "SE",
  te: "IN",
  tr: "TR",
  zh: "CN",
};

/** Explicit region for full tags when the trailing subtag is not a country. */
const LOCALE_REGION_OVERRIDES: Readonly<Record<string, string>> = {
  "zh-cn": "CN",
  "zh-tw": "TW",
  "fr-us": "US",
  "en-gb": "GB",
  "en-us": "US",
  "pt-br": "BR",
  "pt-pt": "PT",
  "he-il": "IL",
  "hi-in": "IN",
  "ja-jp": "JP",
  "tr-tr": "TR",
};

/**
 * Resolve the ISO 3166-1 alpha-2 region used for a flag icon (uppercase).
 * Prefer an explicit regional subtag ({@code fr-fr} → {@code FR}); fall back
 * to a representative country for base language tags ({@code es} → {@code ES}).
 * Returns empty string when no region can be determined.
 */
export function localeRegionCode(code: string): string {
  const norm = normalizeTag(code);
  if (!norm) {
    return "";
  }
  const override = LOCALE_REGION_OVERRIDES[norm];
  if (override) {
    return override;
  }
  const parts = norm.split("-");
  if (parts.length >= 2) {
    const region = parts[parts.length - 1];
    if (region.length === 2 && /^[a-z]{2}$/i.test(region)) {
      return region.toUpperCase();
    }
  }
  const lang = parts[0];
  const defaultRegion = LANGUAGE_DEFAULT_REGION[lang];
  return defaultRegion ? defaultRegion.toUpperCase() : "";
}

/**
 * Convert an ISO 3166-1 alpha-2 region code to a Unicode regional-indicator
 * flag emoji (e.g. {@code "FR"} → 🇫🇷). Returns empty string when the code is
 * not two Latin letters. Prefer {@link LocaleFlag} SVG icons in the UI; this
 * remains as a text fallback for environments without SVG support.
 */
export function regionToFlagEmoji(region: string): string {
  if (!region || region.length !== 2) {
    return "";
  }
  const upper = region.toUpperCase();
  if (!/^[A-Z]{2}$/.test(upper)) {
    return "";
  }
  // Regional Indicator Symbol Letter A = U+1F1E6
  const BASE = 0x1f1e6;
  const cp0 = BASE + (upper.charCodeAt(0) - 65);
  const cp1 = BASE + (upper.charCodeAt(1) - 65);
  return String.fromCodePoint(cp0, cp1);
}

/**
 * Resolve a flag emoji for a BCP-47 locale tag. Prefer SVG via {@code LocaleFlag}
 * in the custom dropdown; emoji is a degraded / text-only fallback.
 */
export function localeFlagEmoji(code: string): string {
  return regionToFlagEmoji(localeRegionCode(code));
}

/**
 * Text option label with optional emoji prefix (legacy / plain-text). The
 * custom login dropdown uses SVG flags + {@link localeLabel} instead.
 */
export function localeOptionLabel(
  code: string,
  viewer: string,
  fallback: string,
): string {
  const label = localeLabel(code, viewer, fallback);
  const flag = localeFlagEmoji(code);
  return flag ? `${flag} ${label}` : label;
}

function getDisplayNames(
  viewer: string,
  type: "language" | "region",
): Intl.DisplayNames | null {
  if (typeof Intl === "undefined" || typeof Intl.DisplayNames !== "function") {
    return null;
  }
  const tag = normalizeTag(viewer);
  if (!tag) {
    return null;
  }
  const cacheKey = type === "language" ? tag : `${tag}::region`;
  const cached = viewerCache.get(cacheKey);
  if (cached !== undefined) {
    return cached;
  }
  let dn: Intl.DisplayNames | null;
  try {
    dn = new Intl.DisplayNames([tag], { type });
  } catch {
    dn = null;
  }
  viewerCache.set(cacheKey, dn);
  return dn;
}

function safeOf(
  dn: Intl.DisplayNames | null,
  code: string,
): string | undefined {
  if (!dn) {
    return undefined;
  }
  try {
    const v = dn.of(code);
    if (typeof v !== "string" || v.length === 0) {
      return undefined;
    }
    // Intl.DisplayNames returns the code itself for unknown tags
    // (e.g. dn.of('zz') === 'zz') — treat that as unknown.
    if (v.toLowerCase() === code.toLowerCase()) {
      return undefined;
    }
    return v;
  } catch {
    return undefined;
  }
}

/**
 * Render the dropdown label for a locale option:
 * {@code "<code> - <Endonym>"}.
 *
 * <p>Endonyms are resolved with {@link Intl.DisplayNames} in the option's
 * own language so the list stays stable when the selected UI locale
 * changes. The optional {@code viewer} argument is retained for call-site
 * compatibility and is intentionally unused.</p>
 *
 * @param code     the option's locale tag (e.g. {@code "fr-fr"} or {@code "es"})
 * @param _viewer  unused; kept so existing call sites keep compiling
 * @param fallback server-provided English display name; used when
 *                 {@link Intl.DisplayNames} is unavailable or the code is unknown
 */
export function localeLabel(
  code: string,
  _viewer: string,
  fallback: string,
): string {
  const norm = normalizeTag(code);
  const codeOut = norm || code;
  const fallbackOut = fallback || codeOut;

  // Prefer curated ship-matrix endonyms so the login list is complete and
  // stable across browsers / incomplete ICU data.
  const ship = SHIP_LOCALE_ENDONYMS[norm];
  if (ship) {
    return `${codeOut} - ${ship}`;
  }

  const parts = norm.split("-");
  const langCode = parts[0] || norm;
  const regionCode = parts.length > 1 ? parts[parts.length - 1] : "";

  // Customer / unknown codes: endonym via Intl when available.
  const langDN = getDisplayNames(langCode, "language");
  const langName = safeOf(langDN, langCode);
  if (!langName) {
    return `${codeOut} - ${fallbackOut}`;
  }

  if (regionCode) {
    const regionDN = getDisplayNames(langCode, "region");
    const regionName = safeOf(regionDN, regionCode.toUpperCase());
    if (regionName) {
      return `${codeOut} - ${langName} (${regionName})`;
    }
  }
  return `${codeOut} - ${langName}`;
}

/** Test-only: clear the module-level viewer cache between unit tests. */
export function __resetLocaleLabelsCache(): void {
  viewerCache.clear();
}
