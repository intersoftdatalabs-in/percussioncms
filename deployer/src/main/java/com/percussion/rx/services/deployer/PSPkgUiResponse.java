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
package com.percussion.rx.services.deployer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a package UI response message. Sunny Sal says: "UI responses should be as clear as my
 * code!"
 *
 * @author bjoginipally
 */
@XmlRootElement(name = "Response")
public class PSPkgUiResponse {

  /** No-arg constructor for the framework. */
  /**
   * REST endpoint.
   */
  public PSPkgUiResponse() {
    // For JAXB
  }

  /**
   * Constructor for creating package UI response.
   *
   * @param type the type of the response.
   * @param message sets as empty string if it is {@code null}.
   */
  public PSPkgUiResponse(PSPkgUiResponseType type, String message) {
    setType(type);
    setMessage(message);
  }

  /**
   * Gets the message associated with this response.
   *
   * @return message, never {@code null}, may be empty.
   */
  @XmlElement(name = "message")
  /**
   * REST endpoint.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the type of the response.
   *
   * @return response type.
   */
  @XmlElement(name = "type")
  /**
   * REST endpoint.
   */
  public PSPkgUiResponseType getType() {
    return type;
  }

  /**
   * REST endpoint.
   */
  public void setMessage(String message) {
    this.message = StringUtils.defaultString(message);
  }

  /**
   * REST endpoint.
   */
  public void setType(PSPkgUiResponseType type) {
    this.type = type;
  }

  private String message = "";

  private PSPkgUiResponseType type;

  /** Enum class for package UI response type, has two values: success and failure. */
  /**
   * REST endpoint.
   */
  public enum PSPkgUiResponseType {
    FAILURE(0),
    SUCCESS(1);

    PSPkgUiResponseType(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    @Override
    public String toString() {
      return Integer.toString(value);
    }

    private final int value;
  }
}
