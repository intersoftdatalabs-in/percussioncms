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

import java.util.HashMap;
import java.util.Map;

/**
 * Response returned by {@link SecureHeaderChecker#check(HttpsURLConnection)} that reports which
 * secure HTTP response headers were present on the inspected connection and whether any required
 * header was missing.
 */
public class SecureHeaderCheckResponse {

  /**
   * Default constructor; provided so the implicit default constructor has explicit Javadoc and
   * doclint does not warn about its use.
   */
  public SecureHeaderCheckResponse() {
    // POJO holding check results - no initialization required
  }

  private boolean failedCheck = false;

  private Map<String, Boolean> checks = new HashMap<>();

  /**
   * Returns the map of header name to presence flag produced by the check.
   *
   * @return a mutable map of secure-header name to a boolean indicating whether that header was
   *     found on the response; never <code>null</code>.
   */
  public Map<String, Boolean> getChecks() {
    return checks;
  }

  /**
   * Replaces the map of header check results held by this response.
   *
   * @param checks a map of secure-header name to presence flag, may not be <code>null</code>.
   */
  public void setChecks(Map<String, Boolean> checks) {
    this.checks = checks;
  }

  /**
   * Whether the overall secure-header check failed.
   *
   * @return <code>true</code> when at least one required secure header was missing from the
   *     inspected connection.
   */
  public boolean isFailedCheck() {
    return failedCheck;
  }

  /**
   * Sets the overall pass/fail flag for the secure-header check.
   *
   * @param failedCheck <code>true</code> to mark the check as failed (at least one required header
   *     was missing), <code>false</code> to mark it as passed.
   */
  public void setFailedCheck(boolean failedCheck) {
    this.failedCheck = failedCheck;
  }
}
