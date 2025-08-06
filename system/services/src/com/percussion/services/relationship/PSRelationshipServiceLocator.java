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
package com.percussion.services.relationship;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Optional;

/**
 * Thread-safe service locator for the relationship service using modern Java 11 patterns.
 *
 * <p>This utility class provides access to the relationship service implementation through
 * Spring bean configuration. It uses double-checked locking for thread-safe singleton
 * initialization and provides both traditional and Optional-based access methods.</p>
 *
 * @author dougrand
 */
public final class PSRelationshipServiceLocator {

   private static volatile IPSRelationshipService relationshipService;

   /**
    * Private constructor to prevent instantiation of this utility class.
    */
   private PSRelationshipServiceLocator() {
      // Utility class - no instances allowed
   }

   /**
    * Gets the relationship service using thread-safe lazy initialization.
    *
    * @return the relationship service, never {@code null}
    * @throws PSMissingBeanConfigurationException if there's a problem with the
    *         Spring configuration or the bean cannot be found
    */
   public static IPSRelationshipService getRelationshipService() throws PSMissingBeanConfigurationException {
      var localRef = relationshipService;
      if (localRef == null) {
         synchronized (PSRelationshipServiceLocator.class) {
            localRef = relationshipService;
            if (localRef == null) {
               relationshipService = localRef = (IPSRelationshipService) PSBaseServiceLocator.getBean("sys_relationshipService");
            }
         }
      }
      return localRef;
   }

   /**
    * Safely gets the relationship service wrapped in an Optional.
    * This method catches any configuration exceptions and returns an empty Optional.
    *
    * @return Optional containing the relationship service, empty if configuration fails
    */
   public static Optional<IPSRelationshipService> getRelationshipServiceSafely() {
      try {
         return Optional.of(getRelationshipService());
      } catch (PSMissingBeanConfigurationException e) {
         // Log the error if needed and return empty Optional
         return Optional.empty();
      }
   }

   /**
    * Checks if the relationship service is available and properly configured.
    *
    * @return {@code true} if the service is available, {@code false} otherwise
    */
   public static boolean isServiceAvailable() {
      return getRelationshipServiceSafely().isPresent();
   }
}
