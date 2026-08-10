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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.itemmanagement.service.IPSWorkflowHelper.PSItemTypeEnum;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.linkmanagement.data.PSManagedLink;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.useritems.IPSUserItemsDao;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for page site-impact REST path ({@code siteimpact/page/{pageId}}). Verifies reverse
 * relationship owners and managed-link parents without a full CMS.
 */
@ExtendWith(MockitoExtension.class)
class PSItemServicePageSiteImpactTest {

  private static final String PAGE_ID = "0-123-456";
  private static final String OWNER_PAGE_ID = "0-123-789";
  private static final String OWNER_TEMPLATE_ID = "0-123-999";
  private static final String PARENT_PAGE_ID = "0-123-555";

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
  private final ObjectMapper mapper = JsonMapper.builder().build();

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
  void getPageSiteImpactIncludesRelationshipOwnersAndManagedLinkPages() throws Exception {
    Set<String> owners = new HashSet<>();
    owners.add(OWNER_PAGE_ID);
    owners.add(OWNER_TEMPLATE_ID);
    when(waRelService.getRelationshipOwners(eq(PAGE_ID), anyBoolean())).thenReturn(owners);
    when(workflowHelper.getItemType(OWNER_PAGE_ID)).thenReturn(PSItemTypeEnum.PAGE);
    when(workflowHelper.getItemType(OWNER_TEMPLATE_ID)).thenReturn(PSItemTypeEnum.TEMPLATE);

    IPSGuid pageGuid = mock(IPSGuid.class);
    when(idMapper.getGuid(PAGE_ID)).thenReturn(pageGuid);
    when(idMapper.getContentId(pageGuid)).thenReturn(456);

    PSManagedLink link = new PSManagedLink();
    link.setLinkId(10L);
    link.setParentId(100);
    link.setChildId(456);
    when(linkService.findLinksByChildId(456)).thenReturn(List.of(link));

    IPSGuid parentGuid = mock(IPSGuid.class);
    when(idMapper.getGuidFromContentId(100)).thenReturn(parentGuid);
    when(parentGuid.toString()).thenReturn(PARENT_PAGE_ID);

    PSContentItem parentPage = new PSContentItem();
    parentPage.setId(PARENT_PAGE_ID);
    parentPage.setType("percPage");
    when(contentItemDao.find(PARENT_PAGE_ID)).thenReturn(parentPage);

    PSItemProperties ownerPageProps = new PSItemProperties();
    ownerPageProps.setId(OWNER_PAGE_ID);
    ownerPageProps.setName("Owner Page");
    ownerPageProps.setPath("/Sites/Demo/owner-page");
    ownerPageProps.setStatus("Live");
    ownerPageProps.setSummary("owner");

    PSItemProperties parentPageProps = new PSItemProperties();
    parentPageProps.setId(PARENT_PAGE_ID);
    parentPageProps.setName("Linker Page");
    parentPageProps.setPath("/Sites/Demo/linker-page");
    parentPageProps.setStatus("Draft");
    parentPageProps.setSummary("linker");

    when(folderHelper.findItemPropertiesById(OWNER_PAGE_ID)).thenReturn(ownerPageProps);
    when(folderHelper.findItemPropertiesById(PARENT_PAGE_ID)).thenReturn(parentPageProps);

    PSTemplateSummary template = new PSTemplateSummary();
    template.setId(OWNER_TEMPLATE_ID);
    template.setName("Base Template");
    when(templateService.find(OWNER_TEMPLATE_ID)).thenReturn(template);
    when(folderHelper.getItemSites(OWNER_TEMPLATE_ID)).thenReturn(Collections.emptyList());

    String json = service.getPageSiteImpact(PAGE_ID);
    JsonNode root = mapper.readTree(json);

    assertTrue(root.has("pages"));
    assertTrue(root.has("templates"));
    assertEquals(2, root.get("pages").size());
    assertEquals(1, root.get("templates").size());
    assertEquals(
        "Base Template", root.get("templates").get(0).get("template").get("name").asString());

    Set<String> pageNames = new HashSet<>();
    for (JsonNode page : root.get("pages")) {
      pageNames.add(page.get("name").asString());
    }
    assertTrue(pageNames.contains("Owner Page"));
    assertTrue(pageNames.contains("Linker Page"));
  }

  @Test
  void getPageSiteImpactReturnsEmptyArraysWhenNoOwners() throws Exception {
    when(waRelService.getRelationshipOwners(eq(PAGE_ID), anyBoolean()))
        .thenReturn(Collections.emptySet());

    IPSGuid pageGuid = mock(IPSGuid.class);
    when(idMapper.getGuid(PAGE_ID)).thenReturn(pageGuid);
    when(idMapper.getContentId(pageGuid)).thenReturn(456);
    when(linkService.findLinksByChildId(456)).thenReturn(Collections.emptyList());

    String json = service.getPageSiteImpact(PAGE_ID);
    JsonNode root = mapper.readTree(json);

    assertTrue(root.get("pages").isArray());
    assertTrue(root.get("templates").isArray());
    assertEquals(0, root.get("pages").size());
    assertEquals(0, root.get("templates").size());
  }

  @Test
  void getAssetSiteImpactSharesBuilderPathWithEmptyResult() throws Exception {
    when(waRelService.getRelationshipOwners(anyString(), anyBoolean()))
        .thenReturn(Collections.emptySet());
    IPSGuid assetGuid = mock(IPSGuid.class);
    when(idMapper.getGuid("asset-1")).thenReturn(assetGuid);
    when(idMapper.getContentId(assetGuid)).thenReturn(1);
    when(linkService.findLinksByChildId(1)).thenReturn(Collections.emptyList());

    String json = service.getAssetSiteImpact("asset-1");
    JsonNode root = mapper.readTree(json);
    assertEquals(0, root.get("pages").size());
    assertEquals(0, root.get("templates").size());
  }
}
