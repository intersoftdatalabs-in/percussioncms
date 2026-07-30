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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.services.useritems.data.PSUserItem;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * isMyPage is an internal sitemanage endpoint. It must produce JSON/XML like sibling item APIs so
 * SPA clients that send {@code Accept: application/json} do not receive HTTP 406. Public, versioned
 * bookmark APIs belong in {@code rest}.
 */
@ExtendWith(MockitoExtension.class)
class PSItemServiceIsMyPageTest {

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

    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfo.KEY_USER, "testuser");
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void isMyPageProducesJsonAndXmlLikeSiblingItemEndpoints() throws Exception {
    Method method = PSItemService.class.getMethod("isMyPage", String.class);
    Produces produces = method.getAnnotation(Produces.class);
    assertTrue(produces != null, "@Produces must be present on isMyPage");
    Set<String> media = new HashSet<>(Arrays.asList(produces.value()));
    assertTrue(
        media.contains(MediaType.APPLICATION_JSON),
        "isMyPage must produce application/json for SPA clients: " + media);
    assertTrue(
        media.contains(MediaType.APPLICATION_XML),
        "isMyPage should produce application/xml like sibling item endpoints: " + media);
    assertFalse(
        media.contains(MediaType.TEXT_PLAIN),
        "internal endpoint should not be text/plain-only; use JSON like siblings: " + media);
  }

  @Test
  void isMyPageTrueWhenUserItemExists() {
    when(idMapper.getContentId("page-guid")).thenReturn(42);
    when(userItemDao.find("testuser", 42)).thenReturn(new PSUserItem());
    assertTrue(service.isMyPage("page-guid"));
  }

  @Test
  void isMyPageFalseWhenNotFavorited() {
    when(idMapper.getContentId("page-guid")).thenReturn(42);
    when(userItemDao.find("testuser", 42)).thenReturn(null);
    assertFalse(service.isMyPage("page-guid"));
  }

  @Test
  void isMyPageBlankPageIdThrowsWebApplicationException() {
    assertThrows(WebApplicationException.class, () -> service.isMyPage(""));
    assertThrows(WebApplicationException.class, () -> service.isMyPage("   "));
    assertThrows(WebApplicationException.class, () -> service.isMyPage(null));
  }
}
