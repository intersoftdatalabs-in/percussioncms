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

package com.percussion.delivery.metadata;

import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataEntry;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Data-access object for indexed metadata entries. Backed by the {@code PSDbMetadataEntry} RDBMS
 * entity, this interface defines the persistence operations used by the DTS metadata indexer to
 * store, query and remove per-page metadata entries and their associated properties.
 */
public interface IPSMetadataDao {

  /**
   * Deletes multiple metadata index entries.
   *
   * @param entriesToDelete collection of page path strings that identifies the index entries.
   *     Cannot be <code>null</code> may be empty.
   */
  public void delete(Collection<String> entriesToDelete);

  /**
   * Deletes a single metadata entry.
   *
   * @param pagepath The pagepath of the metadata entry that should be deleted. Cannot be <code>null
   *     </code> nor empty.
   * @return <code>true</code> if a delete operation actually occurred.
   */
  public boolean delete(String pagepath);

  /**
   * Saves multiple metadata entries.
   *
   * @param entries collection of entries to be saved, cannot be <code>null</code>, may be empty.
   */
  public void save(Collection<IPSMetadataEntry> entries);

  /**
   * Saves a single metadata entry.
   *
   * @param entry A {@link PSDbMetadataEntry} instance to store in the database. Cannot be <code>
   *     null</code>.
   */
  public void save(IPSMetadataEntry entry);

  /** Deletes all metadata entries from the database, along with their metadata properties. */
  public void deleteAllMetadataEntries();

  /**
   * Deletes all entries for a site name. Originally implemented to delete stale entries for a site
   * that was renamed. Fixing an issue with these entries not being removed on publish after the
   * site is renamed.
   *
   * @param prevSiteName the name of the site before site rename.
   * @param newSiteName the name of the site after rename.
   */
  public void deleteBySite(String prevSiteName, String newSiteName);

  /**
   * Returns all metadata entries.
   *
   * @return A list with all metadata entries. Never <code>null</code>, may be empty.
   */
  public List<IPSMetadataEntry> getAllEntries();

  /**
   * Finds a metadata entry according to the given pagepath.
   *
   * @param pagepath The pagepath of the metadata entry to return. Cannot be <code>null</code> nor
   *     empty.
   * @return The metadata entry with the pagepath specified. If no entry is found, null is returned.
   */
  public IPSMetadataEntry findEntry(String pagepath);

  /**
   * Get a list of all sites that have an entry indexed.
   *
   * @return never <code>null</code> may be empty.
   */
  public List<String> getAllSites();

  /**
   * Returns the set of indexed directory paths known to the indexer.
   *
   * @return a set of directory paths, never <code>null</code>, may be empty.
   */
  public Set<String> getAllIndexedDirectories();

  /**
   * Determines whether any of the supplied metadata entries differ from the currently persisted
   * state and therefore need to be re-indexed.
   *
   * @param entries the entries to compare against the persisted state; may not be <code>null</code>
   *     .
   * @return <code>true</code> if at least one entry is dirty.
   */
  public boolean hasDirtyEntries(Collection<IPSMetadataEntry> entries);

  /**
   * Rewrites the stored {@code perc:category} property values so any reference to {@code
   * oldCategoryName} is replaced with {@code newCategoryName}.
   *
   * @param oldCategoryName the previous category name; may be <code>null</code>.
   * @param newCategoryName the new category name; may be <code>null</code>.
   * @return the number of property rows updated, never negative.
   */
  public int updateByCategoryProperty(String oldCategoryName, String newCategoryName);
}
