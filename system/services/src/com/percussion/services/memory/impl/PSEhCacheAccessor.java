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
package com.percussion.services.memory.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.server.cache.PSCacheStatisticsSnapshot;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.memory.PSCacheAccessLocator;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.system.utils.PSBaseBean;
import com.percussion.utils.guid.IPSGuid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ehcache 3.x-based implementation of {@link IPSCacheAccess}.
 *
 * <p>This implementation uses the Ehcache 3.x programmatic API to manage
 * named cache regions. Each region is a {@code Cache<Serializable, Serializable>}
 * created on demand with default heap-based resource pools.
 *
 * <p>Note: TTL/TTI configuration in Ehcache 3.x is managed at the cache configuration
 * level rather than per-entry. The {@code setTimeToIdle} and {@code setTimeToLive}
 * methods log a warning and return {@code false}.
 *
 * @author dougrand
 * @since Ehcache 3.x Migration
 */
@PSBaseBean("sys_cacheAccessor")
public final class PSEhCacheAccessor implements IPSCacheAccess {

    private static final Logger log = LogManager.getLogger(IPSConstants.CACHING_LOG);

    private static final int DEFAULT_HEAP_ENTRIES = 10_000;

    private final CacheManager manager;

    /** Tracks all known region names for iteration in clear() and getStatistics(). */
    private final Set<String> regionNames = ConcurrentHashMap.newKeySet();

    private IPSNotificationService notificationService;

    /**
     * Creates a new cache accessor with a default Ehcache 3.x CacheManager.
     */
    public PSEhCacheAccessor() {
        this.manager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
        log.info("Ehcache 3.x manager initialized");
    }

    /**
     * Creates a cache accessor with a specific cache manager.
     *
     * @param cacheManager the cache manager to use, must not be null
     */
    public PSEhCacheAccessor(CacheManager cacheManager) {
        this.manager = Objects.requireNonNull(cacheManager, "Cache manager cannot be null");
        log.info("Ehcache 3.x accessor initialized with provided manager");
    }

    @Override
    public void save(Serializable key, Serializable data, String region) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        Objects.requireNonNull(data, "Cache data cannot be null");
        validateRegion(region);

        var cache = getOrCreateCache(region);
        cache.put(key, data);

        if (log.isDebugEnabled()) {
            log.debug("Saved object with key '{}' to cache region '{}'", key, region);
        }
    }

    @Override
    public Optional<Serializable> get(Serializable key, String region) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        validateRegion(region);

        var cache = getOrCreateCache(region);
        try {
            var result = cache.get(key);

            if (log.isDebugEnabled()) {
                log.debug("Retrieved object with key '{}' from cache region '{}': {}",
                    key, region, result != null ? "found" : "not found");
            }

            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.error("Error retrieving object with key '{}' from cache region '{}'",
                key, region, e);
            return Optional.empty();
        }
    }

    @Override
    public void evict(Serializable key, String region) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        validateRegion(region);

        var cache = getOrCreateCache(region);
        cache.remove(key);

        if (log.isDebugEnabled()) {
            log.debug("Evicted object with key '{}' from cache region '{}'", key, region);
        }
    }

    @Override
    public void clear() {
        for (var name : regionNames) {
            clearRegion(name);
        }
        log.info("Cleared all Ehcache regions");
    }

    @Override
    public void clear(String region) {
        validateRegion(region);
        clearRegion(region);
    }

    @Override
    public void clearRelationships() {
        log.debug("Clearing relationship cache regions");
        clearRegion(CONTENT_FINDER_RELS);
        clearRegion(RELATIONSHIP_DATA);
    }

    @Override
    public List<PSCacheStatisticsSnapshot> getStatistics() {
        var statList = new ArrayList<PSCacheStatisticsSnapshot>();

        for (var name : regionNames) {
            var cache = manager.getCache(name, Serializable.class, Serializable.class);
            if (cache != null) {
                // Ehcache 3.x statistics require an external StatisticsService;
                // return zero-valued snapshots with the region name.
                var snapshot = new PSCacheStatisticsSnapshot(0, 0, 0, 0, 0, 0, 0);
                snapshot.setName(name);
                statList.add(snapshot);
            }
        }

        statList.sort(Comparator.comparing(
            PSCacheStatisticsSnapshot::getName, String.CASE_INSENSITIVE_ORDER));

        log.debug("Generated statistics for {} cache regions", statList.size());
        return List.copyOf(statList);
    }

    @Override
    public boolean setTimeToIdle(Serializable key, String region, int timeToIdleSeconds) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        validateRegion(region);
        if (timeToIdleSeconds < 0) {
            throw new IllegalArgumentException(
                "Time-to-idle cannot be negative: " + timeToIdleSeconds);
        }

        log.warn("setTimeToIdle is not supported per-entry in Ehcache 3.x. "
            + "Configure TTI in the cache definition. Key: '{}', Region: '{}'", key, region);
        return false;
    }

    @Override
    public boolean setTimeToLive(Serializable key, String region, int timeToLiveSeconds) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        validateRegion(region);
        if (timeToLiveSeconds < 0) {
            throw new IllegalArgumentException(
                "Time-to-live cannot be negative: " + timeToLiveSeconds);
        }

        log.warn("setTimeToLive is not supported per-entry in Ehcache 3.x. "
            + "Configure TTL in the cache definition. Key: '{}', Region: '{}'", key, region);
        return false;
    }

    @Override
    public CacheManager getManager() {
        return manager;
    }

    public Optional<IPSNotificationService> getNotificationService() {
        return Optional.ofNullable(notificationService);
    }

    @Autowired
    public void setNotificationService(IPSNotificationService notificationService) {
        this.notificationService = Objects.requireNonNull(notificationService,
            "Notification service cannot be null");

        this.notificationService.addListener(EventType.OBJECT_INVALIDATION,
            new PSEhCacheNotificationListener());

        log.info("Notification service configured with cache invalidation listener");
    }

    /**
     * Returns the cache for the given region, creating it on demand if necessary.
     */
    private Cache<Serializable, Serializable> getOrCreateCache(String region) {
        var cache = manager.getCache(region, Serializable.class, Serializable.class);
        if (cache == null) {
            cache = manager.createCache(region,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        Serializable.class, Serializable.class,
                        ResourcePoolsBuilder.heap(DEFAULT_HEAP_ENTRIES))
                    .build());
            regionNames.add(region);
            log.debug("Created new cache region: '{}'", region);
        } else if (!regionNames.contains(region)) {
            regionNames.add(region);
        }
        return cache;
    }

    private void clearRegion(String name) {
        var cache = manager.getCache(name, Serializable.class, Serializable.class);
        if (cache != null) {
            cache.clear();
            log.debug("Cleared cache region '{}'", name);
        } else {
            log.warn("Cannot clear unknown cache region: '{}'", name);
        }
    }

    private void validateRegion(String region) {
        Objects.requireNonNull(region, "Cache region cannot be null");
        if (region.trim().isEmpty()) {
            throw new IllegalArgumentException("Cache region cannot be empty");
        }
    }

    /**
     * Notification listener for cache invalidation events.
     */
    public static class PSEhCacheNotificationListener implements IPSNotificationListener {

        @Override
        public void notifyEvent(PSNotificationEvent notification) {
            Objects.requireNonNull(notification, "Notification cannot be null");

            var target = notification.getTarget();
            if (target instanceof IPSGuid guid) {
                try {
                    var cache = PSCacheAccessLocator.getCacheAccess();
                    cache.evict(guid, IPSCacheAccess.IN_MEMORY_STORE);
                    log.debug("Cache invalidation: evicted GUID '{}' from in-memory store", guid);
                } catch (Exception e) {
                    log.error("Error during cache invalidation for GUID '{}'", guid, e);
                }
            } else {
                log.warn("Received cache invalidation event with non-GUID target: {}", target);
            }
        }
    }
}
