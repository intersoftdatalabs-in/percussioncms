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
package com.percussion.services.catalog;

import java.util.Optional;

/**
 * Provides summary information about a catalog object with enhanced Java 11 support.
 *
 * <p>This interface represents a lightweight view of a catalog object, providing
 * essential identification and descriptive information without the full object data.
 * Objects that can be fully loaded implement {@link IPSCatalogItem}.
 *
 * <p>Key features:
 * <ul>
 *   <li>Lightweight object representation</li>
 *   <li>Optional-based safe navigation for nullable properties</li>
 *   <li>Enhanced validation and error handling</li>
 *   <li>Backward compatibility with existing implementations</li>
 * </ul>
 *
 * <p>Common use cases:
 * <ul>
 *   <li>Object browsing and selection in administrative interfaces</li>
 *   <li>Search result summaries</li>
 *   <li>Lightweight object listings for performance</li>
 *   <li>Deployment and migration object identification</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSCatalogSummary extends IPSCatalogIdentifier {

   /**
    * Get the object name.
    * 
    * <p>The name serves as the primary identifier for the object and should be
    * unique within its type scope. Names are typically used for programmatic
    * access and should follow consistent naming conventions.
    *
    * @return the name, never {@code null} or empty
    */
   String getName();

   /**
    * Get the object name with Optional wrapper for enhanced safety.
    *
    * @return Optional containing the name, never empty since name is required
    */
   default Optional<String> getNameOptional() {
      return Optional.ofNullable(getName())
         .filter(name -> !name.trim().isEmpty());
   }

   /**
    * Get the display label of the object.
    *
    * <p>The label provides a human-readable representation of the object, suitable
    * for display in user interfaces. If no specific display label is defined,
    * this method defaults to returning the object name.
    *
    * @return the object's display label, never {@code null} or empty
    */
   String getLabel();

   /**
    * Get the display label with Optional wrapper for enhanced safety.
    *
    * @return Optional containing the label, never empty since label defaults to name
    */
   default Optional<String> getLabelOptional() {
      return Optional.ofNullable(getLabel())
         .filter(label -> !label.trim().isEmpty());
   }

   /**
    * Get a description for the object.
    * 
    * <p>The description provides detailed information about the object's purpose,
    * functionality, or content. This is typically used for tooltips, help text,
    * or detailed object information displays.
    *
    * @return the description, may be {@code null} or empty
    */
   String getDescription();

   /**
    * Get the description with Optional wrapper for safer access.
    *
    * @return Optional containing the description if present and non-empty, empty otherwise
    */
   default Optional<String> getDescriptionOptional() {
      return Optional.ofNullable(getDescription())
         .filter(desc -> !desc.trim().isEmpty());
   }

   /**
    * Check if the object has a meaningful description.
    *
    * @return true if the object has a non-null, non-empty description
    */
   default boolean hasDescription() {
      return getDescriptionOptional().isPresent();
   }

   /**
    * Get a display-friendly string representation of this summary.
    *
    * <p>This method provides a consistent format for displaying summary information,
    * typically in the format "Label (Type)" or "Label" if type is not available.
    *
    * @return formatted display string, never {@code null}
    */
   default String getDisplayString() {
      var label = getLabel();
      var type = getTypeEnum();
      var typeInfo = type != null ? " (" + type + ")" : "";
      return label + typeInfo;
   }

   /**
    * Check if this summary represents the same object as another summary.
    *
    * <p>Two summaries are considered equivalent if they have the same GUID,
    * or if GUIDs are not available, the same name and type.
    *
    * @param other the other summary to compare, may be {@code null}
    * @return true if the summaries represent the same object
    */
   default boolean isSameObject(IPSCatalogSummary other) {
      if (other == null) {
         return false;
      }

      // Compare by GUID if available
      if (getGUID() != null && other.getGUID() != null) {
         return getGUID().equals(other.getGUID());
      }

      // Fallback to name and type comparison
      var t1 = getTypeEnum();
      var t2 = other.getTypeEnum();
      return getName().equals(other.getName()) && java.util.Objects.equals(t1, t2);
   }
}
