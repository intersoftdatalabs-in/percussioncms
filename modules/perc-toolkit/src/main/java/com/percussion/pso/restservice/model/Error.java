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
package com.percussion.pso.restservice.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * REST model representing an error returned for an item operation.
 */
@XmlRootElement(name = "Error")
public class Error {

  /**
   * Well-known error codes for item REST responses.
   */
  public static enum ErrorCode {
    /** Item was not found. */
    NOT_FOUND,
    /** Item was purged. */
    PURGED,
    /** Unexpected error. */
    UNKNOWN_ERROR,
    /** Assembly failed. */
    ASSEMBLY_ERROR,
    /** Operation was skipped. */
    SKIP
  }

  /** Structured error code. */
  private ErrorCode errorCode;
  /** Human-readable error message. */
  private String errorMessage;
  /** Optional related content id. */
  private Integer contentId;

  /**
   * Creates an error with {@link ErrorCode#UNKNOWN_ERROR}.
   */
  public Error() {
    // Direct field assignment avoids this-escape from overridable setters in ctor.
    this.errorCode = ErrorCode.UNKNOWN_ERROR;
  }

  /**
   * Creates an error with a code and message.
   *
   * @param errorCode the error code
   * @param message the error message
   */
  public Error(ErrorCode errorCode, String message) {
    this.errorCode = errorCode;
    this.errorMessage = message;
  }

  /**
   * Creates an error with a code, content id, and message.
   *
   * @param errorCode the error code
   * @param contentId related content id
   * @param message the error message
   */
  public Error(ErrorCode errorCode, Integer contentId, String message) {
    this.errorCode = errorCode;
    this.errorMessage = message;
    this.contentId = contentId;
  }

  /**
   * Creates an error with only a code.
   *
   * @param errorCode the error code
   */
  public Error(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Returns the error code.
   *
   * @return the error code
   */
  @XmlAttribute
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Sets the error code.
   *
   * @param errorCode the error code
   */
  public void setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Returns the error message.
   *
   * @return the error message
   */
  @XmlValue
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Sets the error message.
   *
   * @param errorMessage the error message
   */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   * Sets the related content id.
   *
   * @param contentId the content id
   */
  public void setContentId(Integer contentId) {
    this.contentId = contentId;
  }

  /**
   * Returns the related content id.
   *
   * @return the content id, or {@code null}
   */
  @XmlAttribute
  public Integer getContentId() {
    return contentId;
  }
}
