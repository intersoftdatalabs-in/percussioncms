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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.percussion.comments.service.IPSCommentsService;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSCreateSectionFromFolderRequest;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Regression for GH-866 / v8.1.7 PR #869: when the source folder cannot be found, validation stops
 * after the primary reject and does not also emit parent-path / landing-page errors.
 */
class PSCreateSectionFromFolderValidatorEarlyReturnTest {

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
  }

  @Test
  void missingSourceFolderEmitsSinglePrimaryError() throws Exception {
    when(folderHelper.findFolder(anyString())).thenThrow(new RuntimeException("not found"));

    var req = new PSCreateSectionFromFolderRequest();
    req.setSourceFolderPath("/Sites/Site1/missing");
    req.setParentFolderPath("/Sites/Site1");
    req.setPageName("index.html");

    var validator = service.new PSCreateSectionFromFolderValidator();
    var errors = validator.validate(req);

    assertTrue(errors.hasErrors(), "expected at least one validation error");
    // Only the source-folder reject — not the parent-section or landing-page follow-ons.
    assertEquals(1, errors.getErrorCount(), "must stop after primary folder-not-found reject");
    String all = String.valueOf(errors);
    assertTrue(
        all.contains("folder with that path cannot be found") || all.contains("cannot be found"));
    assertFalse(
        all.contains("parent path"),
        "must not continue into parent-path validation after source folder missing: " + all);
  }
}
