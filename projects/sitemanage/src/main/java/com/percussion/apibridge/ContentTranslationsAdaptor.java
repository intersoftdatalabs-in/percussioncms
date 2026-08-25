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

package com.percussion.apibridge;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.rest.translations.CreateTranslationsRequest;
import com.percussion.rest.translations.CreateTranslationsResult;
import com.percussion.rest.translations.IContentTranslationsAdaptor;
import com.percussion.rest.translations.ItemTranslationVariants;
import com.percussion.rest.translations.TranslationVariant;
import com.percussion.services.content.data.PSAutoTranslation;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.webservices.system.IPSSystemWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public REST create-variant / item-locale façade (#2429).
 *
 * <p>Thin apibridge over {@link IPSContentWs#newTranslations(List, List, String, boolean)} — the
 * same domain path SOAP {@code content.NewTranslations} uses. Item-locale listing uses component
 * summaries plus translation-category dependents.
 *
 * <p><strong>Out of scope:</strong> in-flight translation queue filter and session content-locale
 * context (product disposition on #2411 / #2428).
 */
@PSSiteManageBean
public class ContentTranslationsAdaptor implements IContentTranslationsAdaptor {

  private static final Logger log = LogManager.getLogger(ContentTranslationsAdaptor.class);

  static final String ROLE_SOURCE = "source";
  static final String ROLE_TRANSLATION = "translation";

  private final IPSContentWs contentWs;
  private final IPSSystemWs systemWs;
  private final IPSIdMapper idMapper;
  private final Supplier<IPSCmsObjectMgr> objectMgr;
  private final Function<String, IPSGuid> guidResolver;

  @Autowired
  public ContentTranslationsAdaptor(IPSIdMapper idMapper) {
    this(
        PSContentWsLocator.getContentWebservice(),
        PSSystemWsLocator.getSystemWebservice(),
        idMapper,
        PSCmsObjectMgrLocator::getObjectManager,
        idMapper::getGuid);
  }

  /** Package-visible for unit tests. */
  ContentTranslationsAdaptor(
      IPSContentWs contentWs,
      IPSSystemWs systemWs,
      IPSIdMapper idMapper,
      Supplier<IPSCmsObjectMgr> objectMgr,
      Function<String, IPSGuid> guidResolver) {
    this.contentWs = contentWs;
    this.systemWs = systemWs;
    this.idMapper = idMapper;
    this.objectMgr = objectMgr;
    this.guidResolver = guidResolver;
  }

  @Override
  public CreateTranslationsResult createTranslations(
      URI baseUri, CreateTranslationsRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    List<Long> itemIds = request.getItemIds();
    if (itemIds == null || itemIds.isEmpty()) {
      throw new IllegalArgumentException("itemIds is required and must not be empty");
    }

    List<IPSGuid> guids = new ArrayList<>();
    for (Long id : itemIds) {
      if (id == null || id <= 0) {
        throw new IllegalArgumentException("itemIds must contain positive content ids");
      }
      if (id > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("itemIds content id out of range: " + id);
      }
      // content id + undefined revision — same shape SOAP NewTranslations uses via legacy guids
      guids.add(new PSLegacyGuid(id.intValue(), -1));
    }

    List<PSAutoTranslation> settings = toAutoTranslations(request.getLocales());
    String relationshipType = StringUtils.trimToNull(request.getRelationshipType());
    boolean enableRevisions = Boolean.TRUE.equals(request.getEnableRevisions());

    try {
      List<PSCoreItem> created =
          contentWs.newTranslations(guids, settings, relationshipType, enableRevisions);
      return new CreateTranslationsResult(mapCreated(created, itemIds));
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      log.warn("newTranslations partial/error results: {}", e.getAllErrorString());
      throw new WebApplicationException(
          "Failed to create one or more translations: " + e.getAllErrorString(),
          e,
          Response.Status.INTERNAL_SERVER_ERROR);
    } catch (PSErrorException e) {
      log.error("newTranslations failed: {}", e.getMessage(), e);
      throw new WebApplicationException(
          "Failed to create translations.", e, Response.Status.INTERNAL_SERVER_ERROR);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.error("newTranslations unexpected failure: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Override
  public ItemTranslationVariants listItemVariants(URI baseUri, String itemId) {
    if (StringUtils.isBlank(itemId)) {
      throw new IllegalArgumentException("itemId is required");
    }
    if (!isSafeItemKey(itemId)) {
      throw new IllegalArgumentException("itemId contains invalid characters");
    }

    IPSGuid guid;
    try {
      guid = resolveItemGuid(itemId.trim());
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Could not resolve item id {}: {}", itemId, e.getMessage());
      return null;
    }
    if (guid == null) {
      return null;
    }

    int contentId = contentIdFromGuid(guid);
    PSComponentSummary sourceSummary = loadSummary(contentId);
    if (sourceSummary == null) {
      return null;
    }

    ItemTranslationVariants out = new ItemTranslationVariants();
    out.setItemId(contentId);
    out.setLocale(sourceSummary.getLocale());

    List<TranslationVariant> variants = new ArrayList<>();
    variants.add(fromSummary(sourceSummary, ROLE_SOURCE, null));

    try {
      List<IPSGuid> dependents = findTranslationDependents(guid);
      for (IPSGuid dep : dependents) {
        int depId = contentIdFromGuid(dep);
        if (depId <= 0 || depId == contentId) {
          continue;
        }
        PSComponentSummary depSum = loadSummary(depId);
        if (depSum == null) {
          continue;
        }
        variants.add(fromSummary(depSum, ROLE_TRANSLATION, (long) contentId));
      }
    } catch (RuntimeException e) {
      // AuthZ / infrastructure — surface as 403-style security failure when message suggests
      // access denial; otherwise rethrow for 500.
      if (isAuthzFailure(e)) {
        throw new SecurityException("Cannot list translation variants for " + itemId, e);
      }
      throw e;
    }

    out.setVariants(variants);
    return out;
  }

  private List<IPSGuid> findTranslationDependents(IPSGuid guid) {
    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_TRANSLATION);
    PSLocator owner = idMapper.getLocator(guid);
    if (owner != null) {
      filter.setOwner(owner);
    }
    List<IPSGuid> rows = systemWs.findDependents(guid, filter);
    return rows != null ? rows : List.of();
  }

  private PSComponentSummary loadSummary(int contentId) {
    if (contentId <= 0) {
      return null;
    }
    try {
      // null = not found → resource 404; RuntimeException = infrastructure → 500
      return objectMgr.get().loadComponentSummary(contentId);
    } catch (RuntimeException e) {
      log.warn("loadComponentSummary({}) failed: {}", contentId, e.getMessage());
      throw e;
    }
  }

  static List<PSAutoTranslation> toAutoTranslations(List<String> locales) {
    if (locales == null || locales.isEmpty()) {
      // SOAP parity: null means use all system auto-translations
      return null;
    }
    List<PSAutoTranslation> out = new ArrayList<>();
    for (String locale : locales) {
      if (StringUtils.isBlank(locale)) {
        throw new IllegalArgumentException("locales must not contain blank entries");
      }
      String normalized = locale.trim();
      if (!isSafeLocaleKey(normalized)) {
        throw new IllegalArgumentException("invalid locale: " + locale);
      }
      PSAutoTranslation at = new PSAutoTranslation();
      at.setLocale(normalized);
      out.add(at);
    }
    return out;
  }

  static List<TranslationVariant> mapCreated(List<PSCoreItem> items, List<Long> sourceIds) {
    List<TranslationVariant> out = new ArrayList<>();
    if (items == null) {
      return out;
    }
    Long singleSource =
        sourceIds != null && sourceIds.size() == 1 ? sourceIds.get(0) : null;
    for (PSCoreItem item : items) {
      if (item == null) {
        continue;
      }
      TranslationVariant v = new TranslationVariant();
      v.setContentId(item.getContentId());
      int rev = item.getRevision();
      if (rev > 0) {
        v.setRevision(rev);
      } else {
        int cur = item.getCurrentRevision();
        if (cur > 0) {
          v.setRevision(cur);
        }
      }
      v.setLocale(readLocaleField(item));
      v.setRole(ROLE_TRANSLATION);
      v.setSourceContentId(singleSource);
      out.add(v);
    }
    return out;
  }

  static String readLocaleField(PSCoreItem item) {
    if (item == null) {
      return null;
    }
    try {
      PSItemField field = item.getFieldByName("sys_lang");
      if (field == null || field.getValue() == null) {
        return null;
      }
      return field.getValue().getValueAsString();
    } catch (Exception e) {
      log.warn(
          "Could not read sys_lang locale from content id {}: {}",
          item.getContentId(),
          e.toString());
      return null;
    }
  }

  static TranslationVariant fromSummary(
      PSComponentSummary summary, String role, Long sourceContentId) {
    TranslationVariant v = new TranslationVariant();
    v.setContentId(summary.getContentId());
    v.setLocale(summary.getLocale());
    v.setRole(role);
    v.setSourceContentId(sourceContentId);
    if (summary.getCurrentLocator() != null) {
      int rev = summary.getCurrentLocator().getRevision();
      if (rev > 0) {
        v.setRevision(rev);
      }
    }
    return v;
  }

  static int contentIdFromGuid(IPSGuid guid) {
    if (guid == null) {
      return -1;
    }
    if (guid instanceof PSLegacyGuid legacy) {
      return legacy.getContentId();
    }
    // Non-legacy guids: UUID is the best available numeric id for any type (item or
    // untyped). Callers still validate via loadComponentSummary (null → not found).
    return guid.getUUID();
  }

  /**
   * Resolve {@code itemId} as either a hyphenated {@code host-type-uuid} GUID or a
   * bare numeric content id.
   *
   * <p>Explorer Translations GET historically stripped GUIDs to the last segment
   * ({@code 16777215-101-551} → {@code 551}). {@link
   * com.percussion.share.service.IPSIdMapper#getGuid(String)} can throw on an
   * untyped packed long, which the resource maps as HTTP 404. Bare numbers skip
   * the mapper and become {@link PSLegacyGuid} content ids; GUID strings still
   * go through {@code guidResolver}.
   *
   * @param itemId trimmed, non-blank key
   * @return guid for summary load, never {@code null} on the numeric path
   */
  IPSGuid resolveItemGuid(String itemId) {
    Long contentId = parseBareNumericContentId(itemId);
    if (contentId != null) {
      return new PSLegacyGuid(contentId.intValue(), -1);
    }
    return guidResolver.apply(itemId);
  }

  /**
   * Bare decimal content id with no GUID type bits (Explorer last-segment or
   * legacy numeric path). Hyphenated GUIDs return {@code null}.
   */
  static Long parseBareNumericContentId(String key) {
    if (StringUtils.isBlank(key)) {
      return null;
    }
    String trimmed = key.trim();
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < '0' || c > '9') {
        return null;
      }
    }
    try {
      long raw = Long.parseLong(trimmed);
      if (raw <= 0) {
        return null;
      }
      if (raw > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("itemId content id out of range: " + key);
      }
      return raw;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Max path token length for content id / guid string (path-injection surface). */
  static final int MAX_ITEM_KEY_LENGTH = 128;

  static boolean isSafeItemKey(String key) {
    if (StringUtils.isBlank(key) || key.length() > MAX_ITEM_KEY_LENGTH) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }

  static boolean isSafeLocaleKey(String key) {
    if (StringUtils.isBlank(key)) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0
        && key.length() <= 64;
  }

  /**
   * Prefer exception type hierarchy over message keywords so unrelated errors that mention
   * "access"/"permission" are not mis-mapped to 403.
   */
  static boolean isAuthzFailure(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof SecurityException) {
        return true;
      }
      String simple = t.getClass().getSimpleName();
      if (simple.contains("Authorization")
          || simple.contains("AccessDenied")
          || simple.contains("NotAuthorized")
          || simple.contains("Forbidden")) {
        return true;
      }
    }
    return false;
  }
}
