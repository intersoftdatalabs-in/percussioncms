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
package com.percussion.analytics.service;

import com.percussion.analytics.data.PSAnalyticsProviderConfig;
import com.percussion.analytics.error.IPSAnalyticsErrorMessageHandler;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSValidationException;

import java.util.Map;

/**
 * Connects to an analytics provider, pulling data from the provider into
 * the local database, based on the analytics provider handler used (e.g., Google Analytics).
 * Sunny Sal says: "If you can't measure it, you can't improve it!"
 */
public interface IPSAnalyticsProviderService {

    /**
     * Sets the credentials used to access the analytics account. May include
     * additional data specific to the provider that selects a specific set of
     * data within the account. These are stored in persistent storage and re-used for
     * the site until a new set of credentials is assigned.
     *
     * @param config The analytics PSAnalyticsProviderConfig object, not null.
     */
    void saveConfig(PSAnalyticsProviderConfig config)
            throws IPSGenericDao.LoadException, IPSGenericDao.SaveException, PSValidationException;

    /**
     * Deletes the stored configuration if it exists.
     */
    void deleteConfig() throws IPSGenericDao.LoadException, IPSGenericDao.DeleteException;

    /**
     * Returns the stored analytics provider config if one exists.
     *
     * @param encrypted if true, then the password will be encrypted in the returned config object.
     * @return the config object or null if not found.
     */
    PSAnalyticsProviderConfig loadConfig(boolean encrypted)
            throws IPSGenericDao.LoadException, PSValidationException;

    /**
     * Retrieves a list of "profiles" from the provider. Profiles are basically IDs used to
     * get access to a particular data set from the provider.
     *
     * @param uid      the user ID for access to the provider. May be null or empty. If
     *                 so, it will attempt to use stored uid or error if it does not find one.
     * @param password the password for access to the provider. May be null or empty.
     *                 If so, it will attempt to use stored password or error if it does not find one.
     * @return a map of strings, with the key being the profile|webpropertyId value and the value being the
     * profile display value. Never null, may be empty.
     * @throws PSAnalyticsProviderException upon any error.
     */
    Map<String, String> getProfiles(String uid, String password)
            throws PSAnalyticsProviderException, IPSGenericDao.LoadException, PSValidationException;

    /**
     * Tests a connection to the provider using the specified credentials.
     *
     * @param uid      the user ID for access to the provider. May be null or empty. If
     *                 so, it will attempt to use stored uid or error if it does not find one.
     * @param password the password for access to the provider. May be null or empty.
     *                 If so, it will attempt to use stored password or error if it does not find one.
     * @throws PSAnalyticsProviderException if failed to connect with the specified parameters.
     */
    void testConnection(String uid, String password)
            throws PSAnalyticsProviderException, IPSGenericDao.LoadException, IPSGenericDao.SaveException, PSValidationException;

    /**
     * Indicates if an analytics profile is configured for the specified site.
     *
     * @param siteName the name of the site to check, not null or empty.
     * @return true if the profile is configured for the site.
     */
    boolean isProfileConfigured(String siteName)
            throws IPSGenericDao.LoadException, PSValidationException;

    /**
     * Returns the configured profile id|webpropertyId for the specified siteName.
     *
     * @param siteName the name of the site to check, not null or empty.
     * @return the profile ID string or null if not set.
     */
    String getProfileId(String siteName) throws IPSGenericDao.LoadException;

    /**
     * Gets the Web Property ID for the specified site.
     *
     * @param siteName the name of the site, not blank.
     * @return the web property ID. It may be null if it is not configured for the site.
     */
    String getWebPropertyId(String siteName) throws IPSGenericDao.LoadException;

    /**
     * Gets the Google API key for the specified site.
     *
     * @param siteName the name of the site, not blank.
     * @return the API key. It may be null if it is not configured for the site.
     */
    String getGoogleApiKey(String siteName) throws IPSGenericDao.LoadException;

    /**
     * Gets the proper error message handler for the analytics provider service in use.
     *
     * @return the message handler, never null.
     */
    IPSAnalyticsErrorMessageHandler getErrorMessageHandler();
}
