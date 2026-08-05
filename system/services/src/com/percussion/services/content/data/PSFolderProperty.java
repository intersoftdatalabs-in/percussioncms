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
// REFACTORED: CP-JAVA11
package com.percussion.services.content.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a single folder property with enhanced Java 11 support.
 *
 * <p>Design-object XML root is {@code folder-property}. Jackson opt-in surface suppresses {@link
 * Optional} accessors (issue #1921 / epic #505).
 *
 * @since Java 11 Modernization
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSFolderProperty")
@Table(name = "PSX_PROPERTIES")
@JacksonXmlRootElement(localName = "folder-property")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "contentID",
  "description",
  "propertyName",
  "propertyValue",
  "revisionID",
  "sysID"
})
public class PSFolderProperty implements Serializable {

  /** Serial version UID for serialization compatibility. */
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "CONTENTID", nullable = false)
  private long contentID;

  @Id
  @Column(name = "REVISIONID", nullable = false)
  private long revisionID;

  @Id
  @Column(name = "SYSID", nullable = false)
  private long sysID;

  @Id
  @Column(name = "PROPERTYNAME", nullable = false, length = 50)
  private String propertyName;

  @Basic
  @Column(name = "PROPERTYVALUE", nullable = true, length = 4000)
  private String propertyValue;

  @Basic
  @Column(name = "DESCRIPTION", nullable = true, length = 255)
  private String description;

  /**
   * Default constructor required for JPA/Hibernate.
   *
   * <p>Use factory methods or parameterized constructors for creating new instances.
   */
  public PSFolderProperty() {
    // Required by JPA
  }

  /**
   * Create a new folder property with specified parameters using modern validation.
   *
   * @param contentID the content ID, must be &gt; 0
   * @param revisionID the revision ID, must be &gt; 0
   * @param sysID the system ID, must be &gt; 0
   * @param propertyName the property name, not {@code null} or empty
   * @param propertyValue the property value, may be {@code null}
   * @param description the description, may be {@code null}
   * @throws IllegalArgumentException if validation fails
   */
  public PSFolderProperty(
      long contentID,
      long revisionID,
      long sysID,
      String propertyName,
      String propertyValue,
      String description) {
    if (contentID <= 0) {
      throw new IllegalArgumentException("contentID must be > 0");
    }
    if (revisionID <= 0) {
      throw new IllegalArgumentException("revisionID must be > 0");
    }
    if (sysID <= 0) {
      throw new IllegalArgumentException("sysID must be > 0");
    }
    if (StringUtils.isBlank(propertyName)) {
      throw new IllegalArgumentException("propertyName cannot be null or empty");
    }

    this.contentID = contentID;
    this.revisionID = revisionID;
    this.sysID = sysID;
    this.propertyName = propertyName;
    this.propertyValue = propertyValue;
    this.description = description;
  }

  /**
   * Create a new folder property using factory method with enhanced validation.
   *
   * @param contentID the content ID, must be &gt; 0
   * @param revisionID the revision ID, must be &gt; 0
   * @param sysID the system ID, must be &gt; 0
   * @param propertyName the property name, not {@code null} or empty
   * @param propertyValue the property value, may be {@code null}
   * @return a new PSFolderProperty instance
   * @throws IllegalArgumentException if validation fails
   */
  public static PSFolderProperty of(
      long contentID, long revisionID, long sysID, String propertyName, String propertyValue) {
    return new PSFolderProperty(contentID, revisionID, sysID, propertyName, propertyValue, null);
  }

  /**
   * Get the content ID with Optional wrapper for safer access.
   *
   * @return Optional containing the content ID if valid, empty otherwise
   */
  @JsonIgnore
  public Optional<Long> getContentIDOptional() {
    return contentID > 0 ? Optional.of(contentID) : Optional.empty();
  }

  /**
   * Get the content ID of this folder property.
   *
   * @return the content ID
   */
  @JsonProperty
  public long getContentID() {
    return contentID;
  }

  /**
   * Set the content ID with enhanced validation.
   *
   * @param contentID the content ID, must be &gt; 0
   * @throws IllegalArgumentException if contentID is &lt;= 0
   */
  public void setContentID(long contentID) {
    if (contentID <= 0) {
      throw new IllegalArgumentException("contentID must be > 0");
    }
    this.contentID = contentID;
  }

  /**
   * Get the revision ID with Optional wrapper for safer access.
   *
   * @return Optional containing the revision ID if valid, empty otherwise
   */
  @JsonIgnore
  public Optional<Long> getRevisionIDOptional() {
    return revisionID > 0 ? Optional.of(revisionID) : Optional.empty();
  }

  /**
   * Get the revision ID of this folder property.
   *
   * @return the revision ID
   */
  @JsonProperty
  public long getRevisionID() {
    return revisionID;
  }

  /**
   * Set the revision ID with enhanced validation.
   *
   * @param revisionID the revision ID, must be &gt; 0
   * @throws IllegalArgumentException if revisionID is &lt;= 0
   */
  public void setRevisionID(long revisionID) {
    if (revisionID <= 0) {
      throw new IllegalArgumentException("revisionID must be > 0");
    }
    this.revisionID = revisionID;
  }

  /**
   * Get the system ID with Optional wrapper for safer access.
   *
   * @return Optional containing the system ID if valid, empty otherwise
   */
  @JsonIgnore
  public Optional<Long> getSysIDOptional() {
    return sysID > 0 ? Optional.of(sysID) : Optional.empty();
  }

  /**
   * Get the system ID of this folder property.
   *
   * @return the system ID
   */
  @JsonProperty
  public long getSysID() {
    return sysID;
  }

  /**
   * Set the system ID with enhanced validation.
   *
   * @param sysID the system ID, must be &gt; 0
   * @throws IllegalArgumentException if sysID is &lt;= 0
   */
  public void setSysID(long sysID) {
    if (sysID <= 0) {
      throw new IllegalArgumentException("sysID must be > 0");
    }
    this.sysID = sysID;
  }

  /**
   * Get the property name with Optional wrapper.
   *
   * @return Optional containing the property name if non-null, empty otherwise
   */
  @JsonIgnore
  public Optional<String> getPropertyNameOptional() {
    return Optional.ofNullable(propertyName);
  }

  /**
   * Get the property name of this folder property.
   *
   * @return the property name, never {@code null} or empty
   */
  @JsonProperty
  public String getPropertyName() {
    return propertyName;
  }

  /**
   * Set the property name with enhanced validation.
   *
   * @param propertyName the property name, not {@code null} or empty
   * @throws IllegalArgumentException if propertyName is null or empty
   */
  public void setPropertyName(String propertyName) {
    if (StringUtils.isBlank(propertyName)) {
      throw new IllegalArgumentException("propertyName cannot be null or empty");
    }
    this.propertyName = propertyName;
  }

  /**
   * Get the property value with Optional wrapper.
   *
   * @return Optional containing the property value if non-null, empty otherwise
   */
  @JsonIgnore
  public Optional<String> getPropertyValueOptional() {
    return Optional.ofNullable(propertyValue);
  }

  /**
   * Get the property value of this folder property.
   *
   * @return the property value, may be {@code null}
   */
  @JsonProperty
  public String getPropertyValue() {
    return propertyValue;
  }

  /**
   * Set the property value.
   *
   * @param propertyValue the property value, may be {@code null}
   */
  public void setPropertyValue(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  /**
   * Get the description with Optional wrapper.
   *
   * @return Optional containing the description if non-null, empty otherwise
   */
  @JsonIgnore
  public Optional<String> getDescriptionOptional() {
    return Optional.ofNullable(description);
  }

  /**
   * Get the description of this folder property.
   *
   * @return the description, may be {@code null}
   */
  @JsonProperty
  public String getDescription() {
    return description;
  }

  /**
   * Set the description.
   *
   * @param description the description, may be {@code null}
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Serialize this folder property to design-object XML via {@link PSXmlSerializationHelper}.
   *
   * @return XML string, never {@code null}
   * @throws IOException on write failure
   * @throws SAXException on SAX failure
   */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }

  /**
   * Restore this folder property from design-object XML via {@link PSXmlSerializationHelper}.
   *
   * @param xmlsource XML source, never blank
   * @throws IOException on parse failure
   * @throws SAXException on SAX failure
   */
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PSFolderProperty)) return false;

    var other = (PSFolderProperty) obj;
    return new EqualsBuilder()
        .append(contentID, other.contentID)
        .append(revisionID, other.revisionID)
        .append(sysID, other.sysID)
        .append(propertyName, other.propertyName)
        .append(propertyValue, other.propertyValue)
        .append(description, other.description)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(contentID)
        .append(revisionID)
        .append(sysID)
        .append(propertyName)
        .append(propertyValue)
        .append(description)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("contentID", contentID)
        .append("revisionID", revisionID)
        .append("sysID", sysID)
        .append("propertyName", propertyName)
        .append("propertyValue", propertyValue)
        .append("description", description)
        .toString();
  }
}
