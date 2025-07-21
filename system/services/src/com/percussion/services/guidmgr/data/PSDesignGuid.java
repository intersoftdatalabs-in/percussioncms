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
package com.percussion.services.guidmgr.data;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Design GUID implementation that allows construction from GUID values with type validation.
 *
 * <p>This class is specifically designed for internal use with design web services, where
 * the full GUID value is provided as a long. It extends {@link PSGuid} with enhanced
 * validation and type safety features using Java 11 patterns.
 *
 * <p>All constructors perform strict validation to ensure the GUID contains a valid type
 * and that the type is recognized by the system.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public final class PSDesignGuid extends PSGuid {

   private static final long serialVersionUID = -7095060778250604874L;

   /**
    * Constructs a design GUID from a complete GUID value.
    *
    * <p>The provided value must include both the type information and the actual
    * GUID value. This constructor performs validation to ensure the type is valid
    * and recognized by the system.
    *
    * @param value the complete GUID value as a long, must specify a valid GUID type
    * @throws IllegalArgumentException if the value doesn't specify a valid type
    */
   public PSDesignGuid(long value) {
      m_guid = value;
      validateGuid();
   }
   
   /**
    * Constructs a design GUID from an existing GUID instance.
    *
    * <p>This constructor provides type-safe conversion from other GUID implementations
    * to PSDesignGuid, with proper validation of the source GUID.
    *
    * @param guid the source GUID to copy from, must not be null and must be a PSGuid instance
    * @throws IllegalArgumentException if guid is null or not a PSGuid instance
    */
   public PSDesignGuid(IPSGuid guid) {
      Objects.requireNonNull(guid, "guid cannot be null");

      if (!(guid instanceof PSGuid)) {
         throw new IllegalArgumentException(
             "guid must be an instance of PSGuid, but was: " + guid.getClass().getSimpleName());
      }

      m_guid = ((PSGuid) guid).m_guid;
      validateGuid();
   }
   
   /**
    * Constructs a design GUID from explicit type and value parameters.
    *
    * <p>This constructor delegates to the parent class for standard GUID creation
    * with type and value components.
    *
    * @param type the GUID type, must not be null
    * @param value the GUID value component
    * @throws IllegalArgumentException if type is null or invalid
    */
   public PSDesignGuid(PSTypeEnum type, long value) {
      super(Objects.requireNonNull(type, "type cannot be null"), value);
   }

   /**
    * Retrieves the complete GUID value including all components.
    *
    * <p>The returned value includes the UUID, type ID, and host ID components,
    * allowing for unique reference to Rhythmyx objects across the system.
    *
    * @return the complete GUID value as a long
    */
   public long getValue() {
      return m_guid;
   }
   
   /**
    * Creates a PSDesignGuid safely from a long value, returning an Optional.
    *
    * <p>This factory method provides a safe way to create design GUIDs without
    * throwing exceptions for invalid values.
    *
    * @param value the GUID value to validate and wrap
    * @return an Optional containing the PSDesignGuid, or empty if invalid
    */
   public static Optional<PSDesignGuid> ofNullable(long value) {
      try {
         return Optional.of(new PSDesignGuid(value));
      } catch (IllegalArgumentException e) {
         return Optional.empty();
      }
   }

   /**
    * Creates a PSDesignGuid safely from an existing GUID, returning an Optional.
    *
    * @param guid the source GUID to convert
    * @return an Optional containing the PSDesignGuid, or empty if conversion fails
    */
   public static Optional<PSDesignGuid> ofNullable(IPSGuid guid) {
      try {
         return guid != null ? Optional.of(new PSDesignGuid(guid)) : Optional.empty();
      } catch (IllegalArgumentException e) {
         return Optional.empty();
      }
   }

   /**
    * Validates the GUID to ensure it contains a valid, recognized type.
    *
    * <p>This method performs strict validation of the GUID structure and type
    * information, throwing descriptive exceptions for various failure cases.
    *
    * @throws IllegalArgumentException if the GUID type is missing or unrecognized
    */
   private void validateGuid() {
      var typeValue = getType();
      if (typeValue == 0) {
         throw new IllegalArgumentException("GUID type must be specified (type value was 0)");
      }

      var type = PSTypeEnum.valueOf(typeValue);
      if (type == null) {
         throw new IllegalArgumentException(
             String.format("GUID contains unrecognized type: %d", typeValue));
      }
   }
}
