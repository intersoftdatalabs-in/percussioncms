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

package com.percussion.recent.service.impl;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.cms.IPSConstants;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.itemmanagement.service.impl.PSWorkflowHelper;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetContentType;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.recent.data.PSRecent.RecentType;
import com.percussion.recent.service.IPSRecentServiceBase;
import com.percussion.recent.service.rest.IPSRecentService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.webservices.PSWebserviceUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Service implementation for managing recent items, templates, folders, and asset types. Provides
 * methods to add, find, and clean up recent user actions.
 */
@Transactional(propagation = Propagation.REQUIRED)
@Component("recentService")
@Lazy
public class PSRecentService implements IPSRecentService {
  @Autowired
  private @Qualifier("recentServiceBase") IPSRecentServiceBase recentService;

  @Autowired
  private @Qualifier("pathService") IPSPathService pathService;

  @Autowired private IPSIdMapper idMapper;

  @Autowired
  private @Qualifier("folderHelper") IPSFolderHelper folderHelper;

  @Autowired private IPSAssetService assetService;

  @Autowired private IPSSiteTemplateService siteTemplateService;

  private TransactionTemplate requiresNewReadOnly;

  private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);

  @Autowired
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    requiresNewReadOnly = new TransactionTemplate(transactionManager);
    requiresNewReadOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    requiresNewReadOnly.setReadOnly(true);
  }

  /**
   * Finds recent items for the current user, optionally ignoring archived items.
   *
   * <p>Lookups run in {@link Propagation#REQUIRES_NEW} so a Hibernate failure on one stale recent
   * id cannot mark this transaction rollback-only (which previously caused {@code
   * UnexpectedRollbackException} on Home Recent even after the failure was caught).
   */
  @Override
  public List<PSItemProperties> findRecentItem(boolean ignoreArchivedItems) {
    var user = PSWebserviceUtils.getUserName();
    var recentEntries = recentService.findRecent(user, null, RecentType.ITEM);
    var items = new ArrayList<PSItemProperties>();
    var toDelete = new ArrayList<String>();
    if (log.isDebugEnabled()) {
      log.debug(
          "findRecentItem user={} entryCount={} entries={}",
          user,
          recentEntries.size(),
          recentEntries);
    }
    for (var entry : recentEntries) {
      ItemLookupResult lookup = findItemPropertiesIsolated(entry);
      if (lookup.properties != null) {
        var itemProps = lookup.properties;
        // Don't return archived items and items with no path on home page.
        if (ignoreArchivedItems
            && (PSWorkflowHelper.WF_STATE_ARCHIVE.equals(itemProps.getStatus())
                || itemProps.getPath() == null)) {
          continue;
        }
        items.add(itemProps);
      } else if (lookup.missing) {
        // Only drop entries that are confirmed gone — not load failures
        // (those used to mark the list empty and wipe recent on every Home load).
        log.debug("Removing recent item find returned null : {}", entry);
        toDelete.add(entry);
      } else {
        log.warn("Keeping recent item after lookup failure (entry will not display): {}", entry);
      }
    }
    if (!toDelete.isEmpty()) {
      recentService.deleteRecent(user, null, RecentType.ITEM, toDelete);
    }
    if (!recentEntries.isEmpty() && items.isEmpty()) {
      log.warn(
          "findRecentItem: {} stored entries for user {} but 0 displayable items",
          recentEntries.size(),
          user);
    }
    return items;
  }

  /** Result of an isolated recent-item lookup. */
  private static final class ItemLookupResult {
    final PSItemProperties properties;

    /** True when the item is known missing (null without exception). */
    final boolean missing;

    ItemLookupResult(PSItemProperties properties, boolean missing) {
      this.properties = properties;
      this.missing = missing;
    }

    static ItemLookupResult found(PSItemProperties props) {
      return new ItemLookupResult(props, false);
    }

    static ItemLookupResult notFound() {
      return new ItemLookupResult(null, true);
    }

    static ItemLookupResult failed() {
      return new ItemLookupResult(null, false);
    }
  }

  /**
   * Load item properties in a nested transaction so failures do not poison the outer TX.
   *
   * @param entry content id / guid string from recent storage
   * @return lookup result distinguishing missing items from load failures
   */
  private ItemLookupResult findItemPropertiesIsolated(String entry) {
    if (requiresNewReadOnly == null) {
      return lookupItemProperties(entry);
    }
    return requiresNewReadOnly.execute(
        status -> {
          ItemLookupResult result = lookupItemProperties(entry);
          if (result.properties == null && !result.missing) {
            // Hibernate may have marked the nested session; roll back only the nested TX
            status.setRollbackOnly();
          }
          return result;
        });
  }

  /**
   * Resolve recent entry id to item properties via folderHelper (same path as search result rows).
   */
  private ItemLookupResult lookupItemProperties(String entry) {
    try {
      PSItemProperties props = folderHelper.findItemPropertiesById(entry);
      if (props != null) {
        return ItemLookupResult.found(props);
      }
      return ItemLookupResult.notFound();
    } catch (Exception e) {
      log.warn(
          "folderHelper.findItemPropertiesById failed for {}: {}",
          entry,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return ItemLookupResult.failed();
    }
  }

  /** Finds recent templates for the current user and site. */
  @Override
  public List<PSTemplateSummary> findRecentTemplate(String siteName) {
    var user = PSWebserviceUtils.getUserName();
    var recentEntries = recentService.findRecent(user, siteName, RecentType.TEMPLATE);
    var templates = new ArrayList<PSTemplateSummary>();
    var toDelete = new ArrayList<String>();

    var siteTemplateMap = new HashMap<String, PSTemplateSummary>();
    // If we do not find site we will remove all entries for site that no longer exist
    var siteTemplates = siteTemplateService.findTemplatesBySite(siteName);
    for (var siteTemplate : siteTemplates) {
      siteTemplateMap.put(siteTemplate.getId(), siteTemplate);
    }

    for (var entry : recentEntries) {
      var template = siteTemplateMap.get(entry);
      // Cleanup old or invalid entries
      if (template == null) {
        log.debug("Removing recent template not a current site template :{}", entry);
        toDelete.add(entry);
      } else {
        templates.add(template);
      }
    }

    if (!toDelete.isEmpty()) {
      recentService.deleteRecent(user, siteName, RecentType.TEMPLATE, toDelete);
    }
    return templates;
  }

  /** Finds recent site folders for the current user and site. */
  @Override
  public List<PSPathItem> findRecentSiteFolder(String siteName) {
    var user = PSWebserviceUtils.getUserName();
    var recentEntries = recentService.findRecent(user, siteName, RecentType.SITE_FOLDER);
    var pathItems = new ArrayList<PSPathItem>();
    var toDelete = new ArrayList<String>();

    for (var entry : recentEntries) {
      PSPathItem pathItem = null;
      try {
        pathItem = pathService.find(entry);
        if (pathItem != null) {
          pathItems.add(pathItem);
        } else {
          log.debug("Removing recent siteFolder entry find returned null : {}", entry);
        }
      } catch (Exception e) {
        log.debug(
            "Removing error entry from recent siteFolder list {}, Error: {}",
            entry,
            PSExceptionUtils.getMessageForLog(e));
      }
      if (pathItem == null) {
        toDelete.add(entry);
      }
    }
    if (!toDelete.isEmpty()) {
      recentService.deleteRecent(user, siteName, RecentType.SITE_FOLDER, toDelete);
    }
    return pathItems;
  }

  /** Finds recent asset folders for the current user. */
  @Override
  public List<PSPathItem> findRecentAssetFolder() {
    var user = PSWebserviceUtils.getUserName();
    var recentEntries = recentService.findRecent(user, null, RecentType.ASSET_FOLDER);
    var pathItems = new ArrayList<PSPathItem>();
    var toDelete = new ArrayList<String>();

    for (var entry : recentEntries) {
      PSPathItem pathItem = null;
      try {
        pathItem = pathService.find(entry);
        if (pathItem != null) {
          pathItems.add(pathItem);
        } else {
          log.debug("Removing recent assetFolder entry find returned null :{}", entry);
        }
      } catch (Exception e) {
        log.debug(
            "Removing error entry from recent assetFolder list {}, Error: {}",
            entry,
            PSExceptionUtils.getMessageForLog(e));
      }
      if (pathItem == null) {
        toDelete.add(entry);
      }
    }
    if (!toDelete.isEmpty()) {
      recentService.deleteRecent(user, null, RecentType.ASSET_FOLDER, toDelete);
    }
    return pathItems;
  }

  /** Adds a recent item for the current user. */
  @Override
  public void addRecentItem(String value) {
    var user = PSWebserviceUtils.getUserName();
    if (PSTypeEnum.LEGACY_CONTENT.getOrdinal() != idMapper.getGuid(value).getType()) {
      throw new IllegalArgumentException("Value must be an item guid");
    }
    // Store guid as a revisionless guid.
    var locator = new PSLocator(idMapper.getContentId(value));
    locator.setRevision(-1);
    value = idMapper.getString(locator);
    recentService.addRecent(user, null, RecentType.ITEM, value);
    log.debug("addRecentItem user={} storedValue={}", user, value);
  }

  /** Adds a recent template for the current user and site. */
  @Override
  public void addRecentTemplate(String siteName, String value) {
    var user = PSWebserviceUtils.getUserName();
    if (!isLegacyContentItemGuid(value, idMapper)) {
      // Assembly TEMPLATE guids (perc.pageDatabase) are valid page templates but not
      // percTemplate content items. Skipping avoids IllegalArgumentException marking
      // the page-save TX rollback-only (#3728).
      log.debug("Skipping recent template; not a percTemplate content item guid: {}", value);
      return;
    }
    // Not actually checking template exists for performance, check and filter done on find.
    recentService.addRecent(user, siteName, RecentType.TEMPLATE, value);
  }

  /**
   * Recent item/template rows store percTemplate / page <em>content item</em> guids ({@link
   * PSTypeEnum#LEGACY_CONTENT}), not assembly TEMPLATE type ids.
   */
  static boolean isLegacyContentItemGuid(String value, IPSIdMapper mapper) {
    if (value == null || value.isBlank() || mapper == null) {
      return false;
    }
    try {
      return PSTypeEnum.LEGACY_CONTENT.getOrdinal() == mapper.getGuid(value).getType();
    } catch (RuntimeException e) {
      return false;
    }
  }

  /** Adds a recent site folder for the current user. */
  @Override
  public void addRecentSiteFolder(String value) {
    var user = PSWebserviceUtils.getUserName();
    if (StringUtils.isBlank(value)
        || !(StringUtils.startsWith(value, "//") || StringUtils.startsWith(value, "/"))) {
      return;
    }
    var folderPath = StringUtils.startsWith(value, "//") ? value.substring(1) : value;
    var siteName = PSPathUtils.getSiteFromPath(folderPath);
    if (siteName == null) {
      return;
    }
    // Not checking database for folder to improve performance, check done on way out.
    recentService.addRecent(user, siteName, RecentType.SITE_FOLDER, folderPath);
  }

  /** Adds a recent asset folder for the current user. */
  @Override
  public void addRecentAssetFolder(String value) {
    var user = PSWebserviceUtils.getUserName();
    var pos = value.indexOf("Assets");
    if (pos >= 0 && pos <= 2) {
      value = "/" + value.substring(pos);
    } else {
      return;
    }
    // Not checking database for folder to improve performance, check done on way out.
    recentService.addRecent(user, null, RecentType.ASSET_FOLDER, value);
  }

  /** Adds a recent asset type for the current user. */
  @Override
  public void addRecentAssetType(String value) {
    var user = PSWebserviceUtils.getUserName();
    recentService.addRecent(user, null, RecentType.ASSET_TYPE, value);
  }

  /** Finds recent asset types for the current user. */
  @Override
  public List<PSWidgetContentType> findRecentAssetType() throws PSDataServiceException {
    var resultList = new ArrayList<PSWidgetContentType>();
    var user = PSWebserviceUtils.getUserName();
    var recentEntries = recentService.findRecent(user, null, RecentType.ASSET_TYPE);
    var toDelete = new ArrayList<String>();
    var widgetTypeMap = new HashMap<String, PSWidgetContentType>();
    var widgetTypes = assetService.getAssetTypes("yes");
    for (var wt : widgetTypes) {
      widgetTypeMap.put(wt.getWidgetId(), wt);
    }
    for (var entry : recentEntries) {
      var wtype = widgetTypeMap.get(entry);
      // Cleanup old or invalid entries
      if (wtype == null) {
        log.debug("Removing recent asset type not a current widget type : {}", entry);
        toDelete.add(entry);
      } else {
        resultList.add(wtype);
      }
    }
    if (!toDelete.isEmpty()) {
      recentService.deleteRecent(user, null, RecentType.ASSET_TYPE, toDelete);
    }
    return resultList;
  }

  /** Deletes all recent items for the given user. */
  @Override
  public void deleteUserRecent(String user) {
    recentService.deleteRecent(user, null, null);
  }

  /** Deletes all recent items for the given site. */
  @Override
  public void deleteSiteRecent(String siteName) {
    recentService.deleteRecent(null, siteName, null);
  }

  /** Updates all recent items to use the new site name. */
  @Override
  public void updateSiteNameRecent(String oldSiteName, String newSiteName) {
    try {
      recentService.renameSiteRecent(oldSiteName, newSiteName);
    } catch (Exception e) {
      log.debug(
          "Error updating PSX_RECENT table to rename site from:{}, to {}, Error: {} ",
          oldSiteName,
          newSiteName,
          PSExceptionUtils.getMessageForLog(e));
    }
  }

  @Override
  public void addRecentItemByUser(String userName, String value) {
    recentService.addRecent(userName, null, RecentType.ITEM, value);
  }

  @Override
  public void addRecentTemplateByUser(String userName, String siteName, String value) {
    recentService.addRecent(userName, null, RecentType.TEMPLATE, value);
  }

  @Override
  public void addRecentSiteFolderByUser(String userName, String value) {
    recentService.addRecent(userName, null, RecentType.SITE_FOLDER, value);
  }

  @Override
  public void addRecentAssetFolderByUser(String userName, String value) {
    recentService.addRecent(userName, null, RecentType.ASSET_FOLDER, value);
  }

  @Override
  public void addRecentAssetTypeByUser(String userName, String value) {
    recentService.addRecent(userName, null, RecentType.ASSET_TYPE, value);
  }
}
