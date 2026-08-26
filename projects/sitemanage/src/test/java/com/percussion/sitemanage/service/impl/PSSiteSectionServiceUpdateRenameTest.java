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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.comments.service.IPSCommentsService;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSMoveSiteSection;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.sitemanage.data.PSSiteSectionProperties;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Architecture rename persist: title-only update must not rewrite folder ACL
 * and must set navon properties with production types (#3797).
 */
class PSSiteSectionServiceUpdateRenameTest {

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
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service =
        spy(
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
                contentMgr));
  }

  @Test
  void shouldSkipFolderPropertySave_titleOnlyUnchangedName() {
    PSSiteSectionProperties req = new PSSiteSectionProperties();
    req.setId("10-1");
    req.setTitle("Renamed");
    req.setFolderName("about");
    req.setFolderPermission(null);
    assertTrue(PSSiteSectionService.shouldSkipFolderPropertySave(req, "about"));
  }

  @Test
  void shouldSkipFolderPropertySave_trueWhenNameUnchangedEvenWithPermission() {
    PSSiteSectionProperties req = new PSSiteSectionProperties();
    req.setFolderName("about");
    req.setFolderPermission(new PSFolderPermission());
    assertTrue(PSSiteSectionService.shouldSkipFolderPropertySave(req, "about"));
  }

  @Test
  void shouldSkipFolderPropertySave_falseWhenFolderNameChanges() {
    PSSiteSectionProperties req = new PSSiteSectionProperties();
    req.setFolderName("about-us");
    req.setFolderPermission(null);
    assertFalse(PSSiteSectionService.shouldSkipFolderPropertySave(req, "about"));
  }

  @Test
  void isLandingPageCheckedOut_nullSafe() {
    assertFalse(PSSiteSectionService.isLandingPageCheckedOut(null));
    PSComponentSummary empty = mock(PSComponentSummary.class);
    when(empty.getCheckoutUserName()).thenReturn(null);
    assertFalse(PSSiteSectionService.isLandingPageCheckedOut(empty));
    when(empty.getCheckoutUserName()).thenReturn("");
    assertFalse(PSSiteSectionService.isLandingPageCheckedOut(empty));
    when(empty.getCheckoutUserName()).thenReturn("Admin");
    assertTrue(PSSiteSectionService.isLandingPageCheckedOut(empty));
  }

  @Test
  void applyValidatedSectionUpdate_titleOnlyDoesNotSaveFolder() throws Exception {
    IPSGuid navonId = new PSLegacyGuid(9001, -1);
    IPSGuid folderId = new PSLegacyGuid(501, -1);
    PSSiteSectionProperties req = new PSSiteSectionProperties();
    req.setId("9001--1");
    req.setTitle("Renamed Section");
    req.setFolderName("about");
    req.setFolderPermission(new PSFolderPermission());

    when(idMapper.getGuid("9001--1")).thenReturn(navonId);
    when(publishingWs.getItemSites(navonId)).thenReturn(Collections.singletonList(site));
    when(site.getName()).thenReturn("Demo");
    when(site.isSecure()).thenReturn(false);
    when(navService.getLandingPageFromNavnode(navonId)).thenReturn(null);
    when(folderHelper.getParentFolderId(navonId)).thenReturn(folderId);
    when(contentSrv.loadFolder(folderId, false)).thenReturn(new PSFolder("about", 501, 1001, 1, ""));
    doReturn(new PSSiteSection()).when(service).load(anyString());

    service.applyValidatedSectionUpdate(req);

    verify(navService).setNavonProperties(eq(navonId), anyMap());
    verify(folderHelper, never()).saveFolderProperties(any(PSFolderProperties.class));
    verify(contentSrv, never()).checkinItems(any(), isNull());
  }

  @Test
  void move_delegatesToManagedNavServiceWithTargetIndex() throws Exception {
    PSMoveSiteSection req = new PSMoveSiteSection();
    req.setSourceId("11--1");
    req.setSourceParentId("10--1");
    req.setTargetId("10--1");
    req.setTargetIndex(1);
    IPSGuid src = new PSLegacyGuid(11, -1);
    IPSGuid parent = new PSLegacyGuid(10, -1);
    IPSGuid folderId = new PSLegacyGuid(501, -1);
    when(idMapper.getGuid("11--1")).thenReturn(src);
    when(idMapper.getGuid("10--1")).thenReturn(parent);
    when(publishingWs.getItemSites(src)).thenReturn(Collections.singletonList(site));
    when(site.getName()).thenReturn("Demo");
    when(folderHelper.getParentFolderId(any())).thenReturn(folderId);
    when(folderHelper.hasFolderPermission(anyString(), any())).thenReturn(true);
    doReturn(new PSSiteSection()).when(service).load(anyString());

    service.move(req);

    verify(navService).moveNavon(src, parent, parent, 1);
  }
}
