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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a {@link PSLocaleFormat} for a language tag by walking exact → language-only → {@code
 * en-us}, merging non-null fields from each step, then filling remaining gaps from product floor
 * defaults.
 *
 * <p>Does not require a format row for customer-invented locales.
 */
public final class PSLocaleFormatResolver {

  private PSLocaleFormatResolver() {}

  /**
   * Resolve format for {@code language} using the supplied profiles (keyed by normalized code).
   *
   * @param language requested BCP-47 tag; null/blank → default lang
   * @param byCode map of stored profiles; may be null/empty
   * @return fully populated profile (never null); languageString set to normalized request
   */
  public static PSLocaleFormat resolve(String language, Map<String, PSLocaleFormat> byCode) {
    String normalized = normalize(language);
    if (normalized == null) {
      normalized = normalize(PSI18nUtils.DEFAULT_LANG);
    }
    if (normalized == null) {
      normalized = "en-us";
    }

    Map<String, PSLocaleFormat> catalog = byCode != null ? byCode : Map.of();
    PSLocaleFormat result = new PSLocaleFormat(normalized);

    for (String tag : lookupChain(normalized)) {
      PSLocaleFormat row = catalog.get(tag);
      if (row != null) {
        mergeMissing(result, row);
      }
    }
    mergeMissing(result, PSLocaleFormatDefaults.productFloor());
    // Always report the requested (normalized) code on the result.
    result.setLanguageString(normalized);
    return result;
  }

  /** Convenience when you have a collection instead of a map. */
  public static PSLocaleFormat resolve(String language, Collection<PSLocaleFormat> rows) {
    Map<String, PSLocaleFormat> map = new LinkedHashMap<>();
    if (rows != null) {
      for (PSLocaleFormat row : rows) {
        if (row != null && row.getLanguageString() != null) {
          map.put(normalize(row.getLanguageString()), row);
        }
      }
    }
    return resolve(language, map);
  }

  /**
   * Lookup chain: exact tag, then language-only (if regional), then {@code en-us}. Duplicates
   * omitted.
   */
  public static List<String> lookupChain(String language) {
    LinkedHashSet<String> chain = new LinkedHashSet<>();
    String normalized = normalize(language);
    if (normalized == null || normalized.isEmpty()) {
      chain.add(normalize(PSI18nUtils.DEFAULT_LANG));
      return new ArrayList<>(chain);
    }
    chain.add(normalized);
    int dash = normalized.indexOf('-');
    if (dash > 0) {
      chain.add(normalized.substring(0, dash));
    }
    String def = normalize(PSI18nUtils.DEFAULT_LANG);
    if (def != null) {
      chain.add(def);
    }
    return new ArrayList<>(chain);
  }

  public static String normalize(String tag) {
    if (tag == null) {
      return null;
    }
    String t = tag.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    return t.isEmpty() ? null : t;
  }

  /**
   * Copy any null/empty field on {@code target} from {@code source}. Does not overwrite already-set
   * fields on target.
   */
  static void mergeMissing(PSLocaleFormat target, PSLocaleFormat source) {
    if (target == null || source == null) {
      return;
    }
    if (isBlank(target.getTextDir())) {
      target.setTextDir(source.getTextDir());
    }
    if (isBlank(target.getDatePattern())) {
      target.setDatePattern(source.getDatePattern());
    }
    if (isBlank(target.getTimePattern())) {
      target.setTimePattern(source.getTimePattern());
    }
    if (isBlank(target.getDateTimePattern())) {
      target.setDateTimePattern(source.getDateTimePattern());
    }
    if (isBlank(target.getDecimalSep())) {
      target.setDecimalSep(source.getDecimalSep());
    }
    if (isBlank(target.getGroupingSep())) {
      target.setGroupingSep(source.getGroupingSep());
    }
    if (isBlank(target.getCurrencyCode())) {
      target.setCurrencyCode(source.getCurrencyCode());
    }
    if (isBlank(target.getCurrencyPattern())) {
      target.setCurrencyPattern(source.getCurrencyPattern());
    }
    if (target.getFirstDayOfWeek() == null) {
      target.setFirstDayOfWeek(source.getFirstDayOfWeek());
    }
    if (isBlank(target.getMeasurementSystem())) {
      target.setMeasurementSystem(source.getMeasurementSystem());
    }
    if (isBlank(target.getDefaultTz())) {
      target.setDefaultTz(source.getDefaultTz());
    }
    if (isBlank(target.getNumberingSystem())) {
      target.setNumberingSystem(source.getNumberingSystem());
    }
    if (isBlank(target.getCalendar())) {
      target.setCalendar(source.getCalendar());
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isEmpty();
  }
}
