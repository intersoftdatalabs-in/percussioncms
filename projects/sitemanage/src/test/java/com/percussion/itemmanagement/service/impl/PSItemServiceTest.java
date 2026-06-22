/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent.EventType;
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
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PSItemServiceTest {

  private Mockery context = new JUnit4Mockery();

  private PSItemService service;

  private IPSIdMapper idMapper;
  private IPSSystemService systemService;
  private IPSWorkflowHelper workflowHelper;
  private IPSContentWs contentWs;
  private IPSWidgetAssetRelationshipService waRelService;
  private IPSItemWorkflowService itemWfService;
  private IPSFolderHelper folderHelper;
  private IPSContentItemDao contentItemDao;
  private IPSAssetDao assetDao;
  private IPSTemplateService templateService;
  private IPSUserItemsDao userItemDao;
  private IPSNotificationService notificationService;
  private IPSPublisherService pubService;
  private IPSManagedLinkDao linkService;
  private IPSRecycleService recycleService;

  @Before
  public void setUp() throws Exception {
    idMapper = context.mock(IPSIdMapper.class);
    systemService = context.mock(IPSSystemService.class);
    workflowHelper = context.mock(IPSWorkflowHelper.class);
    contentWs = context.mock(IPSContentWs.class);
    waRelService = context.mock(IPSWidgetAssetRelationshipService.class);
    itemWfService = context.mock(IPSItemWorkflowService.class);
    folderHelper = context.mock(IPSFolderHelper.class);
    contentItemDao = context.mock(IPSContentItemDao.class);
    assetDao = context.mock(IPSAssetDao.class);
    templateService = context.mock(IPSTemplateService.class);
    userItemDao = context.mock(IPSUserItemsDao.class);
    notificationService = context.mock(IPSNotificationService.class);
    pubService = context.mock(IPSPublisherService.class);
    linkService = context.mock(IPSManagedLinkDao.class);
    recycleService = context.mock(IPSRecycleService.class);

    context.checking(
        new Expectations() {
          {
            allowing(notificationService)
                .addListener(with(any(EventType.class)), with(any(IPSNotificationListener.class)));
          }
        });

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

  @After
  public void tearDown() throws Exception {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  public void testGetMyContentFiltersRecycledItems() throws Exception {
    final String username = "testuser";
    final List<PSUserItem> userItems = new ArrayList<>();

    // Add two bookmarked items
    PSUserItem activeItem = new PSUserItem();
    activeItem.setItemId(101);
    userItems.add(activeItem);

    PSUserItem recycledItem = new PSUserItem();
    recycledItem.setItemId(102);
    userItems.add(recycledItem);

    final PSLegacyGuid legacyGuid = new PSLegacyGuid(101, 1);

    // Mocking behaviour
    context.checking(
        new Expectations() {
          {
            // First we find user items for the username
            oneOf(userItemDao).find(username);
            will(returnValue(userItems));

            // Item 101 is NOT in recycler
            oneOf(recycleService).isInRecycler("101");
            will(returnValue(false));

            // Item 102 IS in recycler
            oneOf(recycleService).isInRecycler("102");
            will(returnValue(true));

            // findItemPropertiesById should only be called for the active item 101
            oneOf(idMapper).getGuid(with(any(com.percussion.design.objectstore.PSLocator.class)));
            will(returnValue(legacyGuid));

            oneOf(folderHelper).findItemPropertiesById(legacyGuid.toString());
            will(returnValue(new PSItemProperties()));
          }
        });

    List<PSItemProperties> result = service.getMyContent();
    assertNotNull(result);
    // Only the non-recycled item (101) should be returned, so size should be 1
    assertEquals(1, result.size());
  }
}
