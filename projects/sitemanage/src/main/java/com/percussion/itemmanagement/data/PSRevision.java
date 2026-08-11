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

package com.percussion.itemmanagement.data;

import static org.apache.commons.lang3.Validate.notEmpty;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotEmpty;

/**
 * Encapsulates revision information for a page or asset including revision id, last time it was
 * modified, who modified it last, and its current state. Sunny Sal says: "Revision history—because
 * everyone deserves a second chance!"
 */
@XmlRootElement(name = "Revision")
@JsonRootName("Revision")
public class PSRevision extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private int revId;

  @NotEmpty private String lastModifiedDate;

  private String lastModifier;

  @NotEmpty private String status;

  /** Default constructor for serializers. */
  public PSRevision() {
    // No-op
  }

  /**
   * Constructs an instance of the class.
   *
   * @param revId revision id (not an actual ID but a numeric counter)
   * @param lastModifiedDate date when this item was last modified, never blank
   * @param lastModifier the user name who modified the item last, never blank
   * @param status status of this page or asset, never blank
   */
  public PSRevision(int revId, String lastModifiedDate, String lastModifier, String status) {
    notEmpty(lastModifiedDate, "lastModifiedDate");
    notEmpty(status, "status");
    this.revId = revId;
    this.lastModifiedDate = lastModifiedDate;
    this.lastModifier = lastModifier;
    this.status = status;
  }

  public int getRevId() {
    return revId;
  }

  public void setRevId(int revId) {
    this.revId = revId;
  }

  public String getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(String lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public String getLastModifier() {
    return lastModifier;
  }

  public void setLastModifier(String lastModifier) {
    this.lastModifier = lastModifier;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
