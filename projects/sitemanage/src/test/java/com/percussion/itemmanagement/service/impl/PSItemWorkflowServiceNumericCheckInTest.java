/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.async.IPSAsyncJobService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code GET .../itemmanagement/workflow/checkIn/594} must not fail before
 * GUID mapping (#3688).
 */
@ExtendWith(MockitoExtension.class)
class PSItemWorkflowServiceNumericCheckInTest {

  @Mock private IPSContentWs contentWs;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSSecurityWs securityWs;
  @Mock private IPSWidgetAssetRelationshipService widgetAssetRelationshipService;
  @Mock private IPSPageDao pageDao;
  @Mock private IPSSystemService systemService;
  @Mock private IPSWorkflowHelper workflowHelper;
  @Mock private IPSAssetDao assetDao;
  @Mock private IPSDataItemSummaryService dataItemSummaryService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSWorkflowService workflowService;
  @Mock private IPSiteDao siteDao;
  @Mock private IPSSiteManager siteMgr;
  @Mock private IPSSystemWs systemWs;
  @Mock private IPSAsyncJobService asyncJobService;
  @Mock private IPSRecycleService recycleService;
  @Mock private IPSGuid contentGuid;

  private PSItemWorkflowService service;

  @BeforeEach
  void setUp() {
    service =
        new PSItemWorkflowService(
            contentWs,
            idMapper,
            securityWs,
            widgetAssetRelationshipService,
            pageDao,
            systemService,
            workflowHelper,
            assetDao,
            dataItemSummaryService,
            folderHelper,
            workflowService,
            siteDao,
            siteMgr,
            systemWs,
            asyncJobService,
            recycleService);
  }

  @Test
  void checkInNumericContentIdWhenItemMissingIsNoOp() throws Exception {
    when(dataItemSummaryService.find("594")).thenReturn(null);

    var result = service.checkIn("594", false);

    assertEquals("checkIn", result.getOperation());
  }

  @Test
  void checkInNumericContentIdPassesBareIdToGuidMapper() throws Exception {
    var sum = new PSDataItemSummary();
    sum.setName("Dow futures higher; chip stocks");
    sum.setType("percAsset");
    when(dataItemSummaryService.find("594")).thenReturn(sum);
    when(idMapper.getGuids(anyList())).thenReturn(List.of(contentGuid));

    var result = service.checkIn("594", false);

    assertEquals("checkIn", result.getOperation());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> ids = ArgumentCaptor.forClass(List.class);
    verify(idMapper).getGuids(ids.capture());
    assertEquals(List.of("594"), ids.getValue());
    verify(contentWs).checkinItems(eq(List.of(contentGuid)), isNull(), eq(false));
  }

  @Test
  void checkInRestPathAcceptsBareNumericContentId() throws PSDataServiceException {
    when(dataItemSummaryService.find("594")).thenReturn(null);

    var result = service.checkIn("594");

    assertEquals("checkIn", result.getOperation());
  }

  @Test
  void checkInRestDoesNot500WhenFindReportsUndeterminedGuidType() throws Exception {
    when(dataItemSummaryService.find("594"))
        .thenThrow(
            new DataServiceLoadException(
                new IllegalArgumentException(
                    "Type is undetermined, expecting \"type\" argument")));

    var result = service.checkIn("594");

    assertEquals("checkIn", result.getOperation());
  }

  @Test
  void checkInRestDoesNot500WhenGuidAssembleThrowsUndeterminedType() throws Exception {
    when(dataItemSummaryService.find("594"))
        .thenThrow(
            new IllegalArgumentException("Type is undetermined, expecting \"type\" argument"));

    var result = service.checkIn("594");

    assertEquals("checkIn", result.getOperation());
  }

  @Test
  void isUndeterminedGuidTypeWalksCauseChain() {
    var wrapped =
        new DataServiceLoadException(
            new IllegalArgumentException("Type is undetermined, expecting \"type\" argument"));
    assertEquals(true, PSItemWorkflowService.isUndeterminedGuidType(wrapped));
    assertEquals(false, PSItemWorkflowService.isUndeterminedGuidType(new IllegalArgumentException("other")));
  }
}
