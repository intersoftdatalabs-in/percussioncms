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

import static com.percussion.pagemanagement.data.PSRegionTreeUtils.getChildRegions;
import static com.percussion.pagemanagement.parser.PSTemplateRegionParser.parse;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship.PSAssetResourceType;
import com.percussion.pagemanagement.data.PSAbstractRegion;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionBranches;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSRegionWidgetAssociations;
import com.percussion.pagemanagement.data.PSRenderResult;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.data.PSAbstractPersistantObject;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.share.test.PSTestUtils;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.types.PSPair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSRenderServiceTest {

    private static final String HTML_CONTENT = "TestHTML";
    private static final String HTML_CONTENT_2 = "TestHTML_2";
    private static final String CONTAINER_REGION_ID = "container";
    private static final String MY_WIDGET_REGION = "my-widget-region";

    static PSTestSiteData testSiteData;
    static PSRenderServiceClient renderClient;
    private static String pageId;
    private static PSTemplate template;
    private static PSTemplate template2;
    private static PSTemplate template3;
    private static PSPage page;
    private static PSPage page_2;
    private static PSAsset asset;
    private static PSAsset asset_2;

    @BeforeAll
    public void setUp() throws Exception {
        testSiteData = new PSTestSiteData();
        renderClient = new PSRenderServiceClient();
        PSRestTestCase.setupClient(renderClient);
        testSiteData.setUp();
        pageId = testSiteData.createPage("MyPage", testSiteData.site1.getFolderPath(), testSiteData.template1.getId());
        page = testSiteData.getPageRestClient().load(pageId);

        var templateId = testSiteData.template1.getId();

        template = setupTemplate(templateId);
        template2 = setupTemplate(testSiteData.template2.getId());
        template3 = testSiteData.getTemplateServiceClient().createTemplate("SiteTemplateServiceTest3", testSiteData.baseTemplateId);
        testSiteData.assignTemplatesToSite(testSiteData.site1.getId(), template3.getId());

        var pageId_2 = testSiteData.createPage("MyPage_2", testSiteData.site1.getFolderPath(), template2.getId());
        page_2 = testSiteData.getPageRestClient().load(pageId_2);

        asset = createHtmlSharedAsset("MyAsset", HTML_CONTENT);
        asset_2 = createHtmlSharedAsset("MyAsset_2", HTML_CONTENT_2);
    }

    private static PSAsset createHtmlSharedAsset(String title, String content) {
        var htmlAsset = new PSAsset();
        htmlAsset.getFields().put("sys_title", title);
        htmlAsset.setType("percRawHtmlAsset");
        htmlAsset.getFields().put("html", content);
        htmlAsset.setFolderPaths(asList("//Folders/$System$/Assets"));
        htmlAsset = testSiteData.saveAsset(htmlAsset);
        return htmlAsset;
    }

    public static PSTemplate setupTemplate(String templateId) {
        var template = testSiteData.getTemplateServiceClient().loadTemplate(templateId);
        template.setTheme("percussion");
        assertNotNull(template);
        var regionTree = template.getRegionTree();
        assertNotNull(regionTree);
        var wi = testSiteData.createWidgetItem("TESTME", PSTestSiteData.TEST_WIDGET_DEFINITION);
        regionTree.setRegionWidgets(MY_WIDGET_REGION, asList(wi));

        regionTree.setRootRegion(parse(null, getHtml("widget_template.html")).getRootNode());
        template = testSiteData.getTemplateServiceClient().save(template);

        return template;
    }

    @AfterAll
    public void tearDown() throws Exception {
        testSiteData.tearDown();
    }

    @Test
    public void testRenderPageRegion() throws Exception {
        var result = renderClient.renderRegion(page, CONTAINER_REGION_ID);
        assertRenderResult(result);
    }

    @Test
    public void testRenderPageRegionValidationFailure() {
        var badPage = new PSPage();
        assertThrows(Exception.class, () -> renderClient.renderRegion(badPage, "rid-1"));
    }

    @Test
    public void testRenderTemplateRegion() throws Exception {
        var result = renderClient.renderRegion(template, CONTAINER_REGION_ID);
        assertRenderResult(result);
    }

    @Test
    public void testRenderTemplateRegionValidationFailure() {
        var badTemplate = new PSTemplate();
        assertThrows(Exception.class, () -> renderClient.renderRegion(badTemplate, "rid-1"));
    }

    private void assertRenderResult(PSRenderResult result) {
        assertNotNull(result, "result should not be null");
        assertEquals(CONTAINER_REGION_ID, result.getRegionId());
        assertNotNull(result.getResult());
    }

    @Test
    public void testRenderPage() throws Exception {
        var actual = renderClient.renderPage(pageId);
        assertNotNull(actual);
        assertTrue(isNotBlank(actual));
        log.info("Rendering: " + actual);
    }

    @Test
    public void testRenderPageForEdit() throws Exception {
        var actual = renderClient.renderPageForEdit(pageId);
        assertNotNull(actual);
        assertTrue(isNotBlank(actual));
        log.info("Rendering: " + actual);
    }

    @Test
    public void testRenderTemplate() throws Exception {
        var templateId = testSiteData.template1.getId();
        var actual = renderClient.renderTemplate(templateId);
        assertNotNull(actual);
        assertTrue(isNotBlank(actual));
        log.info("Rendering: " + actual);
    }

    @Test
    public void testRenderWidgetItem() throws Exception {
        var id = template.getRegionTree().getRegionWidgetsMap().get(MY_WIDGET_REGION).get(0).getId();
        assertTrue(isNotBlank(id), "WidgetItem should have an Id.");

        assertNotNull(template);
        var actual = renderClient.renderTemplate(template.getId());

        assertTrue(isNotBlank(actual));
        assertTrue(contains(actual, "TESTME"), "Widget should render properly");
        log.info("Rendering: " + actual);
    }

    @Test
    public void testRenderPageWithWidgetItemButTemplateWillWidgetShow() throws Exception {
        assertNotNull(page.getRegionBranches());
        var branches = page.getRegionBranches();
        var wi = testSiteData.createWidgetItem("adam", PSTestSiteData.TEST_WIDGET_DEFINITION);
        var expected = new HashMap<>(wi.getProperties());
        branches.setRegionWidgets(MY_WIDGET_REGION, asList(wi));

        var newPage = testSiteData.getPageRestClient().save(page);
        wi = newPage.getRegionBranches().getRegionWidgetsMap().get(MY_WIDGET_REGION).get(0);
        assertEquals(expected, wi.getProperties(), "Expect properties to be saved and returned");
        assertTrue(isNotBlank(wi.getId()), "WidgetItem should have an Id.");

        var actual = renderClient.renderPage(newPage.getId());

        assertTrue(isNotBlank(actual));
        assertTrue(contains(actual, "TESTME"), "Widget should render properly");
        assertTrue(contains(actual, "perc-widget"), "Widget should have decoration");
        log.info("Rendering: " + actual);
    }

    @Test
    public void testRenderPageCannotOverrideTemplateWidgetItemAndAsset() throws Exception {
        log.info("testRenderPageWithWidgetItemAndAsset");

        var beforeChangePage = renderClient.renderPage(pageId);

        var page = testSiteData.getPageRestClient().load(pageId);

        assertNotNull(page.getRegionBranches());
        var branches = page.getRegionBranches();
        var wi = testSiteData.createWidgetItem("adam", "percRawHtml");
        branches.setRegionWidgets(MY_WIDGET_REGION, asList(wi));
        page.setRegionBranches(branches);

        var newPage = testSiteData.getPageRestClient().save(page);
        var widgetItemId = assertWidgetRegion(newPage, MY_WIDGET_REGION);

        var awRel = createAssetRelationship(newPage, widgetItemId, "adam", "percRawHtml", asset.getId());

        var afterChangePage = renderClient.renderPage(newPage.getId());

        log.info("Rendering: " + afterChangePage);

        assertEquals(beforeChangePage, afterChangePage);

        var renderResult = renderClient.renderRegion(newPage, MY_WIDGET_REGION);
        var renderRegion = renderResult.getResult();

        assertNotEquals(renderRegion, afterChangePage);

        testSiteData.getAssetRestClient().clearAssetWidgetRelationship(awRel);
    }

    @Test
    public void testRenderPageWithPageAsset() throws Exception {
        log.info("testRenderPageWithPageAsset");

        var pair = addHtmlWidget(template2, MY_WIDGET_REGION);
        template2 = pair.getFirst();

        var beforeChangePage = renderClient.renderPage(page_2.getId());

        var wi = pair.getSecond();
        var awRel = createAssetRelationship(page_2, wi.getId(), wi.getName(), "percRawHtml", asset_2.getId());

        var afterChangePage = renderClient.renderPage(page_2.getId());

        testSiteData.getAssetRestClient().clearAssetWidgetRelationship(awRel);

        assertFalse(beforeChangePage.contains(HTML_CONTENT_2));
        assertNotEquals(beforeChangePage, afterChangePage);
        assertTrue(afterChangePage.contains(HTML_CONTENT_2));
    }

    @Test
    public void testRenderTemplateWithWidgetItemAndAsset() throws Exception {
        log.info("testRenderTemplateWithWidgetItemAndAsset");

        template2 = addHtmlWidget(template2, MY_WIDGET_REGION).getFirst();

        var widgetItemId = assertWidgetRegion(template2.getRegionTree(), MY_WIDGET_REGION);

        var awRel = createAssetRelationship(template2, widgetItemId, "adam", "percRawHtml", asset.getId());

        var actual = renderClient.renderTemplate(template2.getId());

        log.info("Rendering: " + actual);

        assertAsset(actual);

        var renderResult = renderClient.renderRegion(template2, MY_WIDGET_REGION);
        actual = renderResult.getResult();

        assertAsset(actual);

        testSiteData.getAssetRestClient().clearAssetWidgetRelationship(awRel);
    }

    private PSPair<PSTemplate, PSWidgetItem> addHtmlWidget(PSTemplate template, String regionId) {
        assertNotNull(template.getRegionTree());
        var tree = template.getRegionTree();
        var wi = testSiteData.createWidgetItem("adam", "percRawHtml");
        tree.setRegionWidgets(regionId, asList(wi));
        template.setRegionTree(tree);

        template = testSiteData.getTemplateServiceClient().save(template);

        wi = assertWidgetItemFromRegion(template.getRegionTree(), regionId);

        return new PSPair<>(template, wi);
    }

    @Disabled("The default CSS is no longer valid")
    public void testRenderCss() throws Exception {
        var actual = renderClient.renderTemplate(template3.getId());
        assertTrue(contains(actual, "percussion/perc_theme.css"));
    }

    private void assertAsset(String actual) {
        assertTrue(isNotBlank(actual));
        assertTrue(contains(actual, HTML_CONTENT), "Widget should render content properly. Did not find 'TestHTML' marker, actual: " + actual);
        assertTrue(contains(actual, "perc-widget"), "Widget should have decoration");
    }

    private PSAssetWidgetRelationship createAssetRelationship(PSAbstractPersistantObject newPage,
            String widgetItemId, String widgetName, String widgetDefName, String assetId) {
        var awr = new PSAssetWidgetRelationship();
        awr.setAssetId(assetId);
        awr.setAssetOrder(0);
        awr.setWidgetId(Long.parseLong(widgetItemId));
        awr.setWidgetName(widgetDefName);
        awr.setWidgetInstanceName(widgetName);
        awr.setOwnerId(newPage.getId());
        awr.setResourceType(PSAssetResourceType.shared);
        var rid = testSiteData.getAssetRestClient().createAssetWidgetRelationship(awr);
        assertNotNull(rid, "rid");
        log.info("RID: " + rid);

        return awr;
    }

    @Disabled("This test is not really needed because templates override pages")
    @Test
    public void testRenderPageOverrides() throws Exception {
        log.info("testRenderPageOverrides");
        var pageId = testSiteData.createPage("MyPagePageOverrides", testSiteData.site1.getFolderPath(), testSiteData.template1.getId());
        var page = testSiteData.getPageRestClient().load(pageId);

        assertNotNull(page.getRegionBranches());
        var branches = page.getRegionBranches();

        var wi = testSiteData.createWidgetItem("adam", "percRawHtml");
        var html = getHtml("region_override.html");

        var parsed = renderClient.parse(html);
        parsed.setRegionId(MY_WIDGET_REGION);

        branches.setRegionWidgets("page-subregion", asList(wi));
        branches.setRegions(asList(parsed));
        page.setRegionBranches(branches);

        var newPage = testSiteData.getPageRestClient().save(page);
        var widgetItemId = assertWidgetRegion(newPage, "page-subregion");

        var awRel = createAssetRelationship(newPage, widgetItemId, "adam", "percRawHtml", asset.getId());

        var actual = renderClient.renderPage(newPage.getId());

        log.info("Rendering: " + actual);

        assertTrue(isNotBlank(actual));
        assertTrue(contains(actual, HTML_CONTENT), "Widget should render content properly");
        assertTrue(contains(actual, "page-subregion"), "Should render page subregion");
        assertTrue(contains(actual, "perc-widget"), "Widget should have decoration");

        testSiteData.getAssetRestClient().clearAssetWidgetRelationship(awRel);
    }

    private String assertWidgetRegion(PSPage newPage, String regionId) {
        var wr = newPage.getRegionBranches().getRegionWidgetsMap();
        log.debug("WidgetRegions: " + wr);
        assertNotNull(wr.get(regionId), "Should have widget region");
        var widgetItemId = wr.get(regionId).get(0).getId();
        assertEquals(1, wr.get(regionId).size());
        assertEquals("percRawHtml", wr.get(regionId).get(0).getDefinitionId());
        return widgetItemId;
    }

    private String assertWidgetRegion(PSRegionWidgetAssociations assocations, String regionId) {
        var wr = assocations.getRegionWidgetsMap();
        log.debug("WidgetRegions: " + wr);
        assertNotNull(wr.get(regionId), "Should have widget region");
        var widgetItemId = wr.get(regionId).get(0).getId();
        assertEquals(1, wr.get(regionId).size());
        assertEquals("percRawHtml", wr.get(regionId).get(0).getDefinitionId());
        return widgetItemId;
    }

    private PSWidgetItem assertWidgetItemFromRegion(PSRegionWidgetAssociations assocations, String regionId) {
        var wr = assocations.getRegionWidgetsMap();
        log.debug("WidgetRegions: " + wr);
        assertNotNull(wr.get(regionId), "Should have widget region");
        var wi = wr.get(regionId).get(0);
        assertEquals(1, wr.get(regionId).size());
        assertEquals("percRawHtml", wr.get(regionId).get(0).getDefinitionId());
        return wi;
    }

    @Test
    public void testParse() throws Exception {
        var region = renderClient.parse(getHtml("widget_template.html"));
        var children = getChildRegions(region);
        assertNotNull(children);
        assertEquals(CONTAINER_REGION_ID, children.get(0).getRegionId());
        assertEquals(MY_WIDGET_REGION, getChildRegions(children.get(0)).get(0).getRegionId());
    }

    @Test
    public void testErik() throws Exception {
        var erik = "<div class=\"perc-region\" id=\"container\">" +
                "<div class=\"perc-vertical\">" +
                "<div class=\"perc-region perc-region-leaf\" id=\"mainRegion\">" +
                "<div class=\"perc-vertical\"></div></div></div></div>";

        var region = renderClient.parse(erik);
        var children = getChildRegions(region);
        assertNotNull(children);
        assertEquals(CONTAINER_REGION_ID, children.get(0).getRegionId());
        assertEquals("mainRegion", getChildRegions(children.get(0)).get(0).getRegionId());
        log.debug(PSSerializerUtils.marshal(region));
    }

    private static String getHtml(String name) {
        return PSTestUtils.resourceToString(PSRenderServiceTest.class, name);
    }

    private static final Logger log = LogManager.getLogger(PSRenderServiceTest.class);
}
