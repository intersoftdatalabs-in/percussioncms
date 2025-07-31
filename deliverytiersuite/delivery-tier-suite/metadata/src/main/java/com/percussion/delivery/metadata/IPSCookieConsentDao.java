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

package com.percussion.delivery.metadata;

import com.percussion.delivery.metadata.rdbms.impl.PSDbCookieConsent;
import java.util.Collection;
import java.util.Map;

/**
 * Data access object for cookie consent entries.
 */
public interface IPSCookieConsentDao {

    /**
     * Saves a list of cookie consent entries.
     * @param consents the collection of consent objects to save.
     */
    void save(Collection<PSDbCookieConsent> consents);

    /**
     * Gets all cookie consent entries.
     * @return a collection of cookie consent entries.
     */
    Collection<IPSCookieConsent> getAllCookieConsentStats();

    /**
     * Gets cookie consent entries for a site.
     * @param siteName the site name.
     * @return a collection of cookie consent entries.
     */
    Collection<IPSCookieConsent> getAllCookieStatsForSite(String siteName);

    /**
     * Deletes all cookie consent entries.
     * @throws Exception if delete fails.
     */
    void deleteAll() throws Exception;

    /**
     * Deletes all cookie consent entries for the specified site.
     * @param siteName the site name.
     * @throws Exception if delete fails.
     */
    void deleteForSite(String siteName) throws Exception;

    /**
     * Gets totals for all sites.
     * @return map of site name to total.
     * @throws Exception if query fails.
     */
    Map<String, Integer> getTotalsForAllSites() throws Exception;

    /**
     * Gets totals for a specified site.
     * @param siteName the site name.
     * @return map of service name to total.
     * @throws Exception if query fails.
     */
    Map<String, Integer> getTotalsForSite(String siteName) throws Exception;

    /**
     * Updates site name for all entries.
     * @param oldSiteName previous site name.
     * @param newSiteName new site name.
     * @throws Exception if update fails.
     */
    void updateOldSiteName(String oldSiteName, String newSiteName) throws Exception;
}
