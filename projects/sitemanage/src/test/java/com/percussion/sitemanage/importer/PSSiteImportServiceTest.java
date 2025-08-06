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
package com.percussion.sitemanage.importer;

import static com.percussion.sitemanage.importer.helpers.PSHelperTestUtils.USER_AGENT;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.pagemanagement.service.impl.PSPageCatalogService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.PSFolderPathUtils;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.helpers.impl.PSImportHelper;
import com.percussion.sitemanage.importer.helpers.impl.PSSiteCreationHelper;
import com.percussion.sitemanage.service.IPSSiteImportService;

import com.percussion.webservices.security.IPSSecurityWs;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Properties;

/**
 * Tests for site import service and helpers.
 * @author LucasPiccoli, Sunny Sal (refactored)
 */
@Tag("IntegrationTest")
class PSSiteImportServiceTest extends PSSiteImportTestBase {

    private PSSiteDataServletTestCaseFixture fixture;
    private PSSiteCreationHelper siteCreationHelper;
    private PSSite importedSite;
    private IPSSecurityWs securityWs;
    private PSSiteImportService siteImportService;
    private IPSiteDao siteDao;
    private IPSPageService pageService;
    private IPSPageDao pageDao;
    private IPSSiteImportService templateImportService;
    private IPSSiteImportService pageImportService;
    private IPSPageCatalogService pageCatalogService;
    private IPSSystemProperties systemProperties;
    private IPSFolderHelper folderHelper;
    private boolean siteCreated = false;
    private boolean fixtureCreated = false;
    private final String CATALOGED_PAGE_NAME = "CatalogedPage";
    private final String CATALOGED_PAGE_FOLDER = "/folder";
    private final String CATALOGED_PAGE_URL = "http://samples.percussion.com/products/index.html";
    private static final String IMPORT_SITE_URL = "http://samples.percussion.com/products/index.html";

    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        setSecurityWs((IPSSecurityWs) getBean("sys_securityWs"));
        setSiteImportService((PSSiteImportService) getBean("siteImportService"));
        setSiteDao((IPSiteDao) getBean("siteDao"));
        setPageService((IPSPageService) getBean("pageService"));
        setPageDao((IPSPageDao) getBean("pageDao"));
        setTemplateImportService((IPSSiteImportService) getBean("templateImportService"));
        setPageImportService((IPSSiteImportService) getBean("pageImportService"));
        setPageCatalogService((IPSPageCatalogService) getBean("pageCatalogService"));
        setFolderHelper((IPSFolderHelper) getBean("folderHelper"));
        securityWs.login("Admin", "demo", "Default", null);
        initData();
        fixture = new PSSiteDataServletTestCaseFixture(request, response);
        fixture.setUp();
        fixtureCreated = true;
    }

    /**
     * Placeholder test to keep JUnit happy, all other tests ignored as tech debt.
     */
    @Test
    void testNothing() {
        // No-op
    }

    /**
     * Tests if the service can gather all the helpers from the context.
     */
    @Disabled
    public void ignore_testHelperAvailability()
    {
        assertNotNull(siteImportService);
        List<PSImportHelper> mandatoryHelpers = siteImportService.getMandatoryHelpers();
        assertNotNull(mandatoryHelpers);
        assertTrue(!mandatoryHelpers.isEmpty());
        List<PSImportHelper> optionalHelpers = siteImportService.getOptionalHelpers();
        assertNotNull(optionalHelpers);
        assertTrue(!optionalHelpers.isEmpty());
    }

    /**
     * Tests if the whole process of importing a site is successful.
     */
    @Disabled
    public void ignore_testImportSite()
    {
        importedSite = new PSSite();
        importedSite.setName("Test");
        importedSite.setBaseUrl(IMPORT_SITE_URL);
        try
        {
            importedSite = siteImportService.importSiteFromUrl(importedSite, USER_AGENT).getSite();
            siteCreated = true;
        }
        catch (RuntimeException | PSSiteImportException e)
        {
            fail();
        }
    }

    /**
     * Test if the correct helpers configured for templateImportService bean are
     * executed correctly and without errors.
     * @throws Exception 
     * 
     */
    @Disabled
    public void ignore_testImportTemplateFromUrl() throws Exception
    {
        fixture.site1.setBaseUrl(IMPORT_SITE_URL);
        try
        {
            templateImportService.importSiteFromUrl((PSSite) fixture.site1, USER_AGENT);
        }
        catch (RuntimeException e)
        {
            fail();
        }
    }
    
    @Disabled
    public void ignore_testImportPage() throws Exception
    {
        PSPage page1 = addCatalogedPage();
        
        fixture.site1.setBaseUrl(IMPORT_SITE_URL);
        try
        {
            PSSiteImportCtx context = new PSSiteImportCtx();
            pageImportService.importCatalogedPage((PSSite) fixture.site1, page1.getId(), USER_AGENT, context);
            
            checkCreateImportedPage(page1);
        }
        catch (Exception e)
        {
            fail();
        }
    }

    /**
     * Test hardness to import one or more specified web site.
     * @throws Exception
     */
    public void runme_importSite() throws Exception
    {
        importSite("samples.percussion.com." + System.currentTimeMillis(), "samples.percussion.com");
    }
    
    /**
     * Test hardness to create a site and import the cataloged pages identified from the home page.
     */
    private void importSite(String name, String url) throws Exception
    {
        importedSite = new PSSite();
        importedSite.setName(name);
        importedSite.setBaseUrl(url);
        try
        {
            importedSite = siteImportService.importSiteFromUrl(importedSite, USER_AGENT).getSite();
            siteCreated = true;
        }
        catch (RuntimeException e)
        {
            fail();
        }
        
        importCatagedPagesForSite(importedSite);
    }

    private void importCatagedPagesForSite(PSSite site) throws Exception
    {
        List<String> pageIds = pageCatalogService.findCatalogPages(site.getName());
        for (String id : pageIds)
        {
            importPage(site, id);
        }
    }
    
    private void importPage(PSSite site, String pageId)
    {
        try
        {
            PSSiteImportCtx context = new PSSiteImportCtx();
            pageImportService.importCatalogedPage(site, pageId, USER_AGENT, context);
        }
        catch (Exception e)
        {
            fail();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (siteCreated) {
            deleteSite();
            siteCreated = false;
        }
        if (fixtureCreated) {
            fixture.tearDown();
            fixtureCreated = false;
        }
        ((PSPageCatalogService) pageCatalogService).setSystemProps(systemProperties);
        super.tearDown();
    }

    private void initData() throws Exception {
        createHelpers();
    }

    private void deleteSite() {
        if (importedSite != null) {
            var importCtx = new PSSiteImportCtx();
            importCtx.setSite(importedSite);
            siteCreationHelper.rollback(null, importCtx);
            importedSite = null;
        }
    }

    private void createHelpers() {
        siteCreationHelper = new PSSiteCreationHelper(siteDao, pageService);
    }

    private PSPage addCatalogedPage() throws Exception {
        increaseCatalogLimit(1);
        var catalogedPage = pageCatalogService.addCatalogPage(
                fixture.site1.getName(),
                CATALOGED_PAGE_NAME,
                CATALOGED_PAGE_NAME,
                PSFolderPathUtils.concatPath(CATALOGED_PAGE_FOLDER, CATALOGED_PAGE_NAME),
                CATALOGED_PAGE_URL
        );
        increaseCatalogLimit(0);
        if (catalogedPage != null)
            fixture.pageCatalogCleaner.add(catalogedPage.getId());
        return catalogedPage;
    }

    private void increaseCatalogLimit(int max) {
        var svcImpl = (PSPageCatalogService) pageCatalogService;
        var props = new PSMockSystemProps();
        props.setMax(String.valueOf(max));
        svcImpl.setSystemProps(props);
    }

    private void checkCreateImportedPage(PSPage page) throws Exception {
        var pageId = page.getId();
        var expectedFolderPath = "//Sites/PSSiteDataServletTestCaseFixtureSite/folder/CatalogedPage";
        var expectedPath = PSFolderPathUtils.concatPath(expectedFolderPath, page.getName());
        var item = folderHelper.findItemById(pageId);
        assertNotNull(item);
    }

    private static class PSMockSystemProps extends Properties implements IPSSystemProperties {
        public void setMax(String value) {
            setProperty(CATALOG_PAGE_MAX, value);
        }
    }

    /**
     * @return the securityWs
     */
    public IPSSecurityWs getSecurityWs()
    {
        return securityWs;
    }

    /**
     * @param securityWs the securityWs to set
     */
    public void setSecurityWs(IPSSecurityWs securityWs)
    {
        this.securityWs = securityWs;
    }

    /**
     * @return the siteImportService
     */
    public PSSiteImportService getSiteImportService()
    {
        return siteImportService;
    }

    /**
     * @param siteImportService the siteImportService to set
     */
    public void setSiteImportService(PSSiteImportService siteImportService)
    {
        this.siteImportService = siteImportService;
    }

    /**
     * @return the siteDao
     */
    public IPSiteDao getSiteDao()
    {
        return siteDao;
    }

    /**
     * @param siteDao the siteDao to set
     */
    public void setSiteDao(IPSiteDao siteDao)
    {
        this.siteDao = siteDao;
    }

    /**
     * @return the pageService
     */
    public IPSPageService getPageService()
    {
        return pageService;
    }

    /**
     * @param pageService the pageService to set
     */
    public void setPageService(IPSPageService pageService)
    {
        this.pageService = pageService;
    }

    /** 
     * @return the pageDao
     */
    public IPSPageDao getPageDao()
    {
        return this.pageDao;
    }
    
    /**
     * @param pageDao the pageDao to set
     */
    public void setPageDao(IPSPageDao pageDao)
    {
        this.pageDao = pageDao;
    }

    /**
     * @return the templateImportService
     */
    public IPSSiteImportService getTemplateImportService()
    {
        return templateImportService;
    }

    /**
     * @param templateImportService the templateImportService to set
     */
    public void setTemplateImportService(IPSSiteImportService templateImportService)
    {
        this.templateImportService = templateImportService;
    }
    
    /**
     * @return the pageImportService
     */
    public IPSSiteImportService getPageImportService()
    {
        return pageImportService;
    }
    
    /**
     * @param pageImportService the pageImportService to set
     */
    public void setPageImportService(IPSSiteImportService pageImportService)
    {
        this.pageImportService = pageImportService;
    }
    
    /**
     * @return the pageCatalogService
     */
    public IPSPageCatalogService getPageCatalogService()
    {
        return this.pageCatalogService;
    }
    
    /**
     * @param pageCatalogService the pageCatalogService to set
     */
    public void setPageCatalogService(IPSPageCatalogService pageCatalogService)
    {
        this.pageCatalogService = pageCatalogService;
        
        systemProperties = ((PSPageCatalogService) pageCatalogService).getSystemProps();
    }
    
    /**
     * @return the folderHelper
     */
    public IPSFolderHelper getFolderHelper()
    {
        return folderHelper;
    }

    /**
     * @param folderHelper the folderHelper to set
     */
    public void setFolderHelper(IPSFolderHelper folderHelper)
    {
        this.folderHelper = folderHelper;
    }
}
