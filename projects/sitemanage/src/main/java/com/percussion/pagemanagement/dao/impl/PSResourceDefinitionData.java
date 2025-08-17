// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.dao.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.pagemanagement.data.IPSResourceDefinitionVisitor;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFileResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFolderResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.data.PSThemeResource;
import com.percussion.share.service.exception.PSDataServiceException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * A container to hold resource definition data in memory. Hash Maps are used for performance
 * instead of other collections.
 *
 * @author adamgent
 */
@Component("resourceDefinitionData")
@Lazy
public class PSResourceDefinitionData {
  private final Map<PSResourceDefinitionUniqueId, PSResourceDefinition> resourceDefinitions =
      new HashMap<>();
  private final Map<String, PSResourceDefinitionGroup> resourceDefinitionGroups = new HashMap<>();
  private final Map<String, PSAssetResource> primaryAssetResources = new HashMap<>();
  private final Map<String, Set<PSAssetResource>> contentTypeAssetResources = new HashMap<>();
  private final Map<String, Set<PSAssetResource>> legacyTemplateAssetResources = new HashMap<>();
  private final IPSResourceDefinitionVisitor resourceVisitor = new ResourceVisitor();

  public void add(PSResourceDefinitionGroup group) throws PSDataServiceException {
    notNull(group);
    notEmpty(group.getId());
    resourceDefinitionGroups.put(group.getId(), group);
    var rds = new ArrayList<PSResourceDefinition>();
    add(rds, group.getAssetResources());
    add(rds, group.getFileResources());
    add(rds, group.getFolderResources());
    for (var rd : rds) {
      var uid = new PSResourceDefinitionUniqueId(group.getId(), rd.getId());
      rd.setGroupId(uid.getGroupId());
      rd.setId(uid.getLocalId());
      rd.setUniqueId(uid.getUniqueId());
      resourceDefinitions.put(uid, rd);
      rd.accept(resourceVisitor);
    }
  }

  private void add(
      Collection<PSResourceDefinition> merged, Collection<? extends PSResourceDefinition> add) {
    if (add != null) {
      merged.addAll(add);
    }
  }

  /**
   * Returns resources marked as primary where the key is the content type.
   *
   * @return ContentTypeName ==> AssetResource map, never {@code null}.
   */
  public Map<String, PSAssetResource> getPrimaryAssetResources() {
    return primaryAssetResources;
  }

  /**
   * Legacy templates associated to resources. Key is the legacy template name and value is a set of
   * assets with that legacy template.
   *
   * @return LegacyTemplateName ==> Set of Asset resources.
   */
  public Map<String, Set<PSAssetResource>> getLegacyTemplateAssetResources() {
    return legacyTemplateAssetResources;
  }

  /**
   * Resource map where the key is the content type.
   *
   * @return never {@code null}.
   */
  public Map<String, Set<PSAssetResource>> getContentTypeAssetResources() {
    return contentTypeAssetResources;
  }

  public Map<PSResourceDefinitionUniqueId, PSResourceDefinition> getResourceDefinitions() {
    return resourceDefinitions;
  }

  public Map<String, PSResourceDefinitionGroup> getResourceDefinitionGroups() {
    return resourceDefinitionGroups;
  }

  protected class ResourceVisitor implements IPSResourceDefinitionVisitor {
    @Override
    public void visit(PSAssetResource resource) {
      var ct = resource.getContentType();
      if (isBlank(ct)) {
        log.error("Content type is null for resource: {}", resource);
        return;
      }
      var template = resource.getLegacyTemplate();

      // Add content type asset resources associations.
      var ars = contentTypeAssetResources.getOrDefault(ct, new HashSet<>());
      ars.add(resource);
      contentTypeAssetResources.put(ct, ars);

      // Add to primary asset resource associations.
      if (resource.isPrimary()) {
        if (ct != null) {
          primaryAssetResources.put(ct, resource);
        }
      }

      // Add to template asset resource associations.
      if (template != null) {
        var trs = legacyTemplateAssetResources.getOrDefault(template, new HashSet<>());
        trs.add(resource);
        legacyTemplateAssetResources.put(template, trs);
      }
    }

    @Override
    public void visit(@SuppressWarnings("unused") PSFileResource resource) {
      // No-op for file resources.
    }

    @Override
    public void visit(@SuppressWarnings("unused") PSFolderResource resource) {
      // No-op for folder resources.
    }

    @Override
    public void visit(@SuppressWarnings("unused") PSThemeResource resource) {
      // No-op for theme resources.
    }
  }

  /** The log instance to use for this class, never {@code null}. */
  private static final Logger log = LogManager.getLogger(PSResourceDefinitionData.class);
}
