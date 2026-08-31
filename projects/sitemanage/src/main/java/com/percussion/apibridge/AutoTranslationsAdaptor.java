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

import com.percussion.rest.locales.AutoTranslationDesignLockException;
import com.percussion.rest.locales.AutoTranslationRow;
import com.percussion.rest.locales.IAutoTranslationsAdaptor;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.content.data.PSAutoTranslation;
import com.percussion.services.content.data.PSAutoTranslationPK;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.PSRoleMgrLocator;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Singleton auto-translation set for Developer REST (CD-18).
 *
 * <p>Workbench parity: {@link IPSContentDesignWs#loadTranslationSettings} / {@link
 * IPSContentDesignWs#saveTranslationSettings} (same design web service SOAP uses). PUT acquires the
 * set lock and releases it on save. Empty list clears. Unknown locale or content type is {@code
 * 400}.
 *
 * <p>Admin only — same {@link IPSUserService#isAdminUser} gate as locale design writes.
 */
@PSSiteManageBean
public class AutoTranslationsAdaptor implements IAutoTranslationsAdaptor {

  private static final Logger log = LogManager.getLogger(AutoTranslationsAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to read or write auto-translations";

  private final IPSContentDesignWs designWs;
  private final Supplier<List<IPSCatalogSummary>> localeCatalog;
  private final Supplier<List<IPSCatalogSummary>> contentTypeCatalog;
  private final Supplier<List<IPSCatalogSummary>> workflowCatalog;
  private final Supplier<List<IPSCatalogSummary>> communityCatalog;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  /** Production constructor; locators resolve design WS and catalog collaborators. */
  public AutoTranslationsAdaptor() {
    this(
        PSContentWsLocator.getContentDesignWebservice(),
        () ->
            safeCatalog(
                PSContentWsLocator.getContentDesignWebservice().findLocales(null, null)),
        () ->
            safeCatalog(PSContentWsLocator.getContentDesignWebservice().findContentTypes(null)),
        () -> {
          IPSWorkflowService wf = PSWorkflowServiceLocator.getWorkflowService();
          return safeCatalog(wf.findWorkflowsByName(null));
        },
        () -> {
          IPSBackEndRoleMgr roles = PSRoleMgrLocator.getBackEndRoleManager();
          return safeCatalog(roles.findCommunitiesByName(null));
        },
        null);
  }

  /** Package-visible for unit tests. {@code null} adminChecker uses {@link #isCurrentUserAdmin()}. */
  AutoTranslationsAdaptor(
      IPSContentDesignWs designWs,
      Supplier<List<IPSCatalogSummary>> localeCatalog,
      Supplier<List<IPSCatalogSummary>> contentTypeCatalog,
      Supplier<List<IPSCatalogSummary>> workflowCatalog,
      Supplier<List<IPSCatalogSummary>> communityCatalog,
      BooleanSupplier adminChecker) {
    this.designWs = designWs;
    this.localeCatalog = localeCatalog;
    this.contentTypeCatalog = contentTypeCatalog;
    this.workflowCatalog = workflowCatalog;
    this.communityCatalog = communityCatalog;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public List<AutoTranslationRow> getAutoTranslations(URI baseUri) {
    requireAdmin();
    try {
      List<PSAutoTranslation> loaded =
          designWs.loadTranslationSettings(false, false, currentSession(), currentUser());
      return mapRows(loaded);
    } catch (PSLockErrorException e) {
      throw mapLockConflict(e);
    } catch (RuntimeException e) {
      log.warn("Failed to load auto-translations via content design WS", e);
      throw e;
    }
  }

  @Override
  public List<AutoTranslationRow> saveAutoTranslations(URI baseUri, List<AutoTranslationRow> rows) {
    requireAdmin();
    requireSessionUserForWrite();
    if (rows == null) {
      throw new IllegalArgumentException("body is required");
    }
    List<PSAutoTranslation> toSave = toEntities(rows);
    String session = currentSession();
    String user = currentUser();
    try {
      // overrideLock=true steals leftover same-user dummy-GUID locks (H2 QA
      // crashed PUT). Other users still fail createLock with 409.
      List<PSAutoTranslation> current =
          designWs.loadTranslationSettings(true, true, session, user);
      copyVersions(toSave, current);
      designWs.saveTranslationSettings(toSave, true, session, user);
      List<PSAutoTranslation> reloaded =
          designWs.loadTranslationSettings(false, false, session, user);
      return mapRows(reloaded != null ? reloaded : toSave);
    } catch (PSLockErrorException e) {
      throw mapLockConflict(e);
    } catch (RuntimeException e) {
      log.warn("Failed to save auto-translations via content design WS", e);
      throw e;
    }
  }

  List<PSAutoTranslation> toEntities(List<AutoTranslationRow> rows) {
    List<IPSCatalogSummary> locales = safeGet(localeCatalog);
    List<IPSCatalogSummary> types = safeGet(contentTypeCatalog);
    List<IPSCatalogSummary> workflows = safeGet(workflowCatalog);
    List<IPSCatalogSummary> communities = safeGet(communityCatalog);
    List<PSAutoTranslation> out = new ArrayList<>();
    Set<String> keys = new HashSet<>();
    int i = 0;
    for (AutoTranslationRow row : rows) {
      if (row == null) {
        throw new IllegalArgumentException("row[" + i + "] is required");
      }
      IPSCatalogSummary locale = resolveLocale(locales, row.getLocale());
      IPSCatalogSummary type =
          resolveCatalog(types, row.getContentTypeId(), row.getContentTypeName(), "content type");
      IPSCatalogSummary workflow =
          resolveCatalog(workflows, row.getWorkflowId(), row.getWorkflowName(), "workflow");
      IPSCatalogSummary community =
          resolveCatalog(communities, row.getCommunityId(), row.getCommunityName(), "community");
      String lang = locale.getName();
      long typeId = catalogLongId(type);
      String key = typeId + "|" + normalizeLanguageString(lang);
      if (!keys.add(key)) {
        throw new IllegalArgumentException(
            "duplicate locale/content-type row: " + lang + " / " + type.getName());
      }
      PSAutoTranslation at = new PSAutoTranslation();
      at.setLocale(lang);
      at.setContentTypeId(typeId);
      at.setContentTypeName(type.getName());
      at.setWorkflowId(catalogLongId(workflow));
      at.setWorkflowName(workflow.getName());
      at.setCommunityId(catalogLongId(community));
      at.setCommunityName(community.getName());
      out.add(at);
      i++;
    }
    return out;
  }

  List<AutoTranslationRow> mapRows(List<PSAutoTranslation> loaded) {
    List<IPSCatalogSummary> types = safeGet(contentTypeCatalog);
    List<IPSCatalogSummary> workflows = safeGet(workflowCatalog);
    List<IPSCatalogSummary> communities = safeGet(communityCatalog);
    List<AutoTranslationRow> out = new ArrayList<>();
    if (loaded == null) {
      return out;
    }
    for (PSAutoTranslation at : loaded) {
      if (at == null) {
        continue;
      }
      AutoTranslationRow row = new AutoTranslationRow();
      row.setLocale(at.getLocale());
      row.setContentTypeId(at.getContentTypeId());
      row.setContentTypeName(
          firstNonBlank(at.getContentTypeName(), nameForId(types, at.getContentTypeId())));
      row.setWorkflowId(at.getWorkflowId());
      row.setWorkflowName(
          firstNonBlank(at.getWorkflowName(), nameForId(workflows, at.getWorkflowId())));
      row.setCommunityId(at.getCommunityId());
      row.setCommunityName(
          firstNonBlank(at.getCommunityName(), nameForId(communities, at.getCommunityId())));
      out.add(row);
    }
    return out;
  }

  static void copyVersions(List<PSAutoTranslation> incoming, List<PSAutoTranslation> current) {
    if (incoming == null || current == null) {
      return;
    }
    Map<PSAutoTranslationPK, Integer> versions = new HashMap<>();
    for (PSAutoTranslation at : current) {
      if (at != null && StringUtils.isNotBlank(at.getLocale())) {
        versions.put(at.getPersistentKey(), at.getVersion());
      }
    }
    for (PSAutoTranslation at : incoming) {
      if (at == null || StringUtils.isBlank(at.getLocale())) {
        continue;
      }
      Integer version = versions.get(at.getPersistentKey());
      if (version != null) {
        at.setVersion(version);
      }
    }
  }

  static IPSCatalogSummary resolveLocale(List<IPSCatalogSummary> catalog, String locale) {
    if (StringUtils.isBlank(locale)) {
      throw new IllegalArgumentException("locale is required");
    }
    String want = normalizeLanguageString(locale);
    if (want == null) {
      throw new IllegalArgumentException("locale is required");
    }
    for (IPSCatalogSummary sum : safeList(catalog)) {
      if (sum == null) {
        continue;
      }
      String have = normalizeLanguageString(sum.getName());
      if (want.equals(have)) {
        return sum;
      }
      String raw = locale.trim();
      if (StringUtils.isNumeric(raw) && matchesId(sum.getGUID(), Long.parseLong(raw))) {
        return sum;
      }
    }
    throw new IllegalArgumentException("unknown locale: " + locale);
  }

  static IPSCatalogSummary resolveCatalog(
      List<IPSCatalogSummary> catalog, Long id, String name, String field) {
    if (id != null && id > 0) {
      for (IPSCatalogSummary sum : safeList(catalog)) {
        if (sum != null && matchesId(sum.getGUID(), id)) {
          return sum;
        }
      }
      throw new IllegalArgumentException("unknown " + field + ": " + id);
    }
    if (StringUtils.isNotBlank(name)) {
      String want = name.trim();
      for (IPSCatalogSummary sum : safeList(catalog)) {
        if (sum != null && want.equalsIgnoreCase(StringUtils.trimToEmpty(sum.getName()))) {
          return sum;
        }
      }
      throw new IllegalArgumentException("unknown " + field + ": " + name);
    }
    throw new IllegalArgumentException(field + " is required");
  }

  static boolean matchesId(IPSGuid guid, long id) {
    if (guid == null) {
      return false;
    }
    return guid.longValue() == id || guid.getUUID() == id;
  }

  static long catalogLongId(IPSCatalogSummary sum) {
    return catalogPersistentId(sum);
  }

  /**
   * Persist UUID (legacy {@code PSX_AUTOTRANSLATION.CONTENTTYPEID} / workflow /
   * community columns) rather than the typed 64-bit GUID.
   */
  static long catalogPersistentId(IPSCatalogSummary sum) {
    if (sum == null || sum.getGUID() == null) {
      throw new IllegalArgumentException("catalog entry is missing a guid");
    }
    IPSGuid guid = sum.getGUID();
    int uuid = guid.getUUID();
    return uuid > 0 ? uuid : guid.longValue();
  }

  static String nameForId(List<IPSCatalogSummary> catalog, long id) {
    for (IPSCatalogSummary sum : safeList(catalog)) {
      if (sum != null && matchesId(sum.getGUID(), id)) {
        return sum.getName();
      }
    }
    return null;
  }

  static String firstNonBlank(String primary, String fallback) {
    return StringUtils.isNotBlank(primary) ? primary : fallback;
  }

  static String normalizeLanguageString(String lang) {
    if (lang == null) {
      return null;
    }
    String t = lang.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    return t.isEmpty() ? null : t;
  }

  static AutoTranslationDesignLockException mapLockConflict(PSLockErrorException e) {
    String locker = e != null ? e.getLocker() : null;
    if (StringUtils.isNotBlank(locker)) {
      return new AutoTranslationDesignLockException(
          "Could not save auto-translations; locked by " + locker, e);
    }
    return new AutoTranslationDesignLockException(
        "Could not save auto-translations; design lock required", e);
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
          "Request session/user required for auto-translation design write",
          Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  private static List<IPSCatalogSummary> safeGet(Supplier<List<IPSCatalogSummary>> supplier) {
    if (supplier == null) {
      return List.of();
    }
    try {
      return safeList(supplier.get());
    } catch (RuntimeException e) {
      log.debug("Catalog lookup failed: {}", e.getMessage());
      return List.of();
    }
  }

  private static List<IPSCatalogSummary> safeCatalog(List<? extends IPSCatalogSummary> src) {
    return safeList(src);
  }

  private static List<IPSCatalogSummary> safeList(List<? extends IPSCatalogSummary> src) {
    if (src == null || src.isEmpty()) {
      return List.of();
    }
    List<IPSCatalogSummary> out = new ArrayList<>(src.size());
    for (IPSCatalogSummary sum : src) {
      if (sum != null) {
        out.add(sum);
      }
    }
    return out;
  }
}
