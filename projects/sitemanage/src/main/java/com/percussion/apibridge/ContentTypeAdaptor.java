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

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSApplyWhen;
import com.percussion.design.objectstore.PSChoiceTableInfo;
import com.percussion.design.objectstore.PSChoices;
import com.percussion.design.objectstore.PSConditional;
import com.percussion.design.objectstore.PSConditionalExit;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSContentTypeHelper;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSEntry;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSExtensionCallSet;
import com.percussion.design.objectstore.PSExtensionParamValue;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSFieldTranslation;
import com.percussion.design.objectstore.PSFieldValidationRules;
import com.percussion.design.objectstore.IPSReplacementValue;
import com.percussion.design.objectstore.PSInputTranslations;
import com.percussion.design.objectstore.PSOutputTranslations;
import com.percussion.design.objectstore.PSParam;
import com.percussion.design.objectstore.PSPipe;
import com.percussion.design.objectstore.PSRule;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.design.objectstore.PSUrlRequest;
import com.percussion.design.objectstore.PSValidationRules;
import com.percussion.design.objectstore.PSVisibilityRules;
import com.percussion.design.objectstore.PSWorkflowInfo;
import com.percussion.extension.PSExtensionRef;
import com.percussion.rest.DesignGap;
import com.percussion.rest.Guid;
import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeChoiceCatalog;
import com.percussion.rest.contenttypes.ContentTypeChoiceEntry;
import com.percussion.rest.contenttypes.ContentTypeChoiceTable;
import com.percussion.rest.contenttypes.ContentTypeControlProperty;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.rest.contenttypes.ContentTypeFieldConditional;
import com.percussion.rest.contenttypes.ContentTypeFieldControlProperties;
import com.percussion.rest.contenttypes.ContentTypeFieldRule;
import com.percussion.rest.contenttypes.ContentTypeFieldRuleExpressions;
import com.percussion.rest.contenttypes.ContentTypeFilter;
import com.percussion.rest.contenttypes.ContentTypeItemExit;
import com.percussion.rest.contenttypes.ContentTypeItemExitParam;
import com.percussion.rest.contenttypes.ContentTypeItemExits;
import com.percussion.rest.contenttypes.IContentTypesAdaptor;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.services.locking.data.PSObjectLockSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.data.PSContentTypeWorkflow;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.util.PSCollection;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.string.PSStringUtils;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.webservices.system.IPSSystemDesignWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean
public class ContentTypeAdaptor implements IContentTypesAdaptor {

  private static final Logger log = LogManager.getLogger(ContentTypeAdaptor.class);

  /** Typical design-session lock duration in minutes ({@link PSObjectLock#LOCK_INTERVAL}). */
  static final long DESIGN_LOCK_MINUTES = PSObjectLock.LOCK_INTERVAL / 60_000L;

  private final IPSContentDesignWs designSvc;
  private final PSItemDefManager itemDefManager;
  private final IPSSystemDesignWs systemDesign;
  private final BooleanSupplier adminChecker;
  private final IPSAssemblyService assemblyService;
  private final IPSWorkflowService workflowService;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ContentTypeAdaptor() {
    this(
        PSContentWsLocator.getContentDesignWebservice(),
        PSItemDefManager.getInstance(),
        PSSystemWsLocator.getSystemDesignWebservice(),
        null,
        null,
        null);
  }

  /** Package-visible for unit tests that inject design web services and Admin gate. */
  ContentTypeAdaptor(
      IPSContentDesignWs designSvc,
      PSItemDefManager itemDefManager,
      IPSSystemDesignWs systemDesign,
      BooleanSupplier adminChecker) {
    this(designSvc, itemDefManager, systemDesign, adminChecker, null, null);
  }

  /**
   * Package-visible for unit tests that inject design web services and the assembly service (no
   * Admin gate). Defaults to an always-allow Admin check so tests can focus on the lock / template
   * save flow without wiring a user service.
   */
  ContentTypeAdaptor(
      IPSContentDesignWs designSvc,
      PSItemDefManager itemDefManager,
      IPSSystemDesignWs systemDesign,
      IPSAssemblyService assemblyService) {
    this(designSvc, itemDefManager, systemDesign, () -> true, assemblyService, null);
  }

  /**
   * Package-visible for unit tests that inject design web services, Admin gate, and a workflow
   * service mock. Assembly service falls back to the locator.
   */
  ContentTypeAdaptor(
      IPSContentDesignWs designSvc,
      PSItemDefManager itemDefManager,
      IPSSystemDesignWs systemDesign,
      BooleanSupplier adminChecker,
      IPSWorkflowService workflowService) {
    this(designSvc, itemDefManager, systemDesign, adminChecker, null, workflowService);
  }

  ContentTypeAdaptor(
      IPSContentDesignWs designSvc,
      PSItemDefManager itemDefManager,
      IPSSystemDesignWs systemDesign,
      BooleanSupplier adminChecker,
      IPSAssemblyService assemblyService,
      IPSWorkflowService workflowService) {
    this.designSvc = designSvc;
    this.itemDefManager = itemDefManager;
    this.systemDesign = systemDesign;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.assemblyService = assemblyService;
    this.workflowService = workflowService;
  }

  /***
   * List all content types available to the System
   * @param baseUri Requesting URI
   * @return A list of all available Content Types
   */
  @Override
  public List<ContentType> listContentTypes(URI baseUri) {
    var ret = new ArrayList<ContentType>();
    var types = designSvc.findContentTypes("*");
    for (var s : types) {
      ret.add(ApiUtils.convertContentType(s));
    }
    return ret;
  }

  /***
   * List ContentTypes available for the specified Site
   * @param baseUri Originating URI
   * @param siteId Site Id for Site to filter Types by
   * @return An array of ContentTypes
   */
  @Override
  public List<ContentType> listContentTypes(URI baseUri, int siteId) {
    return null;
  }

  /***
   * List ContentTypes available for the specified Site
   * @param baseUri Originating URI
   * @param filter A ContentTypeFilter that can be used to filter content types.
   * @return An array of ContentTypes
   */
  @Override
  public List<ContentType> listContentTypesByFilter(URI baseUri, ContentTypeFilter filter) {
    return null;
  }

  @Override
  public ContentTypeDetail createContentType(URI baseUri, ContentTypeDetail body) {
    requireAdmin();
    if (body == null || StringUtils.isBlank(body.getName())) {
      throw new IllegalArgumentException("name is required");
    }
    String name = body.getName().trim();
    if (containsWhitespace(name)) {
      throw new IllegalArgumentException("name cannot contain spaces");
    }
    if (name.contains("*")) {
      throw new IllegalArgumentException("name must not contain wildcards");
    }
    requireSessionUserForLock();
    String session = currentSession();
    String user = currentUser();
    assertNameUnique(name);
    try {
      List<PSItemDefinition> created =
          designSvc.createContentTypes(Collections.singletonList(name), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createContentTypes returned empty");
      }
      PSItemDefinition def = created.get(0);
      // Design-WS default is enabled=true; set it explicitly so omitted JSON is usable.
      if (body.getEnabled() == null) {
        def.setEnabled(true);
      }
      applyMetaUpdates(def, body);
      // Workbench Finish: persist the new type and release the create lock.
      designSvc.saveContentTypes(Collections.singletonList(def), true, session, user);
      PSItemDefinition reloaded = reloadItemDef(name);
      return reloaded != null ? toDetail(reloaded) : toDetail(def);
    } catch (WebApplicationException | IllegalStateException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw mapCreateNameCollision(name, e);
    } catch (PSErrorException e) {
      throw mapCreatePersistFailure(name, e, "Failed to create content type");
    } catch (PSErrorsException e) {
      throw mapCreatePersistFailure(name, e, "Failed to save new content type");
    } catch (Exception e) {
      log.error("Failed to create content type {}: {}", name, e.getMessage(), e);
      throw new IllegalStateException("Failed to create content type", e);
    }
  }

  @Override
  public ContentTypeDetail getContentType(URI baseUri, String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    try {
      PSItemDefinition def = resolveItemDef(idOrName.trim(), true);
      if (def == null) {
        return null;
      }
      return toDetail(def);
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not found: {}", idOrName);
      return null;
    } catch (Exception e) {
      log.error("Failed to load content type {}: {}", idOrName, e.getMessage(), e);
      throw new RuntimeException(
          "Failed to load content type (" + e.getClass().getName() + "): " + e.getMessage(), e);
    }
  }

  private PSItemDefinition resolveItemDef(String idOrName) throws PSInvalidContentTypeException {
    return resolveItemDef(idOrName, false);
  }

  /**
   * Resolve a content type from the running item-def cache.
   *
   * @param includeDisabledFromStore when {@code true}, a cache miss falls back to the
   *     object store so disabled types (unregistered from the editor cache) still load.
   *     Only GET detail and enable/disable (CD-13) pass {@code true}; other callers keep
   *     the pre-CD-13 cache-only 404 for disabled types.
   */
  private PSItemDefinition resolveItemDef(String idOrName, boolean includeDisabledFromStore)
      throws PSInvalidContentTypeException {
    try {
      // Prefer numeric uuid
      if (StringUtils.isNumeric(idOrName)) {
        long uuid = Long.parseLong(idOrName);
        return itemDefManager.getItemDef(uuid, PSItemDefManager.COMMUNITY_ANY);
      }
      // Guid string forms: 0-2-301 or 0-301
      if (idOrName.contains("-")) {
        try {
          PSGuid g = new PSGuid(idOrName);
          if (g.getType() == 0) {
            g = new PSGuid(PSTypeEnum.NODEDEF, g.getUUID());
          }
          return itemDefManager.getItemDef(g.getUUID(), PSItemDefManager.COMMUNITY_ANY);
        } catch (PSInvalidContentTypeException e) {
          throw e;
        } catch (Exception ignore) {
          // fall through to name
        }
      }
      return itemDefManager.getItemDef(idOrName, PSItemDefManager.COMMUNITY_ANY);
    } catch (PSInvalidContentTypeException e) {
      if (includeDisabledFromStore) {
        PSItemDefinition fromStore = loadItemDefFromObjectStore(idOrName);
        if (fromStore != null) {
          return fromStore;
        }
      }
      throw e;
    }
  }

  /**
   * Object-store load when the item-def cache no longer has a running editor
   * (disabled content types unregister). Used so GET {@code enabled} still
   * reflects the saved application flag (CD-13). Package-visible for tests.
   */
  PSItemDefinition loadItemDefFromObjectStore(String idOrName) {
    try {
      IPSGuid guid = resolveExistingContentTypeGuid(idOrName);
      if (guid == null) {
        return null;
      }
      return PSContentTypeHelper.loadItemDef(guid);
    } catch (Exception e) {
      log.debug(
          "Object-store content type load after cache miss {}: {}: {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      return null;
    }
  }

  private ContentTypeDetail toDetail(PSItemDefinition def) {
    ContentTypeDetail detail = new ContentTypeDetail();
    detail.setName(def.getName());
    detail.setLabel(def.getLabel());
    detail.setDescription(def.getDescription());
    detail.setEnabled(def.isEnabled());
    detail.setHideFromMenu(def.isHidden());
    detail.setAppName(def.getAppName());
    detail.setEditorUrl(def.getEditorUrl());
    detail.setGuid(ApiUtils.convertGuid(new PSGuid(PSTypeEnum.NODEDEF, def.getTypeId())));

    Map<String, String> controlByField = new HashMap<>();
    Map<String, List<ContentTypeControlProperty>> controlPropsByField = new HashMap<>();
    boolean controlsResolved = mapControls(def, controlByField, controlPropsByField);
    List<ContentTypeField> fields = new ArrayList<>();
    List<String> childSets = new ArrayList<>();

    PSFieldSet parentFs = def.getFieldSet();
    if (parentFs != null) {
      addFieldsFromSet(parentFs, null, controlByField, controlPropsByField, fields);
      for (PSFieldSet child : def.getComplexChildren()) {
        if (child != null && StringUtils.isNotBlank(child.getName())) {
          childSets.add(child.getName());
          addFieldsFromSet(child, child.getName(), controlByField, controlPropsByField, fields);
        }
      }
    }
    detail.setFields(fields);
    detail.setChildFieldSets(childSets);

    IPSGuid ctGuid = new PSGuid(PSTypeEnum.NODEDEF, def.getTypeId());
    int defaultWfId = def.getContentEditor() != null ? def.getContentEditor().getWorkflowId() : -1;
    // Always set non-null lists on GET so wire shape stays [] not omitted (NON_NULL include).
    detail.setAllowedWorkflows(loadWorkflows(ctGuid, defaultWfId, def.getContentEditor()));
    if (defaultWfId > 0) {
      detail.setDefaultWorkflow(toWorkflowRef(defaultWfId, true));
    }
    detail.setAllowedTemplates(loadTemplates(ctGuid));

    detail.setDesignGaps(contentTypeDesignGaps(controlsResolved));
    return detail;
  }

  /**
   * Structured designGaps for content-type detail (REST-GAPS-01). Package-visible for unit tests.
   */
  static List<DesignGap> contentTypeDesignGaps(boolean controlsResolved) {
    List<DesignGap> gaps = new ArrayList<>();
    gaps.add(
        DesignGap.of(
              "CT_FIELD_RULE_EXPR",
              "Field rule expressions: GET/PUT .../fields/{fieldName}/ruleExpressions"
                  + " (held lock for write). Detail field rows remain summary strings."
                  + " Apply-when on field validation is read-only. Control property values:"
                  + " GET/PUT .../fields/{fieldName}/controlProperties"));
    gaps.add(
        DesignGap.of(
            "CT_ITEM_EXITS",
            "Item-level exits/validations: GET/PUT /contenttypes/{idOrName}/itemExits"
                + " (held lock for write). Apply-when conditions are read-only"));
    gaps.add(
        DesignGap.of(
            "CT_CREATE_DELETE",
            "Create via POST /services/contenttypes. Rename via PUT /contenttypes/{idOrName}/name"
                + " (held lock). Delete not supported; PUT save requires a held design lock for"
                + " label/description/enabled, field searchable/occurrence, workflows (+ default),"
                + " and templates"));
    gaps.add(
        DesignGap.of(
            "CT_SHARED_FIELD_INCLUSION", "Shared/system field inclusion editing not supported"));
    gaps.add(
        DesignGap.of(
            "CT_WF_TEMPLATE_ASSOC_SEMANTICS",
            "Workflow/template associations: full replace when lists are supplied on PUT; omit"
                + " lists to leave unchanged; empty allowedWorkflows clears associations"));
    gaps.add(
        DesignGap.of(
            "CT_TEMPLATE_ASSOC_SAVE_ORDER",
            "Template association save is a separate design write after content-type save; if it"
                + " fails, meta/field/workflow changes may already be committed"));
    gaps.add(
        DesignGap.of(
            "CT_FIELD_LABELS_WRITE",
            "Field display labels are not writable via PUT content type detail"));
    if (!controlsResolved) {
      gaps.add(
          DesignGap.of(
              "CT_CONTROL_RESOLUTION",
              "Display control/label resolution failed for this content type"));
    }
    return gaps;
  }

  @Override
  public ContentTypeDetail updateContentType(URI baseUri, String idOrName, ContentTypeDetail body) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    requireSessionUserForLock();
    String session = currentSession();
    String user = currentUser();

    try {
      IPSGuid ctGuid = resolveExistingContentTypeGuid(idOrName.trim());
      if (ctGuid == null) {
        return null;
      }
      requireHeldLock(ctGuid);
      List<PSItemDefinition> locked;
      try {
        locked =
            designSvc.loadContentTypes(
                Collections.singletonList(ctGuid), true, false, session, user);
      } catch (PSErrorResultsException e) {
        throwLockOrNotFound(e, idOrName, true);
        return null;
      }
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      PSItemDefinition def = locked.get(0);
      applyMetaUpdates(def, body);
      applyFieldUpdates(def, body.getFields());
      applyWorkflowUpdates(def, body);
      boolean needTemplates = body.getAllowedTemplates() != null;
      try {
        // Keep the held design lock after save; clients release via POST .../unlock.
        // Content-type save and template association save are sequential design writes
        // without a shared rollback — template failure after CT save is partial success.
        designSvc.saveContentTypes(Collections.singletonList(def), false, session, user);
      } catch (PSErrorsException e) {
        log.error("Failed to save content type {}: {}", idOrName, e.getMessage(), e);
        throw new IllegalStateException("Failed to save content type", e);
      }
      if (needTemplates) {
        try {
          List<IPSGuid> templateGuids = resolveTemplateGuids(body.getAllowedTemplates());
          designSvc.saveAssociatedTemplates(ctGuid, templateGuids, false, session, user);
        } catch (PSErrorsException e) {
          log.error(
              "Failed to save template associations for {} (content type already saved): {}",
              idOrName,
              e.getMessage(),
              e);
          throw new IllegalStateException(
              "Content type meta/fields/workflows saved, but template associations failed —"
                  + " retry template update",
              e);
        }
      }
      PSItemDefinition reloaded = reloadItemDef(idOrName.trim());
      return reloaded != null ? toDetail(reloaded) : toDetail(def);
    } catch (IllegalArgumentException | IllegalStateException | WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to update content type {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to update content type", e);
    }
  }

  @Override
  public ObjectLockSummary lockContentType(URI baseUri, String idOrName) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    String session = currentSession();
    String user = currentUser();
    requireSessionUserForLock();
    IPSGuid ctGuid = resolveExistingContentTypeGuid(idOrName.trim());
    if (ctGuid == null) {
      return null;
    }
    try {
      List<PSItemDefinition> locked =
          designSvc.loadContentTypes(Collections.singletonList(ctGuid), true, false, session, user);
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      return toLockSummary(session, user, remainingLockMinutes(ctGuid));
    } catch (PSErrorResultsException e) {
      throwLockOrNotFound(e, idOrName, true);
      return null;
    }
  }

  @Override
  public Boolean unlockContentType(URI baseUri, String idOrName) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    String session = currentSession();
    String user = currentUser();
    requireSessionUserForLock();
    IPSGuid ctGuid = resolveExistingContentTypeGuid(idOrName.trim());
    if (ctGuid == null) {
      return null;
    }
    if (systemDesign == null) {
      throw new IllegalStateException(
          "Could not release content type design session; design service unavailable");
    }
    List<PSObjectSummary> locked;
    try {
      locked = systemDesign.isLocked(Collections.singletonList(ctGuid), user);
    } catch (PSErrorResultsException e) {
      throwLockOrNotFound(e, idOrName, false);
      return null;
    }
    PSObjectSummary summary = locked == null || locked.isEmpty() ? null : locked.get(0);
    if (summary != null && summary.isLocked() && !summary.isLockedBy(user)) {
      PSObjectLockSummary info = summary.getLocked();
      String locker = info != null ? info.getLocker() : null;
      throw new ContentTypeDesignLockException(
          locker != null
              ? "Could not release design lock for content type; locked by " + locker
              : "Could not release design lock for content type");
    }
    systemDesign.releaseLocks(Collections.singletonList(ctGuid), session, user);
    return Boolean.TRUE;
  }

  @Override
  public ContentTypeDetail setContentTypeEnabled(URI baseUri, String idOrName, boolean enabled) {
    requireAdmin();
    requireSessionUserForLock();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    String session = currentSession();
    String user = currentUser();
    try {
      PSItemDefinition current = resolveItemDef(trimmed, true);
      if (current == null) {
        return null;
      }
      IPSGuid ctGuid = new PSGuid(PSTypeEnum.NODEDEF, current.getTypeId());
      requireHeldLock(ctGuid);
      List<PSItemDefinition> locked;
      try {
        locked =
            designSvc.loadContentTypes(
                Collections.singletonList(ctGuid), true, false, session, user);
      } catch (PSErrorResultsException e) {
        throw lockConflict(e, "Could not enable/disable content type");
      }
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      PSItemDefinition def = locked.get(0);
      def.setEnabled(enabled);
      try {
        designSvc.saveContentTypes(Collections.singletonList(def), false, session, user);
      } catch (PSErrorsException e) {
        if (hasLockError(e.getErrors())) {
          throw lockConflict(e, "Could not enable/disable content type");
        }
        log.error("Failed to save content type enabled flag {}: {}", idOrName, e.getMessage(), e);
        throw new IllegalStateException("Failed to save content type enabled flag", e);
      }
      PSItemDefinition reloaded = reloadItemDef(trimmed);
      if (reloaded != null && reloaded != def) {
        reloaded.setEnabled(enabled);
        return toDetail(reloaded);
      }
      return toDetail(def);
    } catch (ContentTypeDesignLockException
        | IllegalArgumentException
        | WebApplicationException e) {
      throw e;
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not found for enable/disable: {}", idOrName);
      return null;
    } catch (Exception e) {
      log.error("Failed to enable/disable content type {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to enable/disable content type", e);
    }
  }

  @Override
  public ContentTypeDetail renameContentType(URI baseUri, String idOrName, String newName) {
    requireAdmin();
    requireSessionUserForLock();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    String validatedName = validateNewContentTypeName(newName);
    String session = currentSession();
    String user = currentUser();
    try {
      PSItemDefinition current = resolveItemDef(trimmed, true);
      if (current == null) {
        return null;
      }
      IPSGuid ctGuid = new PSGuid(PSTypeEnum.NODEDEF, current.getTypeId());
      requireUniqueContentTypeName(validatedName, current.getTypeId());
      requireHeldLock(ctGuid);
      if (validatedName.equals(current.getName())) {
        return toDetail(current);
      }
      List<PSItemDefinition> locked;
      try {
        locked =
            designSvc.loadContentTypes(
                Collections.singletonList(ctGuid), true, false, session, user);
      } catch (PSErrorResultsException e) {
        throw lockConflict(e, "Could not rename content type");
      }
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      PSItemDefinition def = locked.get(0);
      def.setName(validatedName);
      try {
        designSvc.saveContentTypes(Collections.singletonList(def), false, session, user);
      } catch (PSErrorsException e) {
        if (hasLockError(e.getErrors())) {
          throw lockConflict(e, "Could not rename content type");
        }
        log.error("Failed to save content type name {}: {}", idOrName, e.getMessage(), e);
        throw new IllegalStateException("Failed to save content type name", e);
      }
      PSItemDefinition reloaded = reloadItemDef(String.valueOf(def.getTypeId()));
      if (reloaded != null) {
        return toDetail(reloaded);
      }
      return toDetail(def);
    } catch (ContentTypeDesignLockException
        | IllegalArgumentException
        | WebApplicationException e) {
      throw e;
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not found for rename: {}", idOrName);
      return null;
    } catch (Exception e) {
      log.error("Failed to rename content type {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to rename content type", e);
    }
  }

  @Override
  public List<NamedObjectRef> getAllowedTemplates(URI baseUri, String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    try {
      PSItemDefinition def = resolveItemDef(idOrName.trim());
      if (def == null) {
        return null;
      }
      return loadTemplates(new PSGuid(PSTypeEnum.NODEDEF, def.getTypeId()));
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not found for allowed templates: {}", idOrName);
      return null;
    }
  }

  @Override
  public List<NamedObjectRef> replaceAllowedTemplates(
      URI baseUri, String idOrName, List<NamedObjectRef> templates) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (templates == null) {
      throw new IllegalArgumentException("allowedTemplates body is required");
    }
    requireSessionUserForLock();
    String session = currentSession();
    String user = currentUser();
    try {
      IPSGuid ctGuid = resolveExistingContentTypeGuid(idOrName.trim());
      if (ctGuid == null) {
        return null;
      }
      requireHeldLock(ctGuid);
      List<IPSGuid> templateGuids = resolveTemplateGuids(templates);
      try {
        designSvc.saveAssociatedTemplates(ctGuid, templateGuids, false, session, user);
      } catch (PSErrorsException e) {
        if (isNotLockedError(e) || hasLockError(e.getErrors())) {
          throw lockConflict(e, "Could not save template associations");
        }
        log.error(
            "Failed to save template associations for {}: {}", idOrName, e.getMessage(), e);
        throw new IllegalStateException("Failed to save template associations", e);
      }
      return loadTemplates(ctGuid);
    } catch (ContentTypeDesignLockException
        | IllegalArgumentException
        | WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to replace template associations for {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to replace template associations", e);
    }
  }

  @Override
  public ContentTypeDetail setAllowedWorkflows(
      URI baseUri,
      String idOrName,
      List<NamedObjectRef> allowedWorkflows,
      NamedObjectRef defaultWorkflow) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (allowedWorkflows == null) {
      throw new IllegalArgumentException("allowedWorkflows is required");
    }
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    requireSessionUserForLock();
    String session = currentSession();
    String user = currentUser();
    try {
      // Peer setContentTypeEnabled: item-def existence so unknown ids 404 before lock.
      PSItemDefinition current = resolveItemDef(trimmed);
      if (current == null) {
        return null;
      }
      IPSGuid ctGuid = new PSGuid(PSTypeEnum.NODEDEF, current.getTypeId());
      requireHeldLock(ctGuid);
      List<PSItemDefinition> locked;
      try {
        locked =
            designSvc.loadContentTypes(
                Collections.singletonList(ctGuid), true, false, session, user);
      } catch (PSErrorResultsException e) {
        throw lockConflict(e, "Could not update content type workflow associations");
      }
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      PSItemDefinition def = locked.get(0);
      if (def.getContentEditor() == null) {
        throw new IllegalStateException(
            "Could not update content type workflow associations; content editor missing");
      }
      ContentTypeDetail patch = new ContentTypeDetail();
      patch.setAllowedWorkflows(allowedWorkflows);
      patch.setDefaultWorkflow(defaultWorkflow);
      applyWorkflowUpdates(def, patch);
      try {
        designSvc.saveContentTypes(Collections.singletonList(def), false, session, user);
      } catch (PSErrorsException e) {
        if (hasLockError(e.getErrors())) {
          throw lockConflict(e, "Could not update content type workflow associations");
        }
        log.error(
            "Failed to save content type workflow associations {}: {}", idOrName, e.getMessage(), e);
        throw new IllegalStateException("Failed to save content type workflow associations", e);
      }
      // Cache miss after save must not 404 a successful persist (CD-07 peer).
      PSItemDefinition reloaded = reloadItemDef(trimmed);
      return reloaded != null ? toDetail(reloaded) : toDetail(def);
    } catch (ContentTypeDesignLockException
        | IllegalArgumentException
        | WebApplicationException e) {
      throw e;
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not found for workflow associations: {}", idOrName);
      return null;
    } catch (Exception e) {
      log.error(
          "Failed to update content type workflow associations {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to update content type workflow associations", e);
    }
  }

  @Override
  public ContentTypeItemExits getItemExits(URI baseUri, String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    try {
      PSItemDefinition def = resolveItemDef(idOrName.trim());
      if (def == null) {
        return null;
      }
      return toItemExits(def);
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not found for item exits: {}", idOrName);
      return null;
    } catch (Exception e) {
      log.error("Failed to load item exits for {}: {}", idOrName, e.getMessage(), e);
      throw new RuntimeException(
          "Failed to load item-level exits (" + e.getClass().getName() + "): " + e.getMessage(),
          e);
    }
  }

  @Override
  public ContentTypeItemExits replaceItemExits(
      URI baseUri, String idOrName, ContentTypeItemExits body) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (body == null
        || body.getInputTranslations() == null
        || body.getOutputTranslations() == null
        || body.getValidations() == null) {
      throw new IllegalArgumentException(
          "inputTranslations, outputTranslations, and validations are required");
    }
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    requireSessionUserForLock();
    String session = currentSession();
    String user = currentUser();
    try {
      IPSGuid ctGuid = resolveExistingContentTypeGuid(trimmed);
      if (ctGuid == null) {
        return null;
      }
      requireHeldLock(ctGuid);
      List<PSItemDefinition> locked;
      try {
        locked =
            designSvc.loadContentTypes(
                Collections.singletonList(ctGuid), true, false, session, user);
      } catch (PSErrorResultsException e) {
        throw lockConflict(e, "Could not update content type item-level exits");
      }
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      PSItemDefinition def = locked.get(0);
      applyItemExits(def, body);
      try {
        designSvc.saveContentTypes(Collections.singletonList(def), false, session, user);
      } catch (PSErrorsException e) {
        if (hasLockError(e.getErrors())) {
          throw lockConflict(e, "Could not update content type item-level exits");
        }
        String detail = formatSaveErrors(e);
        log.error(
            "Failed to save content type item-level exits {}: {}", idOrName, detail, e);
        for (Object err : e.getErrors().values()) {
          if (err instanceof PSErrorException pe && StringUtils.isNotBlank(pe.getStack())) {
            log.error("saveContentTypes error map stack: {}", pe.getStack());
          }
        }
        String message = "Failed to save content type item-level exits: " + detail;
        if (isValidationSaveFailure(e)) {
          throw new IllegalArgumentException(message, e);
        }
        throw new IllegalStateException(message, e);
      }
      PSItemDefinition reloaded = reloadItemDef(trimmed);
      return reloaded != null ? toItemExits(reloaded) : toItemExits(def);
    } catch (IllegalStateException | IllegalArgumentException | WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to replace item-level exits for {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to replace content type item-level exits", e);
    }
  }

  @Override
  public ContentTypeFieldControlProperties getFieldControlProperties(
      URI baseUri, String idOrName, String fieldName) {
    if (StringUtils.isBlank(idOrName) || StringUtils.isBlank(fieldName)) {
      return null;
    }
    try {
      PSItemDefinition def = resolveItemDef(idOrName.trim());
      if (def == null) {
        return null;
      }
      return loadFieldControlProperties(def, fieldName.trim());
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not found for control properties: {}", idOrName);
      return null;
    }
  }

  @Override
  public ContentTypeFieldControlProperties replaceFieldControlProperties(
      URI baseUri, String idOrName, String fieldName, ContentTypeFieldControlProperties body) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (StringUtils.isBlank(fieldName)) {
      throw new IllegalArgumentException("fieldName is required");
    }
    if (body == null || body.getProperties() == null) {
      throw new IllegalArgumentException("properties is required");
    }
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    String field = fieldName.trim();
    String session = currentSession();
    String user = currentUser();
    requireSessionUserForLock();
    try {
      IPSGuid ctGuid = resolveExistingContentTypeGuid(trimmed);
      if (ctGuid == null) {
        return null;
      }
      requireHeldLock(ctGuid);
      List<PSItemDefinition> locked;
      try {
        locked =
            designSvc.loadContentTypes(
                Collections.singletonList(ctGuid), true, false, session, user);
      } catch (PSErrorResultsException e) {
        throw lockConflict(e, "Could not save control properties");
      }
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      PSItemDefinition def = locked.get(0);
      PSDisplayMapping mapping = requireFieldMapping(def, field);
      applyControlPropertyUpdates(mapping, body);
      try {
        designSvc.saveContentTypes(Collections.singletonList(def), false, session, user);
      } catch (PSErrorsException e) {
        if (hasLockError(e.getErrors())) {
          throw lockConflict(e, "Could not save control properties");
        }
        log.error("Failed to save control properties for {}: {}", idOrName, e.getMessage(), e);
        throw new IllegalStateException("Failed to save control properties", e);
      }
      PSItemDefinition reloaded = reloadItemDef(trimmed);
      return loadFieldControlProperties(reloaded != null ? reloaded : def, field);
    } catch (ContentTypeDesignLockException
        | IllegalArgumentException
        | WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to replace control properties for {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to replace control properties", e);
    }
  }

  @Override
  public ContentTypeFieldRuleExpressions getFieldRuleExpressions(
      URI baseUri, String idOrName, String fieldName) {
    if (StringUtils.isBlank(idOrName) || StringUtils.isBlank(fieldName)) {
      return null;
    }
    try {
      PSItemDefinition def = resolveItemDef(idOrName.trim());
      if (def == null) {
        return null;
      }
      return loadFieldRuleExpressions(def, fieldName.trim(), true);
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not found for field rule expressions: {}", idOrName);
      return null;
    }
  }

  @Override
  public ContentTypeFieldRuleExpressions replaceFieldRuleExpressions(
      URI baseUri, String idOrName, String fieldName, ContentTypeFieldRuleExpressions body) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (StringUtils.isBlank(fieldName)) {
      throw new IllegalArgumentException("fieldName is required");
    }
    if (body == null
        || body.getValidation() == null
        || body.getVisibility() == null
        || body.getInputTranslation() == null
        || body.getOutputTranslation() == null) {
      throw new IllegalArgumentException(
          "validation, visibility, inputTranslation, and outputTranslation are required");
    }
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    String field = fieldName.trim();
    String session = currentSession();
    String user = currentUser();
    requireSessionUserForLock();
    try {
      IPSGuid ctGuid = resolveExistingContentTypeGuid(trimmed);
      if (ctGuid == null) {
        return null;
      }
      requireHeldLock(ctGuid);
      List<PSItemDefinition> locked;
      try {
        locked =
            designSvc.loadContentTypes(
                Collections.singletonList(ctGuid), true, false, session, user);
      } catch (PSErrorResultsException e) {
        throw lockConflict(e, "Could not save field rule expressions");
      }
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      PSItemDefinition def = locked.get(0);
      PSField target = requireField(def, field, true);
      applyFieldRuleExpressions(target, body);
      try {
        designSvc.saveContentTypes(Collections.singletonList(def), false, session, user);
      } catch (PSErrorsException e) {
        if (hasLockError(e.getErrors())) {
          throw lockConflict(e, "Could not save field rule expressions");
        }
        log.error("Failed to save field rule expressions for {}: {}", idOrName, e.getMessage(), e);
        throw new IllegalStateException("Failed to save field rule expressions", e);
      }
      PSItemDefinition reloaded = reloadItemDef(trimmed);
      PSField after = reloaded != null ? findField(reloaded, field) : null;
      if (after == null) {
        after = target;
      }
      return toFieldRuleExpressions(field, after);
    } catch (ContentTypeDesignLockException
        | IllegalArgumentException
        | WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to replace field rule expressions for {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to replace field rule expressions", e);
    }
  }

  /**
   * Apply default workflow id and/or allowed-workflow inclusion list.
   *
   * <p>{@code allowedWorkflows == null} leaves associations unchanged. Non-null list is a full
   * replace — empty list clears associations (does not re-inject the previous default). When both
   * {@code defaultWorkflow} and a non-empty allowed list are supplied, the default is ensured to
   * appear in the inclusion list.
   */
  private void applyWorkflowUpdates(PSItemDefinition def, ContentTypeDetail body) {
    if (body.getDefaultWorkflow() != null) {
      int wfId = resolveWorkflowUuid(body.getDefaultWorkflow(), "defaultWorkflow");
      def.getContentEditor().setWorkflowId(wfId);
    }
    if (body.getAllowedWorkflows() == null) {
      return;
    }
    List<Integer> wfIds = new ArrayList<>();
    int i = 0;
    for (NamedObjectRef ref : body.getAllowedWorkflows()) {
      if (ref == null) {
        throw new IllegalArgumentException("allowedWorkflows[" + i + "] is null");
      }
      int id = resolveWorkflowUuid(ref, "allowedWorkflows[" + i + "]");
      if (!wfIds.contains(id)) {
        wfIds.add(id);
      }
      i++;
    }
    // Only force-include default when the client also set defaultWorkflow this request.
    // Empty list = true clear (do not re-inject prior default).
    if (body.getDefaultWorkflow() != null && !wfIds.isEmpty()) {
      int defaultId = def.getContentEditor().getWorkflowId();
      if (defaultId > 0 && !wfIds.contains(defaultId)) {
        wfIds.add(defaultId);
      }
    } else if (!wfIds.isEmpty()) {
      // Non-empty replace without explicit default: retarget default if orphaned
      int defaultId = def.getContentEditor().getWorkflowId();
      if (defaultId <= 0 || !wfIds.contains(defaultId)) {
        def.getContentEditor().setWorkflowId(wfIds.get(0));
      }
    }
    def.getContentEditor()
        .setWorkflowInfo(
            new PSWorkflowInfo(PSWorkflowInfo.TYPE_INCLUSIONARY, new ArrayList<>(wfIds)));
  }

  private int resolveWorkflowUuid(NamedObjectRef ref, String field) {
    if (ref.getGuid() != null) {
      int fromGuid = uuidFromRestGuid(ref.getGuid(), PSTypeEnum.WORKFLOW, field);
      if (fromGuid > 0) {
        requireWorkflowExists(fromGuid, field);
        return fromGuid;
      }
    }
    if (StringUtils.isNotBlank(ref.getName())) {
      IPSWorkflowService wfSvc =
          workflowService != null ? workflowService : PSWorkflowServiceLocator.getWorkflowService();
      List<PSWorkflow> found = wfSvc.findWorkflowsByName(ref.getName().trim());
      if (found == null || found.isEmpty()) {
        throw new IllegalArgumentException(field + " workflow not found: " + ref.getName());
      }
      return found.get(0).getGUID().getUUID();
    }
    throw new IllegalArgumentException(field + " requires name or guid");
  }

  private void requireWorkflowExists(int workflowUuid, String field) {
    IPSWorkflowService wfSvc =
        workflowService != null ? workflowService : PSWorkflowServiceLocator.getWorkflowService();
    IPSGuid g = new PSGuid(PSTypeEnum.WORKFLOW, workflowUuid);
    if (wfSvc.findWorkflow(g).isEmpty()) {
      throw new IllegalArgumentException(field + " workflow not found: " + workflowUuid);
    }
  }

  List<IPSGuid> resolveTemplateGuids(List<NamedObjectRef> refs) {
    List<IPSGuid> out = new ArrayList<>();
    if (refs == null) {
      return out;
    }
    int i = 0;
    for (NamedObjectRef ref : refs) {
      if (ref == null) {
        throw new IllegalArgumentException("allowedTemplates[" + i + "] is null");
      }
      out.add(resolveTemplateGuid(ref, "allowedTemplates[" + i + "]"));
      i++;
    }
    return out;
  }

  private IPSGuid resolveTemplateGuid(NamedObjectRef ref, String field) {
    IPSAssemblyService asm =
        assemblyService != null
            ? assemblyService
            : PSAssemblyServiceLocator.getAssemblyService();
    if (ref.getGuid() != null) {
      String sv = ref.getGuid().getStringValue();
      if (StringUtils.isNotBlank(sv)) {
        try {
          IPSGuid g = ApiUtils.convertGuid(ref.getGuid());
          // Normalize type if untyped numeric guid
          if (g.getType() == 0) {
            g = new PSGuid(PSTypeEnum.TEMPLATE, g.getUUID());
          }
          try {
            asm.loadTemplate(g, false);
          } catch (PSAssemblyException e) {
            throw new IllegalArgumentException(field + " template not found: " + sv, e);
          }
          return g;
        } catch (IllegalArgumentException e) {
          throw e;
        } catch (Exception e) {
          throw new IllegalArgumentException(field + " invalid template guid: " + sv, e);
        }
      }
      int uuid = ref.getGuid().getUuid();
      if (uuid > 0) {
        IPSGuid g = new PSGuid(PSTypeEnum.TEMPLATE, uuid);
        try {
          asm.loadTemplate(g, false);
        } catch (PSAssemblyException e) {
          throw new IllegalArgumentException(field + " template not found uuid=" + uuid, e);
        }
        return g;
      }
    }
    if (StringUtils.isNotBlank(ref.getName())) {
      try {
        IPSAssemblyTemplate t = asm.findTemplateByName(ref.getName().trim());
        if (t == null || t.getGUID() == null) {
          throw new IllegalArgumentException(field + " template not found: " + ref.getName());
        }
        return t.getGUID();
      } catch (PSAssemblyException e) {
        throw new IllegalArgumentException(field + " template not found: " + ref.getName(), e);
      }
    }
    throw new IllegalArgumentException(field + " requires name or guid");
  }

  /**
   * Prefer stringValue guid form, then uuid field. Returns 0 when guid cannot contribute a positive
   * uuid (caller may fall through to name).
   */
  private int uuidFromRestGuid(Guid guid, PSTypeEnum expectedType, String field) {
    if (guid == null) {
      return 0;
    }
    String sv = guid.getStringValue();
    if (StringUtils.isNotBlank(sv)) {
      try {
        IPSGuid g = ApiUtils.convertGuid(guid);
        if (g.getType() == 0) {
          g = new PSGuid(expectedType, g.getUUID());
        }
        return g.getUUID();
      } catch (Exception e) {
        throw new IllegalArgumentException(field + " invalid guid: " + sv, e);
      }
    }
    int uuid = guid.getUuid();
    return uuid > 0 ? uuid : 0;
  }

  private void applyMetaUpdates(PSItemDefinition def, ContentTypeDetail body) {
    // Name is not applied here; CD-01 rename uses renameContentType / PUT .../name.
    if (body.getLabel() != null) {
      def.setLabel(body.getLabel());
    }
    if (body.getDescription() != null) {
      def.setDescription(body.getDescription());
    }
    if (body.getEnabled() != null) {
      def.setEnabled(body.getEnabled());
    }
  }

  /**
   * Apply writable field patches only ({@code searchable}, occurrence / required). Rule
   * expressions use {@link #replaceFieldRuleExpressions}; control property names/values use
   * {@link #replaceFieldControlProperties}. Field labels on the wire DTO are ignored.
   */
  private void applyFieldUpdates(PSItemDefinition def, List<ContentTypeField> fields) {
    if (fields == null || fields.isEmpty()) {
      return;
    }
    PSFieldSet parentFs = def.getFieldSet();
    if (parentFs == null) {
      return;
    }
    for (ContentTypeField patch : fields) {
      if (patch == null || StringUtils.isBlank(patch.getName())) {
        continue;
      }
      // systemModOnly=false includes classic + extended fields
      PSField field = parentFs.findFieldByName(patch.getName(), false);
      if (field == null) {
        // also search complex children
        for (PSFieldSet child : def.getComplexChildren()) {
          if (child == null) continue;
          field = child.findFieldByName(patch.getName(), false);
          if (field != null) break;
        }
      }
      if (field == null) {
        throw new IllegalArgumentException("Unknown field: " + patch.getName());
      }
      if (patch.getSearchable() != null) {
        field.setUserSearchable(patch.getSearchable());
      }
      if (StringUtils.isNotBlank(patch.getOccurrence())) {
        Integer dim = occurrenceFromApi(patch.getOccurrence());
        if (dim == null) {
          throw new IllegalArgumentException(
              "Invalid occurrence for field " + patch.getName() + ": " + patch.getOccurrence());
        }
        try {
          field.setOccurrenceDimension(dim, null);
        } catch (PSSystemValidationException e) {
          throw new IllegalArgumentException(
              "Invalid occurrence for field " + patch.getName() + ": " + patch.getOccurrence(), e);
        }
      } else if (patch.getRequired() != null) {
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
  }

  /** Map API occurrence string back to PSField dimension, or null if unknown. */
  static Integer occurrenceFromApi(String occurrence) {
    if (occurrence == null) return null;
    return switch (occurrence) {
      case "optional" -> PSField.OCCURRENCE_DIMENSION_OPTIONAL;
      case "required" -> PSField.OCCURRENCE_DIMENSION_REQUIRED;
      case "oneOrMore" -> PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE;
      case "zeroOrMore" -> PSField.OCCURRENCE_DIMENSION_ZERO_OR_MORE;
      case "count" -> PSField.OCCURRENCE_DIMENSION_COUNT;
      default -> null;
    };
  }

  private List<NamedObjectRef> loadWorkflows(
      IPSGuid ctGuid, int defaultWfId, PSContentEditor editor) {
    List<NamedObjectRef> out = new ArrayList<>();
    try {
      IPSContentMgr mgr = PSContentMgrLocator.getContentMgr();
      List<PSContentTypeWorkflow> rels = mgr.findContentTypeWorkflowAssociations(ctGuid);
      if (rels != null) {
        for (PSContentTypeWorkflow rel : rels) {
          if (rel == null || rel.getWorkflowId() == null) continue;
          // IPSGuid.getUUID() is the 32-bit object id (int); do not cast longValue()
          int wfUuid = rel.getWorkflowId().getUUID();
          out.add(toWorkflowRef(wfUuid, wfUuid == defaultWfId));
        }
      }
    } catch (Exception e) {
      log.debug("Could not load workflows for content type {}: {}", ctGuid, e.getMessage());
    }
    if (out.isEmpty() && editor != null && editor.getWorkflowInfo() != null) {
      Iterator<Integer> values = editor.getWorkflowInfo().getValues();
      if (values != null) {
        while (values.hasNext()) {
          Integer id = values.next();
          if (id != null && id > 0) {
            out.add(toWorkflowRef(id, id == defaultWfId));
          }
        }
      }
    }
    out.sort(
        Comparator.comparing(
            r -> r.getLabel() != null ? r.getLabel() : "", String.CASE_INSENSITIVE_ORDER));
    return out;
  }

  private NamedObjectRef toWorkflowRef(int workflowUuid, boolean isDefault) {
    NamedObjectRef ref = new NamedObjectRef();
    IPSGuid g = new PSGuid(PSTypeEnum.WORKFLOW, workflowUuid);
    ref.setGuid(ApiUtils.convertGuid(g));
    ref.setIsDefault(isDefault);
    try {
      IPSWorkflowService wfSvc =
          workflowService != null ? workflowService : PSWorkflowServiceLocator.getWorkflowService();
      PSWorkflow wf = wfSvc.findWorkflow(g).orElse(null);
      if (wf != null) {
        ref.setName(wf.getName());
        ref.setLabel(StringUtils.defaultIfBlank(wf.getLabel(), wf.getName()));
      } else {
        ref.setName(String.valueOf(workflowUuid));
        ref.setLabel(String.valueOf(workflowUuid));
      }
    } catch (Exception e) {
      ref.setName(String.valueOf(workflowUuid));
      ref.setLabel(String.valueOf(workflowUuid));
    }
    return ref;
  }

  private List<NamedObjectRef> loadTemplates(IPSGuid ctGuid) {
    List<NamedObjectRef> out = new ArrayList<>();
    try {
      IPSAssemblyService asm =
          assemblyService != null
              ? assemblyService
              : PSAssemblyServiceLocator.getAssemblyService();
      List<IPSAssemblyTemplate> templates = asm.findTemplatesByContentType(ctGuid);
      if (templates != null) {
        for (IPSAssemblyTemplate t : templates) {
          if (t == null) continue;
          NamedObjectRef ref = new NamedObjectRef();
          try {
            if (t.getGUID() != null) {
              ref.setGuid(ApiUtils.convertGuid(t.getGUID()));
            }
          } catch (Exception ignore) {
            // optional
          }
          ref.setName(t.getName());
          ref.setLabel(StringUtils.defaultIfBlank(t.getLabel(), t.getName()));
          out.add(ref);
        }
      }
    } catch (Exception e) {
      log.debug("Could not load templates for content type {}: {}", ctGuid, e.getMessage());
    }
    out.sort(
        Comparator.comparing(
            r -> r.getLabel() != null ? r.getLabel() : "", String.CASE_INSENSITIVE_ORDER));
    return out;
  }

  private void addFieldsFromSet(
      PSFieldSet fieldSet,
      String fieldSetName,
      Map<String, String> controlByField,
      Map<String, List<ContentTypeControlProperty>> controlPropsByField,
      List<ContentTypeField> out) {
    if (fieldSet == null) {
      return;
    }
    PSField[] all = fieldSet.getAllFields();
    if (all == null) {
      return;
    }
    for (PSField field : all) {
      if (field == null || StringUtils.isBlank(field.getSubmitName())) {
        continue;
      }
      ContentTypeField f = new ContentTypeField();
      f.setName(field.getSubmitName());
      f.setDataType(field.getDataType());
      f.setFieldType(mapFieldOrigin(field.getType()));
      f.setSearchable(field.isUserSearchable());
      f.setFieldSet(fieldSetName);
      f.setControl(controlByField.get(field.getSubmitName()));
      // label: fall back to name when display mapping label not resolved
      f.setLabel(
          controlByField.containsKey(field.getSubmitName() + ":label")
              ? controlByField.get(field.getSubmitName() + ":label")
              : field.getSubmitName());
      int occurrence = field.getOccurrenceDimension(null);
      f.setRequired(
          occurrence == PSField.OCCURRENCE_DIMENSION_REQUIRED
              || occurrence == PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE);
      f.setOccurrence(mapOccurrence(occurrence));
      f.setReadOnly(field.isReadOnly());
      f.setHasValidation(field.hasValidationRules());
      f.setHasVisibilityRules(
          field.getVisibilityRules() != null && !field.getVisibilityRules().isEmpty());
      f.setHasInputTranslation(hasTranslation(field.getInputTranslation()));
      f.setHasOutputTranslation(hasTranslation(field.getOutputTranslation()));
      f.setValidationExpression(summarizeValidationRules(field.getValidationRules()));
      f.setVisibilityExpression(summarizeVisibilityRules(field.getVisibilityRules()));
      f.setInputTranslationExpression(summarizeTranslation(field.getInputTranslation()));
      f.setOutputTranslationExpression(summarizeTranslation(field.getOutputTranslation()));
      List<ContentTypeControlProperty> props = controlPropsByField.get(field.getSubmitName());
      if (props != null && !props.isEmpty()) {
        f.setControlProperties(List.copyOf(props));
        List<String> names = new ArrayList<>(props.size());
        for (ContentTypeControlProperty p : props) {
          if (p != null && StringUtils.isNotBlank(p.getName())) {
            names.add(p.getName());
          }
        }
        f.setControlPropertyNames(names);
      }
      out.add(f);
    }
  }

  private static String mapFieldOrigin(int type) {
    return switch (type) {
      case PSField.TYPE_SYSTEM -> "system";
      case PSField.TYPE_SHARED -> "shared";
      case PSField.TYPE_LOCAL -> "local";
      default -> "unknown";
    };
  }

  /** Package-visible for unit tests. Maps PSField occurrence dimension to API string. */
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

  /** Package-visible for unit tests. True when a field translation has a non-empty call set. */
  static boolean hasTranslation(PSFieldTranslation translation) {
    return translation != null
        && translation.getTranslations() != null
        && !translation.getTranslations().isEmpty();
  }

  /**
   * Package-visible for unit tests. Summarizes validation rules as a human-readable expression
   * string, or {@code null} when empty.
   */
  static String summarizeValidationRules(PSFieldValidationRules rules) {
    if (rules == null) {
      return null;
    }
    List<String> parts = new ArrayList<>();
    for (Iterator<?> it = rules.getRules(); it.hasNext(); ) {
      Object o = it.next();
      if (o instanceof PSRule rule) {
        String s = summarizeRule(rule);
        if (StringUtils.isNotBlank(s)) {
          parts.add(s);
        }
      }
    }
    for (Iterator<?> it = rules.getRuleReferences(); it.hasNext(); ) {
      Object ref = it.next();
      if (ref != null && StringUtils.isNotBlank(ref.toString())) {
        parts.add("ref:" + ref.toString().trim());
      }
    }
    if (parts.isEmpty()) {
      return null;
    }
    return String.join("; ", parts);
  }

  /**
   * Package-visible for unit tests. Summarizes visibility rules, or {@code null} when empty.
   */
  static String summarizeVisibilityRules(PSVisibilityRules rules) {
    if (rules == null || rules.isEmpty()) {
      return null;
    }
    List<String> parts = new ArrayList<>();
    for (Object o : rules) {
      if (o instanceof PSRule rule) {
        String s = summarizeRule(rule);
        if (StringUtils.isNotBlank(s)) {
          parts.add(s);
        }
      }
    }
    if (parts.isEmpty()) {
      return null;
    }
    return String.join("; ", parts);
  }

  /**
   * Package-visible for unit tests. Summarizes translation extension calls, or {@code null} when
   * empty.
   */
  static String summarizeTranslation(PSFieldTranslation translation) {
    if (!hasTranslation(translation)) {
      return null;
    }
    return summarizeExtensionCalls(translation.getTranslations());
  }

  /**
   * Package-visible for unit tests. Summarizes a single design rule (conditionals or extension
   * set).
   */
  static String summarizeRule(PSRule rule) {
    if (rule == null) {
      return null;
    }
    if (rule.isExtensionSetRule()) {
      return summarizeExtensionCalls(rule.getExtensionRules());
    }
    List<String> conds = new ArrayList<>();
    for (Iterator<?> it = rule.getConditionalRules(); it.hasNext(); ) {
      Object o = it.next();
      if (o instanceof PSConditional conditional) {
        String text = conditional.toString();
        if (StringUtils.isNotBlank(text)) {
          conds.add(text.trim());
        }
      }
    }
    if (conds.isEmpty()) {
      return null;
    }
    return String.join(" ", conds);
  }

  /** Package-visible for unit tests. Summarizes extension calls as {@code name(args)}. */
  static String summarizeExtensionCalls(PSExtensionCallSet callSet) {
    if (callSet == null || callSet.isEmpty()) {
      return null;
    }
    List<String> calls = new ArrayList<>();
    for (Object o : callSet) {
      if (o instanceof PSExtensionCall call) {
        String text = call.toString();
        if (StringUtils.isNotBlank(text)) {
          calls.add(text.trim());
        }
      }
    }
    if (calls.isEmpty()) {
      return null;
    }
    return String.join("; ", calls);
  }

  /** Structured gaps for the item-exits envelope (apply-when write). Package-visible for tests. */
  static List<DesignGap> itemExitDesignGaps() {
    return List.of(
        DesignGap.of(
            "CT_ITEM_EXIT_CONDITIONS",
            "Apply-when conditions on item-level exits are read-only; PUT replaces extension"
                + " calls and literal parameters only"));
  }

  /** Package-visible for unit tests. Maps item def exits onto the CD-09 envelope. */
  static ContentTypeItemExits toItemExits(PSItemDefinition def) {
    ContentTypeItemExits out = new ContentTypeItemExits();
    out.setDesignGaps(itemExitDesignGaps());
    PSContentEditor editor = def != null ? def.getContentEditor() : null;
    if (editor == null) {
      out.setInputTranslations(List.of());
      out.setOutputTranslations(List.of());
      out.setValidations(List.of());
      out.setPreExits(List.of());
      out.setPostExits(List.of());
      out.setMaxErrorsToStopValidation(10);
      return out;
    }
    out.setInputTranslations(mapConditionalExits(editor.getInputTranslations()));
    out.setOutputTranslations(mapConditionalExits(editor.getOutputTranslations()));
    out.setValidations(mapConditionalExits(editor.getValidationRules()));
    out.setMaxErrorsToStopValidation(editor.getMaxErrorsToStopValidation());
    PSPipe pipe = editor.getPipe();
    out.setPreExits(
        pipe != null ? mapExtensionCalls(pipe.getInputDataExtensions()) : List.of());
    out.setPostExits(
        pipe != null ? mapExtensionCalls(pipe.getResultDataExtensions()) : List.of());
    return out;
  }

  /** Package-visible for unit tests. One DTO per extension call in each conditional exit. */
  static List<ContentTypeItemExit> mapConditionalExits(Iterator<?> exits) {
    List<ContentTypeItemExit> out = new ArrayList<>();
    if (exits == null) {
      return out;
    }
    while (exits.hasNext()) {
      Object o = exits.next();
      if (!(o instanceof PSConditionalExit conditional)) {
        continue;
      }
      String condition = summarizeApplyWhen(conditional.getCondition());
      Integer maxErrors = conditional.getMaxErrorsToStop();
      PSExtensionCallSet calls = conditional.getRules();
      if (calls == null || calls.isEmpty()) {
        continue;
      }
      for (Object callObj : calls) {
        if (callObj instanceof PSExtensionCall call) {
          ContentTypeItemExit dto = toExitDto(call);
          dto.setCondition(condition);
          dto.setMaxErrorsToStop(maxErrors);
          out.add(dto);
        }
      }
    }
    return out;
  }

  /** Package-visible for unit tests. Maps a raw pipe extension-call set. */
  static List<ContentTypeItemExit> mapExtensionCalls(PSExtensionCallSet callSet) {
    List<ContentTypeItemExit> out = new ArrayList<>();
    if (callSet == null || callSet.isEmpty()) {
      return out;
    }
    for (Object o : callSet) {
      if (o instanceof PSExtensionCall call) {
        out.add(toExitDto(call));
      }
    }
    return out;
  }

  static ContentTypeItemExit toExitDto(PSExtensionCall call) {
    ContentTypeItemExit dto = new ContentTypeItemExit();
    if (call.getExtensionRef() != null) {
      dto.setExtension(call.getExtensionRef().getFQN());
      dto.setName(call.getExtensionRef().getExtensionName());
    }
    List<ContentTypeItemExitParam> params = new ArrayList<>();
    PSExtensionParamValue[] values = call.getParamValues();
    if (values != null) {
      for (PSExtensionParamValue value : values) {
        String text = null;
        if (value != null && value.getValue() != null) {
          text = value.getValue().getValueDisplayText();
        }
        params.add(new ContentTypeItemExitParam(null, text != null ? text : ""));
      }
    }
    dto.setParameters(params);
    String summary = call.toString();
    dto.setSummary(StringUtils.isNotBlank(summary) ? summary.trim() : null);
    return dto;
  }

  /** Package-visible for unit tests. Summarizes apply-when rules, or {@code null} when empty. */
  static String summarizeApplyWhen(PSApplyWhen when) {
    if (when == null || when.isEmpty()) {
      return null;
    }
    List<String> parts = new ArrayList<>();
    for (Object o : when) {
      if (o instanceof PSRule rule) {
        String s = summarizeRule(rule);
        if (StringUtils.isNotBlank(s)) {
          parts.add(s);
        }
      }
    }
    if (parts.isEmpty()) {
      return null;
    }
    return String.join("; ", parts);
  }

  private void applyItemExits(PSItemDefinition def, ContentTypeItemExits body) {
    PSContentEditor editor = def.getContentEditor();
    if (editor == null) {
      throw new IllegalStateException(
          "Could not update content type item-level exits; content editor missing");
    }
    List<PSConditionalExit> existingInput =
        copyConditionalExits(editor.getInputTranslations());
    List<PSConditionalExit> existingOutput =
        copyConditionalExits(editor.getOutputTranslations());
    List<PSConditionalExit> existingValidations =
        copyConditionalExits(editor.getValidationRules());
    editor.setInputTranslation(
        toInputTranslations(body.getInputTranslations(), existingInput, "inputTranslations"));
    editor.setOutputTranslation(
        toOutputTranslations(
            body.getOutputTranslations(), existingOutput, "outputTranslations"));
    PSValidationRules validations =
        toValidationRules(body.getValidations(), existingValidations, "validations");
    if (body.getMaxErrorsToStopValidation() != null) {
      int max = body.getMaxErrorsToStopValidation();
      if (max <= 0) {
        throw new IllegalArgumentException("maxErrorsToStopValidation must be greater than 0");
      }
      validations.setMaxErrorsToStop(max);
    }
    editor.setValidationRules(validations);
    if (body.getPreExits() != null || body.getPostExits() != null) {
      PSPipe pipe = editor.getPipe();
      if (pipe == null) {
        boolean hasPre = body.getPreExits() != null && !body.getPreExits().isEmpty();
        boolean hasPost = body.getPostExits() != null && !body.getPostExits().isEmpty();
        if (hasPre || hasPost) {
          throw new IllegalStateException(
              "Could not update pipe pre/post exits; content editor pipe missing");
        }
      } else {
        if (body.getPreExits() != null) {
          setPipeInputDataExtensions(pipe, toCallSet(body.getPreExits(), "preExits"));
        }
        if (body.getPostExits() != null) {
          pipe.setResultDataExtensions(toCallSet(body.getPostExits(), "postExits"));
        }
      }
    }
  }

  /**
   * Content-editor pipes throw {@link UnsupportedOperationException} from {@link
   * PSPipe#setInputDataExtensions}; use the CE-specific setter so percPage PUT can omit-or-replace
   * pre-exits without SAVE_FAILED / UOE.
   */
  static void setPipeInputDataExtensions(PSPipe pipe, PSExtensionCallSet calls) {
    if (pipe instanceof PSContentEditorPipe cePipe) {
      cePipe.setContentEditorInputDataExtensions(calls);
    } else {
      pipe.setInputDataExtensions(calls);
    }
  }

  @SuppressWarnings("unchecked")
  private static PSInputTranslations toInputTranslations(
      List<ContentTypeItemExit> items, List<PSConditionalExit> existing, String field) {
    PSInputTranslations col = new PSInputTranslations();
    int i = 0;
    for (ContentTypeItemExit item : items) {
      col.add(reuseOrCreateConditionalExit(item, existing, field + "[" + i + "]"));
      i++;
    }
    return col;
  }

  @SuppressWarnings("unchecked")
  private static PSOutputTranslations toOutputTranslations(
      List<ContentTypeItemExit> items, List<PSConditionalExit> existing, String field) {
    PSOutputTranslations col = new PSOutputTranslations();
    int i = 0;
    for (ContentTypeItemExit item : items) {
      col.add(reuseOrCreateConditionalExit(item, existing, field + "[" + i + "]"));
      i++;
    }
    return col;
  }

  @SuppressWarnings("unchecked")
  private static PSValidationRules toValidationRules(
      List<ContentTypeItemExit> items, List<PSConditionalExit> existing, String field) {
    PSValidationRules col = new PSValidationRules();
    int i = 0;
    for (ContentTypeItemExit item : items) {
      col.add(reuseOrCreateConditionalExit(item, existing, field + "[" + i + "]"));
      i++;
    }
    return col;
  }

  /**
   * Keep the original {@link PSConditionalExit} (apply-when, extra rules, ids, param value types)
   * when GET→PUT reconstructs a matching row. Match is the first extension-ref FQN plus ordered
   * param display texts, regardless of how many rules the original has. New FQN/param rows are
   * created from the DTO.
   */
  static PSConditionalExit reuseOrCreateConditionalExit(
      ContentTypeItemExit item, List<PSConditionalExit> existing, String field) {
    PSConditionalExit created = toConditionalExit(item, field);
    if (existing == null || existing.isEmpty()) {
      return created;
    }
    PSExtensionCall createdCall = firstCall(created);
    if (createdCall == null) {
      return created;
    }
    for (int i = 0; i < existing.size(); i++) {
      PSConditionalExit orig = existing.get(i);
      if (orig == null) {
        continue;
      }
      if (sameExitCall(createdCall, firstCall(orig))) {
        existing.remove(i);
        PSConditionalExit clone = (PSConditionalExit) orig.clone();
        if (item != null && item.getMaxErrorsToStop() != null) {
          int max = item.getMaxErrorsToStop();
          if (max <= 0) {
            throw new IllegalArgumentException(field + ".maxErrorsToStop must be greater than 0");
          }
          clone.setMaxErrorsToStop(max);
        }
        return clone;
      }
    }
    return created;
  }

  static List<PSConditionalExit> copyConditionalExits(Iterator<?> exits) {
    List<PSConditionalExit> out = new ArrayList<>();
    if (exits == null) {
      return out;
    }
    while (exits.hasNext()) {
      Object o = exits.next();
      if (o instanceof PSConditionalExit conditional) {
        out.add(conditional);
      }
    }
    return out;
  }

  static PSExtensionCall firstCall(PSConditionalExit exit) {
    if (exit == null || exit.getRules() == null || exit.getRules().isEmpty()) {
      return null;
    }
    Object o = exit.getRules().get(0);
    return o instanceof PSExtensionCall call ? call : null;
  }

  /**
   * Structural equality of an extension call: FQN plus ordered param display texts. Used instead of
   * a delimiter-joined string so a NUL (or any other character) inside a param cannot collide with
   * a split between params.
   */
  static boolean sameExitCall(PSExtensionCall left, PSExtensionCall right) {
    if (left == right) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    if (left.getExtensionRef() == null || right.getExtensionRef() == null) {
      return false;
    }
    if (!Objects.equals(left.getExtensionRef().getFQN(), right.getExtensionRef().getFQN())) {
      return false;
    }
    PSExtensionParamValue[] leftValues = left.getParamValues();
    PSExtensionParamValue[] rightValues = right.getParamValues();
    int leftLen = leftValues == null ? 0 : leftValues.length;
    int rightLen = rightValues == null ? 0 : rightValues.length;
    if (leftLen != rightLen) {
      return false;
    }
    for (int i = 0; i < leftLen; i++) {
      if (!Objects.equals(paramDisplayText(leftValues[i]), paramDisplayText(rightValues[i]))) {
        return false;
      }
    }
    return true;
  }

  static String paramDisplayText(PSExtensionParamValue value) {
    if (value == null || value.getValue() == null) {
      return "";
    }
    String text = value.getValue().getValueDisplayText();
    return text != null ? text : "";
  }

  @SuppressWarnings("unchecked")
  static PSConditionalExit toConditionalExit(ContentTypeItemExit item, String field) {
    PSExtensionCallSet set = new PSExtensionCallSet();
    set.add(toExtensionCall(item, field));
    PSConditionalExit exit = new PSConditionalExit(set);
    if (item != null && item.getMaxErrorsToStop() != null) {
      int max = item.getMaxErrorsToStop();
      if (max <= 0) {
        throw new IllegalArgumentException(field + ".maxErrorsToStop must be greater than 0");
      }
      exit.setMaxErrorsToStop(max);
    }
    return exit;
  }

  static PSExtensionCall toExtensionCall(ContentTypeItemExit item, String field) {
    if (item == null) {
      throw new IllegalArgumentException(field + " is null");
    }
    String fqn = StringUtils.trimToNull(item.getExtension());
    if (fqn == null) {
      fqn = StringUtils.trimToNull(item.getName());
    }
    if (fqn == null) {
      throw new IllegalArgumentException(field + ".extension is required");
    }
    PSExtensionRef ref;
    try {
      ref = new PSExtensionRef(fqn);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(field + " invalid extension FQN: " + fqn, e);
    }
    List<ContentTypeItemExitParam> params =
        item.getParameters() != null ? item.getParameters() : List.of();
    PSExtensionParamValue[] values = new PSExtensionParamValue[params.size()];
    for (int i = 0; i < params.size(); i++) {
      ContentTypeItemExitParam p = params.get(i);
      String text = p != null && p.getValue() != null ? p.getValue() : "";
      values[i] = new PSExtensionParamValue(new PSTextLiteral(text));
    }
    return new PSExtensionCall(ref, values);
  }

  @SuppressWarnings("unchecked")
  static PSExtensionCallSet toCallSet(List<ContentTypeItemExit> items, String field) {
    PSExtensionCallSet set = new PSExtensionCallSet();
    int i = 0;
    for (ContentTypeItemExit item : items) {
      set.add(toExtensionCall(item, field + "[" + i + "]"));
      i++;
    }
    return set;
  }

  /**
   * Package-visible for unit tests. Collects control parameter names (not values) from a control
   * ref.
   */
  static List<String> controlPropertyNames(PSControlRef control) {
    List<String> names = new ArrayList<>();
    for (ContentTypeControlProperty p : controlProperties(control)) {
      names.add(p.getName());
    }
    return names;
  }

  /** Package-visible for unit tests. Collects control parameter name/value pairs. */
  static List<ContentTypeControlProperty> controlProperties(PSControlRef control) {
    if (control == null) {
      return List.of();
    }
    List<ContentTypeControlProperty> out = new ArrayList<>();
    for (Iterator<?> it = control.getParameters(); it.hasNext(); ) {
      Object o = it.next();
      if (o instanceof PSParam param && StringUtils.isNotBlank(param.getName())) {
        String value = param.getValue() != null ? param.getValue().getValueText() : null;
        out.add(new ContentTypeControlProperty(param.getName(), value));
      }
    }
    return out;
  }

  static List<DesignGap> controlPropertyDesignGaps() {
    return List.of(
        DesignGap.of(
            "CT_CHOICE_FILTER",
            "Choice filters, null-entry, and default-selected are not writable"));
  }

  static String choiceTypeName(int type) {
    return switch (type) {
      case PSChoices.TYPE_GLOBAL -> "global";
      case PSChoices.TYPE_LOCAL -> "local";
      case PSChoices.TYPE_LOOKUP -> "lookup";
      case PSChoices.TYPE_INTERNAL_LOOKUP -> "internalLookup";
      case PSChoices.TYPE_TABLE_INFO -> "tableinfo";
      default -> "unknown";
    };
  }

  static String sortOrderName(int sortOrder) {
    return switch (sortOrder) {
      case PSChoices.SORT_ORDER_ASCENDING -> "ascending";
      case PSChoices.SORT_ORDER_DESCENDING -> "descending";
      case PSChoices.SORT_ORDER_USER -> "user";
      default -> "ascending";
    };
  }

  static int parseSortOrder(String sortOrder) {
    if (StringUtils.isBlank(sortOrder) || "ascending".equalsIgnoreCase(sortOrder)) {
      return PSChoices.SORT_ORDER_ASCENDING;
    }
    if ("descending".equalsIgnoreCase(sortOrder)) {
      return PSChoices.SORT_ORDER_DESCENDING;
    }
    if ("user".equalsIgnoreCase(sortOrder)) {
      return PSChoices.SORT_ORDER_USER;
    }
    throw new IllegalArgumentException("choices.sortOrder must be ascending, descending, or user");
  }

  static ContentTypeChoiceCatalog toChoiceCatalog(PSChoices choices) {
    if (choices == null) {
      return null;
    }
    ContentTypeChoiceCatalog cat = new ContentTypeChoiceCatalog();
    cat.setType(choiceTypeName(choices.getType()));
    cat.setSortOrder(sortOrderName(choices.getSortOrder()));
    if (choices.getType() == PSChoices.TYPE_GLOBAL) {
      if (choices.getGlobal() >= 0) {
        cat.setGlobalId(choices.getGlobal());
      }
    } else if (choices.getType() == PSChoices.TYPE_LOCAL) {
      List<ContentTypeChoiceEntry> entries = new ArrayList<>();
      for (Iterator<?> it = choices.getLocal(); it.hasNext(); ) {
        Object o = it.next();
        if (o instanceof PSEntry e) {
          String label = e.getLabel() != null ? e.getLabel().getText() : null;
          entries.add(new ContentTypeChoiceEntry(e.getValue(), label));
        }
      }
      cat.setEntries(entries);
    } else if (choices.getType() == PSChoices.TYPE_LOOKUP
        || choices.getType() == PSChoices.TYPE_INTERNAL_LOOKUP) {
      if (choices.getLookup() != null) {
        cat.setLookupHref(choices.getLookup().getHref());
        cat.setLookupName(choices.getLookup().getName());
      }
    } else if (choices.getType() == PSChoices.TYPE_TABLE_INFO) {
      PSChoiceTableInfo ti = choices.getTableInfo();
      if (ti != null) {
        ContentTypeChoiceTable table = new ContentTypeChoiceTable();
        table.setDataSource(ti.getDataSource());
        table.setTableName(ti.getTableName());
        table.setLabelColumn(ti.getLableColumn());
        table.setValueColumn(ti.getValueColumn());
        cat.setTable(table);
      }
    }
    return cat;
  }

  static PSChoices fromChoiceCatalog(ContentTypeChoiceCatalog catalog) {
    if (catalog == null) {
      return null;
    }
    String type = catalog.getType() == null ? "" : catalog.getType().trim();
    if (type.isEmpty() || "none".equalsIgnoreCase(type)) {
      return null;
    }
    PSChoices choices;
    if ("global".equalsIgnoreCase(type)) {
      if (catalog.getGlobalId() == null || catalog.getGlobalId() < 0) {
        throw new IllegalArgumentException("choices.globalId is required for type global");
      }
      choices = new PSChoices(catalog.getGlobalId());
    } else if ("local".equalsIgnoreCase(type)) {
      List<ContentTypeChoiceEntry> entries = catalog.getEntries();
      if (entries == null || entries.isEmpty()) {
        throw new IllegalArgumentException("choices.entries is required for type local");
      }
      PSCollection<PSEntry> local = new PSCollection<>(PSEntry.class);
      for (int i = 0; i < entries.size(); i++) {
        ContentTypeChoiceEntry e = entries.get(i);
        if (e == null || e.getValue() == null) {
          throw new IllegalArgumentException("choices.entries[" + i + "] value is required");
        }
        String label = e.getLabel() != null ? e.getLabel() : e.getValue();
        local.add(new PSEntry(e.getValue(), new PSDisplayText(label)));
      }
      choices = new PSChoices(local);
    } else if ("lookup".equalsIgnoreCase(type) || "internalLookup".equalsIgnoreCase(type)) {
      if (StringUtils.isBlank(catalog.getLookupHref())) {
        throw new IllegalArgumentException("choices.lookupHref is required for type " + type);
      }
      int lookupType =
          "internalLookup".equalsIgnoreCase(type)
              ? PSChoices.TYPE_INTERNAL_LOOKUP
              : PSChoices.TYPE_LOOKUP;
      String lookupName =
          StringUtils.isBlank(catalog.getLookupName()) ? null : catalog.getLookupName().trim();
      choices =
          new PSChoices(
              new PSUrlRequest(
                  lookupName, catalog.getLookupHref(), new PSCollection<>(PSParam.class)),
              lookupType);
    } else if ("tableinfo".equalsIgnoreCase(type)) {
      ContentTypeChoiceTable table = catalog.getTable();
      if (table == null
          || StringUtils.isBlank(table.getTableName())
          || StringUtils.isBlank(table.getLabelColumn())
          || StringUtils.isBlank(table.getValueColumn())) {
        throw new IllegalArgumentException(
            "choices.table tableName, labelColumn, and valueColumn are required for type tableinfo");
      }
      String ds = table.getDataSource() != null ? table.getDataSource() : "";
      choices =
          new PSChoices(
              new PSChoiceTableInfo(
                  ds, table.getTableName(), table.getLabelColumn(), table.getValueColumn()));
    } else {
      throw new IllegalArgumentException(
          "choices.type must be global, local, lookup, internalLookup, tableinfo, or none");
    }
    if (StringUtils.isNotBlank(catalog.getSortOrder())) {
      choices.setSortOrder(parseSortOrder(catalog.getSortOrder()));
    }
    return choices;
  }

  static PSCollection<PSParam> toParamCollection(List<ContentTypeControlProperty> properties) {
    PSCollection<PSParam> params = new PSCollection<>(PSParam.class);
    if (properties == null) {
      return params;
    }
    for (int i = 0; i < properties.size(); i++) {
      ContentTypeControlProperty p = properties.get(i);
      if (p == null || StringUtils.isBlank(p.getName())) {
        throw new IllegalArgumentException("properties[" + i + "] name is required");
      }
      String value = p.getValue() != null ? p.getValue() : "";
      params.add(new PSParam(p.getName().trim(), new PSTextLiteral(value)));
    }
    return params;
  }

  static PSDisplayMapping findDisplayMapping(PSDisplayMapper dmapper, String fieldName) {
    if (dmapper == null || StringUtils.isBlank(fieldName)) {
      return null;
    }
    for (Iterator<?> it = dmapper.iterator(); it.hasNext(); ) {
      Object o = it.next();
      if (!(o instanceof PSDisplayMapping entry)) {
        continue;
      }
      if (fieldName.equals(entry.getFieldRef())) {
        return entry;
      }
      if (entry.getDisplayMapper() != null) {
        PSDisplayMapping nested = findDisplayMapping(entry.getDisplayMapper(), fieldName);
        if (nested != null) {
          return nested;
        }
      }
    }
    return null;
  }

  private PSDisplayMapper displayMapperOf(PSItemDefinition def) {
    try {
      if (def == null || def.getContentEditor() == null) {
        return null;
      }
      Object pipeObj = def.getContentEditor().getPipe();
      if (!(pipeObj instanceof PSContentEditorPipe pipe)) {
        return null;
      }
      if (pipe.getMapper() == null || pipe.getMapper().getUIDefinition() == null) {
        return null;
      }
      return pipe.getMapper().getUIDefinition().getDisplayMapper();
    } catch (Exception e) {
      log.warn(
          "Could not resolve display mapper for {}: {}",
          def != null ? def.getName() : "?",
          e.getMessage(),
          e);
      return null;
    }
  }

  private ContentTypeFieldControlProperties loadFieldControlProperties(
      PSItemDefinition def, String fieldName) {
    PSDisplayMapping mapping = requireFieldMapping(def, fieldName);
    return toFieldControlProperties(fieldName, mapping);
  }

  private PSDisplayMapping requireFieldMapping(PSItemDefinition def, String fieldName) {
    PSDisplayMapping mapping = findDisplayMapping(displayMapperOf(def), fieldName);
    if (mapping == null) {
      throw new WebApplicationException("Field not found: " + fieldName, 404);
    }
    return mapping;
  }

  /**
   * Locate a field by submit name on the parent field set and complex children.
   *
   * @param notFoundIsBadRequest {@code true} throws {@link IllegalArgumentException} (PUT);
   *     {@code false} throws HTTP 404 (GET)
   */
  private PSField requireField(
      PSItemDefinition def, String fieldName, boolean notFoundIsBadRequest) {
    PSField field = findField(def, fieldName);
    if (field == null) {
      String msg = "Unknown field: " + fieldName;
      if (notFoundIsBadRequest) {
        throw new IllegalArgumentException(msg);
      }
      throw new WebApplicationException(msg, 404);
    }
    return field;
  }

  static PSField findField(PSItemDefinition def, String fieldName) {
    if (def == null || StringUtils.isBlank(fieldName)) {
      return null;
    }
    PSFieldSet parentFs = def.getFieldSet();
    if (parentFs != null) {
      PSField field = parentFs.findFieldByName(fieldName, false);
      if (field != null) {
        return field;
      }
    }
    List<PSFieldSet> children = def.getComplexChildren();
    if (children != null) {
      for (PSFieldSet child : children) {
        if (child == null) {
          continue;
        }
        PSField field = child.findFieldByName(fieldName, false);
        if (field != null) {
          return field;
        }
      }
    }
    return null;
  }

  private ContentTypeFieldRuleExpressions loadFieldRuleExpressions(
      PSItemDefinition def, String fieldName, boolean missingFieldIs404) {
    PSField field = requireField(def, fieldName, !missingFieldIs404);
    return toFieldRuleExpressions(fieldName, field);
  }

  static ContentTypeFieldRuleExpressions toFieldRuleExpressions(String fieldName, PSField field) {
    ContentTypeFieldRuleExpressions out = new ContentTypeFieldRuleExpressions();
    out.setFieldName(fieldName);
    out.setDesignGaps(new ArrayList<>(fieldRuleDesignGaps()));
    if (field == null) {
      out.setValidation(List.of());
      out.setVisibility(List.of());
      out.setInputTranslation(List.of());
      out.setOutputTranslation(List.of());
      return out;
    }
    PSFieldValidationRules validation = field.getValidationRules();
    out.setValidation(toValidationFieldRules(validation));
    out.setVisibility(toVisibilityFieldRules(field.getVisibilityRules()));
    out.setInputTranslation(mapExtensionCalls(translationCalls(field.getInputTranslation())));
    out.setOutputTranslation(mapExtensionCalls(translationCalls(field.getOutputTranslation())));
    out.setValidationExpression(summarizeValidationRules(validation));
    out.setVisibilityExpression(summarizeVisibilityRules(field.getVisibilityRules()));
    out.setInputTranslationExpression(summarizeTranslation(field.getInputTranslation()));
    out.setOutputTranslationExpression(summarizeTranslation(field.getOutputTranslation()));
    if (validation != null) {
      out.setMaxErrorsToStop(validation.getMaxErrorsToStop());
      if (validation.getErrorMessage() != null
          && StringUtils.isNotBlank(validation.getErrorMessage().getText())) {
        out.setErrorMessage(validation.getErrorMessage().getText());
      }
    }
    return out;
  }

  static List<DesignGap> fieldRuleDesignGaps() {
    return List.of(
        DesignGap.of(
            "CT_FIELD_RULE_APPLY_WHEN",
            "Apply-when on field validation is not written; conditional variable/value are"
                + " stored as text literals"));
  }

  static List<ContentTypeFieldRule> toValidationFieldRules(PSFieldValidationRules rules) {
    List<ContentTypeFieldRule> out = new ArrayList<>();
    if (rules == null) {
      return out;
    }
    for (Iterator<?> it = rules.getRules(); it.hasNext(); ) {
      Object o = it.next();
      if (o instanceof PSRule rule) {
        out.addAll(toFieldRules(rule));
      }
    }
    for (Iterator<?> it = rules.getRuleReferences(); it.hasNext(); ) {
      Object ref = it.next();
      if (ref != null && StringUtils.isNotBlank(ref.toString())) {
        ContentTypeFieldRule dto = new ContentTypeFieldRule();
        dto.setType(ContentTypeFieldRule.TYPE_REFERENCE);
        dto.setReference(ref.toString().trim());
        dto.setSummary("ref:" + ref.toString().trim());
        out.add(dto);
      }
    }
    return out;
  }

  static List<ContentTypeFieldRule> toVisibilityFieldRules(PSVisibilityRules rules) {
    List<ContentTypeFieldRule> out = new ArrayList<>();
    if (rules == null || rules.isEmpty()) {
      return out;
    }
    for (Object o : rules) {
      if (o instanceof PSRule rule) {
        out.addAll(toFieldRules(rule));
      }
    }
    return out;
  }

  static List<ContentTypeFieldRule> toFieldRules(PSRule rule) {
    if (rule == null) {
      return List.of();
    }
    if (rule.isExtensionSetRule()) {
      List<ContentTypeFieldRule> out = new ArrayList<>();
      PSExtensionCallSet set = rule.getExtensionRules();
      if (set == null || set.isEmpty()) {
        return out;
      }
      for (Object o : set) {
        if (o instanceof PSExtensionCall call) {
          ContentTypeItemExit exit = toExitDto(call);
          ContentTypeFieldRule dto = new ContentTypeFieldRule();
          dto.setType(ContentTypeFieldRule.TYPE_EXTENSION);
          dto.setExtension(exit.getExtension());
          dto.setName(exit.getName());
          dto.setParameters(exit.getParameters());
          dto.setSummary(exit.getSummary());
          out.add(dto);
        }
      }
      return out;
    }
    ContentTypeFieldRule dto = new ContentTypeFieldRule();
    dto.setType(ContentTypeFieldRule.TYPE_CONDITIONAL);
    List<ContentTypeFieldConditional> conds = new ArrayList<>();
    for (Iterator<?> it = rule.getConditionalRules(); it.hasNext(); ) {
      Object o = it.next();
      if (o instanceof PSConditional conditional) {
        ContentTypeFieldConditional cond = new ContentTypeFieldConditional();
        cond.setVariable(replacementText(conditional.getVariable()));
        cond.setOperator(conditional.getOperator());
        cond.setValue(replacementText(conditional.getValue()));
        cond.setBooleanOperator(conditional.getBoolean());
        conds.add(cond);
      }
    }
    dto.setConditionals(conds);
    dto.setSummary(summarizeRule(rule));
    return List.of(dto);
  }

  static String replacementText(IPSReplacementValue value) {
    if (value == null) {
      return null;
    }
    String text = value.getValueText();
    if (StringUtils.isNotBlank(text)) {
      return text;
    }
    String display = value.getValueDisplayText();
    return StringUtils.isNotBlank(display) ? display : null;
  }

  private static PSExtensionCallSet translationCalls(PSFieldTranslation translation) {
    return translation != null ? translation.getTranslations() : null;
  }

  static void applyFieldRuleExpressions(PSField field, ContentTypeFieldRuleExpressions body) {
    field.setValidationRules(toFieldValidationRules(body));
    field.setVisibilityRules(toVisibilityRules(body.getVisibility(), "visibility"));
    field.setInputTranslation(
        toFieldTranslation(body.getInputTranslation(), "inputTranslation"));
    field.setOutputTranslation(
        toFieldTranslation(body.getOutputTranslation(), "outputTranslation"));
  }

  @SuppressWarnings("unchecked")
  static PSFieldValidationRules toFieldValidationRules(ContentTypeFieldRuleExpressions body) {
    List<ContentTypeFieldRule> items = body.getValidation();
    if (items.isEmpty()
        && body.getMaxErrorsToStop() == null
        && body.getErrorMessage() == null) {
      return null;
    }
    PSCollection<PSRule> rules = new PSCollection<>(PSRule.class);
    PSCollection<String> refs = new PSCollection<>(String.class);
    int i = 0;
    for (ContentTypeFieldRule item : items) {
      String field = "validation[" + i + "]";
      String type = ruleType(item, field);
      if (ContentTypeFieldRule.TYPE_REFERENCE.equals(type)) {
        if (item == null || StringUtils.isBlank(item.getReference())) {
          throw new IllegalArgumentException(field + ".reference is required");
        }
        refs.add(item.getReference().trim());
      } else {
        rules.add(toPsRule(item, field));
      }
      i++;
    }
    if (rules.isEmpty() && refs.isEmpty()) {
      return null;
    }
    PSFieldValidationRules out = new PSFieldValidationRules();
    out.setRules(rules);
    out.setRuleReferences(refs);
    if (body.getMaxErrorsToStop() != null) {
      int max = body.getMaxErrorsToStop();
      if (max <= 0) {
        throw new IllegalArgumentException("maxErrorsToStop must be greater than 0");
      }
      out.setMaxErrorsToStop(max);
    }
    if (body.getErrorMessage() != null) {
      if (StringUtils.isBlank(body.getErrorMessage())) {
        out.setErrorMessage(null);
      } else {
        out.setErrorMessage(new PSDisplayText(body.getErrorMessage().trim()));
      }
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  static PSVisibilityRules toVisibilityRules(List<ContentTypeFieldRule> items, String field) {
    if (items.isEmpty()) {
      return null;
    }
    PSVisibilityRules out = new PSVisibilityRules();
    int i = 0;
    for (ContentTypeFieldRule item : items) {
      String path = field + "[" + i + "]";
      String type = ruleType(item, path);
      if (ContentTypeFieldRule.TYPE_REFERENCE.equals(type)) {
        throw new IllegalArgumentException(path + " type=reference is not allowed on visibility");
      }
      out.add(toPsRule(item, path));
      i++;
    }
    return out;
  }

  static PSFieldTranslation toFieldTranslation(List<ContentTypeItemExit> items, String field) {
    if (items.isEmpty()) {
      return null;
    }
    return new PSFieldTranslation(toCallSet(items, field));
  }

  static String ruleType(ContentTypeFieldRule item, String field) {
    if (item == null) {
      throw new IllegalArgumentException(field + " is null");
    }
    String type = StringUtils.trimToNull(item.getType());
    if (type == null) {
      throw new IllegalArgumentException(field + ".type is required");
    }
    String lower = type.toLowerCase();
    if (ContentTypeFieldRule.TYPE_CONDITIONAL.equals(lower)
        || ContentTypeFieldRule.TYPE_EXTENSION.equals(lower)
        || ContentTypeFieldRule.TYPE_REFERENCE.equals(lower)) {
      return lower;
    }
    throw new IllegalArgumentException(
        field + ".type must be conditional, extension, or reference");
  }

  @SuppressWarnings("unchecked")
  static PSRule toPsRule(ContentTypeFieldRule item, String field) {
    String type = ruleType(item, field);
    if (ContentTypeFieldRule.TYPE_EXTENSION.equals(type)) {
      ContentTypeItemExit exit = new ContentTypeItemExit();
      exit.setExtension(item.getExtension());
      exit.setName(item.getName());
      exit.setParameters(item.getParameters());
      PSExtensionCallSet set = new PSExtensionCallSet();
      set.add(toExtensionCall(exit, field));
      return new PSRule(set);
    }
    if (!ContentTypeFieldRule.TYPE_CONDITIONAL.equals(type)) {
      throw new IllegalArgumentException(field + ".type must be conditional or extension");
    }
    if (item.getConditionals() == null || item.getConditionals().isEmpty()) {
      throw new IllegalArgumentException(field + ".conditionals is required");
    }
    PSCollection<PSConditional> conds = new PSCollection<>(PSConditional.class);
    int i = 0;
    for (ContentTypeFieldConditional conditional : item.getConditionals()) {
      conds.add(toPsConditional(conditional, field + ".conditionals[" + i + "]"));
      i++;
    }
    return new PSRule(conds);
  }

  static PSConditional toPsConditional(ContentTypeFieldConditional item, String field) {
    if (item == null) {
      throw new IllegalArgumentException(field + " is null");
    }
    if (StringUtils.isBlank(item.getVariable())) {
      throw new IllegalArgumentException(field + ".variable is required");
    }
    if (StringUtils.isBlank(item.getOperator())) {
      throw new IllegalArgumentException(field + ".operator is required");
    }
    String op = normalizeOperator(item.getOperator());
    boolean nullOp =
        PSConditional.OPTYPE_ISNULL.equalsIgnoreCase(op)
            || PSConditional.OPTYPE_ISNOTNULL.equalsIgnoreCase(op);
    IPSReplacementValue value =
        item.getValue() != null
            ? new PSTextLiteral(item.getValue())
            : (nullOp ? null : new PSTextLiteral(""));
    try {
      return new PSConditional(
          new PSTextLiteral(item.getVariable().trim()),
          op,
          value,
          StringUtils.trimToNull(item.getBooleanOperator()));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(field + " invalid conditional: " + e.getMessage(), e);
    }
  }

  static String normalizeOperator(String operator) {
    String op = operator.trim();
    if ("!=".equals(op)) {
      return PSConditional.OPTYPE_NOTEQUALS;
    }
    if ("==".equals(op)) {
      return PSConditional.OPTYPE_EQUALS;
    }
    return op;
  }

  private static ContentTypeFieldControlProperties toFieldControlProperties(
      String fieldName, PSDisplayMapping mapping) {
    ContentTypeFieldControlProperties out = new ContentTypeFieldControlProperties();
    out.setFieldName(fieldName);
    PSUISet ui = mapping != null ? mapping.getUISet() : null;
    PSControlRef control = ui != null ? ui.getControl() : null;
    if (control != null && StringUtils.isNotBlank(control.getName())) {
      out.setControl(control.getName());
    }
    out.setProperties(new ArrayList<>(controlProperties(control)));
    if (ui != null) {
      out.setChoices(toChoiceCatalog(ui.getChoices()));
    }
    out.setDesignGaps(new ArrayList<>(controlPropertyDesignGaps()));
    return out;
  }

  private static void applyControlPropertyUpdates(
      PSDisplayMapping mapping, ContentTypeFieldControlProperties body) {
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
      control.setParameters(toParamCollection(properties));
    }
    if (body.getChoices() != null) {
      ui.setChoices(fromChoiceCatalog(body.getChoices()));
    }
  }

  /**
   * Walk parent display mapper for control names and labels keyed by field ref.
   *
   * @return {@code true} when the display mapper was walked successfully; {@code false} when
   *     resolution failed (caller should surface a design gap).
   */
  private boolean mapControls(
      PSItemDefinition def,
      Map<String, String> map,
      Map<String, List<ContentTypeControlProperty>> controlPropsByField) {
    try {
      if (def == null || def.getContentEditor() == null) {
        return false;
      }
      PSContentEditorPipe pipe = (PSContentEditorPipe) def.getContentEditor().getPipe();
      if (pipe == null || pipe.getMapper() == null || pipe.getMapper().getUIDefinition() == null) {
        return false;
      }
      PSDisplayMapper dmapper = pipe.getMapper().getUIDefinition().getDisplayMapper();
      walkDisplayMapper(dmapper, map, controlPropsByField);
      return true;
    } catch (Exception e) {
      log.warn("Could not resolve display controls for {}: {}", def.getName(), e.getMessage(), e);
      return false;
    }
  }

  private void walkDisplayMapper(
      PSDisplayMapper dmapper,
      Map<String, String> map,
      Map<String, List<ContentTypeControlProperty>> controlPropsByField) {
    if (dmapper == null) {
      return;
    }
    for (Iterator<?> it = dmapper.iterator(); it.hasNext(); ) {
      Object o = it.next();
      if (!(o instanceof PSDisplayMapping entry)) {
        continue;
      }
      String fieldRef = entry.getFieldRef();
      if (StringUtils.isNotBlank(fieldRef)) {
        PSUISet ui = entry.getUISet();
        if (ui != null) {
          if (ui.getLabel() != null && StringUtils.isNotBlank(ui.getLabel().getText())) {
            map.put(fieldRef + ":label", ui.getLabel().getText());
          }
          PSControlRef control = ui.getControl();
          if (control != null && StringUtils.isNotBlank(control.getName())) {
            map.put(fieldRef, control.getName());
            List<ContentTypeControlProperty> props = controlProperties(control);
            if (!props.isEmpty()) {
              controlPropsByField.put(fieldRef, props);
            }
          }
        }
      }
      if (entry.getDisplayMapper() != null) {
        walkDisplayMapper(entry.getDisplayMapper(), map, controlPropsByField);
      }
    }
  }

  /**
   * PUT save requires a lock already held by this user. Does not acquire a lock.
   *
   * <p>Session equality is not compared against {@code KEY_JSESSIONID}: lock session ids may be the
   * clientId. A foreign session is rejected by {@code loadContentTypes(..., lock=true,
   * overrideLock=false)}.
   *
   * @throws ContentTypeDesignLockException when unlocked or locked by another user (HTTP 409)
   */
  /**
   * Validates a CD-01 rename target. Names must be non-blank, have no spaces or wildcards, and
   * use only characters allowed for content type names.
   */
  static String validateNewContentTypeName(String newName) {
    if (StringUtils.isBlank(newName)) {
      throw new IllegalArgumentException("name is required");
    }
    String trimmed = newName.trim();
    if (trimmed.indexOf('*') >= 0 || trimmed.indexOf('%') >= 0) {
      throw new IllegalArgumentException("Content type name must not contain wildcards");
    }
    for (int i = 0; i < trimmed.length(); i++) {
      if (Character.isWhitespace(trimmed.charAt(i))) {
        throw new IllegalArgumentException("Content type name must not contain spaces");
      }
    }
    Character invalid = PSStringUtils.validateContentTypeName(trimmed);
    if (invalid != null) {
      throw new IllegalArgumentException(
          "Content type name contains invalid character: " + invalid);
    }
    return trimmed;
  }

  /**
   * Case-insensitive uniqueness against the design catalog. The current type id is excluded so a
   * no-op or case-only rename of the same type is allowed.
   */
  private void requireUniqueContentTypeName(String newName, int currentTypeId) {
    List<IPSCatalogSummary> found = designSvc.findContentTypes("*");
    if (found == null) {
      return;
    }
    for (IPSCatalogSummary sum : found) {
      if (sum == null || !newName.equalsIgnoreCase(sum.getName())) {
        continue;
      }
      IPSGuid guid = sum.getGUID();
      int uuid = guid != null ? guid.getUUID() : -1;
      if (uuid != currentTypeId) {
        throw new IllegalArgumentException("Content type name already exists: " + newName);
      }
    }
  }

  private void requireHeldLock(IPSGuid ctGuid) {
    if (systemDesign == null) {
      throw new IllegalStateException(
          "Could not save content type; design service unavailable");
    }
    List<PSObjectSummary> locked;
    try {
      locked = systemDesign.isLocked(Collections.singletonList(ctGuid), currentUser());
    } catch (PSErrorResultsException e) {
      if (isNotFoundError(e)) {
        throw new ContentTypeDesignLockException(
            "Could not save content type; design lock required", e);
      }
      if (hasLockError(e)) {
        String locker = firstLockLocker(e);
        throw new ContentTypeDesignLockException(
            locker != null
                ? "Could not save content type; locked by " + locker
                : "Could not save content type; design lock required",
            e);
      }
      throw new IllegalStateException("Could not save content type; design service error", e);
    }
    PSObjectSummary summary =
        locked == null || locked.isEmpty() ? null : locked.get(0);
    if (summary == null || !summary.isLocked()) {
      throw new ContentTypeDesignLockException("Could not save content type; design lock required");
    }
    String user = currentUser();
    PSObjectLockSummary info = summary.getLocked();
    if (!summary.isLockedBy(user)) {
      String locker = info != null ? info.getLocker() : null;
      throw new ContentTypeDesignLockException(
          locker != null
              ? "Could not save content type; locked by " + locker
              : "Could not save content type; design lock required");
    }
  }

  private PSItemDefinition reloadItemDef(String idOrName) {
    if (itemDefManager == null) {
      return null;
    }
    try {
      return resolveItemDef(idOrName);
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type not in item-def cache after save: {}", idOrName);
      return null;
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
      throw new WebApplicationException(
          "Admin role required to create, lock, unlock, save, or rename content types",
          Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(
          "Admin role required to create, lock, unlock, save, or rename content types",
          Response.Status.FORBIDDEN);
    }
  }

  /**
   * Case-insensitive uniqueness against the design catalog. Duplicate names are 409 so clients
   * can distinguish them from 400 invalid-name failures.
   */
  private void assertNameUnique(String name) {
    List<IPSCatalogSummary> existing = designSvc.findContentTypes("*");
    if (existing == null) {
      return;
    }
    for (IPSCatalogSummary summary : existing) {
      if (summary != null
          && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
        throw new WebApplicationException("Content type already exists: " + name, 409);
      }
    }
  }

  /**
   * Persist-time duplicate ({@code PSContentTypeHelper.validateUniqueName}) is 409, not 400/500.
   */
  private static RuntimeException mapCreateNameCollision(String name, IllegalArgumentException e) {
    if (isAlreadyExistsFailure(e)) {
      return new WebApplicationException("Content type already exists: " + name, 409);
    }
    return e;
  }

  private RuntimeException mapCreatePersistFailure(String name, Exception e, String fallback) {
    if (isAlreadyExistsFailure(e)) {
      return new WebApplicationException("Content type already exists: " + name, 409);
    }
    log.error("{} {}: {}", fallback, name, e.getMessage(), e);
    return new IllegalStateException(fallback, e);
  }

  /** Design-WS unique-name failures use OBJECT_ALREADY_EXISTS ("… already exists."). */
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

  private static boolean containsWhitespace(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isWhitespace(name.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private IPSAssemblyService requireAssemblyService() {
    if (assemblyService == null) {
      throw new IllegalStateException("Assembly service is not available");
    }
    return assemblyService;
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

  private static void requireSessionUserForLock() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for content type design session",
          Response.Status.FORBIDDEN);
    }
  }

  /**
   * Resolve a content type GUID from uuid, guid string, or unique name via content design find.
   *
   * @return guid or {@code null} when not found
   */
  IPSGuid resolveContentTypeGuid(String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    if (StringUtils.isNumeric(idOrName)) {
      try {
        return new PSGuid(PSTypeEnum.NODEDEF, Long.parseLong(idOrName));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid content type id: " + idOrName, e);
      }
    }
    if (idOrName.contains("-")) {
      try {
        PSGuid g = new PSGuid(idOrName);
        if (g.getType() == 0) {
          g = new PSGuid(PSTypeEnum.NODEDEF, g.getUUID());
        }
        return g;
      } catch (RuntimeException ignore) {
        // fall through to name lookup
      }
    }
    if (idOrName.indexOf('*') >= 0 || idOrName.indexOf('%') >= 0) {
      throw new IllegalArgumentException("Content type name must not contain wildcards");
    }
    List<IPSCatalogSummary> found = designSvc.findContentTypes(idOrName);
    if (found == null || found.isEmpty()) {
      return null;
    }
    for (IPSCatalogSummary sum : found) {
      if (sum != null
          && sum.getGUID() != null
          && (idOrName.equalsIgnoreCase(sum.getName())
              || idOrName.equalsIgnoreCase(sum.getLabel()))) {
        return sum.getGUID();
      }
    }
    return null;
  }

  /**
   * Resolve a guid and, for numeric/guid-string ids, confirm the content type exists (read-only
   * load) so unknown ids 404 instead of 409.
   */
  private IPSGuid resolveExistingContentTypeGuid(String idOrName) {
    IPSGuid guid = resolveContentTypeGuid(idOrName);
    if (guid == null) {
      return null;
    }
    if (StringUtils.isNumeric(idOrName) || idOrName.contains("-")) {
      if (!contentTypeGuidExists(guid)) {
        return null;
      }
    }
    return guid;
  }

  private boolean contentTypeGuidExists(IPSGuid guid) {
    try {
      List<PSItemDefinition> defs =
          designSvc.loadContentTypes(
              Collections.singletonList(guid), false, false, currentSession(), currentUser());
      return defs != null && !defs.isEmpty() && defs.get(0) != null;
    } catch (PSErrorResultsException e) {
      if (isNotFoundError(e)) {
        return false;
      }
      throw new IllegalStateException("Failed to resolve content type", e);
    }
  }

  private long remainingLockMinutes(IPSGuid ctGuid) {
    if (systemDesign == null || ctGuid == null) {
      return DESIGN_LOCK_MINUTES;
    }
    try {
      List<PSObjectSummary> locked =
          systemDesign.isLocked(Collections.singletonList(ctGuid), currentUser());
      if (locked != null && !locked.isEmpty() && locked.get(0) != null) {
        PSObjectLockSummary info = locked.get(0).getLocked();
        if (info != null && info.getRemainingTime() > 0) {
          return info.getRemainingTime();
        }
      }
    } catch (Exception e) {
      log.debug("Could not read remaining lock time: {}", e.getMessage());
    }
    return DESIGN_LOCK_MINUTES;
  }

  private void throwLockOrNotFound(PSErrorResultsException e, String idOrName, boolean acquiring) {
    if (hasLockError(e)) {
      String locker = firstLockLocker(e);
      String verb = acquiring ? "acquire" : "release";
      String msg =
          locker != null
              ? "Could not " + verb + " design lock for content type; locked by " + locker
              : "Could not " + verb + " design lock for content type";
      throw new ContentTypeDesignLockException(msg, e);
    }
    if (isNotFoundError(e)) {
      return;
    }
    throw new IllegalStateException(
        "Failed to "
            + (acquiring ? "open" : "close")
            + " content type design session: "
            + idOrName,
        e);
  }

  /** Package-visible for unit tests. True when any collected error is a lock failure. */
  static boolean hasLockError(PSErrorResultsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (err instanceof PSLockErrorException) {
        return true;
      }
    }
    return false;
  }

  /**
   * Concatenate {@link PSErrorsException} error-map messages so REST 400/500 can surface the
   * design-save failure (PSSystemValidationException, extension interface, etc.) instead of a
   * bare SAVE_FAILED 500.
   */
  static String formatSaveErrors(PSErrorsException e) {
    if (e == null) {
      return "unknown error";
    }
    if (e.getErrors() == null || e.getErrors().isEmpty()) {
      return StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "unknown error";
    }
    List<String> parts = new ArrayList<>();
    for (Object err : e.getErrors().values()) {
      if (err instanceof PSErrorException pe) {
        String msg = pe.getErrorMessage();
        if (StringUtils.isBlank(msg)) {
          msg = pe.getMessage();
        }
        if (StringUtils.isNotBlank(msg)) {
          parts.add(msg.trim());
        }
      } else if (err != null) {
        String msg = String.valueOf(err).trim();
        if (!msg.isEmpty()) {
          parts.add(msg);
        }
      }
    }
    if (parts.isEmpty()) {
      return StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "unknown error";
    }
    return String.join("; ", parts);
  }

  /**
   * True when a batched {@code saveContentTypes} SAVE_FAILED looks like a design validation /
   * extension-interface problem (HTTP 400) rather than an unexpected server failure (HTTP 500).
   */
  static boolean isValidationSaveFailure(PSErrorsException e) {
    String text = formatSaveErrors(e);
    if (StringUtils.isBlank(text)) {
      return false;
    }
    String lower = text.toLowerCase();
    return lower.contains("validation")
        || lower.contains("does not implement")
        || lower.contains("invalid extension")
        || lower.contains("pssystemvalidation")
        || lower.contains("psextensionexception")
        || lower.contains("extension type")
        || lower.contains("invalid_ext");
  }

  /** Map-keyed overload for {@code PSErrorsException} batched save errors. */
  static boolean hasLockError(Map<IPSGuid, Object> errors) {
    if (errors == null || errors.isEmpty()) {
      return false;
    }
    for (Object err : errors.values()) {
      if (err instanceof PSLockErrorException) {
        return true;
      }
    }
    return false;
  }

  /**
   * Package-visible for unit tests. True when a batched save error reports the design lock as
   * missing — typed {@link PSLockErrorException}, or a {@link PSErrorException} whose message
   * contains "is not locked" (lowercase substring). Returns {@code false} for {@code null} or
   * unparseable inputs.
   */
  static boolean isNotLockedError(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (err instanceof PSLockErrorException) {
        return true;
      }
      if (err instanceof PSErrorException pe) {
        String msg = pe.getErrorMessage() != null ? pe.getErrorMessage() : pe.getMessage();
        if (StringUtils.containsIgnoreCase(msg, "is not locked")) {
          return true;
        }
      }
    }
    return false;
  }

  private ContentTypeDesignLockException lockConflict(Exception cause, String prefix) {
    String locker = firstLockLockerForMap(cause);
    if (locker != null) {
      return new ContentTypeDesignLockException(prefix + "; locked by " + locker, cause);
    }
    return new ContentTypeDesignLockException(prefix + "; design lock required", cause);
  }

  private static String firstLockLockerForMap(Exception e) {
    Map<IPSGuid, Object> errors = null;
    if (e instanceof PSErrorResultsException re && re.getErrors() != null) {
      errors = re.getErrors();
    } else if (e instanceof PSErrorsException se && se.getErrors() != null) {
      errors = se.getErrors();
    }
    if (errors == null) {
      return null;
    }
    for (Object err : errors.values()) {
      if (err instanceof PSLockErrorException lockErr
          && StringUtils.isNotBlank(lockErr.getLocker())) {
        return lockErr.getLocker();
      }
    }
    return null;
  }

  static String firstLockLocker(PSErrorResultsException e) {
    if (e == null || e.getErrors() == null) {
      return null;
    }
    for (Object err : e.getErrors().values()) {
      if (err instanceof PSLockErrorException lockErr
          && StringUtils.isNotBlank(lockErr.getLocker())) {
        return lockErr.getLocker();
      }
    }
    return null;
  }

  static boolean isNotFoundError(PSErrorResultsException e) {
    if (e == null || e.getErrors() == null || e.getErrors().isEmpty()) {
      return false;
    }
    if (hasLockError(e)) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (err != null && StringUtils.containsIgnoreCase(String.valueOf(err), "not found")) {
        return true;
      }
    }
    return false;
  }

  static ObjectLockSummary toLockSummary(String session, String user, long remainingMinutes) {
    ObjectLockSummary summary = new ObjectLockSummary();
    summary.setSession(session);
    summary.setLocker(user);
    summary.setRemainingTime(remainingMinutes);
    return summary;
  }
}
