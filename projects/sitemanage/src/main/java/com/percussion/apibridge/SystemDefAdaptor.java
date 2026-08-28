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

import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.rest.systemdef.ISystemDefAdaptor;
import com.percussion.rest.systemdef.SystemDefDesignLockException;
import com.percussion.rest.systemdef.SystemDefDetail;
import com.percussion.rest.systemdef.SystemDefFieldSummary;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Catalog and write of the content-editor system definition ({@link PSContentEditorSystemDef}).
 *
 * <p>Workbench parity: loads and saves via {@link IPSContentDesignWs#loadContentEditorSystemDef} /
 * {@link IPSContentDesignWs#saveContentEditorSystemDef} (same design web service SOAP uses), not
 * {@code PSServer.getContentEditorSystemDef()} alone.
 *
 * <p>Admin (Design) only — same {@link IPSUserService#isAdminUser} gate as shared-field design
 * mutations. There is no global JAX-RS Admin filter on {@code /services/systemdef}. Writes acquire
 * the system-def design lock for the request and release it on save.
 */
@PSSiteManageBean
public class SystemDefAdaptor implements ISystemDefAdaptor {

  private static final Logger log = LogManager.getLogger(SystemDefAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to read or write the system definition";

  private static final List<String> DESIGN_GAPS =
      List.of(
          "System def field create / delete not supported via this API",
          "Control properties, stylesheets, and application flow not exposed",
          "Shared field groups are a separate catalog (Developer Shared Fields)");

  private final IPSContentDesignWs designWs;
  private final Supplier<PSContentEditorSystemDef> systemDefLoader;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public SystemDefAdaptor() {
    this(PSContentWsLocator.getContentDesignWebservice(), null);
  }

  /**
   * Package-visible for unit tests that inject a fake design web service. {@code null} adminChecker
   * uses {@link #isCurrentUserAdmin()}.
   */
  SystemDefAdaptor(IPSContentDesignWs designWs, BooleanSupplier adminChecker) {
    this.designWs = designWs;
    this.systemDefLoader =
        () -> loadSystemDefFromDesignWs(designWs, currentSession(), currentUser());
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  /**
   * Package-visible for unit tests that inject a fake system def source. Admin is allowed so
   * mapping tests can focus on catalog shape. Writes require {@link IPSContentDesignWs}.
   */
  SystemDefAdaptor(Supplier<PSContentEditorSystemDef> systemDefLoader) {
    this(systemDefLoader, () -> true);
  }

  /**
   * Package-visible for unit tests that inject a fake system def source and Admin gate. {@code
   * null} adminChecker uses {@link #isCurrentUserAdmin()}.
   */
  SystemDefAdaptor(
      Supplier<PSContentEditorSystemDef> systemDefLoader, BooleanSupplier adminChecker) {
    this.designWs = null;
    this.systemDefLoader = systemDefLoader;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  /**
   * Production load path used by the default constructor. Package-visible so unit tests can
   * exercise design-WS success, {@link PSErrorException} wrapping, and absent request session/user
   * without mocking static locators.
   */
  static PSContentEditorSystemDef loadSystemDefFromDesignWs(
      IPSContentDesignWs designWs, String sessionId, String user) {
    try {
      return designWs.loadContentEditorSystemDef(false, false, sessionId, user);
    } catch (PSErrorException e) {
      log.error("Failed to load content editor system def via design WS", e);
      throw new IllegalStateException("Failed to load system def", e);
    }
  }

  private void requireDesignWs() {
    if (designWs == null) {
      throw new IllegalStateException("System def design web service is not available");
    }
  }

  private static void requireSessionUserForWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for system def design write", Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  private PSContentEditorSystemDef loadSystemDefLocked(String session, String user) {
    try {
      PSContentEditorSystemDef def =
          designWs.loadContentEditorSystemDef(true, false, session, user);
      if (def == null) {
        throw new IllegalStateException("Failed to load system def for write");
      }
      return def;
    } catch (PSLockErrorException e) {
      throw mapLockConflict(e);
    } catch (PSErrorException e) {
      log.error("Failed to load content editor system def for write", e);
      throw new IllegalStateException("Failed to load system def for write", e);
    }
  }

  private void saveSystemDef(PSContentEditorSystemDef def, String session, String user) {
    try {
      designWs.saveContentEditorSystemDef(def, true, session, user);
    } catch (PSLockErrorException e) {
      throw mapLockConflict(e);
    } catch (PSErrorException e) {
      log.error("Failed to save content editor system def", e);
      throw new IllegalStateException("Failed to save system def", e);
    }
  }

  static SystemDefDesignLockException mapLockConflict(PSLockErrorException e) {
    String locker = e != null ? e.getLocker() : null;
    if (StringUtils.isNotBlank(locker)) {
      return new SystemDefDesignLockException(
          "Could not save system definition; locked by " + locker, e);
    }
    return new SystemDefDesignLockException(
        "Could not save system definition; design lock required", e);
  }

  /**
   * Apply {@code searchable} then occurrence. {@code occurrence} and {@code required} both map to
   * the same object-store dimension. When both are present they must agree ({@code required=true}
   * with {@code required}/{@code oneOrMore}; {@code required=false} with {@code
   * optional}/{@code zeroOrMore}/{@code count}); otherwise this throws {@link
   * IllegalArgumentException}. When they agree, {@code occurrence} is applied. {@code required} is
   * used only when {@code occurrence} is omitted.
   */
  static void applyFieldPatches(PSFieldSet fieldSet, List<SystemDefFieldSummary> patches) {
    if (patches == null || patches.isEmpty()) {
      return;
    }
    if (fieldSet == null) {
      throw new IllegalArgumentException("System def has no field set");
    }
    for (SystemDefFieldSummary patch : patches) {
      if (patch == null || StringUtils.isBlank(patch.getName())) {
        continue;
      }
      PSField field = fieldSet.findFieldByName(patch.getName(), false);
      if (field == null) {
        throw new IllegalArgumentException("Unknown field: " + patch.getName());
      }
      if (patch.getSearchable() != null) {
        field.setUserSearchable(patch.getSearchable());
      }
      applyOccurrenceOrRequired(field, patch);
    }
  }

  static void applyOccurrenceOrRequired(PSField field, SystemDefFieldSummary patch) {
    boolean hasOccurrence = StringUtils.isNotBlank(patch.getOccurrence());
    boolean hasRequired = patch.getRequired() != null;
    if (hasOccurrence) {
      Integer dim = occurrenceFromApi(patch.getOccurrence());
      if (dim == null) {
        throw new IllegalArgumentException(
            "Invalid occurrence for field " + patch.getName() + ": " + patch.getOccurrence());
      }
      if (hasRequired
          && Boolean.TRUE.equals(patch.getRequired()) != occurrenceImpliesRequired(dim)) {
        throw new IllegalArgumentException(
            "occurrence and required conflict for field " + patch.getName());
      }
      setOccurrenceDimension(field, dim, patch.getName(), patch.getOccurrence());
    } else if (hasRequired) {
      int dim =
          Boolean.TRUE.equals(patch.getRequired())
              ? PSField.OCCURRENCE_DIMENSION_REQUIRED
              : PSField.OCCURRENCE_DIMENSION_OPTIONAL;
      try {
        field.setOccurrenceDimension(dim, null);
      } catch (PSSystemValidationException e) {
        throw new IllegalArgumentException(
            "Invalid required flag for field " + patch.getName(), e);
      }
    }
  }

  static boolean occurrenceImpliesRequired(int dimension) {
    return dimension == PSField.OCCURRENCE_DIMENSION_REQUIRED
        || dimension == PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE;
  }

  private static void setOccurrenceDimension(
      PSField field, int dim, String fieldName, String occurrenceLabel) {
    try {
      field.setOccurrenceDimension(dim, null);
    } catch (PSSystemValidationException e) {
      throw new IllegalArgumentException(
          "Invalid occurrence for field " + fieldName + ": " + occurrenceLabel, e);
    }
  }

  static Integer occurrenceFromApi(String occurrence) {
    if (occurrence == null) {
      return null;
    }
    return switch (occurrence) {
      case "optional" -> PSField.OCCURRENCE_DIMENSION_OPTIONAL;
      case "required" -> PSField.OCCURRENCE_DIMENSION_REQUIRED;
      case "oneOrMore" -> PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE;
      case "zeroOrMore" -> PSField.OCCURRENCE_DIMENSION_ZERO_OR_MORE;
      case "count" -> PSField.OCCURRENCE_DIMENSION_COUNT;
      default -> null;
    };
  }

  @Override
  public SystemDefDetail getSystemDef(URI baseUri) {
    requireAdmin();
    // baseUri reserved for HATEOAS; exceptions propagate to JAX-RS mappers
    return toDetail(systemDefLoader.get());
  }

  @Override
  public SystemDefDetail updateSystemDef(URI baseUri, SystemDefDetail body) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSystemDef def = loadSystemDefLocked(session, user);
    applyFieldPatches(def.getFieldSet(), body.getFields());
    saveSystemDef(def, session, user);
    return toDetail(def);
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

  /**
   * Production Admin check via {@link IPSUserService}. Used when Spring wires the no-arg ctor and
   * {@link #adminChecker} is the instance method reference.
   */
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

  /** Package-visible for unit tests. */
  static SystemDefDetail toDetail(PSContentEditorSystemDef def) {
    SystemDefDetail d = new SystemDefDetail();
    if (def != null) {
      d.setCacheTimeoutMinutes(def.getCacheTimeout());
      d.setFields(mapFields(def.getFieldSet()));
    } else {
      d.setFields(List.of());
    }
    d.setFieldCount(d.getFields().size());
    d.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return d;
  }

  static List<SystemDefFieldSummary> mapFields(PSFieldSet fieldSet) {
    List<SystemDefFieldSummary> out = new ArrayList<>();
    if (fieldSet == null) {
      return out;
    }
    PSField[] all = fieldSet.getAllFields();
    if (all == null) {
      return out;
    }
    for (PSField field : all) {
      if (field == null || StringUtils.isBlank(field.getSubmitName())) {
        continue;
      }
      SystemDefFieldSummary f = new SystemDefFieldSummary();
      f.setName(field.getSubmitName());
      f.setDataType(field.getDataType());
      f.setSearchable(field.isUserSearchable());
      f.setReadOnly(field.isReadOnly());
      int occurrence = field.getOccurrenceDimension(null);
      f.setRequired(
          occurrence == PSField.OCCURRENCE_DIMENSION_REQUIRED
              || occurrence == PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE);
      f.setOccurrence(mapOccurrence(occurrence));
      out.add(f);
    }
    out.sort(
        Comparator.comparing(
            SystemDefFieldSummary::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  static String mapOccurrence(int dimension) {
    return switch (dimension) {
      case PSField.OCCURRENCE_DIMENSION_OPTIONAL -> "optional";
      case PSField.OCCURRENCE_DIMENSION_REQUIRED -> "required";
      case PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE -> "oneOrMore";
      case PSField.OCCURRENCE_DIMENSION_ZERO_OR_MORE -> "zeroOrMore";
      case PSField.OCCURRENCE_DIMENSION_COUNT -> "count";
      default -> "unknown";
    };
  }
}
