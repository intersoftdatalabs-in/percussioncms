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

import com.percussion.assetmanagement.data.PSAbstractAssetRequest;
import com.percussion.assetmanagement.data.PSAbstractAssetRequest.AssetType;
import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSBinaryAssetRequest;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.PSContentChangeServiceLocator;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.share.test.PSTestDataCleaner;
import com.percussion.ui.service.IPSListViewHelper;

import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for incremental publishing.
 * // REFACTORED: CP-JAVA11
 * @author JaySeletz (modernized by Sunny Sal)
 */
@Tag("IntegrationTest")
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
public class PSIncrementalPublishTest {

    private PSSiteDataServletTestCaseFixture fixture;
    private IPSSecurityWs securityWs;
    private IPSPageService pageService;
    private IPSAssetService assetService;
    private IPSItemWorkflowService itemWorkflowService;
    private IPSSitePublishService sitePublishService;
    private IPSContentWs contentWs;

    protected PSTestDataCleaner<String> pageCleaner = new PSTestDataCleaner<>() {
        @Override
        protected void clean(String id) throws Exception {
            pageService.delete(id, true);
        }
    };

    protected PSTestDataCleaner<String> assetCleaner = new PSTestDataCleaner<>() {
        @Override
        protected void clean(String id) throws Exception {
            assetService.delete(id);
        }
    };

    @BeforeEach
    public void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        fixture = new PSSiteDataServletTestCaseFixture(request, response);
        fixture.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        securityWs.login("admin1", "demo", "Enterprise_Investments", null);
        pageCleaner.clean();
        assetCleaner.clean();

        fixture.tearDown();

        var folders = contentWs.loadFolders(new String[]{PSAssetPathItemService.ASSET_ROOT + "/Test"});
        if (!folders.isEmpty()) {
            contentWs.deleteFolders(Collections.singletonList(folders.get(0).getGuid()), true);
        }
    }

    @Test
    public void testIncrementalPublish() throws Exception {
        securityWs.login("admin1", "demo", "Enterprise_Investments", null);

        var changeService = PSContentChangeServiceLocator.getContentChangeService();

        // Clear the changes from the service (ensure clean start)
        changeService.deleteChangeEventsForSite(fixture.site1.getSiteId());

        // Create and approve some pages
        var pages = new HashMap<String, PSPage>();
        for (int i = 0; i < 5; i++) {
            var page = createAndApprovePage("page" + i);
            pages.put(page.getId(), page);
        }

        // Create and approve some assets
        var assets = new HashMap<String, PSAsset>();
        for (int i = 0; i < 5; i++) {
            var asset = createAndApproveAsset("asset" + i);
            assets.put(asset.getId(), asset);
        }

        // Get preview, ensure we get them all with correct data
        var pagedItems = sitePublishService.getQueuedIncrementalContent(fixture.site1.getName(), fixture.site1.getName(), 1, 5);
        assertNotNull(pagedItems);
        assertEquals(10, pagedItems.getChildrenCount().intValue());
        var pathItems = pagedItems.getChildrenInPage();
        assertEquals(5, pathItems.size());
        validatePathItems(pages, assets, pathItems);

        pagedItems = sitePublishService.getQueuedIncrementalContent(fixture.site1.getName(), fixture.site1.getName(), 6, 6);
        assertNotNull(pagedItems);
        assertEquals(10, pagedItems.getChildrenCount().intValue());
        pathItems = pagedItems.getChildrenInPage();
        assertEquals(5, pathItems.size());
        validatePathItems(pages, assets, pathItems);

        assertTrue(pages.isEmpty());
        assertTrue(assets.isEmpty());

        // Clear the changes from the service
        changeService.deleteChangeEventsForSite(fixture.site1.getSiteId());
    }

    private void validatePathItems(Map<String, PSPage> pages, Map<String, PSAsset> assets, List<PSPathItem> pathItems) {
        for (var pathItem : pathItems) {
            var page = pages.remove(pathItem.getId());
            if (page != null) {
                validatePage(page, pathItem);
            } else {
                var asset = assets.remove(pathItem.getId());
                assertNotNull(asset, "Got unexpected path item");
                validateAsset(asset, pathItem);
            }
        }
    }

    private void validateAsset(PSAsset asset, PSPathItem pathItem) {
        assertEquals(asset.getName(), pathItem.getName());
        var props = pathItem.getDisplayProperties();

        var paths = asset.getFolderPaths();
        assertNotNull(paths);
        assertFalse(paths.isEmpty());
        assertEquals(PSPathUtils.getFinderPath(paths.get(0) + "/" + asset.getName()), pathItem.getPath());

        assertEquals(asset.getName(), props.get(IPSListViewHelper.TITLE_NAME));
        assertEquals("admin1", props.get(IPSListViewHelper.CONTENT_LAST_MODIFIER_NAME));
        assertTrue(StringUtils.isNotBlank(props.get(IPSListViewHelper.CONTENT_LAST_MODIFIED_DATE_NAME)));
        assertEquals(asset.getType(), pathItem.getType());
    }

    private void validatePage(PSPage page, PSPathItem pathItem) {
        assertEquals(page.getLinkTitle(), pathItem.getName());
        var props = pathItem.getDisplayProperties();

        var paths = page.getFolderPaths();
        assertNotNull(paths);
        assertFalse(paths.isEmpty());
        assertEquals(PSPathUtils.getFinderPath(paths.get(0) + "/" + page.getName()), pathItem.getPath());

        assertEquals(page.getName(), props.get(IPSListViewHelper.TITLE_NAME));
        assertEquals(page.getType(), pathItem.getType());

        assertEquals("admin1", props.get(IPSListViewHelper.CONTENT_LAST_MODIFIER_NAME));
        assertTrue(StringUtils.isNotBlank(props.get(IPSListViewHelper.CONTENT_LAST_MODIFIED_DATE_NAME)));
    }

    private PSAsset createAndApproveAsset(String name) throws PSDataServiceException, PSNotFoundException, IPSItemWorkflowService.PSItemWorkflowServiceException {
        try (InputStream in = IOUtils.toInputStream("This is my file content", StandardCharsets.UTF_8)) {
            var ar = new PSBinaryAssetRequest("/Assets/Test", AssetType.FILE, "test.txt", "text/plain", in);
            var resource = assetService.createAsset(ar);
            var resourceId = resource.getId();
            assetCleaner.add(resourceId);
            itemWorkflowService.performApproveTransition(resourceId, false, null);
            return resource;
        }
    }

    private PSPage createAndApprovePage(String name) throws PSDataServiceException, PSNotFoundException, IPSItemWorkflowService.PSItemWorkflowServiceException {
        var page = createPage(name, fixture.template1.getId());
        var pageId = pageService.save(page).getId();
        pageCleaner.add(pageId);
        itemWorkflowService.performApproveTransition(pageId, false, null);
        return page;
    }

    protected PSPage createPage(String name, String templateId) {
        var pageNew = new PSPage();
        pageNew.setName(name);
        pageNew.setTitle(name);
        pageNew.setFolderPath(fixture.site1.getFolderPath());
        pageNew.setTemplateId(templateId);
        pageNew.setLinkTitle(name + "Link");
        return pageNew;
    }

    // Dependency injection setters
    public void setPageService(IPSPageService pageService) {
        this.pageService = pageService;
    }

    public void setAssetService(IPSAssetService assetService) {
        this.assetService = assetService;
    }

    public void setSecurityWs(IPSSecurityWs securityWs) {
        this.securityWs = securityWs;
    }

    public void setItemWorkflowService(IPSItemWorkflowService itemWorkflowService) {
        this.itemWorkflowService = itemWorkflowService;
    }

    public void setSiteDataService(IPSSitePublishService sitePublishService) {
        this.sitePublishService = sitePublishService;
    }

    public void setContentWs(IPSContentWs contentWs) {
        this.contentWs = contentWs;
    }
}
