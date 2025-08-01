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
package com.percussion.services.memory.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.server.cache.PSCacheStatisticsSnapshot;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.memory.PSCacheAccessLocator;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.util.PSBaseBean;
import com.percussion.utils.guid.IPSGuid;
import net.sf.ehcache.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Modern Java 11 EHCache-based implementation of {@link IPSCacheAccess}.
 *
 * <p>This implementation provides comprehensive cache access using EHCache as the underlying
 * cache provider. It includes thread-safe operations, enhanced validation, and integration
 * with the notification service for cache invalidation.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Thread-safe cache operations with comprehensive error handling</li>
 *   <li>Automatic cache invalidation via notification service integration</li>
 *   <li>Enhanced validation using modern Java 11 patterns</li>
 *   <li>Comprehensive statistics gathering for monitoring</li>
 *   <li>TTL/TTI support for cache entry lifecycle management</li>
 * </ul>
 *
 * <p>This class is configured as a Spring bean and automatically integrates with
 * the notification service for cache invalidation when objects are saved or deleted.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
@PSBaseBean("sys_cacheAccessor")
public final class PSEhCacheAccessor implements IPSCacheAccess {

    /**
     * Logger for cache operations and debugging.
     */
    private static final Logger log = LogManager.getLogger(IPSConstants.CACHING_LOG);

    /**
     * The EHCache manager instance managing all cache regions.
     */
    private final CacheManager manager;

    /**
     * Notification service for cache invalidation events.
     * Initialized by Spring dependency injection.
     */
    private IPSNotificationService notificationService;

    /**
     * Creates a new cache accessor with default EHCache manager configuration.
     */
    public PSEhCacheAccessor() {
        this.manager = CacheManager.create();
        log.info("EHCache manager initialized with {} cache regions", manager.getCacheNames().length);
    }

    /**
     * Creates a cache accessor with a specific cache manager.
     *
     * @param cacheManager the cache manager to use, must not be null
     * @throws IllegalArgumentException if cacheManager is null
     */
    public PSEhCacheAccessor(CacheManager cacheManager) {
        this.manager = Objects.requireNonNull(cacheManager, "Cache manager cannot be null");
        log.info("EHCache accessor initialized with provided manager");
    }

    @Override
    public void save(Serializable key, Serializable data, String region) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        Objects.requireNonNull(data, "Cache data cannot be null");
        validateRegion(region);
        validateCacheManager();

        var cache = getCache(region);
        cache.put(new Element(key, data));

        if (log.isDebugEnabled()) {
            log.debug("Saved object with key '{}' to cache region '{}'", key, region);
        }
    }

    @Override
    public Optional<Serializable> get(Serializable key, String region) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        validateRegion(region);
        validateCacheManager();

        var cache = getCache(region);
        try {
            var element = cache.get(key);
            var result = element != null ? element.getObjectValue() : null;

            if (log.isDebugEnabled()) {
                log.debug("Retrieved object with key '{}' from cache region '{}': {}",
                    key, region, result != null ? "found" : "not found");
            }

            return Optional.ofNullable((Serializable) result);
        } catch (CacheException e) {
            log.error("Error retrieving object with key '{}' from cache region '{}'", key, region, e);
            // Return empty Optional instead of throwing exception for better resilience
            return Optional.empty();
        }
    }

    @Override
    public void evict(Serializable key, String region) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        validateRegion(region);
        validateCacheManager();

        var cache = getCache(region);
        var wasRemoved = cache.remove(key);

        if (log.isDebugEnabled()) {
            log.debug("Evicted object with key '{}' from cache region '{}': {}",
                key, region, wasRemoved ? "success" : "not found");
        }
    }

    @Override
    public void clear() {
        validateCacheManager();

        var regionNames = manager.getCacheNames();
        for (var name : regionNames) {
            clear(name);
        }

        log.info("Cleared all {} EHCache regions", regionNames.length);
    }

    @Override
    public void clear(String region) {
        validateRegion(region);
        validateCacheManager();

        var cache = manager.getEhcache(region);
        if (cache != null) {
            cache.removeAll();
            log.debug("Cleared cache region '{}'", region);
        } else {
            log.warn("Cannot clear unknown cache region: '{}'", region);
        }
    }

    @Override
    public void clearRelationships() {
        log.debug("Clearing relationship cache regions");
        clear(CONTENT_FINDER_RELS);
        clear(RELATIONSHIP_DATA);
    }

    @Override
    public List<PSCacheStatisticsSnapshot> getStatistics() {
        validateCacheManager();

        var statList = new ArrayList<PSCacheStatisticsSnapshot>();
        var regionNames = manager.getCacheNames();

        for (var name : regionNames) {
            var cache = manager.getEhcache(name);
            if (cache != null) {
                var cacheStat = getCacheStatistics(cache);
                cacheStat.setName(name);
                statList.add(cacheStat);
            }
        }

        // Sort by region name for consistent output
        statList.sort(Comparator.comparing(PSCacheStatisticsSnapshot::getName, String.CASE_INSENSITIVE_ORDER));

        log.debug("Generated statistics for {} cache regions", statList.size());
        return List.copyOf(statList); // Return immutable list
    }

    @Override
    public boolean setTimeToIdle(Serializable key, String region, int timeToIdleSeconds) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        validateRegion(region);
        validateTimeToIdle(timeToIdleSeconds);
        validateCacheManager();

        var cache = manager.getCache(region);
        if (cache == null) {
            throw new IllegalArgumentException("Cache region not found: " + region);
        }

        try {
            var element = cache.get(key);
            if (element != null) {
                element.setEternal(false);
                element.setTimeToIdle(timeToIdleSeconds);

                log.debug("Set time-to-idle for key '{}' in region '{}' to {} seconds",
                    key, region, timeToIdleSeconds);
                return true;
            }
            return false;
        } catch (CacheException e) {
            log.error("Error setting time-to-idle for key '{}' in region '{}'", key, region, e);
            throw new IllegalStateException("Failed to set time-to-idle for key: " + key, e);
        }
    }

    @Override
    public boolean setTimeToLive(Serializable key, String region, int timeToLiveSeconds) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        validateRegion(region);
        validateTimeToLive(timeToLiveSeconds);
        validateCacheManager();

        var cache = manager.getCache(region);
        if (cache == null) {
            throw new IllegalArgumentException("Cache region not found: " + region);
        }

        try {
            var element = cache.get(key);
            if (element != null) {
                element.setEternal(false);
                element.setTimeToLive(timeToLiveSeconds);

                log.debug("Set time-to-live for key '{}' in region '{}' to {} seconds",
                    key, region, timeToLiveSeconds);
                return true;
            }
            return false;
        } catch (CacheException e) {
            log.error("Error setting time-to-live for key '{}' in region '{}'", key, region, e);
            throw new IllegalStateException("Failed to set time-to-live for key: " + key, e);
        }
    }

    @Override
    public CacheManager getManager() {
        return manager;
    }

    /**
     * Gets the notification service.
     *
     * @return the notification service, may be null if not configured
     */
    public Optional<IPSNotificationService> getNotificationService() {
        return Optional.ofNullable(notificationService);
    }

    /**
     * Sets the notification service and registers cache invalidation listener.
     *
     * @param notificationService the notification service, must not be null
     * @throws IllegalArgumentException if notificationService is null
     */
    @Autowired
    public void setNotificationService(IPSNotificationService notificationService) {
        this.notificationService = Objects.requireNonNull(notificationService,
            "Notification service cannot be null");

        // Register invalidation listener
        this.notificationService.addListener(EventType.OBJECT_INVALIDATION,
            new PSEhCacheNotificationListener());

        log.info("Notification service configured with cache invalidation listener");
    }

    /**
     * Validates that a cache region name is valid.
     */
    private void validateRegion(String region) {
        Objects.requireNonNull(region, "Cache region cannot be null");
        if (region.trim().isEmpty()) {
            throw new IllegalArgumentException("Cache region cannot be empty");
        }
    }

    /**
     * Validates that the cache manager is configured.
     */
    private void validateCacheManager() {
        if (manager == null) {
            throw new IllegalStateException("Cache manager is not configured");
        }
    }

    /**
     * Validates time-to-idle value.
     */
    private void validateTimeToIdle(int timeToIdleSeconds) {
        if (timeToIdleSeconds < 0) {
            throw new IllegalArgumentException("Time-to-idle cannot be negative: " + timeToIdleSeconds);
        }
    }

    /**
     * Validates time-to-live value.
     */
    private void validateTimeToLive(int timeToLiveSeconds) {
        if (timeToLiveSeconds < 0) {
            throw new IllegalArgumentException("Time-to-live cannot be negative: " + timeToLiveSeconds);
        }
    }

    /**
     * Gets a cache instance, throwing exception if not found.
     */
    private Ehcache getCache(String region) {
        var cache = manager.getEhcache(region);
        if (cache == null) {
            throw new IllegalArgumentException("Cache region not found: " + region);
        }
        return cache;
    }

    /**
     * Generates statistics for a specific cache region.
     */
    private PSCacheStatisticsSnapshot getCacheStatistics(Ehcache cache) {
        Objects.requireNonNull(cache, "Cache cannot be null");

        var stats = cache.getStatistics();

        var memItems = stats.getSize();
        var memUsage = stats.getLocalHeapSizeInBytes();
        var misses = stats.cacheMissCount();
        var totalHits = stats.cacheHitCount();
        var diskHits = stats.localDiskHitCount();
        var diskItems = stats.localDiskPutAddedCount();

        // Estimate disk usage based on memory usage ratio
        var diskUsage = 0L;
        if (diskItems > 0 && memItems > 0) {
            diskUsage = diskItems * (memUsage / memItems);
        }

        return new PSCacheStatisticsSnapshot(diskHits, diskItems, diskUsage,
                memItems, memUsage, misses, totalHits);
    }

    /**
     * Modern Java 11 notification listener for cache invalidation events.
     */
    public static class PSEhCacheNotificationListener implements IPSNotificationListener {

        @Override
        public void notifyEvent(PSNotificationEvent notification) {
            Objects.requireNonNull(notification, "Notification cannot be null");

            var target = notification.getTarget();
            if (target instanceof IPSGuid) {
                var guid = (IPSGuid) target;
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
