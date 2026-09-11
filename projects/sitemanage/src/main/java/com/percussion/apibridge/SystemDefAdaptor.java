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

import com.percussion.design.objectstore.PSApplicationFlow;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSCommandHandlerStylesheets;
import com.percussion.design.objectstore.PSContainerLocator;
import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSExtensionParamValue;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSParam;
import com.percussion.design.objectstore.PSSearchProperties;
import com.percussion.design.objectstore.PSStylesheet;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSTableRef;
import com.percussion.design.objectstore.PSTableSet;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.design.objectstore.PSUrlRequest;
import com.percussion.util.PSCollection;
import com.percussion.rest.DesignGap;
import com.percussion.rest.contenttypes.ContentTypeControlProperty;
import com.percussion.rest.systemdef.ISystemDefAdaptor;
import com.percussion.rest.systemdef.SystemDefApplicationFlow;
import com.percussion.rest.systemdef.SystemDefCommandHandlerRedirect;
import com.percussion.rest.systemdef.SystemDefCommandHandlerStylesheet;
import com.percussion.rest.systemdef.SystemDefConditionalRedirect;
import com.percussion.rest.systemdef.SystemDefConditionalStylesheet;
import com.percussion.rest.systemdef.SystemDefControlProperties;
import com.percussion.rest.systemdef.SystemDefDesignLockException;
import com.percussion.rest.systemdef.SystemDefDetail;
import com.percussion.rest.systemdef.SystemDefFieldNotFoundException;
import com.percussion.rest.systemdef.SystemDefFieldSummary;
import com.percussion.rest.systemdef.SystemDefStylesheets;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
      List.of("Shared field groups are a separate catalog (Developer Shared Fields)");

  /**
   * Structured gaps on GET/PUT {@code .../controlProperties}, {@code .../stylesheets}, and {@code
   * .../applicationFlow}. {@code SYS_STYLESHEET} and {@code SYS_APP_FLOW} are dropped now that those
   * writes ship.
   */
  static final List<DesignGap> CONTROL_PROPERTY_DESIGN_GAPS = List.of();

  static final List<DesignGap> STYLESHEET_DESIGN_GAPS = CONTROL_PROPERTY_DESIGN_GAPS;

  static final List<DesignGap> APPLICATION_FLOW_DESIGN_GAPS = CONTROL_PROPERTY_DESIGN_GAPS;

  private static final Pattern COMMAND_HANDLER_NAME =
      Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,49}$");

  /**
   * Workbench system-def stylesheet hrefs are {@code file:} URLs relative to the content-editor
   * app ({@code file:../sys_resources/stylesheets/activeEdit.xsl}). Extra {@code ..}, absolute
   * paths, and non-file schemes are rejected.
   */
  private static final Pattern SAFE_STYLESHEET_HREF =
      Pattern.compile(
          "^file:\\.\\./(sys_resources|rx_resources)/stylesheets/[A-Za-z0-9][A-Za-z0-9._-]{0,120}\\.xsl$");

  private static final int MAX_STYLESHEET_HREF_LENGTH = 200;

  /**
   * Workbench application-flow default paths are relative CMS app resources ({@code
   * ../sys_cx/mainpage.html}). Extra {@code ..}, absolute paths, and schemes are rejected. Empty
   * href is allowed (Workbench empty MakeAbsLink first param).
   */
  private static final Pattern SAFE_APP_FLOW_HREF =
      Pattern.compile(
          "^\\.\\./(sys_|rx_)[A-Za-z0-9]+(/[A-Za-z0-9][A-Za-z0-9._-]*)+\\.(html|xml|jsp)$");

  private static final int MAX_APP_FLOW_HREF_LENGTH = 200;

  private static final int MAX_FIELD_NAME_LENGTH = 50;

  private static final String DEFAULT_TEXT_CONTROL = "sys_EditBox";

  private static final String DEFAULT_TABLE_ALIAS = "CONTENTSTATUS";

  private static final Pattern QUOTED_COLUMN_NOT_FOUND =
      Pattern.compile("column\\s+[\"'`][^\"'`]+[\"'`]\\s+not found");

  private final IPSContentDesignWs designWs;
  private final Supplier<PSContentEditorSystemDef> systemDefLoader;
  private final BooleanSupplier adminChecker;
  private final SystemDefColumnSchema columnSchema;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public SystemDefAdaptor() {
    this(
        PSContentWsLocator.getContentDesignWebservice(),
        null,
        new JdbcSystemDefColumnSchema());
  }

  /**
   * Package-visible for unit tests that inject a fake design web service. {@code null} adminChecker
   * uses {@link #isCurrentUserAdmin()}. Column DDL is a no-op unless the three-arg constructor is
   * used.
   */
  SystemDefAdaptor(IPSContentDesignWs designWs, BooleanSupplier adminChecker) {
    this(designWs, adminChecker, SystemDefColumnSchema.noop());
  }

  /**
   * Package-visible for unit tests that inject a fake design web service and column schema.
   * {@code null} adminChecker uses {@link #isCurrentUserAdmin()}. {@code null} columnSchema is a
   * no-op.
   */
  SystemDefAdaptor(
      IPSContentDesignWs designWs,
      BooleanSupplier adminChecker,
      SystemDefColumnSchema columnSchema) {
    this.designWs = designWs;
    this.systemDefLoader =
        () -> loadSystemDefFromDesignWs(designWs, currentSession(), currentUser());
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.columnSchema = columnSchema != null ? columnSchema : SystemDefColumnSchema.noop();
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
    this.columnSchema = SystemDefColumnSchema.noop();
  }

  /**
   * Production load path used by the default constructor. Package-visible so unit tests can
   * exercise design-WS success, {@link PSErrorException} wrapping, and absent request session/user
   * without mocking static locators.
   */
  static PSContentEditorSystemDef loadSystemDefFromDesignWs(
      IPSContentDesignWs designWs, String sessionId, String user) {
    try {
      return loadSystemDefFromDesignWsOnce(designWs, false, sessionId, user);
    } catch (RuntimeException e) {
      if (!isMissingColumnFailure(e)) {
        throw e;
      }
      log.warn(
          "System def load hit missing backend column, retrying catalog: {}", e.getMessage());
      return loadSystemDefFromDesignWsOnce(designWs, false, sessionId, user);
    }
  }

  private static PSContentEditorSystemDef loadSystemDefFromDesignWsOnce(
      IPSContentDesignWs designWs, boolean lock, String sessionId, String user) {
    try {
      // Request-lock writes override a stale lock owned by the same user in another
      // session (REST basic vs SPA cookie) so a leaked lock cannot 409 later Admin PUTs.
      PSContentEditorSystemDef def =
          designWs.loadContentEditorSystemDef(lock, lock, sessionId, user);
      if (lock && def == null) {
        throw new IllegalStateException("Failed to load system def for write");
      }
      return def;
    } catch (PSLockErrorException e) {
      throw mapLockConflict(e);
    } catch (PSErrorException e) {
      String msg = lock ? "Failed to load system def for write" : "Failed to load system def";
      log.error("Failed to load content editor system def via design WS", e);
      throw new IllegalStateException(msg, e);
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
      return loadSystemDefFromDesignWsOnce(designWs, true, session, user);
    } catch (RuntimeException e) {
      if (!isMissingColumnFailure(e)) {
        throw e;
      }
      log.warn(
          "System def locked load hit missing backend column, retrying: {}", e.getMessage());
      return loadSystemDefFromDesignWsOnce(designWs, true, session, user);
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
    } catch (NullPointerException e) {
      log.error("NullPointerException saving content editor system def", e);
      throw new IllegalStateException("Failed to save system def", e);
    }
  }

  /**
   * True when {@code t} (or a cause) is a missing backend column, typically H2 {@code no such
   * column} after an XML-only system-def add.
   *
   * <p>Prefers SQLState / vendor codes (H2 {@code 42122}, JDBC {@code 42S22}, PostgreSQL {@code
   * 42703}, Oracle {@code 904}, SQL Server {@code 207}) over loose message matching. Does not treat
   * unrelated "column … not found" phrases (mapping / schema text) as a missing column.
   */
  static boolean isMissingColumnFailure(Throwable t) {
    for (Throwable cur = t; cur != null; cur = cur.getCause()) {
      if (cur instanceof SQLException sqlEx && isMissingColumnSql(sqlEx)) {
        return true;
      }
      String msg = cur.getMessage();
      if (msg == null) {
        continue;
      }
      String lower = msg.toLowerCase(Locale.ROOT);
      if (lower.contains("no such column")
          || QUOTED_COLUMN_NOT_FOUND.matcher(lower).find()
          || lower.contains("invalid column name")
          || lower.contains("unknown column")
          || lower.contains("ora-00904")) {
        return true;
      }
    }
    return false;
  }

  private static boolean isMissingColumnSql(SQLException e) {
    for (SQLException cur = e; cur != null; cur = cur.getNextException()) {
      String state = cur.getSQLState();
      if (state != null) {
        String s = state.toUpperCase(Locale.ROOT);
        if ("42122".equals(s) || "42S22".equals(s) || "42703".equals(s)) {
          return true;
        }
      }
      int code = cur.getErrorCode();
      if (code == 904 || code == 207) {
        return true;
      }
    }
    return false;
  }

  /**
   * True when PUT {@code fields} contains at least one named patch. Null or empty (or only blank
   * names) leaves the catalog unchanged and must not rewrite system-def XML.
   */
  static boolean hasFieldPatches(List<SystemDefFieldSummary> patches) {
    if (patches == null || patches.isEmpty()) {
      return false;
    }
    for (SystemDefFieldSummary patch : patches) {
      if (patch != null && StringUtils.isNotBlank(patch.getName())) {
        return true;
      }
    }
    return false;
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
    if (JdbcSystemDefColumnSchema.isSqlReservedIdent(trimmed)) {
      throw new IllegalArgumentException("name is a reserved SQL identifier");
    }
    return trimmed;
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
   * mapping ({@code sys_EditBox}). Stylesheet/flow write is a later slice.
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
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (!hasFieldPatches(body.getFields())) {
      // Contract: null/empty fields leaves the catalog unchanged. Do not rewrite
      // ContentEditorSystemDef.xml from the in-memory (fixup'd) object — that
      // round-trip NPEs subsequent PUTs on H2.
      return toDetail(systemDefLoader.get());
    }
    requireDesignWs();
    requireSessionUserForWrite();
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
    PSField added = addPersistableField(def, body);
    try {
      columnSchema.ensureColumn(
          tableAliasForSystemDef(def),
          columnNameForField(fieldName),
          added.getDataType(),
          added.getDataFormat());
    } catch (RuntimeException e) {
      log.error("Failed to create backend column for system field {}", fieldName, e);
      throw new IllegalStateException(
          "Failed to create backend column for field " + fieldName, e);
    }
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
    try {
      columnSchema.dropColumnIfPresent(
          tableAliasForSystemDef(def), columnNameForField(validated));
    } catch (RuntimeException e) {
      if (!isMissingColumnFailure(e)) {
        log.error("Failed to drop backend column for system field {}", validated, e);
        throw new IllegalStateException(
            "Failed to drop backend column for field " + validated, e);
      }
      log.warn(
          "Skipping drop of missing system-def column for field {}: {}",
          validated,
          e.getMessage());
    }
    saveSystemDef(def, session, user);
  }

  @Override
  public SystemDefControlProperties getFieldControlProperties(URI baseUri, String fieldName) {
    requireAdmin();
    if (StringUtils.isBlank(fieldName) || !isSafeFieldName(fieldName)) {
      throw new WebApplicationException("System field not found", 404);
    }
    return loadFieldControlProperties(systemDefLoader.get(), fieldName.trim());
  }

  @Override
  public SystemDefControlProperties replaceFieldControlProperties(
      URI baseUri, String fieldName, SystemDefControlProperties body) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (StringUtils.isBlank(fieldName)) {
      throw new IllegalArgumentException("name is required");
    }
    if (body == null || body.getProperties() == null) {
      throw new IllegalArgumentException("properties is required");
    }
    if (!isSafeFieldName(fieldName)) {
      throw new SystemDefFieldNotFoundException("System field not found");
    }
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSystemDef def = loadSystemDefLocked(session, user);
    PSDisplayMapping mapping = requireFieldMapping(def, fieldName.trim(), true);
    applyControlPropertyUpdates(mapping, body);
    saveSystemDef(def, session, user);
    return toFieldControlProperties(fieldName.trim(), mapping);
  }

  @Override
  public SystemDefStylesheets getStylesheets(URI baseUri) {
    requireAdmin();
    return toStylesheets(systemDefLoader.get());
  }

  @Override
  public SystemDefStylesheets replaceStylesheets(URI baseUri, SystemDefStylesheets body) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (body == null || body.getHandlers() == null) {
      throw new IllegalArgumentException("handlers is required");
    }
    List<SystemDefCommandHandlerStylesheet> keep =
        validateStylesheetHandlers(body.getHandlers());
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSystemDef def = loadSystemDefLocked(session, user);
    if (def == null || def.getStyleSheetSet() == null) {
      throw new IllegalStateException("Failed to load system def for write");
    }
    applyValidatedStylesheetUpdates(def.getStyleSheetSet(), keep);
    saveSystemDef(def, session, user);
    return toStylesheets(def);
  }

  @Override
  public SystemDefApplicationFlow getApplicationFlow(URI baseUri) {
    requireAdmin();
    return toApplicationFlow(systemDefLoader.get());
  }

  @Override
  public SystemDefApplicationFlow replaceApplicationFlow(
      URI baseUri, SystemDefApplicationFlow body) {
    requireAdmin();
    requireDesignWs();
    requireSessionUserForWrite();
    if (body == null || body.getHandlers() == null) {
      throw new IllegalArgumentException("handlers is required");
    }
    List<SystemDefCommandHandlerRedirect> keep =
        validateApplicationFlowHandlers(body.getHandlers());
    String session = currentSession();
    String user = currentUser();
    PSContentEditorSystemDef def = loadSystemDefLocked(session, user);
    if (def == null || def.getApplicationFlow() == null) {
      throw new IllegalStateException("Failed to load system def for write");
    }
    applyValidatedApplicationFlowUpdates(def.getApplicationFlow(), keep);
    saveSystemDef(def, session, user);
    return toApplicationFlow(def);
  }

  private SystemDefControlProperties loadFieldControlProperties(
      PSContentEditorSystemDef def, String fieldName) {
    PSDisplayMapping mapping = requireFieldMapping(def, fieldName, false);
    return toFieldControlProperties(fieldName, mapping);
  }

  /**
   * Resolve the field by submit name then its display mapping. Missing field or mapping is 404.
   *
   * @param notFoundIsDeleteStyle {@code true} throws {@link SystemDefFieldNotFoundException} (PUT);
   *     {@code false} throws HTTP 404 (GET)
   */
  private PSDisplayMapping requireFieldMapping(
      PSContentEditorSystemDef def, String fieldName, boolean notFoundIsDeleteStyle) {
    PSField field =
        def != null && def.getFieldSet() != null
            ? def.getFieldSet().findFieldByName(fieldName, false)
            : null;
    PSDisplayMapper mapper =
        def != null && def.getUIDefinition() != null
            ? def.getUIDefinition().getDisplayMapper()
            : null;
    String actual = field != null ? field.getSubmitName() : fieldName;
    PSDisplayMapping mapping = ContentTypeAdaptor.findDisplayMapping(mapper, actual);
    if (field == null || mapping == null) {
      if (notFoundIsDeleteStyle) {
        throw new SystemDefFieldNotFoundException("System field not found");
      }
      throw new WebApplicationException("System field not found", 404);
    }
    return mapping;
  }

  static SystemDefControlProperties toFieldControlProperties(
      String fieldName, PSDisplayMapping mapping) {
    SystemDefControlProperties out = new SystemDefControlProperties();
    out.setFieldName(fieldName);
    PSUISet ui = mapping != null ? mapping.getUISet() : null;
    PSControlRef control = ui != null ? ui.getControl() : null;
    if (control != null && StringUtils.isNotBlank(control.getName())) {
      out.setControl(control.getName());
    }
    out.setProperties(new ArrayList<>(ContentTypeAdaptor.controlProperties(control)));
    if (ui != null) {
      out.setChoices(ContentTypeAdaptor.toChoiceCatalog(ui.getChoices()));
    }
    out.setDesignGaps(new ArrayList<>(CONTROL_PROPERTY_DESIGN_GAPS));
    return out;
  }

  static void applyControlPropertyUpdates(
      PSDisplayMapping mapping, SystemDefControlProperties body) {
    PSUISet ui = mapping.getUISet();
    if (ui == null) {
      ui = new PSUISet();
      mapping.setUISet(ui);
    }
    List<ContentTypeControlProperty> properties = body.getProperties();
    PSControlRef control = ui.getControl();
    if (control == null) {
      if (!properties.isEmpty()) {
        throw new IllegalArgumentException("field has no display control");
      }
    } else {
      control.setParameters(ContentTypeAdaptor.toParamCollection(properties));
    }
    if (body.getChoices() != null) {
      ui.setChoices(ContentTypeAdaptor.fromChoiceCatalog(body.getChoices()));
    }
  }

  static SystemDefStylesheets toStylesheets(PSContentEditorSystemDef def) {
    SystemDefStylesheets out = new SystemDefStylesheets();
    List<SystemDefCommandHandlerStylesheet> handlers = new ArrayList<>();
    PSCommandHandlerStylesheets set = def != null ? def.getStyleSheetSet() : null;
    if (set != null) {
      List<String> names = commandHandlerNames(set);
      names.sort(String.CASE_INSENSITIVE_ORDER);
      for (String name : names) {
        handlers.add(toHandlerRow(set, name));
      }
    }
    out.setHandlers(handlers);
    out.setDesignGaps(new ArrayList<>(STYLESHEET_DESIGN_GAPS));
    return out;
  }

  static SystemDefCommandHandlerStylesheet toHandlerRow(
      PSCommandHandlerStylesheets set, String name) {
    SystemDefCommandHandlerStylesheet row = new SystemDefCommandHandlerStylesheet();
    row.setCommandHandler(name);
    PSStylesheet defaultSheet = set.getDefaultStylesheet(name);
    if (defaultSheet != null && defaultSheet.getRequest() != null) {
      row.setHref(defaultSheet.getRequest().getHref());
    }
    List<SystemDefConditionalStylesheet> conditionals = new ArrayList<>();
    List<PSStylesheet> all = stylesheetList(set, name);
    for (int i = 0; i < all.size() - 1; i++) {
      PSStylesheet sheet = all.get(i);
      SystemDefConditionalStylesheet cond = new SystemDefConditionalStylesheet();
      if (sheet != null && sheet.getRequest() != null) {
        cond.setHref(sheet.getRequest().getHref());
      }
      conditionals.add(cond);
    }
    if (!conditionals.isEmpty()) {
      row.setConditionals(conditionals);
    }
    return row;
  }

  /**
   * Full replace of command-handler default hrefs. Omitted handlers (and blank hrefs) are
   * removed. Conditionals on remaining handlers are preserved via {@link
   * PSCommandHandlerStylesheets#setDefaultStylesheet}. At least one remaining handler is
   * required.
   */
  static void applyStylesheetUpdates(
      PSCommandHandlerStylesheets set, List<SystemDefCommandHandlerStylesheet> handlers) {
    applyValidatedStylesheetUpdates(set, validateStylesheetHandlers(handlers));
  }

  /**
   * Validate PUT handlers without mutating the object store so a 400 does not hold the
   * system-def design lock.
   */
  static List<SystemDefCommandHandlerStylesheet> validateStylesheetHandlers(
      List<SystemDefCommandHandlerStylesheet> handlers) {
    if (handlers == null) {
      throw new IllegalArgumentException("handlers is required");
    }
    List<SystemDefCommandHandlerStylesheet> keep = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (SystemDefCommandHandlerStylesheet row : handlers) {
      if (row == null) {
        continue;
      }
      String name = validateCommandHandlerName(row.getCommandHandler());
      String folded = name.toLowerCase(Locale.ROOT);
      if (seen.contains(folded)) {
        throw new IllegalArgumentException("Duplicate command handler: " + name);
      }
      seen.add(folded);
      if (StringUtils.isBlank(row.getHref())) {
        continue;
      }
      SystemDefCommandHandlerStylesheet copy = new SystemDefCommandHandlerStylesheet();
      copy.setCommandHandler(name);
      copy.setHref(requireSafeStylesheetHref(row.getHref()));
      keep.add(copy);
    }
    if (keep.isEmpty()) {
      throw new IllegalArgumentException("At least one command handler stylesheet is required");
    }
    return keep;
  }

  static void applyValidatedStylesheetUpdates(
      PSCommandHandlerStylesheets set, List<SystemDefCommandHandlerStylesheet> keep) {
    if (set == null) {
      throw new IllegalArgumentException("System def has no stylesheet set");
    }
    if (keep == null || keep.isEmpty()) {
      throw new IllegalArgumentException("At least one command handler stylesheet is required");
    }
    Set<String> keepFolded = new LinkedHashSet<>();
    for (SystemDefCommandHandlerStylesheet row : keep) {
      keepFolded.add(row.getCommandHandler().toLowerCase(Locale.ROOT));
    }
    for (String existing : commandHandlerNames(set)) {
      if (!keepFolded.contains(existing.toLowerCase(Locale.ROOT))) {
        set.removeStylesheets(existing);
      }
    }
    for (SystemDefCommandHandlerStylesheet row : keep) {
      String storedName = findExistingHandlerName(set, row.getCommandHandler());
      String name = storedName != null ? storedName : row.getCommandHandler();
      PSUrlRequest request =
          new PSUrlRequest(null, row.getHref(), new PSCollection<PSParam>(PSParam.class));
      set.setDefaultStylesheet(name, new PSStylesheet(request));
    }
  }

  static String validateCommandHandlerName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("commandHandler is required");
    }
    String trimmed = name.trim();
    if (!isSafeFieldName(trimmed) || !COMMAND_HANDLER_NAME.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("Invalid command handler name: " + trimmed);
    }
    return trimmed;
  }

  /**
   * Workbench default hrefs are {@code file:../sys_resources/stylesheets/*.xsl} (or {@code
   * rx_resources}). Rejects extra traversal, backslashes, absolute paths, and non-file schemes.
   */
  static String requireSafeStylesheetHref(String href) {
    if (StringUtils.isBlank(href)) {
      throw new IllegalArgumentException("stylesheet href is required");
    }
    String trimmed = href.trim();
    if (trimmed.length() > MAX_STYLESHEET_HREF_LENGTH) {
      throw new IllegalArgumentException("stylesheet href exceeds length limit");
    }
    if (trimmed.indexOf('\\') >= 0 || trimmed.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("Invalid stylesheet href");
    }
    if (!SAFE_STYLESHEET_HREF.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("Invalid stylesheet href");
    }
    return trimmed;
  }

  static List<String> commandHandlerNames(PSCommandHandlerStylesheets set) {
    List<String> names = new ArrayList<>();
    if (set == null) {
      return names;
    }
    Iterator<?> it = set.getCommandHandlerNames();
    while (it != null && it.hasNext()) {
      Object next = it.next();
      if (next != null) {
        names.add(String.valueOf(next));
      }
    }
    return names;
  }

  static String findExistingHandlerName(PSCommandHandlerStylesheets set, String name) {
    if (set == null || name == null) {
      return null;
    }
    for (String existing : commandHandlerNames(set)) {
      if (existing.equalsIgnoreCase(name)) {
        return existing;
      }
    }
    return null;
  }

  static SystemDefApplicationFlow toApplicationFlow(PSContentEditorSystemDef def) {
    SystemDefApplicationFlow out = new SystemDefApplicationFlow();
    List<SystemDefCommandHandlerRedirect> handlers = new ArrayList<>();
    PSApplicationFlow flow = def != null ? def.getApplicationFlow() : null;
    if (flow != null) {
      List<String> names = applicationFlowHandlerNames(flow);
      names.sort(String.CASE_INSENSITIVE_ORDER);
      for (String name : names) {
        handlers.add(toApplicationFlowRow(flow, name));
      }
    }
    out.setHandlers(handlers);
    out.setDesignGaps(new ArrayList<>(APPLICATION_FLOW_DESIGN_GAPS));
    return out;
  }

  static SystemDefCommandHandlerRedirect toApplicationFlowRow(PSApplicationFlow flow, String name) {
    SystemDefCommandHandlerRedirect row = new SystemDefCommandHandlerRedirect();
    row.setCommandHandler(name);
    PSUrlRequest defaultRedirect = flow.getDefaultRedirect(name);
    row.setHref(applicationFlowHref(defaultRedirect));
    List<SystemDefConditionalRedirect> conditionals = new ArrayList<>();
    List<PSUrlRequest> all = applicationFlowRedirectList(flow, name);
    for (int i = 0; i < all.size() - 1; i++) {
      SystemDefConditionalRedirect cond = new SystemDefConditionalRedirect();
      cond.setHref(applicationFlowHref(all.get(i)));
      conditionals.add(cond);
    }
    if (!conditionals.isEmpty()) {
      row.setConditionals(conditionals);
    }
    return row;
  }

  /**
   * Validate PUT application-flow handlers without mutating the object store so a 400 does not hold
   * the system-def design lock.
   */
  static List<SystemDefCommandHandlerRedirect> validateApplicationFlowHandlers(
      List<SystemDefCommandHandlerRedirect> handlers) {
    if (handlers == null) {
      throw new IllegalArgumentException("handlers is required");
    }
    List<SystemDefCommandHandlerRedirect> keep = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (SystemDefCommandHandlerRedirect row : handlers) {
      if (row == null) {
        continue;
      }
      String name = validateCommandHandlerName(row.getCommandHandler());
      String folded = name.toLowerCase(Locale.ROOT);
      if (seen.contains(folded)) {
        throw new IllegalArgumentException("Duplicate command handler: " + name);
      }
      seen.add(folded);
      SystemDefCommandHandlerRedirect copy = new SystemDefCommandHandlerRedirect();
      copy.setCommandHandler(name);
      copy.setHref(requireSafeApplicationFlowHref(row.getHref()));
      keep.add(copy);
    }
    if (keep.isEmpty()) {
      throw new IllegalArgumentException("At least one command handler redirect is required");
    }
    return keep;
  }

  static void applyValidatedApplicationFlowUpdates(
      PSApplicationFlow flow, List<SystemDefCommandHandlerRedirect> keep) {
    if (flow == null) {
      throw new IllegalArgumentException("System def has no application flow");
    }
    if (keep == null || keep.isEmpty()) {
      throw new IllegalArgumentException("At least one command handler redirect is required");
    }
    Set<String> keepFolded = new LinkedHashSet<>();
    for (SystemDefCommandHandlerRedirect row : keep) {
      keepFolded.add(row.getCommandHandler().toLowerCase(Locale.ROOT));
    }
    for (String existing : applicationFlowHandlerNames(flow)) {
      if (!keepFolded.contains(existing.toLowerCase(Locale.ROOT))) {
        flow.removeStylesheets(existing);
      }
    }
    for (SystemDefCommandHandlerRedirect row : keep) {
      String storedName = findExistingApplicationFlowHandlerName(flow, row.getCommandHandler());
      String name = storedName != null ? storedName : row.getCommandHandler();
      PSUrlRequest existing = storedName != null ? flow.getDefaultRedirect(storedName) : null;
      flow.setDefaultRedirect(name, withApplicationFlowHref(existing, row.getHref()));
    }
  }

  /**
   * Href from a default or conditional redirect: {@code PSXUrlRequest/Href} when present, otherwise
   * the first {@code sys_MakeAbsLink} text parameter (Workbench default).
   */
  static String applicationFlowHref(PSUrlRequest request) {
    if (request == null) {
      return "";
    }
    if (StringUtils.isNotBlank(request.getHref())) {
      return request.getHref();
    }
    PSExtensionCall converter = request.getConverter();
    if (converter == null) {
      return request.getHref() != null ? request.getHref() : "";
    }
    PSExtensionParamValue[] params = converter.getParamValues();
    if (params == null || params.length == 0 || params[0] == null) {
      return "";
    }
    if (params[0].getValue() instanceof PSTextLiteral text) {
      return text.getText() != null ? text.getText() : "";
    }
    return "";
  }

  static PSUrlRequest withApplicationFlowHref(PSUrlRequest existing, String href) {
    String value = href != null ? href : "";
    if (existing != null && existing.getConverter() != null) {
      PSUrlRequest copy = (PSUrlRequest) existing.clone();
      PSExtensionCall converter = copy.getConverter();
      PSExtensionParamValue[] params = converter != null ? converter.getParamValues() : null;
      if (params != null
          && params.length > 0
          && params[0] != null
          && params[0].getValue() instanceof PSTextLiteral text) {
        text.setText(value);
        return copy;
      }
      throw new IllegalArgumentException("application flow default redirect is not a text path");
    }
    return new PSUrlRequest(null, value, new PSCollection(PSParam.class));
  }

  /**
   * Relative CMS application resource, or empty. Rejects extra traversal, backslashes, absolute
   * paths, and schemes.
   */
  static String requireSafeApplicationFlowHref(String href) {
    String trimmed = href != null ? href.trim() : "";
    if (trimmed.isEmpty()) {
      return "";
    }
    if (trimmed.length() > MAX_APP_FLOW_HREF_LENGTH) {
      throw new IllegalArgumentException("application flow href exceeds length limit");
    }
    if (trimmed.indexOf('\\') >= 0 || trimmed.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("Invalid application flow href");
    }
    if (!SAFE_APP_FLOW_HREF.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("Invalid application flow href");
    }
    return trimmed;
  }

  static List<String> applicationFlowHandlerNames(PSApplicationFlow flow) {
    List<String> names = new ArrayList<>();
    if (flow == null) {
      return names;
    }
    Iterator<?> it = flow.getCommandHandlerNames();
    while (it != null && it.hasNext()) {
      Object next = it.next();
      if (next != null) {
        names.add(String.valueOf(next));
      }
    }
    return names;
  }

  static String findExistingApplicationFlowHandlerName(PSApplicationFlow flow, String name) {
    if (flow == null || name == null) {
      return null;
    }
    for (String existing : applicationFlowHandlerNames(flow)) {
      if (existing.equalsIgnoreCase(name)) {
        return existing;
      }
    }
    return null;
  }

  static List<PSUrlRequest> applicationFlowRedirectList(PSApplicationFlow flow, String name) {
    List<PSUrlRequest> all = new ArrayList<>();
    if (flow == null || name == null) {
      return all;
    }
    Iterator<?> it = flow.getRedirects(name);
    while (it != null && it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSUrlRequest request) {
        all.add(request);
      }
    }
    return all;
  }

  static List<PSStylesheet> stylesheetList(PSCommandHandlerStylesheets set, String name) {
    List<PSStylesheet> all = new ArrayList<>();
    if (set == null || name == null) {
      return all;
    }
    Iterator<?> it = set.getStylesheets(name);
    while (it != null && it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSStylesheet sheet) {
        all.add(sheet);
      }
    }
    return all;
  }

  /**
   * Field names are path-ish identifiers. Reject path traversal and separators so a user-supplied
   * name cannot escape expected object-store layout ({@code java/path-injection}).
   */
  static boolean isSafeFieldName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return !name.contains("..")
        && name.indexOf('/') < 0
        && name.indexOf('\\') < 0
        && name.indexOf('\0') < 0;
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
