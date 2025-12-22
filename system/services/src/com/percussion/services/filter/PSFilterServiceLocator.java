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
package com.percussion.services.filter;

import com.percussion.services.PSBaseServiceLocator;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Locator for the filter service with comprehensive Java 11 modernization.
 * Provides thread-safe access to the filter service using modern concurrency
 * patterns and Optional-based safe access methods.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>AtomicReference for thread-safe service caching</li>
 * <li>Optional-based safe access methods</li>
 * <li>Enhanced error handling and validation</li>
 * <li>Modern concurrency patterns</li>
 * </ul>
 *
 * @author dougrand
 */
public class PSFilterServiceLocator extends PSBaseServiceLocator {

   private static final AtomicReference<IPSFilterService> serviceRef = new AtomicReference<>();

   /**
    * Get the filter service with thread-safe lazy initialization.
    *
    * @return the filter service, never {@code null}
    * @throws IllegalStateException if the service cannot be obtained
    */
   public static IPSFilterService getFilterService() {
      return serviceRef.updateAndGet(current -> {
         if (current == null) {
            var service = (IPSFilterService) getBean("sys_filtermanager");
            if (service == null) {
               throw new IllegalStateException("Filter service not available");
            }
            return service;
         }
         return current;
      });
   }

   /**
    * Get the filter service safely, returning an Optional for error handling.
    * This is the preferred method for filter service access as it provides null safety.
    *
    * @return an Optional containing the filter service if available, empty otherwise
    */
   public static Optional<IPSFilterService> getFilterServiceSafely() {
      try {
         return Optional.of(getFilterService());
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Check if the filter service is available and properly initialized.
    *
    * @return {@code true} if the service is available, {@code false} otherwise
    */
   public static boolean isFilterServiceAvailable() {
      return getFilterServiceSafely().isPresent();
   }

   /**
    * Reset the cached service instance. This method is primarily intended for
    * testing purposes or when the service needs to be reinitialized.
    *
    * <p><strong>Warning:</strong> This method should be used with caution in
    * production environments as it may cause temporary service unavailability.
    */
   public static void resetService() {
      serviceRef.set(null);
   }

   /**
    * Get filter service information for diagnostics.
    *
    * @return a string describing the current service state
    */
   public static String getServiceInfo() {
      var service = serviceRef.get();
      if (service == null) {
         return "FilterService[status=not_initialized]";
      }
      return String.format("FilterService[status=initialized, class=%s]",
          service.getClass().getSimpleName());
   }
}
