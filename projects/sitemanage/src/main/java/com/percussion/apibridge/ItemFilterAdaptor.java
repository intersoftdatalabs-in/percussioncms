/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import com.percussion.rest.Guid;
import com.percussion.rest.itemfilter.IItemFilterAdaptor;
import com.percussion.rest.itemfilter.ItemFilter;
import com.percussion.rest.itemfilter.ItemFilterRuleDefinition;
import com.percussion.rest.itemfilter.ItemFilterRuleDefinitionParam;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.guidmgr.data.PSGuid;
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
import com.percussion.webservices.system.IPSSystemDesignWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adaptor for ItemFilter management in Percussion CMS.
 *
 * <p>GET catalog uses {@link IPSFilterService}. Admin create/update/delete persist through {@link
 * IPSSystemDesignWs} ({@code createItemFilters} / {@code loadItemFilters} / {@code saveItemFilters}
 * / {@code deleteItemFilters}) — the same system design web service SOAP uses. No new SOAP
 * methods.
 */
@PSSiteManageBean
public class ItemFilterAdaptor implements IItemFilterAdaptor {

  static final String ADMIN_REQUIRED = "Admin role required to create, update, or delete item filters";

  private final IPSFilterService filterService;
  private final IPSSystemDesignWs designWs;
  private final BooleanSupplier adminChecker;
  private static final Logger log = LogManager.getLogger(ItemFilterAdaptor.class);

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ItemFilterAdaptor() {
    this(
        PSFilterServiceLocator.getFilterService(),
        PSSystemWsLocator.getSystemDesignWebservice(),
        null);
  }

  /** Package-visible for unit tests. */
  ItemFilterAdaptor(
      IPSFilterService filterService, IPSSystemDesignWs designWs, BooleanSupplier adminChecker) {
    this.filterService = filterService;
    this.designWs = designWs;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  /**
   * Get a list of the ItemFilters available on the system populated with rules and parameters.
   *
   * @return A list of item filters
   */
  @Override
  public List<ItemFilter> getItemFilters() {
    return filterService.findAllFilters().stream()
        .map(this::copyFilter)
        .collect(Collectors.toList());
  }

  private ItemFilter copyFilter(IPSItemFilter filter) {
    var ret = new ItemFilter();
    ret.setFilterId(ApiUtils.convertGuid(filter.getGUID()));
    ret.setDescription(filter.getDescription());
    ret.setName(filter.getName());
    ret.setLegacyAuthtype(filter.getLegacyAuthtypeId());

    if (filter.getParentFilter() != null) {
      ret.setParentFilter(copyFilter(filter.getParentFilter()));
    }

    var rules =
        filter.getRuleDefs().stream()
            .map(this::copyItemFilterRuleDef)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    ret.setRules(rules);
    return ret;
  }

  private ItemFilterRuleDefinition copyItemFilterRuleDef(IPSItemFilterRuleDef def) {
    try {
      var ret = new ItemFilterRuleDefinition();
      ret.setName(def.getRuleName());
      ret.setRuleId(ApiUtils.convertGuid(def.getGUID()));

      var retParams =
          def.getParams().entrySet().stream()
              .map(
                  pair -> {
                    var p = new ItemFilterRuleDefinitionParam();
                    p.setName(pair.getKey());
                    p.setValue(pair.getValue());
                    return p;
                  })
              .collect(Collectors.toList());
      ret.setParams(retParams);
      return ret;
    } catch (PSFilterException e) {
      log.error("Error getting ItemFilter Rule Name. Skipping Rule.", e);
      return null;
    }
  }

  /**
   * Update or create an ItemFilter.
   *
   * <p>No {@code filterId} is create (duplicate name is 409). A {@code filterId} is update (unknown
   * id returns {@code null} → 404).
   *
   * @param filter The filter to update or create.
   * @return The updated ItemFilter.
   */
  @Override
  public ItemFilter updateOrCreateItemFilter(ItemFilter filter) {
    requireAdmin();
    requireSessionUserForWrite();
    if (filter == null) {
      throw new IllegalArgumentException("body is required");
    }
    IPSGuid existingId = toIpsGuid(filter.getFilterId());
    if (existingId != null) {
      return updateFilter(existingId, filter);
    }
    return createFilter(filter);
  }

  /**
   * Delete the specified item filter.
   *
   * @param itemFilterId A valid ItemFilter id. Filter must not be associated with any ContentLists
   *     or it won't be deleted.
   */
  @Override
  public void deleteItemFilter(Guid itemFilterId) throws PSNotFoundException {
    requireAdmin();
    requireSessionUserForWrite();
    IPSGuid id = toIpsGuid(itemFilterId);
    if (id == null) {
      throw new IllegalArgumentException("item filter id is required");
    }
    IPSItemFilter existing = filterService.loadFilter(id);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSItemFilter> locked =
          designWs.loadItemFilters(List.of(existing.getGUID()), true, false, session, user);
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        throw new WebApplicationException(
            "Could not delete item filter; design lock required or held by another user", 409);
      }
      designWs.deleteItemFilters(List.of(existing.getGUID()), false, session, user);
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, existing.getGUID())) {
        throw new PSNotFoundException("Item filter not found");
      }
      throw new WebApplicationException(
          "Could not delete item filter; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("delete", e);
    }
  }

  /**
   * Get a single ItemFilter by id.
   *
   * @param itemFilterId A Valid ItemFilter id
   * @return The ItemFilter
   */
  @Override
  public ItemFilter getItemFilter(Guid itemFilterId) throws PSNotFoundException {
    var filter = filterService.loadFilter(ApiUtils.convertGuid(itemFilterId));
    return copyFilter(filter);
  }

  @Override
  public ItemFilter findItemFilter(String idOrName) {
    if (!isSafeFilterKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    // Prefer name match (common Developer UX); fall back to GUID string.
    for (ItemFilter f : getItemFilters()) {
      if (f != null && key.equalsIgnoreCase(f.getName())) {
        return f;
      }
    }
    try {
      // type-host-uuid or long form accepted by PSGuid
      var guid = new com.percussion.services.guidmgr.data.PSGuid(key);
      return getItemFilter(ApiUtils.convertGuid((com.percussion.utils.guid.IPSGuid) guid));
    } catch (IllegalArgumentException e) {
      // Invalid GUID syntax (incl. NumberFormatException from parse) → generic 404
      log.debug("Invalid item filter GUID syntax: {}", e.getMessage());
      return null;
    } catch (PSNotFoundException e) {
      log.debug("Item filter not found for GUID key: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Single path component / guid token only — reject traversal and separators ({@code
   * java/path-injection}).
   */
  static boolean isSafeFilterKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }

  private ItemFilter createFilter(ItemFilter body) {
    String name = requireValidName(body.getName());
    assertNameUnique(name);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSItemFilter> created = designWs.createItemFilters(List.of(name), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createItemFilters returned empty");
      }
      PSItemFilter domain = created.get(0);
      applyWritableFields(domain, body);
      designWs.saveItemFilters(List.of(domain), true, session, user);
      return reload(domain);
    } catch (WebApplicationException | IllegalStateException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Item filter already exists: " + name, 409);
      }
      throw e;
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("create", e);
    } catch (PSFilterException e) {
      throw new IllegalArgumentException(
          e.getMessage() != null ? e.getMessage() : "Invalid item filter rule", e);
    }
  }

  private ItemFilter updateFilter(IPSGuid id, ItemFilter body) {
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSItemFilter> loaded =
          designWs.loadItemFilters(List.of(id), true, false, session, user);
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        return null;
      }
      PSItemFilter domain = loaded.get(0);
      applyWritableFields(domain, body);
      designWs.saveItemFilters(List.of(domain), true, session, user);
      return reload(domain);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, id)) {
        return null;
      }
      if (hasLockError(e)) {
        throw new WebApplicationException(
            "Could not update item filter; design lock required or held by another user", 409);
      }
      log.error("Failed to load item filter for update: {}", id, e);
      throw new IllegalStateException("Failed to update item filter", e);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("update", e);
    } catch (PSFilterException e) {
      throw new IllegalArgumentException(
          e.getMessage() != null ? e.getMessage() : "Invalid item filter rule", e);
    }
  }

  private void applyWritableFields(IPSItemFilter domain, ItemFilter body) throws PSFilterException {
    if (body.getDescription() != null) {
      domain.setDescription(body.getDescription());
    }
    if (body.getLegacyAuthtype() != null) {
      domain.setLegacyAuthtypeId(body.getLegacyAuthtype());
    }
    applyRules(domain, body.getRules());
    applyParent(domain, body.getParentFilter());
  }

  private void applyRules(IPSItemFilter domain, Set<ItemFilterRuleDefinition> rules)
      throws PSFilterException {
    if (rules == null) {
      return;
    }
    Set<IPSItemFilterRuleDef> defs = new HashSet<>();
    for (ItemFilterRuleDefinition rule : rules) {
      if (rule == null || StringUtils.isBlank(rule.getName())) {
        throw new IllegalArgumentException("rule name is required");
      }
      Map<String, String> params = new HashMap<>();
      if (rule.getParams() != null) {
        for (ItemFilterRuleDefinitionParam p : rule.getParams()) {
          if (p != null && StringUtils.isNotBlank(p.getName()) && p.getValue() != null) {
            params.put(p.getName(), p.getValue());
          }
        }
      }
      defs.add(filterService.createRuleDef(rule.getName().trim(), params));
    }
    domain.setRuleDefs(defs);
  }

  private void applyParent(IPSItemFilter domain, ItemFilter parentDto) {
    if (parentDto == null) {
      return;
    }
    IPSGuid parentId = toIpsGuid(parentDto.getFilterId());
    String parentName = parentDto.getName();
    if (parentId == null && StringUtils.isBlank(parentName)) {
      domain.setParentFilter(null);
      return;
    }
    IPSItemFilter parent = resolveParent(parentId, parentName);
    if (parent == null) {
      throw new IllegalArgumentException("parent filter not found");
    }
    if (domain.getGUID() != null
        && parent.getGUID() != null
        && domain.getGUID().equals(parent.getGUID())) {
      throw new IllegalArgumentException("item filter cannot parent itself");
    }
    domain.setParentFilter(parent);
  }

  private IPSItemFilter resolveParent(IPSGuid parentId, String parentName) {
    if (parentId != null) {
      try {
        return filterService.loadFilter(parentId);
      } catch (PSNotFoundException e) {
        return null;
      }
    }
    if (StringUtils.isBlank(parentName) || !isSafeFilterKey(parentName.trim())) {
      return null;
    }
    return filterService.findFilterByNameSafe(parentName.trim()).orElse(null);
  }

  private ItemFilter reload(IPSItemFilter saved) {
    if (saved == null || saved.getGUID() == null) {
      return saved == null ? null : copyFilter(saved);
    }
    try {
      return copyFilter(filterService.loadFilter(saved.getGUID()));
    } catch (PSNotFoundException e) {
      log.debug("Could not reload item filter after write: {}", e.getMessage());
      return copyFilter(saved);
    }
  }

  private void assertNameUnique(String name) {
    List<IPSCatalogSummary> existing = designWs.findItemFilters(name);
    if (existing == null) {
      return;
    }
    for (IPSCatalogSummary summary : existing) {
      if (summary != null
          && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
        throw new WebApplicationException("Item filter already exists: " + name, 409);
      }
    }
  }

  private static String requireValidName(String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("name is required");
    }
    String name = raw.trim();
    if (containsWhitespace(name)) {
      throw new IllegalArgumentException("name cannot contain whitespace");
    }
    if (name.contains("*") || name.contains("%")) {
      throw new IllegalArgumentException("name must not contain wildcards");
    }
    if (!isSafeFilterKey(name)) {
      throw new IllegalArgumentException("invalid name");
    }
    return name;
  }

  private static boolean containsWhitespace(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isWhitespace(name.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  static IPSGuid toIpsGuid(Guid guid) {
    if (guid == null) {
      return null;
    }
    String sv = guid.getStringValue();
    if (StringUtils.isNotBlank(sv)) {
      try {
        return new PSGuid(sv.trim());
      } catch (IllegalArgumentException e) {
        log.debug("Invalid item filter GUID string: {}", e.getMessage());
      }
    }
    if (guid.getType() > 0 && guid.getUuid() > 0) {
      PSTypeEnum type = PSTypeEnum.valueOf(guid.getType());
      if (type != null) {
        return new PSGuid(type, guid.getUuid());
      }
    }
    if (guid.getLongValue() != 0) {
      return new PSGuid(guid.getLongValue());
    }
    return null;
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
          "Request session/user required for item filter design write", Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  static boolean isAlreadyExistsFailure(IllegalArgumentException e) {
    return e != null && StringUtils.containsIgnoreCase(e.getMessage(), "already exists");
  }

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
      return StringUtils.containsIgnoreCase(e != null ? e.getMessage() : null, "depend");
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

  private RuntimeException mapSaveOrDeleteFailure(String verb, PSErrorsException e) {
    if (isNotLockedError(e)) {
      return new WebApplicationException(
          "Could not " + verb + " item filter; design lock required or held by another user", 409);
    }
    if (isDependencyError(e)) {
      return new WebApplicationException(
          "Item filter is associated with a content list or other dependents", 409);
    }
    log.error("Failed to {} item filter via system design WS", verb, e);
    return new IllegalStateException("Failed to " + verb + " item filter", e);
  }
}
