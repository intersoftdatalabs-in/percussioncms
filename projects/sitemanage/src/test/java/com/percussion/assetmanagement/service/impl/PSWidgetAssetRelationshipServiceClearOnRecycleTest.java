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
package com.percussion.assetmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.service.IPSWidgetAssetRelationshipDao;
import com.percussion.pagemanagement.service.IPSWidgetService;
import com.percussion.searchmanagement.service.IPSPageIndexService;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSNameGenerator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link
 * PSWidgetAssetRelationshipService#clearAssetWidgetRelationshipsForAsset(String)} — GH #2238 /
 * parent #777 (clear stale page↔asset widget bindings when an asset is recycled).
 */
@ExtendWith(MockitoExtension.class)
class PSWidgetAssetRelationshipServiceClearOnRecycleTest {

  private static final String ASSET_ID = "1-101-1";
  private static final int ASSET_CONTENT_ID = 101;

  @Mock private IPSAssetDao assetDao;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSSystemWs systemWs;
  @Mock private IPSWidgetService widgetService;
  @Mock private IPSNameGenerator nameGenerator;
  @Mock private IPSContentDesignWs contentDesignWs;
  @Mock private IPSRenderAssemblyBridge renderAssemblyBridge;
  @Mock private IPSWorkflowHelper workflowHelper;
  @Mock private IPSPageIndexService pageIndexService;
  @Mock private IPSWidgetAssetRelationshipDao widgetAssetRelationshipDao;
  @Mock private IPSCacheAccess ehCache;

  private PSWidgetAssetRelationshipService service;

  @BeforeEach
  void setUp() {
    service =
        new PSWidgetAssetRelationshipService(
            assetDao,
            idMapper,
            systemWs,
            widgetService,
            nameGenerator,
            contentDesignWs,
            renderAssemblyBridge,
            workflowHelper,
            pageIndexService,
            widgetAssetRelationshipDao,
            ehCache);
  }

  @Test
  void clear_deletesSharedNonInlineAndLocalBindings_notAsset() throws Exception {
    stubAssetGuid();

    PSRelationship sharedWidgetRel =
        mockRelationship(false, PSRelationshipConfig.TYPE_ACTIVE_ASSEMBLY);
    PSRelationship localRel = mockRelationship(false, PSRelationshipConfig.TYPE_LOCAL_CONTENT);

    // loadRelationships is called twice (shared filter then local filter).
    // Chain thenReturn to avoid unchecked generic array creation for varargs.
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(List.of(sharedWidgetRel))
        .thenReturn(List.of(localRel));

    int cleared = service.clearAssetWidgetRelationshipsForAsset(ASSET_ID);

    assertEquals(2, cleared);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> guidsCaptor = ArgumentCaptor.forClass(List.class);
    verify(systemWs, times(2)).deleteRelationships(guidsCaptor.capture());
    assertEquals(sharedWidgetRel.getGuid(), guidsCaptor.getAllValues().get(0).get(0));
    assertEquals(localRel.getGuid(), guidsCaptor.getAllValues().get(1).get(0));
    // Must not cascade-delete the asset being recycled
    verify(assetDao, never()).remove(anyString());
  }

  @Test
  void clear_skipsInlineActiveAssembly() throws Exception {
    stubAssetGuid();

    PSRelationship inline = mockRelationship(true, PSRelationshipConfig.TYPE_ACTIVE_ASSEMBLY);
    PSRelationship widget = mockRelationship(false, PSRelationshipConfig.TYPE_ACTIVE_ASSEMBLY);

    // Shared path: both returned from AA filter; service keeps only non-inline
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(List.of(inline, widget))
        .thenReturn(Collections.emptyList());

    int cleared = service.clearAssetWidgetRelationshipsForAsset(ASSET_ID);

    assertEquals(1, cleared);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> guidsCaptor = ArgumentCaptor.forClass(List.class);
    verify(systemWs, times(1)).deleteRelationships(guidsCaptor.capture());
    assertEquals(widget.getGuid(), guidsCaptor.getValue().get(0));
    verify(assetDao, never()).remove(anyString());
  }

  @Test
  void clear_whenNoBindings_isIdempotent() throws Exception {
    stubAssetGuid();
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(Collections.emptyList())
        .thenReturn(Collections.emptyList());

    int cleared = service.clearAssetWidgetRelationshipsForAsset(ASSET_ID);

    assertEquals(0, cleared);
    verify(systemWs, never()).deleteRelationships(anyList());
  }

  @Test
  void clear_whenOnlyShared_deletesOnce() throws Exception {
    stubAssetGuid();

    PSRelationship shared = mockRelationship(false, PSRelationshipConfig.TYPE_ACTIVE_ASSEMBLY);
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(List.of(shared))
        .thenReturn(Collections.emptyList());

    int cleared = service.clearAssetWidgetRelationshipsForAsset(ASSET_ID);

    assertEquals(1, cleared);
    verify(systemWs, times(1)).deleteRelationships(anyList());
    verify(assetDao, never()).remove(anyString());
  }

  @Test
  void clear_blankAssetId_rejected() {
    assertThrows(
        IllegalArgumentException.class, () -> service.clearAssetWidgetRelationshipsForAsset(""));
    // Apache Validate.notEmpty(null) throws NPE (not IAE)
    assertThrows(
        NullPointerException.class, () -> service.clearAssetWidgetRelationshipsForAsset(null));
  }

  @Test
  void clear_loadFailure_throwsServiceException() {
    when(idMapper.getGuid(ASSET_ID)).thenThrow(new RuntimeException("mapper boom"));

    assertThrows(
        PSWidgetAssetRelationshipServiceException.class,
        () -> service.clearAssetWidgetRelationshipsForAsset(ASSET_ID));
  }

  @Test
  void clear_filterUsesDependentContentId() throws Exception {
    stubAssetGuid();
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(Collections.emptyList());

    service.clearAssetWidgetRelationshipsForAsset(ASSET_ID);

    ArgumentCaptor<PSRelationshipFilter> filterCaptor =
        ArgumentCaptor.forClass(PSRelationshipFilter.class);
    verify(systemWs, times(2)).loadRelationships(filterCaptor.capture());
    for (PSRelationshipFilter filter : filterCaptor.getAllValues()) {
      assertEquals(ASSET_CONTENT_ID, filter.getDependent().getId());
    }
  }

  private void stubAssetGuid() {
    PSLegacyGuid assetGuid = new PSLegacyGuid(ASSET_CONTENT_ID, 1);
    when(idMapper.getGuid(ASSET_ID)).thenReturn(assetGuid);
  }

  private static PSRelationship mockRelationship(boolean inline, String configName) {
    PSRelationship rel = mock(PSRelationship.class);
    PSRelationshipConfig config = mock(PSRelationshipConfig.class);
    IPSGuid guid = mock(IPSGuid.class);
    // Shared AA path filters inline; local path does not — lenient for mixed scenarios
    lenient().when(rel.isInlineRelationship()).thenReturn(inline);
    lenient().when(rel.getConfig()).thenReturn(config);
    lenient().when(config.getName()).thenReturn(configName);
    lenient().when(rel.getGuid()).thenReturn(guid);
    if (PSRelationshipConfig.TYPE_LOCAL_CONTENT.equals(configName)) {
      // deleteRelationship indexes the owner page for local content
      lenient().when(rel.getOwner()).thenReturn(new PSLocator(900, 1));
    }
    return rel;
  }
}
