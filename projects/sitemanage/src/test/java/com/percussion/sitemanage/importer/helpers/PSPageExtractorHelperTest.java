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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.helpers;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;
import static org.springframework.util.StringUtils.countOccurrencesOf;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSAssetSummary;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSNameGenerator;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.dao.impl.PSSiteContentDao;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import com.percussion.sitemanage.importer.PSSiteImporter;
import com.percussion.sitemanage.importer.helpers.impl.PSImportHelper;
import com.percussion.sitemanage.importer.helpers.impl.PSPageExtractorHelper;
import com.percussion.sitemanage.importer.helpers.impl.PSSiteCreationHelper;
import com.percussion.sitemanage.importer.helpers.impl.PSTemplateCreationHelper;
import com.percussion.sitemanage.importer.helpers.impl.PSTemplateExtractorHelper;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.security.IPSSecurityWs;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

@TestMethodOrder(OrderAnnotation.class)
@Tag("IntegrationTest")
public class PSPageExtractorHelperTest {

    private final String TEST_SITE_NAME = "TestImportedSite";
    private final String TEST_SITE_URL = "http://www.test.com";
    private final String TEST_THEME_NAME = "ThemeName";

    private boolean siteCreationHelperExecuted = false;
    private boolean templateCreationHelperExecuted = false;

    private PSSiteDataServletTestCaseFixture fixture;
    private PSPageContent pageContent;
    private PSSiteImportCtx importContext;
    private PSTemplateExtractorHelper templateExtractorHelper;
    private PSSiteCreationHelper siteCreationHelper;
    private PSTemplateCreationHelper templateCreationHelper;
    private PSPageExtractorHelper pageExtractorHelper;
    private IPSSecurityWs securityWs;
    private IPSPageService pageService;
    private IPSAssetService assetService;
    private IPSWidgetAssetRelationshipService widgetAssetRelationshipService;
    private IPSItemWorkflowService itemWorkflowService;
    private IPSTemplateService templateService;
    private IPSNameGenerator nameGenerator;
    private IPSiteDao siteDao;
    private IPSPageDao pageDao;
    private IPSAssemblyService assemblyService;
    private IPSIdMapper idMapper;
    private IPSSiteTemplateService siteTemplateService;
    private IPSPageCatalogService pageCatalogService;

    private static final String[] PERCUSSION_TAGS = new String[]{
            "<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.8/jquery.js\" type=\"text/javascript\"></script>",
            "<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js\" type=\"text/javascript\"></script>",
            "<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js\" type=\"text/javascript\"></script>",
            "<script src=\"jquery.js\" type=\"text/javascript\"></script>",
            "<script src=\"jquery.ui.core.js\" type=\"text/javascript\"></script>",
            "<script src=\"/scripts/js/jquery.ui.js\" type=\"text/javascript\"></script>",
            "<script src=\"jquery.tools.min.js\" type=\"text/javascript\"></script>",
            "<script src=\"/scripts/custom/js/jquery-latest.js\" type=\"text/javascript\"></script>",
            "<script src=\"http://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.7.1.min.js\" type=\"text/javascript\"></script>",
            "<script src=\"http://ajax.aspnetcdn.com/ajax/jquery.ui/1.8.18/jquery-ui.min.js\" type=\"text/javascript\"></script>"};

    private static final String COMMENT_START = "<!--";
    private static final String COMMENT_END = "-->";

    private static final String[] NOT_MANAGED_TAGS = new String[]{
            "<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.dialog.min.js\" type=\"text/javascript\"></script>",
            "<script src=\"jquery.carrousel-1.4.2.min.js\" type=\"text/javascript\"></script>",
            "<script src=\"/scripts/js/jquery.ui.carrousel-1.1.js\" type=\"text/javascript\"></script>",
            "<script src=\"http://ajax.aspnetcdn.com/ajax/jQuery/jquery-animation.1.7.1.min.js\" type=\"text/javascript\"></script>",};

    @BeforeEach
    public void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        // Login is needed to create folder for the new site.
        securityWs.login("Admin", "demo", "Default", null);
        createHelpers();
        initData();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (siteCreationHelperExecuted) {
            deleteSite();
            siteCreationHelperExecuted = false;
        }
        if (templateCreationHelperExecuted) {
            fixture.tearDown();
            templateCreationHelperExecuted = false;
        }
    }

    @Test
    @Order(10)
    public void testAddBodyContentToPageWhenImportingSite() throws PSDataServiceException, PSSiteImportException {
        var site = new PSSite();
        site.setBaseUrl(TEST_SITE_URL);
        site.setName(TEST_SITE_NAME);
        importContext.setSite(site);

        siteCreationHelper.process(pageContent, importContext);
        siteCreationHelperExecuted = true;
        templateExtractorHelper.process(pageContent, importContext);
        pageExtractorHelper.process(pageContent, importContext);

        var homePage = pageService.findPage(PSSiteContentDao.HOME_PAGE_NAME, importContext.getSite().getFolderPath());
        assertNotNull(homePage);
        var template = templateService.load(homePage.getTemplateId());
        assertNotNull(template);

        var assetIds = widgetAssetRelationshipService.getLocalAssets(homePage.getId());
        assertFalse(assetIds.isEmpty());
        var assetId = assetIds.iterator().next();
        var assetSummary = assetService.find(assetId);
        assertEquals("percRawHtmlAsset", assetSummary.getType());

        var asset = assetService.load(assetSummary.getId());
        assertTrue(asset.getFields().containsKey("html"));
        var bodyContent = (String) asset.getFields().get("html");
        assertNotNull(bodyContent);
        assertEquals(normalizeHtml(pageContent.getBodyContent()), normalizeHtml(bodyContent));

        validateJSReferencesTags(bodyContent);
        validateLogsForJSTags(importContext.getLogger());
    }

    @Test
    @Order(20)
    public void testAddBodyContentToPageWhenImportingTemplate() throws Exception {
        createFixture();
        importContext.setSite((PSSite) fixture.site1);

        templateCreationHelper.process(pageContent, importContext);
        templateCreationHelperExecuted = true;
        templateExtractorHelper.process(pageContent, importContext);
        pageExtractorHelper.process(pageContent, importContext);

        var homePage = pageService.findPage(importContext.getPageName(), importContext.getSite().getFolderPath());
        assertNotNull(homePage);
        var template = templateService.load(importContext.getTemplateId());
        assertNotNull(template);

        var assetIds = widgetAssetRelationshipService.getLocalAssets(homePage.getId());
        assertFalse(assetIds.isEmpty());
        var assetId = assetIds.iterator().next();
        var assetSummary = assetService.find(assetId);
        assertEquals("percRawHtmlAsset", assetSummary.getType());

        var asset = assetService.load(assetSummary.getId());
        assertTrue(asset.getFields().containsKey("html"));
        var bodyContent = (String) asset.getFields().get("html");
        assertNotNull(bodyContent);
        assertEquals(normalizeHtml(pageContent.getBodyContent()), normalizeHtml(bodyContent));

        validateJSReferencesTags(bodyContent);
        validateLogsForJSTags(importContext.getLogger());
    }

    private String normalizeHtml(String html) {
        var doc = Jsoup.parseBodyFragment(html);
        return doc.body().html();
    }

    private void createHelpers() {
        siteCreationHelper = new PSSiteCreationHelper(siteDao, pageService);
        templateCreationHelper = new PSTemplateCreationHelper(templateService, pageDao, assemblyService, idMapper, siteTemplateService, pageService);
        templateExtractorHelper = new PSTemplateExtractorHelper(templateService);
        pageExtractorHelper = new PSPageExtractorHelper(pageService, assetService, itemWorkflowService, templateService, nameGenerator, idMapper);
        pageExtractorHelper.setRunSaveSyncronously(true);
    }

    private void initData() throws Exception {
        importContext = new PSSiteImportCtx();
        importContext.setLogger(new PSSiteImportLogger(PSLogObjectType.SITE));
        var summaryService = (IPSSiteImportSummaryService) getWebApplicationContext().getBean("siteImportSummaryService");
        importContext.setSummaryService(summaryService);

        try (InputStream in = getClass().getResourceAsStream("CM1094-SamplePage.html");
             OutputStream out = new FileOutputStream(File.createTempFile("samplePage", ".html"))) {
            IOUtils.copy(in, out);
        }
        var pageSampleFile = File.createTempFile("samplePage", ".html");
        var doc = Jsoup.parse(pageSampleFile, "UTF-8");
        pageContent = PSSiteImporter.createPageContent(doc, importContext.getLogger());

        var themeSummary = new PSThemeSummary();
        themeSummary.setName(TEST_THEME_NAME);
        importContext.setThemeSummary(themeSummary);
    }

    private void deleteSite() {
        siteCreationHelper.rollback(pageContent, importContext);
    }

    private void createFixture() throws Exception {
        fixture = new PSSiteDataServletTestCaseFixture(request, response);
        fixture.setUp();
        fixture.pageCleaner.add(fixture.site1.getFolderPath() + "/Page1");
    }

    private void validateLogsForJSTags(IPSSiteImportLogger logger) {
        var log = logger.getLog();
        for (var percussionTag : PERCUSSION_TAGS) {
            var logLine = buildLogLineForCommentedJSElement(percussionTag);
            assertTrue(log.contains(logLine), "The LOG should contain the following line:  '" + logLine + "'.");
