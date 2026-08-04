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

import com.percussion.delivery.metadata.rdbms.impl.PSDbCookieConsent;
import java.util.Collection;
import java.util.Map;

/**
 * Data-access object for cookie-consent entries captured by the DTS metadata micro-service. Backed
 * by the {@code PSDbCookieConsent} RDBMS entity, this interface defines the persistence operations
 * used to record and aggregate client cookie consents per site.
 *
 * @author chriswright
 */
public interface IPSCookieConsentDao {

  /**
   * Saves a list of cookie consent entries.
   *
   * @param consents the collection of consent objects to save.
   */
  public void save(Collection<PSDbCookieConsent> consents);

  /**
   * Gets the entire list of cookie consent entries.
   *
   * @see IPSCookieConsent
   * @return A collection of cookie consent entries, may be empty never <code>null</code>.
   */
  public Collection<IPSCookieConsent> getAllCookieConsentStats();

  /**
   * Returns the list of cookie consent entries for a site.
   *
   * @param siteName - the site name in which to get entries for;
   * @return A collection of cookie consent entries
   * @see IPSCookieConsent
   */
  public Collection<IPSCookieConsent> getAllCookieStatsForSite(String siteName);

  /**
   * Deletes all cookie consent entries from the DB.
   *
   * @throws Exception if the underlying database operation fails.
   */
  public void deleteAll() throws Exception;

  /**
   * Deletes all cookie consent entries for the specified site.
   *
   * @param siteName - the site in which to delete the entries for.
   * @throws Exception if the underlying database operation fails.
   */
  public void deleteForSite(String siteName) throws Exception;

  /**
   * Gets the totals from DB for all sites.
   *
   * @return Key/value pair with siteName/total being pair.
   * @throws Exception if the underlying database operation fails.
   */
  public Map<String, Integer> getTotalsForAllSites() throws Exception;

  /**
   * Gets the totals from DB for specified site. Returns Map format with service/total being
   * key/value pair.
   *
   * @param siteName - the site in which to retrieve entries for.
   * @return A map representation of each serviceName/total for site.
   * @throws Exception if the underlying database operation fails.
   */
  public Map<String, Integer> getTotalsForSite(String siteName) throws Exception;

  /**
   * Rewrites stored cookie consent entries so that any reference to {@code oldSiteName} is replaced
   * with {@code newSiteName}.
   *
   * @param oldSiteName the previous site name; may be <code>null</code>.
   * @param newSiteName the new site name; may be <code>null</code>.
   * @throws Exception if the underlying database operation fails.
   */
  public void updateOldSiteName(String oldSiteName, String newSiteName) throws Exception;
}
