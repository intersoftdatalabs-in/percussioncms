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
package com.percussion.i18n;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure rewrite map for one-time upgrade migration of persisted locale codes to the canonical BCP-47
 * lowercase-hyphen matrix (GH-1547 / i18n recovery plan D1/D6).
 *
 * <p>Two surfaces share the same normalizer but differ in regional promotion:
 *
 * <ul>
 *   <li><strong>sys_lang</strong> (user session preference in {@code PSX_PERSISTEDPROPERTYVALUES}):
 *       {@code hi} → {@code hi-in}; {@code es} stays {@code es} (base locale is still valid for
 *       login); other legacy tags collapse via {@link #normalize(String)}.
 *   <li><strong>content locale</strong> ({@code CONTENTSTATUS.LOCALE}; issue text may say {@code
 *       CT_LOCALE.LOCALE}): seeded {@code es} → {@code es-es} per recovery plan D1; other tags only
 *       normalize.
 * </ul>
 *
 * <p>Never invents codes outside the normalize + explicit promotion map. Idempotent: applying the
 * rewrite to an already-canonical value returns the same string.
 */
public final class PSLocaleCodeRewrite {

  /** Canonical Hindi regional promoted from bare {@code hi} for {@code sys_lang}. */
  public static final String HI_IN = "hi-in";

  /** Canonical Spanish (Spain) regional promoted from bare {@code es} for content locale. */
  public static final String ES_ES = "es-es";

  /** Language-only Spanish base (valid for sys_lang; not rewritten). */
  public static final String ES = "es";

  /** Language-only Hindi base (promoted for sys_lang). */
  public static final String HI = "hi";

  private PSLocaleCodeRewrite() {}

  /**
   * Normalizes a locale tag to BCP-47 lowercase hyphen form. Null-safe: {@code null} stays {@code
   * null}; blank becomes empty after trim.
   *
   * @param raw raw locale tag (may use underscore or mixed case)
   * @return normalized tag, or {@code null} when {@code raw} is {@code null}
   */
  public static String normalize(String raw) {
    if (raw == null) {
      return null;
    }
    return raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  /**
   * Rewrites a persisted {@code sys_lang} property value for upgrade.
   *
   * <ul>
   *   <li>{@code hi} → {@code hi-in}
   *   <li>{@code es} stays {@code es}
   *   <li>other codes → {@link #normalize(String)}
   * </ul>
   *
   * @param raw stored value, may be {@code null}
   * @return rewritten value; {@code null} when input is {@code null}
   */
  public static String rewriteSysLang(String raw) {
    if (raw == null) {
      return null;
    }
    String normalized = normalize(raw);
    if (HI.equals(normalized)) {
      return HI_IN;
    }
    return normalized;
  }

  /**
   * Rewrites a content-item locale ({@code CONTENTSTATUS.LOCALE}) for upgrade.
   *
   * <ul>
   *   <li>{@code es} → {@code es-es}
   *   <li>other codes → {@link #normalize(String)}
   * </ul>
   *
   * @param raw stored value, may be {@code null}
   * @return rewritten value; {@code null} when input is {@code null}
   */
  public static String rewriteContentLocale(String raw) {
    if (raw == null) {
      return null;
    }
    String normalized = normalize(raw);
    if (ES.equals(normalized)) {
      return ES_ES;
    }
    return normalized;
  }

  /**
   * @param raw stored value
   * @return {@code true} when {@link #rewriteSysLang(String)} would change the stored form
   */
  public static boolean sysLangNeedsRewrite(String raw) {
    if (raw == null) {
      return false;
    }
    return !Objects.equals(raw, rewriteSysLang(raw));
  }

  /**
   * @param raw stored value
   * @return {@code true} when {@link #rewriteContentLocale(String)} would change the stored form
   */
  public static boolean contentLocaleNeedsRewrite(String raw) {
    if (raw == null) {
      return false;
    }
    return !Objects.equals(raw, rewriteContentLocale(raw));
  }

  /**
   * Optional wrapper for callers that prefer empty over null.
   *
   * @param raw stored value
   * @return optional rewritten sys_lang, empty when raw is null/blank after normalize
   */
  public static Optional<String> rewriteSysLangOptional(String raw) {
    String rewritten = rewriteSysLang(raw);
    if (rewritten == null || rewritten.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(rewritten);
  }
}
