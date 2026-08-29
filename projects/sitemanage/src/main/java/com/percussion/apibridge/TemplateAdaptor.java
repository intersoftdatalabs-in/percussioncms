/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.apibridge;

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.percussion.rest.DesignGap;
import com.percussion.rest.templates.ITemplatesAdaptor;
import com.percussion.rest.templates.TemplateBindingSummary;
import com.percussion.rest.templates.TemplateDetail;
import com.percussion.rest.templates.TemplateExport;
import com.percussion.rest.templates.TemplateFilter;
import com.percussion.rest.templates.TemplateSlotSummary;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.xml.PSInvalidXmlException;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.PSAssemblyWsLocator;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

/** Adaptor for managing templates in Percussion CMS. */
@PSSiteManageBean
public class TemplateAdaptor implements ITemplatesAdaptor {

  private static final Logger log = LogManager.getLogger(TemplateAdaptor.class);

  /** Default assembler for Design SPA / REST create (HTML-first, no Widget XML). */
  public static final String DEFAULT_CREATE_ASSEMBLER =
      "Java/global/percussion/assembly/htmlAssembler";

  /** API capability notes shared by every detail payload (not per-template data). */
  static final List<DesignGap> TEMPLATE_DESIGN_GAPS =
      List.of(
          DesignGap.of("TPL_LOCK", "Lock not supported via this API"),
          DesignGap.of(
              "TPL_CONTENT_TYPE_ASSOC", "Content-type associations not listed on this payload"));

  static final String ADMIN_REQUIRED =
      "Admin role required to export or import assembly templates";

  private final IPSAssemblyService asmSvc;
  private final IPSContentWs contentwsService;
  private final IPSAssemblyDesignWs designWs;
  private final BooleanSupplier adminChecker;

  /**
   * Injected by Spring in production ({@code required} so a missing bean fails at context load).
   * Unused when {@link #adminChecker} is overridden in tests.
   */
  @Autowired
  private IPSUserService userService;

  public TemplateAdaptor() {
    this(
        PSAssemblyServiceLocator.getAssemblyService(),
        PSContentWsLocator.getContentWebservice(),
        PSAssemblyWsLocator.getAssemblyDesignWebservice(),
        null);
  }

  /** Package-visible for unit tests that inject a fake assembly service. */
  TemplateAdaptor(IPSAssemblyService asmSvc, IPSContentWs contentwsService) {
    this(asmSvc, contentwsService, null, () -> true);
  }

  /**
   * Package-visible for export and import tests that inject assembly design WS and an Admin gate.
   * {@code null} adminChecker uses {@link #isCurrentUserAdmin()}.
   */
  TemplateAdaptor(
      IPSAssemblyService asmSvc,
      IPSContentWs contentwsService,
      IPSAssemblyDesignWs designWs,
      BooleanSupplier adminChecker) {
    this.asmSvc = asmSvc;
    this.contentwsService = contentwsService;
    this.designWs = designWs;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public List<TemplateSummary> listAllTemplateSummaries(URI baseUri) {
    try {
      var summaries = asmSvc.getSummaries(PSTypeEnum.TEMPLATE);
      return summaries.stream().map(ApiUtils::convertTemplateSummary).collect(Collectors.toList());
    } catch (PSCatalogException e) {
      throw new WebApplicationException(e, 500);
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Not Found", 404);
    }
  }

  @Override
  public TemplateDetail getTemplate(URI baseUri, String idOrName) {
    // baseUri reserved for HATEOAS link building
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    try {
      IPSAssemblyTemplate t = resolveTemplate(idOrName.trim());
      if (t == null) {
        return null;
      }
      return toDetail(t);
    } catch (PSNotFoundException e) {
      return null;
    } catch (IllegalArgumentException e) {
      // Bad id form — surface as 400 via resource mapping if desired; treat as not found for now
      log.debug("Invalid template idOrName {}: {}", idOrName, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error(
          "Failed to load template {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @Override
  public TemplateDetail updateTemplate(URI baseUri, String idOrName, TemplateDetail body) {
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    try {
      IPSAssemblyTemplate t = resolveTemplate(idOrName.trim());
      if (t == null) {
        return null;
      }
      if (body.getLabel() != null) {
        t.setLabel(body.getLabel());
      }
      if (body.getDescription() != null) {
        t.setDescription(body.getDescription());
      }
      if (body.getTemplateSource() != null) {
        t.setTemplate(body.getTemplateSource());
      }
      // null = leave unchanged; non-blank string sets assembler extension name
      if (body.getAssembler() != null) {
        String assembler = body.getAssembler().trim();
        if (StringUtils.isBlank(assembler)) {
          throw new IllegalArgumentException("assembler must not be blank when provided");
        }
        t.setAssembler(assembler);
      }
      // null = leave unchanged; non-null list (including empty) = full replace
      if (body.getBindings() != null) {
        t.setBindings(toBindings(body.getBindings()));
      }
      if (body.getSlots() != null) {
        t.setSlots(toSlots(body.getSlots()));
      }
      asmSvc.saveTemplate(t);
      IPSAssemblyTemplate reloaded = resolveTemplate(idOrName.trim());
      return reloaded != null ? toDetail(reloaded) : toDetail(t);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSAssemblyException e) {
      log.error("Failed to save template {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to save template", e);
    } catch (Exception e) {
      log.error(
          "Failed to update template {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new IllegalStateException("Failed to update template", e);
    }
  }

  @Override
  public TemplateDetail createTemplate(URI baseUri, TemplateDetail body) {
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String name = validateCreateName(body.getName());
    if (templateNameExists(name)) {
      throw new IllegalArgumentException("template name already exists: " + name);
    }
    String assembler =
        StringUtils.isNotBlank(body.getAssembler())
            ? body.getAssembler().trim()
            : DEFAULT_CREATE_ASSEMBLER;
    if (StringUtils.isBlank(assembler)) {
      throw new IllegalArgumentException("assembler must not be blank when provided");
    }
    String label =
        StringUtils.isNotBlank(body.getLabel()) ? body.getLabel().trim() : name;
    try {
      IPSAssemblyTemplate t = asmSvc.createTemplate();
      t.setName(name);
      t.setLabel(label);
      if (body.getDescription() != null) {
        t.setDescription(body.getDescription());
      }
      t.setAssembler(assembler);
      t.setMimeType(
          StringUtils.isNotBlank(body.getMimeType()) ? body.getMimeType().trim() : "text/html");
      t.setOutputFormat(IPSAssemblyTemplate.OutputFormat.Snippet);
      t.setTemplateType(IPSAssemblyTemplate.TemplateType.Shared);
      if (body.getTemplateSource() != null) {
        t.setTemplate(body.getTemplateSource());
      } else {
        t.setTemplate("");
      }
      if (body.getBindings() != null) {
        t.setBindings(toBindings(body.getBindings()));
      }
      if (body.getSlots() != null) {
        t.setSlots(toSlots(body.getSlots()));
      }
      asmSvc.saveTemplate(t);
      IPSAssemblyTemplate reloaded = resolveTemplate(name);
      return reloaded != null ? toDetail(reloaded) : toDetail(t);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSAssemblyException e) {
      if (templatePresent(name)) {
        throw new IllegalArgumentException("template name already exists: " + name, e);
      }
      log.error("Failed to create template {}: {}", name, e.getMessage(), e);
      throw new IllegalStateException("Failed to create template", e);
    } catch (Exception e) {
      log.error(
          "Failed to create template {} ({}): {}",
          name,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new IllegalStateException("Failed to create template", e);
    }
  }

  @Override
  public boolean deleteTemplate(URI baseUri, String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    try {
      IPSAssemblyTemplate t = resolveTemplate(idOrName.trim());
      if (t == null) {
        return false;
      }
      if (t.getGUID() == null) {
        log.error(
            "Template '{}' loaded without a GUID; refusing delete (corrupt identifier).",
            idOrName);
        throw new IllegalStateException(
            "Template '" + idOrName + "' has no GUID (corrupt identifier); cannot delete");
      }
      asmSvc.deleteTemplate(t.getGUID());
      return true;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw e;
    } catch (PSAssemblyException e) {
      if (e.getErrorCode() == AssemblyErrorCodes.TEMPLATE_MISSING.numericCode()) {
        return false;
      }
      log.error("Failed to delete template {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to delete template", e);
    } catch (Exception e) {
      log.error(
          "Failed to delete template {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new IllegalStateException("Failed to delete template", e);
    }
  }

  @Override
  public TemplateExport exportTemplate(URI baseUri, String idOrName) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    if (designWs == null) {
      throw new IllegalStateException("assembly design WS is not configured");
    }
    try {
      PSAssemblyTemplateWs loaded = loadTemplateWsReadOnly(idOrName.trim());
      if (loaded == null || loaded.getTemplate() == null) {
        return null;
      }
      IPSAssemblyTemplate template = loaded.getTemplate();
      TemplateExport exported = new TemplateExport();
      exported.setName(template.getName());
      exported.setXml(toDesignXml(template));
      return exported;
    } catch (PSErrorResultsException e) {
      log.debug("Template export not found {}: {}", idOrName, e.getMessage());
      return null;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to export template {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new IllegalStateException("Failed to export template", e);
    }
  }

  @Override
  public TemplateDetail importTemplate(URI baseUri, String xml) {
    requireAdmin();
    requireSessionUserForDesignWrite();
    if (designWs == null) {
      throw new IllegalStateException("assembly design WS is not configured");
    }
    PSAssemblyTemplate parsed = parseDesignXml(xml);
    String name = validateCreateName(parsed.getName());
    assertImportNameUnique(name);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSAssemblyTemplateWs> created =
          designWs.createAssemblyTemplates(List.of(name), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createAssemblyTemplates returned empty");
      }
      PSAssemblyTemplateWs createdWs = created.get(0);
      IPSAssemblyTemplate dest = createdWs.getTemplate();
      if (dest == null || dest.getGUID() == null) {
        throw new IllegalStateException("Design WS createAssemblyTemplates returned no GUID");
      }
      IPSGuid keepGuid = dest.getGUID();
      applyDesignXml(dest, xml, keepGuid);
      dest.setName(name);
      // New object lock is ours; release=true so import does not leave a held lock.
      designWs.saveAssemblyTemplates(List.of(createdWs), true, session, user);
      IPSAssemblyTemplate reloaded = loadImportedReadOnly(name, keepGuid, session, user);
      return reloaded != null ? toDetail(reloaded) : toDetail(dest);
    } catch (WebApplicationException | IllegalStateException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Template already exists: " + name, 409);
      }
      throw e;
    } catch (PSErrorsException e) {
      throw mapImportPersistFailure(name, e);
    } catch (Exception e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Template already exists: " + name, 409);
      }
      log.error(
          "Failed to import template {} ({}): {}",
          name,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new IllegalStateException("Failed to import template", e);
    }
  }

  /**
   * Load one template through {@link IPSAssemblyDesignWs} without locking ({@code lock=false},
   * {@code overrideLock=false}) so export never steals a Workbench lock.
   */
  private PSAssemblyTemplateWs loadTemplateWsReadOnly(String idOrName)
      throws PSErrorResultsException {
    String session = currentSession();
    String user = currentUser();
    IPSGuid guid = parseTemplateGuid(idOrName);
    if (guid != null) {
      List<PSAssemblyTemplateWs> loaded =
          designWs.loadAssemblyTemplates(List.of(guid), false, false, session, user);
      if (loaded == null || loaded.isEmpty()) {
        return null;
      }
      return loaded.get(0);
    }
    List<IPSCatalogSummary> found =
        designWs.findAssemblyTemplates(idOrName, null, null, null, null, null, null);
    if (found == null || found.isEmpty()) {
      return null;
    }
    for (IPSCatalogSummary sum : found) {
      if (sum == null || sum.getGUID() == null) {
        continue;
      }
      if (idOrName.equalsIgnoreCase(sum.getName()) || idOrName.equalsIgnoreCase(sum.getLabel())) {
        List<PSAssemblyTemplateWs> loaded =
            designWs.loadAssemblyTemplates(List.of(sum.getGUID()), false, false, session, user);
        if (loaded != null && !loaded.isEmpty()) {
          return loaded.get(0);
        }
      }
    }
    if (found.size() == 1 && found.get(0) != null && found.get(0).getGUID() != null) {
      List<PSAssemblyTemplateWs> loaded =
          designWs.loadAssemblyTemplates(
              List.of(found.get(0).getGUID()), false, false, session, user);
      if (loaded != null && !loaded.isEmpty()) {
        return loaded.get(0);
      }
    }
    return null;
  }

  static String toDesignXml(IPSAssemblyTemplate template) {
    if (template == null) {
      throw new IllegalArgumentException("template is required");
    }
    try {
      if (template instanceof IPSCatalogItem item) {
        return item.toXML();
      }
      return PSXmlSerializationHelper.writeToXml(template);
    } catch (IOException | SAXException e) {
      throw new IllegalStateException("Failed to serialize template design XML", e);
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

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  private static IPSGuid parseTemplateGuid(String idOrName) {
    try {
      if (StringUtils.isNumeric(idOrName)) {
        return new PSGuid(PSTypeEnum.TEMPLATE, Long.parseLong(idOrName));
      }
      if (idOrName.matches("\\d+-\\d+(-\\d+)?")) {
        PSGuid g = new PSGuid(idOrName);
        if (g.getType() == 0) {
          g = new PSGuid(PSTypeEnum.TEMPLATE, g.getUUID());
        }
        return g;
      }
    } catch (RuntimeException e) {
      log.debug("Not a template GUID: {}", idOrName);
    }
    return null;
  }

  /**
   * Parse Workbench / REST-export {@code assembly-template} design XML. Invalid or non-template XML
   * is 400 via {@link IllegalArgumentException}.
   */
  static PSAssemblyTemplate parseDesignXml(String xml) {
    if (StringUtils.isBlank(xml)) {
      throw new IllegalArgumentException("assembly-template XML is required");
    }
    String trimmed = xml.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      throw new IllegalArgumentException("expected assembly-template XML");
    }
    PSAssemblyTemplate parsed = new PSAssemblyTemplate();
    try {
      parsed.fromXML(trimmed);
    } catch (IOException | SAXException | PSInvalidXmlException | RuntimeException e) {
      throw new IllegalArgumentException("invalid assembly-template XML", e);
    }
    if (StringUtils.isBlank(parsed.getName())) {
      throw new IllegalArgumentException("assembly-template XML is missing name");
    }
    return parsed;
  }

  /**
   * Apply exported design XML onto the newly created template, keeping the GUID assigned by {@code
   * createAssemblyTemplates} so we never steal an existing object's identity or lock.
   */
  private static void applyDesignXml(IPSAssemblyTemplate dest, String xml, IPSGuid keepGuid)
      throws IOException, SAXException, PSInvalidXmlException {
    if (!(dest instanceof IPSCatalogItem item)) {
      throw new IllegalStateException("created template is not a catalog item");
    }
    item.fromXML(xml);
    dest.setGUID(keepGuid);
  }

  private void assertImportNameUnique(String name) {
    List<IPSCatalogSummary> existing =
        designWs.findAssemblyTemplates(name, null, null, null, null, null, null);
    if (existing == null) {
      return;
    }
    for (IPSCatalogSummary summary : existing) {
      if (summary != null && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
        throw new WebApplicationException("Template already exists: " + name, 409);
      }
    }
  }

  /**
   * Reload the imported template without locking ({@code lock=false}, {@code overrideLock=false}).
   */
  private IPSAssemblyTemplate loadImportedReadOnly(
      String name, IPSGuid guid, String session, String user) {
    try {
      if (guid != null) {
        List<PSAssemblyTemplateWs> loaded =
            designWs.loadAssemblyTemplates(List.of(guid), false, false, session, user);
        if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null) {
          return loaded.get(0).getTemplate();
        }
      }
      List<IPSCatalogSummary> found =
          designWs.findAssemblyTemplates(name, null, null, null, null, null, null);
      if (found == null) {
        return null;
      }
      for (IPSCatalogSummary sum : found) {
        if (sum == null || sum.getGUID() == null) {
          continue;
        }
        if (name.equalsIgnoreCase(sum.getName())) {
          List<PSAssemblyTemplateWs> loaded =
              designWs.loadAssemblyTemplates(List.of(sum.getGUID()), false, false, session, user);
          if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null) {
            return loaded.get(0).getTemplate();
          }
        }
      }
    } catch (PSErrorResultsException e) {
      log.debug("Imported template reload skipped for {}: {}", name, e.getMessage());
    }
    return null;
  }

  private RuntimeException mapImportPersistFailure(String name, PSErrorsException e) {
    if (isAlreadyExistsFailure(e)) {
      return new WebApplicationException("Template already exists: " + name, 409);
    }
    if (isLockFailure(e)) {
      return new WebApplicationException(
          "Could not import template; design lock required or held by another user", 409);
    }
    log.error("Failed to save imported template {}: {}", name, e.getMessage(), e);
    return new IllegalStateException("Failed to import template", e);
  }

  static boolean isAlreadyExistsFailure(Throwable t) {
    for (Throwable cur = t; cur != null && cur != cur.getCause(); cur = cur.getCause()) {
      String msg = cur.getMessage();
      if (cur instanceof PSErrorException pe && StringUtils.isNotBlank(pe.getErrorMessage())) {
        msg = pe.getErrorMessage();
      }
      if (msg != null && msg.toLowerCase().contains("already exists")) {
        return true;
      }
    }
    return false;
  }

  private static boolean isLockFailure(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (err == null) {
        continue;
      }
      String msg = err instanceof PSErrorException pe ? pe.getErrorMessage() : String.valueOf(err);
      if (msg != null) {
        String lower = msg.toLowerCase();
        if (lower.contains("lock") || lower.contains("locked")) {
          return true;
        }
      }
    }
    return false;
  }

  private static void requireSessionUserForDesignWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for template design import", Response.Status.FORBIDDEN);
    }
  }

  /**
   * Unique assembly-template name: required, trimmed, no whitespace. Starts with a letter; then
   * letters, digits, {@code .}, {@code _}, or {@code -}.
   */
  static String validateCreateName(String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("name is required");
    }
    String name = raw.trim();
    if (name.chars().anyMatch(Character::isWhitespace)) {
      throw new IllegalArgumentException("name cannot contain spaces");
    }
    if (!name.matches("[A-Za-z][A-Za-z0-9._-]*")) {
      throw new IllegalArgumentException(
          "name must start with a letter and contain only letters, digits, '.', '_' or '-'");
    }
    return name;
  }

  private boolean templateNameExists(String name) {
    try {
      return asmSvc.findTemplateByName(name) != null;
    } catch (PSAssemblyException e) {
      if (e.getErrorCode() == AssemblyErrorCodes.TEMPLATE_MISSING.numericCode()) {
        return false;
      }
      throw new IllegalStateException("Failed to look up template name: " + name, e);
    } catch (PSNotFoundException e) {
      return false;
    }
  }

  /** True when the name is loadable after a failed save (concurrent duplicate). */
  private boolean templatePresent(String name) {
    try {
      return asmSvc.findTemplateByName(name) != null;
    } catch (PSAssemblyException e) {
      return false;
    } catch (PSNotFoundException e) {
      return false;
    }
  }

  private List<PSTemplateBinding> toBindings(List<TemplateBindingSummary> summaries) {
    List<PSTemplateBinding> out = new ArrayList<>();
    if (summaries == null) {
      return out;
    }
    int i = 0;
    for (TemplateBindingSummary s : summaries) {
      i++;
      if (s == null) {
        throw new IllegalArgumentException("bindings[" + (i - 1) + "] is null");
      }
      if (StringUtils.isBlank(s.getVariable())) {
        throw new IllegalArgumentException("bindings[" + (i - 1) + "].variable is required");
      }
      if (StringUtils.isBlank(s.getExpression())) {
        throw new IllegalArgumentException("bindings[" + (i - 1) + "].expression is required");
      }
      int order =
          s.getExecutionOrder() != null && s.getExecutionOrder() > 0 ? s.getExecutionOrder() : i;
      out.add(new PSTemplateBinding(order, s.getVariable().trim(), s.getExpression().trim()));
    }
    return out;
  }

  private Set<IPSTemplateSlot> toSlots(List<TemplateSlotSummary> summaries)
      throws PSAssemblyException {
    Set<IPSTemplateSlot> out = new HashSet<>();
    if (summaries == null) {
      return out;
    }
    int i = 0;
    for (TemplateSlotSummary s : summaries) {
      i++;
      if (s == null) {
        throw new IllegalArgumentException("slots[" + (i - 1) + "] is null");
      }
      IPSTemplateSlot slot = resolveSlotRef(s);
      if (slot == null) {
        String key =
            s.getName() != null
                ? s.getName()
                : (s.getGuid() != null && s.getGuid().getStringValue() != null
                    ? s.getGuid().getStringValue()
                    : "?");
        throw new IllegalArgumentException("slots[" + (i - 1) + "] not found: " + key);
      }
      out.add(slot);
    }
    return out;
  }

  private IPSTemplateSlot resolveSlotRef(TemplateSlotSummary s) throws PSAssemblyException {
    if (s.getGuid() != null && s.getGuid().getStringValue() != null) {
      String sv = s.getGuid().getStringValue();
      if (StringUtils.isNotBlank(sv)) {
        try {
          return asmSvc.loadSlot(ApiUtils.convertGuid(s.getGuid()));
        } catch (Exception e) {
          log.debug("Slot GUID load failed for {}: {}", sv, e.getMessage());
        }
      }
    }
    if (StringUtils.isNotBlank(s.getName())) {
      try {
        return asmSvc.findSlotByName(s.getName().trim());
      } catch (PSAssemblyException e) {
        return null;
      }
    }
    return null;
  }

  /**
   * Resolve by: pure numeric uuid → typed GUID string (digits and dashes only) → name. Names that
   * contain dashes but are not pure GUID digit/dash forms fall through to name lookup.
   */
  private IPSAssemblyTemplate resolveTemplate(String idOrName) throws Exception {
    if (StringUtils.isNumeric(idOrName)) {
      long uuid = Long.parseLong(idOrName);
      IPSGuid g = new PSGuid(PSTypeEnum.TEMPLATE, uuid);
      try {
        return asmSvc.loadTemplate(g, true);
      } catch (PSNotFoundException e) {
        return null;
      }
    }
    // GUID-shaped only (e.g. 0-10-347), not arbitrary names with dashes
    if (idOrName.matches("\\d+-\\d+(-\\d+)?")) {
      try {
        PSGuid g = new PSGuid(idOrName);
        if (g.getType() == 0) {
          g = new PSGuid(PSTypeEnum.TEMPLATE, g.getUUID());
        }
        return asmSvc.loadTemplate(g, true);
      } catch (PSNotFoundException e) {
        return null;
      } catch (IllegalArgumentException e) {
        log.debug("Not a template GUID, trying name: {}", idOrName);
      }
    }
    try {
      return asmSvc.findTemplateByName(idOrName);
    } catch (PSNotFoundException e) {
      return null;
    }
  }

  private TemplateDetail toDetail(IPSAssemblyTemplate t) {
    TemplateDetail d = new TemplateDetail();
    if (t.getGUID() != null) {
      d.setGuid(ApiUtils.convertGuid(t.getGUID()));
      d.setTemplateId(t.getGUID().getUUID());
    }
    d.setName(t.getName());
    d.setLabel(t.getLabel());
    d.setDescription(t.getDescription());
    d.setAssembler(t.getAssembler());
    d.setAssemblyUrl(t.getAssemblyUrl());
    d.setStyleSheet(t.getStyleSheetPath());
    d.setMimeType(t.getMimeType());
    d.setCharset(t.getCharset());
    d.setLocationPrefix(t.getLocationPrefix());
    d.setLocationSuffix(t.getLocationSuffix());
    if (t.getOutputFormat() != null) {
      d.setOutputFormat(t.getOutputFormat().name());
    }
    if (t.getActiveAssemblyType() != null) {
      d.setAaType(t.getActiveAssemblyType().name());
    }
    if (t.getPublishWhen() != null) {
      d.setPublishWhen(t.getPublishWhen().name());
    }
    if (t.getTemplateType() != null) {
      d.setTemplateType(t.getTemplateType().name());
    }
    if (t.getGlobalTemplateUsage() != null) {
      d.setGlobalTemplateUsage(t.getGlobalTemplateUsage().name());
    }
    d.setVariant(t.isVariant());
    d.setTemplateSource(t.getTemplate());

    List<TemplateBindingSummary> bindings = new ArrayList<>();
    List<PSTemplateBinding> rawBindings = t.getBindings();
    if (rawBindings != null) {
      for (PSTemplateBinding b : rawBindings) {
        if (b == null) continue;
        TemplateBindingSummary s = new TemplateBindingSummary();
        s.setExecutionOrder(b.getExecutionOrder());
        s.setVariable(b.getVariable());
        s.setExpression(b.getExpression());
        bindings.add(s);
      }
      bindings.sort(
          Comparator.comparing(
              TemplateBindingSummary::getExecutionOrder, Comparator.nullsLast(Integer::compareTo)));
    }
    d.setBindings(bindings);

    List<TemplateSlotSummary> slots = new ArrayList<>();
    if (t.getSlots() != null) {
      for (IPSTemplateSlot slot : t.getSlots()) {
        if (slot == null) continue;
        TemplateSlotSummary s = new TemplateSlotSummary();
        try {
          if (slot.getGUID() != null) {
            s.setGuid(ApiUtils.convertGuid(slot.getGUID()));
          }
        } catch (Exception e) {
          log.warn("Could not convert slot GUID for {}: {}", slot.getName(), e.getMessage(), e);
        }
        s.setName(slot.getName());
        s.setLabel(StringUtils.defaultIfBlank(slot.getLabel(), slot.getName()));
        s.setDescription(slot.getDescription());
        slots.add(s);
      }
      slots.sort(
          Comparator.comparing(
              TemplateSlotSummary::getLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }
    d.setSlots(slots);
    d.setDesignGaps(new ArrayList<>(TEMPLATE_DESIGN_GAPS));
    return d;
  }

  /**
   * Returns all template summaries that match the supplied filter. NOTE: Currently only contentId
   * is implemented. TODO: Implement for all filter options.
   */
  @Override
  public List<TemplateSummary> listTemplateSummaries(URI baseUri, TemplateFilter filter) {
    if (filter == null) {
      throw new IllegalArgumentException("TemplateFilter cannot be null");
    }
    var ret = new ArrayList<TemplateSummary>();
    int contentID = filter.getContentId();

    try {
      var guids = new ArrayList<IPSGuid>();
      guids.add(PSGuidManagerLocator.getGuidMgr().makeGuid(contentID, PSTypeEnum.LEGACY_CONTENT));
      var items = contentwsService.loadItems(guids, false, false, false, false);
      if (items != null && !items.isEmpty()) {
        var item = items.get(0);
        long contentTypeId = item.getContentTypeId();
        var ctypeGuid =
            PSGuidManagerLocator.getGuidMgr().makeGuid(contentTypeId, PSTypeEnum.NODEDEF);
        var templates = asmSvc.findTemplatesByContentType(ctypeGuid);

        ret.addAll(
            templates.stream().map(ApiUtils::convertTemplateSummary).collect(Collectors.toList()));
        return ret;
      } else {
        throw new PSNotFoundException("Content Id: " + contentID + " not found.");
      }
    } catch (PSAssemblyException | PSErrorResultsException e) {
      throw new WebApplicationException(e, 500);
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Not Found", 404);
    }
  }
}
