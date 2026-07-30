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

import jakarta.servlet.ServletRequest;

/**
 * Defines a simple cache for storing tenant data intended to be used by services that require
 * authorization of tenant data.
 *
 * @author natechadwick
 */
public interface IPSTenantCache {

  /**
   * Sets the maximum time to live that a tenant's info can be cached before it must be
   * re-authorized.
   *
   * @param minutes the TTL expressed in minutes.
   */
  public void setMaxTTL(long minutes);

  /**
   * Returns the number of minutes before an entry in the cache must be re-authorized.
   *
   * @return the cache TTL in minutes.
   */
  public long getMaxTTL();

  /**
   * Returns weather or not the service will authorize expired urls.
   *
   * <p>When false, the cache will simply return null for missing tenants and remove tenants from
   * cache when their TTL expires.
   *
   * @return <code>true</code> if expired tenants are re-authorized, <code>false</code> otherwise.
   */
  public boolean getAuthorizeExpiredTTL();

  /**
   * When set to true, the service will attempt to authorize expiring urls using the provider set in
   * the AuthorizationProvider property.
   *
   * @param ret <code>true</code> to authorize expired tenants, <code>false</code> to evict them.
   */
  public void setAuthorizeExpiredTTL(boolean ret);

  /**
   * Returns the authorization provider used when authorizing expired tenants.
   *
   * @return the active {@link IPSTenantAuthorization}, or <code>null</code> if none is set.
   */
  public IPSTenantAuthorization getAuthorizationProvider();

  /**
   * Sets the authorization provider to use.
   *
   * @param auth the authorization provider, may be <code>null</code>.
   */
  public void setAuthorizationProvider(IPSTenantAuthorization auth);

  /**
   * Returns the specified tenant from the cache.
   *
   * @param id the tenant id to look up, never <code>null</code>.
   * @param req the current servlet request, may be <code>null</code>.
   * @return the cached tenant info, or <code>null</code> if no entry exists.
   */
  public IPSTenantInfo get(String id, ServletRequest req);

  /**
   * Puts the specified tenant into the cache.
   *
   * @param tenant the tenant information to cache; the existing entry will be replaced, never
   *     <code>null</code>.
   */
  public void put(IPSTenantInfo tenant);

  /**
   * Removes the specified tenant from the cache.
   *
   * @param id the tenant id of the entry to remove, never <code>null</code>.
   */
  public void remove(String id);

  /** Clears all tenants from the cache. */
  public void clear();

  /**
   * Scans the cache and re-authorizes any expired tenant entries.
   *
   * @param req the current servlet request, may be <code>null</code>.
   */
  public void scavenge(ServletRequest req);
}
