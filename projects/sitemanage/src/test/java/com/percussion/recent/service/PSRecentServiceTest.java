// REFACTORED: CP-JAVA11
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

package com.percussion.recent.service;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetContentType;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link IPSRecentService}.
 * Sunny Sal: "Recent service, recent Java 11, recent awesomeness!"
 */
@Category(IntegrationTest.class)
class PSRecentServiceTest extends PSServletTestCase {

    private static final int MAX_TEST_ADD = 22;
    private static final String PAGE_PREFIX = "RECENT_PAGE_";
    private static final String ASSET_PREFIX = "RECENT_ASSET_";
    private static final String TEMPLATE_PREFIX = "RECENT_TEMPLATE_";
    private static final String PAGE_FOLDER_PREFIX = "RECENT_PAGE_FOLDER_";
    private static final String ASSET_FOLDER_PREFIX = "RECENT_ASSET_FOLDER_";
    private static final String ASSET_FOLDER = "//Folders/$System$/Assets";

    private IPSRecentService recentService;
    private PSRecentServiceFixture fixture;

    private final List<PSPage> pageList = new ArrayList<>();
    private final List<PSAsset> assetList = new ArrayList<>();
    private List<PSWidgetContentType> widgetTypes = new ArrayList<>();
    private final List<PSTemplateSummary> templateList = new ArrayList<>();
    private final List<String> assetsFolders = new ArrayList<>();
    private final List<String> siteFolders = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        fixture = new PSRecentServiceFixture(request, response);
        fixture.setUp("Admin", "demo", "Default");

        for (int i = 0; i < MAX_TEST_ADD; i++) {
            pageList.add(fixture.createPage(PAGE_PREFIX + i));
        }
        for (int i = 0; i < MAX_TEST_ADD; i++) {
            siteFolders.add(fixture.createSiteFolder(PAGE_FOLDER_PREFIX + i));
        }
        for (int i = 0; i < MAX_TEST_ADD; i++) {
            assetsFolders.add(fixture.createAssetFolder(ASSET_FOLDER_PREFIX + i));
        }
        for (int i = 0; i < MAX_TEST_ADD; i++) {
            var assetCreated = new PSAsset();
            var assetTitle = ASSET_PREFIX + i;
            assetCreated.getFields().put("sys_title", assetTitle);
            assetCreated.setType("percRawHtmlAsset");
            assetCreated.getFields().put("html", "TestHTML");
            assetCreated.setFolderPaths(Collections.singletonList(ASSET_FOLDER + "/" + fixture.site1.getName()));
            var localAssetId = fixture.saveAsset(assetCreated).getId();
            assetCreated.setId(localAssetId);
            assetList.add(assetCreated);
        }
        for (int i = 0; i < MAX_TEST_ADD; i++) {
            templateList.add(fixture.createTemplate(TEMPLATE_PREFIX + i));
        }
        widgetTypes = fixture.getWidgetTypes();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        fixture.tearDown();
    }

    @Test
    void testUpdateRecentPages() {
        for (var page : pageList) {
            recentService.addRecentItem(page.getId());
        }
        var recentList = recentService.findRecentItem(false);
        assertEquals(20, recentList.size());
    }

    @Test
    void testUpdateRecentAssets() {
        for (var asset : assetList) {
            recentService.addRecentItem(asset.getId());
        }
        var recentList = recentService.findRecentItem(false);
        assertEquals(20, recentList.size());
    }

    @Test
    void testUpdateRecentTemplates() {
        for (var template : templateList) {
            recentService.addRecentTemplate(fixture.site1.getName(), template.getId());
        }
        var recentList = recentService.findRecentTemplate(fixture.site1.getName());
        assertEquals(6, recentList.size());
    }

    @Test
    void testUpdateRecentAssetTypes() throws PSDataServiceException {
        for (var type : widgetTypes) {
            recentService.addRecentAssetType(type.getWidgetId());
        }
        var recentList = recentService.findRecentAssetType();
        assertEquals(6, recentList.size());
    }

    @Test
    void testUpdateRecentSiteFolder() {
        for (var folderName : siteFolders) {
            recentService.addRecentSiteFolder(folderName);
        }
        var recentList = recentService.findRecentSiteFolder(fixture.site1.getName());
        assertEquals(10, recentList.size());
    }

    @Test
    void testUpdateRecentAssetFolder() {
        for (var folderName : assetsFolders) {
            recentService.addRecentAssetFolder(folderName);
        }
        var recentList = recentService.findRecentAssetFolder();
        assertEquals(10, recentList.size());
    }

    public IPSRecentService getRecentService() {
        return recentService;
    }

    public void setRecentService(IPSRecentService recentService) {
        this.recentService = recentService;
    }
}
