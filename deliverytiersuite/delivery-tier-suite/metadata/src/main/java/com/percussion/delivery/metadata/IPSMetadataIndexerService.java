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

import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataEntry;
import com.percussion.delivery.listeners.IPSServiceDataChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Indexer for metadata information from pages.
 * Provides methods to save, update, delete, and listen for metadata changes.
 */
public interface IPSMetadataIndexerService {

    /**
     * Gets all indexed directories and subdirectories.
     * @return set of indexed directory paths.
     */
    Set<String> getAllIndexedDirectories();

    /**
     * Deletes multiple metadata index entries.
     * @param pagePaths collection of page path strings to delete.
     */
    void delete(Collection<String> pagePaths);

    /**
     * Deletes a single metadata entry.
     * @param pagePath the page path to delete.
     */
    void delete(String pagePath);

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
     * Adds a metadata listener to the service.
     * @param listener listener to add.
     */
    void addMetadataListener(IPSServiceDataChangeListener listener);

    /**
     * Removes a metadata listener from the service.
     * @param listener listener to remove.
     */
    void removeMetadataListener(IPSServiceDataChangeListener listener);

    /**
     * Deletes all metadata entries and their properties from the database.
     */
    void deleteAllMetadataEntries();

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
}
