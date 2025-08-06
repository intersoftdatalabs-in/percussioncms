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

package com.percussion.pagemanagement.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria.SkipItemsType;
import com.percussion.pathmanagement.web.service.PSPathServiceRestClient;
import com.percussion.share.test.PSRestClient.RestClientException;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.share.test.PSTestDataCleaner;
import com.percussion.theme.data.PSRegionCSS;
import com.percussion.theme.data.PSRegionCSS.Property;
import com.percussion.theme.data.PSRegionCssList;
import com.percussion.theme.data.PSTheme;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Test theme service through REST.
 * <p>
 * Sunny Sal says: "A good theme test is like a good outfit—everything matches, and nothing breaks!"
 */
@Tag(IntegrationTest.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSThemeServiceTest extends PSRestTestCase<PSThemeRestClient> {

    private static PSTestSiteData testSiteData;

    @BeforeEach All
    public static void setUpFixture() throws Exception {
        testSiteData = new PSTestSiteData();
        testSiteData.setUp();
    }

    @BeforeEach
    public void setUp() {
        restClient.prepareForEditRegionCSS(PERC_THEME, TEMPLATE);
    }

    @AfterEach
    public void tearDown() {
        restClient.clearCacheRegionCSS(PERC_THEME, TEMPLATE);
    }

    @AfterAll
    public static void cleanup() throws Exception {
        testSiteData.tearDown();
    }

    @Override
    protected PSThemeRestClient getRestClient(String baseUrl) {
        return new PSThemeRestClient();
    }

    protected PSTemplateServiceClient getTemplateRestClient(String baseUrl) {
        return new PSTemplateServiceClient(baseUrl);
    }

    @Test
    public void testFindAll() {
        var themes = restClient.findAll();
        assertNotNull(themes);
        assertFalse(themes.isEmpty());

        var findPercussion = themes.stream().anyMatch(sum -> sum.getName().equals(PERC_THEME));
        assertTrue(findPercussion, "Must have a \"" + PERC_THEME + "\" theme");
    }

    @Test
    public void testLoadCSS() {
        var themes = restClient.findAll();
        for (var theme : themes) {
            var name = theme.getName();
            var themeCSS = restClient.loadCSS(name);
            assertNotNull(themeCSS);
            var css = themeCSS.getCSS();
            assertNotNull(css);

            if (name.equalsIgnoreCase(PERC_THEME)) {
                assertTrue(css.toLowerCase().contains(PERC_CSS_CONTENT.toLowerCase()));
            }
        }
    }

    @Test
    public void testCreateDelete() {
        var newSum = restClient.create("mynewtheme", PERC_THEME);
        assertNotNull(newSum);

        restClient.delete(newSum.getName());

        Exception thrown = assertThrows(RestClientException.class, () -> restClient.loadCSS(newSum.getName()));
        assertEquals(500, ((RestClientException) thrown).getStatus());
    }

    @Test
    public void testRegionCSS_CRUD() {
        // test with prepareForEditRegionCSS
        validateRegionCSS_CRUD();

        // test without prepareForEditRegionCSS
        restClient.clearCacheRegionCSS(PERC_THEME, TEMPLATE);
        validateRegionCSS_CRUD();
    }

    private void validateRegionCSS_CRUD() {
        restClient.deleteRegionCSS(PERC_THEME, TEMPLATE, "container", "header");
        validateEmptyRegionCSS("container", "header");

        var css = new PSRegionCSS("container", "header");
        var props = new ArrayList<Property>();
        var prop = new Property("border", "12px");
        props.add(prop);
        css.setProperties(props);

        restClient.saveRegionCSS(PERC_THEME, TEMPLATE, css);

        css = restClient.getRegionCSS(PERC_THEME, TEMPLATE, "container", "header");
        assertNotNull(css);

        restClient.deleteRegionCSS(PERC_THEME, TEMPLATE, "container", "header");
        validateEmptyRegionCSS("container", "header");
    }

    private void validateEmptyRegionCSS(String outer, String region) {
        var regionCSS = restClient.getRegionCSS(PERC_THEME, TEMPLATE, outer, region);
        assertNull(regionCSS.getOuterRegionName());
        assertNull(regionCSS.getRegionName());
        assertTrue(regionCSS.getProperties().isEmpty());
    }

    @Test
    public void testMergeRegionCSS() {
        var regions = new PSRegionCssList();
        var css = new PSRegionCSS("container", "header");
        regions.getRegions().add(css);

        var templateId = testSiteData.template1.getId();
        // The merge feature has been extensively tested by its specific component test.
        restClient.mergeRegionCSS(PERC_THEME, templateId, regions);
    }

    @Test
    public void testPrepareForEditRegionCSS() {
        restClient.prepareForEditRegionCSS(PERC_THEME, TEMPLATE);
    }

    @Test
    public void testClearCacheRegionCSS() {
        restClient.clearCacheRegionCSS(PERC_THEME, TEMPLATE);
    }

    private static final String PERC_THEME = "percussion";
    private static final String TEMPLATE = "home";
    private static final String PERC_CSS_CONTENT = "container";
}
