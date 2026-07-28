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
 * Wraps a session id returned by the membership service.
 *
 * @author Percussion Software
 */
public class PSUserSession {

  /** The session id, may be {@code null} if no session has been issued yet. */
  private String sessionId;

  /** Default constructor for use by serialization frameworks. */
  public PSUserSession() {}

  /**
   * Sets the session id.
   *
   * @param sessionId the session id to set, may not be {@code null}.
   */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  /**
   * Gets the session id.
   *
   * @return the session id, may be {@code null} if no session has been issued yet.
   */
  public String getSessionId() {
    return sessionId;
  }
}
