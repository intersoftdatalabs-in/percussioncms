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
package com.percussion.pagemanagement.service.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.dao.IPSTemplateDao;
import com.percussion.pagemanagement.data.PSCatalogPageSummary;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplate.PSTemplateTypeEnum;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.pagemanagement.service.IPSPageService.PSPageException;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.IPSItemEntry;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.share.IPSSitemanageConstants;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.percussion.pathmanagement.service.impl.PSPathUtils.doesItemExist;
import static com.percussion.share.dao.PSFolderPathUtils.concatPath;
import static com.percussion.share.dao.PSFolderPathUtils.pathSeparator;
import static com.percussion.share.service.IPSSystemProperties.CATALOG_PAGE_MAX;
import static com.percussion.sitemanage.service.IPSSiteSectionMetaDataService.PAGE_CATALOG;
import static com.percussion.sitemanage.service.IPSSiteSectionMetaDataService.SECTION_SYSTEM_FOLDER_NAME;

/**
 * Service for managing catalog pages in Percussion CMS.
 * Sunny Sal says: "Catalog pages—because every site deserves a little organization!"
 */
@Component("pageCatalogService")
@Lazy
@Transactional(noRollbackFor = {Exception.class, PSPageException.class})
public class PSPageCatalogService implements IPSPageCatalogService {

    private final IPSFolderHelper folderHelper;
    private final IPSPageDao pageDao;
    private final IPSiteDao siteDao;
    private final IPSTemplateService templateService;
    private final IPSSiteTemplateService siteTemplateService;
    private final IPSItemWorkflowService itemWorkflowService;
    private final IPSTemplateDao templateDao;
    private final IPSIdMapper idMapper;
    private IPSSystemProperties systemProps;
    private final IPSPageDaoHelper pageDaoHelper;
    private final IPSManagedNavService navService;
    private final IPSCmsObjectMgr cmsMgr;

    private final PSSiteCache siteCache = new PSSiteCache();

    private Integer defaultWorkflowId = null;

    private static final String CATALOG_FOLDERS = pathSeparator() + concatPath(SECTION_SYSTEM_FOLDER_NAME, PAGE_CATALOG);
    private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);

    @Autowired
    public PSPageCatalogService(
            IPSFolderHelper folderHelper,
            IPSPageDao pageDao,
            IPSiteDao siteDao,
            IPSSiteTemplateService siteTemplateService,
            IPSItemWorkflowService itemWorkflowService,
            IPSTemplateDao templateDao,
            IPSTemplateService templateService,
            IPSIdMapper idMapper,
            IPSNotificationService notifications,
            IPSPageDaoHelper pageDaoHelper,
            IPSManagedNavService navService,
            IPSSiteManager siteManager,
            IPSCmsObjectMgr cmsMgr) {
        this.folderHelper = folderHelper;
        this.pageDao = pageDao;
        this.siteDao = siteDao;
        this.siteTemplateService = siteTemplateService;
        this.itemWorkflowService = itemWorkflowService;
        this.templateDao = templateDao;
        this.templateService = templateService;
        this.idMapper = idMapper;
        this.pageDaoHelper = pageDaoHelper;
        this.cmsMgr = cmsMgr;
        this.navService = navService;
        setNotifications(notifications);
    }

    @Override
    public List<String> findCatalogPages(String siteName) throws Exception {
        var site = siteDao.findSummary(siteName);
        if (site == null) {
            throw new RuntimeException("Unable to find cataloged pages, the specified site was not found: " + siteName);
        }
        var catalogRoot = getCatalogFolderPath(site);
        return folderHelper.findItemIdsByPath(catalogRoot);
    }

    @Override
    public PSCatalogPageSummary getCatalogPageSummary(String id) throws PSDataServiceException {
        var page = pageDao.find(id);
        if (page == null) {
            return null;
        }
        var sum = new PSCatalogPageSummary();
        sum.setId(page.getId());
        sum.setName(page.getLinkTitle());
        var fullPath = concatPath(page.getFolderPath(), page.getName());
        sum.setPath(getPath(fullPath));
        return sum;
    }

    @Override
    public PSPage addCatalogPage(String siteName, String pageName, String linkText, String folderPath, String href) throws Exception {
        Validate.notEmpty(siteName);
        Validate.notEmpty(pageName);
        Validate.notEmpty(linkText);
        Validate.notEmpty(folderPath);
        Validate.notEmpty(href);

        log.debug("Catalog page: {}, path: {}", pageName, folderPath);

        var site = siteDao.findSummary(siteName);
        if (site == null) {
            throw new RuntimeException("Unable to add cataloged page, the specified site was not found: " + siteName);
        }

        if (isMaxCountReached(site)) {
            return null;
        }

        var fullFolderPath = getFullFolderPath(folderPath, site);
        fullFolderPath = concatPath(getCatalogFolderPath(site), folderPath);

        var siteItemPath = concatPath(site.getFolderPath(), folderPath, pageName);
        var catItemPath = concatPath(fullFolderPath, pageName);

        // see if item exists and return null if found
        if (pageDao.findPageByPath(catItemPath) != null) return null;
        if (pageDao.findPageByPath(siteItemPath) != null) return null;

        if (pageWithFolderPathExists(fullFolderPath)) {
            log.warn("Skip catalog page: {}. Because a page has been created at: {}", catItemPath, fullFolderPath);
            return null;
        }

        return createPageStub(pageName, linkText, href, site, fullFolderPath, catItemPath);
    }

    private PSPage createPageStub(String pageName, String linkText, String href, PSSiteSummary site,
                                  String fullFolderPath, String catItemPath)
            throws IPSItemWorkflowService.PSItemWorkflowServiceException, PSDataServiceException, PSSiteImportException {
        var page = new PSPage();
        page.setName(pageName);
        page.setTitle(linkText);
        page.setLinkTitle(linkText);
        page.setTemplateId(getCatalogTemplateId(site));
        page.setFolderPath(fullFolderPath);
        page.setDescription(href);
        page.setWorkflowId(getDefaultWorkflowId());

        pageDao.save(page);
        incrementCatalogCount(site);
        itemWorkflowService.checkIn(page.getId());

        return pageDao.findPageByPath(catItemPath);
    }

    @Override
    public String getCatalogTemplateIdBySite(String siteName) throws PSDataServiceException, PSSiteImportException {
        var site = siteDao.findSummary(siteName);
        if (site == null) {
            throw new PSPageException("Unable to find the template id, the specified site was not found: " + siteName);
        }
        return getCatalogTemplateId(site);
    }

    @Override
    public void createImportedPage(String pageId) throws Exception {
        var page = pageDao.find(pageId);
        if (page == null) {
            throw new PSPageException("Unable to move the cataloged page, the specified page id was not found: " + pageId);
        }
        var fullPath = concatPath(page.getFolderPath(), page.getName());
        var newPageFolderPath = convertToImportedFolderPath(page.getFolderPath());
        if (!PSPathUtils.doesItemExist(newPageFolderPath)) {
            folderHelper.createFolder(newPageFolderPath);
        }
        folderHelper.moveItem(newPageFolderPath, fullPath, false);
    }

    @Override
    public List<String> findImportedPageIds(String siteName) throws Exception {
        var site = siteDao.findSummary(siteName);
        if (site == null) {
            log.warn("Unable to find cataloged pages, the specified site was not found: {}", siteName);
            return new ArrayList<>();
        }
        var filteredPageIds = new ArrayList<String>();
        try {
            var templateId = getCatalogTemplateIdBySite(siteName);
            var importedPageIds = folderHelper.findItemIdsByPath(site.getFolderPath());
            var catalogedPageIds = findCatalogPages(siteName);
            importedPageIds.removeAll(catalogedPageIds);

            var intPageIds = importedPageIds.stream()
                    .map(PSLegacyGuid::new)
                    .map(PSLegacyGuid::getContentId)
                    .toList();

            Collection<Integer> pageIds = pageDaoHelper.findImportedPageIdsByTemplate(templateId, intPageIds);

            for (var pageId : pageIds) {
                var guid = new PSLegacyGuid(pageId, -1);
                filteredPageIds.add(idMapper.getString(guid));
            }
            Collections.sort(filteredPageIds);
            return filteredPageIds;
        } catch (Exception e) {
            var msg = "An error occurred when getting cataloged pages for site: " + siteName;
            throw new PSPageException(msg, e);
        }
    }

    /**
     * Set the system properties on this service. This service will always use the values provided by
     * the most recently set instance of the properties.
     *
     * @param systemProps the system properties
     */
    @Autowired
    public void setSystemProps(IPSSystemProperties systemProps) {
        this.systemProps = systemProps;
    }

    public IPSSystemProperties getSystemProps() {
        return systemProps;
    }

    @Override
    public boolean doesImportedPageExist(PSPage page) {
        var targetPath = convertToImportedFolderPath(concatPath(page.getFolderPath(), page.getName()));
        return doesItemExist(targetPath);
    }

    /**
     * Convert the full folder path to the path the catalog page would have once imported.
     *
     * @param folderPath The path to convert, assumed not null or empty.
     * @return The path, not null, empty if no paths are found.
     */
    private String getPath(String folderPath) {
        return StringUtils.substringAfter(folderPath, CATALOG_FOLDERS);
    }

    /**
     * Get the full path to the catalog folder for the supplied site.
     * @param site The site, assumed not null.
     * @return The catalog folder path for the site.
     */
    private String getCatalogFolderPath(PSSiteSummary site) {
        return concatPath(site.getFolderPath(), CATALOG_FOLDERS);
    }

    private Integer getDefaultWorkflowId() {
        if (defaultWorkflowId == null) {
            IPSWorkflowService workflowService = PSWorkflowServiceLocator.getWorkflowService();
            defaultWorkflowId = workflowService.getDefaultWorkflowId().getUUID();
        }
        return defaultWorkflowId;
    }

    public String getCatalogTemplateId(PSSiteSummary site) throws PSDataServiceException, PSSiteImportException {
        var siteId = site.getId();
        var templateId = siteCache.getSiteTemplateId(site.getSiteId());
        if (templateId == null) {
            var templates = siteTemplateService.findTypedTemplatesBySite(siteId, PSTemplateTypeEnum.UNASSIGNED);
            for (var sum : templates) {
                if (sum.getName().equals(IPSSitemanageConstants.UNASSIGNED_TEMPLATE_NAME)) {
                    templateId = sum.getId();
                    break;
                }
            }
            if (templateId == null) {
                IPSAssemblyTemplate baseTemplate = templateDao.loadBaseTemplateByName(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME);
                var templateSummary = templateService.createTemplate(
                        IPSSitemanageConstants.UNASSIGNED_TEMPLATE_NAME,
                        idMapper.getString(baseTemplate.getGUID()),
                        siteId,
                        PSTemplateTypeEnum.UNASSIGNED);
                templateId = templateSummary.getId();
                setSiteThemeInTemplate(templateId, site);
            }
            siteCache.setSiteTemplateid(site.getSiteId(), templateId);
        }
        return templateId;
    }

    /**
     * Retrieves the theme name from the site's home template and sets it to the
     * unassigned template for that site.
     *
     * @param templateId {@link String} with the id of the unassigned template.
     * @param siteSummary {@link PSSiteSummary} of the site.
     */
    private void setSiteThemeInTemplate(String templateId, PSSiteSummary siteSummary) throws PSSiteImportException {
        try {
            var rootFolder = idMapper.getGuid(folderHelper.findFolder(siteSummary.getFolderPath()).getId());
            var rootNavNode = navService.findNavigationIdFromFolder(rootFolder);
            var homePageId = navService.getLandingPageFromNavnode(rootNavNode);
            var homePage = pageDao.find(homePageId.toString());
            var homeTemplateSummary = templateService.find(homePage.getTemplateId());
            var homeTemplate = templateService.load(homeTemplateSummary.getId());
            var theme = homeTemplate.getTheme();
            var targetTemplate = templateService.load(templateId);
            targetTemplate.setTheme(theme);
            templateService.save(targetTemplate);
        } catch (Exception e) {
            log.error("Failed to get template from the home page for site name = '{}'.  Error: {}",
                    siteSummary.getName(), PSExceptionUtils.getMessageForLog(e));
            throw new PSSiteImportException(e);
        }
    }

    /**
     * Register listener whenever a design object has been changed.
     *
     * @param notifyService the notification service to set, assumed not null.
     */
    private void setNotifications(IPSNotificationService notifyService) {
        notifyService.addListener(EventType.SITE_DELETED, new ChangeNotificationListener());
    }

    /**
     * Listener which invalidates locally cached information.
     */
    private final class ChangeNotificationListener implements IPSNotificationListener {
        @Override
        public void notifyEvent(PSNotificationEvent notification) {
            if (notification.getType() != EventType.SITE_DELETED) return;
            IPSGuid guid = (IPSGuid) notification.getTarget();
            siteCache.remove(guid.longValue());
        }
    }

    /**
     * Increment the catalog count for the specified site by 1.
     * @param site the site to increment the count for, assumed not null.
     * @return The new value.
     */
    private int incrementCatalogCount(PSSiteSummary site) {
        var count = siteCache.getCatalogCount(site.getSiteId());
        if (count == null) {
            throw new IllegalStateException("catalogCount for site has not been initialized: " + site.getName());
        }
        return count.incrementAndGet();
    }

    /**
     * Determine if the max number of pages have already been cataloged for the specified site.
     * @param site The site to check for, assumed not null.
     * @return true if it has, false if more pages can be cataloged.
     */
    private boolean isMaxCountReached(PSSiteSummary site) throws Exception {
        var maxVal = systemProps.getProperty(CATALOG_PAGE_MAX);
        int max = NumberUtils.toInt(maxVal, -1);
        int count = getCatalogCount(site);
        if (max < 0) return false;
        return count >= max;
    }

    /**
     * Get the current count of cataloged pages for the specified site.
     * @param site The site to check for, assumed not null.
     * @return The count.
     */
    private int getCatalogCount(PSSiteSummary site) throws Exception {
        var count = siteCache.getCatalogCount(site.getSiteId());
        if (count == null) {
            int catCount = findCatalogPages(site.getName()).size();
            count = new AtomicInteger(catCount);
            siteCache.setCatalogCount(site.getSiteId(), count);
        }
        return count.get();
    }

    public String getFullFolderPath(String folderPath, PSSiteSummary site) {
        var catalogRoot = getCatalogFolderPath(site);
        return concatPath(catalogRoot, folderPath);
    }

    /**
     * Check if any page already exists with exactly the given path.
     * @param fullFolderPath the folder path.
     * @return true if a page with the given folder path exists, false otherwise.
     */
    public boolean pageWithFolderPathExists(String fullFolderPath) {
        int contentId = PSPathUtils.getIdByPath(fullFolderPath);
        if (contentId == -1) return false;
        IPSItemEntry item = cmsMgr.findItemEntry(contentId);
        return (item != null && (!item.isFolder()));
    }

    private static class PSSiteCache {
        private final Map<Long, String> templateBySiteCache = new ConcurrentHashMap<>();
        private final Map<Long, AtomicInteger> catalogCountBySiteCache = new ConcurrentHashMap<>();

        public String getSiteTemplateId(Long siteId) {
            return templateBySiteCache.get(siteId);
        }

        public void remove(Long siteId) {
            templateBySiteCache.remove(siteId);
            catalogCountBySiteCache.remove(siteId);
        }

        public void setSiteTemplateid(Long siteId, String templateId) {
            templateBySiteCache.put(siteId, templateId);
        }

        public AtomicInteger getCatalogCount(Long siteId) {
            return catalogCountBySiteCache.get(siteId);
        }

        public void setCatalogCount(Long siteId, AtomicInteger count) {
            catalogCountBySiteCache.put(siteId, count);
        }
    }

    public String convertToImportedFolderPath(String catalogedPagePath) {
        return catalogedPagePath.replaceFirst(CATALOG_FOLDERS, "");
    }
}
