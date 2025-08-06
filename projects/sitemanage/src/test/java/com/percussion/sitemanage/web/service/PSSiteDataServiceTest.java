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
package com.percussion.sitemanage.web.service;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSAssetDropCriteria;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship.PSAssetResourceType;
import com.percussion.pagemanagement.data.PSAbstractRegion;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionNode.PSRegionOwnerType;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSRegionTreeUtils;
import com.percussion.pagemanagement.data.PSRegionWidgets;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pagemanagement.web.service.PSPageRestClient;
import com.percussion.pagemanagement.web.service.PSRenderServiceClient;
import com.percussion.pagemanagement.web.service.PSTemplateServiceClient;
import com.percussion.pagemanagement.web.service.PSTestSiteData;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria.SkipItemsType;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.pathmanagement.web.service.PSPathServiceRestClient;
import com.percussion.share.IPSSitemanageConstants;
import com.percussion.share.async.IPSAsyncJob;
import com.percussion.share.async.PSAsyncJobStatus;
import com.percussion.share.data.PSEnumVals;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.test.PSObjectRestClient;
import com.percussion.share.test.PSObjectRestClient.DataValidationRestClientException;
import com.percussion.share.test.PSRestClient.RestClientException;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.share.test.PSTestDataCleaner;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.share.validation.PSValidationErrors.PSFieldError;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteCopyRequest;
import com.percussion.sitemanage.data.PSSiteProperties;
import com.percussion.sitemanage.data.PSSitePublishProperties;
import com.percussion.sitemanage.data.PSSiteStatisticsSummary;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService.PublishType;
import com.percussion.utils.types.PSPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
/**
 * Integration tests for site data service REST API.
 * // REFACTORED: CP-JAVA11
 */
public class PSSiteDataServiceTest extends PSRestTestCase<PSSiteRestClient> {

    private static final String SITE_NAME_PREFIX = "TestMyTestSite";
    private static PSTestSiteData testSiteData;
    private static final Logger log = LogManager.getLogger(PSSiteDataServiceTest.class);

    PSTestDataCleaner<String> siteCleaner = new PSTestDataCleaner<>() {
        @Override
        protected void clean(String id) throws Exception {
            restClient.delete(id);
        }
    };

    PSTestDataCleaner<String> folderCleaner = new PSTestDataCleaner<>() {
        @Override
        protected void clean(String id) throws Exception {
            var criteria = new PSDeleteFolderCriteria();
            criteria.setPath(id);
            criteria.setSkipItems(SkipItemsType.NO);
            getPathServiceRestClient().deleteFolder(criteria);
        }
    };

    PSTestDataCleaner<String> pageCleaner = new PSTestDataCleaner<>() {
        @Override
        protected void clean(String id) throws Exception {
            getPageRestClient().delete(id);
        }
    };

    @BeforeAll
    public static void setUp() throws Exception {
        testSiteData = new PSTestSiteData();
        testSiteData.setUp();
    }

    @Override
    protected PSSiteRestClient getRestClient(String baseUrl) {
        restClient = new PSSiteRestClient(baseUrl);
        return restClient;
    }

    private PSSiteTemplateRestClient getSiteTemplateServiceRestClient() throws Exception {
        var client = new PSSiteTemplateRestClient();
        client.setUrl(baseUrl);
        setupClient(client);
        return client;
    }

    private PSRenderServiceClient getRenderServiceClient() throws Exception {
        var client = new PSRenderServiceClient();
        client.setUrl(baseUrl);
        setupClient(client);
        return client;
    }

    private PSPathServiceRestClient getPathServiceRestClient() throws Exception {
        var client = new PSPathServiceRestClient(baseUrl);
        setupClient(client);
        return client;
    }

    private PSPageRestClient getPageRestClient() throws Exception {
        var client = new PSPageRestClient(baseUrl);
        setupClient(client);
        return client;
    }

    private PSTemplateServiceClient getTemplateServiceClient() throws Exception {
        var client = new PSTemplateServiceClient(baseUrl);
        setupClient(client);
        return client;
    }

    @AfterEach
    public void tearDownTest() {
        restClient.login("Admin", "demo");
        pageCleaner.clean();
        folderCleaner.clean();
        siteCleaner.clean();
    }

    @AfterAll
    public static void tearDown() {
        try {
            testSiteData.tearDown();
        } catch (Exception e) {
            log.error("Failed to tear down test site data", e);
        }
    }

    @Test
    public void testCreateSite() {
        log.debug("testCreateSite");
        var site = createSite();
        restClient.login("Editor", "demo");
        assertThrows(RestClientException.class, () -> restClient.save(site));
        restClient.login("Admin", "demo");
        var actual = restClient.save(site);
        site.setFolderPath(actual.getFolderPath());
        site.setSiteId(actual.getSiteId());
        assertEquals(site, actual);
    }

    @Test
    public void testEditSite() {
        log.debug("testEditSite");
        var site = createAndSaveSite();
        var siteName = site.getName();

        var props = restClient.getProperties(site.getName());
        var props2 = restClient.updateProperties(props);
        assertEquals(props, props2);

        props = restClient.getProperties(site.getName());
        props.setHomePageLinkText("Percussion Site");
        props.setDescription("Percussion Site Description");
        props.setName(props.getName() + System.currentTimeMillis());

        props2 = restClient.updateProperties(props);
        assertEquals("Percussion Site", props2.getHomePageLinkText());
        assertEquals("Percussion Site Description", props2.getDescription());
        assertEquals(props.getName(), props2.getName());

        props.setName(siteName);
        props2 = restClient.updateProperties(props);
        assertEquals(siteName, props2.getName());
    }

    @Test
    public void testPublishEditSite() {
        log.debug("testPublishEditSite");
        var site = createAndSaveSite();
        var siteName = site.getName();

        var publishprops1 = restClient.getSitePublishProperties(siteName);
        publishprops1.setSiteName(siteName);
        publishprops1.setFtpServerName("ftpserver");
        publishprops1.setFtpUserName("ftpuser");
        publishprops1.setPublishType(PublishType.valueOf("ftp"));

        var publishprops2 = restClient.updateSitePublishProperties(publishprops1);
        assertEquals("ftpserver", publishprops2.getFtpServerName());
        assertEquals("ftpuser", publishprops2.getFtpUserName());
        assertEquals("ftp", publishprops2.getPublishType().toString());
    }

    public PSSite createAndSaveSite() {
        var site = createSite();
        return restClient.save(site);
    }

    @Test
    public void testImportSiteFromUrlAsync() {
        var site = createSite();
        site.setBaseUrl("http://samples.percussion.com/products/index.html");

        var jobId = restClient.importSiteFromUrlAsync(site);
        assertTrue(jobId > 0);

        PSAsyncJobStatus jobStatus;
        do {
            jobStatus = testSiteData.getAsyncJobStatusRestClient().getStatus(Long.toString(jobId));
            assertNotNull(jobStatus);
        } while (!jobStatus.getStatus().equals(IPSAsyncJob.COMPLETE_STATUS)
                && !jobStatus.getStatus().equals(IPSAsyncJob.ABORT_STATUS));

        assertEquals(IPSAsyncJob.COMPLETE_STATUS, jobStatus.getStatus());

        var importedSite = restClient.getImportedSite(jobId);
        assertNotNull(importedSite);
        assertEquals(site.getName(), importedSite.getName());
    }

    @Test
    public void testGetSite() {
        log.debug("testGetSite");
        var newSite = createAndSaveSite();
        var site = restClient.get(newSite.getName());
        assertEquals(newSite.getName(), site.getName());
    }

    @Test
    public void testGetChoices() {
        log.debug("testGetChoices");
        var newSite = createAndSaveSite();
        var newSite2 = createAndSaveSite();

        var sums = restClient.findAll();
        var choices = restClient.getChoices();
        var entries = choices.getEntries();
        assertEquals(sums.size(), entries.size());
        var entryValues = entries.stream().map(PSEnumVals.EnumVal::getValue).toList();
        assertTrue(entryValues.contains(newSite.getName()));
        assertTrue(entryValues.contains(newSite2.getName()));
    }

    @Test
    public void testGetSiteNotFound() {
        log.debug("testGetSiteNotFound");
        assertSiteNotExist("DO_NOT_FIND_ME");
    }

    @Test
    public void testDeleteSite() {
        log.debug("deleteSite");
        var site = createAndSaveSite();
        restClient.login("Editor", "demo");
        assertThrows(RestClientException.class, () -> restClient.delete(site.getName()));
        restClient.login("Admin", "demo");
        restClient.delete(site.getName());
        assertSiteNotExist(site.getName());
    }

    @Test
    public void testFailToSaveInvalidSite() {
        log.debug("failToSaveInvalidSite");
        var s = new PSSite();
        s.setName("");
        var expected = new HashSet<>(asList("name", "homePageTitle", "navigationTitle", "templateName", "templateId", "label"));
        try {
            restClient.save(s);
            fail("Should have thrown DataValidationRestClientException");
        } catch (DataValidationRestClientException e) {
            var errors = e.getErrors();
            var actual = new HashSet<String>();
            for (PSFieldError fe : errors.getFieldErrors()) actual.add(fe.getField());
            assertEquals(expected, actual);
        } catch (PSObjectRestClient.DataRestClientException e) {
            var errorMsg = e.getMessage();
            assertFalse(errorMsg.contains("sitename may not be null or empty"));
        }
    }

    @Test
    public void testCreateSiteWithErikJson() {
        var siteJson = "{\"Site\":{\"name\":\"TestA\",\"label\":\"TestA\",\"description\":\"TestA\"," +
                "\"homePageTitle\":\"Testpage1\",\"navigationTitle\":\"Testpage1\",\"templateId\":\"percSampleTemplate1\"}}";
        restClient.getRequestHeaders().put("Accept", "application/json");
        assertThrows(RestClientException.class, () ->
                restClient.POST("/Rhythmyx/services/sitemanage/site/", siteJson, "application/json"));
    }

    @Test
    @Ignore
    public void testCopy() throws Exception
    {
        log.debug("testCopy");

        PSSite site = createAndSaveSite();
        String siteId = site.getId();
        String siteName = site.getName();
        
        String copyName = "CopyOf" + siteName;
        PSSiteCopyRequest req = new PSSiteCopyRequest();
        req.setSrcSite(siteId);
        req.setCopySite(copyName);
        PSSite copy = restClient.copy(req);
        assertNotNull(copy);
        String copyId = copy.getId();
        siteCleaner.add(copyId);
        
        // check the name
        assertEquals(copy.getName(), copyName);
        
        // check the templates
        PSSiteTemplateRestClient tempClient = getSiteTemplateServiceRestClient();
        List<PSTemplateSummary> siteTemplates = tempClient.findTemplatesBySite(siteId);
        List<PSTemplateSummary> copyTemplates = tempClient.findTemplatesBySite(copyId);
        assertEquals(siteTemplates.size(), copyTemplates.size());
        
        PSTemplateSummary siteTemp = siteTemplates.get(0);
        String siteTempId = siteTemp.getId();
        PSTemplateSummary copyTemp = copyTemplates.get(0);
        assertFalse(copyTemp.getId().equals(siteTempId));
        
        // reset the id so we can compare the templates
        copyTemp.setId(siteTempId);
        copyTemp.setImageThumbPath("Thumbnail");
        siteTemp.setImageThumbPath(copyTemp.getImageThumbPath());
        assertEquals(siteTemp, copyTemp);
    
        // check the publishing configuration
        PSSitePublishProperties sitePubProps = restClient.getSitePublishProperties(siteName);
        String sitePubPropsId = sitePubProps.getId();
        PSSitePublishProperties copyPubProps = restClient.getSitePublishProperties(copyName);
        assertFalse(copyPubProps.getId().equals(sitePubPropsId));
        assertEquals(copyName, copyPubProps.getSiteName());
        
        // reset the id and name so we can compare the configurations
        copyPubProps.setId(sitePubPropsId);
        copyPubProps.setSiteName(siteName);
        assertEquals(sitePubProps, copyPubProps);
        
        // should not be able to copy to an existing site
        try
        {
            restClient.copy(req);
            fail("Should not be able to copy to an existing site");
        }
        catch (RestClientException e)
        {
            assertTrue(e.getResponseBody().contains("site.exists"));
        }
        
        // switch to non-Admin user
        restClient.login("Editor", "demo");
        // should not be able to copy the site
        try
        {
            req.setCopySite("SiteCreatedByEditor");
            restClient.copy(req);
            fail("Non-Admin user should not be able to copy a site");
        }
        catch (RestClientException e)
        {
            assertTrue(e.getResponseBody().contains("site.saveNotAuthorized"));
        }
             

        //Create a new region
        String regionHtml = "<div>MY-CODE</div>"
                          + "<div class=\"perc-region\" id=\"leftsidebar\">"
                          + "#region('leftsidebar' '<div>' '<span>' '</span>' '</div>')"
                          + "</div>";
        PSRegion region = getRenderServiceClient().parse(regionHtml);
        
        //Get the child region
        PSAbstractRegion childRegion = PSRegionTreeUtils.getChildRegions(region).get(0);
        childRegion.setOwnerType(PSRegionOwnerType.PAGE);
        site.getRegionBranches().setRegions(asList((PSRegion) childRegion));

        //Create a new widget
        PSWidgetItem wi = new PSWidgetItem();
        wi.setDefinitionId("percPageAutoList");
        wi.setName("widget5");
        
        //Set the region widgets
        site.getRegionBranches().setRegionWidgets(childRegion.getRegionId(), asList(wi));

        String pageId = testSiteData.getPageRestClient().save(site).getId();
        //Reload the page after save
        site = testSiteData.getPageRestClient().load(pageId);

        Set<PSRegionWidgets> regWidgs = site.getRegionBranches().getRegionWidgetAssociations();
        String widgetId = null;
        for (PSRegionWidgets psRegionWidgets : regWidgs)
        {
            widgetId = psRegionWidgets.getWidgetItems().get(0).getId();
        }
                        
        //create a page autolist
        PSAsset asset = new PSAsset();
        asset.getFields().put("sys_title", "autolist");
        asset.getFields().put("displaytitle", "Test Autolist");
        asset.getFields().put("query", testSiteData.site1.getFolderPath() + "%" +"samplequery");
        asset.getFields().put("title contains", "testPage");
        asset.getFields().put("site_path", testSiteData.site1.getFolderPath());
        asset.setType("percPageAutoList");
        asset = testSiteData.saveAsset(asset);
        String origAssetId = asset.getId();
        testSiteData.getAssetCleaner().remove(origAssetId);
        
        PSAssetWidgetRelationship awRel = new PSAssetWidgetRelationship(pageId, Long.parseLong(widgetId),
                "percPageAutoList", origAssetId, 1, wi.getName());
        String awRelId = testSiteData.getAssetRestClient().createAssetWidgetRelationship(awRel);
        assertNotNull(awRelId); 
        restClient.login("Admin", "demo");
        
        List<PSAssetDropCriteria> orgDropCriteria = testSiteData.getAssetRestClient().
                                                    getWidgetAssetCriteria(pageId,true);
        String origWidgetName = orgDropCriteria.get(0).getWidgetName();
        String origWidgetId = orgDropCriteria.get(0).getWidgetId();
        
        req.setSrcSite(testSiteData.site1.getId());
        req.setCopySite("SiteWithCopy");
        PSSite siteAfterCopy = restClient.copy(req); 
        
        siteCleaner.add(siteAfterCopy.getId());
        PSPage pageAfterCopy = testSiteData.getPageRestClient().findPageByFullFolderPath(siteAfterCopy.getFolderPath() +
                                                                                       "/" + "testPage" );
        // the author should be the logged in user
        // http://bugs.percussion.local/browse/CML-4895
        String pageAfterCopyAuthor = getPageAfterCopyAuthor(siteAfterCopy, pageAfterCopy);
        assertEquals(pageAfterCopyAuthor, "Admin");
        
        List<PSAssetDropCriteria> copyDropCriteria = testSiteData.getAssetRestClient().
                                                     getWidgetAssetCriteria(pageAfterCopy.getId(),true);
        
        String copyWidgetName = copyDropCriteria.get(0).getWidgetName();
        String copyWidgetId = copyDropCriteria.get(0).getWidgetId();
        
        assertEquals(origWidgetName,copyWidgetName);
        assertEquals(origWidgetId, copyWidgetId);
        
        assertListAsset(page.getId(), pageAfterCopy.getId());
    }
    
    /**
     * Gets the author of the page that is passed as the second parameter. The site is used to
     * get its children. The page must be a child of the site, otherwise it will return <code>null</code>
     * 
     * @param siteAfterCopy the site in which we are going to seek for the page
     * @param pageAfterCopy the page that we are looking the author for
     * @return the author of the page or <code>null</code> if we do not find it
     */
    private String getPageAfterCopyAuthor(PSSite siteAfterCopy, PSPage pageAfterCopy)
    {
        String siteAfterCopyPath = siteAfterCopy.getFolderPath().substring(1);
        
        int startIndex = 1;         // begin with the first element.
        int maxResults = 9;         // set 100 as max (should be less elements)
        int displayFormatId = 9;    // the same as the list view in the finder
        String child = null;        // setting the child to null means it won't be used
        
        PSPagedItemList siteChildren = testSiteData.getPathRestClient().findChildren(siteAfterCopyPath, 
                   startIndex, maxResults, child, displayFormatId);
        
        // find the test page
        String author = null;
        for(PSPathItem pathItem : siteChildren.getChildrenInPage())
        {
            if(pathItem.getId().equals(pageAfterCopy.getId()))
            {
                author = pathItem.getDisplayProperties().get("sys_contentcreatedby");
            }
        }
        
        return author;
    }

    @Test
    public void testCopyWithAssets() throws Exception
    {
        log.debug("testCopyWithAssets");
        PSSite site = testSiteData.site1;
        String siteId = site.getId();
        String siteName = site.getName();
                
        //Create a folder in the Assets Library
        String testFolderPath = PSPathUtils.ASSETS_FINDER_ROOT + "/Test";
        getPathServiceRestClient().addFolder(testFolderPath);
        folderCleaner.add(testFolderPath);
        
        //Create a page autolist under the folder
        PSAsset asset = new PSAsset();
        asset.getFields().put("sys_title", "autolist");
        asset.getFields().put("displaytitle", "Test Autolist");
        asset.getFields().put("query", "select rx:sys_contentid, rx:sys_folderid from rx:percPage where jcr:path like '"
                + site.getFolderPath() + "%'");
        asset.getFields().put("title contains", "testPage");
        asset.getFields().put("site_path", site.getFolderPath());
        asset.setFolderPaths(asList(PSPathUtils.getFolderPath(testFolderPath)));
        asset.setType("percPageAutoList");
        asset = testSiteData.saveAsset(asset);
        String origAssetId = asset.getId();
        testSiteData.getAssetCleaner().remove(origAssetId);
        
        //Create a page
        String pageId = testSiteData.createPage("testPage", site.getFolderPath(), testSiteData.template1.getId());
        PSPage page = testSiteData.getPageRestClient().load(pageId);
                       
        //Create a new region
        String regionHtml = "<div>MY-CODE</div>"
                          + "<div class=\"perc-region\" id=\"leftsidebar\">"
                          + "#region('leftsidebar' '<div>' '<span>' '</span>' '</div>')"
                          + "</div>";
        PSRegion region = getRenderServiceClient().parse(regionHtml);
        
        //Get the child region
        PSAbstractRegion childRegion = PSRegionTreeUtils.getChildRegions(region).get(0);
        childRegion.setOwnerType(PSRegionOwnerType.PAGE);
        page.getRegionBranches().setRegions(asList((PSRegion) childRegion));
        
        //Create a new widget
        PSWidgetItem wi = new PSWidgetItem();
        wi.setDefinitionId("percPageAutoList");
        wi.setName("widget5");
        
        //Set the region widgets
        page.getRegionBranches().setRegionWidgets(childRegion.getRegionId(), asList(wi));
        
        pageId = testSiteData.getPageRestClient().save(page).getId();
        //Reload the page after save
        page = testSiteData.getPageRestClient().load(pageId);
        
        Set<PSRegionWidgets> regWidgs = page.getRegionBranches().getRegionWidgetAssociations();
        String widgetId = null;
        for (PSRegionWidgets psRegionWidgets : regWidgs)
        {
            widgetId = psRegionWidgets.getWidgetItems().get(0).getId();
        }
                
        //Add the page auto list to the page
        PSAssetWidgetRelationship awRel = new PSAssetWidgetRelationship(pageId, Long.parseLong(widgetId),
                "percPageAutoList", origAssetId, 1, wi.getName());
        awRel.setResourceType(PSAssetResourceType.shared);
        String awRelId = testSiteData.getAssetRestClient().createAssetWidgetRelationship(awRel);
        assertNotNull(awRelId); 
        
        List<PSAssetDropCriteria> orgDropCriteria = testSiteData.getAssetRestClient().
                                                    getWidgetAssetCriteria(pageId,true);
        String origWidgetName = orgDropCriteria.get(0).getWidgetName();
        String origWidgetId = orgDropCriteria.get(0).getWidgetId();
        
        String copyName = "CopyOf" + siteName;
        PSSiteCopyRequest req = new PSSiteCopyRequest();
        req.setSrcSite(siteId);
        req.setCopySite(copyName);
        req.setAssetFolder("Test");
        
        String newAssetFolder = PSPathUtils.ASSETS_FINDER_ROOT + '/' + copyName;
        
        // delete new asset folder if it exists
        try
        {
            getPathServiceRestClient().find(newAssetFolder);
            PSDeleteFolderCriteria criteria = new PSDeleteFolderCriteria();
            criteria.setPath(newAssetFolder);
            criteria.setSkipItems(SkipItemsType.NO);
            getPathServiceRestClient().deleteFolder(criteria);
        }
        catch (Exception e)
        {
            // folder doesn't exist            
        }
                
        PSSite copy = restClient.copy(req);
        assertNotNull(copy);
        String copyId = copy.getId();
        siteCleaner.add(copyId);
        
        // make sure the asset folder was copied
        try
        {
            getPathServiceRestClient().find(newAssetFolder);
            folderCleaner.add(newAssetFolder);
        }
        catch (Exception e)
        {
            fail("Folder '" + newAssetFolder + "' should have been created");         
        }
        
        // folder should contain copy of page auto list
        List<PSPathItem> children = getPathServiceRestClient().findChildren(newAssetFolder);
        assertEquals(1, children.size());
        
        PSPathItem child = children.get(0);
        assertEquals(asset.getName(), child.getName());
        assertTrue(!child.getId().equals(asset.getId()));
        
        // check the name
        assertEquals(copy.getName(), copyName);
        
        // check the templates
        PSSiteTemplateRestClient tempClient = getSiteTemplateServiceRestClient();
        List<PSTemplateSummary> siteTemplates = tempClient.findTemplatesBySite(siteId);
        List<PSTemplateSummary> copyTemplates = tempClient.findTemplatesBySite(copyId);
        assertEquals(siteTemplates.size(), copyTemplates.size());
        
        PSTemplateSummary siteTemp = siteTemplates.get(0);
        String siteTempId = siteTemp.getId();
        PSTemplateSummary copyTemp = copyTemplates.get(0);
        assertFalse(copyTemp.getId().equals(siteTempId));
        
        // reset the id so we can compare the templates
        copyTemp.setId(siteTempId);
        copyTemp.setImageThumbPath("Thumbnail");
        siteTemp.setImageThumbPath(copyTemp.getImageThumbPath());
        assertEquals(siteTemp, copyTemp);
    
        // check the publishing configuration
        PSSitePublishProperties sitePubProps = restClient.getSitePublishProperties(siteName);
        String sitePubPropsId = sitePubProps.getId();
        PSSitePublishProperties copyPubProps = restClient.getSitePublishProperties(copyName);
        assertFalse(copyPubProps.getId().equals(sitePubPropsId));
        assertEquals(copyName, copyPubProps.getSiteName());
        
        // reset the id and name so we can compare the configurations
        copyPubProps.setId(sitePubPropsId);
        copyPubProps.setSiteName(siteName);
        assertEquals(sitePubProps, copyPubProps);
        
        PSPage pageAfterCopy = testSiteData.getPageRestClient().findPageByFullFolderPath(copy.getFolderPath() +
                "/" + "testPage" );

        List<PSAssetDropCriteria> copyDropCriteria = testSiteData.getAssetRestClient().
        getWidgetAssetCriteria(pageAfterCopy.getId(),true);

        String copyWidgetName = copyDropCriteria.get(0).getWidgetName();
        String copyWidgetId = copyDropCriteria.get(0).getWidgetId();

        assertEquals(origWidgetName,copyWidgetName);
        assertEquals(origWidgetId, copyWidgetId);

        assertListAsset(page.getId(), pageAfterCopy.getId());
        
    }
    
    //Fixme
    @Test(expected=Exception.class)
    public void testCreateSiteWithJsonValidationFailure() throws Exception {
        String siteJson = "{\"PSSite\":{\"name\":\"JsonTest\",\"label\":\"Json Test PSSite\"}}";
        restClient.getRequestHeaders().put("Accept", "application/json");
        String response = restClient.POST("/Rhythmyx/services/sitemanage/site/", siteJson, "application/json");
        assertNotNull(response);
        //restClient.delete("JsonTest");
        
    }
       
    private void assertSiteNotExist(String siteId) {
        try {
            restClient.get(siteId);
            fail("Rest client should have thrown an exception");
        } catch (RestClientException e) {
            assertEquals(500, e.getStatus());
        }
    }
    
    private PSSite createSite()
    {
        PSSite site = new PSSite();
        site.setName(SITE_NAME_PREFIX + "--" + System.currentTimeMillis());
        site.setLabel("My test site");
        site.setHomePageTitle("homePageTitle");
        site.setNavigationTitle("navigationTitle");
        site.setBaseTemplateName(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME);
        site.setTemplateName("templateName");
        
        siteCleaner.add(site.getId());
             
        return site;
    }
      
    /**
     * Asserts that a page auto list asset exists on each of the specified pages with a different id and query field
     * value.
     * 
     * @param origId id of the original page.
     * @param copyId id of the copied page.
     */
    private void assertListAsset(String origId, String copyId)
    {
        String orgRenderStr = testSiteData.getRenderServiceClient().renderPageForEdit(origId);
        String orgAssetid = null;
        String token = "assetId=\"";
        if(orgRenderStr.indexOf(token) != -1)
        {
            orgRenderStr = orgRenderStr.substring(orgRenderStr.indexOf(token) + token.length());
            orgAssetid = orgRenderStr.substring(0,orgRenderStr.indexOf("\""));
        }
        
        String newRenderStr = testSiteData.getRenderServiceClient().renderPageForEdit(copyId);
        String newAssetid = null;
        if(newRenderStr.indexOf(token) != -1)
        {
            newRenderStr = newRenderStr.substring(newRenderStr.indexOf(token) + token.length());
            newAssetid = newRenderStr.substring(0,newRenderStr.indexOf("\""));
        }
        
        assertNotNull(orgAssetid);
        assertNotNull(newAssetid);
        
        assertTrue(!orgAssetid.equals(newAssetid));
        
        //load original asset summary
        PSAsset origAsset =  testSiteData.getAssetRestClient().load(orgAssetid);
        
        //load new asset summary
        PSAsset newAsset =  testSiteData.getAssetRestClient().load(newAssetid);
        
        assertTrue(origAsset.getType().equals("percPageAutoList"));
        assertTrue(newAsset.getType().equals("percPageAutoList"));
        
        Map<String, Object>  origFields = origAsset.getFields();
        
        Map<String, Object>  newFields = newAsset.getFields();
        
        String origQuery = (String)origFields.get("query");
        
        String newQuery = (String)newFields.get("query");
        
        assertTrue(!origQuery.equals(newQuery));
    }
    
    /**
     * Create a Rich text widget item, and its corresponding asset, with the
     * given widget name, widget description, and slot id (or widget id). The
     * asset is saved into the system.
     * 
     * @param name {@link String} with the name of the widget. May be blank.
     * @param description {@link String} with the description of the widget. May
     *            be blank.
     * @param slotid {@link String} with the widget id. May be blank.
     * @return {@link PSPair}<{@link PSWidgetItem}, {@link PSAsset}> never
     *         <code>null</code>, contains the widget item in the first place,
     *         and the asset in the second place.
     */
    private PSPair<PSWidgetItem, PSAsset> createRichTextWidgetItem(String name, String description, String slotid)
    {
        PSAsset asset = new PSAsset();
        asset.getFields().put("sys_title", "LocalAsset" + System.currentTimeMillis());
        asset.setType("percRichTextAsset");
        asset.getFields().put("text", "Test Rich text");
        asset.setFolderPaths(asList("//Folders"));
        asset = testSiteData.saveAsset(asset);

        PSWidgetItem widget = new PSWidgetItem();
        widget.setDefinitionId("percRichText");
        widget.setDescription(description);
        widget.setName(name);
        widget.setId(slotid);

        return new PSPair<PSWidgetItem, PSAsset>(widget, asset);
    }

    /**
     * Create a Raw HTML widget item, and its corresponding asset, with the
     * given widget name, widget description, and slot id (or widget id). The
     * asset is saved into the system.
     * 
     * @param name {@link String} with the name of the widget. May be blank.
     * @param description {@link String} with the description of the widget. May
     *            be blank.
     * @param slotid {@link String} with the widget id. May be blank.
     * @return {@link PSPair}<{@link PSWidgetItem}, {@link PSAsset}> never
     *         <code>null</code>, contains the widget item in the first place,
     *         and the asset in the second place.
     */
    private PSPair<PSWidgetItem, PSAsset> createRawHtmlWidgetItem(String name, String description, String slotid)
    {
        PSAsset asset = new PSAsset();
        asset.getFields().put("sys_title", "LocalAsset" + System.currentTimeMillis());
        asset.setType("percRawHtmlAsset");
        asset.getFields().put("html", "Test Rich text");
        asset.setFolderPaths(asList("//Folders"));
        asset = testSiteData.saveAsset(asset);
        
        PSWidgetItem widget = new PSWidgetItem();
        widget.setDefinitionId("percRawHtml");
        widget.setDescription(description);
        widget.setName(name);
        widget.setId(slotid);
        
        return new PSPair<PSWidgetItem, PSAsset>(widget, asset);
    }

    /**
     * Adds the given widgets to the template. Creates a region and adds the
     * widgets to it. Then it sets that region to a region tree and set it to
     * the template.
     * 
     * @param template {@link PSTemplate} object, must not be <code>null</code>.
     * @param widgetAssetPairs {@link List}<{@link PSWidgetItem}> with the
     *            widgets to add. May be empty but not <code>null</code>.
     * @throws Exception 
     */
    private void addWidgetsToTemplateAndSave(PSTemplate template, List<PSPair<PSWidgetItem, PSAsset>> widgetAssetPairs) throws Exception
    {
        notNull(template);
        notNull(widgetAssetPairs);

        List<PSWidgetItem> widgets = new ArrayList<PSWidgetItem>();
        for (PSPair<PSWidgetItem, PSAsset> pair : widgetAssetPairs)
        {
            widgets.add(pair.getFirst());
        }

        PSRegion region = new PSRegion();
        region.setRegionId("region");
        region.setStartTag("<div id=\"region\" class=\"perc-region\" >");
        region.setEndTag("</div>");

        PSRegionTree regTree = new PSRegionTree();
        regTree.setRegionWidgets(region.getRegionId(), widgets);
        regTree.setRootRegion(region);

        template.setRegionTree(regTree);

        PSTemplateServiceClient tempServiceClient = getTemplateServiceClient();
        tempServiceClient.save(template);
    }
    
    /**
     * Creates a new page with the given name assigned to template given as parameter into the specified folder.
     * 
     * @param name {@link String} the page name, must not be <code>null</code>.
     * @param template {@link PSTemplate} object, must not be <code>null</code>.
     * @param templateId {@link String} the template for the page, must not be <code>null</code
     *
     * @throws Exception 
     */
    private String createPage(String name, String folderPath, String templateId) throws Exception
    {
        notEmpty(templateId);
        String pageId = null;

        PSPage page = new PSPage();
        page.setName(name);
        page.setTitle(name);
        page.setFolderPath(folderPath);
        page.setTemplateId(templateId);
        page.setLinkTitle("dummy");

        PSPage r = testSiteData.getPageRestClient().save(page);
        pageId = r.getId();

        pageCleaner.add(pageId);
        
        return pageId;
    }
    

}
