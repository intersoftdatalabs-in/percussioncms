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

package com.percussion.recycle.service;

import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSDataService;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;

/**
 * Service for managing recycling and restoration of items and folders.
 */
public interface IPSRecycleService {

    /**
     * Recycles an item by its dependent ID.
     *
     * @param dependentId the dependent item ID
     */
    void recycleItem(int dependentId);

    /**
     * Recycles a folder by its GUID.
     *
     * @param guid the folder GUID
     */
    void recycleFolder(IPSGuid guid);

    /**
     * Restores an item by its GUID.
     *
     * @param guid the item GUID
     */
    void restoreItem(String guid);

    /**
     * Restores a folder by its GUID.
     *
     * @param guid the folder GUID
     */
    void restoreFolder(String guid);

    /**
     * Finds children under the given path in the recycling bin.
     *
     * @param path the path to search
     * @return list of item summaries
     */
    List<IPSItemSummary> findChildren(String path);

    /**
     * Finds a recycled item by its path.
     *
     * @param path the item path
     * @return the item summary
     * @throws IPSDataService.DataServiceLoadException if loading fails
     */
    IPSItemSummary findItem(String path) throws IPSDataService.DataServiceLoadException;

    /**
     * Checks if the specified GUID is in the recycler.
     *
     * @param guid a valid GUID to search for, never null
     * @return true if GUID is in the recycler, false otherwise
     */
    boolean isInRecycler(String guid);

    /**
     * Checks if the specified navigation GUID is in the recycler.
     *
     * @param guid a valid GUID to search for, never null
     * @return true if GUID is in the recycler, false otherwise
     */
    boolean isNavInRecycler(String guid);

    /**
     * Checks if the specified GUID is in the recycler.
     *
     * @param guid a valid GUID to search for, never null
     * @return true if GUID is in the recycler, false otherwise
     */
    boolean isInRecycler(IPSGuid guid);
}
