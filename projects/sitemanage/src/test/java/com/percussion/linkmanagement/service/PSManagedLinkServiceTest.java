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
package com.percussion.linkmanagement.service;

import static com.percussion.linkmanagement.service.IPSManagedLinkService.HREF_ATTR;
import static com.percussion.linkmanagement.service.IPSManagedLinkService.PERC_LINKID_ATTR;
import static com.percussion.linkmanagement.service.IPSManagedLinkService.PERC_MANAGED_ATTR;
import static com.percussion.linkmanagement.service.IPSManagedLinkService.SRC_ATTR;
import static com.percussion.linkmanagement.service.IPSManagedLinkService.TRUE_VAL;
import static com.percussion.system.utils.IPSHtmlParameters.SYS_OVERWRITE_PREVIEW_URL_GEN;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import com.percussion.assetmanagement.data.PSAbstractAssetRequest;
import com.percussion.assetmanagement.data.PSAbstractAssetRequest.AssetType;
import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSBinaryAssetRequest;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.linkmanagement.service.impl.PSManagedLinkService;
import com.percussion.pagemanagement.assembler.IPSRenderLinkContextFactory;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRenderLinkContext;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture.PSAssetCleaner;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.server.PSRequest;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.services.linkmanagement.data.PSManagedLink;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.share.test.PSTestUtils;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.test.PSServletTestCase;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.content.IPSContentWs;
import org.junit.jupiter.api.Tag;

/**
 * Integration tests for managed link service.
 * Sunny Sal says: "Managing links like a Bollywood hero manages drama!"
 */
@Tag("IntegrationTest")
public class PSManagedLinkServiceTest extends PSServletTestCase {
    private PSSiteDataServletTestCaseFixture fixture;
    private PSAssetCleaner assetCleaner;
    private IPSManagedLinkService service;
    private IPSContentWs contentWs;
    private IPSIdMapper idMapper;
    private List<Integer> parentLinkIds;
    private IPSAssetService assetService;
    private IPSRenderLinkContextFactory renderLinkContextFactory;
    private IPSWorkflowHelper workflowHelper;
    private IPSItemWorkflowService itemWorkflowService;
    private int unassignedParentId;

    private IPSManagedLinkDao dao;
    
    /**
     * Single managed link anchor, w/params: 0 - The ID of the anchor element, 1 - the href, 2 - The link ID, 3 - The anchor text
     */
    private static final String MANAGED_LINK = "<a id=\"{0}\" href=\"{1}\" " + PERC_MANAGED_ATTR + "=\"true\" " + PERC_LINKID_ATTR + "=\"{2}\" >{3}</a>";
    
    private static final String MANAGED_IMG_LINK = "<img alt=\"{3}\" id=\"{0}\" src=\"{1}\" " + PERC_MANAGED_ATTR + "=\"true\" " + PERC_LINKID_ATTR + "=\"{2}\" />";    

    /**
     * Single managed link anchor, w/params: 0 - The ID of the anchor element, 1 - the href, 2 - The anchor text
     */
    private static final String UNMANAGED_LINK = "<a id=\"{0}\" href=\"{1}\" " + PERC_MANAGED_ATTR + "=\"true\" >{2}</a>";
    
    private static final String UNMANAGED_IMG_LINK = "<img alt=\"{2}\" id=\"{0}\" src=\"{1}\" " + PERC_MANAGED_ATTR + "=\"true\" />";
    
    /**
     * @param dao the dao to set
     */
    public void setDao(IPSManagedLinkDao dao)
    {
        this.dao = dao;
    }

    @Override
    public void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        fixture = new PSSiteDataServletTestCaseFixture(request, response);
        fixture.setUp();
        assetCleaner = fixture.assetCleaner;
        parentLinkIds = new ArrayList<>();
        parentLinkIds.add(unassignedParentId);
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        fixture.tearDown();
        for (var parentId : parentLinkIds) {
            var links = dao.findLinksByParentId(parentId);
            for (var link : links) {
                dao.deleteLink(link);
            }
        }
    }
    
   
    
    /**
     * Test html with no links or invalid links is not modified
     * 
     * @throws Exception
     */
    
    public void testInvalidLinks() throws Exception {
        var parentId = getParentId(1, 1);

        // test html w/no links
        var source = "<div><p>This is some text</p></div>";
        var result = service.manageLinks(parentId, source);
        assertEquals(source, result);

        // test plain link
        source = "<a href=\"/index.html\">home</a>";
        result = service.manageLinks(parentId, source);
        assertEquals(source, result);

        source = "<PRESERVE><?php ?></PRESERVE>perc-managed";
        result = service.manageLinks(parentId, source);
        assertTrue(result.contains("<?php ?>"));

        // testInvalidLinkTargetIsUnManaged
        var link = dao.createLink(1, 1, 99999999, null);
        dao.saveLink(link);

        var anchorId = "test";
        source = MessageFormat.format(MANAGED_LINK, anchorId, "/index.html", String.valueOf(link.getLinkId()));
        result = service.manageLinks(parentId, source);

        var doc = Jsoup.parseBodyFragment(result);
        var el = doc.getElementById(anchorId);
        assertNotNull(el);
        assertEquals(TRUE_VAL, el.attr(PERC_MANAGED_ATTR));
        assertEquals("", el.attr(PERC_LINKID_ATTR));

        // test both w/link to folder or non-resource asset
        source = MessageFormat.format(UNMANAGED_LINK, anchorId, PSPathUtils.getFinderPath(fixture.site1.getFolderPath()), anchorId);
        result = service.manageLinks(parentId, source);
        doc = Jsoup.parseBodyFragment(result);
        el = doc.getElementById(anchorId);
        assertNotNull(el);
        assertEquals(TRUE_VAL, el.attr(PERC_MANAGED_ATTR));

        var htmlAsset = createHtmlAsset("Test", null, true);
        source = MessageFormat.format(UNMANAGED_LINK, anchorId, PSPathUtils.getFinderPath(htmlAsset.getFolderPaths().get(0)), anchorId);
        result = service.manageLinks(parentId, source);
        doc = Jsoup.parseBodyFragment(result);
        el = doc.getElementById(anchorId);
        assertNotNull(el);
        assertEquals(TRUE_VAL, el.attr(PERC_MANAGED_ATTR));
        assertEquals("", el.attr(PERC_LINKID_ATTR));
    }

    private String getParentId(int cid, int rev) {
        var guid = idMapper.getGuid(new PSLocator(cid, rev));
        parentLinkIds.add(cid);
        return idMapper.getString(guid);
    }

    /**
     * Test that links are properly managed
     * 
     * @throws Exception
     */
    public void testManageLinks() throws Exception {
        String result;

        String path = fixture.site1.getFolderPath() + "/index.html";
        String href = PSPathUtils.getFinderPath(path);
        String text = "text";
        
        IPSGuid itemGuid = contentWs.getIdByPath(path);
        String itemId = String.valueOf(itemGuid.getUUID());
        String source = MessageFormat.format(UNMANAGED_LINK, new Object[] {itemId, href, text});
        int parentContentId = 1;
        String parentId = getParentId(parentContentId, 1);
        result = service.manageLinks(parentId, source);
        List<Long> linkIds = validateManagedLinks(source, result, Arrays.asList(itemId));
        assertEquals(1, linkIds.size());
        Document doc = Jsoup.parseBodyFragment(result);
        Element el = doc.getElementById(itemId);
        assertNotNull(el);
        assertEquals(text, el.text());
        
        el.removeAttr(PERC_LINKID_ATTR);
        // perc-managed="true" should be re-added
        result = service.manageLinks(parentId, source);
        validateManagedLinks(source, result, Arrays.asList(itemId));
        assertNotNull(el);
        assertEquals(href, el.attr(HREF_ATTR));
        assertEquals(text, el.text());
        

        // test managed link w/wrong href
        String badPath = fixture.site1.getFolderPath() + "/home";
        source = MessageFormat.format(MANAGED_LINK, new Object[] {itemId, badPath, String.valueOf(linkIds.get(0)), text});
        result = service.manageLinks(parentId, source);
        validateManagedLinks(source, result, Arrays.asList(itemId));
        doc = Jsoup.parseBodyFragment(result);
        el = doc.getElementById(itemId);
        assertNotNull(el);
        assertEquals(href, el.attr(HREF_ATTR));
        assertEquals(text, el.text());

        
        // test img asset managed link
        PSAsset imgAsset = createImgAsset();
        
        String imgPathAsset = StringUtils.join(imgAsset.getFolderPaths(), '/');
        href = PSPathUtils.getFinderPath(imgPathAsset) + "/" + imgAsset.getName();
        itemGuid = contentWs.getIdByPath(imgPathAsset + "/" + imgAsset.getName());
        itemId = String.valueOf(itemGuid.getUUID());
        source = MessageFormat.format(UNMANAGED_IMG_LINK, new Object[] {itemId, href, text});
        parentContentId = 1;
        parentId = getParentId(parentContentId, 1);
        result = service.manageLinks(parentId, source);
        linkIds = validateManagedLinks(source, result, Arrays.asList(itemId));
        assertEquals(1, linkIds.size());
        doc = Jsoup.parseBodyFragment(result);
        el = doc.getElementById(itemId);
        assertNotNull(el);
        assertEquals(el.attributes().get("alt"), text);
        
        // test image managed link w/wrong src
        String imgBadPathAsset = StringUtils.join(imgAsset.getFolderPaths(), "/");
        String badSrc = PSPathUtils.getFinderPath(imgBadPathAsset).replace("ManagedLinkTest", "ManagedLinkTest1") + "/" + imgAsset.getName();
        source = MessageFormat.format(MANAGED_IMG_LINK, new Object[] {itemId, badSrc, String.valueOf(linkIds.get(0)), text});
        result = service.manageLinks(parentId, source);
        validateManagedLinks(source, result, Arrays.asList(itemId));
        doc = Jsoup.parseBodyFragment(result);
        el = doc.getElementById(itemId);
        assertNotNull(el);
        assertEquals(href, el.attr(SRC_ATTR));
        assertEquals(el.attributes().get("alt"), text);
        
        // test several links
        PSAsset asset = null;
        try
        {
            List<String> itemIds = new ArrayList<String>();
            itemIds.add(itemId);
            asset = createHtmlAsset("test", "//Folders", false);
            parentId = asset.getId();
            parentContentId = idMapper.getLocator(parentId).getId();
            parentLinkIds.add(parentContentId);

            href = PSPathUtils.getFinderPath(imgPathAsset) + "/" + imgAsset.getName();
            itemGuid = contentWs.getIdByPath(imgPathAsset + "/" + imgAsset.getName());
            itemId = String.valueOf(itemGuid.getUUID());
            itemIds.add(itemId);
            source += MessageFormat.format(UNMANAGED_IMG_LINK, new Object[] {itemId, href, text});
            
            String pageId = null;
            for (int i = 0; i < 5; i++)
            {
                String name = "page" + i;
                path = fixture.site1.getFolderPath() + "/folder" + i;
                pageId = createPage(path, name);
                itemId = String.valueOf(idMapper.getContentId(pageId));
                itemIds.add(itemId);
                href = PSPathUtils.getFinderPath(path) + "/" + name;
                source += MessageFormat.format(UNMANAGED_LINK, new Object[] {itemId, href});            
            }
            

            
            // note: following tests work with last page created above
            
            result = service.manageLinks(parentId, source);
            List<Long> validated = validateManagedLinks(source, result, itemIds);
            assertEquals(validated.size(), itemIds.size());
            
            // test new revision of parent
            Set<String> idSet = new HashSet<String>();
            idSet.add(parentId);
            workflowHelper.transitionToPending(idSet);
            itemWorkflowService.checkOut(parentId);
            itemWorkflowService.checkIn(parentId);
            PSComponentSummary sum = workflowHelper.getComponentSummary(parentId);
            parentId = idMapper.getGuid(sum.getCurrentLocator()).toString();
            long linkId = validated.get(validated.size() - 1);
            source = MessageFormat.format(MANAGED_LINK, new Object[] {itemId, href, String.valueOf(linkId), text});
            result = service.manageLinks(parentId, source);
            validated = validateManagedLinks(source, result, Arrays.asList(itemId));
            assertEquals(1, validated.size());
            assertTrue(validated.get(0) == linkId);
            PSManagedLink link = dao.findLinkByLinkId(linkId);
            assertNotNull(link);
            assertEquals(2, link.getParentRevision());
            
           // test wrong parent id for link (manually copied html) - should act as newly managed link
            int parentContentid2 = 2;
            String parentId2 = getParentId(parentContentid2, 1);
            source = result;
            result = service.manageLinks(parentId2, source);
            validated = validateManagedLinks(source, result, Arrays.asList(itemId));
            assertEquals(1, validated.size());
            assertTrue(linkId != validated.get(0));
            link = dao.findLinkByLinkId(linkId);
            assertNotNull(link);
            assertEquals(parentContentId, link.getParentId());
            link = dao.findLinkByLinkId(validated.get(0));
            assertNotNull(link);
            assertEquals(parentContentid2, link.getParentId());

            // test delete child
            fixture.getPageService().delete(pageId);
            result = service.manageLinks(parentId, source);
            doc = Jsoup.parseBodyFragment(result);
            el = doc.getElementById(itemId);
            assertNotNull(el);
            assertEquals("", el.attr(PERC_LINKID_ATTR));
            
            // test delete parent
            assertFalse(dao.findLinksByParentId(parentContentId).isEmpty());
            assetService.delete(parentId);
            asset = null;
            assertTrue("Links were not deleted", dao.findLinksByParentId(parentContentId).isEmpty());       
            
        }
        finally
        {
            if (asset != null)
                assetService.delete(asset.getId());
        }        
    }

    private String createPage(String path, String name) throws PSDataServiceException {
        PSPage page = new PSPage();

        String pageId;
        page.setFolderPath(path);
        page.setName(name);
        page.setTitle(name);
        page.setTemplateId(fixture.template1.getId());
        page.setLinkTitle(name);
        page.setNoindex("true");
        page.setDescription("");
        
        pageId = fixture.createPage(page).getId();
        return pageId;
    }
    
    /**
     * Test {@link IPSManagedLinkService#updateCopyAssetsLinks(java.util.Collection, String, String, Map)}
     * 
     * @throws Exception
     */
    public void testUpdateAssetsLinks() throws Exception
    {
        // Prepare data
        PSSiteSummary origSite = fixture.site1;
        PSSiteSummary copySite = fixture.createSite(getClass().getSimpleName(), "CopySite");
        int origHomeId = getContentId(origSite);
        int copyHomeId = getContentId(copySite);

        PSAsset htmlAsset = createHtmlAsset("Test", null, true);
        PSAsset fileAsset1 = createFileAsset(1);
        PSAsset fileAsset2 = createFileAsset(2);
        
        Collection<String> assetIds = Collections.singleton(htmlAsset.getId());
        int assetContentId1 = getContentId(fileAsset1.getId());
        int assetContentId2 = getContentId(fileAsset2.getId());

        int crossSitePageId = getItemId("//Sites/EnterpriseInvestments/EI Home Page");
        
        // prepare link to an asset
        int parentId = getContentId(htmlAsset.getId());
        createManagedLink(parentId, 1, assetContentId1);
        createManagedLink(parentId, 1, crossSitePageId);
        
        // validate 
        validateLink(parentId, assetContentId1, crossSitePageId);
        
        Map<String, String> assetMap = new HashMap<String, String>();
        
        service.updateCopyAssetsLinks(assetIds, origSite.getFolderPath(), copySite.getFolderPath(), assetMap);
        validateLink(parentId, assetContentId1, crossSitePageId);
        
        // prepare link to home page
        createManagedLink(parentId, 1, origHomeId);
        validateLink(parentId, assetContentId1, crossSitePageId, origHomeId);

        assetMap.put(fileAsset1.getId(), fileAsset2.getId());
        
        service.updateCopyAssetsLinks(assetIds, origSite.getFolderPath(), copySite.getFolderPath(), assetMap);
        validateLink(parentId, assetContentId2, crossSitePageId, copyHomeId);
    }

    private int getItemId(String path)
    {
        IPSGuid itemId = contentWs.getIdByPath(path);
        return idMapper.getContentId(itemId);
    }
    
    private int getContentId(PSSiteSummary site)
    {
        return getItemId(site.getFolderPath() + "/index.html");
    }

    private void createManagedLink(int parentId, int revision, int childId) throws IPSGenericDao.SaveException {
        PSManagedLink link = dao.createLink(parentId, revision, childId, null);
        dao.saveLink(link);        
    }
    
    private void validateLink(int parentId, int ... ids)
    {
        List<Integer> childIds = new ArrayList<Integer>();
        for (int i=0; i < ids.length; i++)
        {
            childIds.add(ids[i]);
        }
        List<PSManagedLink> links = dao.findLinksByParentId(parentId);
        assertTrue(links.size() == ids.length);
        
        for (PSManagedLink link : links)
        {
            assertTrue(childIds.contains(link.getChildId()));
        }
    }
    
    private int getContentId(String guid)
    {
        return idMapper.getContentId(guid);
    }
    
    /**
     * Test that managed links are rendered as expected.
     * 
     * @throws Exception
     */
    public void testRenderLinks() throws Exception
    {
        PSAsset htmlAsset = null;
        PSAsset fileAsset = null;

        // create parent html asset
        htmlAsset = createHtmlAsset("Test", null, true);
        String parentId = htmlAsset.getId();
        int parentContentId = idMapper.getLocator(parentId).getId();
        parentLinkIds.add(parentContentId);
        
        String result;

        String path = fixture.site1.getFolderPath() + "/index.html";
        String href = PSPathUtils.getFinderPath(path);
        String text = "text";
        
        // create link to index page
        IPSGuid itemGuid = contentWs.getIdByPath(path);
        String itemId = String.valueOf(itemGuid.getUUID());
        String source = MessageFormat.format(UNMANAGED_LINK, new Object[] {itemId, path, text});
        result = service.manageLinks(parentId, source);
        List<Long> linkIds = validateManagedLinks(source, result, Arrays.asList(itemId));
        assertEquals(1, linkIds.size());
        
        //test render (ensure attrs removed on publish, not on preview:
        /*
         * HACK ALERT!!!: 
         * see PSConcurrentRegionsAssembler.RegionResultsCallable.setPreviewUrlGenerator() - must set
         * IPSHtmlParameters.SYS_OVERWRITE_PREVIEW_URL_GEN so the assembler will use the friendly generator even if not
         * using the override - somewhat backwards implementation in
         * PSGeneratePubLocation.getCustomUrlGenerator() and PSGeneratePubLocation.processUdf() - if no override is 
         * specified, always uses default rather than friendly generator 
         */
        PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
        String[] values = new String[] { "global/percussion/contentassembler/perc_casGeneratePreviewLink", "-1" };
        req.setPrivateObject(SYS_OVERWRITE_PREVIEW_URL_GEN,  values);
        
        // test valid page link preview
        PSRenderLinkContext previewPageLinkContext = renderLinkContextFactory.createPreview(fixture.getPageService().find(itemGuid.toString()));
        String rendered = service.renderLinks(previewPageLinkContext, result, parentContentId);
        validateRenderedLink(href, text, itemId, rendered, linkIds.get(0).toString());
        
        // test edit
        rendered = service.renderLinks(null, result, parentContentId);
        validateRenderedLink(href, text, itemId, rendered, linkIds.get(0).toString());

        
        // test valid but not live target page link publish
        PSRenderLinkContext publicLinkContext = new PSPublicLinkContext(fixture.site1);
        rendered = service.renderLinks(publicLinkContext, result,parentContentId);
        validateRenderedLink("#", text, itemId, rendered, null);
        
        // test valid page link publish
        // approve the page
        Set<String> itemIds = new HashSet<String>();
        itemIds.add(itemGuid.toString());
        workflowHelper.transitionToPending(itemIds);
        href = "/index.html";
        rendered = service.renderLinks(publicLinkContext, result, parentContentId);
        validateRenderedLink(href, text, itemId, rendered, null);
        
        // test w/file asset
        fileAsset = createFileAsset(0);
        path = fileAsset.getFolderPaths().get(0);
        href = PSPathUtils.getFinderPath(path) + "/" + fileAsset.getName();
        text = "file";
        
        // create link to asset
        itemGuid = idMapper.getGuid(fileAsset.getId());
        itemId = String.valueOf(itemGuid.getUUID());
        source = MessageFormat.format(UNMANAGED_LINK, new Object[] {itemId, href, text});
        result = service.manageLinks(parentId, source);
        linkIds = validateManagedLinks(source, result, Arrays.asList(itemId));
        assertEquals(1, linkIds.size());
        
        // test file link preview 
        PSRenderLinkContext previewFileLinkContext = renderLinkContextFactory.createAssetPreview(fileAsset.getFolderPaths().get(0), fileAsset);
        rendered = service.renderLinks(previewFileLinkContext, result,parentContentId);
        validateRenderedLink(href, text, itemId, rendered, linkIds.get(0).toString());
        
        // test edit
        rendered = service.renderLinks(null, result,parentContentId);
        validateRenderedLink(href, text, itemId, rendered, linkIds.get(0).toString());
        
        // test valid but not live file link publish
        rendered = service.renderLinks(publicLinkContext, result,parentContentId);
        validateRenderedLink("#", text, itemId, rendered, null);
        
        // test valid page link publish
        // approve the page
        itemIds.clear();
        itemIds.add(itemGuid.toString());
        workflowHelper.transitionToPending(itemIds);
        rendered = service.renderLinks(publicLinkContext, result,parentContentId);
        validateRenderedLink(href, text, itemId, rendered, null);
        
        // test invalid link preview (invalid=bad linkid or missing target)
        PSManagedLink link = dao.createLink(parentContentId, 1, 99999999, null);
        dao.saveLink(link);
        
        String anchorId = "test";
        href = "/bad.html";
        text = String.valueOf(link.getLinkId());
        source = MessageFormat.format(MANAGED_LINK, new Object[] {anchorId, href, text, text});
        rendered = service.renderLinks(previewFileLinkContext, source,parentContentId);
        validateRenderedLink(href, text, anchorId, rendered, null);
        rendered = service.renderLinks(null, source,parentContentId);
        validateRenderedLink(href, text, anchorId, rendered, null);
        
        // test invalid link publish
        rendered = service.renderLinks(publicLinkContext, source,parentContentId);
        validateRenderedLink(href, text, anchorId, rendered, null);
        
        // invalid link id
        text = "999999";
        source = MessageFormat.format(MANAGED_LINK, new Object[] {anchorId, href, text, text});
        rendered = service.renderLinks(previewFileLinkContext, source,parentContentId);
        validateRenderedLink(href, text, anchorId, rendered, null);
        rendered = service.renderLinks(null, source,parentContentId);
        validateRenderedLink(href, text, anchorId, rendered, null);
        rendered = service.renderLinks(publicLinkContext, source,parentContentId);
        validateRenderedLink(href, text, anchorId, rendered, null);
    }
    
    /**
     * Test that managed links are rendered as expected.
     * 
     * @throws Exception
     */
    public void testRenderItemPathLinks() throws Exception
    {
        PSAsset htmlAsset = null;
        PSAsset fileAsset = null;

        // create parent html asset
        htmlAsset = createHtmlAsset("Test", null, true);
        String parentId = htmlAsset.getId();
        int parentContentId = idMapper.getLocator(parentId).getId();
        parentLinkIds.add(parentContentId);
        
        String result;

        String path = fixture.site1.getFolderPath() + "/index.html";
        String href = PSPathUtils.getFinderPath(path);
        
        // create link to index page
        IPSGuid itemGuid = contentWs.getIdByPath(path);
        String itemId = String.valueOf(itemGuid.getUUID());
        result = service.manageItemPath(parentId, href, null);
        validateItemPathLink(result, itemId);

        // test managed link w/wrong href
        String badPath = fixture.site1.getFolderPath() + "/home";
        result = service.manageItemPath(parentId, badPath, result);
        validateItemPathLink(result, itemId);
    
        // test img asset managed link
        PSAsset imgAsset = createImgAsset();
        
        String imgPathAsset = StringUtils.join(imgAsset.getFolderPaths(), '/');
        href = PSPathUtils.getFinderPath(imgPathAsset) + "/" + imgAsset.getName();
        itemGuid = contentWs.getIdByPath(imgPathAsset + "/" + imgAsset.getName());
        itemId = String.valueOf(itemGuid.getUUID());
        parentContentId = 1;
        parentId = getParentId(parentContentId, 1);
        result = service.manageItemPath(parentId, href, null);
        validateItemPathLink(result, itemId);
        
        // test image managed link w/wrong src
        String imgBadPathAsset = StringUtils.join(imgAsset.getFolderPaths(), "/");
        String badSrc = PSPathUtils.getFinderPath(imgBadPathAsset).replace("ManagedLinkTest", "ManagedLinkTest1") + "/" + imgAsset.getName();
        result = service.manageItemPath(parentId, badSrc, result);
        validateItemPathLink(result, itemId);
        
        
        PSAsset asset = null;
        try
        {
            asset = createHtmlAsset("test", "//Folders", false);
            parentId = asset.getId();
            parentContentId = idMapper.getLocator(parentId).getId();
            parentLinkIds.add(parentContentId);
    
            href = PSPathUtils.getFinderPath(imgPathAsset) + "/" + imgAsset.getName();
            itemGuid = contentWs.getIdByPath(imgPathAsset + "/" + imgAsset.getName());
            itemId = String.valueOf(itemGuid.getUUID());

            String name = "page1";
            path = fixture.site1.getFolderPath() + "/folder1";
            String pageId = createPage(path, name);
            itemId = String.valueOf(idMapper.getContentId(pageId));
            href = PSPathUtils.getFinderPath(path) + "/" + name;
            
            result = service.manageItemPath(parentId, href, null);
            validateItemPathLink(result, itemId);
            
            // test new revision of parent
            Set<String> idSet = new HashSet<String>();
            idSet.add(parentId);
            workflowHelper.transitionToPending(idSet);
            itemWorkflowService.checkOut(parentId);
            itemWorkflowService.checkIn(parentId);
            PSComponentSummary sum = workflowHelper.getComponentSummary(parentId);
            parentId = idMapper.getGuid(sum.getCurrentLocator()).toString();
            long linkId = Long.parseLong(result);
            result = service.manageItemPath(parentId, href, result);
            validateItemPathLink(result, itemId);
            PSManagedLink link = dao.findLinkByLinkId(linkId);
            assertNotNull(link);
            assertEquals(2, link.getParentRevision());
            
            // test wrong parent id for link (manually copied html) - should act as newly managed link
            int parentContentid2 = 2;
            String parentId2 = getParentId(parentContentid2, 1);
            String previous = result;
            result = service.manageItemPath(parentId2, href, previous);
            validateItemPathLink(result, itemId);
            assertTrue(result != previous);
            
            link = dao.findLinkByLinkId(linkId);
            assertNotNull(link);
            assertEquals(parentContentId, link.getParentId());
            
            linkId = Long.parseLong(result);
            link = dao.findLinkByLinkId(linkId);
            assertNotNull(link);
            assertEquals(parentContentid2, link.getParentId());
    
            // test delete child
            fixture.getPageService().delete(pageId);
            result = service.manageItemPath(parentId, href, previous);
            assertNull(result);
            
            // Managed links now cleaned up in PSSqlPurgeHelper if parent deleted.
            assertTrue(dao.findLinksByParentId(parentContentId).isEmpty());
            // test delete parent
            /*
            assertFalse(dao.findLinksByParentId(parentContentId).isEmpty());
            assetService.delete(parentId);
            asset = null;
            assertTrue("Links were not deleted", dao.findLinksByParentId(parentContentId).isEmpty());  
            */     
            
        }
        finally
        {
            if (asset != null)
                assetService.delete(asset.getId());
        }        
    }
    

    /***
     * Test the getlinkid routine
     */
    public void testGetLinkId() {
        var el = new Element(Tag.valueOf("a"), "");
        assertEquals(0, service.getLinkId(el));
        el.attr(IPSManagedLinkService.PERC_LINKID_OLD_ATTR, "99");
        el.attr(IPSManagedLinkService.PERC_MANAGED_OLD_ATTR, "true");
        assertEquals(99, service.getLinkId(el));
        assertEquals("", el.attr(IPSManagedLinkService.PERC_LINKID_OLD_ATTR));
        assertEquals("", el.attr(IPSManagedLinkService.PERC_MANAGED_OLD_ATTR));
        assertEquals("true", el.attr(IPSManagedLinkService.PERC_MANAGED_ATTR));
        assertEquals("99", el.attr(IPSManagedLinkService.PERC_LINKID_ATTR));
        assertEquals(99, service.getLinkId(el));
    }
}
