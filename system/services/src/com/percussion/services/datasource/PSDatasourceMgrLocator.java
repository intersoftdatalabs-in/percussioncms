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
package com.percussion.services.datasource;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.utils.jdbc.IPSDatasourceManager;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Service locator for the datasource manager using modern Java 11 patterns.
 *
 * <p>This locator provides thread-safe access to the datasource manager
 * with atomic reference patterns and Optional-based safe access methods.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSDatasourceMgrLocator {

   /**
    * Thread-safe reference to the datasource manager instance.
    */
   private static final AtomicReference<IPSDatasourceManager> DATASOURCE_MGR_REF =
      new AtomicReference<>();

   /**
    * Lazy service supplier for thread-safe initialization.
    */
   private static final Supplier<IPSDatasourceManager> SERVICE_SUPPLIER = () -> {
      try {
         return (IPSDatasourceManager) PSBaseServiceLocator.getBean("sys_datasourceManager");
      } catch (PSMissingBeanConfigurationException e) {
         throw new RuntimeException("Failed to locate datasource manager", e);
      }
   };

   /**
    * Get the datasource manager using modern lazy initialization patterns.
    *
    * @return the datasource manager, never <code>null</code>.
    * @throws PSMissingBeanConfigurationException if the bean is not configured
    */
   public static IPSDatasourceManager getDatasourceMgr()
      throws PSMissingBeanConfigurationException {

      try {
         return DATASOURCE_MGR_REF.updateAndGet(existing ->
            existing != null ? existing : SERVICE_SUPPLIER.get());
      } catch (RuntimeException e) {
         if (e.getCause() instanceof PSMissingBeanConfigurationException) {
            throw (PSMissingBeanConfigurationException) e.getCause();
         }
         throw e;
      }
   }

   /**
    * Get the datasource manager safely with Optional wrapper.
    *
    * @return Optional containing the datasource manager, or empty if not available
    */
   public static Optional<IPSDatasourceManager> getDatasourceMgrSafely() {
      try {
         return Optional.of(getDatasourceMgr());
      } catch (PSMissingBeanConfigurationException e) {
         return Optional.empty();
      }
   }

   /**
    * Clear the cached datasource manager instance - primarily for testing purposes.
    * This method is thread-safe and will force reinitialization on next access.
    */
   public static void clearCache() {
      DATASOURCE_MGR_REF.set(null);
   }
}
