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

package com.percussion.sitemanage.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

/**
 * Contains information for updating section links. The old section ID is replaced with the new
 * section in the supplied parent.
 */
@XmlRootElement(name = "UpdateSectionLink")
@JsonRootName("UpdateSectionLink")
public class PSUpdateSectionLink {

  @NotBlank @NotNull private String oldSectionId;

  @NotBlank @NotNull private String newSectionId;

  @NotBlank @NotNull private String parentSectionId;

  /**
   * Gets the old section id (string format of the guid).
   *
   * @return old section id.
   */
  public String getOldSectionId() {
    return oldSectionId;
  }

  /**
   * Sets the old section id (string format of the guid).
   *
   * @param oldSectionId old section id, should not be blank for a valid request.
   */
  public void setOldSectionId(String oldSectionId) {
    this.oldSectionId = oldSectionId;
  }

  /**
   * Gets the new section id (string format of the guid).
   *
   * @return new section id.
   */
  public String getNewSectionId() {
    return newSectionId;
  }

  /**
   * Sets the new section id (string format of the guid).
   *
   * @param newSectionId new section id, should not be blank for a valid request.
   */
  public void setNewSectionId(String newSectionId) {
    this.newSectionId = newSectionId;
  }

  /**
   * Gets the parent section id (string format of the guid).
   *
   * @return parent section id.
   */
  public String getParentSectionId() {
    return parentSectionId;
  }

  /**
   * Sets the parent section id (string format of the guid).
   *
   * @param parentSectionId parent section id, should not be blank for a valid request.
   */
  public void setParentSectionId(String parentSectionId) {
    this.parentSectionId = parentSectionId;
  }

  /**
   * Gets the old section id as Optional.
   *
   * @return Optional old section id.
   */
  public Optional<String> getOldSectionIdOptional() {
    return Optional.ofNullable(oldSectionId);
  }

  /**
   * Gets the new section id as Optional.
   *
   * @return Optional new section id.
   */
  public Optional<String> getNewSectionIdOptional() {
    return Optional.ofNullable(newSectionId);
  }

  /**
   * Gets the parent section id as Optional.
   *
   * @return Optional parent section id.
   */
  public Optional<String> getParentSectionIdOptional() {
    return Optional.ofNullable(parentSectionId);
  }
}
