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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.itemmanagement.data.PSItemRevisionCompareResult;
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
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSItemServiceCompareRevisionsTest {

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
  void missingRevisionIs404() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.compareRevisions("42", 0, 1));
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void missingItemIs404() throws Exception {
    when(workflowHelper.getComponentSummary(any())).thenThrow(new RuntimeException("gone"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.compareRevisions("42", 1, 2));
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void noAssignmentIs403() throws Exception {
    when(workflowHelper.getComponentSummary(any())).thenReturn(summary);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.NONE));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.compareRevisions("42", 1, 2));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void unknownRevisionPayloadIs404() throws Exception {
    when(workflowHelper.getComponentSummary(any())).thenReturn(summary);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.ASSIGNEE));
    when(contentItemDao.find(eq("1-101-42"), anyBoolean())).thenReturn(item("Home", "old"));
    when(contentItemDao.find(eq("2-101-42"), anyBoolean())).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.compareRevisions("42", 1, 2));
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
  }

  @Test
  void compareReturnsFieldDiffs() throws Exception {
    when(workflowHelper.getComponentSummary(any())).thenReturn(summary);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(systemService.getContentAssignmentTypes(anyList()))
        .thenReturn(List.of(PSAssignmentTypeEnum.ASSIGNEE));
    when(contentItemDao.find(eq("1-101-42"), anyBoolean())).thenReturn(item("Home", "old"));
    when(contentItemDao.find(eq("2-101-42"), anyBoolean())).thenReturn(item("Home", "new"));

    PSItemRevisionCompareResult out = service.compareRevisions("42", 1, 2);
    assertEquals("1-101-42", out.getItemId());
    assertEquals(1, out.getRev1());
    assertEquals(2, out.getRev2());
    assertTrue(
        out.getFields().stream()
            .anyMatch(f -> "displaytitle".equals(f.getName()) && f.isChanged()));
  }

  private static PSContentItem item(String title, String display) {
    PSContentItem content = new PSContentItem();
    content.setId("42");
    content.setType("percPage");
    content.setName(title);
    Map<String, Object> fields = new HashMap<>();
    fields.put("sys_title", title);
    fields.put("displaytitle", display);
    content.setFields(fields);
    return content;
  }
}
