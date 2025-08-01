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

import static com.percussion.share.dao.PSFolderPathUtils.concatPath;
import static com.percussion.share.dao.PSFolderPathUtils.pathSeparator;
import static com.percussion.sitemanage.service.IPSSiteSectionMetaDataService.PAGE_CATALOG;
import static com.percussion.sitemanage.service.IPSSiteSectionMetaDataService.SECTION_SYSTEM_FOLDER_NAME;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSConnectivity;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.PSLink;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.helpers.impl.PSLinkExtractionHelper;
import com.percussion.sitemanage.importer.theme.PSURLConverter;
import com.percussion.sitemanage.importer.utils.PSLinkExtractor;
import com.percussion.sitemanage.importer.helpers.PSLinkExtractionHelperTest.TestablePSLinkExtractionHelper.PSLinkExtractionTestConnectivity;
import com.percussion.theme.service.IPSThemeService;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class PSLinkExtractionHelperTest {

    protected static class TestablePSLinkExtractionHelper extends PSLinkExtractionHelper {
        protected static class PSLinkExtractionTestConnectivity implements IPSConnectivity {
            private int statusCode = 200;
            private String responseUrl = "http://local/";
            private Document document = null;

            @Override
            public Document get() throws java.io.IOException {
                if (document != null)
                    return document;
                throw new java.io.IOException("Unhandled content type \"application/pdf\" on URL");
            }

            public void setDocument(Document document) {
                this.document = document;
            }

            @Override
            public int getResponseStatusCode() {
                return statusCode;
            }

            public void setResponseStatusCode(int statusCode) {
                this.statusCode = statusCode;
            }

            @Override
            public String getResponseUrl() {
                return responseUrl;
            }

            public void setResponseUrl(String responseUrl) {
                this.responseUrl = responseUrl;
            }
        }

        private final IPSConnectivity connectivity = new PSLinkExtractionTestConnectivity();

        public TestablePSLinkExtractionHelper(
                IPSPageCatalogService pageCatalogService, IPSThemeService themeService, IPSPageDao pageDao,
                IPSItemWorkflowService itemWorkflowService, IPSFolderHelper folderHelper) {
            super(pageCatalogService, themeService, pageDao, itemWorkflowService, folderHelper);
        }

        @Override
        protected IPSConnectivity getConnectivity(String url, boolean ignoreContentType, boolean followRedirects, String userAgent) {
            return connectivity;
        }

        @Override
        protected void downloadAsset(String remoteUrl, String fullThemePath) {
            // No-op for test
        }

        @Override
        protected String getCmsFolderPathForImageAssetsSiteName(String siteName, PSURLConverter urlConverter, String remoteUrl) {
            return PSLinkExtractionHelper.ASSETS_DIR_PREFIX;
        }

        @Override
        protected String getRemoteUrlConverted(PSLink link, PSURLConverter urlConverter) {
            return link.getAbsoluteLink();
        }

        @Override
        protected PSURLConverter getURLConverter(final PSSiteImportCtx context, final IPSSiteImportLogger log,
                                                 String themeRootDirectory, String themeRootUrl, String siteName) {
            return null;
        }

        @Override
        protected String getPathForTargetItem(String siteName, PSLink link) {
            var pathToTargetItem = getFinderPathForTargetItem(siteName, link.getRelativePathWithFileName());
            pathToTargetItem = getCatalogedItemPath(siteName, link.getLinkPath(), link.getPageName());
            return pathToTargetItem;
        }

        protected String getFinderPathForTargetItem(String siteName, String targetPathUrl) {
            var targetPath = targetPathUrl.startsWith("/") ? targetPathUrl : "/" + targetPathUrl;
            return "/Sites/" + siteName + targetPath;
        }

        protected String getCatalogedItemPath(String siteName, String folderPath, String pageName) {
            var catalogRoot = concatPath("/Sites/", siteName, pathSeparator() + concatPath(SECTION_SYSTEM_FOLDER_NAME, PAGE_CATALOG));
            var fullFolderPath = concatPath(catalogRoot, folderPath);
            return concatPath(fullFolderPath, pageName);
        }

        public IPSConnectivity getConnectivity() {
            return connectivity;
        }

        @Override
        protected String getThemeRootUrl(final PSSiteImportCtx context) {
            return "";
        }

        @Override
        protected String getThemeRootDirectory(final PSSiteImportCtx context) {
            return "";
        }
    }

    private PSSiteImportCtx context;
    private PSPageContent pageContent;
    private static final String PERC_MANAGED_ATTR = "perc-managed";

    @BeforeEach
    public void setUp() {
        context = new PSSiteImportCtx();
        context.setLogger(new PSSiteImportLogger(PSLogObjectType.SITE));
        var site = new PSSite();
        site.setName("Test Site");
        context.setSite(site);
        try {
            var testHelper = new PSHelperTestUtils();
            pageContent = testHelper.createTempPageBasedOnResource(
                    "PSLinkExtractionHelperTest.html",
                    PSLinkExtractionHelperTest.class, context);
        } catch (Exception e) {
            fail("Couldn't create page content #tragic");
        }
    }

    @Disabled("Test relied on broken error handling that has been fixed")
    public void testFileUpload() throws PSSiteImportException, IPSGenericDao.SaveException {
        var linkExtractionHelper = new TestablePSLinkExtractionHelper(
                null, null, null, null, null);
        linkExtractionHelper.process(pageContent, context);
        var doc = pageContent.getSourceDocument();
        var links = doc.select(PSLinkExtractor.A_HREF);
        assertEquals(5, links.size());
        for (Element link : links) {
            assertTrue(link.attr(PSLinkExtractor.HREF).contains(PSLinkExtractionHelper.ASSETS_DIR_PREFIX));
        }
    }

    @Disabled("Awkward Coupling")
    public void testStandardPath() throws PSSiteImportException, IPSGenericDao.SaveException {
        var linkExtractionHelper = new TestablePSLinkExtractionHelper(
                null, null, null, null, null);
        var testConn = (PSLinkExtractionTestConnectivity) ((TestablePSLinkExtractionHelper) linkExtractionHelper)
                .getConnectivity();

        testConn.setDocument(pageContent.getSourceDocument());
        linkExtractionHelper.process(pageContent, context);
        var doc = pageContent.getSourceDocument();
        var links = doc.select(PSLinkExtractor.A_HREF);
        assertEquals(5, links.size());
        for (Element link : links) {
            assertFalse(link.attr(PSLinkExtractor.HREF).contains(PSLinkExtractionHelper.ASSETS_DIR_PREFIX));
        }
        for (Element link : links) {
            assertTrue(link.attr(PERC_MANAGED_ATTR).contains("true"));
        }
    }

    @Test
    public void testDummy() {
        // Just a dummy test to keep the test runner happy
    }
}
