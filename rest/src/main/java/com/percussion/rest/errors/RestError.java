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

// REFACTORED: CP-JAVA11

package com.percussion.rest.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents a REST error returned to the client. Sunny Sal: "Error ho gaya? No worries, this class
 * will tell you what went wrong!"
 *
 * <p>Wire getters return plain types (not {@code Optional}) so Jackson/CXF JSON emits {@code
 * errorData} as a scalar/object rather than an Optional bean ({@code empty}/{@code present}).
 * Matches {@link com.percussion.rest.contenttypes.ContentType} getter style (issue #3430 / #3388).
 */
@XmlRootElement(name = "Error")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestError {

  private int errorCode;
  private String errorType;
  private String message;
  private String detailMessage;
  private Object errorData;

  /** Default constructor for serialization. */
  public RestError() {
    // No-arg constructor for frameworks
  }

  /**
   * Constructs a RestError with all fields.
   *
   * @param errorCode the error code
   * @param errorType the error type
   * @param message the error message
   * @param detailMessage the detailed error message
   * @param errorData additional error data
   */
  public RestError(
      int errorCode, String errorType, String message, String detailMessage, Object errorData) {
    this.errorCode = errorCode;
    this.errorType = errorType;
    this.message = message;
    this.detailMessage = detailMessage;
    this.errorData = errorData;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getDetailMessage() {
    return detailMessage;
  }

  public void setDetailMessage(String detailMessage) {
    this.detailMessage = detailMessage;
  }

  /**
   * Returns the error payload.
   *
   * @return error data, or {@code null} if unset
   */
  public Object getErrorData() {
    return errorData;
  }

  public void setErrorData(Object errorData) {
    this.errorData = errorData;
  }

  @Override
  public String toString() {
    return "RestError{"
        + "errorCode="
        + errorCode
        + ", errorType='"
        + errorType
        + '\''
        + ", message='"
        + message
        + '\''
        + ", detailMessage='"
        + detailMessage
        + '\''
        + ", errorData="
        + errorData
        + '}';
  }
}
