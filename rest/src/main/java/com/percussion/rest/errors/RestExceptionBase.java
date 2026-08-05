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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;
import java.util.ResourceBundle;

/** Base class for REST exceptions in Percussion CMS. Sunny Sal: "Exception ka baap yeh hai!" */
@XmlRootElement(name = "Error")
@SuppressWarnings("serial")
public class RestExceptionBase extends WebApplicationException {

  private static final long serialVersionUID = 1L;

  private RestErrorCode errorCode;
  private String message;
  private String detailMessage;
  private Object errorData;
  private Status status;

  public RestExceptionBase() {
    // Default constructor for frameworks
  }

  public RestExceptionBase(
      RestErrorCode errorCode, String detailMessage, Object errorData, Status status) {
    this(errorCode, null, detailMessage, errorData, status);
  }

  public RestExceptionBase(
      RestErrorCode errorCode,
      String message,
      String detailMessage,
      Object errorData,
      Status status) {
    this.errorCode = errorCode;
    if (message == null) {
      var errorMsg = ResourceBundle.getBundle("com.percussion.rest.errors.ErrorMessages");
      this.message = errorMsg.getString(Integer.toString(errorCode.getNumVal()));
    } else {
      this.message = message;
    }
    this.detailMessage = detailMessage;
    this.errorData = errorData;
    this.status = status == null ? Status.INTERNAL_SERVER_ERROR : status;
  }

  public RestErrorCode getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(RestErrorCode errorCode) {
    this.errorCode = errorCode;
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
   * Returns the error data as an Optional.
   *
   * @return Optional containing error data if present
   */
  public Optional<Object> getErrorData() {
    return Optional.ofNullable(errorData);
  }

  public void setErrorData(Object errorData) {
    this.errorData = errorData;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public RestExceptionBase(Throwable cause) {
    super(cause);
  }
}
