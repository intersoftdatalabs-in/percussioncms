/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.sitemanage.service;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSPageTemplateService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.async.IPSAsyncJobService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.service.PSSiteTemplates.CreateTemplate;
import com.percussion.sitemanage.service.impl.PSSiteTemplateService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Scenario description: Validation for site templates. // REFACTORED: CP-JAVA11
 *
 * @author adamgent, Oct 14, 2009 (modernized by Sunny Sal)
 */
@Tag("IntegrationTest")
public class PSSiteTemplateServiceValidationTest {

  private Mockery context = new JUnit4Mockery();
  private PSSiteTemplateService sut;

  private IPSTemplateService templateService;
  private IPSSiteSectionMetaDataService siteSectionMetaDataService;
  private IPSiteDao siteDao;
  private IPSSiteImportService siteImportService;
  private IPSSiteManager siteMgr;
  private IPSAsyncJobService asyncJobService;
  private IPSPageService pageService;
  private IPSAssetService assetService;
  private IPSItemWorkflowService itemWorkflowService;
  private IPSWidgetAssetRelationshipService widgetAssetRelationshipService;
  private IPSPageTemplateService pageTemplateService;
  private IPSFolderHelper folderHelper;

  @BeforeEach
  public void setUp() {
    templateService = context.mock(IPSTemplateService.class);
    siteSectionMetaDataService = context.mock(IPSSiteSectionMetaDataService.class);
    siteDao = context.mock(IPSiteDao.class);
    asyncJobService = context.mock(IPSAsyncJobService.class);
    pageService = context.mock(IPSPageService.class);
    assetService = context.mock(IPSAssetService.class);
    itemWorkflowService = context.mock(IPSItemWorkflowService.class);
    widgetAssetRelationshipService = context.mock(IPSWidgetAssetRelationshipService.class);
    pageTemplateService = context.mock(IPSPageTemplateService.class);
    siteMgr = context.mock(IPSSiteManager.class);
    folderHelper = context.mock(IPSFolderHelper.class);

    sut =
        new PSSiteTemplateService(
            siteDao,
            siteSectionMetaDataService,
            templateService,
            asyncJobService,
            pageService,
            assetService,
            itemWorkflowService,
            widgetAssetRelationshipService,
            pageTemplateService,
            siteMgr,
            folderHelper);
  }

  @Test
  public void shouldValidateSiteTemplatesAndNOTFailForEmptySiteTemplates()
      throws PSBeanValidationException {
    var siteTemplates = new PSSiteTemplates();
    context.checking(
        new Expectations() {
          {
            // No expectations, just validation
          }
        });
    sut.validate(siteTemplates);
    // No exception expected
  }

  @Test
  public void shouldValidateCreateTemplatesAndFailIfNoSourceTemplateId() {
    var siteTemplates = new PSSiteTemplates();
    var ct = new CreateTemplate();
    ct.setName("SetName");
    ct.setSourceTemplateId(null); // null source template id is invalid
    siteTemplates.getCreateTemplates().add(ct);
    context.checking(
        new Expectations() {
          {
            // No expectations, just validation
          }
        });
    assertThrows(PSBeanValidationException.class, () -> sut.validate(siteTemplates));
  }

  @Test
  public void shouldValidateCreateTemplates() throws PSBeanValidationException {
    var siteTemplates = new PSSiteTemplates();
    var ct = new CreateTemplate();
    ct.setName("SetName");
    ct.setSourceTemplateId("sourceTemplateId");
    siteTemplates.getCreateTemplates().add(ct);
    context.checking(
        new Expectations() {
          {
            // No expectations, just validation
          }
        });
    sut.validate(siteTemplates);
    // No exception expected
  }
}
