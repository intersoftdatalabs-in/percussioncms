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
import static org.springframework.util.StringUtils.countOccurrencesOf;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
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
import com.percussion.sitemanage.importer.helpers.impl.PSSiteCreationHelper;
import com.percussion.sitemanage.importer.helpers.impl.PSTemplateExtractorHelper;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.security.IPSSecurityWs;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PSTemplateExtractorHelperTest {

    private static final String TEST_SITE_NAME = "TestImportedSite";
    private static final String TEST_SITE_URL = "http://www.test.com";
    private static final String TEST_THEME_NAME = "ThemeName";
    private static final String[] PERCUSSION_TAGS = new String[]{
            "<title>Web Content Management Software (WCM) | Percussion Software</title>",
            "<meta content=\"text/html; charset=UTF-8\" http-equiv=\"content-type\" />",
            "<meta name=\"generator\" content=\"Percussion\" />", "<meta name=\"robots\" content=\"noindex\" />",
            "<meta name=\"description\" content=\"The description of the page\" />",
            "<meta property=\"dcterms:author\" content=\"author of the page\" />",
            "<meta property=\"dcterms:type\" content=\"page\" />",
            "<meta property=\"dcterms:source\" content=\"perc.template.name\" />",
            "<meta property=\"dcterms:created\" datatype=\"xsd:dateTime\" content=\"2012-10-10\" />",
            "<meta property=\"dcterms:alternative\" content=\"perc.page.linkTitle\" />",
            "<meta property=\"perc:tags\" content=\"tag1.String\" />",
            "<meta property=\"perc:tags\" content=\"tag2.String\" />",
            "<meta property=\"perc:category\" content=\"category.String\" />",
            "<meta property=\"perc:calendar\" content=\"Calendar Name\" />",
            "<meta property=\"perc:start_date\" content=\"10/12/2012\" />",
            "<meta property=\"perc:end_date\" datatype=\"xsd:dateTime\" content=\"10/12/2012\" />",};
    private static final String[] PERCUSSION_JS_REFERENCES = new String[]{
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
            "<meta http-equiv=\"refresh\" content=\"600\" />",
            "<meta http-equiv=\"default-style\" content=\"link_element\" />",
            "<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.dialog.min.js\" type=\"text/javascript\"></script>",
            "<script src=\"jquery.carrousel-1.4.2.min.js\" type=\"text/javascript\"></script>",
            "<script src=\"/scripts/js/jquery.ui.carrousel-1.1.js\" type=\"text/javascript\"></script>",
            "<script src=\"http://ajax.aspnetcdn.com/ajax/jQuery/jquery-animation.1.7.1.min.js\" type=\"text/javascript\"></script>",
            "<script language=\"javascript\" type=\"text/javascript\">llactid=10810</script>",
            "<SCRIPT TYPE=\"text/javascript\" LANGUAGE=\"JavaScript\" SRC=\"/web_resources/themes/perc-web/elqNow/elqCfg.js\"></SCRIPT>",
            "<SCRIPT TYPE=\"text/javascript\" LANGUAGE=\"JavaScript\" SRC=\"/web_resources/themes/perc-web/elqNow/elqImg.js\"></SCRIPT>"};

    private PSPageContent pageContent;
    private PSSiteImportCtx importContext;
    private PSTemplateExtractorHelper templateExtractorHelper;
    private PSSiteCreationHelper siteCreationHelper;
    private IPSSecurityWs securityWs;
    private IPSPageService pageService;
    private IPSTemplateService templateService;
    private IPSiteDao siteDao;

    @BeforeEach
    void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        securityWs.login("Admin", "demo", "Default", null);
        siteCreationHelper = new PSSiteCreationHelper(siteDao, pageService);
        templateExtractorHelper = new PSTemplateExtractorHelper(templateService);
        initData();
        createSite();
    }

    @AfterEach
    void tearDown() {
        deleteSite();
    }

    private void initData() throws Exception {
        var pageSampleFile = createTempConfigFileBasedOn(getClass().getResourceAsStream("CM1094-SamplePage.html"));
        Document doc = Jsoup.parse(pageSampleFile, "UTF-8");
        pageContent = PSSiteImporter.createPageContent(doc, new PSSiteImportLogger(PSLogObjectType.TEMPLATE));
        importContext = new PSSiteImportCtx();
        importContext.setLogger(new PSSiteImportLogger(PSLogObjectType.SITE));
        var summaryService = (IPSSiteImportSummaryService) getWebApplicationContext().getBean("siteImportSummaryService");
        importContext.setSummaryService(summaryService);
        var site = new PSSite();
        site.setBaseUrl(TEST_SITE_URL);
        site.setName(TEST_SITE_NAME);
        importContext.setSite(site);
        var themeSummary = new PSThemeSummary();
        themeSummary.setName(TEST_THEME_NAME);
        importContext.setThemeSummary(themeSummary);
    }

    private File createTempConfigFileBasedOn(InputStream baseConfigFile) throws Exception {
        var tempConfigFile = File.createTempFile("samplePage", ".html");
        try (OutputStream out = new FileOutputStream(tempConfigFile); InputStream in = baseConfigFile) {
            IOUtils.copy(in, out);
        }
        return tempConfigFile;
    }

    private void createSite() throws PSSiteImportException {
        siteCreationHelper.process(pageContent, importContext);
    }

    private void deleteSite() {
        siteCreationHelper.rollback(pageContent, importContext);
    }

    @Test
    @Order(10)
    void testExtractMetadata() throws PSSiteImportException, PSDataServiceException {
        templateExtractorHelper.process(pageContent, importContext);

        var homePage = pageService.findPage(PSSiteContentDao.HOME_PAGE_NAME, importContext.getSite().getFolderPath());
        assertNotNull(homePage);
        var template = templateService.load(homePage.getTemplateId());
        assertNotNull(template);

        assertNotNull(template.getAdditionalHeadContent());
        assertNotEquals("", template.getAdditionalHeadContent());

        assertEquals(
                pageContent.getHeadContent().replaceAll("(\\r|\\n)", "").replaceAll("<!--", "").replaceAll("-->", ""),
                template.getAdditionalHeadContent().replaceAll("(\\r|\\n)", "").replaceAll("<!--", "")
                        .replaceAll("-->", ""));

        validatePercussionGeneratedTags(template.getAdditionalHeadContent());
        validatePercussionJSReferences(template.getAdditionalHeadContent(), "Additional Head Content");
        validatePercussionJSReferences(template.getAfterBodyStartContent(), "After Body Start Content");
        validatePercussionJSReferences(template.getBeforeBodyCloseContent(), "Before Body close Content");
        validateLogsForManagedTags(importContext.getLogger());

        assertNotNull(template.getAfterBodyStartContent());
        assertNotEquals("", template.getAfterBodyStartContent());
        assertEquals(pageContent.getAfterBodyStart(), template.getAfterBodyStartContent());

        assertNotNull(template.getBeforeBodyCloseContent());
        assertNotEquals("", template.getBeforeBodyCloseContent());
        assertEquals(pageContent.getBeforeBodyClose(), template.getBeforeBodyCloseContent());

        assertNotNull(template.getTheme());
        assertNotEquals("", template.getTheme());
        assertEquals(importContext.getThemeSummary().getName(), template.getTheme());
    }

    @Test
    @Order(20)
    void testAddHTMLWidgetToTemplate() throws PSDataServiceException, PSSiteImportException {
        templateExtractorHelper.process(pageContent, importContext);

        var homePage = pageService.findPage(PSSiteContentDao.HOME_PAGE_NAME, importContext.getSite().getFolderPath());
        assertNotNull(homePage);
        var template = templateService.load(homePage.getTemplateId());
        assertNotNull(template);

        List<PSWidgetItem> templateWidgets = template.getWidgets();
        assertNotNull(templateWidgets);
        assertTrue(templateWidgets.size() > 0);
        var widget = templateWidgets.get(0);
        assertEquals("percRawHtml", widget.getDefinitionId());
    }

    // Dependency injection setters for test context
    public void setSecurityWs(IPSSecurityWs securityWs) {
        this.securityWs = securityWs;
    }

    public void setPageService(IPSPageService pageService) {
        this.pageService = pageService;
    }

    public void setTemplateService(IPSTemplateService templateService) {
        this.templateService = templateService;
    }

    public void setSiteDao(IPSiteDao siteDao) {
        this.siteDao = siteDao;
    }

    private void validatePercussionGeneratedTags(String templateField) {
        for (var percussionTag : PERCUSSION_TAGS) {
            assertTrue(templateField.contains(COMMENT_START + percussionTag + COMMENT_END),
                    "The tag '" + percussionTag + "' should have been commented.");
        }
        for (var notManagedTag : NOT_MANAGED_TAGS) {
            assertFalse(templateField.contains(COMMENT_START + notManagedTag + COMMENT_END),
                    "The tag '" + notManagedTag + "' should not have been commented.");
        }
    }

    private void validatePercussionJSReferences(String templateField, String place) {
        for (var percussionTag : PERCUSSION_JS_REFERENCES) {
            assertTrue(templateField.contains(COMMENT_START + percussionTag + COMMENT_END),
                    "The tag '" + percussionTag + "' should have been commented in '" + place + "'.");
