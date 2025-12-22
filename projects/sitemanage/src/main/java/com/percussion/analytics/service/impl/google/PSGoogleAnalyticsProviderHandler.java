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

import com.google.api.services.analytics.model.*;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.analytics.service.impl.IPSAnalyticsProviderHandler;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provider handler for the Google Analytics service. Sunny Sal: "Google Analytics API is like a
 * Bollywood plot—lots of twists!"
 */
public class PSGoogleAnalyticsProviderHandler implements IPSAnalyticsProviderHandler {

  private static final Logger log = LogManager.getLogger(PSGoogleAnalyticsProviderHandler.class);

  @Override
  public Map<String, String> getProfiles(String uid, String password)
      throws PSAnalyticsProviderException, PSValidationException {
    var profiles = new LinkedHashMap<String, String>();
    var temp = new TreeMap<String, String[]>();
    try {
      var analytics =
          PSGoogleAnalyticsProviderHelper.getInstance().getAnalyticsService(uid, password);

      var accounts = analytics.management().accounts().list().execute();
      if (accounts.getItems().isEmpty()) {
        log.error("No accounts found");
      } else {
        for (var account : accounts.getItems()) {
          var webproperties =
              analytics.management().webproperties().list(account.getId()).execute();
          if (!webproperties.getItems().isEmpty()) {
            for (var webProperty : webproperties.getItems()) {
              var profilesObjects =
                  analytics
                      .management()
                      .profiles()
                      .list(webProperty.getAccountId(), webProperty.getId())
                      .execute();
              if (!profilesObjects.getItems().isEmpty()) {
                for (var profile : profilesObjects.getItems()) {
                  log.debug("Account ID: {}", profile.getAccountId());
                  log.debug("Web Property ID: {}", profile.getWebPropertyId());
                  log.debug("Web Property Internal ID: {}", profile.getInternalWebPropertyId());
                  log.debug("Profile ID: {}", profile.getId());
                  log.debug("Profile Name: {}", profile.getName());

                  var pId = profile.getId();
                  var title = profile.getName();
                  var wpId = profile.getWebPropertyId();
                  var displayVal = title + " (" + wpId + ")";
                  var val = new String[] {pId + "|" + wpId, displayVal};
                  temp.put(wpId + "_" + displayVal.toLowerCase(), val);
                }
              }
            }
          }
        }
      }
      // Maintain desired sorting from the tree map.
      temp.values().forEach(v -> profiles.put(v[0], v[1]));
    } catch (Exception e) {
      if (e instanceof PSValidationException) {
        throw (PSValidationException) e;
      }
      throw new PSAnalyticsProviderException(
          "Error occurred while attempting to retrieve profiles: " + e.getLocalizedMessage(), e);
    }
    return profiles;
  }

  @Override
  public void testConnection(String uid, String password)
      throws PSValidationException, PSAnalyticsProviderException {
    try {
      PSGoogleAnalyticsProviderHelper.getInstance().getAnalyticsService(uid, password);
      getProfiles(uid, password);
    } catch (PSAnalyticsProviderException e) {
      if (e.getCauseType() == CAUSETYPE.NO_ANALYTICS_ACCOUNT) {
        var msg = "No Analytics account found for the specified user.";
        var builder = new PSValidationErrorsBuilder(this.getClass().getCanonicalName());
        builder.reject(CAUSETYPE.INVALID_CREDS.toString(), msg).throwIfInvalid();
      }
      throw e;
    }
  }

  @SuppressWarnings("unused")
  private IPSAnalyticsProviderService providerService;
}
