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
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSFieldTranslation;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.rest.contenttypes.ContentTypeFilter;
import com.percussion.rest.contenttypes.IContentTypesAdaptor;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.data.PSContentTypeWorkflow;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@PSSiteManageBean
public class ContentTypeAdaptor implements IContentTypesAdaptor {

  private static final Logger log = LogManager.getLogger(ContentTypeAdaptor.class);

  private final IPSContentDesignWs designSvc;
  private final PSItemDefManager itemDefManager;

  public ContentTypeAdaptor() {
    designSvc = PSContentWsLocator.getContentDesignWebservice();
    itemDefManager = PSItemDefManager.getInstance();
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
          "Failed to load content type ("
              + e.getClass().getName()
              + "): "
              + e.getMessage(),
          e);
    }
  }

  private PSItemDefinition resolveItemDef(String idOrName)
      throws PSInvalidContentTypeException {
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
    boolean controlsResolved = mapControls(def, controlByField);
    List<ContentTypeField> fields = new ArrayList<>();
    List<String> childSets = new ArrayList<>();

    PSFieldSet parentFs = def.getFieldSet();
    if (parentFs != null) {
      addFieldsFromSet(parentFs, null, controlByField, fields);
      for (PSFieldSet child : def.getComplexChildren()) {
        if (child != null && StringUtils.isNotBlank(child.getName())) {
          childSets.add(child.getName());
          addFieldsFromSet(child, child.getName(), controlByField, fields);
        }
      }
    }
    detail.setFields(fields);
    detail.setChildFieldSets(childSets);

    IPSGuid ctGuid = new PSGuid(PSTypeEnum.NODEDEF, def.getTypeId());
    int defaultWfId = def.getContentEditor() != null ? def.getContentEditor().getWorkflowId() : -1;
    detail.setAllowedWorkflows(loadWorkflows(ctGuid, defaultWfId));
    if (defaultWfId > 0) {
      detail.setDefaultWorkflow(toWorkflowRef(defaultWfId, true));
    }
    detail.setAllowedTemplates(loadTemplates(ctGuid));

    List<String> gaps = new ArrayList<>();
    gaps.add(
        "Field rule flags are exposed (validation/visibility/transforms present); full rule"
            + " expressions and control properties are not");
    gaps.add("Item-level pre/post exits not exposed");
    gaps.add("Create / update / delete / lock not supported (read-only)");
    gaps.add("Shared/system field inclusion editing not supported");
    gaps.add("Workflow/template associations are read-only (no add/remove via this API)");
    if (!controlsResolved) {
      gaps.add("Display control/label resolution failed for this content type");
    }
    detail.setDesignGaps(gaps);
    return detail;
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
      PSWorkflow wf =
          wfSvc.findWorkflow(g).orElse(null);
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
      f.setLabel(controlByField.containsKey(field.getSubmitName() + ":label")
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

  /**
   * Package-visible for unit tests. True when a field translation has a non-empty call set.
   */
  static boolean hasTranslation(PSFieldTranslation translation) {
    return translation != null
        && translation.getTranslations() != null
        && !translation.getTranslations().isEmpty();
  }

  /**
   * Walk parent display mapper for control names and labels keyed by field ref.
   *
   * @return {@code true} when the display mapper was walked successfully; {@code false} when
   *     resolution failed (caller should surface a design gap).
   */
  private boolean mapControls(PSItemDefinition def, Map<String, String> map) {
    try {
      PSContentEditorPipe pipe = (PSContentEditorPipe) def.getContentEditor().getPipe();
      PSDisplayMapper dmapper = pipe.getMapper().getUIDefinition().getDisplayMapper();
      walkDisplayMapper(dmapper, map);
      return true;
    } catch (Exception e) {
      log.warn(
          "Could not resolve display controls for {}: {}", def.getName(), e.getMessage(), e);
      return false;
    }
  }

  private void walkDisplayMapper(PSDisplayMapper dmapper, Map<String, String> map) {
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
          }
        }
      }
      if (entry.getDisplayMapper() != null) {
        walkDisplayMapper(entry.getDisplayMapper(), map);
      }
    }
  }
}
