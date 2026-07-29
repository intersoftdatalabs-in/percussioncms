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
package com.percussion.i18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Server-side helpers for the login locale dropdown and default selection.
 *
 * <p>Login shows active regional locales always. A base / language-only locale
 * ({@link PSLocale#isBaseLocale()}) is shown only when no active regional sibling
 * exists for the same language family (e.g. hide {@code es} when {@code es-es}
 * is active; show {@code ar} when no {@code ar-*} regionals exist).
 *
 * <p>When no locale is requested, the product default is {@link
 * PSI18nUtils#DEFAULT_LANG} ({@code en-us}).
 */
public final class PSLocaleLoginSelection {

  private PSLocaleLoginSelection() {}

  /**
   * Filter locales for the login dropdown.
   *
   * @param all all locales from the repository; may be {@code null}
   * @return active locales that should appear on login, never {@code null}; order preserved from
   *     the input iteration
   */
  public static List<PSLocale> forLoginDropdown(Iterable<PSLocale> all) {
    List<PSLocale> active = new ArrayList<>();
    if (all == null) {
      return active;
    }
    for (PSLocale loc : all) {
      if (loc != null && loc.getStatus() == PSLocale.STATUS_ACTIVE) {
        active.add(loc);
      }
    }

    Set<String> activeTags = new HashSet<>();
    for (PSLocale loc : active) {
      String tag = normalizeTag(loc.getLanguageString());
      if (tag != null) {
        activeTags.add(tag);
      }
    }

    List<PSLocale> result = new ArrayList<>();
    for (PSLocale loc : active) {
      if (shouldShowOnLogin(loc, activeTags)) {
        result.add(loc);
      }
    }
    return result;
  }

  /**
   * Resolve the selected login locale: prefer {@code requested} if it is in the allowed login list,
   * else system language if allowed, else {@code en-us}.
   *
   * @param requested candidate from the request (may be null/empty)
   * @param systemLanguage system default language (may be null/empty)
   * @param loginLocales already-filtered login list (may be null)
   * @return never null/empty; defaults to {@link PSI18nUtils#DEFAULT_LANG}
   */
  public static String resolveSelectedLocale(
      String requested, String systemLanguage, Collection<PSLocale> loginLocales) {
    Set<String> allowed = new HashSet<>();
    if (loginLocales != null) {
      for (PSLocale loc : loginLocales) {
        if (loc != null && loc.getLanguageString() != null) {
          allowed.add(normalizeTag(loc.getLanguageString()));
        }
      }
    }

    String req = normalizeTag(requested);
    if (req != null && (allowed.isEmpty() || allowed.contains(req))) {
      return req;
    }
    String sys = normalizeTag(systemLanguage);
    if (sys != null && (allowed.isEmpty() || allowed.contains(sys))) {
      return sys;
    }
    String def = normalizeTag(PSI18nUtils.DEFAULT_LANG);
    return def != null ? def : "en-us";
  }

  /**
   * Whether a single active locale should appear on login given the set of all active language
   * tags.
   */
  static boolean shouldShowOnLogin(PSLocale loc, Set<String> activeTags) {
    if (loc == null || loc.getStatus() != PSLocale.STATUS_ACTIVE) {
      return false;
    }
    if (!loc.isBaseLocale()) {
      return true;
    }
    String base = normalizeTag(loc.getLanguageString());
    if (base == null || base.isEmpty()) {
      return false;
    }
    String prefix = base + "-";
    for (String tag : activeTags) {
      if (tag != null && tag.startsWith(prefix)) {
        return false;
      }
    }
    return true;
  }

  /** Lowercase BCP-47 with hyphens; null/blank → null. */
  static String normalizeTag(String tag) {
    if (tag == null) {
      return null;
    }
    String t = tag.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    return t.isEmpty() ? null : t;
  }
}
