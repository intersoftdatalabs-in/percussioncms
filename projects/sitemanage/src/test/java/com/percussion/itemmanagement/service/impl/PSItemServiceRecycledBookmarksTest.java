/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.services.useritems.data.PSUserItem;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentWs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regression for GH-877 / v8.1.7 PR #893: My Bookmarks / getMyContent must omit items that live in
 * the recycler.
 */
@ExtendWith(MockitoExtension.class)
class PSItemServiceRecycledBookmarksTest {

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
  @Mock private IPSRecycleService recycleService;

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
    service.setRecycleService(recycleService);

    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfo.KEY_USER, "testuser");
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void getMyContentFiltersRecycledItems() throws Exception {
    List<PSUserItem> userItems = new ArrayList<>();
    PSUserItem activeItem = new PSUserItem();
    activeItem.setItemId(101);
    userItems.add(activeItem);
    PSUserItem recycledItem = new PSUserItem();
    recycledItem.setItemId(102);
    userItems.add(recycledItem);

    when(userItemDao.find("testuser")).thenReturn(userItems);
    when(recycleService.isInRecycler("101")).thenReturn(false);
    when(recycleService.isInRecycler("102")).thenReturn(true);

    PSLegacyGuid legacyGuid = new PSLegacyGuid(101, 1);
    when(idMapper.getGuid(any(PSLocator.class))).thenReturn(legacyGuid);
    when(folderHelper.findItemPropertiesById(legacyGuid.toString()))
        .thenReturn(new PSItemProperties());

    List<PSItemProperties> result = service.getMyContent();
    assertEquals(1, result.size());
    verify(recycleService).isInRecycler("101");
    verify(recycleService).isInRecycler("102");
    // Only the non-recycled item is resolved to properties
    verify(folderHelper, times(1)).findItemPropertiesById(legacyGuid.toString());
  }
}
