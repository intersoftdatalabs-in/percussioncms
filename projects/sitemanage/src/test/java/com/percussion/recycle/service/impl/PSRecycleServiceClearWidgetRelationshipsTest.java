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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PSRecycleService#clearWidgetRelationshipsForRecycledItem(IPSGuid)} — GH
 * #2238 / parent #777.
 */
@ExtendWith(MockitoExtension.class)
class PSRecycleServiceClearWidgetRelationshipsTest {

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
    when(idMapper.getString(itemGuid)).thenReturn("1-101-1");
    when(widgetAssetRelationshipService.clearAssetWidgetRelationshipsForAsset("1-101-1"))
        .thenReturn(2);

    service.clearWidgetRelationshipsForRecycledItem(itemGuid);

    verify(widgetAssetRelationshipService).clearAssetWidgetRelationshipsForAsset("1-101-1");
  }

  @Test
  void clearWidgetRelationships_nullGuid_isNoOp() throws Exception {
    service.clearWidgetRelationshipsForRecycledItem(null);

    verify(widgetAssetRelationshipService, never()).clearAssetWidgetRelationshipsForAsset(anyString());
  }

  @Test
  void clearWidgetRelationships_serviceFailure_isSwallowed() throws Exception {
    IPSGuid itemGuid = new PSLegacyGuid(55, 1);
    when(idMapper.getString(itemGuid)).thenReturn("1-55-1");
    doThrow(new PSWidgetAssetRelationshipServiceException("boom"))
        .when(widgetAssetRelationshipService)
        .clearAssetWidgetRelationshipsForAsset(eq("1-55-1"));

    // Must not throw — recycle should still succeed if clear fails
    service.clearWidgetRelationshipsForRecycledItem(itemGuid);

    verify(widgetAssetRelationshipService).clearAssetWidgetRelationshipsForAsset("1-55-1");
  }
}
