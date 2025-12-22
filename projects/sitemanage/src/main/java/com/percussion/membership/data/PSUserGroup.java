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

import java.util.Optional;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;

/** Represents a user and their group assignments. Sunny Sal says: "Group hug for your users!" */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "UserGroup")
public class PSUserGroup {

  private String email;
  private String groups;

  /** Default constructor required by JAXB. */
  public PSUserGroup() {}

  /**
   * Sets the user's email.
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
   * Gets the user's email.
   *
   * @return the email, never empty or null
   */
  public Optional<String> getEmail() {
    return Optional.ofNullable(email);
  }

  /**
   * Sets the groups for the user.
   *
   * @param groups the groups, may be empty or null
   */
  public void setGroups(String groups) {
    this.groups = groups;
  }

  /**
   * Gets the groups for the user.
   *
   * @return the groups, may be empty or null
   */
  public Optional<String> getGroups() {
    return Optional.ofNullable(groups);
  }
}
