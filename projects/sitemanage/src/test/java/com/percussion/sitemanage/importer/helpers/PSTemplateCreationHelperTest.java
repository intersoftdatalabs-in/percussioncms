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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.helpers;

import com.percussion.error.PSExceptionUtils;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.impl.PSPageManagementUtils;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.share.IPSSitemanageConstants;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSTemplateImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import com.percussion.sitemanage.importer.helpers.impl.PSTemplateCreationHelper;
import com.percussion.sitemanage.service.IPSSiteTemplateService;

import com.percussion.webservices.security.IPSSecurityWs;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author LucasPiccoli
 * 
 */
@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PSTemplateCreationHelperTest {

    private static final Logger log = LogManager.getLogger(PSTemplateCreationHelperTest.class);

    private static final String SITE_NAME = "PercussionTest";
    private static final String SITE_TEMPLATE_NAME = "PercussionTestTemplate";
    private String siteId;

    private PSTemplateCreationHelper templateCreationHelper;
    private IPSTemplateService templateService;
    private IPSPageService pageService;
    private IPSSecurityWs securityWs;
    private IPSiteDao siteDao;
    private IPSPageDao pageDao;
    private IPSAssemblyService assemblyService;
    private IPSIdMapper idMapper;
    private IPSSiteTemplateService siteTemplateService;

    @BeforeEach
    void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        securityWs.login("Admin", "demo", "Default", null);
        templateCreationHelper = new PSTemplateCreationHelper(templateService, pageDao, assemblyService, idMapper,
                siteTemplateService, pageService);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (StringUtils.isNotEmpty(siteId)) {
            siteDao.delete(siteId);
            siteId = "";
        }
    }

    /**
     * Tests if name is correctly extracted from different URLs. In case of URLs
     * where name cannot be extracted, tests if it's generated correctly.
     */
    @Test
    @Order(10)
    void testNameExtractionFromUrl() {
        var testPageName = "pageName";
        var testURL = "";
        var extractedPageName = "";

        testURL = "www.test.com/" + testPageName;
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals(testPageName, extractedPageName);

        testURL = "www.test.com";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals("", extractedPageName);

        testURL = "www.test.com/";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals("", extractedPageName);
        
        testURL = "https://www.test.com/";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals("", extractedPageName);

        testURL = "www.test.com/" + testPageName + ".html";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals(testPageName, extractedPageName);

        testURL = "http://www.test.com/blog/sports/";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals("sports", extractedPageName);
        
        testURL = "http://www.test.com/subpath/subpath/" + testPageName + ".html";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals(testPageName, extractedPageName);

        testURL = "www.test.com/" + testPageName + ".html?urlParam1=value";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals(testPageName, extractedPageName);

        testURL = "www.test.com/" + testPageName + "?urlParam1=value&urlParam2=value";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals(testPageName, extractedPageName);

        testURL = "www.test.com/" + testPageName + ";query";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals(testPageName, extractedPageName);

        testURL = "www.test.com/" + testPageName + ".html?urlParam=val;query";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals(testPageName, extractedPageName);

        testURL = "www.test.com/" + testPageName + "?urlParam=val;query";
        extractedPageName = templateCreationHelper.extractPageNameFromUrl(testURL);
        assertEquals(testPageName, extractedPageName);
    }

    /**
     * Tests if template and page creation methods work. Checks that the created
     * page uses the template.
     */
    @Test
    @Order(20)
    void testTemplateAndPageCreation() throws PSDataServiceException {
        var siteData = initSiteData();
        siteData = siteDao.save(siteData);
        siteId = siteData.getId();

        PSTemplateSummary newTemplate = null;
        try {
            newTemplate = templateService.createNewTemplate(siteData.getBaseTemplateName(), "Test-Template", siteData.getId());
        } catch (PSAssemblyException e) {
            fail();
        }
        assertNotNull(newTemplate);
        var newPage = templateCreationHelper.createNewPage("Test-Page", newTemplate.getId(), siteData.getFolderPath());
        assertNotNull(newPage);
        assertEquals(newTemplate.getId(), newPage.getTemplateId());
    }

    /**
     * Tests if page and template name generation works correctly. Name
     * generation has to avoid all possible naming collisions with existing
     * templates and pages.
     */
    @Test
    @Order(30)
    void testPageNameGeneration() throws PSDataServiceException {
        var siteData = initSiteData();
        siteData = siteDao.save(siteData);
        siteId = siteData.getId();

        var genericNewTemplateName = PSPageManagementUtils.TEMPLATE_NAME;
        var genericNewPageName = PSPageManagementUtils.PAGE_NAME;

        var templateName = siteTemplateService.generateNewTemplateName(genericNewTemplateName, siteData.getId());
        assertEquals("Template", templateName);

        PSTemplateSummary newTemplate = null;
        try {
            newTemplate = templateService.createNewTemplate(siteData.getBaseTemplateName(), templateName, siteData.getId());
        } catch (PSAssemblyException e) {
            fail();
        }

        templateName = siteTemplateService.generateNewTemplateName(genericNewTemplateName, siteData.getId());
        assertEquals("Template-1", templateName);

        var pageName = pageService.generateNewPageName(genericNewPageName, siteData.getFolderPath());
        assertEquals("Page", pageName);
        templateCreationHelper.createNewPage(pageName, newTemplate.getId(), siteData.getFolderPath());
        pageName = pageService.generateNewPageName(genericNewPageName, siteData.getFolderPath());
        assertEquals("Page-1", pageName);
        templateCreationHelper.createNewPage(pageName, newTemplate.getId(), siteData.getFolderPath());
        pageName = pageService.generateNewPageName(genericNewPageName, siteData.getFolderPath());
        assertEquals("Page-2", pageName);
    }

    /**
     * Tests main process method of templateCreationHelper is executed
     * succesfully and context object is correctly updated afterwards. Tests if
     * rollback method actually deletes everything the helper has created.
     */
    @Test
    @Order(40)
    void testHelperProcessAndRollback() {
        try {
            var siteData = initSiteData();
            siteData = siteDao.save(siteData);
            siteId = siteData.getId();

            var context = new PSSiteImportCtx();
            context.setSite(siteData);
            context.setLogger(new PSSiteImportLogger(PSLogObjectType.TEMPLATE));

            templateCreationHelper.process(null, context);

            assertNotNull(context.getTemplateId());
            assertNotNull(context.getPageName());
            assertNotNull(context.getTemplateName());
            assertNotNull(context.getLogger());
            assertNotNull(context.getLogger().getLog());
            assertTrue(context.getLogger().getLog().contains(PSTemplateCreationHelper.LOG_ENTRY_PREFIX));

            try {
                var templateSummary = templateService.find(context.getTemplateId());
                assertEquals(context.getTemplateId(), templateSummary.getId());
                assertEquals(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME, templateSummary.getSourceTemplateName());
            } catch (DataServiceLoadException e) {
                fail();
            }

            var page = pageService.findPage(context.getPageName(), siteData.getFolderPath());
            assertNotNull(page);
            assertEquals(context.getTemplateId(), page.getTemplateId());

            templateCreationHelper.rollback(null, context);

            var siteTemplates = siteTemplateService.findTemplatesBySite(context.getSite().getId());
            var templateNames = new ArrayList<String>();
            for (var template : siteTemplates) {
                templateNames.add(template.getName());
            }
            assertFalse(templateNames.contains(context.getTemplateName()));

            try {
                page = pageService.findPage(context.getPageName(), siteData.getFolderPath());
                assertNull(page);
            } catch (RuntimeException e) {
                // Expected: page not found after rollback
            }
        } catch (RuntimeException e) {
            fail();
        } catch (IPSPageService.PSPageException | PSTemplateImportException | PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    private PSSite initSiteData() {
        var newSite = new PSSite();
        newSite.setBaseUrl("http://www.percussion.com/");
        newSite.setName(SITE_NAME);
        newSite.setBaseTemplateName(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME);
        newSite.setTemplateName(SITE_TEMPLATE_NAME);
        newSite.setNavigationTitle(SITE_NAME);
        newSite.setHomePageTitle("Home");
        return newSite;
    }

    // Dependency injection setters for test context
    public void setTemplateService(IPSTemplateService templateService) {
        this.templateService = templateService;
    }

    public void setPageService(IPSPageService pageService) {
        this.pageService = pageService;
    }

    public void setSecurityWs(IPSSecurityWs securityWs) {
        this.securityWs = securityWs;
    }

    public void setSiteDao(IPSiteDao siteDao) {
        this.siteDao = siteDao;
    }

    public void setAssemblyService(IPSAssemblyService assemblyService) {
        this.assemblyService = assemblyService;
    }

    public void setIdMapper(IPSIdMapper idMapper) {
        this.idMapper = idMapper;
    }

    public void setPageDao(IPSPageDao pageDao) {
        this.pageDao = pageDao;
    }

    public void setSiteTemplateService(IPSSiteTemplateService siteTemplateService) {
        this.siteTemplateService = siteTemplateService;
    }

}
