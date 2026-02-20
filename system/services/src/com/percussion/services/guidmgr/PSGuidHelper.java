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
package com.percussion.services.guidmgr;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A helper class for GUID construction and management with enhanced Java 11 support.
 *
 * <p>This utility class provides convenient methods for generating GUIDs, UUIDs, and
 * performing common GUID operations. It serves as a simplified facade over the
 * {@link IPSGuidManager} service for common use cases.
 *
 * <p>Key features:
 * <ul>
 *   <li>Simple GUID and UUID generation methods</li>
 *   <li>Batch generation with Stream API support</li>
 *   <li>Optional-based safe navigation</li>
 *   <li>Enhanced validation and error handling</li>
 * </ul>
 *
 * <p>Thread Safety: This class is thread-safe as it delegates to the underlying
 * thread-safe {@link IPSGuidManager} service.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public final class PSGuidHelper {

   /**
    * Private constructor to prevent instantiation of utility class.
    */
   private PSGuidHelper() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
   }

   /**
    * Generate just the UUID part of a GUID with enhanced validation.
    *
    * <p>This method is useful for initializing child table IDs where only the
    * numeric portion of the GUID is needed.
    *
    * @param type the enumeration to use, not {@code null}
    * @return the UUID portion as a long value
    * @throws IllegalArgumentException if type is null
    */
   public static long generateNextLong(PSTypeEnum type) {
      Objects.requireNonNull(type, "type cannot be null");
      var guidManager = PSGuidManagerLocator.getGuidMgr();
      return guidManager.createGuid(type).getUUID();
   }

   /**
    * Generate a complete new GUID with enhanced validation.
    *
    * @param type the enumeration to use, not {@code null}
    * @return the new GUID, never {@code null}
    * @throws IllegalArgumentException if type is null
    */
   public static IPSGuid generateNext(PSTypeEnum type) {
      Objects.requireNonNull(type, "type cannot be null");
      var guidManager = PSGuidManagerLocator.getGuidMgr();
      return guidManager.createGuid(type);
   }

   /**
    * Generate multiple GUIDs efficiently using Stream API.
    *
    * @param type the enumeration to use, not {@code null}
    * @param count the number of GUIDs to generate, must be positive
    * @return list of generated GUIDs, never {@code null}
    * @throws IllegalArgumentException if type is null or count is not positive
    */
   public static List<IPSGuid> generateNext(PSTypeEnum type, int count) {
      Objects.requireNonNull(type, "type cannot be null");
      if (count <= 0) {
         throw new IllegalArgumentException("count must be positive");
      }

      var guidManager = PSGuidManagerLocator.getGuidMgr();
      return guidManager.createGuids(type, count);
   }

   /**
    * Generate multiple UUID values efficiently.
    *
    * @param type the enumeration to use, not {@code null}
    * @param count the number of UUIDs to generate, must be positive
    * @return list of generated UUID values, never {@code null}
    * @throws IllegalArgumentException if type is null or count is not positive
    */
   public static List<Long> generateNextLongs(PSTypeEnum type, int count) {
      Objects.requireNonNull(type, "type cannot be null");
      if (count <= 0) {
         throw new IllegalArgumentException("count must be positive");
      }

      return generateNext(type, count)
         .stream()
         .mapToLong(IPSGuid::getUUID)
         .boxed()
         .toList();
   }

   /**
    * Generate a GUID with Optional wrapper for safer error handling.
    *
    * @param type the enumeration to use, may be {@code null}
    * @return Optional containing the GUID if successful, empty if type is null
    */
   public static Optional<IPSGuid> generateNextSafe(PSTypeEnum type) {
      if (type == null) {
         return Optional.empty();
      }

      try {
         return Optional.of(generateNext(type));
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Generate a UUID with Optional wrapper for safer error handling.
    *
    * @param type the enumeration to use, may be {@code null}
    * @return Optional containing the UUID if successful, empty if type is null
    */
   public static Optional<Long> generateNextLongSafe(PSTypeEnum type) {
      if (type == null) {
         return Optional.empty();
      }

      try {
         return Optional.of(generateNextLong(type));
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Check if the GUID manager service is available.
    *
    * @return true if the GUID manager is available and functional
    */
   public static boolean isGuidManagerAvailable() {
      try {
         var guidManager = PSGuidManagerLocator.getGuidMgr();
         return guidManager != null;
      } catch (Exception e) {
         return false;
      }
   }

   /**
    * Get a stream of GUIDs for functional processing.
    *
    * @param type the enumeration to use, not {@code null}
    * @param count the number of GUIDs to generate, must be positive
    * @return Stream of GUIDs for functional processing
    * @throws IllegalArgumentException if type is null or count is not positive
    */
   public static IntStream generateNextAsStream(PSTypeEnum type, int count) {
      Objects.requireNonNull(type, "type cannot be null");
      if (count <= 0) {
         throw new IllegalArgumentException("count must be positive");
      }

      return IntStream.range(0, count)
         .map(i -> (int) generateNextLong(type));
   }

   /**
    * Generate GUIDs for multiple types in a single operation.
    *
    * @param types the types to generate GUIDs for, not {@code null} or empty
    * @return list of GUIDs corresponding to each type, never {@code null}
    * @throws IllegalArgumentException if types is null or empty
    */
   public static List<IPSGuid> generateForTypes(PSTypeEnum... types) {
      Objects.requireNonNull(types, "types cannot be null");
      if (types.length == 0) {
         throw new IllegalArgumentException("types cannot be empty");
      }

      return IntStream.range(0, types.length)
         .mapToObj(i -> generateNext(types[i]))
         .toList();
   }
}
