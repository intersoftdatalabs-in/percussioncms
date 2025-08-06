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

package com.percussion.queue;

import com.percussion.queue.impl.PSSiteQueue;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;

import java.util.List;

/**
 * Service for managing the page import queue for sites.
 */
public interface IPSPageImportQueue {

    /**
     * Adds cataloged page IDs for a site and user agent.
     *
     * @param site the site, not null
     * @param userAgent the user agent string, not null
     * @param ids list of page IDs to add, not null
     */
    void addCatalogedPageIds(PSSite site, String userAgent, List<Integer> ids);

    /**
     * Gets the IDs of pages currently being imported for a site.
     *
     * @param siteId the site ID
     * @return list of importing page IDs
     */
    List<Integer> getImportingPageIds(Long siteId);

    /**
     * Gets the cataloged page IDs for a site.
     *
     * @param siteId the site ID
     * @return list of cataloged page IDs
     */
    List<Integer> getCatalogedPageIds(Long siteId);

    /**
     * Gets the imported page IDs for a site.
     *
     * @param siteId the site ID
     * @return list of imported page IDs
     */
    List<Integer> getImportedPageIds(Long siteId);

    /**
     * Adds an imported page ID for a site.
     *
     * @param siteId the site ID
     * @param id the imported page ID
     */
    void addImportedId(Long siteId, Integer id);

    /**
     * Removes the specified page from the specified site.
     *
     * @param siteName the name of the site, not blank
     * @param pageId the imported page ID, not blank
     */
    void removeImportPage(String siteName, String pageId);

    /**
     * Gets the page IDs that are cached for the specified site context.
     *
     * @param siteContext the site import context, not null
     * @return the page IDs, cloned from the cached info, never null
     */
    PSSiteQueue getPageIds(PSSiteImportCtx siteContext);

    /**
     * Gets the page IDs that are cached for the specified site.
     *
     * @param siteId the site ID
     * @return the page IDs, cloned from the cached info, never null
     */
    PSSiteQueue getPageIds(Long siteId);

    /**
     * Marks the site queue as dirty for the given site ID.
     *
     * @param siteId the site ID
     */
    void dirtySiteQueue(Long siteId);
}
