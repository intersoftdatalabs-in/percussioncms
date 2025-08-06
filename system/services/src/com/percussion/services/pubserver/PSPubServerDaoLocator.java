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
package com.percussion.services.pubserver;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Service locator for the publication server DAO using modern Java 11 patterns.
 * Provides thread-safe singleton access to the publication server manager with
 * enhanced error handling and atomic reference patterns.
 *
 * @author leonardohildt
 */
public final class PSPubServerDaoLocator {

   /**
    * Bean name for the publication server DAO.
    */
   public static final String PUB_SERVER_DAO_BEAN = "sys_pubserverdao";

   /**
    * Thread-safe reference to the publication server DAO instance.
    */
   private static final AtomicReference<IPSPubServerDao> PUB_SERVER_DAO_REF =
       new AtomicReference<>();

   /**
    * Lazy supplier for thread-safe initialization.
    */
   private static final Supplier<IPSPubServerDao> DAO_SUPPLIER = () -> {
      var dao = (IPSPubServerDao) PSBaseServiceLocator.getBean(PUB_SERVER_DAO_BEAN);
      Objects.requireNonNull(dao, "Publication server DAO bean cannot be null");
      return dao;
   };

   /**
    * Private constructor to prevent instantiation.
    */
   private PSPubServerDaoLocator() {
      // Utility class - no instantiation
   }

   /**
    * Get the publication server DAO with thread-safe lazy initialization.
    * Uses double-checked locking pattern with atomic reference for optimal performance.
    *
    * @return the publication server DAO, never null
    * @throws PSMissingBeanConfigurationException if the bean doesn't exist
    * @throws IllegalStateException if the DAO cannot be initialized
    */
   public static IPSPubServerDao getPubServerManager()
         throws PSMissingBeanConfigurationException {

      var dao = PUB_SERVER_DAO_REF.get();
      if (dao == null) {
         dao = PUB_SERVER_DAO_REF.updateAndGet(current ->
            current != null ? current : DAO_SUPPLIER.get());
      }

      return dao;
   }

   /**
    * Clear the cached DAO instance for testing or reconfiguration purposes.
    * This method is primarily intended for unit testing scenarios.
    */
   public static void clearCache() {
      PUB_SERVER_DAO_REF.set(null);
   }

   /**
    * Check if the DAO is currently cached.
    *
    * @return true if the DAO instance is cached, false otherwise
    */
   public static boolean isCached() {
      return PUB_SERVER_DAO_REF.get() != null;
   }
}
