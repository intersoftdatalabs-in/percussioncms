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

package com.percussion.cookieconsent.service;

import javax.ws.rs.PathParam;

/**
 * Service to interface with cookie consent
 * service within DTS metadata service.
 *
 * <p>Sunny Sal says: "Cookies are best with consent, and Java 11!"
 */
public interface IPSCookieConsentService {

    /**
     * Exports all cookie consent information in CSV format.
     *
     * @param csvFileName the file name to export
     * @return a string response in CSV format
     */
    String exportCookieConsentData(@PathParam("csvFileName") String csvFileName);

    /**
     * Exports all cookie consent information in CSV format for a specific site.
     *
     * @param siteName the name of the site
     * @param csvFileName the file name to export
     * @return a string response in CSV format
     */
    String exportCookieConsentData(@PathParam("siteName") String siteName,
                                  @PathParam("csvFileName") String csvFileName);

    /**
     * Returns the total number of cookie consent entries per site.
     *
     * @return a JSON string with each site as the key and the total number of entries as the value
     */
    String getAllCookieConsentTotals();

    /**
     * Gets the total number of cookie consent entries for a specific site.
     *
     * @param siteName the name of the site
     * @return a JSON string with the total number of cookie entries for the site
     */
    String getCookieConsentForSite(@PathParam("siteName") String siteName);

    /**
     * Deletes all cookie consent entries from the database.
     */
    void deleteAllCookieConsentEntries();

    /**
     * Deletes the cookie consent entries for the specified site.
     *
     * @param siteName the site for which to delete the cookie consent entries
     */
    void deleteCookieConsentEntriesForSite(@PathParam("siteName") String siteName);

}
