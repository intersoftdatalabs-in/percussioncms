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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.recycle.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException;
import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.server.PSRequest;
import com.percussion.server.cache.PSFolderRelationshipCache;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PSRecycleService#clearWidgetRelationshipsForRecycledItem(IPSGuid)} and the
 * {@link PSRecycleService#recycleItem(int)} → clear hook — GH #2238 / parent #777.
 */
@ExtendWith(MockitoExtension.class)
class PSRecycleServiceClearWidgetRelationshipsTest {

  private static final int DEPENDENT_ID = 101;
  private static final String ASSET_ID = "1-101-1";

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
  void clearWidgetRelationships_invokesWidgetServiceWithMappedId() throws Exception {
    IPSGuid itemGuid = new PSLegacyGuid(101, 1);
    when(idMapper.getString(itemGuid)).thenReturn(ASSET_ID);
    when(workflowHelper.isAsset(ASSET_ID)).thenReturn(true);
    when(widgetAssetRelationshipService.clearAssetWidgetRelationshipsForAsset(ASSET_ID))
        .thenReturn(2);

    service.clearWidgetRelationshipsForRecycledItem(itemGuid);

    verify(widgetAssetRelationshipService).clearAssetWidgetRelationshipsForAsset(ASSET_ID);
  }

  @Test
  void clearWidgetRelationships_nullGuid_isNoOp() throws Exception {
    service.clearWidgetRelationshipsForRecycledItem(null);

    verify(widgetAssetRelationshipService, never())
        .clearAssetWidgetRelationshipsForAsset(anyString());
  }

  @Test
  void clearWidgetRelationships_nonAsset_skipsWidgetService() throws Exception {
    IPSGuid itemGuid = new PSLegacyGuid(202, 1);
    when(idMapper.getString(itemGuid)).thenReturn("1-202-1");
    when(workflowHelper.isAsset("1-202-1")).thenReturn(false);

    service.clearWidgetRelationshipsForRecycledItem(itemGuid);

    verify(widgetAssetRelationshipService, never())
        .clearAssetWidgetRelationshipsForAsset(anyString());
  }

  @Test
  void clearWidgetRelationships_serviceFailure_isSwallowed() throws Exception {
    IPSGuid itemGuid = new PSLegacyGuid(55, 1);
    when(idMapper.getString(itemGuid)).thenReturn("1-55-1");
    when(workflowHelper.isAsset("1-55-1")).thenReturn(true);
    doThrow(new PSWidgetAssetRelationshipServiceException("boom"))
        .when(widgetAssetRelationshipService)
        .clearAssetWidgetRelationshipsForAsset(eq("1-55-1"));

    // Must not throw — recycle should still succeed if clear fails
    service.clearWidgetRelationshipsForRecycledItem(itemGuid);

    verify(widgetAssetRelationshipService).clearAssetWidgetRelationshipsForAsset("1-55-1");
  }

  /**
   * Behavioral coverage: {@link PSRecycleService#recycleItem(int)} must invoke the widget-clear
   * hook after the folder relationship is updated (Kilo review on #2268).
   */
  @Test
  void recycleItem_invokesClearWidgetRelationshipsForAsset() throws Exception {
    IPSGuid itemGuid = new PSLegacyGuid(DEPENDENT_ID, 1);
    PSLocator dependent = new PSLocator(DEPENDENT_ID, 1);
    PSRelationship folderRel = mock(PSRelationship.class);
    when(folderRel.getDependent()).thenReturn(dependent);
    lenient().when(folderRel.getId()).thenReturn(9001);

    when(systemWs.loadRelationships(any())).thenReturn(List.of(folderRel));
    when(idMapper.getGuid(dependent)).thenReturn(itemGuid);
    when(idMapper.getString(itemGuid)).thenReturn(ASSET_ID);
    when(idMapper.getString(dependent)).thenReturn(ASSET_ID);

    PSDataItemSummary summary = mock(PSDataItemSummary.class);
    when(summary.getFolderPaths()).thenReturn(List.of("//Sites/Demo"));
    when(itemSummaryService.find(itemGuid.toString(), PSRelationshipConfig.TYPE_FOLDER_CONTENT))
        .thenReturn(summary);

    // Short path id list → createRootSiteDeleteRelationship returns early
    when(contentWs.findPathIds("//Sites/Demo")).thenReturn(Collections.emptyList());
    // Non-workflowable item → transitionWorkflowItem no-ops
    when(workflowHelper.getTransitions(ASSET_ID)).thenReturn(null);
    // renameIfRequired no-ops on empty load
    when(contentWs.loadItems(
            any(), eq(false), eq(false), eq(false), eq(true), eq(false), anyString()))
        .thenReturn(Collections.emptyList());

    when(workflowHelper.isAsset(ASSET_ID)).thenReturn(true);
    when(widgetAssetRelationshipService.clearAssetWidgetRelationshipsForAsset(ASSET_ID))
        .thenReturn(1);

    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    // Relationship already exists → skip saveRelationships
    when(processor.checkIfRelationshipAlreadyExists(any())).thenReturn(folderRel);

    PSRequest request = mock(PSRequest.class);
    when(request.getServletRequest()).thenReturn(mock(HttpServletRequest.class));

    try (MockedStatic<PSRelationshipCommandHandler> cmd =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> proc = mockStatic(PSRelationshipProcessor.class);
        MockedStatic<PSFolderRelationshipCache> cache =
            mockStatic(PSFolderRelationshipCache.class);
        MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      cmd.when(() -> PSRelationshipCommandHandler.getRelationshipConfig(anyString()))
          .thenReturn(recycledConfig);
      proc.when(PSRelationshipProcessor::getInstance).thenReturn(processor);
      // null cache → skip updateParentFolders
      cache.when(PSFolderRelationshipCache::getInstance).thenReturn(null);
      security.when(PSSecurityFilter::getCurrentRequest).thenReturn(request);

      service.recycleItem(DEPENDENT_ID);
    }

    verify(widgetAssetRelationshipService).clearAssetWidgetRelationshipsForAsset(ASSET_ID);
  }
}
