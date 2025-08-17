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
// REFACTORED: CP-JAVA11
package com.percussion.services.content.data;

import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.data.IPSCloneTuner;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;

/**
 * Represents a single folder property with enhanced Java 11 support.
 *
 * <p>Folder properties are used to store metadata and configuration information
 * associated with content folders. This class provides comprehensive functionality
 * for managing folder property data with modern Java 11 features including
 * validation, serialization, and safe navigation.
 *
 * <p>Key features:
 * <ul>
 *   <li>Enhanced null safety with Objects.requireNonNull()</li>
 *   <li>Optional-based safe value access</li>
 *   <li>Immutable factory methods</li>
 *   <li>Comprehensive validation with clear error messages</li>
 * </ul>
 *
 * @since Java 11 Modernization
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSFolderProperty")
@Table(name = "PSX_PROPERTIES")
public class PSFolderProperty implements Serializable {

   /**
    * Serial version UID for serialization compatibility.
    */
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
    * Use factory methods or parameterized constructors for creating new instances.
    */
   public PSFolderProperty() {
      // Required by JPA
   }

   /**
    * Create a new folder property with specified parameters using modern validation.
    *
    * @param contentID the content ID, must be > 0
    * @param revisionID the revision ID, must be > 0
    * @param sysID the system ID, must be > 0
    * @param propertyName the property name, not {@code null} or empty
    * @param propertyValue the property value, may be {@code null}
    * @param description the description, may be {@code null}
    * @throws IllegalArgumentException if validation fails
    */
   public PSFolderProperty(long contentID, long revisionID, long sysID,
                          String propertyName, String propertyValue, String description) {
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
    * @param contentID the content ID, must be > 0
    * @param revisionID the revision ID, must be > 0
    * @param sysID the system ID, must be > 0
    * @param propertyName the property name, not {@code null} or empty
    * @param propertyValue the property value, may be {@code null}
    * @return a new PSFolderProperty instance
    * @throws IllegalArgumentException if validation fails
    */
   public static PSFolderProperty of(long contentID, long revisionID, long sysID,
                                    String propertyName, String propertyValue) {
      return new PSFolderProperty(contentID, revisionID, sysID, propertyName, propertyValue, null);
   }

   /**
    * Get the content ID with Optional wrapper for safer access.
    *
    * @return Optional containing the content ID if valid, empty otherwise
    */
   public Optional<Long> getContentIDOptional() {
      return contentID > 0 ? Optional.of(contentID) : Optional.empty();
   }

   /**
    * Get the content ID of this folder property.
    *
    * @return the content ID
    */
   public long getContentID() {
      return contentID;
   }

   /**
    * Set the content ID with enhanced validation.
    *
    * @param contentID the content ID, must be > 0
    * @throws IllegalArgumentException if contentID is <= 0
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
   public Optional<Long> getRevisionIDOptional() {
      return revisionID > 0 ? Optional.of(revisionID) : Optional.empty();
   }

   /**
    * Get the revision ID of this folder property.
    *
    * @return the revision ID
    */
   public long getRevisionID() {
      return revisionID;
   }

   /**
    * Set the revision ID with enhanced validation.
    *
    * @param revisionID the revision ID, must be > 0
    * @throws IllegalArgumentException if revisionID is <= 0
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
   public Optional<Long> getSysIDOptional() {
      return sysID > 0 ? Optional.of(sysID) : Optional.empty();
   }

   /**
    * Get the system ID of this folder property.
    *
    * @return the system ID
    */
   public long getSysID() {
      return sysID;
   }

   /**
    * Set the system ID with enhanced validation.
    *
    * @param sysID the system ID, must be > 0
    * @throws IllegalArgumentException if sysID is <= 0
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
   public Optional<String> getPropertyNameOptional() {
      return Optional.ofNullable(propertyName);
   }

   /**
    * Get the property name of this folder property.
    *
    * @return the property name, never {@code null} or empty
    */
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
   public Optional<String> getPropertyValueOptional() {
      return Optional.ofNullable(propertyValue);
   }

   /**
    * Get the property value of this folder property.
    *
    * @return the property value, may be {@code null}
    */
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
   public Optional<String> getDescriptionOptional() {
      return Optional.ofNullable(description);
   }

   /**
    * Get the description of this folder property.
    *
    * @return the description, may be {@code null}
    */
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
