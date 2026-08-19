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
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;
import java.util.Collections;
import java.util.Map;

import com.percussion.comments.service.IPSCommentsService;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSectionNode;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Missing / empty NavTree must not 500 or delete the site (#3218 / parent #3197).
 * Entitled sites (folder root present) get a NavTree on first open (#3352).
 */
class PSSiteSectionServiceLoadTreeEmptyTest {

  @Mock private IPSPageDao pageDao;
  @Mock private IPSManagedNavService navService;
  @Mock private IPSContentWs contentSrv;
  @Mock private IPSContentDesignWs contentDsSrv;
  @Mock private IPSSiteManager siteMgr;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSRenderAssemblyBridge asmBridge;
  @Mock private IPSFolderHelper folderHelper;
  @Mock private IPSWorkflowHelper workflowHelper;
  @Mock private IPSPublishingWs publishingWs;
  @Mock private IPSTemplateService templateSrv;
  @Mock private IPSiteDao siteDao;
  @Mock private IPSSiteTemplateService siteTemplateSrv;
  @Mock private IPSCommentsService commentsService;
  @Mock private IPSPageDaoHelper pageDaoHelper;
  @Mock private IPSItemWorkflowService itemWorkflowService;
  @Mock private IPSContentMgr contentMgr;
  @Mock private IPSSite site;

  private PSSiteSectionService service;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    service =
        new PSSiteSectionService(
            pageDao,
            navService,
            contentSrv,
            contentDsSrv,
            siteMgr,
            idMapper,
            asmBridge,
            folderHelper,
            workflowHelper,
            publishingWs,
            templateSrv,
            siteDao,
            siteTemplateSrv,
            commentsService,
            pageDaoHelper,
            itemWorkflowService,
            contentMgr);
    when(site.getName()).thenReturn("Demo");
    when(site.getFolderRoot()).thenReturn("//Sites/Demo");
    when(siteMgr.loadSite("Demo")).thenReturn(site);
    when(navService.findNavigationIdFromFolder("//Sites/Demo")).thenReturn(null);
  }

  @Test
  void loadTreeCreatesNavTreeOnFirstOpenWithoutCheckin() throws Exception {
    PSLegacyGuid created = new PSLegacyGuid(364, 1);
    stubCreateNavTreeSave(created);
    stubCreatedNavTreeLoad(created);

    PSSectionNode tree = service.loadTree("Demo");
    assertEquals(new PSLegacyGuid(364, -1).toString(), tree.getId());
    assertEquals("Demo", tree.getTitle());
    assertEquals("//Sites/Demo", tree.getFolderPath().orElse(null));
    verify(navService).addNavTreeToFolder("//Sites/Demo", "Demo-NavTree", "Demo", -1);
    verify(contentSrv, never()).checkinItems(any(), eq(null));
    verify(siteMgr, never()).deleteSite(any());
  }

  @Test
  void loadTreeRetriesFolderWorkflowThenKeepsRoot() throws Exception {
    PSLegacyGuid created = new PSLegacyGuid(365, 1);
    when(pageDaoHelper.getWorkflowIdForPath("//Sites/Demo")).thenReturn(4);
    when(navService.addNavTreeToFolder("//Sites/Demo", "Demo-NavTree", "Demo", -1))
        .thenThrow(new RuntimeException("default workflow failed"));
    when(navService.addNavTreeToFolder("//Sites/Demo", "Demo-NavTree", "Demo", 4))
        .thenReturn(created);
    stubCreatedNavTreeLoad(created);

    PSSectionNode tree = service.loadTree("Demo");
    assertEquals(new PSLegacyGuid(365, -1).toString(), tree.getId());
    verify(navService).addNavTreeToFolder("//Sites/Demo", "Demo-NavTree", "Demo", -1);
    verify(navService).addNavTreeToFolder("//Sites/Demo", "Demo-NavTree", "Demo", 4);
    verify(contentSrv, never()).checkinItems(any(), eq(null));
  }

  @Test
  void loadTreeReturnsEmptyNodeWhenNavTreeCreateFails() throws Exception {
    when(navService.addNavTreeToFolder(anyString(), anyString(), anyString(), anyInt()))
        .thenThrow(new RuntimeException("folder missing"));

    PSSectionNode tree = service.loadTree("Demo");
    assertNotNull(tree);
    assertNull(tree.getId());
    assertEquals("Demo", tree.getTitle());
    assertEquals("//Sites/Demo", tree.getFolderPath().orElse(null));
    assertTrue(tree.getChildNodes().isEmpty());
    verify(siteMgr, never()).deleteSite(any());
  }

  @Test
  void loadRootReturnsEmptySectionWhenCreateFailsAndDoesNotDeleteSite() throws Exception {
    when(navService.addNavTreeToFolder(anyString(), anyString(), anyString(), anyInt()))
        .thenThrow(new RuntimeException("no nav type"));

    PSSiteSection root = service.loadRoot("Demo");
    assertNotNull(root);
    assertNull(root.getId());
    assertEquals("Demo", root.getTitle());
    assertEquals("//Sites/Demo", root.getFolderPath());
    assertTrue(root.getChildIds() == null || root.getChildIds().isEmpty());
    verify(siteMgr, never()).deleteSite(any());
  }

  @Test
  void loadTreeReturnsEmptyWhenSiteHasNoFolderRoot() throws Exception {
    when(site.getFolderRoot()).thenReturn(null);
    PSSectionNode tree = service.loadTree("Demo");
    assertNotNull(tree);
    assertNull(tree.getId());
    assertTrue(tree.getChildNodes().isEmpty());
    verify(contentSrv, never()).createItems(anyString(), anyInt());
    verify(navService, never()).addNavTreeToFolder(anyString(), anyString(), anyString(), anyInt());
  }

  @Test
  void loadTreeUsesExistingRffNavTreeWithoutSeedingSecond() throws Exception {
    PSLegacyGuid existing = new PSLegacyGuid(360, 1);
    when(navService.findNavigationIdFromFolder("//Sites/Demo")).thenReturn(existing);
    stubCreatedNavTreeLoad(existing);

    PSSectionNode tree = service.loadTree("Demo");
    assertEquals(new PSLegacyGuid(360, -1).toString(), tree.getId());
    assertEquals("Demo", tree.getTitle());
    verify(navService, never())
        .addNavTreeToFolder(anyString(), anyString(), anyString(), anyInt());
    verify(siteMgr, never()).deleteSite(any());
  }

  @Test
  void loadTreeUsesExistingNavTreeAfterConcurrentCreate() throws Exception {
    PSLegacyGuid raced = new PSLegacyGuid(361, 1);
    when(navService.addNavTreeToFolder(anyString(), anyString(), anyString(), anyInt()))
        .thenThrow(new RuntimeException("already has"));
    when(navService.findNavigationIdFromFolder("//Sites/Demo")).thenReturn(null, raced);
    stubCreatedNavTreeLoad(raced);

    PSSectionNode tree = service.loadTree("Demo");
    assertEquals(new PSLegacyGuid(361, -1).toString(), tree.getId());
    assertEquals("Demo", tree.getTitle());
  }

  private void stubCreateNavTreeSave(IPSGuid created) {
    when(navService.addNavTreeToFolder("//Sites/Demo", "Demo-NavTree", "Demo", -1))
        .thenReturn(created);
  }

  private void stubCreatedNavTreeLoad(IPSGuid navGuid) {
    when(contentDsSrv.getItemGuid(any())).thenReturn(navGuid);
    when(navService.findChildNavonIds(any())).thenReturn(Collections.emptyList());
    when(idMapper.getStrings(any())).thenReturn(Collections.emptyList());
    when(idMapper.getString(org.mockito.ArgumentMatchers.<IPSGuid>any()))
        .thenAnswer(
            inv -> {
              IPSGuid guid = inv.getArgument(0, IPSGuid.class);
              return guid == null ? null : guid.toString();
            });
    when(idMapper.getGuid(anyString())).thenReturn(navGuid);
    when(navService.getNavonProperties(any(), any()))
        .thenReturn(Map.of("displaytitle", "Demo"));
    when(navService.findAncestorNavonIds(any())).thenReturn(Collections.emptyList());
    when(navService.getNavTitle(any())).thenReturn("Demo");
  }

  @Test
  void missingNavTreeMessageIsRecognized() {
    assertTrue(
        PSSiteSectionService.isMissingNavTreeMessage(
            "Cannot find navigation tree for site: Demo"));
    assertTrue(
        PSSiteSectionService.isMissingNavTreeMessage(
            "NAVIGATION_SERVICE_CANNOT_FIND_NAVTREE_FOR_SITE: Demo"));
    assertTrue(!PSSiteSectionService.isMissingNavTreeMessage("database unavailable"));
    assertTrue(!PSSiteSectionService.isMissingNavTreeMessage(null));
  }
}
