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
import com.percussion.design.objectstore.PSConditional;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSExtensionCallSet;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSFieldTranslation;
import com.percussion.design.objectstore.PSFieldValidationRules;
import com.percussion.design.objectstore.PSParam;
import com.percussion.design.objectstore.PSRule;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.design.objectstore.PSVisibilityRules;
import com.percussion.design.objectstore.PSWorkflowInfo;
import com.percussion.rest.DesignGap;
import com.percussion.rest.Guid;
import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.rest.contenttypes.ContentTypeFilter;
import com.percussion.rest.contenttypes.IContentTypesAdaptor;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
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
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
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
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean
public class ContentTypeAdaptor implements IContentTypesAdaptor {

  private static final Logger log = LogManager.getLogger(ContentTypeAdaptor.class);

  /** Typical design-session lock duration reported by SOAP (minutes). */
  static final long DESIGN_LOCK_MINUTES = 30L;

  private final IPSContentDesignWs designSvc;
  private final PSItemDefManager itemDefManager;
  private final IPSSystemDesignWs systemDesign;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ContentTypeAdaptor() {
    this(
        PSContentWsLocator.getContentDesignWebservice(),
        PSItemDefManager.getInstance(),
        PSSystemWsLocator.getSystemDesignWebservice(),
        null);
  }

  /** Package-visible for unit tests that inject design web services and Admin gate. */
  ContentTypeAdaptor(
      IPSContentDesignWs designSvc,
      PSItemDefManager itemDefManager,
      IPSSystemDesignWs systemDesign,
      BooleanSupplier adminChecker) {
    this.designSvc = designSvc;
    this.itemDefManager = itemDefManager;
    this.systemDesign = systemDesign;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
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
  public ContentTypeDetail getContentType(URI baseUri, String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    try {
      PSItemDefinition def = resolveItemDef(idOrName.trim());
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
      } catch (Exception ignore) {
        // fall through to name
      }
    }
    return itemDefManager.getItemDef(idOrName, PSItemDefManager.COMMUNITY_ANY);
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
    Map<String, List<String>> controlPropsByField = new HashMap<>();
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
    detail.setAllowedWorkflows(loadWorkflows(ctGuid, defaultWfId));
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
              "Field rule expressions and control property names are read-only; rule write/save and"
                  + " full control property catalogs/values are not supported"));
    gaps.add(DesignGap.of("CT_ITEM_EXITS", "Item-level pre/post exits not exposed"));
    gaps.add(
        DesignGap.of(
            "CT_CREATE_DELETE",
            "Create / delete not supported; PUT save requires a held design lock for"
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
            "Field display labels and control properties are not writable via this API"));
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
      IPSGuid ctGuid = resolveContentTypeGuid(idOrName.trim());
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
    IPSGuid ctGuid = resolveContentTypeGuid(idOrName.trim());
    if (ctGuid == null) {
      return null;
    }
    try {
      List<PSItemDefinition> locked =
          designSvc.loadContentTypes(Collections.singletonList(ctGuid), true, false, session, user);
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        return null;
      }
      return toLockSummary(session, user, DESIGN_LOCK_MINUTES);
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
    IPSGuid ctGuid = resolveContentTypeGuid(idOrName.trim());
    if (ctGuid == null) {
      return null;
    }
    // Detect another user's lock without stealing it (self-only, overrideLock=false).
    try {
      designSvc.loadContentTypes(Collections.singletonList(ctGuid), true, false, session, user);
    } catch (PSErrorResultsException e) {
      throwLockOrNotFound(e, idOrName, false);
      return null;
    }
    if (systemDesign != null) {
      systemDesign.releaseLocks(Collections.singletonList(ctGuid), session, user);
    }
    return Boolean.TRUE;
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
        return fromGuid;
      }
    }
    if (StringUtils.isNotBlank(ref.getName())) {
      IPSWorkflowService wfSvc = PSWorkflowServiceLocator.getWorkflowService();
      List<PSWorkflow> found = wfSvc.findWorkflowsByName(ref.getName().trim());
      if (found == null || found.isEmpty()) {
        throw new IllegalArgumentException(field + " workflow not found: " + ref.getName());
      }
      return found.get(0).getGUID().getUUID();
    }
    throw new IllegalArgumentException(field + " requires name or guid");
  }

  private List<IPSGuid> resolveTemplateGuids(List<NamedObjectRef> refs) {
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
    IPSAssemblyService asm = PSAssemblyServiceLocator.getAssemblyService();
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
   * expressions, control property names/values, and field labels on the wire DTO are ignored.
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

  private List<NamedObjectRef> loadWorkflows(IPSGuid ctGuid, int defaultWfId) {
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
      IPSWorkflowService wfSvc = PSWorkflowServiceLocator.getWorkflowService();
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
      IPSAssemblyService asm = PSAssemblyServiceLocator.getAssemblyService();
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
      Map<String, List<String>> controlPropsByField,
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
      List<String> propNames = controlPropsByField.get(field.getSubmitName());
      if (propNames != null && !propNames.isEmpty()) {
        f.setControlPropertyNames(List.copyOf(propNames));
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

  /**
   * Package-visible for unit tests. Collects control parameter names (not values) from a control
   * ref.
   */
  static List<String> controlPropertyNames(PSControlRef control) {
    if (control == null) {
      return List.of();
    }
    List<String> names = new ArrayList<>();
    for (Iterator<?> it = control.getParameters(); it.hasNext(); ) {
      Object o = it.next();
      if (o instanceof PSParam param && StringUtils.isNotBlank(param.getName())) {
        names.add(param.getName());
      }
    }
    return names;
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
      Map<String, List<String>> controlPropsByField) {
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
      Map<String, List<String>> controlPropsByField) {
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
            List<String> propNames = controlPropertyNames(control);
            if (!propNames.isEmpty()) {
              controlPropsByField.put(fieldRef, propNames);
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
   * PUT save requires a lock already held by this user/session. Does not acquire a lock.
   *
   * @throws IllegalStateException when unlocked or locked by another user/session (HTTP 409)
   */
  private void requireHeldLock(IPSGuid ctGuid) {
    if (systemDesign == null) {
      throw new IllegalStateException("Could not save content type; design lock required");
    }
    List<PSObjectSummary> locked;
    try {
      locked = systemDesign.isLocked(Collections.singletonList(ctGuid), currentUser());
    } catch (PSErrorResultsException e) {
      if (isNotFoundError(e)) {
        throw new IllegalStateException("Could not save content type; design lock required", e);
      }
      if (hasLockError(e)) {
        String locker = firstLockLocker(e);
        throw new IllegalStateException(
            locker != null
                ? "Could not save content type; locked by " + locker
                : "Could not save content type; design lock required",
            e);
      }
      throw new IllegalStateException("Could not save content type; design lock required", e);
    }
    PSObjectSummary summary =
        locked == null || locked.isEmpty() ? null : locked.get(0);
    if (summary == null || !summary.isLocked()) {
      throw new IllegalStateException("Could not save content type; design lock required");
    }
    String user = currentUser();
    String session = currentSession();
    PSObjectLockSummary info = summary.getLocked();
    if (!summary.isLockedBy(user)) {
      String locker = info != null ? info.getLocker() : null;
      throw new IllegalStateException(
          locker != null
              ? "Could not save content type; locked by " + locker
              : "Could not save content type; design lock required");
    }
    if (info != null
        && StringUtils.isNotBlank(info.getSession())
        && !session.equals(info.getSession())) {
      throw new IllegalStateException(
          "Could not save content type; locked by " + user + " in another session");
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
          "Admin role required to lock, unlock, or save content types", Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(
          "Admin role required to lock, unlock, or save content types", Response.Status.FORBIDDEN);
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

  private static void requireSessionUserForLock() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new IllegalStateException(
          "Request session/user required for content type design session");
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
      return new PSGuid(PSTypeEnum.NODEDEF, Long.parseLong(idOrName));
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
    if (found.size() == 1 && found.get(0) != null && found.get(0).getGUID() != null) {
      return found.get(0).getGUID();
    }
    return null;
  }

  private void throwLockOrNotFound(PSErrorResultsException e, String idOrName, boolean acquiring) {
    if (hasLockError(e)) {
      String locker = firstLockLocker(e);
      String verb = acquiring ? "acquire" : "release";
      String msg =
          locker != null
              ? "Could not " + verb + " design lock for content type; locked by " + locker
              : "Could not " + verb + " design lock for content type";
      throw new IllegalStateException(msg, e);
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
      if (err != null && StringUtils.containsIgnoreCase(String.valueOf(err), "lock")) {
        return true;
      }
    }
    return false;
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
