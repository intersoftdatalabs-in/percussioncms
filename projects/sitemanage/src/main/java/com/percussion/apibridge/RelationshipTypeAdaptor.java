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

import com.percussion.design.objectstore.PSConditionalEffect;
import com.percussion.design.objectstore.PSEntry;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSProperty;
import com.percussion.design.objectstore.PSPropertySet;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.rest.Guid;
import com.percussion.rest.relationshiptypes.IRelationshipTypeAdaptor;
import com.percussion.rest.relationshiptypes.RelationshipType;
import com.percussion.rest.relationshiptypes.RelationshipTypeEffect;
import com.percussion.rest.relationshiptypes.RelationshipTypeProperty;
import com.percussion.services.catalog.IPSCatalogSummary;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Design catalog adaptor for relationship types and Admin user-type write (SY-03) over {@link
 * IPSSystemDesignWs}. System relationship types are never mutated.
 */
@PSSiteManageBean
@Lazy
public class RelationshipTypeAdaptor implements IRelationshipTypeAdaptor {

  private static final Logger log = LogManager.getLogger(RelationshipTypeAdaptor.class);

  static final String ADMIN_REQUIRED =
      "Admin role required to create, update, or delete user relationship types";

  static final String IMMUTABLE_TYPE = "System relationship types cannot be updated or deleted";

  /** Catalog-level capability notes. Attached on detail only (REST-GAPS-02 list dedup). */
  static final List<String> DESIGN_GAPS =
      List.of(
          "Cloning field override editor not supported via this API",
          "Effect condition and execution-context edit not supported via this API");

  private final IPSSystemDesignWs designWs;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public RelationshipTypeAdaptor() {
    this(PSSystemWsLocator.getSystemDesignWebservice(), null);
  }

  /** Package-visible for unit tests. */
  RelationshipTypeAdaptor(IPSSystemDesignWs designWs) {
    this(designWs, null);
  }

  /** Package-visible for unit tests. */
  RelationshipTypeAdaptor(IPSSystemDesignWs designWs, BooleanSupplier adminChecker) {
    this.designWs = designWs;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public List<RelationshipType> listRelationshipTypes() {
    try {
      List<IPSCatalogSummary> summaries = designWs.findRelationshipTypes(null, null);
      if (summaries == null || summaries.isEmpty()) {
        return List.of();
      }
      List<IPSGuid> guids = new ArrayList<>();
      for (IPSCatalogSummary s : summaries) {
        if (s != null && s.getGUID() != null) {
          guids.add(s.getGUID());
        }
      }
      if (guids.isEmpty()) {
        return List.of();
      }
      String user = currentUser();
      String session = currentSession();
      List<PSRelationshipConfig> configs =
          designWs.loadRelationshipTypes(guids, false, false, session, user);
      List<RelationshipType> out = new ArrayList<>();
      if (configs != null) {
        for (PSRelationshipConfig cfg : configs) {
          if (cfg != null) {
            // REST-GAPS-02: list rows omit identical designGaps; detail re-attaches them.
            out.add(copyConfig(cfg, false));
          }
        }
      }
      return out;
    } catch (PSErrorException | PSErrorResultsException e) {
      throw new RuntimeException("Failed to list relationship types", e);
    }
  }

  @Override
  public RelationshipType findRelationshipType(String idOrName) {
    if (!isSafeRelationshipTypeKey(idOrName)) {
      return null;
    }
    PSRelationshipConfig cfg = resolveConfig(idOrName.trim(), false);
    return cfg != null ? withDesignGaps(copyConfig(cfg, false)) : null;
  }

  @Override
  public RelationshipType createRelationshipType(RelationshipType body) {
    requireAdmin();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String name = requireValidName(body.getName());
    requireSessionUserForDesignWrite();
    String session = currentSession();
    String user = currentUser();
    assertNameUnique(name);

    String copyFrom = StringUtils.trimToNull(body.getCopyFrom());
    PSRelationshipConfig source = null;
    String category;
    if (copyFrom != null) {
      if (!isSafeRelationshipTypeKey(copyFrom)) {
        throw new IllegalArgumentException("copyFrom is invalid");
      }
      source = resolveConfig(copyFrom, false);
      if (source == null) {
        throw new IllegalArgumentException("copyFrom relationship type not found: " + copyFrom);
      }
      category = source.getCategory();
    } else {
      category = resolveCategoryCode(body.getCategory());
      if (category == null) {
        throw new IllegalArgumentException(
            "category is required when copyFrom is not set (code or label, e.g."
                + " rs_activeassembly or Active Assembly)");
      }
    }

    try {
      List<PSRelationshipConfig> created =
          designWs.createRelationshipTypes(
              Collections.singletonList(name),
              Collections.singletonList(category),
              session,
              user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createRelationshipTypes returned empty");
      }
      PSRelationshipConfig config = created.get(0);
      if (source != null) {
        copyMutableFromSource(config, source);
      }
      applyCreateOverrides(config, body, name);
      designWs.saveRelationshipTypes(Collections.singletonList(config), true, session, user);
      PSRelationshipConfig reloaded = resolveConfig(name, false);
      return reloaded != null
          ? withDesignGaps(copyConfig(reloaded, false))
          : withDesignGaps(copyConfig(config, false));
    } catch (WebApplicationException | IllegalStateException | IllegalArgumentException e) {
      throw e;
    } catch (PSLockErrorException e) {
      throw new WebApplicationException(
          "Could not create relationship type; design lock required or held by another user",
          409);
    } catch (PSErrorsException e) {
      throw mapPersistFailure(name, e, "Failed to save new relationship type");
    } catch (PSErrorException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Relationship type already exists: " + name, 409);
      }
      throw mapPersistFailure(name, e, "Failed to create relationship type");
    } catch (Exception e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Relationship type already exists: " + name, 409);
      }
      log.error("Failed to create relationship type {}: {}", name, e.getMessage(), e);
      throw new IllegalStateException("Failed to create relationship type", e);
    }
  }

  @Override
  public RelationshipType updateRelationshipType(String idOrName, RelationshipType body) {
    requireAdmin();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (!isSafeRelationshipTypeKey(idOrName)) {
      return null;
    }
    requireSessionUserForDesignWrite();
    String session = currentSession();
    String user = currentUser();
    String key = idOrName.trim();
    try {
      PSRelationshipConfig unlocked = resolveConfig(key, false);
      if (unlocked == null) {
        return null;
      }
      rejectImmutableMutation(unlocked);
      PSRelationshipConfig locked;
      try {
        locked = resolveConfig(key, true);
      } catch (RuntimeException e) {
        if (e.getCause() instanceof PSErrorResultsException
            || e instanceof WebApplicationException) {
          throw lockConflict("Could not update relationship type");
        }
        throw e;
      }
      if (locked == null) {
        throw lockConflict("Could not update relationship type");
      }
      rejectImmutableMutation(locked);
      applyUpdateFields(locked, body);
      designWs.saveRelationshipTypes(Collections.singletonList(locked), true, session, user);
      PSRelationshipConfig reloaded = resolveConfig(locked.getName(), false);
      return reloaded != null
          ? withDesignGaps(copyConfig(reloaded, false))
          : withDesignGaps(copyConfig(locked, false));
    } catch (WebApplicationException | IllegalArgumentException | IllegalStateException e) {
      throw e;
    } catch (PSErrorsException e) {
      if (isLockFailure(e)) {
        throw lockConflict("Could not update relationship type");
      }
      throw mapPersistFailure(key, e, "Failed to update relationship type");
    } catch (PSErrorException e) {
      throw mapPersistFailure(key, e, "Failed to update relationship type");
    } catch (Exception e) {
      log.error("Failed to update relationship type {}: {}", key, e.getMessage(), e);
      throw new IllegalStateException("Failed to update relationship type", e);
    }
  }

  @Override
  public boolean deleteRelationshipType(String idOrName) {
    requireAdmin();
    if (!isSafeRelationshipTypeKey(idOrName)) {
      return false;
    }
    requireSessionUserForDesignWrite();
    String session = currentSession();
    String user = currentUser();
    String key = idOrName.trim();
    try {
      PSRelationshipConfig current = resolveConfig(key, false);
      if (current == null) {
        return false;
      }
      rejectImmutableMutation(current);
      if (current.getGUID() == null) {
        throw new IllegalStateException(
            "Relationship type '" + key + "' has no GUID; cannot delete");
      }
      PSRelationshipConfig locked;
      try {
        locked = resolveConfig(key, true);
      } catch (RuntimeException e) {
        throw lockConflict("Could not delete relationship type");
      }
      if (locked == null || locked.getGUID() == null) {
        throw lockConflict("Could not delete relationship type");
      }
      rejectImmutableMutation(locked);
      try {
        designWs.deleteRelationshipTypes(
            Collections.singletonList(locked.getGUID()), false, session, user);
      } catch (PSErrorsException e) {
        if (isLockFailure(e)) {
          throw lockConflict("Could not delete relationship type");
        }
        String details = formatErrors(e);
        throw new IllegalArgumentException("Could not delete relationship type: " + details, e);
      }
      return true;
    } catch (IllegalArgumentException | IllegalStateException | WebApplicationException e) {
      throw e;
    } catch (PSErrorException e) {
      throw mapPersistFailure(key, e, "Failed to delete relationship type");
    } catch (Exception e) {
      log.error("Failed to delete relationship type {}: {}", key, e.getMessage(), e);
      throw new IllegalStateException("Failed to delete relationship type", e);
    }
  }

  private PSRelationshipConfig resolveConfig(String idOrName, boolean lock) {
    if (!isSafeRelationshipTypeKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    try {
      // Prefer exact name lookup via find summaries.
      List<IPSCatalogSummary> byName = designWs.findRelationshipTypes(key, null);
      IPSGuid guid = null;
      if (byName != null) {
        for (IPSCatalogSummary s : byName) {
          if (s != null && key.equalsIgnoreCase(StringUtils.defaultString(s.getName()))) {
            guid = s.getGUID();
            break;
          }
        }
      }
      if (guid == null) {
        // GUID string path
        try {
          var parsed = new com.percussion.services.guidmgr.data.PSGuid(key);
          List<IPSCatalogSummary> all = designWs.findRelationshipTypes(null, null);
          if (all != null) {
            int uuid = parsed.getUUID();
            short type = parsed.getType();
            for (IPSCatalogSummary s : all) {
              if (s != null
                  && s.getGUID() != null
                  && s.getGUID().getUUID() == uuid
                  && s.getGUID().getType() == type) {
                guid = s.getGUID();
                break;
              }
            }
          }
        } catch (IllegalArgumentException e) {
          log.debug("Invalid relationship type GUID syntax: {}", e.getMessage());
        }
      }
      if (guid == null) {
        return null;
      }
      String session = currentSession();
      String user = currentUser();
      List<PSRelationshipConfig> loaded =
          designWs.loadRelationshipTypes(
              Collections.singletonList(guid), lock, false, session, user);
      if (loaded == null || loaded.isEmpty()) {
        return null;
      }
      return loaded.get(0);
    } catch (PSErrorResultsException e) {
      if (lock) {
        throw new WebApplicationException(
            "Could not lock relationship type; design lock required or held by another user",
            409);
      }
      throw new RuntimeException("Failed to load relationship type: " + key, e);
    } catch (PSErrorException e) {
      throw new RuntimeException("Failed to resolve relationship type: " + key, e);
    }
  }

  private void applyCreateOverrides(PSRelationshipConfig config, RelationshipType body, String name) {
    if (StringUtils.isNotBlank(body.getLabel())) {
      config.setLabel(body.getLabel().trim());
    } else if (StringUtils.isBlank(config.getLabel())) {
      // Keep copyFrom source label when present; otherwise default to name.
      config.setLabel(name);
    }
    if (body.getDescription() != null) {
      config.setDescription(body.getDescription());
    }
    if (body.getUserProperties() != null && !body.getUserProperties().isEmpty()) {
      applyUserProperties(config, body.getUserProperties());
    }
  }

  private void applyUpdateFields(PSRelationshipConfig config, RelationshipType body) {
    if (StringUtils.isNotBlank(body.getLabel())) {
      config.setLabel(body.getLabel().trim());
    }
    if (body.getDescription() != null) {
      config.setDescription(body.getDescription());
    }
    if (StringUtils.isNotBlank(body.getCategory())) {
      String category = resolveCategoryCode(body.getCategory());
      if (category == null) {
        throw new IllegalArgumentException("invalid category: " + body.getCategory());
      }
      config.setCategory(category);
    }
    // Wire booleans are primitives — clients should round-trip GET then PUT.
    setBooleanSysProp(config, PSRelationshipConfig.RS_ALLOWCLONING, body.isAllowCloning());
    setBooleanSysProp(config, PSRelationshipConfig.RS_USEOWNERREVISION, body.isUseOwnerRevision());
    setBooleanSysProp(
        config, PSRelationshipConfig.RS_USEDEPENDENTREVISION, body.isUseDependentRevision());
    if (body.getUserProperties() != null) {
      applyUserProperties(config, body.getUserProperties());
    }
  }

  private static void copyMutableFromSource(
      PSRelationshipConfig dest, PSRelationshipConfig source) {
    if (StringUtils.isNotBlank(source.getLabel())) {
      dest.setLabel(source.getLabel());
    }
    dest.setDescription(source.getDescription());
    // Copy system property values (cloning / revision flags, etc.)
    for (Object nameObj : PSRelationshipConfig.RS_PROPERTY_NAME_ENUM) {
      if (!(nameObj instanceof String pname)) {
        continue;
      }
      PSProperty srcProp = source.getSysProperty(pname);
      PSProperty destProp = dest.getSysProperty(pname);
      if (srcProp != null && destProp != null && !destProp.isLocked()) {
        destProp.setValue(srcProp.getValue());
      }
    }
    // Copy user-defined properties
    PSPropertySet userProps = new PSPropertySet();
    Iterator<?> userIt = source.getUserDefProperties();
    while (userIt.hasNext()) {
      Object o = userIt.next();
      if (o instanceof PSProperty p) {
        userProps.add((PSProperty) p.clone());
      }
    }
    dest.setUserDefProperties(userProps.iterator());
    // Copy effects (cloned)
    List<PSConditionalEffect> effects = new ArrayList<>();
    Iterator<?> effectIt = source.getEffects();
    while (effectIt.hasNext()) {
      Object o = effectIt.next();
      if (o instanceof PSConditionalEffect ce) {
        effects.add((PSConditionalEffect) ce.clone());
      }
    }
    dest.setEffects(effects.iterator());
  }

  private static void applyUserProperties(
      PSRelationshipConfig config, List<RelationshipTypeProperty> props) {
    PSPropertySet set = new PSPropertySet();
    if (props != null) {
      for (RelationshipTypeProperty p : props) {
        if (p == null || StringUtils.isBlank(p.getName())) {
          continue;
        }
        PSProperty prop = new PSProperty(p.getName().trim());
        prop.setValue(p.getValue());
        set.add(prop);
      }
    }
    config.setUserDefProperties(set.iterator());
  }

  private static void setBooleanSysProp(
      PSRelationshipConfig config, String propName, boolean value) {
    PSProperty prop = config.getSysProperty(propName);
    if (prop == null) {
      return;
    }
    if (prop.isLocked()) {
      return;
    }
    prop.setValue(Boolean.valueOf(value));
    // Keep the string map used by isCloningAllowed / getSystemProperty in sync.
    String str = value ? PSProperty.XML_BOOL_YES : PSProperty.XML_BOOL_NO;
    config.getSystemProperties().put(propName, str);
  }

  private void assertNameUnique(String name) {
    try {
      List<IPSCatalogSummary> existing = designWs.findRelationshipTypes(name, null);
      if (existing == null) {
        return;
      }
      for (IPSCatalogSummary summary : existing) {
        if (summary != null
            && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
          throw new WebApplicationException("Relationship type already exists: " + name, 409);
        }
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorException e) {
      throw new IllegalStateException("Failed to check relationship type existence", e);
    }
  }

  private static void rejectImmutableMutation(PSRelationshipConfig config) {
    if (config != null && config.isSystem()) {
      throw new WebApplicationException(IMMUTABLE_TYPE, 409);
    }
  }

  static String resolveCategoryCode(String categoryOrLabel) {
    if (StringUtils.isBlank(categoryOrLabel)) {
      return null;
    }
    String key = categoryOrLabel.trim();
    for (PSEntry entry : PSRelationshipConfig.CATEGORY_ENUM) {
      if (entry == null) {
        continue;
      }
      if (key.equalsIgnoreCase(entry.getValue())) {
        return entry.getValue();
      }
      if (entry.getLabel() != null
          && entry.getLabel().getText() != null
          && key.equalsIgnoreCase(entry.getLabel().getText())) {
        return entry.getValue();
      }
    }
    return null;
  }

  static String requireValidName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name is required");
    }
    String trimmed = name.trim();
    for (int i = 0; i < trimmed.length(); i++) {
      if (Character.isWhitespace(trimmed.charAt(i))) {
        throw new IllegalArgumentException("name cannot contain whitespace");
      }
    }
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("name must not contain wildcards");
    }
    if (!isSafeRelationshipTypeKey(trimmed)) {
      throw new IllegalArgumentException("name is invalid");
    }
    return trimmed;
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

  private static void requireSessionUserForDesignWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for relationship type design session",
          Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    Object session = PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    return session != null ? session.toString() : null;
  }

  private static String currentUser() {
    Object user = PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
    return user != null ? user.toString() : null;
  }

  private static WebApplicationException lockConflict(String prefix) {
    return new WebApplicationException(
        prefix + "; design lock required or held by another user", 409);
  }

  private static RuntimeException mapPersistFailure(String name, Exception e, String prefix) {
    log.error("{} {}: {}", prefix, name, e.getMessage(), e);
    if (e instanceof PSErrorsException pe && isLockFailure(pe)) {
      return lockConflict(prefix);
    }
    return new IllegalStateException(prefix + ": " + e.getMessage(), e);
  }

  private static boolean isLockFailure(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (err == null) {
        continue;
      }
      String msg = err.toString().toLowerCase(Locale.ROOT);
      if (msg.contains("lock") || err instanceof PSLockErrorException) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAlreadyExistsFailure(Throwable e) {
    Throwable cur = e;
    while (cur != null) {
      String msg = StringUtils.defaultString(cur.getMessage()).toLowerCase(Locale.ROOT);
      if (msg.contains("must be unique") || msg.contains("already exists")) {
        return true;
      }
      cur = cur.getCause();
    }
    return false;
  }

  private static String formatErrors(PSErrorsException e) {
    if (e == null || e.getErrors() == null || e.getErrors().isEmpty()) {
      return e != null ? StringUtils.defaultString(e.getMessage()) : "";
    }
    StringBuilder sb = new StringBuilder();
    for (Object err : e.getErrors().values()) {
      if (err == null) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append("; ");
      }
      sb.append(err);
    }
    return sb.toString();
  }

  private RelationshipType copyConfig(PSRelationshipConfig cfg, boolean includeDesignGaps) {
    RelationshipType ret = new RelationshipType();
    ret.setName(cfg.getName());
    ret.setLabel(cfg.getLabel());
    ret.setDescription(cfg.getDescription());
    ret.setType(cfg.getType());
    ret.setCategory(cfg.getCategory());
    ret.setCategoryLabel(categoryLabel(cfg.getCategory()));
    ret.setSystemType(cfg.isSystem());
    ret.setUserType(cfg.isUser());
    ret.setAllowCloning(cfg.isCloningAllowed());
    ret.setUseOwnerRevision(cfg.useOwnerRevision());
    ret.setUseDependentRevision(cfg.useDependentRevision());
    if (cfg.isAssinedId()) {
      ret.setGuid(copyGuid(cfg.getGUID()));
    }

    List<RelationshipTypeEffect> effects = new ArrayList<>();
    Iterator<?> effectIt = cfg.getEffects();
    while (effectIt.hasNext()) {
      Object o = effectIt.next();
      if (o instanceof PSConditionalEffect ce) {
        effects.add(copyEffect(ce));
      }
    }
    ret.setEffects(effects);

    ret.setSystemProperties(mapToProps(cfg.getSystemProperties()));
    ret.setUserProperties(mapToProps(cfg.getUserProperties()));
    ret.setDesignGaps(includeDesignGaps ? new ArrayList<>(DESIGN_GAPS) : null);
    return ret;
  }

  /** Attach catalog designGaps to a list projection for the detail response. */
  static RelationshipType withDesignGaps(RelationshipType listRow) {
    if (listRow == null) {
      return null;
    }
    listRow.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return listRow;
  }

  private RelationshipTypeEffect copyEffect(PSConditionalEffect ce) {
    RelationshipTypeEffect e = new RelationshipTypeEffect();
    e.setActivationEndPoint(ce.getActivationEndPoint());
    PSExtensionCall call = ce.getEffect();
    if (call != null) {
      e.setName(call.getName());
      if (call.getExtensionRef() != null) {
        e.setExtensionRef(call.getExtensionRef().toString());
      }
    }
    return e;
  }

  private static List<RelationshipTypeProperty> mapToProps(Map<String, String> map) {
    List<RelationshipTypeProperty> out = new ArrayList<>();
    if (map == null) {
      return out;
    }
    for (Map.Entry<String, String> entry : map.entrySet()) {
      out.add(new RelationshipTypeProperty(entry.getKey(), entry.getValue()));
    }
    return out;
  }

  private static String categoryLabel(String category) {
    if (category == null || category.isBlank()) {
      return null;
    }
    for (PSEntry entry : PSRelationshipConfig.CATEGORY_ENUM) {
      if (entry != null && category.equals(entry.getValue())) {
        if (entry.getLabel() != null && entry.getLabel().getText() != null) {
          return entry.getLabel().getText();
        }
      }
    }
    return category;
  }

  private Guid copyGuid(IPSGuid guid) {
    Guid g = new Guid();
    g.setHostId(guid.getHostId());
    g.setLongValue(guid.longValue());
    g.setStringValue(guid.toString());
    g.setType(guid.getType());
    g.setUuid(guid.getUUID());
    g.setUntypedString(guid.toStringUntyped());
    return g;
  }

  /**
   * Single path component / guid token only — reject traversal and separators ({@code
   * java/path-injection}).
   */
  static boolean isSafeRelationshipTypeKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }
}
