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

/**
 * REST model for a single ACL entry on a content item.
 */
@XmlRootElement(name = "AclEntry")
public class AclItem {
  /** Principal type (for example user or role). */
  private String type;
  /** Whether read permission is granted. */
  private boolean read;
  /** Whether write permission is granted. */
  private boolean write;
  /** Whether admin permission is granted. */
  private boolean admin;
  /** Whether this entry is an explicit deny. */
  private boolean deny;
  /** Principal name for this ACL entry. */
  private String name;

  /**
   * Creates an empty ACL entry.
   */
  public AclItem() {}

  /**
   * Returns the principal name.
   *
   * @return the principal name
   */
  @XmlAttribute
  public String getName() {
    return name;
  }

  /**
   * Sets the principal name.
   *
   * @param name the principal name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the principal type.
   *
   * @return the principal type
   */
  @XmlAttribute
  public String getType() {
    return type;
  }

  /**
   * Sets the principal type.
   *
   * @param type the principal type
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns whether read is granted.
   *
   * @return {@code true} if read is granted
   */
  @XmlAttribute
  public boolean isRead() {
    return read;
  }

  /**
   * Sets whether read is granted.
   *
   * @param read {@code true} to grant read
   */
  public void setRead(boolean read) {
    this.read = read;
  }

  /**
   * Returns whether write is granted.
   *
   * @return {@code true} if write is granted
   */
  @XmlAttribute
  public boolean isWrite() {
    return write;
  }

  /**
   * Sets whether write is granted.
   *
   * @param write {@code true} to grant write
   */
  public void setWrite(boolean write) {
    this.write = write;
  }

  /**
   * Returns whether admin is granted.
   *
   * @return {@code true} if admin is granted
   */
  @XmlAttribute
  public boolean isAdmin() {
    return admin;
  }

  /**
   * Sets whether admin is granted.
   *
   * @param admin {@code true} to grant admin
   */
  public void setAdmin(boolean admin) {
    this.admin = admin;
  }

  /**
   * Returns whether this entry is a deny rule.
   *
   * @return {@code true} if this is a deny entry
   */
  @XmlAttribute
  public boolean isDeny() {
    return deny;
  }

  /**
   * Sets whether this entry is a deny rule.
   *
   * @param deny {@code true} if this is a deny entry
   */
  public void setDeny(boolean deny) {
    this.deny = deny;
  }
}
