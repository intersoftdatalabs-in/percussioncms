/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.services.memory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.Serializable;
import java.time.Duration;

/**
 * Spring FactoryBean that creates an Ehcache 3.x {@link Cache} instance.
 * This replaces the removed {@code org.springframework.cache.ehcache.EhCacheFactoryBean}
 * from Spring 6+.
 *
 * <p>Configure this bean in Spring XML with properties for cache name, heap size,
 * and TTL/TTI settings. It will create or look up the named cache within the
 * provided {@link CacheManager}.
 *
 * @since Ehcache 3.x Migration
 */
public class PSEhCacheFactoryBean implements FactoryBean<Cache<Serializable, Serializable>>,
        InitializingBean {

    private static final Logger log = LogManager.getLogger(PSEhCacheFactoryBean.class);

    private CacheManager cacheManager;
    private String cacheName = "default";
    private int maxElementsInMemory = 10_000;
    private int timeToLive;
    private int timeToIdle;
    private boolean eternal;

    private Cache<Serializable, Serializable> cache;

    @Override
    public void afterPropertiesSet() {
        if (cacheManager == null) {
            throw new IllegalStateException("cacheManager must be set");
        }

        cache = cacheManager.getCache(cacheName, Serializable.class, Serializable.class);
        if (cache == null) {
            var configBuilder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Serializable.class, Serializable.class,
                ResourcePoolsBuilder.heap(maxElementsInMemory));

            if (!eternal && timeToLive > 0) {
                configBuilder = configBuilder.withExpiry(
                    ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(timeToLive)));
            } else if (!eternal && timeToIdle > 0) {
                configBuilder = configBuilder.withExpiry(
                    ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(timeToIdle)));
            }

            cache = cacheManager.createCache(cacheName, configBuilder.build());
            log.info("Created Ehcache 3.x cache region: '{}' (heap={}, ttl={}s, tti={}s)",
                cacheName, maxElementsInMemory, timeToLive, timeToIdle);
        } else {
            log.info("Using existing Ehcache 3.x cache region: '{}'", cacheName);
        }
    }

    @Override
    public Cache<Serializable, Serializable> getObject() {
        return cache;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<? extends Cache> getObjectType() {
        return Cache.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public void setMaxElementsInMemory(int maxElementsInMemory) {
        this.maxElementsInMemory = maxElementsInMemory;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    public void setTimeToIdle(int timeToIdle) {
        this.timeToIdle = timeToIdle;
    }

    public void setEternal(boolean eternal) {
        this.eternal = eternal;
    }

    /** No-op in Ehcache 3.x; disk persistence is configured at creation time. */
    public void setDiskPersistent(boolean diskPersistent) {
        // ignored
    }

    /** No-op in Ehcache 3.x; overflow is configured at creation time. */
    public void setOverflowToDisk(boolean overflowToDisk) {
        // ignored
    }
}
