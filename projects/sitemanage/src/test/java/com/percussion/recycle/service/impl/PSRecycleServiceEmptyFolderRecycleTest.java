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
package com.percussion.recycle.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.server.cache.PSFolderRelationshipCache;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Empty-folder recycle must convert the FOLDER relationship to RECYCLED (not delete it), or folders
 * leave Assets/Sites without appearing under Recycling (#2488 residual of #2423 / #2464).
 */
@ExtendWith(MockitoExtension.class)
class PSRecycleServiceEmptyFolderRecycleTest {

  // UUID < 300 so updateParentFolders short-circuits (system-folder guard).
  private static final int FOLDER_CONTENT_ID = 250;
  private static final String FOLDER_PATH = "//Folders/$System$/Assets/qa-empty-folder";

  @Mock private IPSSystemWs systemWs;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSDataItemSummaryService itemSummaryService;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSWorkflowHelper workflowHelper;
  @Mock private IPSManagedNavService navService;
  @Mock private IPSWidgetAssetRelationshipService widgetAssetRelationshipService;

  private PSRecycleService service;

  @BeforeEach
  void setUp() {
    service =
        new PSRecycleService(
            systemWs,
            idMapper,
            itemSummaryService,
            contentWs,
            workflowHelper,
            navService,
            widgetAssetRelationshipService);
  }

  @Test
  void recycleFolder_emptyFolder_convertsRelationshipAndDoesNotDelete() throws Exception {
    IPSGuid folderGuid = new PSLegacyGuid(FOLDER_CONTENT_ID, 1);
    PSLocator dependent = new PSLocator(FOLDER_CONTENT_ID, 1);
    PSRelationshipConfig folderConfig = mock(PSRelationshipConfig.class);
    when(folderConfig.getName()).thenReturn(PSRelationshipConfig.TYPE_FOLDER_CONTENT);

    PSRelationship folderRel = mock(PSRelationship.class);
    lenient().when(folderRel.getDependent()).thenReturn(dependent);
    when(folderRel.getConfig()).thenReturn(folderConfig);
    lenient().when(folderRel.getId()).thenReturn(7001);
    lenient().when(folderRel.getGuid()).thenReturn(new PSLegacyGuid(7001, 1));

    when(idMapper.getContentId(folderGuid)).thenReturn(FOLDER_CONTENT_ID);

    PSDataItemSummary summary = new PSDataItemSummary();
    summary.setFolderPaths(List.of(FOLDER_PATH));
    when(itemSummaryService.find(folderGuid.toString(), PSRelationshipConfig.TYPE_FOLDER_CONTENT))
        .thenReturn(summary);

    // createRootSiteDeleteRelationship early-out when pathids too short
    when(contentWs.findPathIds(FOLDER_PATH)).thenReturn(Collections.emptyList());

    PSFolderRelationshipCache cache = mock(PSFolderRelationshipCache.class);
    when(cache.getChildren(any(PSLocator.class), any())).thenReturn(Collections.emptyList());

    when(systemWs.loadRelationships(any())).thenReturn(List.of(folderRel));

    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.checkIfRelationshipAlreadyExists(any())).thenReturn(null);

    try (MockedStatic<PSFolderRelationshipCache> cacheStatic =
            mockStatic(PSFolderRelationshipCache.class);
        MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      cacheStatic.when(PSFolderRelationshipCache::getInstance).thenReturn(cache);
      cmdStatic
          .when(
              () ->
                  PSRelationshipCommandHandler.getRelationshipConfig(
                      PSRelationshipConfig.TYPE_RECYCLED_CONTENT))
          .thenReturn(recycledConfig);
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);

      service.recycleFolder(folderGuid);
    }

    // Must convert to recycled — not delete the relationship (orphan path).
    verify(systemWs, never()).deleteRelationships(anyList());
    verify(folderRel).setConfig(recycledConfig);
    verify(systemWs).saveRelationships(anyList());
  }
}
