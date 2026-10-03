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
package com.percussion.share.dao.impl;

import com.percussion.i18n.PSLocale;
import com.percussion.pathmanagement.data.PSFolderLocaleCatalog;
import com.percussion.pathmanagement.data.PSFolderLocaleChoice;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds the folder locale catalog. Codes are the language string stored on the folder
 * ({@code en-us}). Inactive locales, blanks, and codes that are not a language token are omitted
 * (#5106).
 */
public final class FolderLocaleCatalogRules {
  private static final Pattern CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");

  private FolderLocaleCatalogRules() {}

  /** Active locales only. Inactive and null rows are dropped before {@link #fromChoices}. */
  public static List<PSFolderLocaleChoice> choicesFromLocales(List<PSLocale> locales) {
    List<PSFolderLocaleChoice> raw = new ArrayList<>();
    if (locales == null) {
      return raw;
    }
    for (PSLocale locale : locales) {
      if (locale == null || locale.getStatus() != PSLocale.STATUS_ACTIVE) {
        continue;
      }
      PSFolderLocaleChoice choice = new PSFolderLocaleChoice();
      choice.setCode(locale.getLanguageString());
      choice.setName(locale.getDisplayName());
      raw.add(choice);
    }
    return raw;
  }

  public static PSFolderLocaleCatalog fromChoices(List<PSFolderLocaleChoice> raw) {
    Map<String, PSFolderLocaleChoice> byCode = new LinkedHashMap<>();
    if (raw != null) {
      for (PSFolderLocaleChoice choice : raw) {
        PSFolderLocaleChoice cleaned = clean(choice);
        if (cleaned != null) {
          byCode.putIfAbsent(key(cleaned.getCode()), cleaned);
        }
      }
    }
    List<PSFolderLocaleChoice> choices = new ArrayList<>(byCode.values());
    choices.sort(
        Comparator.comparing(PSFolderLocaleChoice::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(PSFolderLocaleChoice::getCode, String.CASE_INSENSITIVE_ORDER));
    PSFolderLocaleCatalog catalog = new PSFolderLocaleCatalog();
    catalog.setChoices(choices);
    return catalog;
  }

  public static boolean contains(PSFolderLocaleCatalog catalog, String code) {
    return nameOf(catalog, code) != null;
  }

  /** Display name for a catalog code, or {@code null} when the code is not listed. */
  public static String nameOf(PSFolderLocaleCatalog catalog, String code) {
    if (catalog == null || catalog.getChoices() == null) {
      return null;
    }
    String wanted = key(code);
    if (wanted.isEmpty()) {
      return null;
    }
    for (PSFolderLocaleChoice choice : catalog.getChoices()) {
      if (choice != null && wanted.equals(key(choice.getCode()))) {
        String name = trim(choice.getName());
        return name.isEmpty() ? trim(choice.getCode()) : name;
      }
    }
    return null;
  }

  /** True when both codes are non-blank and equal ignoring case. */
  public static boolean sameCode(String left, String right) {
    String a = key(left);
    String b = key(right);
    return !a.isEmpty() && a.equals(b);
  }

  private static PSFolderLocaleChoice clean(PSFolderLocaleChoice choice) {
    if (choice == null) {
      return null;
    }
    String code = trim(choice.getCode());
    if (!CODE.matcher(code).matches()) {
      return null;
    }
    String name = trim(choice.getName());
    if (name.isEmpty()) {
      name = code;
    }
    PSFolderLocaleChoice out = new PSFolderLocaleChoice();
    out.setCode(code);
    out.setName(name);
    return out;
  }

  private static String key(String value) {
    return trim(value).toLowerCase(Locale.ROOT);
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
