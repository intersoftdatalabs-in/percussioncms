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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.percussion.services.publisher.data.PSItemPublishingHistory;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** GET pubhistory maps blank/invalid ids, missing items, and forbidden reads. */
@ExtendWith(MockitoExtension.class)
class PSItemServicePublishingHistoryTest {

  private static final String ITEM = "16777215-101-42";

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
  @Mock private IPSGuid guid;
  @Mock private PSComponentSummary summary;

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
  void blankIdIsBadRequest() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.getPublishingHistory("  "));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void nonNumericIdIsBadRequest() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.getPublishingHistory("abc"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void missingItemIsNotFound() throws Exception {
    when(workflowHelper.getComponentSummary(ITEM))
        .thenThrow(new IllegalStateException("gone"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.getPublishingHistory(ITEM));
    assertEquals(404, ex.getResponse().getStatus());
    verify(pubService, never()).findItemPublishingHistory(any());
  }

  @Test
  void noAssignmentIsForbidden() throws Exception {
    when(workflowHelper.getComponentSummary(ITEM)).thenReturn(summary);
    when(idMapper.getGuid(ITEM)).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.NONE));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.getPublishingHistory(ITEM));
    assertEquals(403, ex.getResponse().getStatus());
    verify(pubService, never()).findItemPublishingHistory(any());
  }

  @Test
  void readerSeesHistoryRows() throws Exception {
    when(workflowHelper.getComponentSummary(ITEM)).thenReturn(summary);
    when(idMapper.getGuid(ITEM)).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.READER));
    PSItemPublishingHistory row =
        new PSItemPublishingHistory(
            42, 3, "prod", new Date(1_700_000_000_000L), "SUCCESS", "PUBLISH", "", "/index.html");
    when(pubService.findItemPublishingHistory(guid)).thenReturn(List.of(row));
    List<PSItemPublishingHistory> rows = service.getPublishingHistory(ITEM);
    assertEquals(1, rows.size());
    assertEquals("prod", rows.get(0).getServer());
    assertEquals("SUCCESS", rows.get(0).getStatus());
  }

  @Test
  void publisherFailureIsServerError() throws Exception {
    when(workflowHelper.getComponentSummary(ITEM)).thenReturn(summary);
    when(idMapper.getGuid(ITEM)).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(Collections.singletonList(PSAssignmentTypeEnum.ASSIGNEE));
    when(pubService.findItemPublishingHistory(guid)).thenThrow(new IllegalStateException("db"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.getPublishingHistory(ITEM));
    assertEquals(500, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).length() > 0);
  }
}
