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

import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSContainerLocator;
import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSSearchProperties;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.design.objectstore.PSTableRef;
import com.percussion.design.objectstore.PSTableSet;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUISet;
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
import java.util.Iterator;
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
          "Control properties, stylesheets, and application flow not exposed",
          "Shared field groups are a separate catalog (Developer Shared Fields)");

  private static final int MAX_FIELD_NAME_LENGTH = 50;

  private static final String DEFAULT_TEXT_CONTROL = "sys_EditBox";

  private static final String DEFAULT_TABLE_ALIAS = "CONTENTSTATUS";

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

  static String validateFieldName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name is required");
    }
    String trimmed = name.trim();
    if (containsWhitespace(trimmed)) {
      throw new IllegalArgumentException("name cannot contain spaces");
    }
    if (trimmed.length() > MAX_FIELD_NAME_LENGTH) {
      throw new IllegalArgumentException("name exceeds maximum length");
    }
    if (!isSafeFieldName(trimmed)) {
      throw new IllegalArgumentException("name contains invalid path characters");
    }
    char first = trimmed.charAt(0);
    if (!Character.isLetter(first)) {
      throw new IllegalArgumentException("name must start with a letter");
    }
    for (int i = 1; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '_') {
        throw new IllegalArgumentException("name must be letters, digits, or underscore");
      }
    }
    return trimmed;
  }

  static boolean isSafeFieldName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return !name.contains("..")
        && name.indexOf('/') < 0
        && name.indexOf('\\') < 0
        && name.indexOf('\0') < 0;
  }

  private static boolean containsWhitespace(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isWhitespace(name.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  static String columnNameForField(String fieldName) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fieldName.length(); i++) {
      char c = fieldName.charAt(i);
      if (Character.isLetterOrDigit(c) || c == '_') {
        sb.append(Character.toUpperCase(c));
      }
    }
    if (sb.length() == 0) {
      throw new IllegalArgumentException("name does not yield a valid column");
    }
    return sb.toString();
  }

  static String tableAliasForSystemDef(PSContentEditorSystemDef def) {
    if (def == null || def.getContainerLocator() == null) {
      return DEFAULT_TABLE_ALIAS;
    }
    PSContainerLocator loc = def.getContainerLocator();
    Iterator<?> sets = loc.getTableSets();
    if (sets != null) {
      while (sets.hasNext()) {
        Object o = sets.next();
        if (!(o instanceof PSTableSet ts)) {
          continue;
        }
        Iterator<?> refs = ts.getTableRefs();
        if (refs == null) {
          continue;
        }
        while (refs.hasNext()) {
          Object r = refs.next();
          if (r instanceof PSTableRef ref) {
            if (StringUtils.isNotBlank(ref.getAlias())) {
              return ref.getAlias();
            }
            if (StringUtils.isNotBlank(ref.getName())) {
              return ref.getName();
            }
          }
        }
      }
    }
    return DEFAULT_TABLE_ALIAS;
  }

  /**
   * Persistable TYPE_SYSTEM field with backend column locator, default text mapping, and display
   * mapping ({@code sys_EditBox}). Control/stylesheet/flow write is a later slice.
   */
  static PSField addPersistableField(PSContentEditorSystemDef def, SystemDefFieldSummary body) {
    if (def == null) {
      throw new IllegalArgumentException("system def is required");
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String fieldName = validateFieldName(body.getName());
    PSFieldSet fieldSet = def.getFieldSet();
    if (fieldSet == null) {
      throw new IllegalArgumentException("System def has no field set");
    }
    PSField field = newPersistableSystemField(def, fieldName, body);
    fieldSet.add(field);
    applyOccurrenceOrRequired(field, body);
    appendDefaultDisplayMapping(def, fieldName);
    return field;
  }

  static PSField newPersistableSystemField(
      PSContentEditorSystemDef def, String fieldName, SystemDefFieldSummary body) {
    String tableAlias = tableAliasForSystemDef(def);
    PSBackEndTable table = new PSBackEndTable(tableAlias);
    PSField field =
        new PSField(
            PSField.TYPE_SYSTEM, fieldName, new PSBackEndColumn(table, columnNameForField(fieldName)));
    String dataType =
        body == null || StringUtils.isBlank(body.getDataType())
            ? PSField.DT_TEXT
            : body.getDataType().trim();
    try {
      field.setDataType(dataType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid dataType for field " + fieldName + ": " + dataType, e);
    }
    if (PSField.DT_TEXT.equals(field.getDataType())) {
      field.setMimeType("text/plain");
      field.setDataFormat("50");
    }
    boolean searchable =
        body == null || body.getSearchable() == null || Boolean.TRUE.equals(body.getSearchable());
    field.setSearchProperties(new PSSearchProperties(searchable));
    try {
      field.setOccurrenceDimension(PSField.OCCURRENCE_DIMENSION_OPTIONAL, null);
    } catch (PSSystemValidationException e) {
      throw new IllegalArgumentException("Invalid occurrence for field " + fieldName, e);
    }
    return field;
  }

  static void appendDefaultDisplayMapping(PSContentEditorSystemDef def, String fieldName) {
    PSUIDefinition ui = def.getUIDefinition();
    if (ui == null) {
      throw new IllegalArgumentException("System def has no UI definition");
    }
    PSDisplayMapper mapper = ui.getDisplayMapper();
    if (mapper == null) {
      throw new IllegalArgumentException("System def has no display mapper");
    }
    if (ui.getMapping(fieldName) != null) {
      return;
    }
    PSUISet uiSet = new PSUISet();
    uiSet.setLabel(new PSDisplayText(fieldName + ":"));
    uiSet.setErrorLabel(new PSDisplayText(fieldName + ":"));
    uiSet.setControl(new PSControlRef(DEFAULT_TEXT_CONTROL));
    ui.appendMapping(mapper, new PSDisplayMapping(fieldName, uiSet));
  }

  static boolean removeFieldAndMapping(PSContentEditorSystemDef def, String fieldName) {
    if (def == null || def.getFieldSet() == null || StringUtils.isBlank(fieldName)) {
      return false;
    }
    PSField field = def.getFieldSet().getFieldByName(fieldName);
    if (field == null) {
      return false;
    }
    if (field.isSystemMandatory()) {
      throw new IllegalArgumentException("System-mandatory field cannot be deleted");
    }
    if (field.isSystemInternal()) {
      throw new IllegalArgumentException("System-internal field cannot be deleted");
    }
    String actual = field.getSubmitName();
    def.getFieldSet().remove(actual);
    PSUIDefinition ui = def.getUIDefinition();
    if (ui != null) {
      removeDisplayMapping(ui.getDisplayMapper(), actual);
    }
    return true;
  }

  /**
   * Index-based mapping removal. {@link PSDisplayMapper#removeMapping} uses {@code
   * Iterator.remove()}, which {@code PSConcurrentIterator} rejects.
   */
  static boolean removeDisplayMapping(PSDisplayMapper mapper, String fieldRef) {
    if (mapper == null || StringUtils.isBlank(fieldRef)) {
      return false;
    }
    for (int i = 0; i < mapper.size(); i++) {
      Object o = mapper.get(i);
      if (!(o instanceof PSDisplayMapping mapping)) {
        continue;
      }
      if (fieldRef.equals(mapping.getFieldRef())) {
        mapper.remove(i);
        return true;
      }
      if (mapping.getDisplayMapper() != null
          && removeDisplayMapping(mapping.getDisplayMapper(), fieldRef)) {
        return true;
      }
    }
    return false;
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

  @Override
  public SystemDefDetail addField(URI baseUri, SystemDefFieldSummary body) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String fieldName = validateFieldName(body.getName());
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSystemDef def = loadSystemDefLocked(session, user);
    PSFieldSet fieldSet = def.getFieldSet();
    if (fieldSet == null) {
      throw new IllegalArgumentException("System def has no field set");
    }
    if (fieldSet.getFieldByName(fieldName) != null) {
      throw new WebApplicationException("System field already exists: " + fieldName, 409);
    }
    addPersistableField(def, body);
    saveSystemDef(def, session, user);
    return toDetail(def);
  }

  @Override
  public void deleteField(URI baseUri, String fieldName) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    String validated = validateFieldName(fieldName);
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSystemDef def = loadSystemDefLocked(session, user);
    if (!removeFieldAndMapping(def, validated)) {
      throw new IllegalArgumentException("Unknown field: " + validated);
    }
    saveSystemDef(def, session, user);
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
