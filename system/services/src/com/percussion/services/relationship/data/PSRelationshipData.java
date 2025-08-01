/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.services.relationship.data;

import com.percussion.cms.IPSConstants;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipPropertyData;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FilterDef;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Modern JPA entity representing a single relationship row in the PSX_RELATIONSHIPS table.
 *
 * <p>This entity provides comprehensive relationship data management with enhanced
 * validation, Optional-based safe access, and Java 11 features. It maintains full
 * backward compatibility while offering improved type safety and null handling.</p>
 *
 * <p>Key features include:
 * <ul>
 *   <li>Optional-based property access for safe null handling</li>
 *   <li>Enhanced validation with descriptive error messages</li>
 *   <li>Stream API support for property collections</li>
 *   <li>Builder pattern support for object construction</li>
 *   <li>Immutable view methods for thread-safe access</li>
 * </ul>
 * </p>
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSRelationshipData")
@Table(name = IPSConstants.PSX_RELATIONSHIPS)
@FilterDef(name = "relationshipConfigFilter")
public class PSRelationshipData implements Serializable {

   private static final long serialVersionUID = 1L;

   /**
    * Unknown ID constant used for new relationships not yet persisted.
    */
   public static final int UNKNOWN_ID = -1;

   @Id
   @Column(name = "RID")
   private int rid = UNKNOWN_ID;

   @Basic
   @Column(name = "CONFIG_ID")
   private int configId = -1;

   @Basic
   @Column(name = "OWNER_ID")
   private int ownerId;

   @Basic
   @Column(name = "OWNER_REVISION")
   private int ownerRevision = -1;

   @Basic
   @Column(name = "DEPENDENT_ID")
   private int dependentId;

   @Basic
   @Column(name = "DEPENDENT_REVISION")
   private int dependentRevision = -1;

   @Basic
   @Column(name = "SLOT_ID")
   private Long slotId;

   @Basic
   @Column(name = "SORT_RANK")
   private Integer sortRank;

   @Basic
   @Column(name = "VARIANT_ID")
   private Long variantId;

   @Basic
   @Column(name = "FOLDER_ID")
   private Integer folderId;

   @Basic
   @Column(name = "SITE_ID")
   private Long siteId;

   @Basic
   @Column(name = "INLINE_RELATIONSHIP")
   private String inlineRelationship;

   @Basic
   @Column(name = "WIDGET_NAME")
   private String widgetName;

   @Transient
   private List<PSRelationshipPropertyData> childProperties = new ArrayList<>();

   @Transient
   private PSRelationshipConfig config;

   @Transient
   private boolean isPersisted = true;

   /**
    * Default constructor required by JPA/Hibernate.
    */
   public PSRelationshipData() {
      // JPA requires default constructor
   }

   /**
    * Creates a relationship data instance with comprehensive validation.
    *
    * @param relationshipId the relationship ID, must be {@link #UNKNOWN_ID} for new objects
    * @param config the relationship configuration, never {@code null}
    * @param ownerId the owner ID
    * @param ownerRevision the owner revision
    * @param dependentId the dependent ID
    * @param dependentRevision the dependent revision
    * @throws IllegalArgumentException if config is null
    */
   public PSRelationshipData(int relationshipId, PSRelationshipConfig config,
                           int ownerId, int ownerRevision, int dependentId, int dependentRevision) {
      this.config = Objects.requireNonNull(config, "Relationship config cannot be null");
      this.rid = relationshipId;
      this.ownerId = ownerId;
      this.ownerRevision = ownerRevision;
      this.dependentId = dependentId;
      this.dependentRevision = dependentRevision;
   }

   // Getters with enhanced documentation and validation

   /**
    * Gets the unique relationship identifier.
    *
    * @return the relationship ID, {@link #UNKNOWN_ID} for new objects
    */
   public int getId() {
      return rid;
   }

   /**
    * Gets the relationship configuration ID.
    *
    * @return the configuration ID, may be -1 if not set
    */
   public int getConfigId() {
      return configId;
   }

   /**
    * Gets the owner object ID.
    *
    * @return the owner ID
    */
   public int getOwnerId() {
      return ownerId;
   }

   /**
    * Gets the owner revision number.
    *
    * @return the owner revision, may be -1 if unknown
    */
   public int getOwnerRevision() {
      return ownerRevision;
   }

   /**
    * Gets the dependent object ID.
    *
    * @return the dependent ID
    */
   public int getDependentId() {
      return dependentId;
   }

   /**
    * Gets the dependent revision number.
    *
    * @return the dependent revision, may be -1 if unknown
    */
   public int getDependentRevision() {
      return dependentRevision;
   }

   /**
    * Gets the slot ID with safe null handling.
    *
    * @return the slot ID, -1 if not set
    */
   public long getSlotId() {
      return Optional.ofNullable(slotId).orElse(-1L);
   }

   /**
    * Gets the slot ID as Optional for safe access.
    *
    * @return Optional containing slot ID, empty if not set
    */
   public Optional<Long> getSlotIdOptional() {
      return Optional.ofNullable(slotId);
   }

   /**
    * Gets the sort rank with safe null handling.
    *
    * @return the sort rank, -1 if not set
    */
   public int getSortRank() {
      return Optional.ofNullable(sortRank).orElse(-1);
   }

   /**
    * Gets the sort rank as Optional for safe access.
    *
    * @return Optional containing sort rank, empty if not set
    */
   public Optional<Integer> getSortRankOptional() {
      return Optional.ofNullable(sortRank);
   }

   /**
    * Gets the variant/template ID with safe null handling.
    *
    * @return the variant ID, -1 if not set
    */
   public long getVariantId() {
      return Optional.ofNullable(variantId).orElse(-1L);
   }

   /**
    * Gets the variant ID as Optional for safe access.
    *
    * @return Optional containing variant ID, empty if not set
    */
   public Optional<Long> getVariantIdOptional() {
      return Optional.ofNullable(variantId);
   }

   /**
    * Gets the folder ID with safe null handling.
    *
    * @return the folder ID, -1 if not set
    */
   public int getFolderId() {
      return Optional.ofNullable(folderId).orElse(-1);
   }

   /**
    * Gets the folder ID as Optional for safe access.
    *
    * @return Optional containing folder ID, empty if not set
    */
   public Optional<Integer> getFolderIdOptional() {
      return Optional.ofNullable(folderId);
   }

   /**
    * Gets the site ID with safe null handling.
    *
    * @return the site ID, -1 if not set
    */
   public long getSiteId() {
      return Optional.ofNullable(siteId).orElse(-1L);
   }

   /**
    * Gets the site ID as Optional for safe access.
    *
    * @return Optional containing site ID, empty if not set
    */
   public Optional<Long> getSiteIdOptional() {
      return Optional.ofNullable(siteId);
   }

   /**
    * Gets the inline relationship property.
    *
    * @return Optional containing inline relationship value, empty if not set
    */
   public Optional<String> getInlineRelationship() {
      return Optional.ofNullable(inlineRelationship);
   }

   /**
    * Gets the widget name property.
    *
    * @return Optional containing widget name, empty if not set
    */
   public Optional<String> getWidgetName() {
      return Optional.ofNullable(widgetName);
   }

   // Enhanced setters with validation

   /**
    * Sets a valid relationship ID with validation.
    *
    * @param id the relationship ID, must be greater than 0
    * @throws IllegalArgumentException if id is not positive
    * @throws IllegalStateException if valid ID is already set
    */
   public void setId(int id) {
      if (id <= 0) {
         throw new IllegalArgumentException("Relationship ID must be positive: " + id);
      }
      if (this.rid > 0) {
         throw new IllegalStateException("Cannot reset an existing valid relationship ID");
      }
      this.rid = id;
   }

   /**
    * Sets the configuration ID with validation.
    *
    * @param configId the configuration ID, must be positive
    * @throws IllegalArgumentException if configId is not positive
    */
   public void setConfigId(int configId) {
      if (configId <= 0) {
         throw new IllegalArgumentException("Config ID must be positive: " + configId);
      }
      this.configId = configId;
   }

   /**
    * Sets the owner ID.
    *
    * @param ownerId the owner ID
    */
   public void setOwnerId(int ownerId) {
      this.ownerId = ownerId;
   }

   /**
    * Sets the owner revision.
    *
    * @param ownerRevision the owner revision
    */
   public void setOwnerRevision(int ownerRevision) {
      this.ownerRevision = ownerRevision;
   }

   /**
    * Sets the dependent ID.
    *
    * @param dependentId the dependent ID
    */
   public void setDependentId(int dependentId) {
      this.dependentId = dependentId;
   }

   /**
    * Sets the dependent revision.
    *
    * @param dependentRevision the dependent revision
    */
   public void setDependentRevision(int dependentRevision) {
      this.dependentRevision = dependentRevision;
   }

   /**
    * Sets the slot ID.
    *
    * @param slotId the slot ID
    */
   public void setSlotId(long slotId) {
      this.slotId = slotId;
   }

   /**
    * Sets the sort rank.
    *
    * @param sortRank the sort rank
    */
   public void setSortRank(int sortRank) {
      this.sortRank = sortRank;
   }

   /**
    * Sets the variant/template ID.
    *
    * @param variantId the variant ID
    */
   public void setVariantId(long variantId) {
      this.variantId = variantId;
   }

   /**
    * Sets the folder ID with zero conversion to -1.
    *
    * @param folderId the folder ID
    */
   public void setFolderId(int folderId) {
      this.folderId = (folderId == 0) ? -1 : folderId;
   }

   /**
    * Sets the site ID with zero conversion to -1.
    *
    * @param siteId the site ID
    */
   public void setSiteId(long siteId) {
      this.siteId = (siteId == 0) ? -1L : siteId;
   }

   /**
    * Sets the inline relationship property.
    *
    * @param inlineRelationship the inline relationship value
    */
   public void setInlineRelationship(String inlineRelationship) {
      this.inlineRelationship = inlineRelationship;
   }

   /**
    * Sets the widget name property.
    *
    * @param widgetName the widget name
    */
   public void setWidgetName(String widgetName) {
      this.widgetName = widgetName;
   }

   // Configuration and properties management

   /**
    * Sets the relationship configuration with validation.
    *
    * @param config the relationship configuration, never {@code null}
    * @throws IllegalArgumentException if config is null
    */
   public void setConfig(PSRelationshipConfig config) {
      this.config = Objects.requireNonNull(config, "Relationship config cannot be null");
   }

   /**
    * Gets the relationship configuration.
    *
    * @return the relationship configuration
    * @throws IllegalStateException if config has not been set
    */
   public PSRelationshipConfig getConfig() {
      if (config == null) {
         throw new IllegalStateException("Relationship config has not been set");
      }
      return config;
   }

   /**
    * Gets child properties as an immutable collection.
    *
    * @return immutable collection of child properties, never {@code null}
    */
   public Collection<PSRelationshipPropertyData> getChildProperties() {
      return List.copyOf(childProperties);
   }

   /**
    * Streams child properties for efficient processing.
    *
    * @return Stream of child properties, never {@code null}
    */
   public Stream<PSRelationshipPropertyData> streamChildProperties() {
      return childProperties.stream();
   }

   /**
    * Sets child properties with comprehensive validation.
    *
    * @param properties the child properties, never {@code null}
    * @throws IllegalArgumentException if properties is null
    * @throws IllegalStateException if config not set or no properties expected
    */
   public void setProperties(Collection<PSRelationshipPropertyData> properties) {
      Objects.requireNonNull(properties, "Child properties cannot be null");

      if (config == null) {
         throw new IllegalStateException("Must set config before setting properties");
      }

      if (config.getCustomPropertyNames().isEmpty()) {
         throw new IllegalStateException("This relationship type does not support additional properties");
      }

      childProperties.clear();
      properties.forEach(prop -> {
         prop.setPersisted(isPersisted());
         childProperties.add(prop);
      });
   }

   /**
    * Adds a single property with validation.
    *
    * @param property the property to add, never {@code null}
    * @throws IllegalArgumentException if property is null
    */
   public void addProperty(PSRelationshipPropertyData property) {
      Objects.requireNonNull(property, "Property cannot be null");
      property.setPersisted(isPersisted());
      childProperties.add(property);
   }

   /**
    * Gets a property by name with Optional-based safe access.
    *
    * @param propertyName the property name to search for
    * @return Optional containing the property if found, empty otherwise
    */
   public Optional<PSRelationshipPropertyData> getProperty(String propertyName) {
      if (propertyName == null || propertyName.trim().isEmpty()) {
         return Optional.empty();
      }

      return childProperties.stream()
            .filter(prop -> prop.getName().equalsIgnoreCase(propertyName))
            .findFirst();
   }

   // Persistence state management

   /**
    * Checks if this object is persisted in the repository.
    *
    * @return {@code true} if persisted, {@code false} otherwise
    */
   public boolean isPersisted() {
      return rid != UNKNOWN_ID && isPersisted;
   }

   /**
    * Sets the persistence status with property propagation.
    *
    * @param isPersisted the persistence status
    */
   public void setPersisted(boolean isPersisted) {
      this.isPersisted = isPersisted;
      if (!isPersisted) {
         childProperties.forEach(prop -> prop.setPersisted(false));
      }
   }

   // Enhanced object methods

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PSRelationshipData)) return false;
      var other = (PSRelationshipData) obj;
      return rid == other.rid;
   }

   @Override
   public int hashCode() {
      return Objects.hash(rid);
   }

   @Override
   public String toString() {
      return String.format("PSRelationshipData{rid=%d, configId=%d, ownerId=%d, ownerRev=%d, " +
                          "dependentId=%d, dependentRev=%d, slotId=%s, sortRank=%s, variantId=%s, " +
                          "folderId=%s, siteId=%s, inline='%s', widget='%s', persisted=%s}",
            rid, configId, ownerId, ownerRevision, dependentId, dependentRevision,
            slotId, sortRank, variantId, folderId, siteId,
            inlineRelationship, widgetName, isPersisted);
   }

   /**
    * Creates a builder for constructing PSRelationshipData instances.
    *
    * @return a new builder instance
    */
   public static Builder builder() {
      return new Builder();
   }

   /**
    * Builder pattern for creating PSRelationshipData instances with validation.
    */
   public static class Builder {
      private final PSRelationshipData data = new PSRelationshipData();

      public Builder withId(int id) {
         data.rid = id;
         return this;
      }

      public Builder withConfig(PSRelationshipConfig config) {
         data.setConfig(config);
         return this;
      }

      public Builder withOwner(int ownerId, int ownerRevision) {
         data.ownerId = ownerId;
         data.ownerRevision = ownerRevision;
         return this;
      }

      public Builder withDependent(int dependentId, int dependentRevision) {
         data.dependentId = dependentId;
         data.dependentRevision = dependentRevision;
         return this;
      }

      public Builder withSlotId(long slotId) {
         data.slotId = slotId;
         return this;
      }

      public Builder withSortRank(int sortRank) {
         data.sortRank = sortRank;
         return this;
      }

      public PSRelationshipData build() {
         Objects.requireNonNull(data.config, "Config must be set");
         return data;
      }
   }
}
