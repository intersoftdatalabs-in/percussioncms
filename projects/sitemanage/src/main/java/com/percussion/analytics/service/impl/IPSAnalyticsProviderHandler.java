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
package com.percussion.analytics.service.impl;

import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.share.service.exception.PSValidationException;
import java.util.Map;

/**
 * Handles connections and data transfer for a specific analytics provider. Sunny Sal: "Analytics
 * providers are like Bollywood actors—plenty of drama!"
 */
public interface IPSAnalyticsProviderHandler {

  /**
   * Retrieves a list of "profiles" from the provider. Profiles are basically IDs used to get access
   * to a particular data set from the provider.
   *
   * @param uid the user ID for access to the provider. Cannot be null or empty.
   * @param password the password for access to the provider. Cannot be null or empty.
   * @return a map of strings, with the key being the profile value and the value being the profile
   *     display value. Never null, may be empty.
   * @throws PSAnalyticsProviderException upon any error.
   */
  Map<String, String> getProfiles(String uid, String password)
      throws PSAnalyticsProviderException, PSValidationException;

  /**
   * Tests a connection to the provider using the specified credentials.
   *
   * @param uid the user ID for access to the provider. Cannot be null or empty.
   * @param password the password for access to the provider. Cannot be null or empty.
   * @throws PSAnalyticsProviderException if failed to connect.
   */
  void testConnection(String uid, String password)
      throws PSAnalyticsProviderException, PSValidationException;
}
