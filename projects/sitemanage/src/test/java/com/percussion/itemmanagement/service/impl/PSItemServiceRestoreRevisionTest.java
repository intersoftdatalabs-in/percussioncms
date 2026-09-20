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
package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Restore prior revision REST maps authorization / not-found failures to 403 / 404 (#4604).
 * Successes return 200 with PSNoContent; failures are not silent successes.
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSItemServiceRestoreRevisionTest {

  @Mock private IPSIdMapper idMapper;
  @Mock private IPSSystemService systemService;
  @Mock private IPSWorkflowHelper workflowHelper;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSWidgetAssetRelationshipService waRelService;
  @Mock private IPSItemWorkflowService itemWfService;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSContentItemDao contentItemDao;
  @Mock private IPSAssetDao assetDao;
  @Mock private IPSTemplateService templateService;
  @Mock private IPSUserItemsDao userItemDao;
  @Mock private IPSNotificationService notificationService;
  @Mock private IPSPublisherService pubService;
  @Mock private IPSManagedLinkDao linkService;
  @Mock private PSComponentSummary summary;
  @Mock private IPSGuid guid;

  private PSItemService service;

  @BeforeEach
  void setUp() {
    service =
        new PSItemService(
            idMapper,
            systemService,
            workflowHelper,
            contentWs,
            waRelService,
            itemWfService,
            folderHelper,
            contentItemDao,
            assetDao,
            templateService,
            userItemDao,
            notificationService,
            pubService,
            linkService);
  }

  @Test
  void noAssignmentMapsToForbidden() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.NONE));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.restoreRevision("42"));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void readerAssignmentMapsToForbidden() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.READER));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.restoreRevision("42"));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void missingComponentSummaryMapsToNotFound() throws Exception {
    when(workflowHelper.getComponentSummary(anyString()))
        .thenThrow(new com.percussion.services.error.PSNotFoundException(42));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.restoreRevision("42"));
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void blankIdIsForbidden() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> service.restoreRevision("   "));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
  }
}
