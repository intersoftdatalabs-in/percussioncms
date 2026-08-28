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

import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndCredential;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSContainerLocator;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSSearchProperties;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.design.objectstore.PSTableLocator;
import com.percussion.design.objectstore.PSTableRef;
import com.percussion.design.objectstore.PSTableSet;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.rest.sharedfields.ISharedFieldsAdaptor;
import com.percussion.rest.sharedfields.SharedFieldDesignLockException;
import com.percussion.rest.sharedfields.SharedFieldGroupDetail;
import com.percussion.rest.sharedfields.SharedFieldGroupSummary;
import com.percussion.rest.sharedfields.SharedFieldNotFoundException;
import com.percussion.rest.sharedfields.SharedFieldSummary;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.util.PSCollection;
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
 * Catalog and write of content-editor shared field groups ({@link PSContentEditorSharedDef}).
 *
 * <p>Workbench parity: loads and saves via {@link IPSContentDesignWs#loadContentEditorSharedDef} /
 * {@link IPSContentDesignWs#saveContentEditorSharedDef} (same design web service SOAP uses), not
 * {@code PSServer.getContentEditorSharedDef()} alone.
 *
 * <p>Admin (Design) only — same {@link IPSUserService#isAdminUser} gate as content-type design
 * mutations. There is no global JAX-RS Admin filter on {@code /services/sharedfields}. Writes
 * acquire the shared-def design lock for the request and release it on save.
 */
@PSSiteManageBean
public class SharedFieldsAdaptor implements ISharedFieldsAdaptor {

  private static final Logger log = LogManager.getLogger(SharedFieldsAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to read or write shared field groups";

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Field control / choice write not supported via this API",
          "System def (global fields) is a separate catalog (later slice)");

  private static final int MAX_FIELD_NAME_LENGTH = 50;

  private static final String DEFAULT_TEXT_CONTROL = "sys_EditBox";

  private final IPSContentDesignWs designWs;
  private final Supplier<PSContentEditorSharedDef> sharedDefLoader;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public SharedFieldsAdaptor() {
    this(PSContentWsLocator.getContentDesignWebservice(), null);
  }

  /**
   * Package-visible for unit tests that inject a fake design web service. {@code null}
   * adminChecker uses {@link #isCurrentUserAdmin()}.
   */
  SharedFieldsAdaptor(IPSContentDesignWs designWs, BooleanSupplier adminChecker) {
    this.designWs = designWs;
    this.sharedDefLoader =
        () ->
            loadSharedDefFromDesignWs(
                designWs, currentSession(), currentUser());
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  /**
   * Package-visible for unit tests that inject a fake shared def source. Admin is allowed so
   * mapping tests can focus on catalog shape. Writes require {@link IPSContentDesignWs}.
   */
  SharedFieldsAdaptor(Supplier<PSContentEditorSharedDef> sharedDefLoader) {
    this(sharedDefLoader, () -> true);
  }

  /**
   * Package-visible for unit tests that inject a fake shared def source and Admin gate. {@code
   * null} adminChecker uses {@link #isCurrentUserAdmin()}.
   */
  SharedFieldsAdaptor(
      Supplier<PSContentEditorSharedDef> sharedDefLoader, BooleanSupplier adminChecker) {
    this.designWs = null;
    this.sharedDefLoader = sharedDefLoader;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  /**
   * Production load path used by the default constructor. Package-visible so unit tests can
   * exercise design-WS success, {@link PSErrorException} wrapping, and absent request session/user
   * without mocking static locators.
   */
  static PSContentEditorSharedDef loadSharedDefFromDesignWs(
      IPSContentDesignWs designWs, String sessionId, String user) {
    try {
      return designWs.loadContentEditorSharedDef(false, false, sessionId, user);
    } catch (PSErrorException e) {
      log.error("Failed to load content editor shared def via design WS", e);
      throw new IllegalStateException("Failed to load shared def", e);
    }
  }

  private void requireDesignWs() {
    if (designWs == null) {
      throw new IllegalStateException("Shared field design web service is not available");
    }
  }

  private static void requireSessionUserForWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for shared field design write",
          Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  private PSContentEditorSharedDef loadSharedDefLocked(String session, String user) {
    try {
      PSContentEditorSharedDef def =
          designWs.loadContentEditorSharedDef(true, false, session, user);
      return def != null ? def : new PSContentEditorSharedDef();
    } catch (PSLockErrorException e) {
      throw mapLockConflict(e);
    } catch (PSErrorException e) {
      log.error("Failed to load content editor shared def for write", e);
      throw new IllegalStateException("Failed to load shared def for write", e);
    }
  }

  private void saveSharedDef(PSContentEditorSharedDef def, String session, String user) {
    try {
      designWs.saveContentEditorSharedDef(def, true, session, user);
    } catch (PSLockErrorException e) {
      throw mapLockConflict(e);
    } catch (PSErrorException e) {
      log.error("Failed to save content editor shared def", e);
      throw new IllegalStateException("Failed to save shared def", e);
    }
  }

  static SharedFieldDesignLockException mapLockConflict(PSLockErrorException e) {
    String locker = e != null ? e.getLocker() : null;
    if (StringUtils.isNotBlank(locker)) {
      return new SharedFieldDesignLockException(
          "Could not save shared field group; locked by " + locker, e);
    }
    return new SharedFieldDesignLockException(
        "Could not save shared field group; design lock required", e);
  }

  static String validateGroupName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name is required");
    }
    if (containsWhitespace(name)) {
      throw new IllegalArgumentException("name cannot contain spaces");
    }
    if (name.contains("*")) {
      throw new IllegalArgumentException("name must not contain wildcards");
    }
    if (!isSafeGroupName(name)) {
      throw new IllegalArgumentException("name contains invalid path characters");
    }
    return name;
  }

  static String normalizeFilename(String filename, String groupName) {
    String raw = StringUtils.isBlank(filename) ? groupName + ".xml" : filename.trim();
    if (containsWhitespace(raw)) {
      throw new IllegalArgumentException("filename cannot contain spaces");
    }
    if (!isSafeGroupName(stripXmlSuffix(raw))) {
      throw new IllegalArgumentException("filename contains invalid path characters");
    }
    if (raw.toLowerCase().endsWith(".xml")) {
      return stripXmlSuffix(raw) + ".xml";
    }
    if (raw.contains(".")) {
      throw new IllegalArgumentException("filename must use a .xml extension");
    }
    return raw + ".xml";
  }

  private static String stripXmlSuffix(String filename) {
    if (filename.toLowerCase().endsWith(".xml")) {
      return filename.substring(0, filename.length() - 4);
    }
    return filename;
  }

  private static boolean containsWhitespace(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isWhitespace(name.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Persistable empty group: locator + empty field set + empty display mapper. Fields are added
   * with {@link #addPersistableField}.
   */
  static PSSharedFieldGroup newEmptyGroup(String name, String filename) {
    PSBackEndCredential cred = new PSBackEndCredential("contentCredential");
    cred.setDataSource("");
    PSTableLocator tableLoc = new PSTableLocator(cred);
    PSTableRef tableRef = new PSTableRef(tableNameForGroup(name));
    PSTableSet ts = new PSTableSet(tableLoc, tableRef);
    PSCollection<PSTableSet> tsCol = new PSCollection<>(PSTableSet.class);
    tsCol.add(ts);
    PSContainerLocator loc = new PSContainerLocator(tsCol);
    PSFieldSet fs = new PSFieldSet(name);
    PSDisplayMapper mapper = new PSDisplayMapper(name);
    PSUIDefinition ui = new PSUIDefinition(mapper);
    PSSharedFieldGroup group = new PSSharedFieldGroup(name, loc, fs, ui);
    group.setFilename(filename);
    return group;
  }

  static String tableNameForGroup(String name) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (Character.isLetterOrDigit(c)) {
        sb.append(Character.toUpperCase(c));
      } else if (c == '_' && sb.length() > 0) {
        sb.append('_');
      }
    }
    return sb.length() == 0 ? "SHARED_FIELDS" : sb.toString();
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
    if (!isSafeGroupName(trimmed)) {
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

  static String tableAliasForGroup(PSSharedFieldGroup group) {
    if (group == null || group.getLocator() == null) {
      return tableNameForGroup(group != null ? group.getName() : "SHARED");
    }
    Iterator<?> sets = group.getLocator().getTableSets();
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
    return tableNameForGroup(group.getName());
  }

  /**
   * Persistable TYPE_SHARED field with backend column locator, default text mapping, and display
   * mapping ({@code sys_EditBox}). Control/choice write is a later slice.
   */
  static PSField addPersistableField(PSSharedFieldGroup group, SharedFieldSummary body) {
    if (group == null) {
      throw new IllegalArgumentException("group is required");
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String fieldName = validateFieldName(body.getName());
    PSFieldSet fieldSet = group.getFieldSet();
    if (fieldSet == null) {
      throw new IllegalArgumentException("Shared field group has no field set");
    }
    PSField field = newPersistableSharedField(group, fieldName, body);
    fieldSet.add(field);
    applyOccurrenceOrRequired(field, body);
    appendDefaultDisplayMapping(group, fieldName);
    return field;
  }

  static PSField newPersistableSharedField(
      PSSharedFieldGroup group, String fieldName, SharedFieldSummary body) {
    String tableAlias = tableAliasForGroup(group);
    PSBackEndTable table = new PSBackEndTable(tableAlias);
    PSField field =
        new PSField(
            PSField.TYPE_SHARED, fieldName, new PSBackEndColumn(table, columnNameForField(fieldName)));
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

  static void appendDefaultDisplayMapping(PSSharedFieldGroup group, String fieldName) {
    PSUIDefinition ui = group.getUIDefinition();
    if (ui == null) {
      throw new IllegalArgumentException("Shared field group has no UI definition");
    }
    PSDisplayMapper mapper = ui.getDisplayMapper();
    if (mapper == null) {
      throw new IllegalArgumentException("Shared field group has no display mapper");
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

  static boolean removeFieldAndMapping(PSSharedFieldGroup group, String fieldName) {
    if (group == null || group.getFieldSet() == null || StringUtils.isBlank(fieldName)) {
      return false;
    }
    PSField field = group.getFieldSet().findFieldByName(fieldName, false);
    if (field == null) {
      return false;
    }
    String actual = field.getSubmitName();
    group.getFieldSet().remove(actual);
    PSUIDefinition ui = group.getUIDefinition();
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

  /**
   * Apply {@code searchable} then occurrence. {@code occurrence} and {@code required} both map to
   * the same object-store dimension. When both are present they must agree ({@code required=true}
   * with {@code required}/{@code oneOrMore}; {@code required=false} with {@code
   * optional}/{@code zeroOrMore}/{@code count}); otherwise this throws {@link
   * IllegalArgumentException}. When they agree, {@code occurrence} is applied. {@code required} is
   * used only when {@code occurrence} is omitted.
   */
  static void applyFieldPatches(PSSharedFieldGroup group, List<SharedFieldSummary> patches) {
    if (patches == null || patches.isEmpty() || group == null) {
      return;
    }
    PSFieldSet fieldSet = group.getFieldSet();
    if (fieldSet == null) {
      throw new IllegalArgumentException("Shared field group has no field set");
    }
    for (SharedFieldSummary patch : patches) {
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

  static void applyOccurrenceOrRequired(PSField field, SharedFieldSummary patch) {
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
  public List<SharedFieldGroupSummary> listGroups(URI baseUri) {
    requireAdmin();
    // baseUri reserved for HATEOAS
    PSContentEditorSharedDef def = loadSharedDef();
    return mapSummaries(def);
  }

  @Override
  public SharedFieldGroupDetail getGroup(URI baseUri, String name) {
    requireAdmin();
    if (!isSafeGroupName(name)) {
      return null;
    }
    PSContentEditorSharedDef def = loadSharedDef();
    PSSharedFieldGroup group = findGroup(def, name.trim());
    if (group == null) {
      return null;
    }
    return toDetail(group);
  }

  @Override
  public SharedFieldGroupDetail createGroup(URI baseUri, SharedFieldGroupDetail body) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (body == null || StringUtils.isBlank(body.getName())) {
      throw new IllegalArgumentException("name is required");
    }
    String name = validateGroupName(body.getName().trim());
    String filename = normalizeFilename(body.getFilename(), name);
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSharedDef def = loadSharedDefLocked(session, user);
    if (findGroup(def, name) != null) {
      throw new WebApplicationException("Shared field group already exists: " + name, 409);
    }
    PSSharedFieldGroup created = newEmptyGroup(name, filename);
    def.addFieldGroup(created);
    saveSharedDef(def, session, user);
    return toDetail(created);
  }

  @Override
  public SharedFieldGroupDetail updateGroup(URI baseUri, String name, SharedFieldGroupDetail body) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name is required");
    }
    if (!isSafeGroupName(name)) {
      return null;
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSharedDef def = loadSharedDefLocked(session, user);
    PSSharedFieldGroup group = findGroup(def, name.trim());
    if (group == null) {
      return null;
    }
    if (StringUtils.isNotBlank(body.getFilename())) {
      group.setFilename(normalizeFilename(body.getFilename(), group.getName()));
    }
    if (StringUtils.isNotBlank(body.getName())) {
      String newName = validateGroupName(body.getName().trim());
      if (!newName.equalsIgnoreCase(group.getName())) {
        if (findGroup(def, newName) != null) {
          throw new WebApplicationException("Shared field group already exists: " + newName, 409);
        }
        group.setName(newName);
      }
    }
    applyFieldPatches(group, body.getFields());
    saveSharedDef(def, session, user);
    return toDetail(group);
  }

  @Override
  public void deleteGroup(URI baseUri, String name) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name is required");
    }
    if (!isSafeGroupName(name)) {
      throw new SharedFieldNotFoundException("Shared field group not found");
    }
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSharedDef def = loadSharedDefLocked(session, user);
    PSSharedFieldGroup group = findGroup(def, name.trim());
    if (group == null) {
      throw new SharedFieldNotFoundException("Shared field group not found");
    }
    def.removeFieldGroup(group);
    saveSharedDef(def, session, user);
  }

  @Override
  public SharedFieldGroupDetail addField(URI baseUri, String groupName, SharedFieldSummary body) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (StringUtils.isBlank(groupName)) {
      throw new IllegalArgumentException("name is required");
    }
    if (!isSafeGroupName(groupName)) {
      return null;
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String fieldName = validateFieldName(body.getName());
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSharedDef def = loadSharedDefLocked(session, user);
    PSSharedFieldGroup group = findGroup(def, groupName.trim());
    if (group == null) {
      return null;
    }
    if (findSharedField(def, fieldName) != null) {
      throw new WebApplicationException("Shared field already exists: " + fieldName, 409);
    }
    addPersistableField(group, body);
    saveSharedDef(def, session, user);
    return toDetail(group);
  }

  @Override
  public void deleteField(URI baseUri, String groupName, String fieldName) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (StringUtils.isBlank(groupName) || StringUtils.isBlank(fieldName)) {
      throw new IllegalArgumentException("name is required");
    }
    if (!isSafeGroupName(groupName)) {
      throw new SharedFieldNotFoundException("Shared field group not found");
    }
    if (!isSafeGroupName(fieldName)) {
      throw new SharedFieldNotFoundException("Shared field not found");
    }
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSharedDef def = loadSharedDefLocked(session, user);
    PSSharedFieldGroup group = findGroup(def, groupName.trim());
    if (group == null) {
      throw new SharedFieldNotFoundException("Shared field group not found");
    }
    if (!removeFieldAndMapping(group, fieldName.trim())) {
      throw new SharedFieldNotFoundException("Shared field not found");
    }
    saveSharedDef(def, session, user);
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

  private PSContentEditorSharedDef loadSharedDef() {
    try {
      PSContentEditorSharedDef def = sharedDefLoader.get();
      return def != null ? def : new PSContentEditorSharedDef();
    } catch (RuntimeException e) {
      log.warn("Failed to load content editor shared def", e);
      throw e;
    }
  }

  /**
   * Group names are path-ish identifiers. Reject path traversal and separators so a user-supplied
   * name cannot escape expected object-store layout ({@code java/path-injection}).
   */
  static boolean isSafeGroupName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return !name.contains("..")
        && name.indexOf('/') < 0
        && name.indexOf('\\') < 0
        && name.indexOf('\0') < 0;
  }

  /** Package-visible for unit tests. */
  static List<SharedFieldGroupSummary> mapSummaries(PSContentEditorSharedDef def) {
    List<SharedFieldGroupSummary> out = new ArrayList<>();
    if (def == null) {
      return out;
    }
    for (Iterator<?> it = def.getFieldGroups(); it.hasNext(); ) {
      Object o = it.next();
      if (!(o instanceof PSSharedFieldGroup group)) {
        continue;
      }
      try {
        out.add(toSummary(group));
      } catch (Exception e) {
        log.debug("Skipping shared field group {}: {}", group.getName(), e.getMessage());
      }
    }
    out.sort(
        Comparator.comparing(
            SharedFieldGroupSummary::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  static PSSharedFieldGroup findGroup(PSContentEditorSharedDef def, String name) {
    if (def == null || StringUtils.isBlank(name)) {
      return null;
    }
    for (Iterator<?> it = def.getFieldGroups(); it.hasNext(); ) {
      Object o = it.next();
      if (o instanceof PSSharedFieldGroup group && name.equalsIgnoreCase(group.getName())) {
        return group;
      }
    }
    return null;
  }

  static PSField findSharedField(PSContentEditorSharedDef def, String fieldName) {
    if (def == null || StringUtils.isBlank(fieldName)) {
      return null;
    }
    for (Iterator<?> it = def.getFieldGroups(); it.hasNext(); ) {
      Object o = it.next();
      if (!(o instanceof PSSharedFieldGroup group) || group.getFieldSet() == null) {
        continue;
      }
      PSField found = group.getFieldSet().findFieldByName(fieldName, false);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  static SharedFieldGroupSummary toSummary(PSSharedFieldGroup group) {
    SharedFieldGroupSummary s = new SharedFieldGroupSummary();
    s.setName(group.getName());
    s.setFilename(group.getFilename());
    s.setFieldCount(countFields(group.getFieldSet()));
    return s;
  }

  static SharedFieldGroupDetail toDetail(PSSharedFieldGroup group) {
    SharedFieldGroupDetail d = new SharedFieldGroupDetail();
    d.setName(group.getName());
    d.setFilename(group.getFilename());
    d.setFields(mapFields(group.getFieldSet()));
    d.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return d;
  }

  static int countFields(PSFieldSet fieldSet) {
    if (fieldSet == null) {
      return 0;
    }
    PSField[] all = fieldSet.getAllFields();
    if (all == null) {
      return 0;
    }
    int n = 0;
    for (PSField field : all) {
      if (field != null && StringUtils.isNotBlank(field.getSubmitName())) {
        n++;
      }
    }
    return n;
  }

  static List<SharedFieldSummary> mapFields(PSFieldSet fieldSet) {
    List<SharedFieldSummary> out = new ArrayList<>();
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
      SharedFieldSummary f = new SharedFieldSummary();
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
            SharedFieldSummary::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
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
