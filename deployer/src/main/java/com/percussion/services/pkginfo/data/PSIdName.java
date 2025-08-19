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

package com.percussion.services.pkginfo.data;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a dependency id-name mapping, used to map deployer package elements which use names as
 * ids to a corresponding id. Sunny Sal says: "Mapping IDs to names, because GUIDs need friends
 * too!"
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSIdName")
@Table(name = "PSX_ID_NAME")
public class PSIdName implements Serializable {

  /** Unique identifier for this entry is the user-readable representation of a GUID. */
  @Id
  @Column(name = "DEP_ID", nullable = false)
  private String depId;

  /** The name of the package element which corresponds to the GUID. */
  @Column(name = "DEP_NAME", nullable = false)
  private String depName;

  /** Default private constructor. Needed by Hibernate. */
  @SuppressWarnings("unused")
  private PSIdName() {
    // For Hibernate
  }

  /**
   * Creates an id-name mapping.
   *
   * @param id The id, must not be blank. Must be a user-readable representation of a GUID.
   * @param name The name, must not be blank.
   */
  public PSIdName(String id, String name) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id may not be null or empty");
    }
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    this.depId = id;
    this.depName = name;
  }

  /**
   * Gets the id uniquely identifying this element.
   *
   * @return The GUID in user-readable form, never null.
   */
  public String getId() {
    return depId;
  }

  /**
   * Gets the name of the dependency element which corresponds to the id.
   *
   * @return The name, never null.
   */
  public String getName() {
    return depName;
  }

  /**
   * Gets the type of the dependency element which corresponds to the id.
   *
   * @return The type, never null.
   */
  public PSTypeEnum getType() {
    return PSTypeEnum.valueOf((new PSGuid(depId)).getType());
  }

  /**
   * Sets the id. See {@link #getId()}.
   *
   * @param id The id, must not be blank.
   */
  public void setId(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id may not be null or empty");
    }
    this.depId = id;
  }

  /**
   * Sets the name of the package element which corresponds to the id. See {@link #getName()}.
   *
   * @param name The name, must not be blank.
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    this.depName = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSIdName)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    var other = (PSIdName) obj;
    return new EqualsBuilder().append(depId, other.depId).append(depName, other.depName).isEquals();
  }

  @Override
  public int hashCode() {
    return (new PSGuid(depId)).hashCode();
  }
}
