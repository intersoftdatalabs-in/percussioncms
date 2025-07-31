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

package com.percussion.pagemanagement.web.service;

import static com.percussion.share.test.PSMatchers.containsRegEx;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.pagemanagement.data.PSInlineLinkRequest;
import com.percussion.pagemanagement.data.PSRenderLink;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.share.test.PSTestUtils;

import java.io.InputStream;

import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.*;

import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSRenderLinkServiceTest {

    private static PSRenderLinkServiceClient renderClient;
    private static PSTestSiteData testSiteData;
    private static String pageId;
    private static PSAsset asset;
    private static String file;

    @BeforeAll
    public void setUp() throws Exception {
        testSiteData = new PSTestSiteData();
        testSiteData.setUp();
        renderClient = new PSRenderLinkServiceClient();
        PSRestTestCase.setupClient(renderClient);
        pageId = testSiteData.createPage("TestLinkToPage", testSiteData.site1.getFolderPath(), testSiteData.template1.getId());
        file = PSTestUtils.resourceToBase64(PSRenderLinkServiceTest.class, "inline-link.txt");

        asset = new PSAsset();
        asset.getFields().put("sys_title", "MyFile");
        asset.getFields().put("displaytitle", "MyFile Displaytitle");
        asset.getFields().put("filename", "inline-link.txt");
        asset.getFields().put("item_file_attachment", file);
        asset.getFields().put("item_file_attachment_filename", "inline-link.txt");
        asset.getFields().put("item_file_attachment_type", "text/plain");
        asset.setType("percFileAsset");
        asset.setFolderPaths(asList(PSAssetPathItemService.ASSET_ROOT));
        asset = testSiteData.saveAsset(asset);
    }

    @AfterAll
    public void tearDown() throws Exception {
        testSiteData.tearDown();
    }

    @Test
    public void testPreviewPageLink() throws Exception {
        var actual = renderClient.getPreviewPageLink(pageId);
        assertNotNull(actual.getUrl());
        assertFalse(actual.getUrl().isEmpty());
        assertThat(actual.getUrl(), containsRegEx("sys_siteid=[0-9]+"));
        assertThat(actual.getUrl(), not(containsString("sys_siteid=0")));
    }

    @Test
    public void testPostFileLink() throws Exception {
        var link = new PSInlineLinkRequest();
        link.setTargetId(asset.getId());

        var actual = renderClient.previewPostLinkRequest(link);
        assertNotNull(actual.getUrl());
        assertFalse(actual.getUrl().isEmpty());
    }

    @Test
    public void testGetFileLinkForResource() throws Exception {
        var actual = renderClient.getPreviewLink(asset.getId(), "percSystem.fileBinary");
        assertNotNull(actual.getUrl());
        assertFalse(actual.getUrl().isEmpty());
    }

    @Test
    public void testFollowFileLink() throws Exception {
        var actual = renderClient.getPreviewLink(asset.getId(), "percSystem.fileBinary");
        try (InputStream content = renderClient.followLink(actual.getUrl())) {
            var linkContent = PSTestUtils.resourceToBase64(content);
            assertNotNull(linkContent);
            assertEquals("File uploaded should be the same as the render link followed", file, linkContent);
        }
    }

    @Disabled("Cannot create image asset with rest yet.")
    @Test
    public void testImagePreviewLink() throws Exception {
        var link = new PSInlineLinkRequest();
        link.setTargetId(asset.getId());

        var actual = renderClient.previewPostLinkRequest(link);
        assertNotNull(actual);
    }
}
