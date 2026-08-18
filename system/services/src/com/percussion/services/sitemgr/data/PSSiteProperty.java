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
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * Represents a single property for the site. Each property is keyed to a particular context.
 *
 * <p>Design-object XML root / nested package element is {@code site-property} (registered from
 * {@link PSSite} via {@link PSXmlSerializationHelper#addType}). Parent {@link #getSite()} is
 * suppressed (circular); restore is via {@link PSSite#setProperties(java.util.Set)}. Jackson opt-in
 * surface (issue #1918 / #1892 / epic #505).
 *
 * @author dougrand
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSSiteProperty")
@Table(name = "RXASSEMBLERPROPERTIES")
@JacksonXmlRootElement(localName = "site-property")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"contextId", "guid", "name", "propertyId", "value"})
public class PSSiteProperty implements IPSCatalogItem, Serializable {
  /** Serial id identifies versions of serialized data */
  private static final long serialVersionUID = 1L;

  static {
    // Register types with XML serializer for read creation of objects
    PSXmlSerializationHelper.addType("context", PSPublishingContext.class);
  }

  @Id
  @Column(name = "PROPERTYID")
  long propertyId;

  @Version
  @Column(name = "VERSION")
  Integer version;

  /**
   * Owning {@code RXASSEMBLERPROPERTIES.SITEID} mapping. Must stay insertable so Hibernate 6 writes
   * SITEID on INSERT (Create Site and Developer Virtual Site save). {@code insertable=false} plus a
   * parent-side {@code @JoinColumn} omitted the column and H2 rejected {@code NULL SITEID} (#3511 /
   * #3521). Parent {@link PSSite} properties use {@code mappedBy = "site"}.
   */
  @ManyToOne(targetEntity = PSSite.class, optional = false)
  @JoinColumn(name = "SITEID", nullable = false)
  IPSSite site;

  @Column(name = "CONTEXTID")
  Integer contextId;

  @Basic
  @Column(name = "PROPERTYNAME")
  String name;

  @Basic
  @Column(name = "PROPERTYVALUE")
  String value;

  /**
   * The database id for this object
   *
   * @return Returns the propertyId, never <code>null</code> after persistence
   */
  @JsonProperty
  public long getPropertyId() {
    return propertyId;
  }

  /**
   * @param propertyId The propertyId to set.
   */
  public void setPropertyId(long propertyId) {
    this.propertyId = propertyId;
  }

  /**
   * The parent site object
   *
   * @return Returns the site, never <code>null</code>
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public IPSSite getSite() {
    return site;
  }

  /**
   * @param site The site to set, may be <code>null</code> when disconnecting
   */
  @JsonIgnore
  public void setSite(IPSSite site) {
    this.site = site;
  }

  /**
   * Get the property name
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
   * Get the value
   *
   * @return Returns the value.
   */
  @JsonProperty
  public String getValue() {
    return value;
  }

  /**
   * @param value The value to set.
   */
  public void setValue(String value) {
    this.value = value;
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
   * @param version the version of the object, must be &gt;= 0.
   */
  public void setVersion(Integer version) {
    if (this.version != null && version != null)
      throw new IllegalStateException("Version can only be set once");

    this.version = version;
  }

  /**
   * Get the publishing context ID
   *
   * @return Returns the context ID, never <code>null</code> after initialization
   */
  @JsonProperty
  public IPSGuid getContextId() {
    if (contextId == null) {
      return null;
    }
    return new PSGuid(PSTypeEnum.CONTEXT, contextId);
  }

  /**
   * Set the publishing context id. Used by serialization and property assignment.
   *
   * @param ctxId the context id
   */
  public void setContextId(IPSGuid ctxId) {
    if (ctxId == null) throw new IllegalArgumentException("ctxId may not be null.");

    // Allow overwrite on design-object XML restore (BeanUtils + Jackson)
    contextId = ctxId.getUUID();
  }

  /** (non-Javadoc) */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSSiteProperty)) return false;
    PSSiteProperty b = (PSSiteProperty) obj;
    return new EqualsBuilder()
        .append(propertyId, b.propertyId)
        .append(name, b.name)
        .append(value, b.value)
        .isEquals();
  }

  /** (non-Javadoc) */
  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(name).toHashCode();
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSSiteProperty{");
    sb.append("propertyId=").append(propertyId);
    sb.append(", version=").append(version);
    sb.append(", site=").append(site);
    sb.append(", contextId=").append(contextId);
    sb.append(", name='").append(name).append('\'');
    sb.append(", value='").append(value).append('\'');
    sb.append('}');
    return sb.toString();
  }

  /** (non-Javadoc) */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }

  /** (non-Javadoc) */
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  /** (non-Javadoc) */
  @JsonProperty("guid")
  public IPSGuid getGUID() {
    return new PSGuid(PSTypeEnum.SITE_PROPERTY, getPropertyId());
  }

  /** (non-Javadoc) */
  public void setGUID(IPSGuid newguid) throws IllegalStateException {
    if (newguid == null) throw new IllegalArgumentException("newguid may be not null.");

    // Allow overwrite on design-object XML restore
    setPropertyId(newguid.longValue());
  }
}
