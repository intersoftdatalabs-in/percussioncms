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
package com.percussion.share.dao.impl;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notEmpty;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.recycle.service.impl.PSRecycleService;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.content.data.PSItemSummary.ObjectTypeEnum;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.IPSItemSummary.Category;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSCatalogFactoryService;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSItemSummaryFactoryService;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTypeEnum;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.thread.PSThreadUtils;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentWs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean("itemSummaryService")
public class PSItemSummaryService
    implements IPSItemSummaryFactoryService, IPSDataItemSummaryService {
  private static final String ICON_BASE_PATH = "/Rhythmyx/";

  private static final String FOLDER_ICON_PATH =
      ICON_BASE_PATH + "sys_resources/images/finderFolder.png";
  private static final String FOLDER_NAVON_ICON_PATH =
      ICON_BASE_PATH + "sys_resources/images/finderFolderNavigation.png";
  private static final String PAGE_ICON_PATH =
      ICON_BASE_PATH + "sys_resources/images/finderPage.png";
  private static final String LANDING_PAGE_ICON_PATH =
      ICON_BASE_PATH + "sys_resources/images/finderLandingPage.png";

  private final IPSContentWs contentWs;
  private final PSItemDefManager itemDefManager;
  private final IPSIdMapper idMapper;
  private final IPSManagedNavService navService;

  private static final String ASSET_ROOT = PSAssetPathItemService.ASSET_ROOT;

  private static final String FOLDER_RELATE_TYPE = PSRelationshipConfig.TYPE_FOLDER_CONTENT;
  private static final String RECYCLED_TYPE = PSRelationshipConfig.TYPE_RECYCLED_CONTENT;
  private static final String RECYCLING_ROOT = PSRecycleService.RECYCLING_ROOT;

  /** Folders created by the system. */
  private static final String[] SYSTEM_FOLDERS =
      new String[] {
        ASSET_ROOT,
        ASSET_ROOT + "/forms",
        ASSET_ROOT + "/uploads",
        ASSET_ROOT + "/uploads/files",
        ASSET_ROOT + "/uploads/images",
        ASSET_ROOT + "/calendars",
        ASSET_ROOT + "/polls"
      };

  @Autowired
  public PSItemSummaryService(
      IPSContentWs contentWs,
      PSItemDefManager itemDefManager,
      IPSIdMapper idMapper,
      IPSManagedNavService navService) {
    this.contentWs = contentWs;
    this.itemDefManager = itemDefManager;
    this.idMapper = idMapper;
    this.navService = navService;
  }

  @Override
  public String pathToId(String path) throws DataServiceNotFoundException {
    if (path == null) {
      throw new IllegalArgumentException("path may not be null or empty");
    }
    if (path.contains(RECYCLING_ROOT)) {
      return pathToId(path, RECYCLED_TYPE);
    } else {
      return pathToId(path, FOLDER_RELATE_TYPE);
    }
  }

  @Override
  public String pathToId(String path, String relationshipTypeName)
      throws DataServiceNotFoundException {
    notEmpty(path, "Path cannot be null or empty");
    try {
      var normalizedPath = StringUtils.removeEnd(path, "/");
      if (log.isTraceEnabled())
        log.trace("Getting id for path: {} normalized: {}", path, normalizedPath);
      var guid = contentWs.getIdByPath(normalizedPath, relationshipTypeName);
      var id = guid == null ? null : idMapper.getString(guid);
      if (log.isTraceEnabled())
        log.trace(format("Converted path: {0} to id {1}", normalizedPath, id));
      return id;
    } catch (PSErrorException e) {
      throw new DataServiceNotFoundException("Failed to convert path: " + path, e);
    }
  }

  @Override
  public PSDataItemSummary find(String id) throws DataServiceLoadException {
    notEmpty(id, "id");
    return find(defaultItemSummaryFactory, id, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
  }

  @Override
  public PSDataItemSummary find(String id, String relationshipTypeName)
      throws DataServiceLoadException {
    notEmpty(id, "id");
    return find(defaultItemSummaryFactory, id, relationshipTypeName);
  }

  @Override
  public List<PSDataItemSummary> findChildFolders(String id) throws DataServiceLoadException {
    notEmpty(id, "id");
    try {
      var guid = guidForItemId(id);
      var sums = contentWs.findChildFolders(guid);
      return convert(defaultItemSummaryFactory, sums, -1, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
    } catch (Exception e) {
      log.error("Failed to load: {}", id);
      throw new DataServiceLoadException(e);
    }
  }

  @Override
  public List<PSDataItemSummary> findFolderChildren(String id) throws DataServiceLoadException {
    return findFolderChildren(defaultItemSummaryFactory, id);
  }

  @Override
  public List<PSDataItemSummary> findAll()
      throws DataServiceLoadException, DataServiceNotFoundException {
    throw new UnsupportedOperationException("findAll is not yet supported");
  }

  public static final Logger log = LogManager.getLogger(PSItemSummaryService.class);

  private <F extends IPSItemSummary> List<F> convert(
      IPSCatalogItemFactory<F, String> factory,
      List<PSItemSummary> sums,
      int landingPageId,
      String relationshipTypeName)
      throws PSErrorException, PSInvalidContentTypeException, DataServiceLoadException {
    var items = new ArrayList<F>();
    for (var sum : sums) {
      PSThreadUtils.checkForInterrupt();
      try {
        // Pre-check with the checked PSInvalidContentTypeException so we do
        // not call getNavonProperties / item-def APIs that wrap type 315
        // in RuntimeException and mark the listing TX rollback-only (#3410).
        if (!isKnownContentType(sum.getContentTypeId())) {
          log.warn(
              "Skipping folder child with unknown content type (name={}, type={}, guid={})",
              sum.getName(),
              sum.getContentTypeId(),
              sum.getGUID());
          continue;
        }
        var item = factory.create("");
        var isLandingPage = ((PSLegacyGuid) sum.getGUID()).getContentId() == landingPageId;
        convert(sum, item, isLandingPage, relationshipTypeName);
        items.add(item);
      } catch (RuntimeException | PSInvalidContentTypeException e) {
        // Sample FF nav types (313-315) may be absent when perc.nav owns percNav*
        // (#3410). Skip the unreadable child instead of failing the folder list.
        log.warn(
            "Skipping folder child that cannot be summarized (name={}, guid={}): {}",
            sum.getName(),
            sum.getGUID(),
            e.getMessage());
      }
    }
    return items;
  }

  private boolean isFolder(PSItemSummary item) {
    return item.getObjectType().getOrdinal() == ObjectTypeEnum.FOLDER.getOrdinal();
  }

  /**
   * True when {@link PSItemDefManager} has a running handler for the id.
   * Uses the checked {@link PSInvalidContentTypeException} so a missing FF
   * type (313–315) does not mark the Spring listing transaction rollback-only.
   */
  boolean isKnownContentType(int contentTypeId) {
    if (itemDefManager == null) {
      return false;
    }
    try {
      var name = itemDefManager.contentTypeIdToName(contentTypeId);
      return name != null && !name.isBlank();
    } catch (PSInvalidContentTypeException e) {
      return false;
    }
  }

  String getNavFolderType(PSItemSummary item, String relationshipTypeName) {
    if (!isFolder(item)) return null;
    try {
      var childNavId = navService.findNavigationIdFromFolder(item.getGUID(), relationshipTypeName);
      if (childNavId == null) {
        return null;
      }
      // findNodesByIds inside getNavonProperties throws RuntimeException for
      // rffNavTree 315 and marks the folder-list TX rollback-only (#3410).
      if (!navChildHasKnownContentType(childNavId)) {
        log.warn(
            "Ignoring nav type for folder {} — nav item content type is not registered",
            item.getName());
        return null;
      }
      var map =
          navService.getNavonProperties(
              childNavId, Collections.singletonList(IPSManagedNavService.NAVON_FIELD_TYPE));
      return map.get(IPSManagedNavService.NAVON_FIELD_TYPE);
    } catch (RuntimeException e) {
      log.warn(
          "Ignoring nav type for folder {} — nav content type missing or unreadable: {}",
          item.getName(),
          e.getMessage());
      return null;
    }
  }

  /**
   * Lightweight summary lookup (no item-def load). False when the nav child
   * is an FF type that perc.nav never registered.
   */
  boolean navChildHasKnownContentType(IPSGuid childNavId) {
    if (childNavId == null) {
      return false;
    }
    try {
      var navSums = contentWs.findItems(Collections.singletonList(childNavId), false);
      if (navSums == null || navSums.isEmpty() || navSums.get(0) == null) {
        return false;
      }
      return isKnownContentType(navSums.get(0).getContentTypeId());
    } catch (RuntimeException e) {
      log.debug("Could not read nav child type for {}: {}", childNavId, e.getMessage());
      return false;
    }
  }

  // override the generic factory method required by IPSCatalogFactoryService
  @Override
  public <F extends IPSItemSummary> List<F> findAll(
      IPSCatalogFactoryService.IPSCatalogItemFactory<F, String> factory)
      throws DataServiceLoadException, DataServiceNotFoundException {
    // simple no-op implementation; clients can provide a factory and handle empty list
    return new ArrayList<>();
  }

  protected <F extends IPSItemSummary> void convert(
      PSItemSummary itemSummary,
      F dataItemSummary,
      boolean isLandingPage,
      String relationshipTypeName)
      throws PSErrorException, PSInvalidContentTypeException {
    var id = idMapper.getString(itemSummary.getGUID());
    dataItemSummary.setId(id);
    dataItemSummary.setName(itemSummary.getName());

    var ctType = itemSummary.getContentTypeName();
    dataItemSummary.setType(ctType);

    var ctId = itemDefManager.contentTypeNameToId(ctType);
    dataItemSummary.setLabel(itemDefManager.contentTypeIdToLabel(ctId));

    var parentPaths =
        asList(contentWs.findFolderPaths(itemSummary.getGUID(), relationshipTypeName));
    dataItemSummary.setFolderPaths(parentPaths);

    var navType = getNavFolderType(itemSummary, relationshipTypeName);
    var cat = getCategory(itemSummary, navType, isLandingPage, parentPaths);
    dataItemSummary.setCategory(cat);

    var revisionLock = itemSummary.isRevisionLock();
    dataItemSummary.setRevisionable(revisionLock);

    var icon = getIcon(itemSummary, isLandingPage, navType);
    dataItemSummary.setIcon(icon);
  }

  private Category getCategory(
      PSItemSummary itemSummary, String navType, boolean isLandingPage, List<String> parentPaths) {
    var ctType = itemSummary.getContentTypeName();
    var category = Category.ASSET;
    if (isLandingPage) category = Category.LANDING_PAGE;
    else if (navType != null) {
      if (navType.equals(PSSectionTypeEnum.externallink.name()))
        category = Category.EXTERNAL_SECTION_FOLDER;
      else category = Category.SECTION_FOLDER;
    } else if (IPSPageService.PAGE_CONTENT_TYPE.equals(ctType)) category = Category.PAGE;
    else if (isFolder(itemSummary)) {
      var path = parentPaths.get(0) + "/" + itemSummary.getName();
      if (isSystemFolder(path)) category = Category.SYSTEM;
      else category = Category.FOLDER;
    }
    return category;
  }

  private boolean isSystemFolder(String folderPath) {
    for (var path : SYSTEM_FOLDERS) {
      if (path.equals(folderPath)) return true;
    }
    return false;
  }

  private int getLandingPageId(List<PSItemSummary> sums) {
    var navonCType = navService.getNavonContentTypeIds();
    var navtreeCType = navService.getNavTreeContentTypeIds();
    PSItemSummary navItem = null;
    for (var sum : sums) {
      PSThreadUtils.checkForInterrupt();
      var type = sum.getContentTypeId();
      // type is an int while collections hold Long, convert before checking
      long typeLong = type;
      if (navonCType.contains(typeLong) || navtreeCType.contains(typeLong)) {
        navItem = sum;
        break;
      }
    }
    if (navItem == null) return -1;
    var navId = (PSLegacyGuid) navItem.getGUID();
    var pageGuid = navService.getLandingPageFromNavnode(navId);
    return (pageGuid == null) ? -1 : ((PSLegacyGuid) pageGuid).getContentId();
  }

  private String getIcon(PSItemSummary item, boolean isLandingPage, String navType) {
    if (isLandingPage) {
      return LANDING_PAGE_ICON_PATH;
    } else if (navType != null) {
      return FOLDER_NAVON_ICON_PATH;
    } else if (item.getObjectType().getOrdinal() == ObjectTypeEnum.FOLDER.getOrdinal()) {
      return FOLDER_ICON_PATH;
    }
    var ctType = item.getContentTypeName();
    if (IPSPageService.PAGE_CONTENT_TYPE.equals(ctType)) {
      return PAGE_ICON_PATH;
    }
    var id = ((PSLegacyGuid) item.getGUID());
    return getIcon(idMapper.getString(id));
  }

  public <F extends IPSItemSummary> List<F> findFolderChildren(
      IPSCatalogItemFactory<F, String> factory, String id) throws DataServiceLoadException {
    try {
      isTrue(
          !StringUtils.startsWith(id, "//"),
          "findFolderChildren takes an id not a path. Use pathToId(path).");
      var guid = guidForItemId(id);
      var sums = contentWs.findFolderChildren(guid, false);
      var landingPageId = getLandingPageId(sums);
      return convert(factory, sums, landingPageId, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
    } catch (Exception e) {
      var err = "Failed to load: " + id;
      log.error(err, e);
      throw new DataServiceLoadException(err, e);
    }
  }

  private boolean isLandingPage(PSItemSummary item, String relationshipTypeName) {
    if (!item.getContentTypeName().equals(IPSPageService.PAGE_CONTENT_TYPE)) return false;
    return navService.isLandingPage(item.getGUID(), relationshipTypeName);
  }

  public <F extends IPSItemSummary> F find(IPSCatalogItemFactory<F, String> factory, String id)
      throws DataServiceLoadException {
    return find(factory, id, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
  }

  public <F extends IPSItemSummary> F find(
      IPSCatalogItemFactory<F, String> factory, String id, String relationshipTypeName)
      throws DataServiceLoadException {
    try {
      var guid = guidForItemId(id);
      var sums = contentWs.findItems(Collections.singletonList(guid), false);
      if (sums.isEmpty()) return null;
      var item = sums.get(0);
      var landingPageId =
          isLandingPage(item, relationshipTypeName)
              ? ((PSLegacyGuid) item.getGUID()).getContentId()
              : -1;
      return convert(factory, sums, landingPageId, relationshipTypeName).get(0);
    } catch (Exception e) {
      var err = "Failed to load: " + id;
      log.error(err, e);
      throw new DataServiceLoadException(e);
    }
  }

  protected String getIcon(String id) {
    var path = getIconFromSystem(id);
    if (path != null) path = path.replaceFirst("^\\.\\./", ICON_BASE_PATH);
    return path;
  }

  /**
   * Map an Explorer / REST item id to a GUID. Bare numeric content ids such as
   * FastForward {@code 594} must not fail with untyped {@code PSGuid.assemble}
   * ({@code Type is undetermined}) — retry as {@code LEGACY_CONTENT} (#3722).
   */
  IPSGuid guidForItemId(String id) {
    try {
      return idMapper.getGuid(id);
    } catch (IllegalArgumentException e) {
      if (!isUndeterminedGuidType(e)) {
        throw e;
      }
      try {
        return idMapper.getGuidFromContentId(Long.parseLong(id.trim()));
      } catch (NumberFormatException nfe) {
        throw e;
      }
    }
  }

  static boolean isUndeterminedGuidType(Throwable error) {
    for (Throwable t = error; t != null; t = t.getCause()) {
      String msg = t.getMessage();
      if (msg != null && msg.contains("Type is undetermined")) {
        return true;
      }
    }
    return false;
  }

  protected String getIconFromSystem(String id) {
    var locator = idMapper.getLocator(id);
    var paths = itemDefManager.getContentTypeIconPaths(Collections.singletonList(locator));
    return paths.get(locator);
  }

  public static class PSDataItemSummaryFactory
      implements IPSCatalogItemFactory<PSDataItemSummary, String> {
    @Override
    public PSDataItemSummary create(String id) throws DataServiceLoadException {
      return new PSDataItemSummary();
    }
  }

  private final PSDataItemSummaryFactory defaultItemSummaryFactory = new PSDataItemSummaryFactory();
}
