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

package com.percussion.apibridge;

import com.percussion.design.objectstore.PSConditionalEffect;
import com.percussion.design.objectstore.PSEntry;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.rest.Guid;
import com.percussion.rest.relationshiptypes.IRelationshipTypeAdaptor;
import com.percussion.rest.relationshiptypes.RelationshipType;
import com.percussion.rest.relationshiptypes.RelationshipTypeEffect;
import com.percussion.rest.relationshiptypes.RelationshipTypeProperty;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.system.IPSSystemDesignWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;

/** Design catalog adaptor for relationship types (SY-03 read). */
@PSSiteManageBean
@Lazy
public class RelationshipTypeAdaptor implements IRelationshipTypeAdaptor {

  private static final Logger log = LogManager.getLogger(RelationshipTypeAdaptor.class);

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Relationship type create / update / delete not supported via this API",
          "Cloning field override editor not supported via this API",
          "Effect condition and execution-context edit not supported via this API");

  private final IPSSystemDesignWs designWs;

  public RelationshipTypeAdaptor() {
    this(PSSystemWsLocator.getSystemDesignWebservice());
  }

  /** Package-visible for unit tests. */
  RelationshipTypeAdaptor(IPSSystemDesignWs designWs) {
    this.designWs = designWs;
  }

  @Override
  public List<RelationshipType> listRelationshipTypes() {
    try {
      List<IPSCatalogSummary> summaries = designWs.findRelationshipTypes(null, null);
      if (summaries == null || summaries.isEmpty()) {
        return List.of();
      }
      List<IPSGuid> guids = new ArrayList<>();
      for (IPSCatalogSummary s : summaries) {
        if (s != null && s.getGUID() != null) {
          guids.add(s.getGUID());
        }
      }
      if (guids.isEmpty()) {
        return List.of();
      }
      String user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
      String session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
      List<PSRelationshipConfig> configs =
          designWs.loadRelationshipTypes(guids, false, false, session, user);
      List<RelationshipType> out = new ArrayList<>();
      if (configs != null) {
        for (PSRelationshipConfig cfg : configs) {
          if (cfg != null) {
            out.add(copyConfig(cfg));
          }
        }
      }
      return out;
    } catch (PSErrorException | PSErrorResultsException e) {
      throw new RuntimeException("Failed to list relationship types", e);
    }
  }

  @Override
  public RelationshipType findRelationshipType(String idOrName) {
    if (!isSafeRelationshipTypeKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    for (RelationshipType t : listRelationshipTypes()) {
      if (t == null) {
        continue;
      }
      if (key.equalsIgnoreCase(t.getName())) {
        return t;
      }
      if (t.getGuid() != null) {
        String guidStr = t.getGuid().getStringValue().orElse(null);
        if (guidStr != null && key.equalsIgnoreCase(guidStr)) {
          return t;
        }
      }
    }
    try {
      var guid = new com.percussion.services.guidmgr.data.PSGuid(key);
      for (RelationshipType t : listRelationshipTypes()) {
        if (t != null
            && t.getGuid() != null
            && t.getGuid().getUuid() == guid.getUUID()
            && t.getGuid().getType() == guid.getType()) {
          return t;
        }
      }
    } catch (IllegalArgumentException e) {
      log.debug("Invalid relationship type GUID syntax: {}", e.getMessage());
    }
    return null;
  }

  private RelationshipType copyConfig(PSRelationshipConfig cfg) {
    RelationshipType ret = new RelationshipType();
    ret.setName(cfg.getName());
    ret.setLabel(cfg.getLabel());
    ret.setDescription(cfg.getDescription());
    ret.setType(cfg.getType());
    ret.setCategory(cfg.getCategory());
    ret.setCategoryLabel(categoryLabel(cfg.getCategory()));
    ret.setSystemType(cfg.isSystem());
    ret.setUserType(cfg.isUser());
    ret.setAllowCloning(cfg.isCloningAllowed());
    ret.setUseOwnerRevision(cfg.useOwnerRevision());
    ret.setUseDependentRevision(cfg.useDependentRevision());
    if (cfg.isAssinedId()) {
      ret.setGuid(copyGuid(cfg.getGUID()));
    }

    List<RelationshipTypeEffect> effects = new ArrayList<>();
    Iterator<?> effectIt = cfg.getEffects();
    while (effectIt.hasNext()) {
      Object o = effectIt.next();
      if (o instanceof PSConditionalEffect ce) {
        effects.add(copyEffect(ce));
      }
    }
    ret.setEffects(effects);

    ret.setSystemProperties(mapToProps(cfg.getSystemProperties()));
    ret.setUserProperties(mapToProps(cfg.getUserProperties()));
    ret.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return ret;
  }

  private RelationshipTypeEffect copyEffect(PSConditionalEffect ce) {
    RelationshipTypeEffect e = new RelationshipTypeEffect();
    e.setActivationEndPoint(ce.getActivationEndPoint());
    PSExtensionCall call = ce.getEffect();
    if (call != null) {
      e.setName(call.getName());
      if (call.getExtensionRef() != null) {
        e.setExtensionRef(call.getExtensionRef().toString());
      }
    }
    return e;
  }

  private static List<RelationshipTypeProperty> mapToProps(Map<String, String> map) {
    List<RelationshipTypeProperty> out = new ArrayList<>();
    if (map == null) {
      return out;
    }
    for (Map.Entry<String, String> entry : map.entrySet()) {
      out.add(new RelationshipTypeProperty(entry.getKey(), entry.getValue()));
    }
    return out;
  }

  private static String categoryLabel(String category) {
    if (category == null || category.isBlank()) {
      return null;
    }
    for (PSEntry entry : PSRelationshipConfig.CATEGORY_ENUM) {
      if (entry != null && category.equals(entry.getValue())) {
        if (entry.getLabel() != null && entry.getLabel().getText() != null) {
          return entry.getLabel().getText();
        }
      }
    }
    return category;
  }

  private Guid copyGuid(IPSGuid guid) {
    Guid g = new Guid();
    g.setHostId(guid.getHostId());
    g.setLongValue(guid.longValue());
    g.setStringValue(guid.toString());
    g.setType(guid.getType());
    g.setUuid(guid.getUUID());
    g.setUntypedString(guid.toStringUntyped());
    return g;
  }

  /**
   * Single path component / guid token only — reject traversal and separators ({@code
   * java/path-injection}).
   */
  static boolean isSafeRelationshipTypeKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }
}
