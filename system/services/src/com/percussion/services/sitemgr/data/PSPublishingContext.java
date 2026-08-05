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
import com.percussion.services.catalog.IPSCatalogIdentifier;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import com.percussion.utils.xml.PSInvalidXmlException;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * The publishing context controls how links are generated in the HTML documents during assembly.
 *
 * <p>Design-object XML root is {@code publishing-context} under the Jackson-backed {@link
 * PSXmlSerializationHelper} (issue #1918 / #1892 / epic #505). Historical attribute-shaped {@code
 * PSXPublishingContext} root is a documented deviation (see task deviations doc). {@code
 * default-scheme} object is suppressed; wire form uses {@link #getDefaultSchemeId()}.
 *
 * @author dougrand
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSPublishingContext")
@Table(name = "RXCONTEXT")
@JacksonXmlRootElement(localName = "publishing-context")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"defaultSchemeId", "description", "guid", "id", "name"})
public class PSPublishingContext
    implements IPSCatalogItem, IPSPublishingContext, Serializable, IPSCatalogIdentifier, Cloneable {
  /** Serial id identifies versions of serialized data */
  private static final long serialVersionUID = 1L;

  static {
    // Register types with XML serializer for read creation of objects
    PSXmlSerializationHelper.addType("default-scheme", PSLocationScheme.class);
  }

  @Id
  @Column(name = "CONTEXTID")
  private long id = -1L;

  @Version
  @Column(name = "VERSION")
  private Integer version;

  @Basic
  @Column(name = "CONTEXTNAME")
  String name;

  @Basic
  @Column(name = "CONTEXTDESC")
  String description;

  @Basic
  @Column(name = "DEFAULTSCHEMEID")
  Long defaultSchemeId;

  transient IPSLocationScheme m_defaultScheme;

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#getDescription()
   */
  @JsonProperty
  public String getDescription() {
    return description;
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#setDescription(java.lang.String)
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Get the hibernate version information for this object.
   *
   * @return returns the version, may be <code>null</code>.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public Integer getVersion() {
    return version;
  }

  /**
   * Set the hibernate version information for this object.
   *
   * @param version The version to set.
   * @throws IllegalStateException if an attempt is made to set a previously set version to a
   *     non-<code>null</code> value.
   */
  public void setVersion(Integer version) {
    if (this.version != null && version != null)
      throw new IllegalStateException("Version can only be set once");

    this.version = version;
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#getGUID()
   */
  @JsonProperty("guid")
  public IPSGuid getGUID() {
    return id == -1L ? null : new PSGuid(PSTypeEnum.CONTEXT, id);
  }

  /* (non-Javadoc)
   * @see com.percussion.services.catalog.IPSCatalogItem#setGUID(IPSGuid)
   */
  public void setGUID(IPSGuid guid) {
    if (guid == null) throw new IllegalArgumentException("guid may not be null");

    // Allow overwrite on design-object XML restore (BeanUtils + Jackson)
    id = guid.longValue();
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#getName()
   */
  @JsonProperty
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#setName(java.lang.String)
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty");
    }
    this.name = name;
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#getDefaultSchemeId()
   */
  @JsonProperty
  public IPSGuid getDefaultSchemeId() {
    return defaultSchemeId == null
        ? null
        : new PSGuid(PSTypeEnum.LOCATION_SCHEME, defaultSchemeId);
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#setDefaultSchemeId(com.percussion.services.sitemgr.IPSLocationScheme)
   */
  public void setDefaultSchemeId(IPSGuid schemeId) {
    if (schemeId == null) {
      this.defaultSchemeId = null;
      m_defaultScheme = null;
    } else {
      this.defaultSchemeId = schemeId.longValue();
      if (m_defaultScheme != null && (!m_defaultScheme.getGUID().equals(schemeId))) {
        m_defaultScheme = null;
      }
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSPublishingContext)) return false;
    PSPublishingContext pb = (PSPublishingContext) obj;

    EqualsBuilder builder =
        new EqualsBuilder()
            .append(description, pb.description)
            .append(name, pb.name)
            .append(id, pb.id)
            .append(defaultSchemeId, pb.defaultSchemeId);

    return builder.isEquals();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(name).toHashCode();
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSPublishingContext{");
    sb.append("id=").append(id);
    sb.append(", version=").append(version);
    sb.append(", name='").append(name).append('\'');
    sb.append(", description='").append(description).append('\'');
    sb.append(", defaultSchemeId=").append(defaultSchemeId);
    sb.append(", m_defaultScheme=").append(m_defaultScheme);
    sb.append('}');
    return sb.toString();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#getId()
   */
  @JsonProperty
  public Integer getId() {
    return (int) id;
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#setId(java.lang.Integer)
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#getDefaultScheme()
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public IPSLocationScheme getDefaultScheme() {
    return m_defaultScheme;
  }

  /* (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#setDefaultScheme(com.percussion.services.sitemgr.IPSLocationScheme)
   */
  /**
   * Associate a default scheme object. {@code null} clears the association (BeanUtils property-copy
   * after Jackson deserialize may pass null when the object was suppressed on the wire).
   */
  @JsonIgnore
  public void setDefaultScheme(IPSLocationScheme defaultScheme) {
    if (defaultScheme == null) {
      m_defaultScheme = null;
      return;
    }
    setDefaultSchemeId(defaultScheme.getGUID());
    this.m_defaultScheme = defaultScheme;
  }

  /**
   * Restores this object's state from its XML representation via {@link
   * PSXmlSerializationHelper}.
   */
  public void fromXML(String xmlsource) throws IOException, SAXException, PSInvalidXmlException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  /**
   * Serializes this object's state via {@link PSXmlSerializationHelper} (Jackson element shape).
   *
   * <p>Historical attribute-shaped root {@code PSXPublishingContext} is no longer written; modern
   * root is {@code publishing-context} with child elements.
   */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.services.sitemgr.IPSPublishingContext#copy(com.percussion.services.sitemgr.IPSPublishingContext)
   */
  public void copy(IPSPublishingContext other) {
    if (other == null) throw new IllegalArgumentException("other may not be null.");
    if (!(other instanceof PSPublishingContext))
      throw new IllegalArgumentException("other must be instance of PSPublishingContext.");
    PSPublishingContext src = (PSPublishingContext) other;
    name = src.name;
    description = src.description;
    defaultSchemeId = src.defaultSchemeId;
    m_defaultScheme = src.m_defaultScheme;
  }

  /**
   * Historical root node name of the attribute-shaped XML representation (pre-Jackson helper).
   * Retained for callers that still reference the constant.
   */
  public static final String XML_NODE_NAME = "PSXPublishingContext";
}
