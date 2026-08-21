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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.comments.service.IPSCommentsService;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.content.data.PSItemStatus;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSCreateSiteSection;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Create section landing attach must not call {@code checkinItems} — sample
 * rffNavTree percNavon check-in NPEs and rolls back POST /section/create (#3676).
 */
class PSSiteSectionServiceCreateLandingCheckinTest {

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
    when(asmBridge.getDispatchTemplate()).thenReturn("perc.page");
  }

  @Test
  void attachDoesNotCheckinWhenPrepareSucceeds() {
    IPSGuid pageId = new PSLegacyGuid(8001, 1);
    IPSGuid navonId = new PSLegacyGuid(9001, 1);
    PSItemStatus status = Mockito.mock(PSItemStatus.class);
    when(contentSrv.prepareForEdit(navonId)).thenReturn(status);

    service.attachLandingPageWithoutForcedCheckin(pageId, navonId);

    verify(navService).addLandingPageToNavnode(pageId, navonId, "perc.page");
    verify(contentSrv).releaseFromEdit(status, false);
    verify(contentSrv, never()).checkinItems(any(), eq(null));
  }

  @Test
  void attachStillLinksWhenPrepareForEditNpes() {
    IPSGuid pageId = new PSLegacyGuid(8002, 1);
    IPSGuid navonId = new PSLegacyGuid(9002, 1);
    when(contentSrv.prepareForEdit(navonId)).thenThrow(new NullPointerException());

    service.attachLandingPageWithoutForcedCheckin(pageId, navonId);

    verify(navService).addLandingPageToNavnode(pageId, navonId, "perc.page");
    verify(contentSrv, never()).releaseFromEdit(any(PSItemStatus.class), anyBoolean());
    verify(contentSrv, never()).checkinItems(any(), eq(null));
  }

  @Test
  void attachRethrowsNonSamplePrepareFailure() {
    IPSGuid pageId = new PSLegacyGuid(8003, 1);
    IPSGuid navonId = new PSLegacyGuid(9003, 1);
    when(contentSrv.prepareForEdit(navonId)).thenThrow(new IllegalStateException("duplicate slot"));

    assertThrows(
        IllegalStateException.class,
        () -> service.attachLandingPageWithoutForcedCheckin(pageId, navonId));
    verify(navService, never()).addLandingPageToNavnode(any(), any(), anyString());
  }

  @Test
  void createSectionDoesNotCheckinLandingOrNavon() throws Exception {
    PSCreateSiteSection req = new PSCreateSiteSection();
    req.setPageTitle("Products");
    req.setPageLinkTitle("Products");
    req.setPageUrlIdentifier("products");
    req.setPageName("products.html");
    req.setTemplateId("tpl-1");
    req.setFolderPath("//Sites/Corporate_Investments/Home");
    req.setCopyTemplates(Boolean.FALSE);

    IPSGuid parentId = new PSLegacyGuid(100, 1);
    IPSGuid navonId = new PSLegacyGuid(9004, 1);
    IPSGuid pageGuid = new PSLegacyGuid(8004, 1);
    PSFolder folder = new PSFolder("products", 501, 1001, 1, "");
    PSTemplateSummary template = new PSTemplateSummary();
    template.setId("tpl-1");
    PSPage saved = new PSPage();
    saved.setId("page-8004");
    PSItemStatus status = Mockito.mock(PSItemStatus.class);

    when(contentSrv.getIdByPath("//Sites/Corporate_Investments/Home/products")).thenReturn(null);
    when(contentSrv.getIdByPath("//Sites/Corporate_Investments/Home")).thenReturn(parentId);
    when(templateSrv.find("tpl-1")).thenReturn(template);
    when(contentSrv.isChildExistInFolder("//Sites/Corporate_Investments/Home", "products"))
        .thenReturn(false);
    when(contentSrv.addFolder("products", "//Sites/Corporate_Investments/Home", false))
        .thenReturn(folder);
    when(pageDaoHelper.getWorkflowIdForPath("//Sites/Corporate_Investments/Home/products"))
        .thenReturn(4);
    when(navService.addNavonToFolder(eq(parentId), any(), eq("products-Navon"), eq("Products"), eq(4)))
        .thenReturn(navonId);
    when(pageDao.save(any(PSPage.class))).thenReturn(saved);
    when(idMapper.getGuid("page-8004")).thenReturn(pageGuid);
    when(contentSrv.prepareForEdit(navonId)).thenReturn(status);
    when(idMapper.getString(any(IPSGuid.class))).thenReturn("9004-1");
    when(idMapper.getGuid("9004-1")).thenReturn(new PSLegacyGuid(9004, -1));
    when(publishingWs.getItemSites(any())).thenReturn(Collections.singletonList(site));
    when(site.isSecure()).thenReturn(false);

    PSSiteSection section = service.create(req);

    assertEquals("//Sites/Corporate_Investments/Home/products", section.getFolderPath());
    assertEquals("Products", section.getTitle());
    verify(navService).addLandingPageToNavnode(pageGuid, navonId, "perc.page");
    verify(contentSrv, never()).checkinItems(any(), eq(null));
  }

  @Test
  void sampleWorkflowDetectorMatchesCheckinNpeAndMissingState() {
    assertTrue(
        PSSiteSectionService.isSampleWorkflowLandingAttachFailure(new NullPointerException()));
    assertTrue(
        PSSiteSectionService.isSampleWorkflowLandingAttachFailure(
            new IllegalStateException("Field sys_contentstateid not found")));
    assertTrue(
        PSSiteSectionService.isSampleWorkflowLandingAttachFailure(
            new IllegalArgumentException("Workflow id is not loadable: 0")));
    assertFalse(
        PSSiteSectionService.isSampleWorkflowLandingAttachFailure(
            new IllegalStateException("duplicate slot")));
    assertFalse(PSSiteSectionService.isSampleWorkflowLandingAttachFailure(null));
  }
}
