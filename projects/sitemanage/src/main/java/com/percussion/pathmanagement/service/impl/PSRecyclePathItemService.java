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

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notEmpty;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.ui.service.IPSListViewHelper;
import com.percussion.user.service.IPSUserService;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Path item service for the Recycling bin. */
@Component("recyclePathItemService")
@Lazy
public class PSRecyclePathItemService extends PSPathItemService {

  private final IPSRecycleService recycleService;
  private final IPSManagedNavService navService;
  private String navTreeType;
  private String navonType;

  @Autowired
  public PSRecyclePathItemService(
      IPSFolderHelper folderHelper,
      IPSIdMapper idMapper,
      IPSPageService pageService,
      IPSItemWorkflowService itemWorkflowService,
      IPSAssetService assetService,
      IPSWidgetAssetRelationshipService widgetAssetRelationshipService,
      IPSContentMgr contentMgr,
      IPSWorkflowService workflowService,
      @Qualifier("cm1ListViewHelper") IPSListViewHelper listViewHelper,
      IPSUserService userService,
      IPSRecycleService recycleService,
      IPSManagedNavService navService) {
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
    this.recycleService = recycleService;
    this.navService = navService;
    this.rootName = "Recycling";
  }

  @Override
  protected String getFullFolderPath(String path) {
    notEmpty(path, "path");
    log.debug("Getting full folder path for path: {}", path);

    var fullFolderPath = RECYCLING_ROOT;
    if (!"/".equals(path)) {
      fullFolderPath = folderHelper.concatPath(fullFolderPath, path);
    }
    return fullFolderPath;
  }

  @Override
  public PSItemProperties findItemProperties(String path) throws PSPathNotFoundServiceException {
    notEmpty(path, "path");
    if (log.isDebugEnabled()) {
      log.debug("find item properties: {}", path);
    }
    var fullFolderPath = getFullFolderPath(path);
    try {
      return folderHelper.findItemProperties(fullFolderPath, RECYCLED_RELATE_TYPE);
    } catch (Exception e) {
      throw new PSPathNotFoundServiceException("Path not found: " + path);
    }
  }

  @Override
  protected List<PSPathItem> findItems(String path) {
    var fullPath = getFullFolderPath(path);
    log.debug("findItems path: {}", fullPath);
    var items = new ArrayList<PSPathItem>();
    var summaries = recycleService.findChildren(fullPath);
    for (var summ : summaries) {
      var pathItem = new PSPathItem();
      convert(summ, pathItem);
      pathItem.setPath(path + summ.getName());
      pathItem.setFolderPath(fullPath + summ.getName());
      pathItem.setFolderPaths(asList(fullPath));
      if (!shouldFilterItem(pathItem)) {
        items.add(pathItem);
      }
    }
    return items;
  }

  @Override
  public int deleteFolder(PSDeleteFolderCriteria criteria)
      throws PSPathServiceException,
          PSValidationException,
          IPSDataService.DataServiceNotFoundException,
          IPSDataService.DataServiceLoadException,
          PSNotFoundException {
    var folder = findItem(criteria.getPath());
    if (folder.getCategory() == IPSItemSummary.Category.SECTION_FOLDER) {
      log.debug("Detected section folder being purged. Id is: {}", criteria.getPath());
      var folderPath = folder.getFolderPath();
      try {
        // Purge the navon item, then call super delete folder to handle the rest.
        folderHelper.removeItem(
            folderPath,
            idMapper.getString(
                navService.findNavigationIdFromFolder(folderPath, RECYCLED_RELATE_TYPE)),
            true);
      } catch (IllegalArgumentException e) {
        throw new PSPathServiceException(e.getMessage());
      } catch (Exception e) {
        throw new PSPathServiceException(
            "Failed to delete navon from section: " + criteria.getPath(), e);
      }
    }
    return super.deleteFolder(criteria);
  }

  @Override
  protected PSPathItem findItem(String path) throws IPSDataService.DataServiceLoadException {
    var fullPath = getFullFolderPath(path);
    log.debug("findItem path: {}", fullPath);
    var summary = recycleService.findItem(fullPath);
    var pathItem = new PSPathItem();
    convert(summary, pathItem);
    pathItem.setPath(path);
    pathItem.setFolderPath(fullPath);
    pathItem.setFolderPaths(asList(fullPath));
    return pathItem;
  }

  @Override
  protected void convert(IPSItemSummary dataItem, PSPathItem newPathItem) {
    super.convert(dataItem, newPathItem);
    newPathItem.setId(dataItem.getId());
    newPathItem.setName(dataItem.getName());
    newPathItem.setType(dataItem.getType());
  }

  @Override
  protected boolean shouldFilterItem(IPSItemSummary item) {
    if (navonType == null) {
      navonType = navService.getNavonContentTypeNames().get(0);
    }
    if (navTreeType == null) {
      navTreeType = navService.getNavTreeContentTypeNames().get(0);
    }
    return item == null
        || ".system".equals(item.getName())
        || item.getCategory().equals(IPSItemSummary.Category.EXTERNAL_SECTION_FOLDER)
        || (navonType != null && navService.getNavonContentTypeNames().contains(item.getType()))
        || (navTreeType != null
            && navService.getNavTreeContentTypeNames().contains(item.getType()));
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
  protected String getFolderRoot() {
    return RECYCLING_ROOT;
  }

  /** Constant for the recycling root folder path. */
  public static final String RECYCLING_ROOT_SUB = "//Folders/$System$";

  /** Constant for the Recycling Folder. */
  public static final String RECYCLING_ROOT = RECYCLING_ROOT_SUB + "/Recycling";

  /** Constant for the response given when a folder contains in use pages. */
  public static final String PAGES_IN_USE_PAGES = "PagesInUsePages";

  /** Constant for the response given when a user is not authorized to remove pages in a folder. */
  public static final String PAGES_NOT_AUTHORIZED = "PagesNotAuthorized";

  /** Constant for the response given when a folder contains pages linked by templates. */
  public static final String PAGES_IN_USE_TEMPLATES = "PagesInUseTemplates";

  /** The log instance to use for this class, never null. */
  private static final Logger log = LogManager.getLogger(PSRecyclePathItemService.class);

  /** Static constant to represent the recycled content relationship type. */
  private static final String RECYCLED_RELATE_TYPE = PSRelationshipConfig.TYPE_RECYCLED_CONTENT;
}
