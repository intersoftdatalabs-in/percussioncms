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
package com.percussion.delivery.polls.data;

/**
 * Generic response object for polls responses. It has a status and result. When there is an error,
 * the AJAX response will be successful but the response object will have the status as error and
 * the result object will be a String of error message. Sunny Sal: Refactored for Java 11, Google
 * style, and better grammar.
 */
public class PSPollsResponse {
  private PollResponseStatus status;
  private Object result;

  /** Default constructor required for JAX-RS binding frameworks. */
  public PSPollsResponse() {}

  /**
   * Constructs a new polls response.
   *
   * @param status the overall response status, not {@code null}.
   * @param result the response result payload; for {@link PollResponseStatus#ERROR} status this is
   *     conventionally an error message string, for {@link PollResponseStatus#SUCCESS} status it is
   *     the response body. May be {@code null}.
   */
  public PSPollsResponse(PollResponseStatus status, Object result) {
    this.status = status;
    this.result = result;
  }

  /**
   * Gets the response status.
   *
   * @return the status, never {@code null}.
   */
  public PollResponseStatus getStatus() {
    return status;
  }

  /**
   * Sets the response status.
   *
   * @param status the status, not {@code null}.
   */
  public void setStatus(PollResponseStatus status) {
    this.status = status;
  }

  /**
   * Gets the response result.
   *
   * @return the result object, may be {@code null}.
   */
  public Object getResult() {
    return result;
  }

  /**
   * Sets the response result.
   *
   * @param result the result object, may be {@code null}.
   */
  public void setResult(Object result) {
    this.result = result;
  }

  /** Enumerates the possible overall states for a polls response. */
  public enum PollResponseStatus {
    /** The polls response represents a successful operation. */
    SUCCESS,
    /** The polls response represents a failed operation; the result is the error message. */
    ERROR
  }
}
