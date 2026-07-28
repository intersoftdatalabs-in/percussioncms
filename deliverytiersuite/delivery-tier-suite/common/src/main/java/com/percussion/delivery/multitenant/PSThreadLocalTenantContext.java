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

package com.percussion.delivery.multitenant;

/**
 * Tenant context that stores its context data within the thread local data.
 *
 * @author erikserating
 */
public class PSThreadLocalTenantContext implements IPSTenantContext {

  /** Default constructor. */
  public PSThreadLocalTenantContext() {}

  private static ThreadLocal<String> userLocal = new ThreadLocal<>();

  /**
   * Returns the current tenant id from the calling thread's local context.
   *
   * @return the current tenant id, or <code>null</code> if no tenant id is set.
   * @see IPSTenantContext#getTenantId()
   */
  public String getTenantId() {
    return userLocal.get();
  }

  /**
   * Sets the current thread's tenant id.
   *
   * @param tenantId the tenant id; may be <code>null</code>, but should not be empty.
   */
  public static void setTenantId(String tenantId) {
    userLocal.set(tenantId);
  }

  /** Clear the tenant id value, setting it to <code>null</code>. */
  public static void clearTenantId() {
    userLocal.set(null);
  }

  /**
   * Indicates whether the calling thread's tenant context currently has a tenant id set.
   *
   * @return <code>true</code> if the context has a tenant id set.
   */
  public static boolean hasTenantId() {
    return userLocal.get() != null;
  }
}
