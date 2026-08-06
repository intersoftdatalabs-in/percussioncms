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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.data.PSPageLinkedToItem;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.linkmanagement.data.PSManagedLink;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regression for GH-664 / v8.1.7 PR #665: findPagesLinkedToItem must skip managed links with
 * invalid parent ids (orphaned) instead of resolving guid -1.
 */
@ExtendWith(MockitoExtension.class)
class PSItemServiceOrphanedManagedLinksTest {

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
  void findPagesLinkedToItemSkipsOrphanedParentIds() {
    when(idMapper.getContentId("//item")).thenReturn(42);

    PSManagedLink orphan = new PSManagedLink();
    orphan.setLinkId(99L);
    orphan.setParentId(-1);
    orphan.setChildId(42);
    when(linkService.findLinksByChildId(42)).thenReturn(List.of(orphan));

    List<PSPageLinkedToItem> result = service.findPagesLinkedToItem("//item");

    assertTrue(result != null && result.isEmpty());
    verify(idMapper, never()).getGuidFromContentId(anyInt());
  }

  @Test
  void findPagesLinkedToItemSkipsZeroParentId() {
    when(idMapper.getContentId("//item")).thenReturn(7);

    PSManagedLink orphan = new PSManagedLink();
    orphan.setLinkId(1L);
    orphan.setParentId(0);
    orphan.setChildId(7);
    when(linkService.findLinksByChildId(7)).thenReturn(Collections.singletonList(orphan));

    service.findPagesLinkedToItem("//item");

    verify(idMapper, never()).getGuidFromContentId(0);
    verify(idMapper, never()).getGuidFromContentId(-1);
  }
}
