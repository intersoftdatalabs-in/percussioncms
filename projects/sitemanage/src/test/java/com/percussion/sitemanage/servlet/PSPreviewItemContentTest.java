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
package com.percussion.sitemanage.servlet;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.server.PSServer;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.api.*;

import java.net.URL;

/**
 * Integration tests for previewing item content via friendly URLs.
 * // REFACTORED: CP-JAVA11
 * @author Percussion (modernized by Sunny Sal)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PSPreviewItemContentTest {

    private PSSiteDataServletTestCaseFixture fixture;
    private String pageFinderPath = "";
    private String assetFinderPath = "";
    private static final String ASSET_FOLDER = "//Folders/$System$/Assets/uploads";
    private static HttpClient conn;

    private IPSPageService pageService;
    private IPSIdMapper idMapper;
    private IPSSystemWs systemWs;
    private IPSPageDao pageDao;
    private IPSWidgetAssetRelationshipService widgetService;
    private IPSContentWs contentWs;
    private IPSAssetService assetService;
    private IPSItemService itemService;

    @BeforeEach All
    public void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        fixture = new PSSiteDataServletTestCaseFixture(request, response);
        fixture.setUp();

        var url = new URL(getUrlRoot());
        int port = url.getPort();
        String host = url.getHost();

        conn = new HttpClient();
        conn.getState().setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials("admin1", "demo"));
    }

    private String getUrlRoot() {
        return "http://" + PSServer.getHostName() + ":" + PSServer.getListenerPort();
    }

    @AfterAll
    public void tearDown() throws Exception {
        fixture.tearDown();
    }

    @Test
    @Order(1)
    public void testPageFriendlyUrl() throws Exception {
        var name = "Page1";
        var title = "Page1";
        var folderPath = fixture.site1.getFolderPath();
        var linkTitle = "TestLink";
        var url = "testurl.file";
        var pageId = createPage(name, title, fixture.template1.getId(), folderPath, linkTitle, url, "true", "This is Page 1.");

        assertNotNull(pageId);

        pageFinderPath = fixture.site1.getFolderPath().substring(1) + "/Page1";
        var path = getUrlRoot() + pageFinderPath;

        System.out.println("pageFinderPath " + pageFinderPath);
        var req = new GetMethod(path);
        try {
            int status = conn.executeMethod(req);
            assertEquals(200, status, "Request to get page " + req.getPath() + " should succeed");
            var src = req.getResponseBodyAsString();
            System.out.println(status + "\n" + src);
        } finally {
            req.releaseConnection();
        }
    }

    @Test
    @Order(2)
    public void testUpperCasePageFriendlyUrl() throws Exception {
        var name = "pageStory399";
        var title = "Page1";
        var folderPath = fixture.site1.getFolderPath();
        var linkTitle = "TestLink";
        var url = "testurl.file";
        var pageId = createPage(name, title, fixture.template1.getId(), folderPath, linkTitle, url, "true", "This is Page 1.");

        assertNotNull(pageId);

        fixture.assetCleaner.remove(pageId);

        pageFinderPath = fixture.site1.getFolderPath().substring(1) + "/Pagestory399";
        var path = getUrlRoot() + pageFinderPath;
        System.out.println("pageFinderPath " + pageFinderPath);

        var req = new GetMethod(path);
        try {
            int status = conn.executeMethod(req);
            assertEquals(200, status, "Request to get asset " + req.getPath() + " should succeed");
            var src = req.getResponseBodyAsString();
            System.out.println(status + "\n" + src);
        } finally {
            req.releaseConnection();
        }
    }

    @Test
    @Order(3)
    public void testNotFoundFriendlyUrl() throws Exception {
        pageFinderPath = fixture.site1.getFolderPath() + "/PageTest";
        var path = getUrlRoot() + pageFinderPath;
        System.out.println("pageFinderPath " + pageFinderPath);

        var req = new GetMethod(path);
        try {
            int status = conn.executeMethod(req);
            assertEquals(404, status, "Request to get page " + req.getPath() + " should fail with 404");
            var src = req.getResponseBodyAsString();
            System.out.println(status + "\n" + src);
        } finally {
            req.releaseConnection();
        }
    }

    @Test
    @Order(4)
    public void testAssetFriendlyUrl() throws Exception {
        var assetCreated = new PSAsset();
        var assetTitle = "testAssetHtmlSearch" + System.currentTimeMillis();
        assetCreated.getFields().put("sys_title", assetTitle);
        assetCreated.setType("percRawHtmlAsset");
        assetCreated.getFields().put("html", "TestHTML");
        assetCreated.setFolderPaths(asList(ASSET_FOLDER));

        var localAssetId = fixture.saveAsset(assetCreated).getId();

        fixture.assetCleaner.remove(localAssetId);

        assertNotNull(localAssetId);

        assetFinderPath = "/Assets/uploads/" + assetTitle;
        var path = getUrlRoot() + assetFinderPath;
        System.out.println("assetFinderPath " + assetFinderPath);

        var req = new GetMethod(path);
        try {
            int status = conn.executeMethod(req);
            assertEquals(200, status, "Request to get asset " + req.getPath() + " should succeed");
            var src = req.getResponseBodyAsString();
            System.out.println(status + "\n" + src);
        } finally {
            req.releaseConnection();
        }
    }

    /**
     * Creates and saves a page using the testcase fixture.
     */
    private String createPage(String name, String title, String templateId, String folderPath, String linkTitle,
                              String url, String noindex, String description) throws PSDataServiceException {
        var page = new PSPage();
        page.setFolderPath(folderPath);
        page.setName(name);
        page.setTitle(title);
        page.setTemplateId(templateId);
        page.setLinkTitle(linkTitle);
        page.setNoindex(noindex);
        page.setDescription(description);

        return fixture.createPage(page).getId();
    }

    // Dependency injection setters
    public IPSPageService getPageService() {
        return pageService;
    }

    public void setPageService(IPSPageService pageService) {
        this.pageService = pageService;
    }

    public IPSIdMapper getIdMapper() {
        return idMapper;
    }

    public void setIdMapper(IPSIdMapper idMapper) {
        this.idMapper = idMapper;
    }

    public IPSSystemWs getSystemWs() {
        return systemWs;
    }

    public void setSystemWs(IPSSystemWs systemWs) {
        this.systemWs = systemWs;
    }

    public IPSPageDao getPageDao() {
        return pageDao;
    }

    public void setPageDao(IPSPageDao pageDao) {
        this.pageDao = pageDao;
    }

    public IPSWidgetAssetRelationshipService getWidgetService() {
        return widgetService;
    }

    public void setWidgetService(IPSWidgetAssetRelationshipService widgetService) {
        this.widgetService = widgetService;
    }

    public IPSAssetService getAssetService() {
        return assetService;
    }

    public void setAssetService(IPSAssetService assetService) {
        this.assetService = assetService;
    }

    public IPSContentWs getContentWs() {
        return contentWs;
    }

    public void setContentWs(IPSContentWs contentWs) {
        this.contentWs = contentWs;
    }

    public IPSItemService getItemService() {
        return itemService;
    }

    public void setItemService(IPSItemService itemService) {
        this.itemService = itemService;
    }
}
