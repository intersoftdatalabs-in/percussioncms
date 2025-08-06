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

package com.percussion.recent.service.rest;

import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetContentType;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.exception.PSDataServiceException;

import java.util.List;

/**
 * Service interface for managing recent items, templates, folders, and asset types.
 */
public interface IPSRecentService {

    /**
     * Finds recent items for the current user.
     * @param ignoreArchivedItems if true, ignores archived items
     */
    List<PSItemProperties> findRecentItem(boolean ignoreArchivedItems);

    /**
     * Finds recent templates for the current user and site.
     */
    List<PSTemplateSummary> findRecentTemplate(String siteName);

    /**
     * Finds recent site folders for the current user and site.
     */
    List<PSPathItem> findRecentSiteFolder(String siteName);

    /**
     * Finds recent asset folders for the current user.
     */
    List<PSPathItem> findRecentAssetFolder();

    /**
     * Finds recent asset types for the current user.
     */
    List<PSWidgetContentType> findRecentAssetType() throws PSDataServiceException;

    /**
     * Adds a recent item for the current user.
     */
    void addRecentItem(String value);

    /**
     * Adds a recent template for the current user and site.
     */
    void addRecentTemplate(String siteName, String value);

    /**
     * Adds a recent site folder for the current user.
     */
    void addRecentSiteFolder(String value);

    /**
     * Adds a recent asset folder for the current user.
     */
    void addRecentAssetFolder(String value);

    /**
     * Adds a recent asset type for the current user.
     */
    void addRecentAssetType(String value);

    /**
     * Adds a recent item for the given user.
     */
    void addRecentItemByUser(String userName, String value);

    /**
     * Adds a recent template for the given user and site.
     */
    void addRecentTemplateByUser(String userName, String siteName, String value);

    /**
     * Adds a recent site folder for the given user.
     */
    void addRecentSiteFolderByUser(String userName, String value);

    /**
     * Adds a recent asset folder for the given user.
     */
    void addRecentAssetFolderByUser(String userName, String value);

    /**
     * Adds a recent asset type for the given user.
     */
    void addRecentAssetTypeByUser(String userName, String value);

    /**
     * Deletes all recent items for the given user.
     */
    void deleteUserRecent(String user);

    /**
     * Deletes all recent items for the given site.
     */
    void deleteSiteRecent(String siteName);

    /**
     * Updates all recent items to use the new site name.
     */
    void updateSiteNameRecent(String oldSiteName, String newSiteName);
}
