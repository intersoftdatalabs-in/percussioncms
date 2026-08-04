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

import com.percussion.delivery.metadata.data.PSCookieConsentQuery;
import java.util.Collection;
import java.util.Map;

/**
 * Service contract for the DTS cookie-consent tracking feature. Records and aggregates the
 * opt-in/opt-out decisions captured from clients visiting published pages, both globally and per
 * site.
 *
 * @author chriswright
 */
public interface IPSCookieConsentService {

  /**
   * Saves the client cookie consent information.
   *
   * @param consentQueries the consent queries to save.
   */
  public void save(Collection<PSCookieConsentQuery> consentQueries);

  /**
   * Saves the client cookie consent information.
   *
   * @param oldName the old site name to replace.
   * @param newName the new site name to use.
   */
  public void updateOldSiteName(String oldName, String newName);

  /**
   * Gets a list of cookie consent entries in the database.
   *
   * @see IPSCookieConsent
   * @return a list of cookie consent entries. May be empty, never <code>null</code>.
   */
  public Collection<IPSCookieConsent> getAllConsentStats();

  /**
   * Gets a list of cookie consent entries for site.
   *
   * @param siteName - the name of the site to get the entries for.
   * @return a list of cookie consent entries.
   */
  public Collection<IPSCookieConsent> getAllConsentStatsForSite(String siteName);

  /**
   * Deletes all cookie consent entries from the database.
   *
   * @throws Exception if the underlying database operation fails.
   */
  public void deleteAllCookieConsentEntries() throws Exception;

  /**
   * Deletes all cookie consent entries for the specified site.
   *
   * @param siteName - the site in which to delete the entries for.
   * @throws Exception if the underlying database operation fails.
   */
  public void deleteCookieConsentEntriesForSite(String siteName) throws Exception;

  /**
   * Gets cookie consent totals for all sites.
   *
   * @return A map which contains corresponding siteName/cookie totals.
   * @throws Exception if the underlying database operation fails.
   */
  public Map<String, Integer> getAllConsentEntryTotals() throws Exception;

  /**
   * Gets cookie consent entries / totals for specified site. Key/value pair with key being
   * service/cookie name value being total approved.
   *
   * @param siteName - the name of the site to find entries for.
   * @return A map which contains key/value pairs for service/cookie name and total entries.
   * @throws Exception if the underlying database operation fails.
   */
  public Map<String, Integer> getCookieConsentEntryTotalsPerSite(String siteName) throws Exception;
}
