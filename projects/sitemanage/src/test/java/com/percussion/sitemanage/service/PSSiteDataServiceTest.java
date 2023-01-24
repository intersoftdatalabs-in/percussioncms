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
package com.percussion.sitemanage.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.importer.dao.IPSImportLogDao;
import com.percussion.sitemanage.service.impl.PSSiteDataService;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.publishing.IPSPublishingWs;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

//import static java.util.Arrays.*;
//import static org.hamcrest.CoreMatchers.*;
//import static org.junit.matchers.JUnitMatchers.*;

/**
 * Scenario description: 
 * @author adamgent, Oct 7, 2009
 */
@RunWith(JMock.class)
@Ignore("What is this thing?")
@Category(IntegrationTest.class)
public class PSSiteDataServiceTest
{

    Mockery context = new JUnit4Mockery();

    IPSSiteDataService sut;

    IPSiteDao siteDao;
    IPSPublishingWs publishingWs;
    IPSSiteManager siteMgr;
    IPSManagedNavService navService;
    IPSIdMapper idMapper;
    IPSSiteSectionService sectionService;
    IPSContentWs contentWs;
    IPSUserService userService;
    IPSSiteTemplateService siteTemplateService;
    IPSPageDao pageDao;
    IPSItemWorkflowService itemWorkflowService;
    IPSWidgetAssetRelationshipService widgetAssetRelationshipService;
    IPSAssetDao assetDao;
    IPSItemService itemService;
    IPSFolderHelper folderHelper;
    IPSSiteImportService siteImportService;
    IPSAsyncJobService asyncJobService;
    IPSPageImportQueue pageImportQueue;
    IPSImportLogDao importLogDao;
    IPSSiteImportSummaryService siteImportSummaryService;
    IPSContentChangeService contentChangeService;
    IPSRecentService recentService;
        
    @Before
    public void setUp() throws Exception
    {
        
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
        sut = new PSSiteDataService(siteDao, publishingWs, siteMgr, navService, idMapper, sectionService, contentWs,
                userService, null, siteTemplateService, pageDao, itemWorkflowService, widgetAssetRelationshipService,
                assetDao, itemService, folderHelper, siteImportService, asyncJobService, pageImportQueue,importLogDao,siteImportSummaryService,
                contentChangeService, recentService);
        //TODO !!! wire the dao to the SUT ie call the setter !!!
 
    }
    
    @Test
    public void shouldHandleNullFromFindAllOnDao()
    {
        /*
         * Given: TODO initial setup.
         */
        

        /* 
         * Expect: TODO expect some methods to be called on the dao.
         */

        context.checking(new Expectations() {{
            one(siteDao).findAllSummaries();
            will(returnValue(null));
        }});

        /*
         * When: TODO executes HandleNullFromFindAllOnDao on the SUT
         */
        List<PSSiteSummary> sites =  sut.findAll();

        /*
         * Then: TODO check to see if the behavior is correct.
         */
        assertNotNull(sites);
        assertTrue(sites.isEmpty());

    }

}
