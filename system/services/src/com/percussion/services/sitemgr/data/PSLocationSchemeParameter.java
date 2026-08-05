/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.sitemgr.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a location scheme parameter.
 *
 * <p>Design-object nested element is {@code location-scheme-parameter}. Parent {@link #getScheme()}
 * is suppressed (circular); restore is via {@link PSLocationScheme#setParameterSet(java.util.Set)}.
 * Jackson opt-in surface (issue #1918 / #1892 / epic #505).
 *
 * @author dougrand
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSLocationScheme_Parameters")
@Table(name = "RXLOCATIONSCHEMEPARAMS")
@JacksonXmlRootElement(localName = "location-scheme-parameter")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"name", "parameterId", "sequence", "type", "value"})
public class PSLocationSchemeParameter implements Serializable {
  /** Serial id identifies versions of serialized data */
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "SCHEMEPARAMID")
  Integer parameterId;

  @ManyToOne(targetEntity = PSLocationScheme.class)
  @JoinColumn(name = "SCHEMEID", nullable = false, insertable = false, updatable = false)
  IPSLocationScheme scheme;

  @Basic Integer sequence;

  @Basic String type;

  @Basic String name;

  @Basic String value;

  /**
   * @param parameterId The parameterId to set.
   */
  public void setParameterId(Integer parameterId) {
    this.parameterId = parameterId;
  }

  /**
   * Get the name of the parameter
   *
   * @return Returns the name, never <code>null</code> or empty
   */
  @JsonProperty
  public String getName() {
    return name;
  }

  /**
   * @param name The name to set, never <code>null</code> or empty
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    this.name = name;
  }

  /**
   * Get the primary key used to store this parameter
   *
   * @return Returns the parameterId.
   */
  @JsonProperty
  public Integer getParameterId() {
    return parameterId;
  }

  /**
   * Get the parent location scheme for this parameter
   *
   * @return Returns the scheme, never <code>null</code>
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public IPSLocationScheme getScheme() {
    return scheme;
  }

  /**
   * Set a new location scheme, should only be used on a newly constructed object.
   *
   * @param scheme The scheme to set, never <code>null</code>
   */
  /**
   * Set parent scheme. {@code null} is allowed for BeanUtils property-copy after XML restore
   * (parent is re-linked via {@link PSLocationScheme#setParameterSet}).
   */
  @JsonIgnore
  public void setScheme(IPSLocationScheme scheme) {
    this.scheme = scheme;
  }

  /**
   * The sequence is really only used in the user interface to order the parameters.
   *
   * @return Returns the sequence.
   */
  @JsonProperty
  public Integer getSequence() {
    return sequence;
  }

  /**
   * Set a new sequence
   *
   * @param sequence The sequence to set, never <code>null</code>
   */
  public void setSequence(Integer sequence) {
    if (sequence == null) {
      throw new IllegalArgumentException("sequence may not be null");
    }
    this.sequence = sequence;
  }

  /**
   * Get the type of the value.
   *
   * @return Returns the type.
   */
  @JsonProperty
  public String getType() {
    return type;
  }

  /**
   * Set a new type
   *
   * @param type The type to set, never <code>null</code> or empty
   */
  public void setType(String type) {
    if (StringUtils.isBlank(type)) {
      throw new IllegalArgumentException("type may not be null or empty");
    }
    this.type = type;
  }

  /**
   * Get the value of the particular parameter
   *
   * @return Returns the value, never <code>null</code> or empty
   */
  @JsonProperty
  public String getValue() {
    return value;
  }

  /**
   * Set a new value
   *
   * @param value The value to set, never <code>null</code> or empty
   */
  public void setValue(String value) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException("value may not be null or empty");
    }
    this.value = value;
  }

  /** (non-Javadoc) */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSLocationSchemeParameter)) {
      return false;
    }
    PSLocationSchemeParameter pb = (PSLocationSchemeParameter) obj;

    return new EqualsBuilder()
        .append(parameterId, pb.parameterId)
        .append(sequence, pb.sequence)
        .append(type, pb.type)
        .append(name, pb.name)
        .append(value, pb.value)
        .isEquals();
  }

  /** (non-Javadoc) */
  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSLocationSchemeParameter{");
    sb.append("parameterId=").append(parameterId);
    sb.append(", scheme=").append(scheme);
    sb.append(", sequence=").append(sequence);
    sb.append(", type='").append(type).append('\'');
    sb.append(", name='").append(name).append('\'');
    sb.append(", value='").append(value).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
