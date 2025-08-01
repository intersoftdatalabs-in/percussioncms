                // ignore
/*
        }
    }

 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
    private boolean hasRelatedItems(String siteName) throws Exception {
        var contentWs = PSContentWsLocator.getContentWebservice();
import com.percussion.fastforward.managednav.IPSManagedNavService;
        var folderRoot = "//Sites/" + siteName;
import com.percussion.pagemanagement.data.PSPage;
        try {
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.rx.publisher.IPSPublisherJobStatus;
        } catch (PSErrorResultsException e) {
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.IPSSitemanageConstants;
        var items = contentWs.findFolderChildren(folderRoot, false);
        for (var item : items) {
            var ctName = item.getContentTypeName();
            if (getManagedNavService().getNavTreeContentTypeNames().contains(ctName)) {
import com.percussion.sitemanage.impl.PSSitePublishDaoHelper;
            } else if (item.getName().equals(PSSiteContentDao.HOME_PAGE_NAME)) {
import com.percussion.util.PSUrlUtils;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.PSErrorResultsException;
        return foundNavTree && foundHomePage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
    private boolean doesSiteFolderExist(String siteName) throws Exception {
        var contentWs = PSContentWsLocator.getContentWebservice();
@TestInstance(Lifecycle.PER_CLASS)
        var folderRoot = "//Sites/" + siteName;

        try {
    @Test
    public void testCRUD() throws Exception {
        } catch (PSErrorResultsException e) {
        var site1 = createSite("Site1-" + System.currentTimeMillis(), "Site 1");
        final String fileExt = "xhtml";
        final String siteProtocol = "http";
        final String defaultDocument = "index.xhtml";
        final String canonicalDist = "sections";
        site1.setDefaultFileExtention(fileExt);
    private void assertPublishingItems(String siteName) throws Exception {
        site1.setDefaultDocument(defaultDocument);
        site1.setCanonicalDist(canonicalDist);

    private void assertPublishingItems(String siteName, int edtns, int clists) throws Exception {
        var pubSvc = getPublisherService();
        try {
        var site = getSiteManager().loadSite(siteName);
        var siteId = site.getGUID();
        var editions = pubSvc.findAllEditionsBySite(siteId);
        var cLists = pubSvc.findAllContentListsBySite(siteId);

        assertEquals(edtns, editions.size(), "editions: " + editions.size());
        assertEquals(clists, cLists.size(), "content lists: " + cLists.size());
            assertNotNull(savedSite1, "Should retrieve saved site");
            assertEquals(fileExt, savedSite1.getDefaultFileExtention(), "The default file extension must be set to: " + fileExt);
    private boolean hasPublishingInfo(long jobId) {
        var pubSvc = getPublisherService();
            // publish the site
            var pubReq = new PSSitePublishRequest();

    private PSSite createSite(String name, String title) throws Exception {
        var site = new PSSite();
            assertTrue(jobId > -1);

            IPSPublisherJobStatus jobStatus;
            State jobState;

            // Time this out after a few minutes

            do {

    private PSFolder createFolder(String folderName, String sitePath) throws Exception {
        var contentWs = PSContentWsLocator.getContentWebservice();
        var testFolder = contentWs.addFolder(folderName, sitePath);
                }
            } while (jobState != State.COMPLETED &&
                    jobState != State.COMPLETED_W_FAILURE &&
                    jobState != State.BADCONFIG &&
    private void assertSite(PSSite site, boolean checkTemplate) throws Exception {
        var siteName = site.getName();

            assertNull(savedSite1);

            // site data should not exist
        if (checkTemplate) {
            //TODO Temporary commented until the fix is ready.
            var templates = getSiteTemplateService().findTemplatesBySite(siteName);
            assertEquals(1, templates.size());
            // test find all
            int currSites = siteDao.findAll().size();
            var userTemplates = getTemplateService().findAllUserTemplates();
            for (var userTemplate : userTemplates) {
                if (userTemplate.getName().equals(site.getTemplateName())) {
            site2.setFolderPath(savedSite2.getFolderPath());

            var sites = siteDao.findAll();
            assertEquals(2 + currSites, sites.size());
            assertTrue(sites.contains(site1));
            assertNotNull(siteTemplate, "Template was not created for site " + siteName);
        } finally {
            try {
                siteDao.delete(site1.getName());

    private IPSTemplateService getTemplateService() {
            }
            PSSiteDataServletTestCaseFixture.templateCleanUp(TEST_TEMPLATE_BASE, request, response);
        }
    private IPSSiteTemplateService getSiteTemplateService() {
    @Test
    public void testDefault_defaultFileExtention() throws Exception {
        var site = createSite("Site-" + System.currentTimeMillis(), "Site");
    private IPSManagedNavService getManagedNavService() {

    @Test

    private IPSRxPublisherService getRxPublisherService() {
    }


    private IPSPublisherService getPublisherService() {
        assertEquals("https", site.getSiteProtocol(), "The site protocol should be set to: https");
    }

    private IPSSiteManager getSiteManager() {
        var site = createSite("Site-" + System.currentTimeMillis(), "Site");
        assertEquals("index.html", site.getDefaultDocument(), "The default document should be set to: index.html");

    private PSSiteContentDao getSiteContentDao() {
    public void testDefault_canonicalDist() throws Exception {
        var site = createSite("Site-" + System.currentTimeMillis(), "Site");

    public void testDefault_isCanonicalReplace() throws Exception {
        var site = createSite("Site-" + System.currentTimeMillis(), "Site");
        assertTrue(site.isCanonicalReplace(), "The canonical replace should be ON");
    }

    @Test
    public void testUpdateSitePublishProperties() throws Exception {
        PSSecurityWsLocator.getSecurityWebservice().login(request, response, "admin1", "demo", null,
                "Enterprise_Investments_Admin", null);

        PSSite testingsite = null;
        var siteDao = (IPSiteDao) getBean("siteDao");
        var sitemgr = getSiteManager();
        try {
            try {
                testingsite = createSite("testingsite", "testingsite");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            // test save
            siteDao.save(testingsite);
            // Create the publish properties object
            var publishProps = new PSSitePublishProperties();
            publishProps.setSiteName(testingsite.getName());
            publishProps.setDeliveryRootPath("ftpsitelocation");
            publishProps.setFtpServerName("ftpserver");
            publishProps.setFtpUserName("user");
            publishProps.setFtpPassword("password");
            publishProps.setPublishType(PublishType.valueOf("ftp"));
            publishProps.setFtpServerPort(9999);
            publishProps.setId(testingsite.getId());

            // update the site with publish properties
            var psSite = sitemgr.loadSiteModifiable(testingsite.getName());
            var localDeliveryLocation = psSite.getRoot();
            siteDao.updateSitePublishProperties(psSite, publishProps);

            // Now check updated publish properties for this site
            var modifiedSite = sitemgr.loadSite(testingsite.getName());
            assertTrue(modifiedSite.getRoot().startsWith(publishProps.getDeliveryRootPath()));
            assertEquals(publishProps.getFtpServerName(), modifiedSite.getIpAddress());
            assertEquals(publishProps.getPublishType().toString(), getSiteDeliveryType(modifiedSite, siteDao));
            assertEquals(publishProps.getFtpUserName(), modifiedSite.getUserId());
            assertEquals(publishProps.getFtpPassword(), modifiedSite.getPassword());
            assertEquals(publishProps.getFtpServerPort(), modifiedSite.getPort());

            // let's switch it back to local
            publishProps.setPublishType(PublishType.valueOf("filesystem"));
            psSite = sitemgr.loadSiteModifiable(testingsite.getName());
            siteDao.updateSitePublishProperties(psSite, publishProps);
            modifiedSite = sitemgr.loadSite(testingsite.getName());
            assertEquals(localDeliveryLocation, modifiedSite.getRoot());
        } finally {
            try {
                siteDao.delete(testingsite.getName());
            } catch (DeleteException e) {
                // ignore
            }
        }
    }

    @Test
    public void testGetSiteDeliveryType() throws Exception {
        PSSecurityWsLocator.getSecurityWebservice().login(request, response, "admin1", "demo", null,
                "Enterprise_Investments_Admin", null);

        PSSite testingsite1 = null;
        var siteDao = (IPSiteDao) getBean("siteDao");
        var sitemgr = getSiteManager();
        try {
            try {
                testingsite1 = createSite("testingsite1", "testingsite1");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            // test save
            siteDao.save(testingsite1);
            var psSite = sitemgr.loadSiteModifiable(testingsite1.getName());
            assertEquals("filesystem", getSiteDeliveryType(psSite, siteDao));
        } finally {
            try {
                siteDao.delete(testingsite1.getName());
            } catch (DeleteException e) {
                // ignore
            }
        }
    }

    @Test
    public void testOnDemandPublish() throws Exception {
        PSSecurityWsLocator.getSecurityWebservice().login(request, response, "admin1", "demo", null,
                "Enterprise_Investments_Admin", null);

        PSSite testingsite2 = null;
        var siteDao = (IPSiteDao) getBean("siteDao");
        var sitemgr = getSiteManager();
        try {
            testingsite2 = createSite("testingsite2", "testingsite2");
            testingsite2 = siteDao.save(testingsite2);

            var siteName = testingsite2.getName();

            var publishProps = new PSSitePublishProperties();
            publishProps.setDeliveryRootPath("");
            publishProps.setSiteName(siteName);
            publishProps.setFtpServerName("ftpserver");
            publishProps.setFtpUserName("user");
            publishProps.setFtpPassword("password");
            publishProps.setPublishType(PublishType.valueOf("sftp"));
            publishProps.setFtpServerPort(9999);
            publishProps.setId(testingsite2.getId());

            // update the site with publish properties
            var psSite = sitemgr.loadSiteModifiable(siteName);
            siteDao.updateSitePublishProperties(psSite, publishProps);
            var deliveryType = getSiteDeliveryType(psSite, siteDao);
            assertEquals(publishProps.getPublishType().toString(), deliveryType);

            checkOnDemandPublishEdition(siteDao, siteName, psSite, deliveryType, PSSitePublishDaoHelper.PUBLISH_NOW);

            checkOnDemandPublishEdition(siteDao, siteName, psSite, deliveryType, PSSitePublishDaoHelper.UNPUBLISH_NOW);
        } finally {
            try {
                if (testingsite2 != null) {
                    siteDao.delete(testingsite2.getName());
                }
            } catch (DeleteException e) {
                // ignore
            }
        }
    }

    private void checkOnDemandPublishEdition(IPSiteDao siteDao, String siteName,
                                             IPSSite psSite, String deliveryType, String publishType) throws PSNotFoundException {
        // remove publish now from site
        var edtnCListName = PSSitePublishDaoHelper.createName(siteName, publishType);
        var pubSvc = getPublisherService();
        pubSvc.deleteEdition(pubSvc.findEditionByName(edtnCListName));
        pubSvc.deleteContentLists(Collections.singletonList(pubSvc.findContentListByName(edtnCListName)));

        assertNull(pubSvc.findEditionByName(edtnCListName));
        assertNull(pubSvc.findContentListByName(edtnCListName));

        // add publish now to site
        if (publishType.equals(PSSitePublishDaoHelper.PUBLISH_NOW))
            siteDao.addPublishNow(psSite);
        else
            siteDao.addUnpublishNow(psSite);

        assertNotNull(pubSvc.findEditionByName(edtnCListName));
        var cList = pubSvc.findContentListByName(edtnCListName);
        assertNotNull(cList);
        assertEquals(deliveryType, PSUrlUtils.getUrlParameterValue(cList.getUrl(),
                IPSHtmlParameters.SYS_DELIVERYTYPE));
    }

    @Test
    public void testCopy() throws Exception {
        PSSecurityWsLocator.getSecurityWebservice().login(request, response, "admin1", "demo", null,
                "Enterprise_Investments_Admin", null);

        PSSite origSite = null;
        PSSite copySite = null;
        PSSite site1 = null;
        PSSite site2 = null;
        var siteDao = (IPSiteDao) getBean("siteDao");
        var pageDao = (IPSPageDao) getBean("pageDao");

        PSPage site1Page1 = null;
        PSPage site1Page2 = null;
        try {
            // create a site
            var origSiteName = "origsite" + System.currentTimeMillis();
            origSite = createSite(origSiteName, origSiteName);
            origSite = siteDao.save(origSite);
            var origSiteId = origSite.getId();

            // create a new copy of the site
            var copySiteName = "copysite" + System.currentTimeMillis();
            copySite = siteDao.createSiteWithContent(origSiteId, copySiteName);
            var copySiteId = copySite.getId();

            // compare the copy with the original
            assertEquals(copySiteName, copySite.getName());
            assertEquals(origSite.getDescription(), copySite.getDescription());
            assertNotEquals(copySiteId, origSiteId);
            assertNotEquals(copySite.getFolderPath(), origSite.getFolderPath());
            assertNotEquals(copySite.getLabel(), origSite.getLabel());

            // check the copy
            var copySiteObj = siteDao.find(copySiteId);
            assertSite(copySiteObj, false);

            // Create a site to test sections, folders, pages
            var site1Name = "site1" + System.currentTimeMillis();
            site1 = createSite(site1Name, site1Name);
            site1 = siteDao.save(site1);
            var site1Id = site1.getId();

            // Add a folder
            createFolder("site1folder1", "//Sites/" + site1Name);

            // Create and save the page under the folder
            var folderPath = site1.getFolderPath() + "/" + "sitefolder1";
            site1Page2 = new PSPage();
            site1Page2.setName("site1page2");
            site1Page2.setTitle("page under the folder");
            site1Page2.setFolderPath(folderPath);
            site1Page2.setTemplateId(site1.getTemplateName());
            site1Page2.setLinkTitle("testing page2");
            pageDao.save(site1Page2);

            // Create and save the page under the root itself
            site1Page1 = new PSPage();
            site1Page1.setName("site1page1");
            site1Page1.setTitle("test new page title");
            site1Page1.setFolderPath(site1.getFolderPath());
            site1Page1.setTemplateId(site1.getTemplateName());
            site1Page1.setLinkTitle("testing");
            pageDao.save(site1Page1);

            // create the copy of the site1
            var site2Name = "site2" + System.currentTimeMillis();
            site2 = siteDao.createSiteWithContent(site1Id, site2Name);

            var contentWs = PSContentWsLocator.getContentWebservice();
            assertTrue(contentWs.isChildExistInFolder(site2.getFolderPath() + "/" + "sitefolder1", "site1page2"));
            assertTrue(contentWs.isChildExistInFolder(site2.getFolderPath(), "site1page1"));

        } finally {
            // If any of these Delete operations fail then there is a bug in delete site
            // that needs to be fixed.
            if (origSite != null) {
                siteDao.delete(origSite.getId());
            }

            if (copySite != null) {
                siteDao.delete(copySite.getId());
            }

            if (site1 != null) {
                siteDao.delete(site1.getId());
            }

            if (site2 != null) {
                siteDao.delete(site2.getId());
            }
        }
    }

    @Test
    public void testFindByPath() throws Exception {
        PSSecurityWsLocator.getSecurityWebservice().login(request, response, "admin1", "demo", null,
                "Enterprise_Investments_Admin", null);

        PSSiteSummary testingSite3 = null;
        PSSiteSummary testingSite4 = null;
        var siteDao = (IPSiteDao) getBean("siteDao");
        try {
            // shouldn't be any sites
            assertNull(siteDao.findByPath("//Sites/foo/fooPage"));

            // create two sites
            var ts3 = siteDao.save(createSite("testingsite3", "testingsite3"));
            testingSite3 = siteDao.findSummary(ts3.getId());
            assertNotNull(testingSite3);
            var ts4 = siteDao.save(createSite("testingsite4", "testingsite4"));
            testingSite4 = siteDao.findSummary(ts4.getId());
            assertNotNull(testingSite4);

            // find paths
            var ts3Home = getSiteContentDao().getHomePage(testingSite3);
            assertEquals(testingSite3, siteDao.findByPath(ts3Home.getFolderPath()));
            var ts4Home = getSiteContentDao().getHomePage(testingSite4);
            assertEquals(testingSite4, siteDao.findByPath(ts4Home.getFolderPath()));
            assertNull(siteDao.findByPath("//Sites/foo/fooPage"));
        } finally {
            try {
                if (testingSite3 != null) {
                    siteDao.delete(testingSite3.getId());
                }

                if (testingSite4 != null) {
                    siteDao.delete(testingSite4.getId());
                }
            } catch (DeleteException e) {
            }
        }    
    } 
    
    private String getSiteDeliveryType(IPSSite psSite, IPSiteDao siteDao) throws PSNotFoundException {
        return siteDao.getSiteDeliveryType(psSite);
    }

    /**
     * Checks if required related items exist for the specified site.
     * 
     * @param siteName The name of the site, assumed not <code>null</code>.
     * @return <code>true</code> if the items exist, <code>false</code>
     * otherwise.
     * 
     * @throws Exception
     */
    private boolean hasRelatedItems(String siteName) throws Exception
    {
        IPSContentWs contentWs = PSContentWsLocator.getContentWebservice();

        String folderRoot = "//Sites/" + siteName;

        try
        {
            // Check for Site Folder
            contentWs.loadFolders(new String[]{folderRoot});
        }
        catch (PSErrorResultsException e)
        {
            return false;
        }

        // Check for NavTree, home page
        boolean foundNavTree = false;
        boolean foundHomePage = false;

        List<PSItemSummary> items = contentWs.findFolderChildren(folderRoot,
                false);
        for (PSItemSummary item : items)
        {
            String ctName = item.getContentTypeName();
            if (getManagedNavService().getNavTreeContentTypeNames().contains(ctName))
            {
                foundNavTree = true;
            }
            else if (item.getName().equals(PSSiteContentDao.HOME_PAGE_NAME))
            {
                foundHomePage = true;
            }
        }

        if (!foundNavTree || !foundHomePage)
        {
            return false;
        }

        return true;
    }

    /**
     * Checks if required related items exist for the specified site.
     * 
     * @param siteName The name of the site, assumed not <code>null</code>.
     * @return <code>true</code> if the items exist, <code>false</code>
     * otherwise.
     * 
     * @throws Exception
     */
    private boolean doesSiteFolderExist(String siteName) throws Exception
    {
        IPSContentWs contentWs = PSContentWsLocator.getContentWebservice();

        String folderRoot = "//Sites/" + siteName;

        try
        {
            // check for Site Folder
            contentWs.loadFolders(new String[]{folderRoot});
        }
        catch (PSErrorResultsException e)
        {
            return false;
        }

        return true;
    }

    private void assertPublishingItems(String siteName) throws Exception
    {
        assertPublishingItems(siteName, 4, 5);
    }
    
    private void assertPublishingItems(String siteName, int edtns, int clists) throws Exception
    {
        IPSPublisherService pubSvc = getPublisherService();

        IPSSite site = getSiteManager().loadSite(siteName);
        IPSGuid siteId = site.getGUID();
        List<IPSEdition> editions = pubSvc.findAllEditionsBySite(siteId);
        List<IPSContentList> cLists = pubSvc.findAllContentListsBySite(siteId);
        
        assertEquals("editions: " + editions.size(), edtns, editions.size());
        assertEquals("content lists: " + cLists.size(), clists, cLists.size());
    }

    /**
     * Determines if publishing info exists for the specified publishing job.
     * 
     * @param jobId
     * 
     * @return <code>true</code> if pub info exists, <code>false</code> otherwise.
     */
    private boolean hasPublishingInfo(long jobId)
    {
        IPSPublisherService pubSvc = getPublisherService();
        
        return pubSvc.findPubStatusForJob(jobId) != null;
    }
    
    /**
     * Creates a test site.
     * 
     * @param name the name of the site, assumed not <code>null</code>.
     * @param title the title of the home page of the site, assumed not
     * <code>null</code>.
     * 
     * @return the site, never <code>null</code>.
     * 
     * @throws Exception if an error occurs creating the template.
     */
    private PSSite createSite(String name, String title)
    throws Exception
    {
        PSSite site = new PSSite();
        site.setName(name);
        site.setHomePageTitle(title);
        site.setNavigationTitle(name);
        site.setDescription("This is " + title);
        site.setBaseTemplateName(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME);
        site.setTemplateName(name + "PageTemplate");

        return site;
    }
   
    private PSFolder createFolder(String folderName, String sitePath)throws Exception
    {
        IPSContentWs contentWs = PSContentWsLocator.getContentWebservice();
        PSFolder testFolder  = contentWs.addFolder(folderName, sitePath);
        contentWs.saveFolder(testFolder);
        return testFolder;
    }
    

    /**
     * Tests that a site was created properly.
     * 
     * @param site
     * @param checkTemplate <code>true</code> if the home page template should be verified, <code>false</code>
     * otherwise.
     * 
     * @throws Exception
     */
    private void assertSite(PSSite site, boolean checkTemplate) throws Exception
    {
        String siteName = site.getName();
        assertTrue(doesSiteFolderExist(siteName));
        assertTrue(hasRelatedItems(siteName));
        assertPublishingItems(siteName);

        if (checkTemplate)
        {
            // make sure template association was created
            List<PSTemplateSummary> templates = getSiteTemplateService().findTemplatesBySite(siteName);
            assertEquals(templates.size(), 1);

            PSTemplateSummary siteTemplate = null;
            List<PSTemplateSummary> userTemplates = getTemplateService().findAllUserTemplates();
            for (PSTemplateSummary userTemplate : userTemplates)
            {
                if (userTemplate.getName().equals(site.getTemplateName()))
                {
                    siteTemplate = userTemplate;
                    break;
                }
            }

            assertNotNull("Template was not created for site " + siteName, siteTemplate);
            assertEquals(templates.get(0).getName(), siteTemplate.getName());
        }
    }
    
    private IPSTemplateService getTemplateService()
    {
        return (IPSTemplateService) getBean("sys_templateService");
    }

    private IPSSiteTemplateService getSiteTemplateService()
    {
        return (IPSSiteTemplateService) getBean("siteTemplateService");
    }

    private IPSManagedNavService getManagedNavService()
    {
        return (IPSManagedNavService) getBean("sys_managedNavService");
    }
    
    private IPSRxPublisherService getRxPublisherService()
    {
        return (IPSRxPublisherService) getBean("sys_rxpublisherservice");
    }
    
    private IPSPublisherService getPublisherService()
    {
        return (IPSPublisherService) getBean("sys_publisherservice");
    }
    
    private IPSSiteManager getSiteManager()
    {
        return (IPSSiteManager) getBean("sys_sitemanager");
    }
    
    private PSSiteContentDao getSiteContentDao()
    {
        return (PSSiteContentDao) getBean("siteContentDao");
    }
    
    /**
     * Base name for test templates created for sites.
     */
    private static final String TEST_TEMPLATE_BASE = "TestTemplate";
}
