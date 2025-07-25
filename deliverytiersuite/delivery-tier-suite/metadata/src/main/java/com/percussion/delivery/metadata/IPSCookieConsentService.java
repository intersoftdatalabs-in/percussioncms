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

import com.percussion.delivery.metadata.data.PSCookieConsentQuery;
import java.util.Collection;
import java.util.Map;

/**
 * Service for managing client cookie consent information.
 */
public interface IPSCookieConsentService {

    /**
     * Saves client cookie consent information.
     * @param consentQueries the consent objects to save.
     */
    void save(Collection<PSCookieConsentQuery> consentQueries);

    /**
     * Updates site name for all consent entries.
     * @param oldName previous site name.
     * @param newName new site name.
     */
    void updateOldSiteName(String oldName, String newName);

    /**
     * Gets all cookie consent entries.
     * @return a collection of cookie consent entries.
     */
    Collection<IPSCookieConsent> getAllConsentStats();

    /**
     * Gets cookie consent entries for a site.
     * @param siteName the site name.
     * @return a collection of cookie consent entries.
     */
    Collection<IPSCookieConsent> getAllConsentStatsForSite(String siteName);

    /**
     * Deletes all cookie consent entries.
     * @throws Exception if delete fails.
     */
    void deleteAllCookieConsentEntries() throws Exception;

    /**
     * Deletes all cookie consent entries for the specified site.
     * @param siteName the site name.
     * @throws Exception if delete fails.
     */
    void deleteCookieConsentEntriesForSite(String siteName) throws Exception;

    /**
     * Gets cookie consent totals for all sites.
     * @return map of site name to total.
     * @throws Exception if query fails.
     */
    Map<String, Integer> getAllConsentEntryTotals() throws Exception;

    /**
     * Gets cookie consent totals for a specified site.
     * @param siteName the site name.
     * @return map of service name to total.
     * @throws Exception if query fails.
     */
    Map<String, Integer> getCookieConsentEntryTotalsPerSite(String siteName) throws Exception;
}
