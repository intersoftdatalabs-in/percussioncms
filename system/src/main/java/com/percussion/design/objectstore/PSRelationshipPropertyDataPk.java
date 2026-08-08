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
package com.percussion.design.objectstore;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The primary key for {@link PSRelationshipPropertyData} object. This is needed for the persistent
 * service layer.
 */
@Embeddable
public class PSRelationshipPropertyDataPk implements Serializable {
  /** Serialization id for {@link java.io.Serializable}. */
  private static final long serialVersionUID = 1L;
  /** The relationship (or parent) id. */
  @Basic
  @Column(name = "RID")
  private int m_rid;

  /** The name of the property, never <code>null</code> */
  @Basic
  @Column(name = "PROPERTYNAME")
  private String m_propertyName;

  /** Default ctor, needed by the services of the persistent layer. */
  private PSRelationshipPropertyDataPk() {
    // Empty
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof PSRelationshipPropertyDataPk)) return false;
    PSRelationshipPropertyDataPk other = (PSRelationshipPropertyDataPk) obj;

    return new EqualsBuilder()
        .append(m_propertyName, other.m_propertyName)
        .append(m_rid, other.m_rid)
        .isEquals();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return new HashCodeBuilder(13, 3)
        .appendSuper(super.hashCode())
        .append(m_rid)
        .append(m_propertyName)
        .toHashCode();
  }
}
