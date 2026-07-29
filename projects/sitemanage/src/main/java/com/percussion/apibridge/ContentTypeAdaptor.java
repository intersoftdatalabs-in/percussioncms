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
import com.percussion.design.objectstore.PSUISet;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.rest.contenttypes.ContentTypeFilter;
import com.percussion.rest.contenttypes.IContentTypesAdaptor;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import java.net.URI;
import java.util.ArrayList;
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
      throw new RuntimeException("Failed to load content type: " + e.getMessage(), e);
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

    Map<String, String> controlByField = mapControls(def);
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

    List<String> gaps = new ArrayList<>();
    gaps.add("Field validation, visibility, editability, and transform rules not exposed");
    gaps.add("Item-level pre/post exits not exposed");
    gaps.add("Allowed workflows / default workflow not exposed on this endpoint");
    gaps.add("Allowed templates association not exposed on this endpoint");
    gaps.add("Create / update / delete / lock not supported (read-only)");
    gaps.add("Shared/system field inclusion editing not supported");
    detail.setDesignGaps(gaps);
    return detail;
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

  /**
   * Walk parent display mapper for control names and labels keyed by field ref.
   */
  private Map<String, String> mapControls(PSItemDefinition def) {
    Map<String, String> map = new HashMap<>();
    try {
      PSContentEditorPipe pipe = (PSContentEditorPipe) def.getContentEditor().getPipe();
      PSDisplayMapper dmapper = pipe.getMapper().getUIDefinition().getDisplayMapper();
      walkDisplayMapper(dmapper, map);
    } catch (Exception e) {
      log.debug("Could not resolve display controls for {}: {}", def.getName(), e.getMessage());
    }
    return map;
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
