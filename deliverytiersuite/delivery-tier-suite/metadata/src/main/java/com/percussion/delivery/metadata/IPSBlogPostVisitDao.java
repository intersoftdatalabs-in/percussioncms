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
import com.percussion.delivery.metadata.rdbms.impl.PSDbBlogPostVisit;

/**
 * Data access object for blog post visits.
 */
public interface IPSBlogPostVisitDao {

    /**
     * Deletes multiple page visit entries.
     * @param pagepaths collection of page path strings whose visits need to be deleted.
     */
    void delete(Collection<String> pagepaths);

    /**
     * Deletes a single page visit entry.
     * @param pagepath the page path of the visit entry to delete.
     * @return true if a delete operation occurred.
     */
    boolean delete(String pagepath);

    /**
     * Saves multiple page visit entries.
     * @param visits collection of entries to be saved.
     */
    void save(Collection<IPSBlogPostVisit> visits);

    /**
     * Saves a single page visit entry.
     * @param visit a {@link IPSBlogPostVisit} instance to store.
     */
    void save(IPSBlogPostVisit visit);

    /**
     * Returns list of most visited pages within the supplied number of days, limited to the supplied amount.
     * @param sectionPath the path of the section.
     * @param days the number of days to filter.
     * @param limit the result limit.
     * @param sortOrder sort order ("asc" or "desc").
     * @return a list of page paths.
     */
    List<String> getTopVisitedPages(String sectionPath, int days, int limit, String sortOrder);

    /**
     * Finds page visits by page path.
     * @param pagepath the page path to search.
     * @return list of matching visits, or empty if none found.
     */
    List<PSDbBlogPostVisit> findBlogPostVisit(String pagepath);

    /**
     * Updates blog post visits after a site rename.
     * @param prevSiteName previous site name.
     * @param newSiteName new site name.
     * @throws Exception if update fails.
     */
    void updatePostsAfterSiteRename(String prevSiteName, String newSiteName) throws Exception;
}
