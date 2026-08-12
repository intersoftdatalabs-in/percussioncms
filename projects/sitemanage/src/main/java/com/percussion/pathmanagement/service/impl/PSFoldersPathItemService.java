/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.pathmanagement.service.impl;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.ui.service.IPSListViewHelper;
import com.percussion.user.service.IPSUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Path item service for the classic Rhythmyx {@code //Folders} repository root.
 *
 * <p>Registers as finder path {@code /Folders/} so Explorer {@code findChildren("/")} returns a
 * <strong>Folders</strong> root next to Sites, Assets, Design, and Recycling (#3044). Children are
 * the real folder hierarchy under {@code //Folders} (including {@code $System$}).</p>
 *
 * <p>CM1 convenience roots ({@code /Assets}, {@code /Recycling}) remain separate path services that
 * map into {@code //Folders/$System$/…}; this service exposes the full classic tree without
 * replacing those roots.</p>
 */
@Component("foldersPathItemService")
public class PSFoldersPathItemService extends PSPathItemService {

  @Autowired
  public PSFoldersPathItemService(
      IPSFolderHelper folderHelper,
      IPSIdMapper idMapper,
      IPSItemWorkflowService itemWorkflowService,
      IPSAssetService assetService,
      IPSWidgetAssetRelationshipService widgetAssetRelationshipService,
      IPSContentMgr contentMgr,
      IPSWorkflowService workflowService,
      IPSPageService pageService,
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
    // Seed protected field directly — setRootName is final but still a this method call in ctor.
    this.rootName = "Folders";
  }

  @Override
  protected String getFullFolderPath(String path) throws PSPathNotFoundServiceException {
    PSPathUtils.validatePath(path);
    var fullFolderPath = FOLDERS_ROOT;
    if (!"/".equals(path)) {
      fullFolderPath = folderHelper.concatPath(fullFolderPath, path);
    }
    return fullFolderPath;
  }

  @Override
  protected String getFolderRoot() {
    return FOLDERS_ROOT;
  }

  @Override
  protected String getInUsePagesResult() {
    return FOLDERS_IN_USE_PAGES;
  }

  @Override
  protected String getNotAuthorizedResult() {
    return FOLDERS_NOT_AUTHORIZED;
  }

  @Override
  protected String getInUseTemplatesResult() {
    return FOLDERS_IN_USE_TEMPLATES;
  }

  /** Internal repository root for classic Rhythmyx folders. */
  public static final String FOLDERS_ROOT = "//Folders";

  /** Finder / pathmanagement prefix (trailing slash is the registry key form). */
  public static final String FOLDERS_FINDER_ROOT = "/Folders";

  public static final String FOLDERS_IN_USE_PAGES = "FoldersInUsePages";

  public static final String FOLDERS_NOT_AUTHORIZED = "FoldersNotAuthorized";

  public static final String FOLDERS_IN_USE_TEMPLATES = "FoldersInUseTemplates";
}
