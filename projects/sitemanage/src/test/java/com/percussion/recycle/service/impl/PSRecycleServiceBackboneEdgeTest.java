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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.cms.objectstore.PSFolder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Recycling backbone repair: recycles must re-create the structural Recycling -&gt; Sites /
 * Assets recycled edge when it is missing (#6307). Without the edge, recycled content dangles
 * outside the bin tree (invisible listings, 409/400 on purge/restore).
 */
@ExtendWith(MockitoExtension.class)
class PSRecycleServiceBackboneEdgeTest {

  private static final String RECYCLED = PSRelationshipConfig.TYPE_RECYCLED_CONTENT;

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
  void ensureBackboneEdge_createsMissingAssetsEdge() throws Exception {
    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getRelationshipList(any())).thenReturn(Collections.emptyList());

    try (MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      cmdStatic
          .when(() -> PSRelationshipCommandHandler.getRelationshipConfig(RECYCLED))
          .thenReturn(recycledConfig);
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);

      service.ensureRecycledBackboneEdge(PSFolder.SYS_ASSETS_ID);
    }

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSRelationship>> captor = ArgumentCaptor.forClass(List.class);
    verify(systemWs).saveRelationships(captor.capture());
    List<PSRelationship> saved = captor.getValue();
    assertEquals(1, saved.size());
    assertEquals(PSFolder.SYS_RECYCLING_ID, saved.get(0).getOwner().getId());
    assertEquals(PSFolder.SYS_ASSETS_ID, saved.get(0).getDependent().getId());
    assertSame(recycledConfig, saved.get(0).getConfig());
  }

  @Test
  void ensureBackboneEdge_skipsExistingEdge() throws Exception {
    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    when(recycledConfig.getName()).thenReturn(RECYCLED);
    PSRelationship existing = mock(PSRelationship.class);
    when(existing.getConfig()).thenReturn(recycledConfig);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getRelationshipList(any())).thenReturn(List.of(existing));

    try (MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      cmdStatic
          .when(() -> PSRelationshipCommandHandler.getRelationshipConfig(RECYCLED))
          .thenReturn(recycledConfig);
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);

      service.ensureRecycledBackboneEdge(PSFolder.SYS_SITES_ID);
    }

    verify(systemWs, never()).saveRelationships(anyList());
  }

  @Test
  void ensureBackboneEdge_recreatesWhenOnlyFolderEdgeExists() throws Exception {
    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    when(recycledConfig.getName()).thenReturn(RECYCLED);
    PSRelationshipConfig folderConfig = mock(PSRelationshipConfig.class);
    when(folderConfig.getName()).thenReturn(PSRelationshipConfig.TYPE_FOLDER_CONTENT);
    PSRelationship folderRel = mock(PSRelationship.class);
    when(folderRel.getConfig()).thenReturn(folderConfig);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getRelationshipList(any())).thenReturn(List.of(folderRel));

    try (MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      cmdStatic
          .when(() -> PSRelationshipCommandHandler.getRelationshipConfig(RECYCLED))
          .thenReturn(recycledConfig);
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);

      service.ensureRecycledBackboneEdge(PSFolder.SYS_ASSETS_ID);
    }

    verify(systemWs).saveRelationships(anyList());
  }

  @Test
  void recycleFolder_recreatesMissingAssetsBackbone() throws Exception {
    int folderId = 5001;
    String folderPath = "//Folders/$System$/Assets/qa-backbone";
    IPSGuid folderGuid = new PSLegacyGuid(folderId, 1);
    PSLocator dependent = new PSLocator(folderId, -1);

    PSRelationshipConfig folderConfig = mock(PSRelationshipConfig.class);
    when(folderConfig.getName()).thenReturn(PSRelationshipConfig.TYPE_FOLDER_CONTENT);
    PSRelationship folderRel = mock(PSRelationship.class);
    lenient().when(folderRel.getDependent()).thenReturn(dependent);
    lenient().when(folderRel.getOwner()).thenReturn(new PSLocator(PSFolder.SYS_ASSETS_ID, -1));
    when(folderRel.getConfig()).thenReturn(folderConfig);
    lenient().when(folderRel.getId()).thenReturn(7001);

    when(idMapper.getContentId(folderGuid)).thenReturn(folderId);
    PSDataItemSummary summary = new PSDataItemSummary();
    summary.setFolderPaths(List.of(folderPath));
    when(itemSummaryService.find(folderGuid.toString(), PSRelationshipConfig.TYPE_FOLDER_CONTENT))
        .thenReturn(summary);
    when(contentWs.findPathIds(folderPath))
        .thenReturn(
            List.of(
                new PSLegacyGuid(PSFolder.SYS_FOLDERS_ID, -1),
                new PSLegacyGuid(PSFolder.SYS_SYSTEM_FOLDER_ID, -1),
                new PSLegacyGuid(PSFolder.SYS_ASSETS_ID, -1),
                new PSLegacyGuid(folderId, -1)));

    PSFolderRelationshipCache cache = mock(PSFolderRelationshipCache.class);
    when(cache.getChildren(any(PSLocator.class), any())).thenReturn(Collections.emptyList());
    when(systemWs.loadRelationships(any())).thenReturn(List.of(folderRel));

    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    when(recycledConfig.getName()).thenReturn(RECYCLED);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    // No relationships anywhere: backbone missing, no mirror chain yet.
    when(processor.getRelationshipList(any())).thenReturn(Collections.emptyList());
    PSRelationship alreadyThere = mock(PSRelationship.class);
    when(processor.checkIfRelationshipAlreadyExists(any())).thenReturn(alreadyThere);

    try (MockedStatic<PSFolderRelationshipCache> cacheStatic =
            mockStatic(PSFolderRelationshipCache.class);
        MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      cacheStatic.when(PSFolderRelationshipCache::getInstance).thenReturn(cache);
      cmdStatic
          .when(() -> PSRelationshipCommandHandler.getRelationshipConfig(RECYCLED))
          .thenReturn(recycledConfig);
      procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);

      service.recycleFolder(folderGuid);
    }

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSRelationship>> captor = ArgumentCaptor.forClass(List.class);
    verify(systemWs, Mockito.atLeastOnce()).saveRelationships(captor.capture());
    boolean backbone =
        captor.getAllValues().stream()
            .flatMap(List::stream)
            .anyMatch(
                r ->
                    r.getOwner().getId() == PSFolder.SYS_RECYCLING_ID
                        && r.getDependent().getId() == PSFolder.SYS_ASSETS_ID
                        && RECYCLED.equals(r.getConfig().getName()));
    assertTrue(backbone, "recycle must re-create the missing Recycling -> Assets backbone edge");
  }

  private static <T> MockedStatic<T> mockStatic(Class<T> cls) {
    return Mockito.mockStatic(cls);
  }
}
