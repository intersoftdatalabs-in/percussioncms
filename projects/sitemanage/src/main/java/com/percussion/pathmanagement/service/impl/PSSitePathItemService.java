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
package com.percussion.pathmanagement.service.impl;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.i18n.ui.PSI18NTranslationKeyValues;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.data.PSRenameFolderItem;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.dao.PSFolderPathUtils;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.ui.service.IPSListViewHelper;
import com.percussion.user.service.IPSUserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Path item service for site folders and navigation. */
@Component("sitePathItemService")
@Lazy
public class PSSitePathItemService extends PSPathItemService {

  private final IPSSiteDataService siteDataService;
  private final IPSPageService pageService;
  private final IPSManagedNavService navService;
  private Pattern sitePathPattern = Pattern.compile("^/([^/]*?)(/.*)$");
  private String navTreeType = null;
  private String navonType = null;
  private List<String> filteredItemNames = null;

  @Autowired
  public PSSitePathItemService(
      IPSSiteDataService siteDataService,
      IPSFolderHelper folderHelper,
      IPSIdMapper idMapper,
      IPSManagedNavService navService,
      IPSPageService pageService,
      IPSItemWorkflowService itemWorkflowService,
      IPSAssetService assetService,
      IPSWidgetAssetRelationshipService widgetAssetRelationshipService,
      IPSContentMgr contentMgr,
      IPSWorkflowService workflowService,
      @Qualifier("cm1ListViewHelper") IPSListViewHelper listViewHelper,
      IPSUserService userService) {
    super(
        folderHelper,
        idMapper,
        itemWorkflowService,
        assetService,
        widgetAssetRelationshipService,
        contentMgr,
        workflowService,
        pageService,
        listViewHelper,
        userService);
    this.siteDataService = siteDataService;
    this.navService = navService;
    this.pageService = pageService;
    this.setRootName("Sites");
  }

  @Override
  protected PSPathItem findItem(String path)
      throws PSPathNotFoundServiceException,
          IPSDataService.DataServiceNotFoundException,
          PSValidationException,
          DataServiceLoadException {
    var sfp = getSiteIdAndFolderPath(path);
    PSSiteSummary site;
    try {
      site = siteDataService.find(sfp.getSiteId());
    } catch (DataServiceLoadException | PSValidationException | IPSGenericDao.LoadException e) {
      try {
        site = siteDataService.findByPath(("/Sites/" + path).replace("//", "/"));
      } catch (IPSDataService.DataServiceNotFoundException | PSValidationException e1) {
        // Site not found, assume orphaned path (messages from TMX; EN uses curly apostrophes)
        var msg =
            sfp.isOnlySiteId()
                ? PSI18NTranslationKeyValues.getInstance()
                        .getTranslationValue(
                            "perc.ui.pathmanagement@Oops.  We can't find the site ")
                    + sfp.getSiteId()
                    + PSI18NTranslationKeyValues.getInstance()
                        .getTranslationValue("perc.ui.pathmanagement@.  It may have been deleted.")
                : PSI18NTranslationKeyValues.getInstance()
                    .getTranslationValue(
                        "perc.ui.pathmanagement@Oops. We're sorry. The requested page is no longer available.");
        throw new PSPathNotFoundServiceException(msg);
      }
    }
    // Only the site id.
    if (sfp.isOnlySiteId()) {
      var item = createPathItem();
      convert(site, item);
      return item;
    }
    return super.findItem(path);
  }

  protected void convert(PSSiteSummary site, PSPathItem item) {
    super.convert(site, item);
    item.setId(site.getId());
    item.setPath("/" + site.getId() + "/");
    item.setFolderPath(site.getFolderPath());
    item.setName(site.getName());
    item.setLeaf(false);
    item.setType(PSDataItemSummary.TYPE_SITE);
  }

  protected SiteIdAndFolderPath getSiteIdAndFolderPath(String path)
      throws PSPathNotFoundServiceException {
    PSPathUtils.validatePath(path);
    var matcher = sitePathPattern.matcher(path);
    if (matcher.find()) {
      var siteId = matcher.group(1).trim();
      var preFp = matcher.group(2).trim();
      var folderPath = preFp;
      var sfp = new SiteIdAndFolderPath(siteId, folderPath);
      sfp.onlySiteId = "/".equals(preFp);
      return sfp;
    }
    throw new PSPathNotFoundServiceException(
        "Could not extract site id or folder path from: " + path);
  }

  protected List<PSPathItem> findRootChildren() {
    var sites = siteDataService.findAll();
    var items = new ArrayList<PSPathItem>();
    for (var site : sites) {
      var item = createPathItem();
      convert(site, item);
      items.add(item);
    }
    return items;
  }

  @Override
  protected List<PSPathItem> findItems(String path)
      throws IPSDataService.DataServiceNotFoundException,
          PSPathNotFoundServiceException,
          PSValidationException {
    if ("/".equals(path)) {
      return findRootChildren();
    }
    return super.findItems(path);
  }

  @Override
  protected String getFullFolderPath(String path)
      throws IPSDataService.DataServiceNotFoundException,
          PSPathNotFoundServiceException,
          PSValidationException {
    notEmpty(path, "path");
    var fullFolderPath = SITE_ROOT;
    if (!"/".equals(path)) {
      var sfp = getSiteIdAndFolderPath(path);
      var site = siteDataService.findByPath(SITE_ROOT + path);
      fullFolderPath = sfp.getFullFolderPath(site.getFolderPath());
    }
    return fullFolderPath;
  }

  @Override
  public PSPathItem addNewFolder(String path)
      throws PSPathServiceException,
          IPSDataService.DataServiceNotFoundException,
          PSValidationException,
          DataServiceLoadException {
    PSPathUtils.validatePath(path);
    if ("/".equals(path)) {
      throw new PSPathServiceException("New folders may not be added as sites");
    }
    return super.addNewFolder(path);
  }

  @Override
  public PSPathItem renameFolder(PSRenameFolderItem item)
      throws PSValidationException,
          PSPathServiceException,
          IPSDataService.DataServiceNotFoundException,
          DataServiceLoadException {
    var path = item.getPath();
    if (getSiteIdAndFolderPath(path).isOnlySiteId()) {
      throw new PSPathServiceException("Site folders may not be renamed");
    }
    return super.renameFolder(item);
  }

  @Override
  public int deleteFolder(PSDeleteFolderCriteria criteria)
      throws PSPathServiceException,
          IPSDataService.DataServiceNotFoundException,
          PSValidationException,
          DataServiceLoadException,
          PSNotFoundException {
    var path = criteria.getPath();
    if (getSiteIdAndFolderPath(path).isOnlySiteId()) {
      throw new PSPathServiceException("Site folders may not be deleted");
    }
    var folder = findItem(path);
    if (folder.getCategory() == IPSItemSummary.Category.SECTION_FOLDER) {
      // This is a section folder, so delete the navon now because it's a filtered type
      var folderPath = folder.getFolderPath();
      try {
        // purgeItem is false so item is recycled.
        folderHelper.removeItem(
            folderPath,
            idMapper.getString(navService.findNavigationIdFromFolder(folderPath)),
            false);
      } catch (IllegalArgumentException e) {
        throw new PSPathServiceException(e.getMessage());
      } catch (Exception e) {
        throw new PSPathServiceException("Failed to delete navon from section: " + path, e);
      }
    }
    return super.deleteFolder(criteria);
  }

  @Override
  protected String getInUsePagesResult() {
    return PAGES_IN_USE_PAGES;
  }

  @Override
  protected String getNotAuthorizedResult() {
    return PAGES_NOT_AUTHORIZED;
  }

  @Override
  protected String getInUseTemplatesResult() {
    return PAGES_IN_USE_TEMPLATES;
  }

  @Override
  protected boolean shouldFilterItem(IPSItemSummary item) {
    return item == null
        || getFilteredItemTypes().contains(item.getType())
        || getFilteredItemNames().contains(item.getName())
        || item.getCategory().equals(IPSItemSummary.Category.EXTERNAL_SECTION_FOLDER);
  }

  @Override
  protected void removeItem(String fullFolderPath, PSPathItem item, boolean purgeItem)
      throws Exception {
    notEmpty(fullFolderPath);
    notNull(item);
    if (isPage(item)) {
      pageService.delete(item.getId(), true, purgeItem);
    } else {
      super.removeItem(fullFolderPath, item, purgeItem);
    }
  }

  @Override
  protected Set<String> getApprovedPages(PSPathItem item)
      throws PSValidationException, PSNotFoundException {
    notNull(item);
    return itemWorkflowService.getApprovedPages(
        item.getId(), PSFolderPathUtils.parentPath(item.getFolderPath()));
  }

  @Override
  protected String getFolderRoot() {
    return SITE_ROOT;
  }

  /**
   * Used to determine items to be filtered by type.
   *
   * @return list of item types that should not be displayed. Never null.
   */
  private List<String> getFilteredItemTypes() {
    var types = new ArrayList<String>();
    types.addAll(navService.getNavonContentTypeNames());
    types.addAll(navService.getNavTreeContentTypeNames());
    return types;
  }

  /**
   * Used to determine items to be filtered by name.
   *
   * @return list of item names that should not be displayed. Never null.
   */
  private List<String> getFilteredItemNames() {
    if (filteredItemNames == null) {
      filteredItemNames = new ArrayList<>();
      filteredItemNames.add(".system");
    }
    return filteredItemNames;
  }

  private PSSiteSummary getSite(String id)
      throws PSPathNotFoundServiceException,
          DataServiceLoadException,
          PSValidationException,
          IPSGenericDao.LoadException {
    var site = siteDataService.find(id);
    if (log.isDebugEnabled()) {
      log.debug("Loaded site: {}", site);
    }
    if (site == null) throw new PSPathNotFoundServiceException("Site could not be found for id");
    return site;
  }

  public static class SiteIdAndFolderPath {
    private final String siteId;
    private final String folderPath;
    private boolean onlySiteId = false;

    public SiteIdAndFolderPath(String siteId, String sitePath) {
      this.siteId = siteId;
      this.folderPath = sitePath;
    }

    public String getSiteId() {
      return siteId;
    }

    public boolean isOnlySiteId() {
      return onlySiteId;
    }

    public String getFullFolderPath(String siteFolderPath) {
      if (siteFolderPath == null)
        throw new IllegalArgumentException("site folder path cannot be null");
      return siteFolderPath + folderPath;
    }
  }

  /** Constant for the site root folder path. */
  public static final String SITE_ROOT_SUB = "/Sites";

  public static final String SITE_ROOT = "/" + SITE_ROOT_SUB;

  /** Constant for the response given when a folder contains in use pages. */
  public static final String PAGES_IN_USE_PAGES = "PagesInUsePages";

  /** Constant for the response given when a user is not authorized to remove pages in a folder. */
  public static final String PAGES_NOT_AUTHORIZED = "PagesNotAuthorized";

  /** Constant for the response given when a folder contains pages linked by templates. */
  public static final String PAGES_IN_USE_TEMPLATES = "PagesInUseTemplates";

  /** The log instance to use for this class, never null. */
  private static final Logger log = LogManager.getLogger(PSSitePathItemService.class);
}
