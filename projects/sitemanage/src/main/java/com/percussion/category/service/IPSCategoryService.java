/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.category.service;

import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryNode;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;

/**
 * Category service is responsible for creating, editing, and finding categories.
 * <p>
 * This interface is Java 11 compatible and follows Google Java Style Guide.
 * All method signatures are backward compatible.
 */
public interface IPSCategoryService {

    /**
     * Gets a list of all categories in the system for the specified site.
     *
     * @param siteName the name of the site; may be {@code null} for all sites.
     * @return the category tree for the site; never {@code null}.
     * @throws PSDataServiceException if an error occurs retrieving categories.
     */
    PSCategory getCategoryList(String siteName) throws PSDataServiceException;

    /**
     * Gets the category tree object for a site.
     *
     * @param siteName        the name of the site; may be {@code null} for all sites.
     * @param rootPath        the root path to search from; may be {@code null} for the root.
     * @param includeDeleted  whether to include deleted categories.
     * @param includeSelectable whether to include non-selectable categories.
     * @return the category tree for the site; never {@code null}.
     * @throws PSDataServiceException if an error occurs retrieving categories.
     */
    PSCategory getCategoryTreeForSite(String siteName, String rootPath, boolean includeDeleted, boolean includeSelectable)
            throws PSDataServiceException;

    /**
     * Adds, updates, or marks as deleted a category in the respective XML.
     *
     * @param category the category to update; must not be {@code null}.
     * @param siteName the site name; may be {@code null}.
     * @return the updated list of categories; never {@code null}.
     * @throws PSValidationException if validation fails.
     */
    PSCategory updateCategories(PSCategory category, String siteName) throws PSValidationException;

    /**
     * Gets information about whether the category tab in the Administration UI is being used by an admin.
     *
     * @return lock details as a JSON string if one exists; otherwise, returns an empty JSON object.
     */
    String getLockInfo();

    /**
     * Creates a file indicating that an admin is using the category tab in the Administration UI.
     *
     * @param date the current date as a string; must not be {@code null}.
     */
    void lockCategoryTab(String date);

    /**
     * Deletes the file that has the admin information for the category tab lock.
     */
    void removeCategoryTabLock();

    /**
     * Updates a category in the DTS when it is modified for any of its properties.
     * The method is responsible for updating the relevant DTS based on the request.
     *
     * @param siteName       the site in which the category is modified; must not be {@code null}.
     * @param deliveryServer the delivery server ("Staging" or "Production"); must not be {@code null}.
     */
    void updateCategoryInDTS(String siteName, String deliveryServer);

    /**
     * Finds a category node by site and root path, with options for deleted and selectable nodes.
     *
     * @param siteName             the site name; may be {@code null}.
     * @param rootPath             the root path; may be {@code null}.
     * @param includeDeleted       whether to include deleted nodes.
     * @param includeNotSelectable whether to include non-selectable nodes.
     * @return the found category node, or {@code null} if not found.
     */
    PSCategoryNode findCategoryNode(String siteName, String rootPath, boolean includeDeleted, boolean includeNotSelectable);
}
