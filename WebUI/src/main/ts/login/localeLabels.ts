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
  "de-de": "Deutsch (Deutschland)",
  "en-gb": "English (United Kingdom)",
  "en-us": "English (United States)",
  es: "español",
  "es-cl": "español (Chile)",
  "es-es": "español (España)",
  "es-mx": "español (México)",
  "fr-ca": "français (Canada)",
  "fr-fr": "français (France)",
  hi: "हिन्दी",
  "hi-in": "हिन्दी (भारत)",
  "it-it": "italiano (Italia)",
  "ja-jp": "日本語 (日本)",
  "nl-nl": "Nederlands (Nederland)",
  "pt-br": "português (Brasil)",
  "pt-pt": "português (Portugal)",
  te: "తెలుగు",
  "tr-tr": "Türkçe (Türkiye)",
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
