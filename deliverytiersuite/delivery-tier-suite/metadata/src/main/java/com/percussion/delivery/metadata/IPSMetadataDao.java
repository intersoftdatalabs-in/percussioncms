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

// REFACTORED: CP-JAVA11

package com.percussion.delivery.metadata;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataEntry;

/**
 * Data access object for metadata entries.
 * Provides CRUD operations and utility methods for indexed metadata.
 */
public interface IPSMetadataDao {

    /**
     * Deletes multiple metadata index entries.
     * @param pagePaths collection of page path strings identifying entries to delete.
     */
    void delete(Collection<String> pagePaths);

    /**
     * Deletes a single metadata entry.
     * @param pagePath the page path of the metadata entry to delete.
     * @return true if a delete operation occurred.
     */
    boolean delete(String pagePath);

    /**
     * Saves multiple metadata entries.
     * @param entries collection of entries to save.
     */
    void save(Collection<IPSMetadataEntry> entries);

    /**
     * Saves a single metadata entry.
     * @param entry metadata entry to store.
     */
    void save(IPSMetadataEntry entry);

    /**
     * Deletes all metadata entries and their properties from the database.
     */
    void deleteAllMetadataEntries();

    /**
     * Deletes all entries for a site name (e.g., after site rename).
     * @param prevSiteName previous site name.
     * @param newSiteName new site name.
     */
    void deleteBySite(String prevSiteName, String newSiteName);

    /**
     * Returns all metadata entries.
     * @return list of all metadata entries.
     */
    List<IPSMetadataEntry> getAllEntries();

    /**
     * Finds a metadata entry by page path.
     * @param pagePath the page path to search.
     * @return the metadata entry, or null if not found.
     */
    IPSMetadataEntry findEntry(String pagePath);

    /**
     * Gets all sites that have an indexed entry.
     * @return list of site names.
     */
    List<String> getAllSites();

    /**
     * Gets all indexed directories.
     * @return set of indexed directory paths.
     */
    Set<String> getAllIndexedDirectories();

    /**
     * Checks if any entries are "dirty" (field values differ from DB).
     * @param entries collection of entries to check.
     * @return true if any entry is dirty.
     */
    boolean hasDirtyEntries(Collection<IPSMetadataEntry> entries);

    /**
     * Updates category property values for entries.
     * @param oldCategoryName previous category name.
     * @param newCategoryName new category name.
     * @return number of updated rows.
     */
    int updateByCategoryProperty(String oldCategoryName, String newCategoryName);
}
