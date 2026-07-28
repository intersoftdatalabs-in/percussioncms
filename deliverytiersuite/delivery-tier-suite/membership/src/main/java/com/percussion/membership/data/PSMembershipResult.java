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
 * Base class for membership rest call results
 *
 * @author JaySeletz
 */
public class PSMembershipResult {

  /** The status of the membership REST call. */
  protected STATUS status;

  /** The detail message associated with the status. */
  protected String message;

  /**
   * Constructs a new membership result.
   *
   * @param status the status of the call, never {@code null}.
   * @param message the detail message associated with the status, may be {@code null}.
   */
  public PSMembershipResult(STATUS status, String message) {
    this.status = status;
    this.message = message;
  }

  /**
   * Gets the status of the membership REST call.
   *
   * @return the status, never {@code null}.
   */
  public STATUS getStatus() {
    return status;
  }

  /**
   * Gets the detail message associated with the status.
   *
   * @return the message, may be {@code null} when no message is associated.
   */
  public String getMessage() {
    return message;
  }

  /** Enumeration of result status. */
  public enum STATUS {
    /** The call completed successfully. */
    SUCCESS,
    /** One or more parameters failed validation. */
    INVALID_PARAM,
    /** The call failed for an unexpected reason. */
    UNEXPECTED_ERROR,
    /** The member being created already exists. */
    MEMBER_EXISTS,
    /** Authentication failed. */
    AUTH_FAILED,
    /** The supplied password reset key is invalid. */
    INVALID_RESET_KEY
  }
}
