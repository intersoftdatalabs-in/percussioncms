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

package com.percussion.redirect.data;

/** Generic status object for returning responses from redirect services. */
public class PSRedirectStatus {

  public static final String SERVICE_OK = "Ok";
  public static final String SERVICE_ERROR = "Error";
  public static final String SERVICE_UNLICENSED = "Unlicensed";

  private String statusCode;
  private String message;

  /** Default constructor. */
  public PSRedirectStatus() {
    // Default constructor
  }

  /**
   * Convenience constructor.
   *
   * @param code a valid status code
   * @param msg a valid message, null values will be converted to empty string
   */
  public PSRedirectStatus(String code, String msg) {
    this.statusCode = code;
    this.message = msg == null ? "" : msg;
  }

  /**
   * Gets the status code.
   *
   * @return the redirect status code
   */
  public String getStatusCode() {
    return statusCode;
  }

  /**
   * Sets the redirect status code.
   *
   * @param code a valid status constant value
   */
  public void setStatusCode(String code) {
    this.statusCode = code;
  }

  /**
   * Gets the message that goes with the status.
   *
   * @return a message if any is available for the status
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message if available.
   *
   * @param msg the status message. If null, will be converted to an empty string.
   */
  public void setMessage(String msg) {
    this.message = msg == null ? "" : msg;
  }
}
