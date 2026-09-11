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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import com.percussion.i18n.PSLocale;
import com.percussion.i18n.PSLocaleFormat;
import com.percussion.rest.locales.ILocalesAdaptor;
import com.percussion.rest.locales.LocaleDesignLockException;
import com.percussion.rest.locales.LocaleDetail;
import com.percussion.rest.locales.LocaleFormatSummary;
import com.percussion.rest.locales.LocaleNotFoundException;
import com.percussion.rest.locales.LocaleSummary;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.utils.exceptions.PSORMException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * CMS locale catalog for Developer REST (CD-18).
 *
 * <p>Workbench parity for locale definitions: {@link IPSContentDesignWs#findLocales} / {@link
 * IPSContentDesignWs#loadLocales} / {@link IPSContentDesignWs#createLocales} / {@link
 * IPSContentDesignWs#saveLocales} / {@link IPSContentDesignWs#deleteLocales} (same design web
 * service SOAP uses). Writes acquire a design lock for the request and release it on save. Format
 * profiles ({@link PSLocaleFormat} / RXLOCALEFORMAT) have no design-WS twin — read and write go
 * through {@link IPSCmsObjectMgr} keyed by BCP-47 language string (not LOCALEID). Format writes
 * ride locale POST/PUT when {@code hasFormatProfile} or {@code format} is present so omitted
 * fields do not clear an existing row.
 *
 * <p>Admin only for create/update/delete — same {@link IPSUserService#isAdminUser} gate as other
 * design catalog writes. GET remains a catalog read.
 */
@PSSiteManageBean
public class LocalesAdaptor implements ILocalesAdaptor {

  private static final Logger log = LogManager.getLogger(LocalesAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to create, update, or delete locales";

  private static final List<String> DESIGN_GAPS =
      List.of("Format resolution chain (regional → base → en-us defaults) is runtime-only");

  private static final String LANGUAGE_PATTERN = "[a-z]{2,8}(-[a-z0-9]{1,8})*";

  private final IPSContentDesignWs designWs;
  private final Function<String, Optional<PSLocaleFormat>> formatByLang;
  private final Supplier<Set<String>> formatLanguageIndex;
  private final BooleanSupplier adminChecker;
  private final Consumer<PSLocaleFormat> formatSaver;
  private final Consumer<String> formatDeleter;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public LocalesAdaptor() {
    this(
        PSContentWsLocator.getContentDesignWebservice(),
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
                .collect(Collectors.toCollection(HashSet::new)),
        null,
        fmt -> {
          try {
            PSCmsObjectMgrLocator.getObjectManager().saveLocaleFormat(fmt);
          } catch (PSORMException e) {
            throw new IllegalStateException("Failed to save locale format", e);
          }
        },
        lang -> {
          try {
            PSCmsObjectMgrLocator.getObjectManager().deleteLocaleFormat(lang);
          } catch (PSORMException e) {
            throw new IllegalStateException("Failed to delete locale format", e);
          }
        });
  }

  /** Package-visible for unit tests. {@code null} adminChecker uses {@link #isCurrentUserAdmin()}. */
  LocalesAdaptor(
      IPSContentDesignWs designWs,
      Function<String, Optional<PSLocaleFormat>> formatByLang,
      Supplier<Set<String>> formatLanguageIndex) {
    this(designWs, formatByLang, formatLanguageIndex, null);
  }

  /** Package-visible for unit tests that inject a fake Admin gate. */
  LocalesAdaptor(
      IPSContentDesignWs designWs,
      Function<String, Optional<PSLocaleFormat>> formatByLang,
      Supplier<Set<String>> formatLanguageIndex,
      BooleanSupplier adminChecker) {
    this(designWs, formatByLang, formatLanguageIndex, adminChecker, fmt -> {}, lang -> {});
  }

  /** Package-visible for unit tests that inject format save/delete. */
  LocalesAdaptor(
      IPSContentDesignWs designWs,
      Function<String, Optional<PSLocaleFormat>> formatByLang,
      Supplier<Set<String>> formatLanguageIndex,
      BooleanSupplier adminChecker,
      Consumer<PSLocaleFormat> formatSaver,
      Consumer<String> formatDeleter) {
    this.designWs = designWs;
    this.formatByLang = formatByLang;
    this.formatLanguageIndex = formatLanguageIndex;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.formatSaver = formatSaver != null ? formatSaver : fmt -> {};
    this.formatDeleter = formatDeleter != null ? formatDeleter : lang -> {};
  }

  @Override
  public List<LocaleSummary> listLocales(URI baseUri) {
    try {
      Set<String> formatLangs = safeFormatIndex();
      return mapSummaries(loadAllLocalesViaDesignWs(), formatLangs);
    } catch (RuntimeException e) {
      log.warn("Failed to list CMS locales via content design WS", e);
      throw e;
    }
  }

  @Override
  public LocaleDetail getLocale(URI baseUri, String idOrLang) {
    if (!isSafeLocaleKey(idOrLang)) {
      return null;
    }
    try {
      List<PSLocale> all = loadAllLocalesViaDesignWs();
      PSLocale found = resolveLocale(all, idOrLang.trim());
      if (found == null) {
        return null;
      }
      String lang = normalizeLanguageString(found.getLanguageString());
      PSLocaleFormat format = lang == null ? null : formatByLang.apply(lang).orElse(null);
      return toDetail(found, format);
    } catch (RuntimeException e) {
      log.warn("Failed to load CMS locale via content design WS", e);
      throw e;
    }
  }

  @Override
  public LocaleDetail createLocale(URI baseUri, LocaleDetail body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String code = normalizeLanguageString(body.getLanguageString());
    if (code == null) {
      throw new IllegalArgumentException("languageString is required");
    }
    if (!isSafeLocaleKey(code) || !isValidLanguageString(code)) {
      throw new IllegalArgumentException("invalid languageString: " + body.getLanguageString());
    }
    if (StringUtils.isBlank(body.getLabel())) {
      throw new IllegalArgumentException("label is required");
    }
    validateFormatWrite(body);
    assertLanguageUnique(code);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSLocale> created =
          designWs.createLocales(
              Collections.singletonList(code),
              Collections.singletonList(body.getLabel().trim()),
              session,
              user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createLocales returned empty");
      }
      PSLocale loc = created.get(0);
      applyWritableFields(loc, body, false);
      designWs.saveLocales(Collections.singletonList(loc), true, session, user);
      persistFormatIfRequested(code, body);
      return reloadDetail(loc);
    } catch (WebApplicationException e) {
      throw e;
    } catch (LocaleDesignLockException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Locale already exists: " + code, 409);
      }
      throw e;
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("create", e);
    } catch (PSErrorException e) {
      throw mapCreateFailure(code, e);
    }
  }

  @Override
  public LocaleDetail updateLocale(URI baseUri, String idOrLang, LocaleDetail body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (StringUtils.isBlank(idOrLang) || !isSafeLocaleKey(idOrLang)) {
      throw new IllegalArgumentException("id is required");
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    PSLocale existing = resolveFromCatalog(idOrLang.trim());
    if (existing == null) {
      return null;
    }
    if (StringUtils.isNotBlank(body.getLanguageString())) {
      String want = normalizeLanguageString(body.getLanguageString());
      String have = normalizeLanguageString(existing.getLanguageString());
      if (want != null && have != null && !want.equals(have)) {
        throw new IllegalArgumentException("languageString cannot be changed");
      }
    }
    validateFormatWrite(body);
    IPSGuid g = existing.getGUID();
    String session = currentSession();
    String user = currentUser();
    String lang = normalizeLanguageString(existing.getLanguageString());
    try {
      List<PSLocale> loaded =
          designWs.loadLocales(Collections.singletonList(g), true, false, session, user);
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        return null;
      }
      PSLocale loc = loaded.get(0);
      applyWritableFields(loc, body, true);
      designWs.saveLocales(Collections.singletonList(loc), true, session, user);
      persistFormatIfRequested(lang, body);
      return reloadDetail(loc);
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, g)) {
        return null;
      }
      if (hasLockError(e)) {
        throw mapLockConflict(e);
      }
      log.error("Failed to load locale for update via design WS: {}", idOrLang, e);
      throw new IllegalStateException("Failed to update locale", e);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("update", e);
    }
  }

  @Override
  public void deleteLocale(URI baseUri, String idOrLang) {
    requireAdmin();
    requireSessionUserForWrite();
    if (StringUtils.isBlank(idOrLang) || !isSafeLocaleKey(idOrLang)) {
      throw new IllegalArgumentException("id is required");
    }
    PSLocale existing = resolveFromCatalog(idOrLang.trim());
    if (existing == null) {
      throw new LocaleNotFoundException("Locale not found");
    }
    IPSGuid g = existing.getGUID();
    String lang = normalizeLanguageString(existing.getLanguageString());
    String session = currentSession();
    String user = currentUser();
    try {
      designWs.deleteLocales(Collections.singletonList(g), false, session, user);
      if (lang != null) {
        formatDeleter.accept(lang);
      }
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("delete", e);
    }
  }

  private LocaleDetail reloadDetail(PSLocale saved) {
    if (saved == null || saved.getGUID() == null) {
      return toDetail(saved, null);
    }
    try {
      List<PSLocale> loaded =
          designWs.loadLocales(
              Collections.singletonList(saved.getGUID()),
              false,
              false,
              currentSession(),
              currentUser());
      if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null) {
        PSLocale loc = loaded.get(0);
        String lang = normalizeLanguageString(loc.getLanguageString());
        PSLocaleFormat format = lang == null ? null : formatByLang.apply(lang).orElse(null);
        return toDetail(loc, format);
      }
    } catch (PSErrorResultsException e) {
      log.debug("Could not reload locale after write: {}", e.getMessage());
    }
    String lang = normalizeLanguageString(saved.getLanguageString());
    PSLocaleFormat format = lang == null ? null : formatByLang.apply(lang).orElse(null);
    return toDetail(saved, format);
  }

  private PSLocale resolveFromCatalog(String idOrLang) {
    return resolveLocale(loadAllLocalesViaDesignWs(), idOrLang);
  }

  private void assertLanguageUnique(String code) {
    List<IPSCatalogSummary> matches = designWs.findLocales(code, null);
    if (matches == null || matches.isEmpty()) {
      return;
    }
    for (IPSCatalogSummary existing : matches) {
      if (existing == null) {
        continue;
      }
      String name = normalizeLanguageString(existing.getName());
      if (code.equals(name)) {
        throw new WebApplicationException("Locale already exists: " + code, 409);
      }
    }
  }

  private List<PSLocale> loadAllLocalesViaDesignWs() {
    List<IPSCatalogSummary> summaries = designWs.findLocales(null, null);
    if (summaries == null || summaries.isEmpty()) {
      return List.of();
    }
    List<IPSGuid> guids = new ArrayList<>();
    for (IPSCatalogSummary sum : summaries) {
      if (sum != null && sum.getGUID() != null) {
        guids.add(sum.getGUID());
      }
    }
    if (guids.isEmpty()) {
      return List.of();
    }
    try {
      List<PSLocale> loaded =
          designWs.loadLocales(guids, false, false, currentSession(), currentUser());
      return loaded != null ? loaded : List.of();
    } catch (PSErrorResultsException e) {
      log.error("Failed to load locales via content design WS", e);
      throw new IllegalStateException("Failed to load locales", e);
    }
  }

  private Set<String> safeFormatIndex() {
    try {
      return formatLanguageIndex.get();
    } catch (RuntimeException e) {
      log.debug("Locale format index unavailable: {}", e.getMessage());
      return Set.of();
    }
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Admin check failed: {}", e.getMessage());
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
  }

  boolean isCurrentUserAdmin() {
    if (userService == null) {
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  private static void requireSessionUserForWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for locale design write", Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  /** Language strings (BCP-47 style) or numeric locale ids. Reject path separators / traversal. */
  static boolean isSafeLocaleKey(String key) {
    if (StringUtils.isBlank(key)) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }

  static boolean isValidLanguageString(String lang) {
    return lang != null && lang.matches(LANGUAGE_PATTERN);
  }

  /** Normalize like {@link PSLocaleFormat#setLanguageString}: lower-case, {@code _} → {@code -}. */
  static String normalizeLanguageString(String lang) {
    if (lang == null) {
      return null;
    }
    String t = lang.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    return t.isEmpty() ? null : t;
  }

  /**
   * Opt-in format write. Omitted {@code hasFormatProfile} and {@code format} leave the
   * RXLOCALEFORMAT row unchanged so existing locale PUT clients are safe.
   */
  void persistFormatIfRequested(String language, LocaleDetail body) {
    if (body == null || language == null) {
      return;
    }
    boolean clear = Boolean.FALSE.equals(body.getHasFormatProfile());
    boolean write = Boolean.TRUE.equals(body.getHasFormatProfile()) || body.getFormat() != null;
    if (!clear && !write) {
      return;
    }
    if (clear) {
      formatDeleter.accept(language);
      return;
    }
    LocaleFormatSummary src =
        body.getFormat() != null ? body.getFormat() : new LocaleFormatSummary();
    PSLocaleFormat row = formatByLang.apply(language).orElseGet(() -> new PSLocaleFormat(language));
    applyFormatFields(row, src);
    row.setLanguageString(language);
    formatSaver.accept(row);
  }

  static void validateFormatWrite(LocaleDetail body) {
    if (body == null) {
      return;
    }
    if (Boolean.FALSE.equals(body.getHasFormatProfile())) {
      return;
    }
    if (!Boolean.TRUE.equals(body.getHasFormatProfile()) && body.getFormat() == null) {
      return;
    }
    LocaleFormatSummary src =
        body.getFormat() != null ? body.getFormat() : new LocaleFormatSummary();
    validateFormat(src);
  }

  static void validateFormat(LocaleFormatSummary src) {
    if (src == null) {
      return;
    }
    validateDateTimePattern("datePattern", src.getDatePattern());
    validateDateTimePattern("timePattern", src.getTimePattern());
    validateDateTimePattern("dateTimePattern", src.getDateTimePattern());
    validateCurrencyPattern(src.getCurrencyPattern());
    validateTextDir(src.getTextDir());
    validateMeasurement(src.getMeasurementSystem());
    validateFirstDay(src.getFirstDayOfWeek());
    validateCurrencyCode(src.getCurrencyCode());
    validateSep("decimalSep", src.getDecimalSep(), 4);
    validateSep("groupingSep", src.getGroupingSep(), 4);
    validateMaxLen("defaultTz", src.getDefaultTz(), 64);
    validateMaxLen("numberingSystem", src.getNumberingSystem(), 16);
    validateMaxLen("calendar", src.getCalendar(), 32);
    if (StringUtils.isNotBlank(src.getLanguageString())) {
      String want = normalizeLanguageString(src.getLanguageString());
      if (want != null && !isValidLanguageString(want)) {
        throw new IllegalArgumentException("invalid format languageString: " + src.getLanguageString());
      }
    }
  }

  static void applyFormatFields(PSLocaleFormat row, LocaleFormatSummary src) {
    if (row == null || src == null) {
      return;
    }
    row.setTextDir(src.getTextDir());
    row.setDatePattern(src.getDatePattern());
    row.setTimePattern(src.getTimePattern());
    row.setDateTimePattern(src.getDateTimePattern());
    row.setDecimalSep(src.getDecimalSep());
    row.setGroupingSep(src.getGroupingSep());
    row.setCurrencyCode(src.getCurrencyCode());
    row.setCurrencyPattern(src.getCurrencyPattern());
    row.setFirstDayOfWeek(src.getFirstDayOfWeek());
    row.setMeasurementSystem(src.getMeasurementSystem());
    row.setDefaultTz(src.getDefaultTz());
    row.setNumberingSystem(src.getNumberingSystem());
    row.setCalendar(src.getCalendar());
  }

  static void validateDateTimePattern(String field, String pattern) {
    if (StringUtils.isBlank(pattern)) {
      return;
    }
    try {
      new SimpleDateFormat(pattern.trim(), Locale.ROOT);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("invalid " + field + " pattern: " + pattern, e);
    }
  }

  static void validateCurrencyPattern(String pattern) {
    if (StringUtils.isBlank(pattern)) {
      return;
    }
    try {
      new DecimalFormat(pattern.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("invalid currencyPattern pattern: " + pattern, e);
    }
  }

  static void validateTextDir(String textDir) {
    if (StringUtils.isBlank(textDir)) {
      return;
    }
    String v = textDir.trim().toLowerCase(Locale.ROOT);
    if (!PSLocaleFormat.TEXT_DIR_LTR.equals(v) && !PSLocaleFormat.TEXT_DIR_RTL.equals(v)) {
      throw new IllegalArgumentException("invalid textDir: " + textDir);
    }
  }

  static void validateMeasurement(String measurement) {
    if (StringUtils.isBlank(measurement)) {
      return;
    }
    String v = measurement.trim().toLowerCase(Locale.ROOT);
    if (!PSLocaleFormat.MEASUREMENT_US.equals(v)
        && !PSLocaleFormat.MEASUREMENT_UK.equals(v)
        && !PSLocaleFormat.MEASUREMENT_METRIC.equals(v)) {
      throw new IllegalArgumentException("invalid measurementSystem: " + measurement);
    }
  }

  static void validateFirstDay(Integer firstDay) {
    if (firstDay == null) {
      return;
    }
    if (firstDay < PSLocaleFormat.FIRST_DAY_MONDAY || firstDay > PSLocaleFormat.FIRST_DAY_SUNDAY) {
      throw new IllegalArgumentException("invalid firstDayOfWeek: " + firstDay);
    }
  }

  static void validateCurrencyCode(String code) {
    if (StringUtils.isBlank(code)) {
      return;
    }
    String v = code.trim();
    if (v.length() != 3 || !v.chars().allMatch(Character::isLetter)) {
      throw new IllegalArgumentException("invalid currencyCode: " + code);
    }
  }

  static void validateSep(String field, String value, int maxLen) {
    if (StringUtils.isBlank(value)) {
      return;
    }
    if (value.trim().length() > maxLen) {
      throw new IllegalArgumentException("invalid " + field + ": too long");
    }
  }

  static void validateMaxLen(String field, String value, int maxLen) {
    if (StringUtils.isBlank(value)) {
      return;
    }
    if (value.trim().length() > maxLen) {
      throw new IllegalArgumentException("invalid " + field + ": too long");
    }
  }

  static void applyWritableFields(PSLocale loc, LocaleDetail body, boolean allowLabel) {
    if (loc == null || body == null) {
      return;
    }
    if (allowLabel && StringUtils.isNotBlank(body.getLabel())) {
      loc.setDisplayName(body.getLabel().trim());
    }
    if (body.getDescription() != null) {
      loc.setDescription(body.getDescription());
    }
    if (body.getStatus() != null) {
      loc.setStatus(parseStatus(body.getStatus()));
    }
    if (body.getBaseLocale() != null) {
      loc.setBaseLocale(body.getBaseLocale());
    }
  }

  static int parseStatus(String status) {
    if (StringUtils.isBlank(status)) {
      throw new IllegalArgumentException("status is required");
    }
    String want = status.trim();
    for (int i = 0; i < PSLocale.STATUS_ENUM.length; i++) {
      if (PSLocale.STATUS_ENUM[i].equalsIgnoreCase(want)) {
        return i;
      }
    }
    throw new IllegalArgumentException("invalid status: " + status);
  }

  static boolean isAlreadyExistsFailure(IllegalArgumentException e) {
    if (e == null) {
      return false;
    }
    return StringUtils.containsIgnoreCase(e.getMessage(), "already exists");
  }

  /**
   * True when the design WS reported an error for the specific requested locale GUID and did not
   * return a result for that GUID.
   */
  static boolean isNotFound(PSErrorResultsException e, IPSGuid requested) {
    if (e == null || requested == null) {
      return false;
    }
    Map<IPSGuid, Object> errors = e.getErrors();
    Map<IPSGuid, Object> results = e.getResults();
    boolean errored = errors != null && errors.containsKey(requested);
    boolean hasResult = results != null && results.containsKey(requested);
    return errored && !hasResult && !hasLockError(e);
  }

  static boolean hasLockError(PSErrorResultsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (isLockErrorObject(err)) {
        return true;
      }
    }
    return false;
  }

  static boolean isNotLockedError(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (isLockErrorObject(err)) {
        return true;
      }
    }
    return false;
  }

  static boolean isDependencyError(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      String msg = errorMessage(err);
      if (StringUtils.containsIgnoreCase(msg, "depend")) {
        return true;
      }
    }
    return StringUtils.containsIgnoreCase(e.getMessage(), "depend");
  }

  private static boolean isLockErrorObject(Object err) {
    if (err instanceof PSLockErrorException) {
      return true;
    }
    if (err instanceof PSErrorException pe) {
      String msg = pe.getErrorMessage() != null ? pe.getErrorMessage() : pe.getMessage();
      return StringUtils.containsIgnoreCase(msg, "is not locked")
          || StringUtils.containsIgnoreCase(msg, "not locked for");
    }
    return StringUtils.containsIgnoreCase(String.valueOf(err), "is not locked");
  }

  private static String errorMessage(Object err) {
    if (err instanceof PSErrorException pe) {
      return StringUtils.defaultIfBlank(pe.getErrorMessage(), pe.getMessage());
    }
    return err != null ? String.valueOf(err) : null;
  }

  static LocaleDesignLockException mapLockConflict(Exception e) {
    String locker = firstLockLocker(e);
    if (StringUtils.isNotBlank(locker)) {
      return new LocaleDesignLockException("Could not save locale; locked by " + locker, e);
    }
    return new LocaleDesignLockException("Could not save locale; design lock required", e);
  }

  static String firstLockLocker(Exception e) {
    Map<IPSGuid, Object> errors = null;
    if (e instanceof PSErrorResultsException re && re.getErrors() != null) {
      errors = re.getErrors();
    } else if (e instanceof PSErrorsException se && se.getErrors() != null) {
      errors = se.getErrors();
    } else if (e instanceof PSLockErrorException lockErr) {
      return lockErr.getLocker();
    }
    if (errors == null) {
      return null;
    }
    for (Object err : errors.values()) {
      if (err instanceof PSLockErrorException lockErr && StringUtils.isNotBlank(lockErr.getLocker())) {
        return lockErr.getLocker();
      }
    }
    return null;
  }

  private RuntimeException mapCreateFailure(String code, PSErrorException e) {
    if (e instanceof PSLockErrorException) {
      return mapLockConflict(e);
    }
    if (isAlreadyExistsFailure(new IllegalArgumentException(errorMessage(e)))) {
      return new WebApplicationException("Locale already exists: " + code, 409);
    }
    log.error("Failed to create locale via content design WS", e);
    return new IllegalStateException("Failed to create locale", e);
  }

  private RuntimeException mapSaveOrDeleteFailure(String verb, PSErrorsException e) {
    if (isNotLockedError(e)) {
      return mapLockConflict(e);
    }
    if (isDependencyError(e)) {
      return new LocaleDesignLockException("Locale has dependents", e);
    }
    log.error("Failed to {} locale via content design WS", verb, e);
    return new IllegalStateException("Failed to " + verb + " locale", e);
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
            LocaleSummary::getLanguageString, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
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
