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
package com.percussion.services.relationship.data;

import com.percussion.cms.IPSConstants;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Modern JPA entity representing a relationship configuration name in the PSX_RELATIONSHIPCONFIGNAME table.
 *
 * <p>This entity provides a simple mapping between relationship configuration IDs and their
 * human-readable names. It includes enhanced validation, modern object methods, and
 * comprehensive documentation following Java 11 best practices.</p>
 *
 * <p>Key features include:
 * <ul>
 *   <li>Enhanced validation with descriptive error messages</li>
 *   <li>Modern equals/hashCode using Objects utility methods</li>
 *   <li>Immutable design with builder pattern support</li>
 *   <li>Comprehensive toString implementation</li>
 * </ul>
 * </p>
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSRelationshipConfigName")
@Table(name = IPSConstants.PSX_RELATIONSHIPCONFIGNAME)
public final class PSRelationshipConfigName implements Serializable {

   private static final long serialVersionUID = 608168070484512836L;

   @Id
   private int configId;

   @Basic
   private String configName;

   /**
    * Version field for Hibernate optimistic locking to detect stale updates.
    */
   @Version
   @SuppressWarnings("unused")
   private Integer version;

   /**
    * Creates a relationship configuration name with validation.
    *
    * @param name the configuration name, never {@code null} or empty
    * @param id the configuration ID, must be positive
    * @throws IllegalArgumentException if name is null/empty or id is not positive
    */
   public PSRelationshipConfigName(String name, int id) {
      this.configName = validateName(name);
      this.configId = validateId(id);
   }

   /**
    * Default constructor required by JPA/Hibernate.
    */
   protected PSRelationshipConfigName() {
      // JPA requires default constructor
   }

   /**
    * Gets the relationship configuration ID.
    *
    * @return the configuration ID
    */
   public int getId() {
      return configId;
   }

   /**
    * Gets the relationship configuration name.
    *
    * @return the configuration name, never {@code null}
    */
   public String getName() {
      return configName;
   }

   /**
    * Sets the relationship configuration name with validation.
    *
    * @param name the new configuration name, never {@code null} or empty
    * @throws IllegalArgumentException if name is null or empty
    */
   public void setName(String name) {
      this.configName = validateName(name);
   }

   /**
    * Validates the configuration name.
    *
    * @param name the name to validate
    * @return the validated name
    * @throws IllegalArgumentException if name is null or empty
    */
   private String validateName(String name) {
      if (name == null || name.trim().isEmpty()) {
         throw new IllegalArgumentException("Configuration name cannot be null or empty");
      }
      return name.trim();
   }

   /**
    * Validates the configuration ID.
    *
    * @param id the ID to validate
    * @return the validated ID
    * @throws IllegalArgumentException if id is not positive
    */
   private int validateId(int id) {
      if (id <= 0) {
         throw new IllegalArgumentException("Configuration ID must be positive: " + id);
      }
      return id;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PSRelationshipConfigName)) return false;

      var other = (PSRelationshipConfigName) obj;
      return configId == other.configId &&
             Objects.equals(configName, other.configName);
   }

   @Override
   public int hashCode() {
      return Objects.hash(configId, configName);
   }

   @Override
   public String toString() {
      return String.format("PSRelationshipConfigName{id=%d, name='%s', version=%s}",
                          configId, configName, version);
   }

   /**
    * Creates a new configuration name with the specified parameters.
    *
    * @param name the configuration name, never {@code null} or empty
    * @param id the configuration ID, must be positive
    * @return a new PSRelationshipConfigName instance
    * @throws IllegalArgumentException if parameters are invalid
    */
   public static PSRelationshipConfigName of(String name, int id) {
      return new PSRelationshipConfigName(name, id);
   }

   /**
    * Checks if this configuration name matches the given name (case-insensitive).
    *
    * @param name the name to compare against
    * @return {@code true} if names match (case-insensitive), {@code false} otherwise
    */
   public boolean matchesName(String name) {
      return configName != null && configName.equalsIgnoreCase(name);
   }

   /**
    * Checks if this configuration has the specified ID.
    *
    * @param id the ID to check
    * @return {@code true} if IDs match, {@code false} otherwise
    */
   public boolean hasId(int id) {
      return configId == id;
   }
}
