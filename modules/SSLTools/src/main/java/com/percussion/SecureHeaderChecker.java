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

package com.percussion;

import javax.net.ssl.HttpsURLConnection;

/**
 * Utility class that inspects an {@link HttpsURLConnection} for the presence of a known set of
 * security-related HTTP response headers (for example <code>Strict-Transport-Security</code> and
 * <code>X-Frame-Options</code>) and reports which ones are missing.
 */
public class SecureHeaderChecker {

  /**
   * Default constructor; provided so the implicit default constructor has explicit Javadoc and
   * doclint does not warn about its use. This class only exposes static methods, so the constructor
   * is intentionally empty.
   */
  public SecureHeaderChecker() {
    // utility class - no instance state
  }

  private static final String[] secureHeaders = {
    "X-Frame-Options",
    "Content-Security-Policy",
    "X-Content-Type-Options",
    "Strict-Transport-Security",
    "X-XSS-Protection",
    "Cache-Control",
    "Referrer-Policy"
  };

  /**
   * Checks the connection for the presence of secure headers and returns a response describing each
   * header check.
   *
   * @param conn the live HTTPS connection to inspect, may not be <code>null</code>.
   * @return a {@link SecureHeaderCheckResponse} summarizing which secure headers are present and
   *     marking the overall check as failed when any header is missing.
   */
  public static SecureHeaderCheckResponse check(HttpsURLConnection conn) {

    SecureHeaderCheckResponse response = new SecureHeaderCheckResponse();

    for (String h : secureHeaders) {
      String result = conn.getHeaderField(h);
      if (result == null) {
        response.getChecks().put(h, false);
        response.setFailedCheck(true);
      } else {
        response.getChecks().put(h, true);
      }
    }
    return response;
  }
}
