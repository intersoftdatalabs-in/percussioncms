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

import com.percussion.utils.guid.IPSGuid;

import java.io.Serializable;
import java.util.Optional;

/**
 * Provides globally unique identification for catalog objects with enhanced Java 11 support.
 *
 * <p>This foundational interface ensures that all catalog objects have consistent GUID-based
 * identification, enabling reliable object tracking, comparison, and persistence operations
 * across the system.
 *
 * <p>Key features:
 * <ul>
 *   <li>Consistent GUID-based object identification</li>
 *   <li>Optional-based safe navigation for nullable GUIDs</li>
 *   <li>Type information access for catalog operations</li>
 *   <li>Enhanced comparison and validation utilities</li>
 * </ul>
 *
 * <p>GUID Requirements:
 * <ul>
 *   <li>Must be globally unique across the entire system</li>
 *   <li>Should remain stable throughout object lifecycle</li>
 *   <li>Must support persistence and serialization</li>
 *   <li>Should enable efficient comparison and lookup operations</li>
 * </ul>
 *
 * @since Java 11 Modernization
 */
public interface IPSCatalogIdentifier extends Serializable {

   /**
    * Get the globally unique identifier for this catalog object.
    *
    * <p>The GUID serves as the primary key for object identification and should be
    * consistent across all system operations including persistence, serialization,
    * and inter-service communication.
    *
    * @return the globally unique identifier, may be {@code null} for uninitialized objects
    * @see IPSGuid for detailed information about GUID structure and behavior
    */
   IPSGuid getGUID();

   /**
    * Get the GUID with Optional wrapper for safer access.
    *
    * <p>This method provides a safer alternative to {@link #getGUID()} by wrapping
    * the result in an Optional, enabling functional-style null handling and
    * reducing the risk of NullPointerException.
    *
    * @return Optional containing the GUID if present, empty otherwise
    */
   default Optional<IPSGuid> getGUIDOptional() {
      return Optional.ofNullable(getGUID());
   }

   /**
    * Get the (legacy) textual type identifier for this catalog object.
    *
    * <p>Historically many objects exposed a textual {@code getType()} method. To preserve
    * backwards compatibility with those callers, this default returns the enum name if the
    * underlying GUID provides a known type, or {@code null} otherwise.
    *
    * @return a textual representation of the type (enum name), or {@code null} when unknown
    */
   default String getType() {
      PSTypeEnum te = getTypeEnum();
      return te == null ? null : te.name();
   }

   /**
    * Get the type enumeration for this catalog object.
    *
    * <p>Use this for new code that prefers the {@link PSTypeEnum} representation. It
    * converts the underlying GUID numeric type into the enum where possible.
    *
    * @return the object type enum, or {@code null} if the underlying type is unknown
    */
   default PSTypeEnum getTypeEnum() {
      return getGUIDOptional().map(g -> PSTypeEnum.valueOf(g.getType())).orElse(null);
   }

   /**
    * Get the numeric type ordinal for legacy callers.
    *
    * @return the numeric ordinal, or {@code -1} if not available
    */
   default int getTypeOrdinal() {
      return getGUIDOptional().map(IPSGuid::getType).map(Short::intValue).orElse(-1);
   }

   /**
    * Get the type with Optional wrapper for safer access.
    *
    * @return Optional containing the type if GUID is available, empty otherwise
    */
   default Optional<PSTypeEnum> getTypeOptional() {
      return Optional.ofNullable(getTypeEnum());
   }

   /**
    * Check if this object has a valid GUID assigned.
    *
    * @return true if the object has a non-null GUID
    */
   default boolean hasGUID() {
      return getGUID() != null;
   }

   /**
    * Check if this object has the specified type.
    *
    * @param expectedType the type to check against, may be {@code null}
    * @return true if the object's type matches the expected type
    */
   default boolean hasType(PSTypeEnum expectedType) {
      return getTypeOptional()
         .map(type -> type == expectedType)
         .orElse(expectedType == null);
   }

   /**
    * Get the UUID portion of the GUID as a long value.
    *
    * @return the UUID as a long, or 0 if GUID is not available
    */
   default long getUUID() {
      return getGUIDOptional()
         .map(IPSGuid::getUUID)
         .map(Integer::longValue)
         .orElse(0L);
   }

   /**
    * Check if this identifier represents the same object as another identifier.
    *
    * <p>Two identifiers are considered equivalent if they have the same GUID.
    * If either identifier lacks a GUID, they are considered non-equivalent.
    *
    * @param other the other identifier to compare, may be {@code null}
    * @return true if both identifiers have the same non-null GUID
    */
   default boolean isSameObject(IPSCatalogIdentifier other) {
      if (other == null) {
         return false;
      }

      return getGUIDOptional()
         .flatMap(guid -> other.getGUIDOptional().map(otherGuid -> guid.equals(otherGuid)))
         .orElse(false);
   }

   /**
    * Get a string representation suitable for identification and logging.
    *
    * @return formatted string with type and GUID information
    */
   default String getIdentifierString() {
      var guid = getGUID();
      var type = getTypeEnum();

      if (guid != null) {
         return String.format("%s:%d",
                            type != null ? type : "UNKNOWN",
                            guid.getUUID());
      } else {
         return "UNIDENTIFIED:" + (type != null ? type : "UNKNOWN");
      }
   }
}
