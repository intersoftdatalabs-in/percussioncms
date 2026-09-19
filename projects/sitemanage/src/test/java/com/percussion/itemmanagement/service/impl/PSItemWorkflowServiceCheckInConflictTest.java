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
package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.system.IPSSystemWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Check-in REST maps workflow conflicts to HTTP 409 (#4603). */
@ExtendWith(MockitoExtension.class)
class PSItemWorkflowServiceCheckInConflictTest {

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
  void checkInRestReturnsConflictWhenCheckinItemsFails() throws Exception {
    var sum = new PSDataItemSummary();
    sum.setName("Home");
    sum.setType("percAsset");
    when(dataItemSummaryService.find("42")).thenReturn(sum);
    when(idMapper.getGuids(anyList())).thenReturn(List.of());
    doThrow(new PSErrorsException())
        .when(contentWs)
        .checkinItems(anyList(), isNull(), anyBoolean());

    WebApplicationException thrown =
        assertThrows(WebApplicationException.class, () -> service.checkIn("42"));
    assertEquals(Response.Status.CONFLICT.getStatusCode(), thrown.getResponse().getStatus());
  }
}
