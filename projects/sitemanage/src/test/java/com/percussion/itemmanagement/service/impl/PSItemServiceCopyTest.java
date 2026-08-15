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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.itemmanagement.data.PSItemCopyResult;
import com.percussion.itemmanagement.service.IPSItemService;
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
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** New copy / promotable version REST on {@link PSItemService}. */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSItemServiceCopyTest {

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
  void createNewCopy_usesFirstFolderPath() throws Exception {
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.findFolderPaths(guid)).thenReturn(new String[] {"//Sites/Demo"});
    PSCoreItem copy = mock(PSCoreItem.class);
    PSLocator locator = new PSLocator(99, 1);
    when(copy.getLocator()).thenReturn(locator);
    when(contentWs.newCopies(anyList(), anyList(), isNull(), anyBoolean()))
        .thenReturn(List.of(copy));
    when(idMapper.getString(locator)).thenReturn("99");

    PSItemCopyResult result = service.createNewCopy("42");
    assertEquals("99", result.getItemId());
    assertEquals("//Sites/Demo", result.getFolderPath());
    assertFalse(result.isPromotable());
    verify(contentWs).newCopies(anyList(), anyList(), isNull(), anyBoolean());
    verify(contentWs, never()).newPromotableVersions(anyList(), anyList(), isNull(), anyBoolean());
  }

  @Test
  void createPromotableVersion_usesPromotableWs() throws Exception {
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.findFolderPaths(guid)).thenReturn(new String[] {"//Sites/Demo"});
    PSCoreItem copy = mock(PSCoreItem.class);
    PSLocator locator = new PSLocator(100, 1);
    when(copy.getLocator()).thenReturn(locator);
    when(contentWs.newPromotableVersions(anyList(), anyList(), isNull(), anyBoolean()))
        .thenReturn(List.of(copy));
    when(idMapper.getString(locator)).thenReturn("100");

    PSItemCopyResult result = service.createPromotableVersion("1-101-42");
    assertEquals("100", result.getItemId());
    assertTrue(result.isPromotable());
    verify(contentWs).newPromotableVersions(anyList(), anyList(), isNull(), anyBoolean());
    verify(contentWs, never()).newCopies(anyList(), anyList(), isNull(), anyBoolean());
  }

  @Test
  void createNewCopy_rejectsMissingFolder() throws Exception {
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.findFolderPaths(guid)).thenReturn(new String[0]);
    assertThrows(IPSItemService.PSItemServiceException.class, () -> service.createNewCopy("42"));
    verify(contentWs, never()).newCopies(anyList(), anyList(), isNull(), anyBoolean());
  }

  @Test
  void createNewCopy_rejectsBlankId() {
    assertThrows(
        jakarta.ws.rs.WebApplicationException.class, () -> service.createNewCopy("  "));
  }

  @Test
  void createNewCopy_rejectsEmptyResult() throws Exception {
    when(idMapper.getGuid(anyString())).thenReturn(guid);
    when(contentWs.findFolderPaths(guid)).thenReturn(new String[] {"//Sites/Demo"});
    when(contentWs.newCopies(anyList(), anyList(), isNull(), anyBoolean()))
        .thenReturn(Collections.emptyList());
    assertThrows(IPSItemService.PSItemServiceException.class, () -> service.createNewCopy("42"));
  }
}
