/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import com.google.analytics.admin.v1alpha.AccountSummary;
import com.google.analytics.admin.v1alpha.AnalyticsAdminServiceClient;
import com.google.analytics.admin.v1alpha.ListAccountSummariesRequest;
import com.google.analytics.admin.v1alpha.PropertySummary;
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
 * Provider handler for the Google Analytics service.
 *
 * @author erikserating
 */
public class PSGoogleAnalyticsProviderHandler implements IPSAnalyticsProviderHandler {
  private static final Logger log = LogManager.getLogger(PSGoogleAnalyticsProviderHandler.class);

  /* */
  /* (non-Javadoc)
   * @see com.percussion.analytics.service.impl.IPSAnalyticsProviderHandler#getProfiles(java.lang.String, java.lang.String)
   */
  public Map<String, String> getProfiles(String uid, String password)
      throws PSAnalyticsProviderException, PSValidationException {
    Map<String, String> profiles = new LinkedHashMap<>();
    Map<String, String[]> temp = new TreeMap<>();
    try (AnalyticsAdminServiceClient adminClient =
        PSGoogleAnalyticsProviderHelper.getInstance().getGa4AdminClient(uid, password)) {

      if (adminClient == null) {
        throw new PSAnalyticsProviderException(
            "Failed to initialize Google Analytics Admin Client.");
      }

      boolean accountsFound = false;
      for (AccountSummary account :
          adminClient
              .listAccountSummaries(ListAccountSummariesRequest.newBuilder().build())
              .iterateAll()) {
        accountsFound = true;
        for (PropertySummary property : account.getPropertySummariesList()) {
          log.debug("Account ID: " + account.getAccount());
          log.debug("Property ID: " + property.getProperty());
          log.debug("Property Name: " + property.getDisplayName());

          String pId = property.getProperty(); // Format: properties/123456789
          String title = property.getDisplayName();
          String displayVal = title + " (" + pId + ")";

          String[] val = {pId, displayVal};
          // Sort by display value
          temp.put(displayVal.toLowerCase(), val);
        }
      }

      if (!accountsFound) {
        log.error("No accounts found");
      }

      // Add to linked hash map to maintain sorting
      for (String key : temp.keySet()) {
        String[] v = temp.get(key);
        profiles.put(v[0], v[1]);
      }
    } catch (Exception e) {
      if (e instanceof PSValidationException) {
        throw (PSValidationException) e;
      }
      throw new PSAnalyticsProviderException(
          "Error occurred while attempting to retrieve profiles: " + e.getLocalizedMessage(), e);
    }
    return profiles;
  }

  /* (non-Javadoc)
   * @see com.percussion.analytics.service.impl.IPSAnalyticsProviderHandler#testConnection(java.lang.String, java.lang.String)
   */
  public void testConnection(String uid, String password)
      throws PSValidationException, PSAnalyticsProviderException {
    try {
      try (AnalyticsAdminServiceClient client =
          PSGoogleAnalyticsProviderHelper.getInstance().getGa4AdminClient(uid, password)) {
        if (client == null) {
          throw new PSAnalyticsProviderException(
              "Failed to initialize Google Analytics Admin Client.", CAUSETYPE.INVALID_CREDS);
        }
      }
      getProfiles(uid, password);
    } catch (PSAnalyticsProviderException e) {
      if (e.getCauseType() == CAUSETYPE.NO_ANALYTICS_ACCOUNT) {
        String msg = "No Analytics account found for the specified user.";
        PSValidationErrorsBuilder builder =
            new PSValidationErrorsBuilder(this.getClass().getCanonicalName());
        builder
            .reject(PSAnalyticsProviderException.CAUSETYPE.INVALID_CREDS.toString(), msg)
            .throwIfInvalid();
      }
      throw e;
    } catch (Exception e) {
      throw new PSAnalyticsProviderException(
          "Error occurred while testing connection: " + e.getLocalizedMessage(), e);
    }
  }

  @SuppressWarnings("unused")
  private IPSAnalyticsProviderService providerService;
}
