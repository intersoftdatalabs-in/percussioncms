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
package com.percussion.delivery.multitenant;

/**
 * Tenant context that stores its context data within thread-local storage.
 */
public class PSThreadLocalTenantContext implements IPSTenantContext {

    private static final ThreadLocal<String> userLocal = new ThreadLocal<>();

    @Override
    public String getTenantId() {
        return userLocal.get();
    }

    /**
     * Sets the tenant ID for the current thread.
     *
     * @param tenantId may be {@code null}, but should not be empty
     */
    public static void setTenantId(String tenantId) {
        userLocal.set(tenantId);
    }

    /**
     * Clears the tenant ID value, setting it to {@code null}.
     */
    public static void clearTenantId() {
        userLocal.set(null);
    }

    /**
     * Returns {@code true} if the context has a tenant ID set.
     */
    public static boolean hasTenantId() {
        return userLocal.get() != null;
    }
}
