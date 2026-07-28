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
 * Represents the result of a call to look up a user summary.
 *
 * @author JaySeletz
 */
public class PSGetUserResult extends PSMembershipResult {
  private PSUserSummary userSummary;

  /**
   * Constructs a new look-up result.
   *
   * @param status the status of the operation, never {@code null}.
   * @param message the detail message associated with the status, may be {@code null}.
   * @param userSummary the user summary returned by the look-up, may be {@code null}.
   */
  public PSGetUserResult(STATUS status, String message, PSUserSummary userSummary) {
    super(status, message);
    this.userSummary = userSummary;
  }

  /**
   * Gets the user summary returned by the look-up.
   *
   * @return the user summary, may be {@code null} when the operation did not return one.
   */
  public PSUserSummary getUserSummary() {
    return userSummary;
  }
}
