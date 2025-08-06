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

// REFACTORED: CP-JAVA11
package com.percussion.services.memory;

import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.timing.PSStopwatch;
import net.sf.ehcache.CacheManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Modern JUnit5 tests for the cache service interface.
 *
 * <p>This test suite validates the cache service functionality including basic
 * CRUD operations, performance characteristics, and TTL/TTI behavior.</p>
 *
 * @author dougrand
 */
@Tag("IntegrationTest")
@DisplayName("Cache Access Service Tests")
class PSCacheAccessTest {

   /**
    * Testing cache configuration for EHCache.
    */
   private static final String EHCACHE_CONFIG = """
         <ehcache>
             <defaultCache
                 maxElementsInMemory="10000"
                 eternal="false"
                 timeToIdleSeconds="3000"
                 timeToLiveSeconds="3000" />
             <cache name="object"
                 maxElementsInMemory="1000"
                 eternal="false"
                 overflowToDisk="false"
                 timeToIdleSeconds="5"
                 timeToLiveSeconds="10" />
             <cache name="region2"
                 maxElementsInMemory="1000"
                 eternal="false"
                 overflowToDisk="false"
                 timeToIdleSeconds="500"
                 timeToLiveSeconds="10000" />
         </ehcache>""";

   /**
    * Test cache region name.
    */
   private static final String TEST_REGION = "object";

   /**
    * Test data class for cache operations.
    */
   public static final class TestClass implements Serializable {

      private static final long serialVersionUID = 1L;

      private final int a, b, c, d;
      private final long e, f, g, i;
      private final float j, k, l, m;
      private final String o, p, q;

      /**
       * Creates a new test instance with random data.
       */
      public TestClass() {
         var random = new SecureRandom();

         this.a = random.nextInt();
         this.b = random.nextInt();
         this.c = random.nextInt();
         this.d = random.nextInt();
         this.e = random.nextLong();
         this.f = random.nextLong();
         this.g = random.nextLong();
         this.i = random.nextLong();
         this.j = random.nextFloat();
         this.k = random.nextFloat();
         this.l = random.nextFloat();
         this.m = random.nextFloat();
         this.o = generateRandomString(random);
         this.p = generateRandomString(random);
         this.q = generateRandomString(random);
      }

      /**
       * Generates a random string for testing.
       */
      private String generateRandomString(SecureRandom random) {
         var length = random.nextInt(400) + 100;
         var builder = new StringBuilder(length);

         for (int x = 0; x < length; x++) {
            builder.append((char) (random.nextInt(26) + 'A'));
         }

         return builder.toString();
      }
      
      @Override
      public boolean equals(Object other) {
         if (this == other) return true;
         if (!(other instanceof TestClass that)) return false;

         return a == that.a && b == that.b && c == that.c && d == that.d &&
                e == that.e && f == that.f && g == that.g && i == that.i &&
                Float.compare(that.j, j) == 0 && Float.compare(that.k, k) == 0 &&
                Float.compare(that.l, l) == 0 && Float.compare(that.m, m) == 0 &&
                Objects.equals(o, that.o) && Objects.equals(p, that.p) &&
                Objects.equals(q, that.q);
      }
      
      @Override
      public int hashCode() {
         return Objects.hash(a, b, c, d, e, f, g, i, j, k, l, m, o, p, q);
      }
   }

   @BeforeAll
   static void setupCache() throws Exception {
      CacheManager.create(new ByteArrayInputStream(EHCACHE_CONFIG.getBytes()));
   }

   @Test
   @DisplayName("Basic cache operations: save, get, evict")
   void testBasicCacheOperations() throws Exception {
      var cache = PSCacheAccessLocator.getCacheAccess();

      // Test save and get
      cache.save("testKey", "testValue", TEST_REGION);
      var result = cache.get("testKey", TEST_REGION);

      assertTrue(result.isPresent(), "Cache should contain saved value");
      assertEquals("testValue", result.get(), "Retrieved value should match saved value");

      // Test eviction
      cache.evict("testKey", TEST_REGION);
      var evictedResult = cache.get("testKey", TEST_REGION);

      assertTrue(evictedResult.isEmpty(), "Cache should be empty after eviction");

      // Test another save/get cycle
      cache.save("anotherKey", "anotherValue", TEST_REGION);
      var anotherResult = cache.get("anotherKey", TEST_REGION);

      assertTrue(anotherResult.isPresent(), "Cache should contain second saved value");
      assertEquals("anotherValue", anotherResult.get(), "Second retrieved value should match");
   }
   
   @Test
   @DisplayName("Cache performance comparison with HashMap")
   void testCachePerformance() throws Exception {
      var cache = PSCacheAccessLocator.getCacheAccess();
      var stopwatch = new PSStopwatch();
      var mapStore = new HashMap<String, Object>();

      // Generate test data
      var instances = new TestClass[100];
      var keys = new String[100];

      for (int i = 0; i < instances.length; i++) {
         instances[i] = new TestClass();
         keys[i] = "key" + i;
      }

      // Test HashMap performance - write
      stopwatch.start();
      for (int i = 0; i < instances.length; i++) {
         mapStore.put(keys[i], instances[i]);
      }
      stopwatch.stop();
      System.out.println("HashMap PUT (" + instances.length + " items): " + stopwatch);

      // Test HashMap performance - read
      stopwatch.start();
      for (int i = 0; i < instances.length; i++) {
         mapStore.get(keys[i]);
      }
      stopwatch.stop();
      System.out.println("HashMap GET (" + instances.length + " items): " + stopwatch);

      // Test cache performance - write
      stopwatch.start();
      for (int i = 0; i < instances.length; i++) {
         cache.save(keys[i], instances[i], TEST_REGION);
      }
      stopwatch.stop();
      System.out.println("Cache PUT (" + instances.length + " items): " + stopwatch);

      // Test cache performance - read
      stopwatch.start();
      for (int i = 0; i < instances.length; i++) {
         var result = cache.get(keys[i], TEST_REGION);
         assertTrue(result.isPresent(), "Cache should contain saved object");

         var retrievedValue = (TestClass) result.get();
         assertEquals(instances[i], retrievedValue, "Retrieved object should equal original");
      }
      stopwatch.stop();
      System.out.println("Cache GET (" + instances.length + " items): " + stopwatch);
   }

   @Test
   @DisplayName("Cache statistics and region management")
   void testCacheStatistics() throws Exception {
      var cache = PSCacheAccessLocator.getCacheAccess();

      // Save some test data
      cache.save("stat1", "value1", TEST_REGION);
      cache.save("stat2", "value2", TEST_REGION);
      cache.save("stat3", "value3", TEST_REGION);

      // Get statistics
      var stats = cache.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should contain cache regions");

      // Test cache clearing
      cache.clear(TEST_REGION);

      var result = cache.get("stat1", TEST_REGION);
      assertTrue(result.isEmpty(), "Cache region should be empty after clear");
   }

   @Test
   @DisplayName("Cache validation and error handling")
   void testCacheValidation() {
      var cache = PSCacheAccessLocator.getCacheAccess();

      // Test null key validation
      assertThrows(NullPointerException.class, () ->
         cache.save(null, "value", TEST_REGION), "Should throw on null key");

      // Test null data validation
      assertThrows(NullPointerException.class, () ->
         cache.save("key", null, TEST_REGION), "Should throw on null data");

      // Test null region validation
      assertThrows(NullPointerException.class, () ->
         cache.save("key", "value", null), "Should throw on null region");

      // Test empty region validation
      assertThrows(IllegalArgumentException.class, () ->
         cache.save("key", "value", ""), "Should throw on empty region");
   }
}
