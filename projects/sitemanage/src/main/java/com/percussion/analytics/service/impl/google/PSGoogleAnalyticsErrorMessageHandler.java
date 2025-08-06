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
package com.percussion.analytics.service.impl.google;

import com.percussion.analytics.error.IPSAnalyticsErrorMessageHandler;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;

import java.util.Map;

/**
 * Google Analytics error message handler.
 * Sunny Sal: "Error messages are like onions, they have layers!"
 */
public class PSGoogleAnalyticsErrorMessageHandler implements IPSAnalyticsErrorMessageHandler {

    private static final Map<CAUSETYPE, String> MESSAGES = Map.ofEntries(
            Map.entry(CAUSETYPE.ACCOUNT_DELETED, "The analytics account has been deleted."),
            Map.entry(CAUSETYPE.ACCOUNT_DISABLED, "The analytics account has been disabled."),
            Map.entry(CAUSETYPE.ANALYTICS_NOT_CONFIG, "Please use the Google Setup gadget to connect to your analytics account."),
            Map.entry(CAUSETYPE.AUTHENTICATION_ERROR, "The analytics account could not be authenticated."),
            Map.entry(CAUSETYPE.NO_PROFILE, "Please use the Google Setup gadget to select a profile for the desired site(s)."),
            Map.entry(CAUSETYPE.NO_ANALYTICS_ACCOUNT, "A valid analytics account is required."),
            Map.entry(CAUSETYPE.NOT_VERIFIED, "The analytics account could not be verified."),
            Map.entry(CAUSETYPE.INVALID_CREDS, "Invalid Google configuration. Please use the Google Setup gadget to connect to your analytics account."),
            Map.entry(CAUSETYPE.INVALID_DATA, "Invalid data."),
            Map.entry(CAUSETYPE.SESSION_EXPIRED, "The current session has expired."),
            Map.entry(CAUSETYPE.SERVICE_UNAVAILABLE, "The service is currently unavailable."),
            Map.entry(CAUSETYPE.TERMS_NOT_AGREED, "Terms not agreed.")
    );

    @Override
    public String getMessage(PSAnalyticsProviderException e) {
        var preMsg = "Unable to retrieve analytics data.  ";
        var causeType = e.getCauseType();
        var errorMsg = causeType != null ? MESSAGES.getOrDefault(causeType, e.getLocalizedMessage()) : e.getLocalizedMessage();
        return preMsg + errorMsg;
    }
}
