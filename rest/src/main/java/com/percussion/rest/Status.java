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

package com.percussion.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Represents a generic REST API status response carrying a human-readable message and a numeric
 * status code.
 *
 * @author stephenbolton
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Status")
public class Status {
  /** Human-readable message describing the status. */
  @Schema(name = "message", description = "The message for the Status response")
  private String message;

  /** Numeric status code accompanying the message. */
  @Schema(name = "statusCode", description = "The numeric code for the Status message")
  private int statusCode;

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public Status() {
    // Default constructor
  }

  public Status(String message) {
    this.message = message;
  }

  public Status(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  /**
   * @return status message as Optional
   */
  public Optional<String> getMessage() {
    return Optional.ofNullable(message);
  }

  /**
   * @param message status message
   */
  public void setMessage(String message) {
    this.message = message;
  }
}
