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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.data.PSItemCreateRequest;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSItemServiceCreatePageTest {

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
  @Mock private IPSPageService pageService;

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
    service.setPageService(pageService);
  }

  @Test
  void createPageRequiresTemplate() {
    PSItemCreateRequest req = new PSItemCreateRequest();
    req.setContentType("percPage");
    req.setFolderPath("/Sites/Demo");
    IPSItemService.PSItemServiceException ex =
        assertThrows(
            IPSItemService.PSItemServiceException.class, () -> service.createEditorItem(req));
    assertTrue(ex.getMessage().contains("template"));
  }

  @Test
  void createPageUsesPageService() throws Exception {
    PSPage saved = new PSPage();
    saved.setId("1-101-88");
    when(pageService.save(any(PSPage.class))).thenReturn(saved);

    PSItemCreateRequest req = new PSItemCreateRequest();
    req.setContentType("percPage");
    req.setFolderPath("/Sites/Demo");
    req.setTemplateId("tpl-1");
    req.setName("About");

    var result = service.createEditorItem(req);
    assertEquals("1-101-88", result.getItemId());
    assertEquals("percPage", result.getContentType());

    ArgumentCaptor<PSPage> cap = ArgumentCaptor.forClass(PSPage.class);
    verify(pageService).save(cap.capture());
    assertEquals("About.html", cap.getValue().getName());
    assertEquals("About", cap.getValue().getTitle());
    assertEquals("About", cap.getValue().getLinkTitle());
    assertEquals("tpl-1", cap.getValue().getTemplateId());
    assertEquals("//Sites/Demo", cap.getValue().getFolderPath());
  }

  @Test
  void createAssetAssignsFolderWorkflow() throws Exception {
    PSContentItem saved = new PSContentItem();
    saved.setId("1-101-7");
    when(contentItemDao.save(any(PSContentItem.class))).thenReturn(saved);
    IPSItemSummary folder = mock(IPSItemSummary.class);
    when(folder.getId()).thenReturn("folder-1");
    when(folder.getType()).thenReturn("Folder");
    when(folderHelper.findItem("//Folders/$System$/Assets")).thenReturn(folder);
    PSFolderProperties props = mock(PSFolderProperties.class);
    when(folderHelper.findFolderProperties("folder-1")).thenReturn(props);
    when(folderHelper.getValidWorkflowId(props)).thenReturn(5);

    PSItemCreateRequest req = new PSItemCreateRequest();
    req.setContentType("percSimpleTextAsset");
    req.setFolderPath("//Folders/$System$/Assets");
    req.setName("qa3656");

    var result = service.createEditorItem(req);
    assertEquals("1-101-7", result.getItemId());
    ArgumentCaptor<PSContentItem> cap = ArgumentCaptor.forClass(PSContentItem.class);
    verify(contentItemDao).save(cap.capture());
    assertEquals("5", cap.getValue().getFields().get("sys_workflowid"));
    assertEquals("qa3656", cap.getValue().getFields().get("sys_title"));
  }

  @Test
  void createAssetAssignsParentFolderWorkflowWhenPathIsNotFolder() throws Exception {
    PSContentItem saved = new PSContentItem();
    saved.setId("1-101-8");
    when(contentItemDao.save(any(PSContentItem.class))).thenReturn(saved);
    IPSItemSummary itemAtPath = mock(IPSItemSummary.class);
    when(itemAtPath.getId()).thenReturn("item-1");
    when(itemAtPath.getType()).thenReturn("percSimpleTextAsset");
    when(folderHelper.findItem("//Folders/$System$/Assets/qa3656")).thenReturn(itemAtPath);
    IPSGuid itemGuid = mock(IPSGuid.class);
    IPSGuid parentGuid = mock(IPSGuid.class);
    when(idMapper.getGuid("item-1")).thenReturn(itemGuid);
    when(folderHelper.getParentFolderId(itemGuid, true)).thenReturn(parentGuid);
    when(idMapper.getString(parentGuid)).thenReturn("parent-folder-1");
    PSFolderProperties props = mock(PSFolderProperties.class);
    when(folderHelper.findFolderProperties("parent-folder-1")).thenReturn(props);
    when(folderHelper.getValidWorkflowId(props)).thenReturn(9);

    PSItemCreateRequest req = new PSItemCreateRequest();
    req.setContentType("percSimpleTextAsset");
    req.setFolderPath("//Folders/$System$/Assets/qa3656");
    req.setName("qa3656-child");

    var result = service.createEditorItem(req);
    assertEquals("1-101-8", result.getItemId());
    ArgumentCaptor<PSContentItem> cap = ArgumentCaptor.forClass(PSContentItem.class);
    verify(contentItemDao).save(cap.capture());
    assertEquals("9", cap.getValue().getFields().get("sys_workflowid"));
    verify(folderHelper).getParentFolderId(itemGuid, true);
  }

  @Test
  void createPageRequiresPageService() {
    service.setPageService(null);
    PSItemCreateRequest req = new PSItemCreateRequest();
    req.setContentType("percPage");
    req.setFolderPath("/Sites/Demo");
    req.setTemplateId("tpl-1");
    IPSItemService.PSItemServiceException ex =
        assertThrows(
            IPSItemService.PSItemServiceException.class, () -> service.createEditorItem(req));
    assertTrue(ex.getMessage().contains("Page service"));
  }
}
