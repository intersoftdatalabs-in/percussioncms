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
package com.percussion.sitemanage.importer.helpers;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.share.dao.IPSGenericDao.LoadException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.dao.impl.PSSiteContentDao;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import com.percussion.sitemanage.importer.helpers.impl.PSSiteCreationHelper;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.security.IPSSecurityWs;
import org.junit.jupiter.api.*;

@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PSSiteCreationHelperTest {

    private static final String TEST_SITE_NAME = "TestImportedSite";
    private static final String TEST_SITE_URL = "http://www.test.com";
    private static final String TEST_PAGE_TITLE = "TestTitle";

    private PSPageContent pageContent;
    private PSSiteImportCtx importContext;
    private PSSiteCreationHelper siteCreationHelper;
    private IPSiteDao siteDao;
    private IPSSecurityWs securityWs;
    private IPSPageService pageService;
    private IPSTemplateService templateService;

    @BeforeEach
    void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        securityWs.login("Admin", "demo", "Default", null);
        siteCreationHelper = new PSSiteCreationHelper(siteDao, pageService);
        initData();
    }

    private void initData() {
        pageContent = new PSPageContent();
        pageContent.setTitle(TEST_PAGE_TITLE);
        importContext = new PSSiteImportCtx();
        importContext.setLogger(new PSSiteImportLogger(PSLogObjectType.SITE));
        var summaryService = (IPSSiteImportSummaryService) getWebApplicationContext().getBean("siteImportSummaryService");
        importContext.setSummaryService(summaryService);
        var site = new PSSite();
        site.setBaseUrl(TEST_SITE_URL);
        site.setName(TEST_SITE_NAME);
        importContext.setSite(site);
    }

    @Test
    @Order(10)
    void testCreateSite() throws PSDataServiceException, PSSiteImportException {
        assertNotNull(siteCreationHelper);
        siteCreationHelper.process(pageContent, importContext);

        var siteSummary = siteDao.findSummary(importContext.getSite().getName());
        assertNotNull(siteSummary);

        var homePage = pageService.findPage(PSSiteContentDao.HOME_PAGE_NAME, importContext.getSite().getFolderPath());
        assertNotNull(homePage);
        assertEquals(TEST_PAGE_TITLE, homePage.getTitle());

        var template = templateService.load(homePage.getTemplateId());
        assertNotNull(template);
        assertEquals("Home", template.getName());
    }

    @Test
    @Order(20)
    void testRollback() {
        siteCreationHelper.rollback(pageContent, importContext);
        try {
            var siteSummary = siteDao.findSummary(importContext.getSite().getName());
            assertNull(siteSummary);
        } catch (LoadException ex) {
            // Expected: site not found after rollback
        }
    }

    // Dependency injection setters for test context
    public void setSiteDao(IPSiteDao siteDao) {
        this.siteDao = siteDao;
    }

    public void setSecurityWs(IPSSecurityWs securityWs) {
        this.securityWs = securityWs;
    }

    public void setPageService(IPSPageService pageService) {
        this.pageService = pageService;
    }

    public void setTemplateService(IPSTemplateService templateService) {
        this.templateService = templateService;
    }
}
