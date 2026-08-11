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
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * REST model for a folder ACL and its entries.
 */
public class FolderAcl {

  /** ACL entries for the folder. */
  private List<AclItem> entries;
  /** Community name associated with this folder ACL. */
  private String communityName;

  /**
   * Creates an empty folder ACL.
   */
  public FolderAcl() {}

  /**
   * Returns the ACL entries.
   *
   * @return the entries
   */
  @XmlElement(name = "AclEntry")
  public List<AclItem> getEntries() {
    return entries;
  }

  /**
   * Sets the ACL entries.
   *
   * @param entries the entries
   */
  public void setEntries(List<AclItem> entries) {
    this.entries = entries;
  }

  /**
   * Returns the community name.
   *
   * @return the community name
   */
  @XmlAttribute
  public String getCommunityName() {
    return communityName;
  }

  /**
   * Sets the community name.
   *
   * @param communityName the community name
   */
  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }
}
