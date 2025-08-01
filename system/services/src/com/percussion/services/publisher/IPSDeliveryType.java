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
package com.percussion.services.publisher;

import com.percussion.services.catalog.IPSCatalogItem;

import java.util.Objects;
import java.util.Optional;

/**
 * A delivery type represents a single publisher plugin with comprehensive Java 11 modernization.
 * The delivery type determines what Spring bean is looked up in the publisher and dictates
 * whether an item needs to be assembled for unpublishing.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for nullable properties</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Improved boolean property handling</li>
 * <li>Modern documentation with clear contracts</li>
 * </ul>
 *
 * @author Percussion Software
 */
public interface IPSDeliveryType extends IPSCatalogItem {

   /**
    * Get the name of the bean to be used when publishing. This name is used to
    * look up a Spring bean on the publisher side of the delivery.
    *
    * @return the beanName, never {@code null} or empty
    */
   String getBeanName();

   /**
    * Set the bean name with enhanced validation.
    *
    * @param beanName the beanName to set, not {@code null} or empty
    * @throws IllegalArgumentException if beanName is null or empty
    */
   default void setBeanName(String beanName) {
      Objects.requireNonNull(beanName, "beanName cannot be null");
      if (beanName.trim().isEmpty()) {
         throw new IllegalArgumentException("beanName cannot be empty");
      }
      setBeanNameImpl(beanName.trim());
   }

   /**
    * Internal implementation for bean name setting.
    */
   void setBeanNameImpl(String beanName);

   /**
    * Get the description that describes this delivery type safely.
    *
    * @return Optional containing the description if present and non-empty, empty otherwise
    */
   default Optional<String> findDescription() {
      return Optional.ofNullable(getDescription())
          .filter(desc -> !desc.trim().isEmpty());
   }

   /**
    * Get the description that describes this delivery type.
    *
    * @return the description, may be {@code null} or empty
    */
   String getDescription();

   /**
    * Set the description with validation.
    *
    * @param description the description to set, may be {@code null}
    */
   void setDescription(String description);

   /**
    * Get the name of the delivery type.
    *
    * @return the name, never {@code null} or empty
    */
   String getName();

   /**
    * Set the name of the delivery type with enhanced validation.
    *
    * @param name the name to set, not {@code null} or empty
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
    * Determines if the item needs to be assembled for unpublishing.
    *
    * @return {@code true} if the item must be assembled for the
    * unpublishing case; otherwise return {@code false}
    */
   boolean isUnpublishingRequiresAssembly();

   /**
    * Set whether unpublishing requires assembly.
    *
    * @param unpublishingRequiresAssembly {@code true} if unpublishing requires assembly,
    *                                     {@code false} otherwise
    */
   void setUnpublishingRequiresAssembly(boolean unpublishingRequiresAssembly);

   /**
    * Check if this delivery type has a meaningful description.
    *
    * @return {@code true} if description is present and non-empty, {@code false} otherwise
    */
   default boolean hasDescription() {
      return findDescription().isPresent();
   }

   /**
    * Check if this delivery type is configured for assembly-based unpublishing.
    *
    * @return {@code true} if assembly is required for unpublishing
    */
   default boolean requiresAssemblyForUnpublishing() {
      return isUnpublishingRequiresAssembly();
   }

   /**
    * Get a summary string representation of this delivery type.
    *
    * @return a formatted string containing name and bean name, never {@code null}
    */
   default String getSummary() {
      return String.format("DeliveryType[name='%s', bean='%s', unpublishAssembly=%s]",
          getName(), getBeanName(), isUnpublishingRequiresAssembly());
   }
}
