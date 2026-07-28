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
package com.percussion.membership.data;

/**
 * Represents the result of a member login attempt.
 *
 * @author JaySeletz
 */
public class PSLoginResult extends PSMembershipResult {
  private String sessionId;

  /**
   * Constructs a new login result.
   *
   * @param status the status of the login operation, never {@code null}.
   * @param message the detail message associated with the status, may be {@code null}.
   * @param sessionId the session id issued for the login, may be {@code null} when login failed.
   */
  public PSLoginResult(STATUS status, String message, String sessionId) {
    super(status, message);
    this.sessionId = sessionId;
  }

  /**
   * Gets the session id issued for the login.
   *
   * @return the session id, may be {@code null} when login failed.
   */
  public String getSessionId() {
    return sessionId;
  }
}
