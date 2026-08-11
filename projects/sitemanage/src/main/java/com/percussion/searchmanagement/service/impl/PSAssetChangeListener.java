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

// REFACTORED: CP-JAVA11
package com.percussion.searchmanagement.service.impl;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.IPSEditorChangeListener;
import com.percussion.cms.PSEditorChangeEvent;
import com.percussion.cms.handlers.PSContentEditorHandler;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.searchmanagement.service.IPSPageIndexService;
import com.percussion.server.IPSHandlerInitListener;
import com.percussion.server.IPSRequestHandler;
import com.percussion.server.PSServer;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** Listens for changes on pages, templates, or shared assets and triggers re-indexing. */
@PSSiteManageBean("assetChangeListener")
public class PSAssetChangeListener implements IPSEditorChangeListener, IPSHandlerInitListener {
  private static final Logger log = LogManager.getLogger(PSAssetChangeListener.class);

  private final IPSWorkflowHelper workflowHelper;
  private final IPSWidgetAssetRelationshipService widgetAssetRelationshipService;
  private final IPSIdMapper idMapper;
  private final IPSPageIndexService pageIndexService;

  /**
   * Intentional publish-to-registry of {@code this} as a server init listener. Justified {@code
   * this-escape} suppress: registration must occur when the Spring bean is created.
   */
  @SuppressWarnings("this-escape")
  @Autowired
  public PSAssetChangeListener(
      IPSWorkflowHelper workflowHelper,
      IPSWidgetAssetRelationshipService widgetAssetRelationshipService,
      IPSIdMapper idMapper,
      IPSPageIndexService indexService) {
    this.workflowHelper = workflowHelper;
    this.widgetAssetRelationshipService = widgetAssetRelationshipService;
    this.idMapper = idMapper;
    this.pageIndexService = indexService;
    PSServer.addInitListener(this);
  }

  /** Notifies listeners when a page, template, or shared asset changes. */
  @Override
  public void editorChanged(PSEditorChangeEvent changeEvent) throws PSValidationException {
    if (changeEvent.getActionType() == PSEditorChangeEvent.ACTION_DELETE) {
      return;
    }

    var contentId = changeEvent.getContentId();
    // use the interface type so we can reassign from other implementations
    Set<Integer> pageContentIds = new HashSet<>();
    var myGuid = PSGuidUtils.makeGuid(contentId, PSTypeEnum.LEGACY_CONTENT);
    var myGuidStr = idMapper.getString(myGuid);

    if (workflowHelper.isTemplate(myGuidStr)) {
      pageContentIds.add(myGuid.getUUID());
    }

    try {
      if (workflowHelper.isAsset(myGuidStr)) {
        if (changeEvent.getActionType() == PSEditorChangeEvent.ACTION_INSERT
            || changeEvent.getActionType() == PSEditorChangeEvent.ACTION_UPDATE) {
          pageContentIds = getAssetOwners(myGuidStr);
        }
      }
    } catch (PSNotFoundException e) {
      log.error("Error notifying listeners for asset change with id: {}", myGuidStr, e);
    }

    if (!pageContentIds.isEmpty()) {
      pageIndexService.index(pageContentIds);
    }
  }

  @Override
  public void initHandler(IPSRequestHandler requestHandler) {
    if (requestHandler instanceof PSContentEditorHandler ceh) {
      ceh.addEditorChangeListener(this);
    }
  }

  @Override
  public void shutdownHandler(IPSRequestHandler requestHandler) {
    // No-op
  }

  /** Finds owners of the provided assetId and returns set of content ids of the owners. */
  private Set<Integer> getAssetOwners(String assetId) {
    var owners = widgetAssetRelationshipService.getRelationshipOwners(assetId);
    return owners.stream()
        .map(owner -> idMapper.getGuid(owner).getUUID())
        .collect(Collectors.toSet());
  }
}
