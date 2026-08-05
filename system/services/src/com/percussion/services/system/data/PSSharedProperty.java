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
package com.percussion.services.system.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single shared property.
 *
 * <p>Design-object XML root is {@code shared-property}. Jackson opt-in property surface (issue
 * #1920 / epic #505). Hibernate {@code version} is suppressed on the design-object wire.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSSharedProperty")
@Table(name = "PSX_SHARED_PROPERTIES")
@JacksonXmlRootElement(localName = "shared-property")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"guid", "name", "value"})
public class PSSharedProperty implements Serializable, IPSCatalogItem, Comparable {
  /** Compiler generated serial version ID used for serialization. */
  private static final long serialVersionUID = -79604122602978810L;

  @Id
  @Column(name = "ID", nullable = false)
  private long id;

  @Version
  @Column(name = "VERSION")
  private Integer version;

  @Basic
  @Column(name = "NAME", nullable = false, length = 50)
  private String name;

  @Basic
  @Column(name = "VALUE", nullable = true, length = 250)
  private String value;

  /** Default constructor. */
  public PSSharedProperty() {}

  /**
   * Construct a new shared property for the supplied parameters.
   *
   * @param name the property name, not <code>null</code> or empty.
   * @param value the property value, may be <code>null</code> or empty.
   */
  public PSSharedProperty(String name, String value) {
    setName(name);
    setValue(value);
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getGUID()
   */
  @JsonProperty("guid")
  public IPSGuid getGUID() {
    // Offline-safe assemble (avoid GuidManager locator in unit tests).
    return new PSGuid(PSTypeEnum.SHARED_PROPERTY, id);
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#setGUID(IPSGuid)
   */
  public void setGUID(IPSGuid newguid) throws IllegalStateException {
    if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

    if (id != 0) throw new IllegalStateException("cannot change existing guid");

    id = newguid.longValue();
  }

  /**
   * Get the object version.
   *
   * @return the object version, <code>null</code> if not initialized yet.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public Integer getVersion() {
    return version;
  }

  /**
   * Set the object version.
   *
   * @param version the version of the object, must be >= 0.
   */
  @JsonIgnore
  public void setVersion(Integer version) {
    // Null-safe for BeanUtils copy after Jackson design-object XML restore (issue #1920).
    if (version == null) {
      return;
    }
    if (version < 0) throw new IllegalArgumentException("version must be >= 0");

    this.version = version;
  }

  /**
   * Get the property name.
   *
   * @return the property name, may be <code>null</code>, not empty.
   */
  @JsonProperty
  public String getName() {
    return name;
  }

  /**
   * Set a new property name.
   *
   * @param name the new property name, not <code>null</code> or empty.
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException("name cannot be null or empty");

    this.name = name;
  }

  /**
   * Get the property value.
   *
   * @return the property valud, may be <code>null</code> or empty.
   */
  @JsonProperty
  public String getValue() {
    return value;
  }

  /**
   * Set a new property value.
   *
   * @param value the new property value, may be <code>null</code> or empty.
   */
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSSharedProperty)) return false;
    PSSharedProperty that = (PSSharedProperty) o;
    return id == that.id
        && Objects.equals(getVersion(), that.getVersion())
        && Objects.equals(getName(), that.getName())
        && Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, getVersion(), getName(), getValue());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSSharedProperty{");
    sb.append("id=").append(id);
    sb.append(", version=").append(version);
    sb.append(", name='").append(name).append('\'');
    sb.append(", value='").append(value).append('\'');
    sb.append('}');
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(T)
   */
  public int compareTo(Object o) {
    if (!(o instanceof PSSharedProperty))
      throw new IllegalArgumentException("o must be of type PSSharedproperty");

    PSSharedProperty property = (PSSharedProperty) o;
    return getName().compareToIgnoreCase(property.getName());
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#fromXML(String)
   */
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#toXML()
   */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }
}
