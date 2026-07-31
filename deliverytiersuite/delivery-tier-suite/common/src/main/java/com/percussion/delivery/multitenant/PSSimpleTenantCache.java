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

import com.percussion.delivery.multitenant.IPSTenantAuthorization.Status;
import jakarta.servlet.ServletRequest;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides a very simple in memory cache for tenant information , usage, and authorizations.
 *
 * @author natechadwick
 */
public class PSSimpleTenantCache implements IPSTenantCache {

  /** Default constructor. */
  public PSSimpleTenantCache() {}

  /** Thread-safe map that holds the cached tenant entries keyed by tenant id. */
  private ConcurrentHashMap<String, IPSTenantInfo> cache = new ConcurrentHashMap<>();

  /** Minutes cache entries have before needing re-authorization. */
  private long ttl;

  /** Log for this class. */
  private static final Logger log = LogManager.getLogger(PSSimpleTenantCache.class);

  private boolean authorizeExpiredTTL;
  private IPSTenantAuthorization auth;

  /**
   * Sets the maximum time to live that cached tenant entries may remain in the cache before being
   * re-authorized.
   *
   * @param minutes the cache TTL in minutes.
   */
  @Override
  public void setMaxTTL(long minutes) {
    this.ttl = minutes;
  }

  /**
   * Returns the maximum time to live that cached tenant entries may remain in the cache before
   * being re-authorized.
   *
   * @return the cache TTL in minutes.
   */
  @Override
  public long getMaxTTL() {
    return this.ttl;
  }

  /**
   * Returns the cached tenant information for the given id, incrementing its API usage count on
   * each call. If the cache entry's TTL has expired and {@link #getAuthorizeExpiredTTL()} returns
   * <code>true</code>, the tenant is re-authorized before being returned.
   *
   * @param id the tenant id, never <code>null</code>.
   * @param req the current servlet request, may be <code>null</code>.
   * @return the cached tenant info, or <code>null</code> if no tenant is cached for the given id.
   */
  @Override
  public IPSTenantInfo get(String id, ServletRequest req) {

    IPSTenantInfo t = cache.get(id);

    // Record overall calls
    if (t != null) {
      t.addAPIUsage(1);
      cache.put(id, t);
    }

    if (t != null && ttl < checkTTLAge(t.getLastAuthorizationCheckDate())) {
      log.debug("Cached Authorization expired for Tenant " + id);
      if (authorizeExpiredTTL) {
        reauthorize(t, req);
        return cache.get(id);
      }
    }

    return t;
  }

  /**
   * Puts the supplied tenant information into the cache, replacing any existing entry for the same
   * tenant id.
   *
   * @param tenant the tenant information to cache, never <code>null</code>.
   */
  @Override
  public void put(IPSTenantInfo tenant) {
    if (cache.replace(tenant.getTenantId(), tenant) == null)
      cache.put(tenant.getTenantId(), tenant);
  }

  /**
   * Removes the specified tenant from the cache.
   *
   * @param id the tenant id of the entry to remove, never <code>null</code>.
   */
  @Override
  public void remove(String id) {
    cache.remove(id);
  }

  /** Clears all tenants from the cache. */
  @Override
  public void clear() {
    cache.clear();
  }

  /**
   * Scans the cache and re-authorizes any tenants whose TTL has expired.
   *
   * @param req the current servlet request, may be <code>null</code>.
   */
  @Override
  public void scavenge(ServletRequest req) {

    log.debug("Initiating scavenge for expired entries...");

    Iterator<Entry<String, IPSTenantInfo>> it = cache.entrySet().iterator();
    IPSTenantInfo t;

    while (it.hasNext()) {
      Map.Entry<String, IPSTenantInfo> pairs = it.next();

      t = pairs.getValue();

      if (ttl < checkTTLAge(t.getLastAuthorizationCheckDate())) {
        log.debug("Authorization expired for tenant " + t.getTenantId() + " reauthorizing");
        reauthorize(t, req);
      }
    }
  }

  /**
   * Determines how long ago a TTL reference date occurred, in minutes.
   *
   * @param last the reference date to compare against now, never <code>null</code>.
   * @return the number of full minutes between {@code last} and the current time.
   */
  private long checkTTLAge(Date last) {
    return ((new Date().getTime() - last.getTime()) / 1000) / 60;
  }

  /**
   * Returns whether the cache re-authorizes tenants whose TTL has expired.
   *
   * @return <code>true</code> if expired entries are re-authorized, <code>false</code> if they are
   *     evicted without re-authorization.
   */
  @Override
  public boolean getAuthorizeExpiredTTL() {
    return this.authorizeExpiredTTL;
  }

  /**
   * Configures whether the cache re-authorizes tenants whose TTL has expired.
   *
   * @param ret <code>true</code> to re-authorize expired entries, <code>false</code> to evict them.
   */
  @Override
  public void setAuthorizeExpiredTTL(boolean ret) {
    this.authorizeExpiredTTL = ret;
  }

  /**
   * Returns the authorization provider used to re-authorize tenants.
   *
   * @return the active {@link IPSTenantAuthorization}, or <code>null</code> if none is set.
   */
  @Override
  public IPSTenantAuthorization getAuthorizationProvider() {
    return this.auth;
  }

  /**
   * Sets the authorization provider used to re-authorize tenants.
   *
   * @param auth the authorization provider to use, may be <code>null</code>.
   */
  @Override
  public void setAuthorizationProvider(IPSTenantAuthorization auth) {
    this.auth = auth;
  }

  /**
   * Re-authorizes the specified tenant with the authorization provider if one is configured.
   *
   * @param t the tenant to re-authorize, never <code>null</code>.
   * @param req the current servlet request, may be <code>null</code>.
   * @return <code>true</code> if the tenant has been authorized and refreshed, <code>false</code>
   *     if authorization did not succeed.
   */
  private boolean reauthorize(IPSTenantInfo t, ServletRequest req) {
    boolean ret = false;

    if (this.auth != null) {
      log.warn("Tenant Authorization service not initialized.");
    } else {
      log.debug("Reauthorizing tenant " + t.getTenantId());

      PSLicenseStatus s = auth.authorize(t.getTenantId(), t.getAPIUsage(), null);

      if (s.getStatusCode() == Status.SUCCESS) {
        t.setLastAuthorizationCheckDate(new Date());
        cache.put(t.getTenantId(), t);
        ret = true;
      } else {
        log.debug("Tenanant " + t.getTenantId() + "Not authorized");
        cache.remove(t.getTenantId());
        ret = false;
      }
    }

    return ret;
  }
}
