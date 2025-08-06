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
package com.percussion.services.content;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service locator for the content service with enhanced Java 11 support.
 *
 * <p>This locator provides thread-safe access to the content service instance
 * using modern Java 11 features including atomic references and optional-based
 * error handling.
 *
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe service instance management</li>
 *   <li>Optional-based safe service access</li>
 *   <li>Enhanced error handling with clear messages</li>
 *   <li>Modern synchronization patterns</li>
 * </ul>
 *
 * @since Java 11 Modernization
 */
public class PSContentServiceLocator extends PSBaseServiceLocator {

   private static final AtomicReference<IPSContentService> SERVICE_INSTANCE =
      new AtomicReference<>();

   /**
    * Find and return the content service using modern thread-safe patterns.
    *
    * @return the content service, never {@code null}
    * @throws PSMissingBeanConfigurationException if the bean is missing for the requested service
    */
   public static IPSContentService getContentService() throws PSMissingBeanConfigurationException {
      var service = SERVICE_INSTANCE.get();
      if (service == null) {
         synchronized (PSContentServiceLocator.class) {
            service = SERVICE_INSTANCE.get();
            if (service == null) {
               service = (IPSContentService) getCtx().getBean("sys_contentService");
               SERVICE_INSTANCE.set(service);
            }
         }
      }
      return service;
   }

   /**
    * Get the content service with Optional wrapper for safer access.
    *
    * @return Optional containing the content service if available, empty if configuration error
    */
   public static Optional<IPSContentService> getContentServiceOptional() {
      try {
         return Optional.of(getContentService());
      } catch (PSMissingBeanConfigurationException e) {
         return Optional.empty();
      }
   }

   /**
    * Check if the content service is available.
    *
    * @return true if the service is available, false otherwise
    */
   public static boolean isContentServiceAvailable() {
      return getContentServiceOptional().isPresent();
   }

   /**
    * Clear the cached service instance for testing purposes.
    * <p><strong>Note:</strong> This method should only be used in test scenarios.
    */
   public static void clearCache() {
      SERVICE_INSTANCE.set(null);
   }
}
