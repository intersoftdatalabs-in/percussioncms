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
package com.percussion.services.memory;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.error.PSMissingBeanConfigurationException;

/**
 * Locator for cache service using thread-safe lazy initialization.
 *
 * <p>This class provides access to the cache service implementation through
 * Spring bean configuration. It uses double-checked locking for thread-safe
 * singleton initialization.</p>
 *
 * @author dougrand
 */
public final class PSCacheAccessLocator {

   private static volatile IPSCacheAccess cacheAccess;

   /**
    * Private constructor to prevent instantiation of this utility class.
    */
   private PSCacheAccessLocator() {
      // Utility class - no instances allowed
   }

   /**
    * Get the cache accessor using thread-safe lazy initialization.
    *
    * @return the cache accessor, never {@code null}
    * @throws PSMissingBeanConfigurationException if there's a problem with the
    *         Spring configuration or the bean cannot be found
    */
   public static IPSCacheAccess getCacheAccess() throws PSMissingBeanConfigurationException {
      var localRef = cacheAccess;
      if (localRef == null) {
         synchronized (PSCacheAccessLocator.class) {
            localRef = cacheAccess;
            if (localRef == null) {
               cacheAccess = localRef = (IPSCacheAccess) PSBaseServiceLocator.getBean("sys_cacheAccessor");
            }
         }
      }
      return localRef;
   }
}
