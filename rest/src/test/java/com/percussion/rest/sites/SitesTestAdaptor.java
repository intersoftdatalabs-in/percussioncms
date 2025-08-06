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

package com.percussion.rest.sites;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class SitesTestAdaptor implements ISiteAdaptor {

    /**
     * Find all sites.
     *
     * @return SiteList
     */
    @Override
    public SiteList findAllSites() {
        return null;
    }

    /**
     * Save a site.
     *
     * @param site the site to save
     */
    @Override
    public void saveSite(Site site) {
        // No-op for test adaptor
    }

    /**
     * Find site by name.
     *
     * @param name the site name
     * @return the site, or null if not found
     */
    @Override
    public Site findByName(String name) {
        return null;
    }

    /**
     * Find site by GUID.
     *
     * @param guid the site GUID
     * @return the site, or null if not found
     */
    @Override
    public Site findByGuid(String guid) {
        return null;
    }

    /**
     * Delete the site.
     *
     * @param site the site to delete
     */
    @Override
    public void deleteSite(Site site) {
        // No-op for test adaptor
    }

    /**
     * Create a new site.
     *
     * @return the new site
     */
    @Override
    public Site createSite() {
        return null;
    }
}
