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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    this.rootName = "Sites";
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
                        "perc.ui.pathmanagement@Oops. We're sorry. The requested page is no longer"
                            + " available.");
        throw new PSPathNotFoundServiceException(msg);
      }
    }
    // Only the site id.
    if (sfp.isOnlySiteId()) {
      var item = createPathItem();
      convert(site, item);
      return item;
    }
    try {
      return super.findItem(path);
    } catch (PSPathNotFoundServiceException e) {
      var virtual = findVirtualSiteChromeItem(path, sfp);
      if (virtual != null) {
        return virtual;
      }
      throw e;
    }
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
    // REST /folder/Sites/<folderRoot> may omit the trailing slash
    // (PathItem.folderPath is //Sites/CorporateInvestments). validatePath
    // requires start+end '/'; treat /name as /name/ (site-only) (#3410).
    if (path != null) {
      var trimmed = path.trim();
      if (trimmed.startsWith("/") && trimmed.length() > 1 && trimmed.indexOf('/', 1) < 0) {
        path = trimmed + "/";
      }
    }
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
    List<PSPathItem> items = null;
    Exception failure = null;
    try {
      items = super.findItems(path);
    } catch (Exception e) {
      failure = e;
    }
    if (items != null && !items.isEmpty()) {
      return injectVirtualSiteChromeIfNeeded(path, items);
    }
    var recovered = recoverSiteFolderChildren(path);
    if (recovered != null) {
      return injectVirtualSiteChromeIfNeeded(path, recovered);
    }
    if (items != null) {
      return injectVirtualSiteChromeIfNeeded(path, items);
    }
    if (failure instanceof PSPathNotFoundServiceException notFound) {
      throw notFound;
    }
    if (failure instanceof PSValidationException validation) {
      throw validation;
    }
    if (failure instanceof IPSDataService.DataServiceNotFoundException missing) {
      throw missing;
    }
    if (failure != null) {
      throw new PSPathNotFoundServiceException("Path not found: " + path, failure);
    }
    return List.of();
  }

  /**
   * Sample sites use SITENAME {@code Corporate_Investments} and FOLDER_ROOT
   * {@code //Sites/CorporateInvestments}. pathToId on FOLDER_ROOT can fail
   * (404) even when folder 523 exists under {@code //Sites} (#3410 / #3326).
   *
   * <p>Nested {@code /Pages} (and missing {@code /Files}) is Explorer chrome
   * for CM1-style paths. FastForward sites have no Pages folder — empty
   * {@code folderHelper.findItems} must recover site-root children (#3457).
   */
  private List<PSPathItem> recoverSiteFolderChildren(String path) {
    try {
      var sfp = getSiteIdAndFolderPath(path);
      var siteFolder = findMatchingSiteFolder(sfp.getSiteId());
      if (siteFolder == null) {
        return null;
      }
      var siteFolderPath = folderHelper.concatPath(SITE_ROOT, siteFolder.getName());
      var remaining = trimFolderSegments(sfp.getRelativeFolderPath());
      if (remaining.isEmpty() || sfp.isOnlySiteId()) {
        var sums = folderHelper.findChildItems(siteFolder.getId());
        return toPathItems(ensureTrailingSlash(path), siteFolderPath, sums);
      }

      var current = siteFolder;
      var currentPath = siteFolderPath;
      var segs = remaining.split("/");
      for (var i = 0; i < segs.length; i++) {
        var seg = segs[i];
        if (seg.isEmpty()) {
          continue;
        }
        var last = i == segs.length - 1;
        var kids = folderHelper.findChildItems(current.getId());
        var next = findChildNamed(kids, seg);
        if (last && isPagesSegment(seg)) {
          if (next != null) {
            var existing =
                toPathItems(
                    "/" + sfp.getSiteId() + "/Pages/",
                    folderHelper.concatPath(currentPath, next.getName()),
                    folderHelper.findChildItems(next.getId()));
            if (hasNonFolderChild(existing)) {
              return existing;
            }
          }
          return listVirtualPagesChildren(sfp, siteFolder, siteFolderPath);
        }
        if (last && isFilesSegment(seg)) {
          if (next != null) {
            var existing =
                toPathItems(
                    "/" + sfp.getSiteId() + "/Files/",
                    folderHelper.concatPath(currentPath, next.getName()),
                    folderHelper.findChildItems(next.getId()));
            if (!existing.isEmpty()) {
              return existing;
            }
          }
          return listVirtualFilesChildren(sfp, siteFolder, siteFolderPath);
        }
        if (next != null) {
          current = next;
          currentPath = folderHelper.concatPath(currentPath, next.getName());
          continue;
        }
        return null;
      }
      var sums = folderHelper.findChildItems(current.getId());
      return toPathItems(
          "/" + sfp.getSiteId() + "/" + remaining + "/", currentPath, sums);
    } catch (Exception ex) {
      log.debug("Site folder recover failed for path {}", path, ex);
    }
    return null;
  }

  private IPSItemSummary findMatchingSiteFolder(String siteId) throws Exception {
    if (folderHelper == null || siteId == null) {
      return null;
    }
    var sitesKids = folderHelper.findItems(SITE_ROOT);
    for (var kid : sitesKids) {
      if (kid != null && siteFolderNameMatches(siteId, kid.getName())) {
        return kid;
      }
    }
    return null;
  }

  private static IPSItemSummary findChildNamed(List<IPSItemSummary> kids, String name) {
    if (kids == null || name == null) {
      return null;
    }
    for (var kid : kids) {
      if (kid != null && siteFolderNameMatches(name, kid.getName())) {
        return kid;
      }
    }
    return null;
  }

  private List<PSPathItem> listVirtualPagesChildren(
      SiteIdAndFolderPath sfp, IPSItemSummary siteFolder, String siteFolderPath)
      throws Exception {
    var rel = "/" + sfp.getSiteId() + "/Pages/";
    var sums = folderHelper.findChildItems(siteFolder.getId());
    var items = toPathItems(rel, siteFolderPath, sums);
    if (hasNonFolderChild(items)) {
      return items;
    }
    // FastForward pages often live in About… section folders, not the site root.
    var pages = new ArrayList<PSPathItem>();
    for (var child : sums) {
      if (shouldFilterItem(child) || child == null || !child.isFolder()) {
        continue;
      }
      var grand = folderHelper.findChildItems(child.getId());
      var childFolderPath = folderHelper.concatPath(siteFolderPath, child.getName());
      pages.addAll(toPathItems(rel, childFolderPath, grand));
    }
    return pages.isEmpty() ? items : pages;
  }

  private List<PSPathItem> listVirtualFilesChildren(
      SiteIdAndFolderPath sfp, IPSItemSummary siteFolder, String siteFolderPath)
      throws Exception {
    var files = findChildNamed(folderHelper.findChildItems(siteFolder.getId()), "Files");
    var rel = "/" + sfp.getSiteId() + "/Files/";
    if (files != null) {
      var sums = folderHelper.findChildItems(files.getId());
      return toPathItems(rel, folderHelper.concatPath(siteFolderPath, files.getName()), sums);
    }
    return toPathItems(rel, siteFolderPath, folderHelper.findChildItems(siteFolder.getId()));
  }

  private static boolean hasNonFolderChild(List<PSPathItem> items) {
    if (items == null) {
      return false;
    }
    for (var item : items) {
      if (item != null && !item.isFolder()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Explorer expects CM1 {@code Pages} (and {@code Files} when missing) under
   * the site node. FastForward sample sites do not seed a Pages folder (#3457).
   */
  private List<PSPathItem> injectVirtualSiteChromeIfNeeded(
      String path, List<PSPathItem> items) {
    try {
      var sfp = getSiteIdAndFolderPath(path);
      if (!sfp.isOnlySiteId()) {
        return items;
      }
      var out = items == null ? new ArrayList<PSPathItem>() : new ArrayList<>(items);
      var siteFolder = findMatchingSiteFolder(sfp.getSiteId());
      var siteFolderPath =
          siteFolder != null
              ? folderHelper.concatPath(SITE_ROOT, siteFolder.getName())
              : folderHelper.concatPath(SITE_ROOT, sfp.getSiteId());
      if (!containsNamedFolder(out, "Pages")) {
        out.add(0, createVirtualSiteFolder(sfp.getSiteId(), siteFolderPath, "Pages"));
      }
      return out;
    } catch (Exception ex) {
      log.debug("Virtual site chrome inject failed for path {}", path, ex);
      return items;
    }
  }

  private static boolean containsNamedFolder(List<PSPathItem> items, String name) {
    if (items == null) {
      return false;
    }
    for (var item : items) {
      if (item != null && siteFolderNameMatches(name, item.getName())) {
        return true;
      }
    }
    return false;
  }

  private PSPathItem createVirtualSiteFolder(
      String siteId, String siteFolderPath, String name) {
    var item = createPathItem();
    item.setName(name);
    item.setType("Folder");
    item.setCategory(IPSItemSummary.Category.FOLDER);
    item.setLeaf(false);
    item.setHasItemChildren(true);
    item.setHasFolderChildren(true);
    item.setPath("/" + siteId + "/" + name + "/");
    item.setFolderPath(folderHelper.concatPath(siteFolderPath, name));
    return item;
  }

  private PSPathItem findVirtualSiteChromeItem(String path, SiteIdAndFolderPath sfp) {
    var remaining = trimFolderSegments(sfp.getRelativeFolderPath());
    if (remaining.isEmpty() || remaining.contains("/")) {
      return null;
    }
    if (!isPagesSegment(remaining) && !isFilesSegment(remaining)) {
      return null;
    }
    try {
      var siteFolder = findMatchingSiteFolder(sfp.getSiteId());
      var siteFolderPath =
          siteFolder != null
              ? folderHelper.concatPath(SITE_ROOT, siteFolder.getName())
              : folderHelper.concatPath(SITE_ROOT, sfp.getSiteId());
      var item = createVirtualSiteFolder(sfp.getSiteId(), siteFolderPath, remaining);
      item.setPath(ensureTrailingSlash(path));
      return item;
    } catch (Exception ex) {
      log.debug("Virtual site chrome find failed for path {}", path, ex);
      return null;
    }
  }

  public static boolean isPagesSegment(String name) {
    return name != null && "pages".equals(name.trim().toLowerCase(Locale.ROOT));
  }

  public static boolean isFilesSegment(String name) {
    return name != null && "files".equals(name.trim().toLowerCase(Locale.ROOT));
  }

  public static String trimFolderSegments(String relative) {
    if (relative == null) {
      return "";
    }
    var t = relative.trim();
    while (t.startsWith("/")) {
      t = t.substring(1);
    }
    while (t.endsWith("/")) {
      t = t.substring(0, t.length() - 1);
    }
    return t;
  }

  static String ensureTrailingSlash(String path) {
    if (path == null || path.isEmpty()) {
      return "/";
    }
    return path.endsWith("/") ? path : path + "/";
  }

  /**
   * Match finder site id to a //Sites child name (underscore / space variants).
   */
  public static boolean siteFolderNameMatches(String siteId, String folderName) {
    if (siteId == null || folderName == null) {
      return false;
    }
    var a = normalizeSiteFolderToken(siteId);
    var b = normalizeSiteFolderToken(folderName);
    return !a.isEmpty() && a.equals(b);
  }

  static String normalizeSiteFolderToken(String raw) {
    return String.valueOf(raw)
        .trim()
        .replace('_', ' ')
        .replaceAll("\\s+", "")
        .toLowerCase(Locale.ROOT);
  }

  private List<PSPathItem> toPathItems(
      String relativePath, String fullFolderPath, List<IPSItemSummary> sums) {
    var pathFolderItems = new ArrayList<PSPathItem>();
    var pathItems = new ArrayList<PSPathItem>();
    for (var data : sums) {
      if (shouldFilterItem(data)) {
        continue;
      }
      var item = createPathItem();
      convert(data, item);
      item.setPath(relativePath + item.getName());
      item.setFolderPath(folderHelper.concatPath(fullFolderPath, item.getName()));
      if (data.isFolder()) {
        pathFolderItems.add(item);
      } else {
        pathItems.add(item);
      }
    }
    Collections.sort(pathFolderItems, PSPathItemComparator.getInstance());
    Collections.sort(pathItems, PSPathItemComparator.getInstance());
    pathFolderItems.addAll(pathItems);
    return pathFolderItems;
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
      if (site == null || site.getFolderPath() == null) {
        throw new PSPathNotFoundServiceException("Site could not be found for path: " + path);
      }
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
        || IPSItemSummary.Category.EXTERNAL_SECTION_FOLDER.equals(item.getCategory());
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
    if (navService == null) {
      return types;
    }
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

    /**
     * Relative folder suffix including a leading slash ({@code /} for site-only,
     * {@code /Pages} for the Pages chrome path).
     */
    public String getRelativeFolderPath() {
      return folderPath;
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
