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

import com.percussion.delivery.multitenant.IPSTenantContext;

/**
 * Tenant context that stores its context data within the thread local data.
 * @author erikserating
 *
 */
public class PSThreadLocalTenantContext implements IPSTenantContext {
    private static final ThreadLocal<String> userLocal = new ThreadLocal<>();

    @Override
    public String getTenantId() {
        return userLocal.get();
    }

    /**
     * @param tenantId may be {@code null}, but should not be empty.
     */
    public static void setTenantId(String tenantId) {
        userLocal.set(tenantId);
    }

    /**
     * Clear the tenant id value, setting it to {@code null}.
     */
    public static void clearTenantId() {
        userLocal.remove();
    }

    /**
     * @return {@code true} if the context has a tenant id set.
     */
    public static boolean hasTenantId() {
        return userLocal.get() != null;
    }
}
