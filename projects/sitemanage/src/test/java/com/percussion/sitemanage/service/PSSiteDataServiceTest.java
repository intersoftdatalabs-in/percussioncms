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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.assetmanagement.dao.IPSAssetDao;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.queue.IPSPageImportQueue;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.async.IPSAsyncJobService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.importer.dao.IPSImportLogDao;
import com.percussion.sitemanage.service.impl.PSSiteDataService;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import com.percussion.user.service.IPSUserService;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Scenario description: Tests the PSSiteDataService for correct handling of null returns from the
 * DAO. // REFACTORED: CP-JAVA11
 *
 * @author adamgent, Oct 7, 2009 (modernized by Sunny Sal)
 */
@Disabled("What is this thing?")
public class PSSiteDataServiceTest {

  private Mockery context = new JUnit4Mockery();

  private IPSSiteDataService sut;

  private IPSiteDao siteDao;
  private IPSPublishingWs publishingWs;
  private IPSSiteManager siteMgr;
  private IPSManagedNavService navService;
  private IPSIdMapper idMapper;
  private IPSSiteSectionService sectionService;
  private IPSContentWs contentWs;
  private IPSUserService userService;
  private IPSSiteTemplateService siteTemplateService;
  private IPSPageDao pageDao;
  private IPSItemWorkflowService itemWorkflowService;
  private IPSWidgetAssetRelationshipService widgetAssetRelationshipService;
  private IPSAssetDao assetDao;
  private IPSItemService itemService;
  private IPSFolderHelper folderHelper;
  private IPSSiteImportService siteImportService;
  private IPSAsyncJobService asyncJobService;
  private IPSPageImportQueue pageImportQueue;
  private IPSImportLogDao importLogDao;
  private IPSSiteImportSummaryService siteImportSummaryService;
  private IPSContentChangeService contentChangeService;
  private IPSRecentService recentService;

  @BeforeEach
  public void setUp() throws Exception {
    siteDao = context.mock(IPSiteDao.class);
    publishingWs = context.mock(IPSPublishingWs.class);
    siteMgr = context.mock(IPSSiteManager.class);
    navService = context.mock(IPSManagedNavService.class);
    idMapper = context.mock(IPSIdMapper.class);
    sectionService = context.mock(IPSSiteSectionService.class);
    contentWs = context.mock(IPSContentWs.class);
    userService = context.mock(IPSUserService.class);
    siteTemplateService = context.mock(IPSSiteTemplateService.class);
    pageDao = context.mock(IPSPageDao.class);
    itemWorkflowService = context.mock(IPSItemWorkflowService.class);
    widgetAssetRelationshipService = context.mock(IPSWidgetAssetRelationshipService.class);
    itemService = context.mock(IPSItemService.class);
    folderHelper = context.mock(IPSFolderHelper.class);
    siteImportService = context.mock(IPSSiteImportService.class);
    asyncJobService = context.mock(IPSAsyncJobService.class);
    pageImportQueue = context.mock(IPSPageImportQueue.class);
    recentService = context.mock(IPSRecentService.class);
    importLogDao = context.mock(IPSImportLogDao.class);
    contentChangeService = context.mock(IPSContentChangeService.class);
    siteImportSummaryService = context.mock(IPSSiteImportSummaryService.class);

    sut =
        new PSSiteDataService(
            siteDao,
            publishingWs,
            siteMgr,
            navService,
            idMapper,
            sectionService,
            contentWs,
            userService,
            null,
            siteTemplateService,
            pageDao,
            itemWorkflowService,
            widgetAssetRelationshipService,
            assetDao,
            itemService,
            folderHelper,
            siteImportService,
            asyncJobService,
            pageImportQueue,
            importLogDao,
            siteImportSummaryService,
            contentChangeService,
            recentService);
    // TODO: Wire the DAO to the SUT if needed.
  }

  @Test
  public void shouldHandleNullFromFindAllOnDao() {
    // Given: siteDao.findAllSummaries() returns null
    context.checking(
        new Expectations() {
          {
            one(siteDao).findAllSummaries();
            will(returnValue(null));
          }
        });

    // When: findAll is called on the SUT
    var sites = sut.findAll();

    // Then: sites should not be null and should be empty
    assertNotNull(sites, "Returned site list should not be null");
    assertTrue(sites.isEmpty(), "Returned site list should be empty when DAO returns null");
  }
}
