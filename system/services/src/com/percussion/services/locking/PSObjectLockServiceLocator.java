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
package com.percussion.services.locking;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Modern service locator for the object locking service with comprehensive Java 11 features.
 * Provides thread-safe access to the locking service using modern concurrency patterns
 * and Optional-based safe access methods.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>AtomicReference for thread-safe service caching</li>
 * <li>Optional-based safe access methods</li>
 * <li>Enhanced error handling and validation</li>
 * <li>Modern concurrency patterns</li>
 * </ul>
 */
public class PSObjectLockServiceLocator extends PSBaseServiceLocator {

   private static final AtomicReference<IPSObjectLockService> serviceRef = new AtomicReference<>();

   /**
    * Find and return the locking service with thread-safe lazy initialization.
    *
    * @return the locking service, never {@code null}
    * @throws PSMissingBeanConfigurationException if the bean is missing for the requested service
    */
   public static IPSObjectLockService getLockingService() throws PSMissingBeanConfigurationException {
      return serviceRef.updateAndGet(current -> {
         if (current == null) {
            var service = (IPSObjectLockService) getCtx().getBean("sys_lockingService");
            if (service == null) {
               throw new RuntimeException("Locking service not available from Spring context");
            }
            return service;
         }
         return current;
      });
   }

   /**
    * Get the locking service safely, returning an Optional for error handling.
    * This is the preferred method for locking service access as it provides null safety.
    *
    * @return an Optional containing the locking service if available, empty otherwise
    */
   public static Optional<IPSObjectLockService> getLockingServiceSafely() {
      try {
         return Optional.of(getLockingService());
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Check if the locking service is available and properly initialized.
    *
    * @return {@code true} if the service is available, {@code false} otherwise
    */
   public static boolean isLockingServiceAvailable() {
      return getLockingServiceSafely().isPresent();
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
    * Get locking service information for diagnostics.
    *
    * @return a string describing the current service state
    */
   public static String getServiceInfo() {
      var service = serviceRef.get();
      if (service == null) {
         return "LockingService[status=not_initialized]";
      }
      return String.format("LockingService[status=initialized, class=%s]",
          service.getClass().getSimpleName());
   }
}
