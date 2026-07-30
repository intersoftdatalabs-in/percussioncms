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

package com.percussion.apibridge;

import com.percussion.i18n.PSLocale;
import com.percussion.i18n.PSLocaleFormat;
import com.percussion.rest.locales.ILocalesAdaptor;
import com.percussion.rest.locales.LocaleDetail;
import com.percussion.rest.locales.LocaleFormatSummary;
import com.percussion.rest.locales.LocaleSummary;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.system.utils.PSSiteManageBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Read-only CMS locale catalog aligned with recent RXLOCALE / RXLOCALEFORMAT model:
 *
 * <ul>
 *   <li>{@link PSLocale} — {@code LANGUAGESTRING}, display name, status, {@code ISBASE}
 *   <li>{@link PSLocaleFormat} — optional format profile keyed by language string (not LOCALEID)
 * </ul>
 *
 * <p>Mapping helpers are pure so unit tests need no Hibernate.
 */
@PSSiteManageBean
public class LocalesAdaptor implements ILocalesAdaptor {

  private static final Logger log = LogManager.getLogger(LocalesAdaptor.class);

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Locale create / edit / delete not supported via this API",
          "RXLOCALEFORMAT create / edit not supported via this API (read of exact row only)",
          "Format resolution chain (regional → base → en-us defaults) is runtime-only",
          "Auto-translation configuration not exposed via this API");

  private final Supplier<List<PSLocale>> localeLoader;
  private final Function<String, Optional<PSLocaleFormat>> formatByLang;
  private final Supplier<Set<String>> formatLanguageIndex;

  public LocalesAdaptor() {
    this(
        () ->
            PSCmsObjectMgrLocator.getObjectManager()
                .findAllLocales()
                .collect(Collectors.toList()),
        lang -> {
          IPSCmsObjectMgr mgr = PSCmsObjectMgrLocator.getObjectManager();
          return mgr.findLocaleFormatByLanguageString(lang);
        },
        () ->
            PSCmsObjectMgrLocator.getObjectManager()
                .findAllLocaleFormats()
                .map(PSLocaleFormat::getLanguageString)
                .filter(StringUtils::isNotBlank)
                .map(LocalesAdaptor::normalizeLanguageString)
                .collect(Collectors.toCollection(HashSet::new)));
  }

  /** Package-visible for unit tests. */
  LocalesAdaptor(
      Supplier<List<PSLocale>> localeLoader,
      Function<String, Optional<PSLocaleFormat>> formatByLang,
      Supplier<Set<String>> formatLanguageIndex) {
    this.localeLoader = localeLoader;
    this.formatByLang = formatByLang;
    this.formatLanguageIndex = formatLanguageIndex;
  }

  @Override
  public List<LocaleSummary> listLocales(URI baseUri) {
    // baseUri reserved for HATEOAS
    try {
      Set<String> formatLangs = formatLanguageIndex.get();
      return mapSummaries(localeLoader.get(), formatLangs);
    } catch (RuntimeException e) {
      log.warn("Failed to list CMS locales", e);
      throw e;
    }
  }

  @Override
  public LocaleDetail getLocale(URI baseUri, String idOrLang) {
    if (!isSafeLocaleKey(idOrLang)) {
      return null;
    }
    try {
      List<PSLocale> all = localeLoader.get();
      PSLocale found = resolveLocale(all, idOrLang.trim());
      if (found == null) {
        return null;
      }
      String lang = normalizeLanguageString(found.getLanguageString());
      PSLocaleFormat format =
          lang == null ? null : formatByLang.apply(lang).orElse(null);
      return toDetail(found, format);
    } catch (RuntimeException e) {
      log.warn("Failed to load CMS locale", e);
      throw e;
    }
  }

  /**
   * Language strings (BCP-47 style) or numeric locale ids. Reject path separators / traversal.
   */
  static boolean isSafeLocaleKey(String key) {
    if (StringUtils.isBlank(key)) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }

  /**
   * Normalize like {@link PSLocaleFormat#setLanguageString}: lower-case, {@code _} → {@code -}.
   */
  static String normalizeLanguageString(String lang) {
    if (lang == null) {
      return null;
    }
    String t = lang.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    return t.isEmpty() ? null : t;
  }

  /** Package-visible for unit tests. */
  static List<LocaleSummary> mapSummaries(List<PSLocale> locales, Set<String> formatLangs) {
    Set<String> formats = formatLangs != null ? formatLangs : Set.of();
    List<LocaleSummary> out = new ArrayList<>();
    if (locales == null) {
      return out;
    }
    for (PSLocale loc : locales) {
      if (loc == null) {
        continue;
      }
      try {
        out.add(toSummary(loc, formats));
      } catch (Exception e) {
        log.debug("Skipping locale {}: {}", loc.getLanguageString(), e.getMessage());
      }
    }
    out.sort(
        Comparator.comparing(
            LocaleSummary::getLanguageString,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  static PSLocale resolveLocale(List<PSLocale> locales, String idOrLang) {
    if (locales == null || StringUtils.isBlank(idOrLang)) {
      return null;
    }
    if (StringUtils.isNumeric(idOrLang)) {
      int id = Integer.parseInt(idOrLang);
      for (PSLocale loc : locales) {
        if (loc != null && loc.getLocaleId() == id) {
          return loc;
        }
      }
      return null;
    }
    String want = normalizeLanguageString(idOrLang);
    if (want == null) {
      return null;
    }
    for (PSLocale loc : locales) {
      if (loc == null) {
        continue;
      }
      String have = normalizeLanguageString(loc.getLanguageString());
      if (want.equals(have)) {
        return loc;
      }
    }
    return null;
  }

  static LocaleSummary toSummary(PSLocale loc, Set<String> formatLangs) {
    LocaleSummary s = new LocaleSummary();
    s.setId(loc.getLocaleId());
    s.setLanguageString(loc.getLanguageString());
    s.setLabel(loc.getDisplayName());
    s.setDescription(loc.getDescription());
    s.setStatus(mapStatus(loc.getStatus()));
    s.setBaseLocale(loc.isBaseLocale());
    String lang = normalizeLanguageString(loc.getLanguageString());
    s.setHasFormatProfile(lang != null && formatLangs.contains(lang));
    return s;
  }

  static LocaleDetail toDetail(PSLocale loc, PSLocaleFormat format) {
    LocaleDetail d = new LocaleDetail();
    d.setId(loc.getLocaleId());
    d.setLanguageString(loc.getLanguageString());
    d.setLabel(loc.getDisplayName());
    d.setDescription(loc.getDescription());
    d.setStatus(mapStatus(loc.getStatus()));
    d.setBaseLocale(loc.isBaseLocale());
    d.setFormat(toFormatSummary(format));
    d.setHasFormatProfile(format != null);
    d.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return d;
  }

  static LocaleFormatSummary toFormatSummary(PSLocaleFormat format) {
    if (format == null) {
      return null;
    }
    LocaleFormatSummary f = new LocaleFormatSummary();
    f.setLanguageString(format.getLanguageString());
    f.setTextDir(format.getTextDir());
    f.setDatePattern(format.getDatePattern());
    f.setTimePattern(format.getTimePattern());
    f.setDateTimePattern(format.getDateTimePattern());
    f.setDecimalSep(format.getDecimalSep());
    f.setGroupingSep(format.getGroupingSep());
    f.setCurrencyCode(format.getCurrencyCode());
    f.setCurrencyPattern(format.getCurrencyPattern());
    f.setFirstDayOfWeek(format.getFirstDayOfWeek());
    f.setMeasurementSystem(format.getMeasurementSystem());
    f.setDefaultTz(format.getDefaultTz());
    f.setNumberingSystem(format.getNumberingSystem());
    f.setCalendar(format.getCalendar());
    return f;
  }

  static String mapStatus(int status) {
    if (status >= 0 && status < PSLocale.STATUS_ENUM.length) {
      return PSLocale.STATUS_ENUM[status];
    }
    return "unknown";
  }
}
