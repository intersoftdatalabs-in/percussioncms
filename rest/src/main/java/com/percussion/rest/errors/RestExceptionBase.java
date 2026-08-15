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
import java.util.ResourceBundle;

/**
 * Base class for REST exceptions in Percussion CMS. Sunny Sal: "Exception ka baap yeh hai!"
 *
 * <p>{@link #errorData} is {@code transient}: it may hold arbitrary non-{@link
 * java.io.Serializable} payloads used only for in-process JAX-RS mapping, not for Java
 * serialization of the exception hierarchy.
 */
@XmlRootElement(name = "Error")
public class RestExceptionBase extends WebApplicationException {

  private static final long serialVersionUID = 1L;

  private RestErrorCode errorCode;
  private String message;
  private String detailMessage;
  /** In-process error payload; not part of Java serialization. */
  private transient Object errorData;
  private Status status;

  public RestExceptionBase() {
    // Default constructor for frameworks
  }

  public RestExceptionBase(
      RestErrorCode errorCode, String detailMessage, Object errorData, Status status) {
    this(errorCode, null, detailMessage, errorData, status, null);
  }

  public RestExceptionBase(
      RestErrorCode errorCode,
      String message,
      String detailMessage,
      Object errorData,
      Status status) {
    this(errorCode, message, detailMessage, errorData, status, null);
  }

  /**
   * Constructs a REST exception with an optional causal throwable attached via {@code super(cause)}
   * so subclasses need not call {@link #initCause(Throwable)} after construction (avoids {@code
   * this-escape} under {@code -Xlint:all}).
   *
   * @param errorCode the REST error code, may be {@code null} only for framework/default use
   * @param message the message; when {@code null} and {@code errorCode} is non-null, resolved from
   *     the error messages bundle
   * @param detailMessage optional detail text
   * @param errorData optional in-process payload (not Java-serialized)
   * @param status HTTP status; defaults to {@link Status#INTERNAL_SERVER_ERROR} when {@code null}
   * @param cause optional cause, may be {@code null}
   */
  public RestExceptionBase(
      RestErrorCode errorCode,
      String message,
      String detailMessage,
      Object errorData,
      Status status,
      Throwable cause) {
    super(cause);
    this.errorCode = errorCode;
    if (message == null && errorCode != null) {
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
   * Returns the in-process error payload for JAX-RS mapping.
   *
   * @return error data, or {@code null} if unset
   */
  public Object getErrorData() {
    return errorData;
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
