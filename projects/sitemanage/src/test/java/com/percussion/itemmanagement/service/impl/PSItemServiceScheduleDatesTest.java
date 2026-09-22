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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.itemmanagement.data.PSItemDates;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** POST setitemdates maps invalid dates, forbidden writes, and checkout conflicts. */
@ExtendWith(MockitoExtension.class)
class PSItemServiceScheduleDatesTest {

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
  void blankIdIsBadRequest() throws Exception {
    PSItemDates req = new PSItemDates();
    req.setItemId("  ");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.setItemDates(req));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void pastPublishDateIsBadRequest() throws Exception {
    PSItemDates req = new PSItemDates();
    req.setItemId(ITEM);
    req.setStartDate("01/01/2000 09:00 am");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.setItemDates(req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("precedes"));
    verify(contentItemDao, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void unparseableDateIsBadRequest() throws Exception {
    PSItemDates req = new PSItemDates();
    req.setItemId(ITEM);
    req.setStartDate("not-a-date");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.setItemDates(req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("Invalid schedule dates"));
    verify(contentItemDao, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void readerIsForbidden() throws Exception {
    when(idMapper.getGuid(ITEM)).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.READER));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.setItemDates(emptyDates()));
    assertEquals(403, ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void checkoutByOtherUserIsConflict() throws Exception {
    when(idMapper.getGuid(ITEM)).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.ASSIGNEE));
    when(workflowHelper.getComponentSummary(ITEM)).thenReturn(summary);
    when(summary.getCheckoutUserName()).thenReturn("other");
    when(workflowHelper.isCheckedOutToCurrentUser(ITEM)).thenReturn(false);
    when(workflowHelper.isPage(ITEM)).thenReturn(true);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.setItemDates(emptyDates()));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("other"));
    verify(contentItemDao, never()).save(org.mockito.ArgumentMatchers.any());
  }

  private static PSItemDates emptyDates() {
    PSItemDates req = new PSItemDates();
    req.setItemId(ITEM);
    req.setStartDate("");
    req.setEndDate("");
    return req;
  }
}
