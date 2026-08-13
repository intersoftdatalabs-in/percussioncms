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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * FactoryBean type + create/lookup branches without Spring (#3291 leftover rawtypes).
 */
@Tag("UnitTest")
class PSEhCacheFactoryBeanTest {

  @Test
  @DisplayName("getObjectType matches FactoryBean Class<?> and returns Cache.class")
  void objectTypeIsCacheClass() {
    PSEhCacheFactoryBean bean = new PSEhCacheFactoryBean();
    assertEquals(Cache.class, bean.getObjectType());
    assertTrue(bean.isSingleton());
  }

  @Test
  @DisplayName("afterPropertiesSet requires cacheManager")
  void missingCacheManager() {
    PSEhCacheFactoryBean bean = new PSEhCacheFactoryBean();
    bean.setCacheName("missing-mgr");
    IllegalStateException ex = assertThrows(IllegalStateException.class, bean::afterPropertiesSet);
    assertEquals("cacheManager must be set", ex.getMessage());
  }

  @Test
  @DisplayName("creates a heap cache and supports put/get")
  void createsCache() {
    try (CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder().build(true)) {
      PSEhCacheFactoryBean bean = newFactory(manager, "created-region");
      bean.setMaxElementsInMemory(32);
      bean.afterPropertiesSet();

      Cache<Serializable, Serializable> cache = bean.getObject();
      assertNotNull(cache);
      assertSame(manager.getCache("created-region", Serializable.class, Serializable.class), cache);
      cache.put("k", "v");
      assertEquals("v", cache.get("k"));
    }
  }

  @Test
  @DisplayName("reuses an existing named cache region")
  void reusesExistingCache() {
    try (CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder().build(true)) {
      Cache<Serializable, Serializable> existing =
          manager.createCache(
              "existing-region",
              CacheConfigurationBuilder.newCacheConfigurationBuilder(
                      Serializable.class, Serializable.class, ResourcePoolsBuilder.heap(8))
                  .build());
      existing.put("seed", 1);

      PSEhCacheFactoryBean bean = newFactory(manager, "existing-region");
      bean.afterPropertiesSet();
      assertSame(existing, bean.getObject());
      assertEquals(1, bean.getObject().get("seed"));
    }
  }

  @Test
  @DisplayName("TTL, TTI, and eternal expiry branches create a usable cache")
  void expiryBranches() {
    try (CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder().build(true)) {
      PSEhCacheFactoryBean ttl = newFactory(manager, "ttl-region");
      ttl.setTimeToLive(30);
      ttl.afterPropertiesSet();
      assertNotNull(ttl.getObject());

      PSEhCacheFactoryBean tti = newFactory(manager, "tti-region");
      tti.setTimeToLive(0);
      tti.setTimeToIdle(15);
      tti.afterPropertiesSet();
      assertNotNull(tti.getObject());

      PSEhCacheFactoryBean eternal = newFactory(manager, "eternal-region");
      eternal.setEternal(true);
      eternal.setTimeToLive(30);
      eternal.afterPropertiesSet();
      assertNotNull(eternal.getObject());
    }
  }

  private static PSEhCacheFactoryBean newFactory(CacheManager manager, String name) {
    PSEhCacheFactoryBean bean = new PSEhCacheFactoryBean();
    bean.setCacheManager(manager);
    bean.setCacheName(name);
    bean.setMaxElementsInMemory(16);
    return bean;
  }
}
