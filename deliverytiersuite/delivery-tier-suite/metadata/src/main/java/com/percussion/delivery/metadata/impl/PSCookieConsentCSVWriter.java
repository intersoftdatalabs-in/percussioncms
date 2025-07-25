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

package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSCookieConsent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

/**
 * Merges the contents of a PSCookieConsent object into a CSV file, each as a single row.
 * @author chriswright
 */
public class PSCookieConsentCSVWriter {

    private static final Logger log = LogManager.getLogger(PSCookieConsentCSVWriter.class);

    private final Collection<IPSCookieConsent> entries;

    /**
     * Constructor to initialize the list of cookie consent entries.
     * @param consents the list of entries to convert to CSV.
     */
    public PSCookieConsentCSVWriter(Collection<IPSCookieConsent> consents) {
        this.entries = consents;
    }

    /**
     * Converts each IPSCookieConsent entry to a line in a CSV file.
     * @return a string representation of the CSV file.
     */
    public String writeCSVFile() {
        var sb = new StringBuilder();
        sb.append("Site Name,Service Name,Consent Date,IP Address,Opt In\n");

        for (var consent : entries) {
            try {
                sb.append(String.format("%s,%s,%s,%s,%s\n",
                        consent.getSiteName(),
                        consent.getService(),
                        consent.getConsentDate(),
                        consent.getIP(),
                        consent.getOptIn()));
            } catch (NullPointerException e) {
                log.error("Error writing cookie consent entry to CSV file. Check for NULL entries in the DTS database.", e);
            }
        }
        return sb.toString();
    }
}
