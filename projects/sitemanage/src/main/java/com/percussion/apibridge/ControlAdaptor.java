/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import com.percussion.design.objectstore.PSControlMeta;
import com.percussion.design.objectstore.PSControlParameter;
import com.percussion.rest.cecontrols.ControlDef;
import com.percussion.rest.cecontrols.ControlParameter;
import com.percussion.rest.cecontrols.IControlAdaptor;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.xml.PSXmlDocumentBuilder;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * CE control catalog adaptor (UI-01 read/write) over system + custom control managers. Admin
 * create/save/delete persist user controls as XSL files under {@code
 * rx_resources/stylesheets/controls} plus {@code PSCustomControlManager.writeImports}. System
 * controls are never mutated.
 */
@PSSiteManageBean
@Lazy
public class ControlAdaptor implements IControlAdaptor {

  private static final Logger log = LogManager.getLogger(ControlAdaptor.class);

  static final String ADMIN_REQUIRED =
      "Admin role required to create, update, or delete user CE controls";

  static final String SYSTEM_CONTROL_READONLY = "System controls cannot be updated or deleted";

  static final int MAX_CONTROL_NAME_LENGTH = 100;

  /** Catalog-level capability notes (same for every control). Exposed on detail only. */
  static final List<String> DESIGN_GAPS =
      List.of(
          "Full XSL source editor UX is not provided by this API; optional xslSource may be supplied on write",
          "System controls are read-only packaged defaults");

  private static final List<String> DIMENSIONS = List.of("single", "array", "table");
  private static final List<String> CHOICE_SETS = List.of("none", "required", "optional");

  private final BooleanSupplier adminChecker;
  private final UserControlIo io;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  @Autowired
  public ControlAdaptor() {
    this(null, null);
  }

  /** Package-visible for unit tests. */
  ControlAdaptor(BooleanSupplier adminChecker, UserControlIo io) {
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.io = io != null ? io : new ManagerUserControlIo();
  }

  @Override
  public List<ControlDef> listControls() {
    // REST-GAPS-02: omit identical designGaps on every list row; detail re-attaches them.
    return loadCatalog(false);
  }

  @Override
  public ControlDef findControlByName(String name) {
    if (!isSafeControlKey(name)) {
      return null;
    }
    String key = name.trim();
    for (ControlDef c : loadCatalog(true)) {
      if (c != null && key.equalsIgnoreCase(c.getName())) {
        return c;
      }
    }
    return null;
  }

  @Override
  public ControlDef createControl(ControlDef body) {
    requireAdmin();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String name = requireValidName(body.getName());
    assertNameUnique(name);
    String dimension = resolveDimension(body.getDimension());
    String choiceSet = resolveChoiceSet(body.getChoiceSet());
    String xsl = resolveXslSource(name, body, null, dimension, choiceSet);
    writeUserControlFile(name, xsl);
    ControlDef created = findControlByName(name);
    if (created == null) {
      created = projectionFromBody(body, name, dimension, choiceSet, true);
    }
    return created;
  }

  @Override
  public ControlDef saveControl(String name, ControlDef body) {
    requireAdmin();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (!isSafeControlKey(name)) {
      return null;
    }
    String key = name.trim();
    Path existingFile = findContainedUserFile(key);
    if (existingFile == null) {
      rejectSystemMutation(key);
      return null;
    }
    ControlDef existing = findControlByName(key);
    String dimension =
        body.getDimension() != null && !body.getDimension().isBlank()
            ? resolveDimension(body.getDimension())
            : resolveDimension(existing != null ? existing.getDimension() : null);
    String choiceSet =
        body.getChoiceSet() != null && !body.getChoiceSet().isBlank()
            ? resolveChoiceSet(body.getChoiceSet())
            : resolveChoiceSet(existing != null ? existing.getChoiceSet() : null);
    String xsl = resolveXslSource(key, body, existing, dimension, choiceSet);
    writeUserControlFile(key, xsl);
    ControlDef updated = findControlByName(key);
    if (updated == null) {
      updated = projectionFromBody(body, key, dimension, choiceSet, true);
    }
    return updated;
  }

  @Override
  public boolean deleteControl(String name) {
    requireAdmin();
    if (!isSafeControlKey(name)) {
      return false;
    }
    String key = name.trim();
    Path existingFile = findContainedUserFile(key);
    if (existingFile == null) {
      rejectSystemMutation(key);
      return false;
    }
    try {
      Files.deleteIfExists(existingFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete user control file", e);
    }
    io.writeImports();
    return true;
  }

  private List<ControlDef> loadCatalog(boolean includeDesignGaps) {
    Map<String, ControlDef> byName = new LinkedHashMap<>();
    for (ControlDef c : loadScope("system", includeDesignGaps)) {
      if (c.getName() != null) {
        byName.putIfAbsent(c.getName().toLowerCase(Locale.ROOT), c);
      }
    }
    for (ControlDef c : loadScope("user", includeDesignGaps)) {
      if (c.getName() != null) {
        // User overrides system when same name appears in both catalogs.
        byName.put(c.getName().toLowerCase(Locale.ROOT), c);
      }
    }
    return new ArrayList<>(byName.values());
  }

  private List<ControlDef> loadScope(String scope, boolean includeDesignGaps) {
    List<ControlDef> out = new ArrayList<>();
    try {
      List<PSControlMeta> metas =
          "system".equals(scope) ? io.loadSystemControls() : io.loadUserControls();
      if (metas == null) {
        return out;
      }
      for (PSControlMeta meta : metas) {
        if (meta != null) {
          out.add(copyMeta(meta, scope, includeDesignGaps));
        }
      }
    } catch (IllegalStateException e) {
      // Managers require server init; empty catalog rather than 500 in partial boot.
      log.debug("CE control manager not initialized for scope {}: {}", scope, e.getMessage());
    } catch (RuntimeException e) {
      log.warn("Failed to load {} CE controls: {}", scope, e.getMessage());
      log.debug(e);
    }
    return out;
  }

  private ControlDef copyMeta(PSControlMeta meta, String scope, boolean includeDesignGaps) {
    ControlDef d = new ControlDef();
    d.setName(meta.getName());
    d.setDisplayName(meta.getDisplayName());
    d.setDescription(meta.getDescription());
    d.setDimension(meta.getDimension());
    d.setChoiceSet(meta.getChoiceSet());
    d.setScope(scope);
    d.setDeprecated(meta.isDeprecated());
    d.setDeprecatedReplacement(meta.getDeprecatedReplacementName());
    d.setParameters(copyParams(meta.getParams()));
    // null + NON_NULL omits designGaps on list rows; detail attaches the shared catalog list.
    d.setDesignGaps(includeDesignGaps ? new ArrayList<>(DESIGN_GAPS) : null);
    return d;
  }

  private static List<ControlParameter> copyParams(List<?> params) {
    List<ControlParameter> out = new ArrayList<>();
    if (params == null) {
      return out;
    }
    for (Object o : params) {
      if (o instanceof PSControlParameter p) {
        ControlParameter cp = new ControlParameter();
        cp.setName(p.getName());
        cp.setDescription(p.getDescription());
        cp.setDataType(p.getDataType());
        cp.setParamType(p.getParamType());
        cp.setDefaultValue(p.getDefaultValue());
        cp.setRequired(p.isRequired());
        out.add(cp);
      }
    }
    return out;
  }

  private ControlDef projectionFromBody(
      ControlDef body, String name, String dimension, String choiceSet, boolean includeGaps) {
    ControlDef d = new ControlDef();
    d.setName(name);
    d.setDisplayName(
        StringUtils.isNotBlank(body.getDisplayName()) ? body.getDisplayName().trim() : name);
    d.setDescription(body.getDescription() != null ? body.getDescription() : "");
    d.setDimension(dimension);
    d.setChoiceSet(choiceSet);
    d.setScope("user");
    d.setDesignGaps(includeGaps ? new ArrayList<>(DESIGN_GAPS) : null);
    return d;
  }

  private void writeUserControlFile(String name, String xsl) {
    Path dir = io.userControlsDirectory();
    Path file = dir.resolve(name + ".xsl");
    Path dirNorm = dir.toAbsolutePath().normalize();
    Path fileNorm = file.toAbsolutePath().normalize();
    if (!fileNorm.startsWith(dirNorm)) {
      throw new IllegalArgumentException("invalid control file path");
    }
    try {
      Files.createDirectories(dir);
      Files.writeString(file, xsl, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write user control file", e);
    }
    io.writeImports();
  }

  private Path findContainedUserFile(String name) {
    Path file = io.findUserControlFile(name);
    if (file == null) {
      return null;
    }
    Path dir = io.userControlsDirectory().toAbsolutePath().normalize();
    Path fileNorm = file.toAbsolutePath().normalize();
    if (!fileNorm.startsWith(dir)) {
      // Packaged / system path — never delete or overwrite.
      return null;
    }
    return fileNorm;
  }

  private void rejectSystemMutation(String name) {
    if (isSystemName(name)) {
      throw new WebApplicationException(SYSTEM_CONTROL_READONLY, 409);
    }
  }

  private boolean isSystemName(String name) {
    try {
      List<PSControlMeta> metas = io.loadSystemControls();
      if (metas == null) {
        return false;
      }
      for (PSControlMeta meta : metas) {
        if (meta != null && name.equalsIgnoreCase(meta.getName())) {
          return true;
        }
      }
    } catch (RuntimeException e) {
      log.debug("Unable to load system CE controls while checking {}: {}", name, e.getMessage());
    }
    return false;
  }

  private void assertNameUnique(String name) {
    for (ControlDef existing : loadCatalog(false)) {
      if (existing != null && name.equalsIgnoreCase(existing.getName())) {
        throw new WebApplicationException("Control already exists: " + name, 409);
      }
    }
  }

  private String resolveXslSource(
      String name, ControlDef body, ControlDef existing, String dimension, String choiceSet) {
    if (body.getXslSource() != null && !body.getXslSource().isBlank()) {
      validateXslSource(name, body.getXslSource());
      return body.getXslSource();
    }
    String displayName =
        firstNonBlank(
            body.getDisplayName(), existing != null ? existing.getDisplayName() : null, name);
    String description =
        body.getDescription() != null
            ? body.getDescription()
            : (existing != null ? existing.getDescription() : "");
    return generateDefaultXsl(name, displayName, description, dimension, choiceSet);
  }

  static void validateXslSource(String name, String xsl) {
    try {
      Document doc = PSXmlDocumentBuilder.createXmlDocument(new StringReader(xsl), false);
      NodeList nodes = doc.getElementsByTagName(PSControlMeta.XML_NODE_NAME);
      if (nodes.getLength() != 1) {
        throw new IllegalArgumentException(
            "xslSource must contain exactly one " + PSControlMeta.XML_NODE_NAME);
      }
      PSControlMeta meta = new PSControlMeta((Element) nodes.item(0));
      if (!name.equals(meta.getName())) {
        throw new IllegalArgumentException("xslSource control name must match " + name);
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("invalid xslSource", e);
    }
  }

  static String generateDefaultXsl(
      String name, String displayName, String description, String dimension, String choiceSet) {
    String n = xmlEscape(name);
    String dn = xmlEscape(displayName);
    String desc = xmlEscape(description != null ? description : "");
    String dim = xmlEscape(dimension);
    String cs = xmlEscape(choiceSet);
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<xsl:stylesheet version=\"1.1\"\n"
        + "  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n"
        + "  xmlns:psxctl=\"urn:percussion.com/control\"\n"
        + "  xmlns=\"http://www.w3.org/1999/xhtml\"\n"
        + "  exclude-result-prefixes=\"psxctl\">\n"
        + "  <xsl:template match=\"/\"/>\n"
        + "  <psxctl:ControlMeta name=\""
        + n
        + "\" displayName=\""
        + dn
        + "\" dimension=\""
        + dim
        + "\" choiceset=\""
        + cs
        + "\">\n"
        + "    <psxctl:Description>"
        + desc
        + "</psxctl:Description>\n"
        + "  </psxctl:ControlMeta>\n"
        + "  <xsl:template match=\"Control[@name='"
        + n
        + "']\" mode=\"psxcontrol\">\n"
        + "    <input type=\"text\" name=\"{@paramName}\" value=\"{Value}\"/>\n"
        + "  </xsl:template>\n"
        + "</xsl:stylesheet>\n";
  }

  static String xmlEscape(String raw) {
    if (raw == null) {
      return "";
    }
    return raw.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  static String requireValidName(String raw) {
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
    if (!isSafeControlKey(name)) {
      throw new IllegalArgumentException("invalid name");
    }
    if (name.length() > MAX_CONTROL_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "name must not exceed " + MAX_CONTROL_NAME_LENGTH + " characters");
    }
    return name;
  }

  static String resolveDimension(String raw) {
    if (StringUtils.isBlank(raw)) {
      return "single";
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (!DIMENSIONS.contains(value)) {
      throw new IllegalArgumentException("dimension must be single, array, or table");
    }
    return value;
  }

  static String resolveChoiceSet(String raw) {
    if (StringUtils.isBlank(raw)) {
      return "none";
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (!CHOICE_SETS.contains(value)) {
      throw new IllegalArgumentException("choiceSet must be none, required, or optional");
    }
    return value;
  }

  private static boolean containsWhitespace(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isWhitespace(name.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String v : values) {
      if (StringUtils.isNotBlank(v)) {
        return v.trim();
      }
    }
    return "";
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

  /**
   * Single path segment name only — ASCII identifier characters used by CE control names ({@code
   * sys_EditBox}, {@code myCustomControl}). Rejects traversal, separators, and any non-identifier
   * Unicode.
   */
  static boolean isSafeControlKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    // Control names are conventional ASCII identifiers with _ . - only.
    return key.matches("[A-Za-z0-9_.-]+");
  }
}
