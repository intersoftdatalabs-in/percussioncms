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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.itemmanagement.data.PSItemEditorField;
import com.percussion.itemmanagement.data.PSItemEditorFields;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EditorHost field save (#4645): PUT maps a stale revision to HTTP 409 and does not write.
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSItemServiceSaveEditorFieldsTest {

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
  void staleRevisionMapsToConflictAndDoesNotSave() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(42, 3));
    when(summary.getCheckoutUserName()).thenReturn("");

    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(1);
    req.setFields(List.of(new PSItemEditorField("displaytitle", "Nope")));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.saveEditorFields("42", req));
    assertEquals(Response.Status.CONFLICT.getStatusCode(), ex.getResponse().getStatus());
    verify(contentItemDao, never()).save(any());
  }

  @Test
  void matchingRevisionSavesAndReturnsLiveRevision() throws Exception {
    when(workflowHelper.getComponentSummary(anyString())).thenReturn(summary);
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(42, 2));
    when(summary.getCheckoutUserName()).thenReturn("admin");
    when(workflowHelper.isCheckedOutToCurrentUser(anyString())).thenReturn(true);
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.prepareForEdit(guid)).thenReturn(null);
    PSContentItem item = new PSContentItem();
    item.setId("42");
    item.setType("percPage");
    item.setName("Home");
    item.setFields(new HashMap<>());
    when(contentItemDao.find(anyString(), anyBoolean())).thenReturn(item);

    PSItemEditorFields req = new PSItemEditorFields();
    req.setRevision(2);
    req.setFields(List.of(new PSItemEditorField("displaytitle", "Welcome")));

    PSItemEditorFields saved = service.saveEditorFields("42", req);
    assertEquals(2, saved.getRevision());
    assertEquals("Welcome", item.getFields().get("displaytitle"));
    verify(contentItemDao).save(item);
  }

  @Test
  void currentRevisionReadsLocator() {
    when(summary.getCurrentLocator()).thenReturn(new PSLocator(7, 4));
    assertEquals(4, PSItemService.currentRevision(summary));
    assertEquals(0, PSItemService.currentRevision(null));
  }
}
