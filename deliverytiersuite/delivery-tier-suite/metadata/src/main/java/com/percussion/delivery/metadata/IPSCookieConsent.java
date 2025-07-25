// REFACTORED: CP-JAVA11
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

import java.util.Date;

/**
 * Stores information related to client cookie consent.
 * <ul>
 * <li>Consent Date</li>
 * <li>IP Address</li>
 * <li>Opt In</li>
 * <li>Service Name</li>
 * <li>Site Name</li>
 * </ul>
 */
public interface IPSCookieConsent {

    /**
     * Sets the name of the site for cookie consent.
     * @param siteName the site name.
     */
    void setSiteName(String siteName);

    /**
     * Gets the name of the site.
     * @return the site name.
     */
    String getSiteName();

    /**
     * Sets the IP address.
     * @param ip the IP address.
     */
    void setIP(String ip);

    /**
     * Gets the IP address.
     * @return the IP address.
     */
    String getIP();

    /**
     * Sets the date consent was given.
     * @param consentDate the consent date.
     */
    void setConsentDate(Date consentDate);

    /**
     * Gets the date consent was given.
     * @return the consent date.
     */
    Date getConsentDate();

    /**
     * Sets the approved service name for cookies.
     * @param serviceName the service name.
     */
    void setService(String serviceName);

    /**
     * Gets the approved service name for cookies.
     * @return the service name.
     */
    String getService();

    /**
     * Sets whether the user opted in for cookie consent.
     * @param optIn true if opted in.
     */
    void setOptIn(boolean optIn);

    /**
     * Returns whether the user opted in for cookie consent.
     * @return true if opted in.
     */
    boolean getOptIn();
}
