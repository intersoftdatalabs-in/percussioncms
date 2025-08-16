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
package com.percussion.services.publisher;

import com.percussion.services.PSBaseServiceLocator;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe service locator for publisher services with enhanced error handling
 * and Optional-based safe access patterns. This locator provides modern Java 11
 * patterns for accessing publisher service instances.
 *
 * @author dougrand
 */
public final class PSPublisherServiceLocator extends PSBaseServiceLocator {

   private static final AtomicReference<IPSPublisherService> serviceRef = new AtomicReference<>();
   private static final String SERVICE_BEAN_NAME = "sys_publisherservice";

   // Private constructor to prevent instantiation
   private PSPublisherServiceLocator() {
      super();
   }

   /**
    * Get the publisher service with thread-safe lazy initialization.
    *
    * @return the publisher service, never {@code null} in a correct configuration
    * @throws IllegalStateException if the service cannot be located
    */
   public static IPSPublisherService getPublisherService() {
      return serviceRef.updateAndGet(current -> {
         if (current == null) {
            var service = (IPSPublisherService) getBean(SERVICE_BEAN_NAME);
            Objects.requireNonNull(service, "Publisher service bean not found: " + SERVICE_BEAN_NAME);
            return service;
         }
         return current;
      });
   }

   /**
    * Get the publisher service safely with Optional wrapper.
    *
    * @return Optional containing the publisher service, or empty if not available
    */
   public static Optional<IPSPublisherService> getPublisherServiceSafely() {
      try {
         return Optional.of(getPublisherService());
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Check if the publisher service is available.
    *
    * @return true if the service is available, false otherwise
    */
   public static boolean isServiceAvailable() {
      return getPublisherServiceSafely().isPresent();
   }

   /**
    * Reset the cached service instance for testing or reconfiguration.
    */
   public static void reset() {
      serviceRef.set(null);
   }

   /**
    * Get the service bean name for configuration reference.
    *
    * @return the Spring bean name for the publisher service
    */
   public static String getServiceBeanName() {
      return SERVICE_BEAN_NAME;
   }
}
