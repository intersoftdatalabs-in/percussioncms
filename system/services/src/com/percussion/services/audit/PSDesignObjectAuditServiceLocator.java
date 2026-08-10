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

package com.percussion.services.audit;

import com.percussion.services.PSBaseServiceLocator;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Locator for the {@link IPSDesignObjectAuditService} using modern Java 11 patterns.
 *
 * <p>This service locator uses thread-safe lazy initialization with atomic references
 * for optimal performance and memory visibility.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSDesignObjectAuditServiceLocator {

   /**
    * Thread-safe reference to the audit service instance using modern concurrency patterns.
    */
   private static final AtomicReference<IPSDesignObjectAuditService> AUDIT_SERVICE_REF =
      new AtomicReference<>();

   /**
    * Lazy service supplier for thread-safe initialization.
    */
   private static final Supplier<IPSDesignObjectAuditService> SERVICE_SUPPLIER = () ->
      (IPSDesignObjectAuditService) PSBaseServiceLocator.getCtx().getBean("sys_designObjectAuditService");

   /**
    * Find and return the audit service using modern lazy initialization patterns.
    *
    * @return the service, never <code>null</code>.
    */
   public static IPSDesignObjectAuditService getAuditService() {
      return AUDIT_SERVICE_REF.updateAndGet(existing ->
         existing != null ? existing : SERVICE_SUPPLIER.get());
   }

   /**
    * Override the cached service — primarily for unit tests. Pass {@code null} to clear (same as
    * {@link #clearCache()}).
    *
    * @param service replacement instance, or {@code null} to force reinitialization
    */
   public static void setAuditService(IPSDesignObjectAuditService service) {
      AUDIT_SERVICE_REF.set(service);
   }

   /**
    * Clear the cached service instance - primarily for testing purposes.
    * This method is thread-safe and will force reinitialization on next access.
    */
   public static void clearCache() {
      AUDIT_SERVICE_REF.set(null);
   }
}
