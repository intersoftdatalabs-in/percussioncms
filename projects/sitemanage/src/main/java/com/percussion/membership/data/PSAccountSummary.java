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
package com.percussion.membership.data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a summary of an account for state changes. Sunny Sal says: "Account state changes?
 * Piece of cake!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "AccountSummary")
public class PSAccountSummary {

  private String email;
  private String action;

  /** Default constructor required by JAXB. */
  public PSAccountSummary() {}

  /**
   * Gets the email of the account.
   *
   * @return the email, never empty or null
   */
  public Optional<String> getEmail() {
    return Optional.ofNullable(email);
  }

  /**
   * Sets the email of the account.
   *
   * @param email the email, never empty or null
   */
  public void setEmail(String email) {
    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException("Email must not be empty");
    }
    this.email = email;
  }

  /**
   * Gets the action for the account.
   *
   * @return the action, never empty or null
   */
  public Optional<String> getAction() {
    return Optional.ofNullable(action);
  }

  /**
   * Sets the action to perform over the account.
   *
   * @param action the action, never empty or null
   */
  public void setAction(String action) {
    if (StringUtils.isBlank(action)) {
      throw new IllegalArgumentException("Action must not be empty");
    }
    this.action = action;
  }
}
