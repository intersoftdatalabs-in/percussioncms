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
package com.percussion.services.publisher;

import com.percussion.services.catalog.IPSCatalogIdentifier;
import com.percussion.services.publisher.data.PSEditionType;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An edition is a collection of content lists and tasks for a site that embodies a unit
 * of publishing work with comprehensive Java 11 modernization. An edition is "run" and first
 * the pre-tasks associated with the edition are executed. Then the content lists are evaluated
 * and the content published to the delivery engine. Finally, the post-tasks associated with
 * the edition are executed.
 * <p>
 * Any particular edition will only be run singly, but multiple editions can be run
 * simultaneously on a given site.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for nullable properties</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Improved enum design with lookup capabilities</li>
 * <li>Stream API for priority operations</li>
 * </ul>
 *
 * @author dougrand
 */
public interface IPSEdition extends IPSCatalogIdentifier {

   /**
    * Priorities of the Edition with enhanced Java 11 features.
    */
   enum Priority {
      /**
       * The highest priority
       */
      HIGHEST(5),
      
      /**
       * High priority, but less than the {@link #HIGHEST}
       */
      HIGH(4),
      
      /**
       * Medium priority
       */
      MEDIUM(3),
      
      /**
       * Low priority, but higher than {@link #LOWEST}
       */
      LOW(2),
      
      /**
       * The lowest priority
       */
      LOWEST(1);
      
      private final int value;

      /**
       * Constructs a priority from a value.
       * @param value the value of the priority
       */
      Priority(int value) {
         this.value = value;
      }
      
      /**
       * Gets the value of the priority.
       * @return the value of the priority
       */
      public int getValue() {
         return value;
      }
      
      /**
       * Find priority by value with Optional return.
       *
       * @param value the priority value to search for
       * @return Optional containing the matching priority, empty if not found
       */
      public static Optional<Priority> findByValue(int value) {
         return Stream.of(values())
             .filter(p -> p.getValue() == value)
             .findFirst();
      }

      /**
       * Find priority by value with fallback to default.
       *
       * @param value the priority value to search for
       * @param defaultPriority the default priority if not found, not {@code null}
       * @return the matching priority or the default
       * @throws IllegalArgumentException if defaultPriority is null
       */
      public static Priority findByValueOrDefault(int value, Priority defaultPriority) {
         Objects.requireNonNull(defaultPriority, "defaultPriority cannot be null");
         return findByValue(value).orElse(defaultPriority);
      }

      /**
       * Check if this priority is higher than another priority.
       *
       * @param other the priority to compare against, not {@code null}
       * @return {@code true} if this priority is higher
       * @throws IllegalArgumentException if other is null
       */
      public boolean isHigherThan(Priority other) {
         Objects.requireNonNull(other, "other cannot be null");
         return this.value > other.value;
      }

      /**
       * Check if this priority is lower than another priority.
       *
       * @param other the priority to compare against, not {@code null}
       * @return {@code true} if this priority is lower
       * @throws IllegalArgumentException if other is null
       */
      public boolean isLowerThan(Priority other) {
         Objects.requireNonNull(other, "other cannot be null");
         return this.value < other.value;
      }
   }

   /**
    * Get the unique name of this edition. The name has limitations on allowed chars.
    *
    * @return Never {@code null} or empty
    */
   String getName();
   
   /**
    * Set the name of this edition with enhanced validation.
    * @param name the name, not {@code null} or empty
    * @throws IllegalArgumentException if name is null or empty
    */
   default void setName(String name) {
      Objects.requireNonNull(name, "name cannot be null");
      if (name.trim().isEmpty()) {
         throw new IllegalArgumentException("name cannot be empty");
      }
      setNameImpl(name.trim());
   }

   /**
    * Internal implementation for name setting.
    */
   void setNameImpl(String name);

   /**
    * Get the display title safely, which is shown in the user interface.
    * @return Optional containing the display title if present and non-empty, empty otherwise
    */
   default Optional<String> findDisplayTitle() {
      return Optional.ofNullable(getDisplayTitle())
          .filter(title -> !title.trim().isEmpty());
   }

   /**
    * Get the display title, which is shown in the user interface.
    * @return May be {@code null}, never empty
    */
   String getDisplayTitle();

   /**
    * Set the display title with validation.
    * @param displayTitle the display title, may be {@code null}
    */
   void setDisplayTitle(String displayTitle);

   /**
    * Get the comment about the edition safely.
    * @return Optional containing the comment if present and non-empty, empty otherwise
    */
   default Optional<String> findComment() {
      return Optional.ofNullable(getComment())
          .filter(comment -> !comment.trim().isEmpty());
   }

   /**
    * Get the comment about the edition.
    * @return the comment, may be {@code null}
    */
   String getComment();

   /**
    * Set the comment with validation.
    * @param comment the comment, may be {@code null}
    */
   void setComment(String comment);

   /**
    * Get the edition type safely.
    * @return Optional containing the edition type if present, empty otherwise
    */
   default Optional<PSEditionType> findEditionType() {
      return Optional.ofNullable(getEditionType());
   }

   /**
    * Get the edition type.
    * @return the edition type, never {@code null}
    */
   PSEditionType getEditionType();

   /**
    * Set the edition type with enhanced validation.
    * @param editionType the edition type, not {@code null}
    * @throws IllegalArgumentException if editionType is null
    */
   default void setEditionType(PSEditionType editionType) {
      Objects.requireNonNull(editionType, "editionType cannot be null");
      setEditionTypeImpl(editionType);
   }

   /**
    * Internal implementation for edition type setting.
    */
   void setEditionTypeImpl(PSEditionType editionType);

   /**
    * Get the destination site ID safely.
    * @return Optional containing the site ID if present, empty otherwise
    */
   default Optional<IPSGuid> findSiteId() {
      return Optional.ofNullable(getSiteId());
   }

   /**
    * Get the destination site ID.
    * @return the destination site ID, may be {@code null}
    */
   IPSGuid getSiteId();

   /**
    * Set the destination site ID.
    * @param siteId the destination site ID, may be {@code null}
    */
   void setSiteId(IPSGuid siteId);

   /**
    * Gets the publish server ID if it is not {@code null}; otherwise gets the site ID.
    * @return Optional containing the publish server or site ID, empty if both are null
    */
   default Optional<IPSGuid> findPubServerOrSiteId() {
      return Optional.ofNullable(getPubServerOrSiteId());
   }

   /**
    * Gets the publish server ID if it is not {@code null}; otherwise gets the site ID.
    * @return the publish server or site ID, may be {@code null}
    */
   IPSGuid getPubServerOrSiteId();
   
   /**
    * Get the publish server ID safely.
    * @return Optional containing the publish server ID if present, empty otherwise
    */
   default Optional<IPSGuid> findPubServerId() {
      return Optional.ofNullable(getPubServerId());
   }

   /**
    * Get the publish server ID.
    * @return the publish server ID, may be {@code null}
    */
   IPSGuid getPubServerId();

   /**
    * Set the publish server ID.
    * @param serverId the publish server ID, may be {@code null}
    */
   void setPubServerId(IPSGuid serverId);

   /**
    * Get the priority.
    * @return the priority, never {@code null}
    */
   Priority getPriority();

   /**
    * Set the priority with enhanced validation.
    * @param priority the priority, not {@code null}
    * @throws IllegalArgumentException if priority is null
    */
   default void setPriority(Priority priority) {
      Objects.requireNonNull(priority, "priority cannot be null");
      setPriorityImpl(priority);
   }

   /**
    * Internal implementation for priority setting.
    */
   void setPriorityImpl(Priority priority);

   /**
    * Check if this edition has a display title.
    * @return {@code true} if display title is present and non-empty
    */
   default boolean hasDisplayTitle() {
      return findDisplayTitle().isPresent();
   }

   /**
    * Check if this edition has a comment.
    * @return {@code true} if comment is present and non-empty
    */
   default boolean hasComment() {
      return findComment().isPresent();
   }

   /**
    * Check if this edition has a site ID configured.
    * @return {@code true} if site ID is present
    */
   default boolean hasSiteId() {
      return findSiteId().isPresent();
   }

   /**
    * Check if this edition has a publish server ID configured.
    * @return {@code true} if publish server ID is present
    */
   default boolean hasPubServerId() {
      return findPubServerId().isPresent();
   }

   /**
    * Check if this edition has high priority (HIGH or HIGHEST).
    * @return {@code true} if priority is HIGH or HIGHEST
    */
   default boolean isHighPriority() {
      var priority = getPriority();
      return priority == Priority.HIGH || priority == Priority.HIGHEST;
   }

   /**
    * Check if this edition has low priority (LOW or LOWEST).
    * @return {@code true} if priority is LOW or LOWEST
    */
   default boolean isLowPriority() {
      var priority = getPriority();
      return priority == Priority.LOW || priority == Priority.LOWEST;
   }

   /**
    * Get a summary string representation of this edition.
    * @return a formatted string containing key edition details, never {@code null}
    */
   default String getSummary() {
      return String.format("Edition[name='%s', type='%s', priority=%s, siteId=%s]",
          getName(),
          findEditionType().map(Object::toString).orElse("none"),
          getPriority(),
          findSiteId().map(Object::toString).orElse("none"));
   }

   /**
    * {@inheritDoc}
    */
   boolean equals(Object b);

   /**
    * {@inheritDoc}
    */
   int hashCode();

   /**
    * {@inheritDoc}
    */
   String toString();

   /**
    * Make a copy of this edition.
    * @return the copy, never {@code null}
    * @throws CloneNotSupportedException if cloning is not supported
    */
   Object clone() throws CloneNotSupportedException;
   
   /**
    * Copy all properties from a given Edition, except its internal ID and 
    * version number if there is any.
    *  
    * @param other the to be copied Edition, not {@code null}
    * @throws IllegalArgumentException if other is null
    */
   default void copy(IPSEdition other) {
      Objects.requireNonNull(other, "other cannot be null");
      copyImpl(other);
   }

   /**
    * Internal implementation for copying.
    */
   void copyImpl(IPSEdition other);
}
