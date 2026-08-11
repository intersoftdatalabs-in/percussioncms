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
package com.percussion.integritymanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.persistence.*;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** Represents a property for an integrity task. */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSIntegrityTaskProperty")
@Table(name = "PSX_INTEGRITY_TASK_PROPERTIES")
public class PSIntegrityTaskProperty extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "TASKRPROPERTYID")
  private long taskPropertyId = -1L;

  @Basic
  @Column(name = "TASKID", insertable = false, updatable = false)
  private long taskId;

  @Basic
  @Column(name = "PROPERTYNAME")
  private String name;

  @Basic
  @Column(name = "PROPERTYVALUE")
  private String value;

  /** Default constructor. */
  public PSIntegrityTaskProperty() {
    // Default constructor for JPA
  }

  public PSIntegrityTaskProperty(String name, String value) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    this.name = name;
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSIntegrityTaskProperty)) {
      return false;
    }
    var other = (PSIntegrityTaskProperty) obj;
    return Objects.equals(name, other.name) && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  /**
   * Gets the property name.
   *
   * @return the property name, never null or empty
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the property name.
   *
   * @param name the name to set, never null or empty
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    this.name = name;
  }

  /**
   * Gets the value.
   *
   * @return the value, may be null or empty
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }

  public long getTaskPropertyId() {
    return taskPropertyId;
  }

  public void setTaskPropertyId(long taskPropertyId) {
    this.taskPropertyId = taskPropertyId;
  }

  public long getTaskId() {
    return taskId;
  }

  public void setTaskId(long taskId) {
    this.taskId = taskId;
  }
}
