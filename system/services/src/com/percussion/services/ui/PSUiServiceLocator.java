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
package com.percussion.services.ui;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Service locator for UI services using modern Java 11 patterns.
 * Provides thread-safe access to UI service instances with enhanced caching
 * and validation for reliable UI component management.
 *
 * @author Percussion Software
 */
public final class PSUiServiceLocator {

   /**
    * Bean name for the UI service.
    */
   public static final String UI_SERVICE_BEAN = "sys_uiService";

   /**
    * Thread-safe reference to the UI service instance.
    */
   private static final AtomicReference<IPSUiService> UI_SERVICE_REF =
       new AtomicReference<>();

   /**
    * Lazy supplier for thread-safe initialization.
    */
   private static final Supplier<IPSUiService> SERVICE_SUPPLIER = () -> {
      var service = (IPSUiService) PSBaseServiceLocator.getCtx().getBean(UI_SERVICE_BEAN);
      Objects.requireNonNull(service, "UI service bean cannot be null");
      return service;
   };

   /**
    * Private constructor to prevent instantiation.
    */
   private PSUiServiceLocator() {
      // Utility class - no instantiation
   }

   /**
    * Find and return the UI service with thread-safe lazy initialization.
    * Uses double-checked locking pattern with atomic reference for optimal performance.
    *
    * @return the UI service, never null
    * @throws PSMissingBeanConfigurationException if the requested service bean is missing
    * @throws IllegalStateException if the service cannot be initialized
    */
   public static IPSUiService getUiService() throws PSMissingBeanConfigurationException {
      var service = UI_SERVICE_REF.get();
      if (service == null) {
         service = UI_SERVICE_REF.updateAndGet(current ->
            current != null ? current : SERVICE_SUPPLIER.get());
      }
      return service;
   }

   /**
    * Get the UI service with safe access using Optional for null-safe operations.
    *
    * @return Optional containing the UI service if available, empty if not found
    */
   public static Optional<IPSUiService> findUiService() {
      try {
         return Optional.of(getUiService());
      } catch (Exception e) {
         // Log the exception if needed, but return empty Optional
         return Optional.empty();
      }
   }

   /**
    * Create a supplier for lazy UI service retrieval.
    *
    * @return Supplier that provides the UI service when called
    */
   public static Supplier<IPSUiService> createUiServiceSupplier() {
      return () -> {
         try {
            return getUiService();
         } catch (PSMissingBeanConfigurationException e) {
            throw new IllegalStateException("UI service not available", e);
         }
      };
   }

   /**
    * Clear the cached service instance for testing or reconfiguration purposes.
    * This method is primarily intended for unit testing scenarios.
    */
   public static void clearCache() {
      UI_SERVICE_REF.set(null);
   }

   /**
    * Check if the UI service is currently cached.
    *
    * @return true if the service instance is cached, false otherwise
    */
   public static boolean isCached() {
      return UI_SERVICE_REF.get() != null;
   }

   /**
    * Get the UI service bean name for configuration purposes.
    *
    * @return the bean name used for service lookup
    */
   public static String getServiceBeanName() {
      return UI_SERVICE_BEAN;
   }
}
