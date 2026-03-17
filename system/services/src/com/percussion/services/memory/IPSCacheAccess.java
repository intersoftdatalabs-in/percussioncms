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

import com.percussion.server.cache.PSCacheStatisticsSnapshot;
import org.ehcache.CacheManager;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * This service provides access to the runtime cache mechanism. The
 * implementation details are known only to the service implementation.
 *
 * <p>This interface provides methods for storing, retrieving, and managing
 * cached objects across different cache regions. It supports TTL and TTI
 * configurations for individual cache entries.</p>
 *
 * @author dougrand
 */
public interface IPSCacheAccess {
   /**
    * The identifier for the cache region used to store relationships
    * used for relationship content finder.
    */
   String CONTENT_FINDER_RELS = "slot";

   /**
    * The identifier for the cache region used by hibernate 2nd level cache
    * to cache individual relationship object.
    */
   String RELATIONSHIP_DATA = "PSRelationshipData";

   /**
    * The identifier for the cache region used to store general in memory
    * objects. These objects are used in a generally read-only fashion. The
    * only modifications performed are those done to transient data. Such
    * modifications must, of course, be properly guarded against multi-thread
    * access. See the {@code ehcache.xml} file for the definition of the
    * cache regions.
    */
   String IN_MEMORY_STORE = "memory";

   /**
    * Store the given object into the cache using the specified key and region.
    *
    * @param key the key to store the object. The semantics of a key are
    *           identical to the key used for {@code Map} access.
    * @param data the data to be stored, never {@code null}
    * @param region the region to use for storage, never {@code null} or
    *           empty
    * @throws IllegalArgumentException if key, data, or region is null/empty
    */
   void save(Serializable key, Serializable data, String region);

   /**
    * Retrieve the given object from the cache using the specified key and
    * region.
    *
    * @param key the key to store the object. The semantics of a key are
    *           identical to the key used for {@code Map} access.
    * @param region the region to use for storage, never {@code null} or
    *           empty
    * @return an Optional containing the stored data, or empty if the data
    *         is no longer present or has expired
    * @throws IllegalArgumentException if key or region is null/empty
    */
   Optional<Serializable> get(Serializable key, String region);

   /**
    * Remove the given object from the cache using the specified key and region.
    * Has no effect if the object has expired from the cache.
    *
    * @param key the key to store the object. The semantics of a key are
    *           identical to the key used for {@code Map} access.
    * @param region the region to use for storage, never {@code null} or
    *           empty
    * @throws IllegalArgumentException if key or region is null/empty
    */
   void evict(Serializable key, String region);

   /**
    * Clear all elements from the cache, regardless of region. This primarily
    * is for use by the cache cleanup from the console, although other uses are
    * valid.
    */
   void clear();

   /**
    * Clear the specified region in the cache.
    *
    * @param region the region, never {@code null} or empty
    * @throws IllegalArgumentException if region is null or empty
    */
   void clear(String region);

   /**
    * Clear regions that relate to relationship caches. This includes
    * both {@link #CONTENT_FINDER_RELS} and {@link #RELATIONSHIP_DATA} regions.
    */
   void clearRelationships();

   /**
    * Get the statistics for all cache regions.
    * Note: the disk usage is estimated.
    *
    * @return the statistics of all cache regions, sorted by region name.
    *         Never {@code null} but may be empty if the cache is not used at all.
    */
   List<PSCacheStatisticsSnapshot> getStatistics();

   /**
    * Set the maximum number of seconds an element can exist in the cache
    * without being accessed. The element expires at this limit and will no
    * longer be returned from the cache. The default value is 0, which means
    * no TTI eviction takes place (infinite lifetime).
    *
    * @param key the cache key, never {@code null}
    * @param region the cache region, never {@code null} or empty
    * @param timeToIdleSeconds the time to idle in seconds, must be >= 0
    * @return {@code true} if the TTI was successfully set, {@code false} otherwise
    * @throws IllegalArgumentException if key/region is null/empty or timeToIdleSeconds &lt; 0
    */
   boolean setTimeToIdle(Serializable key, String region, int timeToIdleSeconds);

   /**
    * Set the maximum number of seconds an element can exist in the cache
    * regardless of use. The element expires at this limit and will no longer
    * be returned from the cache. The default value is 0, which means no TTL
    * eviction takes place (infinite lifetime).
    *
    * @param key the cache key, never {@code null}
    * @param region the cache region, never {@code null} or empty
    * @param timeToLiveSeconds the time to live in seconds, must be >= 0
    * @return {@code true} if the TTL was successfully set, {@code false} otherwise
    * @throws IllegalArgumentException if key/region is null/empty or timeToLiveSeconds &lt; 0
    */
   boolean setTimeToLive(Serializable key, String region, int timeToLiveSeconds);

   /**
    * Get the underlying cache manager for advanced operations.
    *
    * @return the cache manager instance, never {@code null}
    */
   CacheManager getManager();
}
