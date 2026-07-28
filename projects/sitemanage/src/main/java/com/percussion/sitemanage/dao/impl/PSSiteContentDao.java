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
package com.percussion.sitemanage.dao.impl;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.cms.objectstore.PSCloningOptions;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.search.PSSearchIndexEventQueue;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSGenericDao.DeleteException;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSUnknownContentTypeException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import java.util.Collections;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("siteContentDao")
@Lazy
public class PSSiteContentDao implements com.percussion.sitemanage.dao.IPSSiteContentDao {

  private IPSFolderHelper folderHelper;

  private IPSContentItemDao contentItemDao;

  private IPSTemplateService templateService;

  private IPSAssemblyService assemblyService;

  private IPSIdMapper idMapper;

  private IPSPageDao pageDao;

  private IPSPageDaoHelper pageDaoHelper;

  private IPSManagedNavService navService;

  private IPSRenderAssemblyBridge asmBridge;

  private IPSContentDesignWs contentDesignWs;

  private IPSContentWs contentWs;

  @Autowired private IPSRecycleService recyclerService;

  @Autowired
  public PSSiteContentDao(
      IPSAssemblyService assemblyService,
      IPSContentItemDao contentItemDao,
      IPSFolderHelper folderHelper,
      IPSIdMapper idMapper,
      IPSPageDao pageDao,
      IPSPageDaoHelper pageDaoHelper,
      IPSTemplateService templateService,
      IPSManagedNavService navService,
      IPSRenderAssemblyBridge asmBridge,
      IPSContentDesignWs contentDesignWs,
      IPSContentWs contentWs) {
    super();
    this.assemblyService = assemblyService;
    this.contentItemDao = contentItemDao;
    this.folderHelper = folderHelper;
    this.idMapper = idMapper;
    this.pageDao = pageDao;
    this.pageDaoHelper = pageDaoHelper;
    this.templateService = templateService;
    this.navService = navService;
    this.asmBridge = asmBridge;
    this.contentDesignWs = contentDesignWs;
    this.contentWs = contentWs;
  }

  /**
   * Creates the related content items required for the specified site.
   *
   * @param site The site model for which a home page will be created, may not be <code>null</code>.
   * @throws PSErrorException If an error occurs.
   */
  @Override
  public void createRelatedItems(PSSite site) {
    if (site == null) {
      throw new IllegalArgumentException("site may not be null");
    }
    var folderRoot = site.getFolderPath();
    try {
      folderHelper.createFolder(folderRoot, PSFolderPermission.Access.WRITE);
      var navSummary = navService.findNavSummary(folderRoot);
      // If navSummary exists, check for homepage and return if already present
      if (navSummary != null) {
        var homepage = this.getHomePage(site);
        if (homepage != null && !recyclerService.isInRecycler(homepage.getId())) {
          return;
        } else {
          PSTemplateSummary templateSummary = null;
          try {
            var baseTemplate = assemblyService.findTemplateByName(site.getTemplateName());
            var tempId =
                templateService.findUserTemplateIdByName(site.getTemplateName(), site.getName());
            templateSummary = templateService.find(idMapper.getString(tempId));
          } catch (Exception e) {
            // If no template found, will create new one below
          }
          createHomePageAndTemplate(
              site, folderRoot, idMapper.getGuid(navSummary.getCurrentLocator()), templateSummary);
          return;
        }
      }
      // Else create NavTree
      var navtreeId =
          navService.addNavTreeToFolder(
              folderRoot,
              site.getName() + "-NavTree",
              site.getNavigationTitle(),
              pageDaoHelper.getWorkflowIdForPath(folderRoot));
      createHomePageAndTemplate(site, folderRoot, navtreeId, null);
    } catch (Exception e) {
      // Log nested cause — outer Spring tx often only surfaces UnexpectedRollbackException
      log.error("Error creating site items for site={}: {}", site.getName(), e.toString(), e);
      throw new RuntimeException("Error creating site items", e);
    }
  }

  private void createHomePageAndTemplate(
      PSSite site, String folderRoot, IPSGuid navtreeId, PSTemplateSummary templateSummary) {
    try {
      if (templateSummary == null) {
        templateSummary = createSiteTemplate(site);
      }
      var page = new PSPage();
      page.setName(HOME_PAGE_NAME);
      page.setFolderPath(folderRoot);
      page.setTitle(site.getHomePageTitle());
      page.setTemplateId(templateSummary.getId());
      page.setLinkTitle(site.getHomePageTitle());
      pageDaoHelper.setWorkflowAccordingToParentFolder(page);
      var pageId = pageDao.save(page).getId();
      var pageGuid = idMapper.getGuid(pageId);
      var status = contentWs.prepareForEdit(navtreeId);
      navService.addLandingPageToNavnode(pageGuid, navtreeId, asmBridge.getDispatchTemplate());
      contentWs.releaseFromEdit(status, false);
      contentWs.checkinItems(Collections.singletonList(pageGuid), null);
      folderHelper.addItem(folderRoot, pageId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Copies all folder content from an existing site, including sections, folders, and pages.
   *
   * @param srcSite The source site, may not be <code>null</code>.
   * @param destSite The destination site, may not be <code>null</code>.
   */
  @Override
  public void copy(PSSite srcSite, PSSite destSite) {
    if (srcSite == null) {
      throw new IllegalArgumentException("srcSite may not be null");
    }
    if (destSite == null) {
      throw new IllegalArgumentException("destSite may not be null");
    }
    var srcName = srcSite.getName();
    var destName = destSite.getName();
    var fp = PSServerFolderProcessor.getInstance();
    try {
      var srcSum = folderHelper.findFolder(srcSite.getFolderPath());
      var srcLoc = idMapper.getLocator(srcSum.getId());
      var tgtSum =
          folderHelper.findFolder(PSPathUtils.getFolderPath(PSPathUtils.SITES_FINDER_ROOT));
      var tgtLoc = idMapper.getLocator(tgtSum.getId());
      var options =
          new PSCloningOptions(
              PSCloningOptions.TYPE_SITE,
              srcName,
              destName,
              destName,
              PSCloningOptions.COPY_ALL_CONTENT,
              PSCloningOptions.COPYCONTENT_AS_NEW_COPY,
              null);
      options.setUseSrcItemWorkflow(true);
      fp.copyFolder(srcLoc, tgtLoc, options, true, asmBridge.getDispatchTemplate());
    } catch (Exception e) {
      throw new RuntimeException(
          "Error occurred during copy of content from site '"
              + srcName
              + "' to site '"
              + destName
              + "':",
          e);
    }
  }

  /**
   * Creates the 1st template for the specified site.
   *
   * @param site the created site, assumed not <code>null</code>.
   * @return the template summary, not <code>null</code>.
   * @throws PSAssemblyException if failed to find the base template specified in the site object.
   */
  private PSTemplateSummary createSiteTemplate(PSSite site)
      throws PSAssemblyException, PSDataServiceException {
    var baseTemplate = assemblyService.findTemplateByName(site.getBaseTemplateName());
    PSTemplateSummary templateSummary = null;
    IPSGuid tempId = null;
    try {
      tempId = templateService.findUserTemplateIdByName(site.getTemplateName(), site.getName());
    } catch (PSValidationException | IPSDataService.DataServiceLoadException e) {
      // Template doesn't exist, will create below
    }
    if (tempId == null) {
      // siteMgr.findSite accepts site name; getId() may be null on a just-created site model
      var siteKey =
          (site.getId() != null && !site.getId().isBlank()) ? site.getId() : site.getName();
      templateSummary =
          templateService.createTemplate(
              site.getTemplateName(), idMapper.getString(baseTemplate.getGUID()), siteKey);
    } else {
      templateSummary = templateService.find(tempId.toString());
    }
    return templateSummary;
  }

  /**
   * Creates a new content item.
   *
   * @param contentType The type of content item, may not be blank.
   * @param fields The map of field names to values for the item. Never <code>null</code>.
   * @param path The path of the folder to which the item will be added.
   * @return The id of the newly created item.
   * @throws PSUnknownContentTypeException If the content type does not exist.
   * @throws PSErrorException If error occurs creating the item.
   */
  protected String createItem(String contentType, Map<String, Object> fields, String path)
      throws PSUnknownContentTypeException, PSErrorException, PSDataServiceException {
    notEmpty(contentType, "contentType");
    notNull(fields, "fields");
    notEmpty(path, "path");
    var item = new PSContentItem();
    item.setFields(fields);
    item.setType(contentType);
    item.setFolderPaths(asList(path));
    item = contentItemDao.save(item);
    return item.getId();
  }

  /**
   * Deletes all related content items associated with the specified site.
   *
   * @param site The site, may not be <code>null</code>.
   */
  public void deleteRelatedItems(PSSiteSummary site) throws DeleteException {
    notNull(site, "site");
    var indexer = PSSearchIndexEventQueue.getInstance();
    indexer.pause();
    try {
      log.info("Deleting items for site in {}", site.getFolderPath());
      deleteFolder(site.getFolderPath());
    } catch (Exception e) {
      log.error(
          "Error deleting site related items from folder: {}, Error: {}",
          site.getFolderPath(),
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new DeleteException(
          "Failed to delete site folder: " + site.getFolderPath() + " while deleting site", e);
    } finally {
      indexer.resume();
    }
  }

  /**
   * Get the homepage for the specified site.
   *
   * @param site the summary describing the site, may not be <code>null</code>.
   * @return the homepage page item, or <code>null</code> if one could not be found.
   */
  @Override
  public PSPage getHomePage(PSSiteSummary site) throws PSNavException, PSDataServiceException {
    notNull(site, "site");
    PSPage homePage = null;
    var navTree = getNavTree(site);
    if (navTree != null) {
      var pageId = navService.getLandingPageFromNavnode(idMapper.getGuid(navTree.getId()));
      if (pageId != null) {
        pageId = contentDesignWs.getItemGuid(pageId);
        homePage = pageDao.find(idMapper.getString(pageId));
      }
    }
    return homePage;
  }

  /**
   * Gets the navigation title associated with the specified site.
   *
   * @param siteSummary describing the site, may not be <code>null</code>.
   * @return the nav title, never <code>null</code>, may be empty.
   */
  @Override
  public String getNavTitle(PSSiteSummary siteSummary)
      throws PSNavException, PSDataServiceException {
    notNull(siteSummary, "siteSummary");
    var navTree = getNavTree(siteSummary);
    if (navTree != null) {
      var navTitle = navTree.getFields().get("displaytitle");
      return navTitle != null ? navTitle.toString() : "";
    }
    return "";
  }

  /**
   * Gets the navigation tree for the specified site.
   *
   * @param siteSummary describing the site, assumed not <code>null</code>.
   * @return the nav tree content item, may be <code>null</code> if not found.
   * @throws Exception if an error occurs finding the item.
   */
  private PSContentItem getNavTree(PSSiteSummary siteSummary)
      throws PSNavException, PSDataServiceException {
    PSContentItem navTree = null;
    var navSummary = navService.findNavSummary(siteSummary.getFolderPath());
    if (navSummary != null) {
      var id = new PSLegacyGuid(navSummary.getCurrentLocator());
      navTree = contentItemDao.find(idMapper.getString(id));
    }
    return navTree;
  }

  @Override
  public void loadTemplateInfo(PSSite site) throws PSDataServiceException {
    if (site.getBaseTemplateName() != null) {
      var tempSummary = templateService.find(site.getBaseTemplateName());
      site.setBaseTemplateName(tempSummary.getSourceTemplateName());
      site.setTemplateName(tempSummary.getName());
    } else {
      log.warn("Site: {}, does not have a base template.", site.getName());
    }
  }

  /**
   * Recursively deletes the folder specified by the given path.
   *
   * @param folderPath the path of the site folder, assumed not blank.
   * @throws Exception if an error occurs deleting the folder
   */
  private void deleteFolder(String folderPath) throws Exception {
    deleteFolder(folderPath, DELETE_FOLDER_RETRY_COUNT);
  }

  /**
   * Recursively deletes the folder specified by the given path. Delete happens while other items
   * may be locked. Retry is added to make sure that items that fail to be deleted because of action
   * in another thread are cleaned up fully.
   *
   * @param folderPath the path of the site folder, assumed not blank.
   * @param retryCount the number of times to try, assumed not blank.
   * @throws Exception if an error occurs deleting the folder
   */
  private void deleteFolder(String folderPath, int retryCount) throws Exception {
    // Not implemented: see comments in original code.
  }

  private static final int DELETE_FOLDER_RETRY_COUNT = 2;

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSSiteContentDao.class);
}
