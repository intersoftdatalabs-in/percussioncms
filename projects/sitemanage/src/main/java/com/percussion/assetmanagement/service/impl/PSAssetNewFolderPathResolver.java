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

package com.percussion.assetmanagement.service.impl;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.assetmanagement.service.impl.PSAssetNewFolderPathResolver.PSResolvedFolderPath.PSResolvedFolderPathType;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.share.dao.PSFolderPathUtils;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import java.util.Collection;
import java.util.List;

/**
 * Resolves the folder path for an asset to be associated with an owner (page or template).
 * <strong>Does not actually move the asset.</strong> That is handled elsewhere.
 */
public class PSAssetNewFolderPathResolver {

  private final IPSSiteTemplateService siteTemplateService;
  private final IPSPageService pageService;

  public PSAssetNewFolderPathResolver(
      IPSPageService pageService, IPSSiteTemplateService siteTemplateService) {
    this.pageService = pageService;
    this.siteTemplateService = siteTemplateService;
  }

  /**
   * Resolves the folder path for where an asset should live in a site folder path given an owner.
   *
   * @param owner Could be a page or a template, never {@code null}.
   * @param asset The asset to be associated with the owner.
   * @return the resolved path.
   */
  public PSResolvedFolderPath resolveFolderPath(IPSItemSummary owner, IPSItemSummary asset)
      throws IPSDataService.DataServiceLoadException,
          PSValidationException,
          IPSDataService.DataServiceNotFoundException {
    var assetPaths = asset.getFolderPaths();

    if (IPSTemplateService.TPL_CONTENT_TYPE.equals(owner.getType())) {
      // Owner is a template; check if asset is in the same site.
      var sitePath = getSiteFolderPath(owner.getId());
      var matchingPaths = matchingPaths(sitePath, assetPaths);

      if (matchingPaths.isEmpty()) {
        // Not in the same site; request to add to base.
        return new PSResolvedFolderPath(sitePath, false, PSResolvedFolderPathType.TEMPLATE);
      }
      return new PSResolvedFolderPath(
          matchingPaths.get(0), true, PSResolvedFolderPathType.TEMPLATE);

    } else if (IPSPageService.PAGE_CONTENT_TYPE.equals(owner.getType())) {
      // Owner is a page; check if asset is in the same site as the page's template.
      var page = pageService.find(owner.getId());
      var templateId = page.getTemplateId();
      if (templateId != null) {
        var sitePath = getSiteFolderPath(templateId);
        var matchingPaths = matchingPaths(sitePath, assetPaths);
        if (matchingPaths.isEmpty()) {
          // Not in the same site; use the page's folder path.
          return new PSResolvedFolderPath(
              page.getFolderPath(), false, PSResolvedFolderPathType.PAGE);
        }
        // Use existing path; do not add to folder.
        return new PSResolvedFolderPath(matchingPaths.get(0), true, PSResolvedFolderPathType.PAGE);
      }
      throw new RuntimeException(
          "Cannot add an asset to a folder with a page that does not have a template. Page: "
              + page);
    } else {
      throw new IllegalStateException(
          "Cannot add item to owner of type: "
              + owner.getType()
              + " (should be a page or template).");
    }
  }

  private String getSiteFolderPath(String templateId) {
    var sites = siteTemplateService.findSitesByTemplate(templateId);
    isTrue(!sites.isEmpty(), "Template should have a site associated with it");
    return sites.get(0).getFolderPath();
  }

  private List<String> matchingPaths(String sitePath, Collection<String> folderPaths) {
    return PSFolderPathUtils.matchingDescedentPaths(sitePath, folderPaths);
  }

  /**
   * Represents a resolved path. None of the properties should be null. {@link #isAlreadyInFolder()}
   * {@code true} means it's already in the folder.
   */
  public static class PSResolvedFolderPath {

    private PSResolvedFolderPathType type;
    private String folderPath;
    private boolean alreadyInFolder;

    public boolean isAlreadyInFolder() {
      return alreadyInFolder;
    }

    public void setAlreadyInFolder(boolean alreadyInFolder) {
      this.alreadyInFolder = alreadyInFolder;
    }

    public PSResolvedFolderPathType getType() {
      return type;
    }

    public void setType(PSResolvedFolderPathType type) {
      this.type = type;
    }

    public String getFolderPath() {
      return folderPath;
    }

    public void setFolderPath(String folderPath) {
      this.folderPath = folderPath;
    }

    public PSResolvedFolderPath(
        String folderPath, boolean alreadyInFolder, PSResolvedFolderPathType type) {
      notNull(folderPath, "folderPath must not be null");
      notNull(type, "type must not be null");
      this.folderPath = folderPath;
      this.alreadyInFolder = alreadyInFolder;
      this.type = type;
    }

    public enum PSResolvedFolderPathType {
      PAGE,
      TEMPLATE
    }
  }
}
