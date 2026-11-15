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
 * Behavioral coverage for the decomposed
 * {@code PSRecycleService#createRootSiteDeleteRelationship} mirror chain (#6360). The
 * decomposition must preserve the legacy link behavior: Sites/Assets paths rebuild the
 * Recycling backbone plus the per-folder recycled edges, while Recycling-root and
 * unrecognized paths stay a no-op (the latter previously fell through to a null-owner
 * dereference).
 */
@ExtendWith(MockitoExtension.class)
class PSRecycleServiceRootDeleteRelationshipTest {

  private static final String RECYCLED = PSRelationshipConfig.TYPE_RECYCLED_CONTENT;
  private static final String FOLDER = PSRelationshipConfig.TYPE_FOLDER_CONTENT;
  private static final int SITE_ID = 900;
  private static final int FOLDER_CONTENT_ID = 250;

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
  void sitesPath_linksChainAndBackbone() throws Exception {
    IPSGuid folderGuid = new PSLegacyGuid(FOLDER_CONTENT_ID, 1);
    String sitesPath = "//Sites/Demo";
    List<IPSGuid> pathids =
        List.of(
            new PSLegacyGuid(PSFolder.SYS_SITES_ID, -1),
            new PSLegacyGuid(SITE_ID, -1),
            new PSLegacyGuid(FOLDER_CONTENT_ID, -1));

    PSRelationship folderRel = stubEmptyFolderRecycle(folderGuid, sitesPath, pathids);

    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    when(recycledConfig.getName()).thenReturn(RECYCLED);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getRelationshipList(any())).thenReturn(Collections.emptyList());
    // updateRelationshipConfigId path: relationship already exists → no save there
    when(processor.checkIfRelationshipAlreadyExists(any())).thenReturn(folderRel);

    try (MockedStatic<PSFolderRelationshipCache> cacheStatic =
            mockStatic(PSFolderRelationshipCache.class);
        MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      wireStatics(cacheStatic, cmdStatic, procStatic, recycledConfig, processor);

      service.recycleFolder(folderGuid);
    }

    List<PSRelationship> saved = allSavedRelationships();
    assertTrue(
        hasEdge(saved, PSFolder.SYS_RECYCLING_ID, PSFolder.SYS_SITES_ID),
        "sites recycle must re-create the Recycling -> Sites backbone edge");
    assertTrue(
        hasEdge(saved, PSFolder.SYS_SITES_ID, SITE_ID),
        "sites recycle must link Sites root -> site");
    assertTrue(
        hasEdge(saved, SITE_ID, FOLDER_CONTENT_ID),
        "sites recycle must link site -> recycled folder");
  }

  @Test
  void assetsPath_linksChainAndBackbone() throws Exception {
    IPSGuid folderGuid = new PSLegacyGuid(FOLDER_CONTENT_ID, 1);
    String assetsPath = PSRecycleService.ASSETS_ROOT + "/qa-chain";
    List<IPSGuid> pathids =
        List.of(
            new PSLegacyGuid(PSFolder.SYS_FOLDERS_ID, -1),
            new PSLegacyGuid(PSFolder.SYS_SYSTEM_FOLDER_ID, -1),
            new PSLegacyGuid(PSFolder.SYS_ASSETS_ID, -1),
            new PSLegacyGuid(FOLDER_CONTENT_ID, -1));

    PSRelationship folderRel = stubEmptyFolderRecycle(folderGuid, assetsPath, pathids);

    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    when(recycledConfig.getName()).thenReturn(RECYCLED);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);
    when(processor.getRelationshipList(any())).thenReturn(Collections.emptyList());
    when(processor.checkIfRelationshipAlreadyExists(any())).thenReturn(folderRel);

    try (MockedStatic<PSFolderRelationshipCache> cacheStatic =
            mockStatic(PSFolderRelationshipCache.class);
        MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      wireStatics(cacheStatic, cmdStatic, procStatic, recycledConfig, processor);

      service.recycleFolder(folderGuid);
    }

    List<PSRelationship> saved = allSavedRelationships();
    assertTrue(
        hasEdge(saved, PSFolder.SYS_RECYCLING_ID, PSFolder.SYS_ASSETS_ID),
        "assets recycle must re-create the Recycling -> Assets backbone edge");
    assertTrue(
        hasEdge(saved, PSFolder.SYS_ASSETS_ID, FOLDER_CONTENT_ID),
        "assets recycle must link Assets root -> recycled folder");
  }

  @Test
  void recyclingRootPath_isNoOp() throws Exception {
    IPSGuid folderGuid = new PSLegacyGuid(FOLDER_CONTENT_ID, 1);
    String recyclingPath = PSRecycleService.RECYCLING_ROOT + "/my-item";
    List<IPSGuid> pathids =
        List.of(
            new PSLegacyGuid(PSFolder.SYS_RECYCLING_ID, -1),
            new PSLegacyGuid(FOLDER_CONTENT_ID, -1));

    stubEmptyFolderRecycle(folderGuid, recyclingPath, pathids);

    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);

    try (MockedStatic<PSFolderRelationshipCache> cacheStatic =
            mockStatic(PSFolderRelationshipCache.class);
        MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      wireStatics(cacheStatic, cmdStatic, procStatic, recycledConfig, processor);

      // Must not throw and must not query or mirror any recycle relationships.
      service.recycleFolder(folderGuid);
    }

    verify(processor, never()).getRelationshipList(any());
  }

  @Test
  void unrecognizedPrefix_isNoOpWithoutQueries() throws Exception {
    IPSGuid folderGuid = new PSLegacyGuid(FOLDER_CONTENT_ID, 1);
    // Separator-free fixture: no filesystem or CMS repository path is ever
    // constructed from this value (findPathIds is mocked); it only exercises
    // the unrecognized-prefix early-out.
    String unknownPath = "unknown-prefix-path";
    List<IPSGuid> pathids =
        List.of(new PSLegacyGuid(FOLDER_CONTENT_ID, -1), new PSLegacyGuid(4242, -1));

    stubEmptyFolderRecycle(folderGuid, unknownPath, pathids);

    PSRelationshipConfig recycledConfig = mock(PSRelationshipConfig.class);
    PSRelationshipProcessor processor = mock(PSRelationshipProcessor.class);

    try (MockedStatic<PSFolderRelationshipCache> cacheStatic =
            mockStatic(PSFolderRelationshipCache.class);
        MockedStatic<PSRelationshipCommandHandler> cmdStatic =
            mockStatic(PSRelationshipCommandHandler.class);
        MockedStatic<PSRelationshipProcessor> procStatic =
            mockStatic(PSRelationshipProcessor.class)) {
      wireStatics(cacheStatic, cmdStatic, procStatic, recycledConfig, processor);

      // Legacy code fell through with a null owner and dereferenced it; the
      // decomposed chain returns early instead.
      service.recycleFolder(folderGuid);
    }

    verify(processor, never()).getRelationshipList(any());
  }

  /** Stubs an empty-folder recycle so the flow reaches the root-delete relationship step. */
  private PSRelationship stubEmptyFolderRecycle(
      IPSGuid folderGuid, String folderPath, List<IPSGuid> pathids) throws Exception {
    PSLocator dependent = new PSLocator(FOLDER_CONTENT_ID, 1);
    PSRelationshipConfig folderConfig = mock(PSRelationshipConfig.class);
    when(folderConfig.getName()).thenReturn(FOLDER);

    PSRelationship folderRel = mock(PSRelationship.class);
    lenient().when(folderRel.getDependent()).thenReturn(dependent);
    lenient()
        .when(folderRel.getOwner())
        .thenReturn(new PSLocator(PSFolder.SYS_ASSETS_ID, -1));
    when(folderRel.getConfig()).thenReturn(folderConfig);
    lenient().when(folderRel.getId()).thenReturn(7001);

    when(idMapper.getContentId(folderGuid)).thenReturn(FOLDER_CONTENT_ID);

    PSDataItemSummary summary = new PSDataItemSummary();
    summary.setFolderPaths(List.of(folderPath));
    when(itemSummaryService.find(folderGuid.toString(), FOLDER)).thenReturn(summary);
    when(contentWs.findPathIds(folderPath)).thenReturn(pathids);

    PSFolderRelationshipCache cache = mock(PSFolderRelationshipCache.class);
    when(cache.getChildren(any(PSLocator.class), any())).thenReturn(Collections.emptyList());
    when(systemWs.loadRelationships(any())).thenReturn(List.of(folderRel));

    // FOLDER_CONTENT_ID < 300 → updateParentFolders early-out; stash cache for static wiring.
    pendingCache = cache;
    return folderRel;
  }

  private PSFolderRelationshipCache pendingCache;

  private void wireStatics(
      MockedStatic<PSFolderRelationshipCache> cacheStatic,
      MockedStatic<PSRelationshipCommandHandler> cmdStatic,
      MockedStatic<PSRelationshipProcessor> procStatic,
      PSRelationshipConfig recycledConfig,
      PSRelationshipProcessor processor) {
    cacheStatic.when(PSFolderRelationshipCache::getInstance).thenReturn(pendingCache);
    cmdStatic
        .when(() -> PSRelationshipCommandHandler.getRelationshipConfig(any()))
        .thenReturn(recycledConfig);
    procStatic.when(PSRelationshipProcessor::getInstance).thenReturn(processor);
  }

  private List<PSRelationship> allSavedRelationships() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSRelationship>> captor = ArgumentCaptor.forClass(List.class);
    verify(systemWs, Mockito.atLeastOnce()).saveRelationships(captor.capture());
    return captor.getAllValues().stream().flatMap(List::stream).toList();
  }

  private static boolean hasEdge(List<PSRelationship> saved, int ownerId, int dependentId) {
    return saved.stream()
        .anyMatch(
            r ->
                r.getOwner() != null
                    && r.getDependent() != null
                    && r.getOwner().getId() == ownerId
                    && r.getDependent().getId() == dependentId
                    && r.getConfig() != null
                    && RECYCLED.equals(r.getConfig().getName()));
  }

  private static <T> MockedStatic<T> mockStatic(Class<T> cls) {
    return Mockito.mockStatic(cls);
  }
}
