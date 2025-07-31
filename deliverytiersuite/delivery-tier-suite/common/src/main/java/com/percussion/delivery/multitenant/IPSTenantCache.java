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

import javax.servlet.ServletRequest;

/**
 * Defines a simple cache for storing tenant data,
 * intended for services requiring tenant authorization.
 */
public interface IPSTenantCache {

    /**
     * Sets the maximum time-to-live (TTL) in minutes for cached tenant info before re-authorization.
     *
     * @param minutes TTL in minutes
     */
    void setMaxTTL(long minutes);

    /**
     * Returns the TTL in minutes before a cache entry must be re-authorized.
     *
     * @return TTL in minutes
     */
    long getMaxTTL();

    /**
     * Returns whether the service will authorize expired URLs.
     * When false, the cache returns null for missing tenants and removes tenants when TTL expires.
     *
     * @return true if expired TTLs are authorized, false otherwise
     */
    boolean getAuthorizeExpiredTTL();

    /**
     * Sets whether the service will attempt to authorize expiring URLs using the AuthorizationProvider.
     *
     * @param ret true to authorize expired TTLs
     */
    void setAuthorizeExpiredTTL(boolean ret);

    /**
     * Returns the Authorization Provider used for authorizing expired tenants.
     *
     * @return the authorization provider
     */
    IPSTenantAuthorization getAuthorizationProvider();

    /**
     * Sets the authorization provider to use.
     *
     * @param auth the authorization provider
     */
    void setAuthorizationProvider(IPSTenantAuthorization auth);

    /**
     * Returns the specified tenant from the cache.
     *
     * @param id tenant ID
     * @param req servlet request
     * @return tenant info, or null if not found
     */
    IPSTenantInfo get(String id, ServletRequest req);

    /**
     * Puts the specified tenant into the cache.
     *
     * @param tenant tenant information
     */
    void put(IPSTenantInfo tenant);

    /**
     * Removes the specified tenant from the cache.
     *
     * @param id tenant ID
     */
    void remove(String id);

    /**
     * Clears all tenants from the cache.
     */
    void clear();

    /**
     * Scans the cache and removes any expired tenants.
     *
     * @param req servlet request
     */
    void scavenge(ServletRequest req);
}
