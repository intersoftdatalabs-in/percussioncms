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

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.percussion.delivery.multitenant.IPSTenantAuthorization.Status;

/**
 * Provides a simple in-memory cache for tenant information, usage, and authorizations.
 */
public class PSSimpleTenantCache implements IPSTenantCache {

    private final ConcurrentHashMap<String, IPSTenantInfo> cache = new ConcurrentHashMap<>();
    private long ttl;
    private static final Logger log = LogManager.getLogger(PSSimpleTenantCache.class);
    private boolean authorizeExpiredTTL;
    private IPSTenantAuthorization auth;

    @Override
    public void setMaxTTL(long minutes) {
        this.ttl = minutes;
    }

    @Override
    public long getMaxTTL() {
        return this.ttl;
    }

    @Override
    public IPSTenantInfo get(String id, ServletRequest req) {
        var t = cache.get(id);
        if (t != null) {
            t.addAPIUsage(1);
            cache.put(id, t);
            if (ttl < checkTTLAge(t.getLastAuthorizationCheckDate())) {
                log.debug("Cached Authorization expired for Tenant {}", id);
                if (authorizeExpiredTTL) {
                    reauthorize(t, req);
                    return cache.get(id);
                }
            }
        }
        return t;
    }

    @Override
    public void put(IPSTenantInfo tenant) {
        cache.put(tenant.getTenantId(), tenant);
    }

    @Override
    public void remove(String id) {
        cache.remove(id);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void scavenge(ServletRequest req) {
        log.debug("Initiating scavenge for expired entries...");
        for (var entry : cache.entrySet()) {
            var t = entry.getValue();
            if (ttl < checkTTLAge(t.getLastAuthorizationCheckDate())) {
                log.debug("Authorization expired for tenant {} reauthorizing", t.getTenantId());
                reauthorize(t, req);
            }
        }
    }

    /**
     * Helper method to determine if a TTL date has expired.
     */
    private long checkTTLAge(Date last) {
        return ((new Date().getTime() - last.getTime()) / 1000) / 60;
    }

    @Override
    public boolean getAuthorizeExpiredTTL() {
        return this.authorizeExpiredTTL;
    }

    @Override
    public void setAuthorizeExpiredTTL(boolean ret) {
        this.authorizeExpiredTTL = ret;
    }

    @Override
    public IPSTenantAuthorization getAuthorizationProvider() {
        return this.auth;
    }

    @Override
    public void setAuthorizationProvider(IPSTenantAuthorization auth) {
        this.auth = auth;
    }

    /**
     * Re-authorizes the specified tenant with the authorization provider if configured.
     *
     * @param t tenant info
     * @param req servlet request
     * @return true if the tenant has been authorized and refreshed, false otherwise
     */
    private boolean reauthorize(IPSTenantInfo t, ServletRequest req) {
        if (this.auth == null) {
            log.warn("Tenant Authorization service not initialized.");
            return false;
        }
        log.debug("Reauthorizing tenant {}", t.getTenantId());
        var s = auth.authorize(t.getTenantId(), t.getAPIUsage(), req);
        if (s.getStatusCode() == Status.SUCCESS) {
            t.setLastAuthorizationCheckDate(new Date());
            cache.put(t.getTenantId(), t);
            return true;
        } else {
            log.debug("Tenant {} not authorized", t.getTenantId());
            cache.remove(t.getTenantId());
            return false;
        }
    }
}
