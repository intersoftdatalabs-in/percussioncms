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
package com.percussion.services.ui.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Persist an association between a hierarchy node and a hierarchy node property.
 *
 * <p>Design-object XML root is {@code hierarchy-node-property}. Jackson opt-in property surface
 * (issue #1920 / epic #505). Historical {@code PSHierarchyNodeProperty.betwixt} mapped a
 * non-existent {@code parentGuid}/{@code parentId} property; production bean identity is scalar
 * {@code node-id}. Hibernate {@code version} is suppressed on the design-object wire.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSHierarchyNodeProperty")
@IdClass(PSHierarchyNodePropertyPK.class)
@Table(name = "PSX_WB_HIERARCHY_NODE_PROP")
@JacksonXmlRootElement(localName = "hierarchy-node-property")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"name", "nodeId", "value"})
public class PSHierarchyNodeProperty implements Serializable {
  /** Compiler generated serial version ID used for serialization. */
  private static final long serialVersionUID = -982738490837214578L;

  @Id
  @Column(name = "NODE_ID")
  private long nodeId;

  @Id
  @Column(name = "NAME")
  private String name;

  @Version
  @Column(name = "VERSION")
  private Integer version;

  @Basic
  @Column(name = "VALUE")
  private String value;

  /** Default constructor. */
  public PSHierarchyNodeProperty() {}

  /**
   * Construct a new hierarchy node property for the supplied name and value.
   *
   * @param propName the name of the property, not <code>null</code> or empty.
   * @param propValue the value of the property, may be <code>null</code> or empty.
   */
  public PSHierarchyNodeProperty(String propName, String propValue, IPSGuid parent) {
    if (StringUtils.isBlank(propName))
      throw new IllegalArgumentException("name must not be null/blank");

    if (parent == null) throw new IllegalArgumentException("parent cannot be null");

    nodeId = parent.longValue();
    setName(propName);
    setValue(propValue);
  }

  @JsonProperty("node-id")
  public long getNodeId() {
    return nodeId;
  }

  public void setNodeId(long nodeId) {
    this.nodeId = nodeId;
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
   * Set the object version. The version can only be set once in the life cycle of this object.
   *
   * @param version the version of the object, must be >= 0.
   */
  @JsonIgnore
  public void setVersion(Integer version) {
    // Null-safe for BeanUtils copy after Jackson design-object XML restore (issue #1920).
    if (version == null) {
      return;
    }
    if (this.version != null)
      throw new IllegalStateException("version can only be initialized once");

    if (version < 0) throw new IllegalArgumentException("version must be >= 0");

    this.version = version;
  }

  /**
   * Get the property name.
   *
   * @return the property name, may be <code>null</code>, never empty.
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
   * @return the property value, may be <code>null</code> or empty.
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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + (int) (nodeId ^ (nodeId >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PSHierarchyNodeProperty other = (PSHierarchyNodeProperty) obj;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    if (nodeId != other.nodeId) return false;
    return true;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSHierarchyNodeProperty{");
    sb.append("nodeId=").append(nodeId);
    sb.append(", name='").append(name).append('\'');
    sb.append(", version=").append(version);
    sb.append(", value='").append(value).append('\'');
    sb.append('}');
    return sb.toString();
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
