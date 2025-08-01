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

package com.percussion.server.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class for managing aging operations and time-based expiration.
 * Provides thread-safe aging functionality using Java 11 time APIs.
 *
 * @since Java 11
 */
public final class PSAging {

   private static final ConcurrentMap<String, AgingEntry> agingCache = new ConcurrentHashMap<>();

   /**
    * Private constructor to prevent instantiation of utility class.
    */
   private PSAging() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
   }

   /**
    * Records a timestamp for the given key.
    *
    * @param key the key to associate with the timestamp, must not be null
    * @throws IllegalArgumentException if key is null
    */
   public static void recordTime(String key) {
      PSUtils.validateNotNull(key, "key");
      agingCache.put(key, new AgingEntry(Instant.now()));
   }

   /**
    * Checks if the entry for the given key has aged beyond the specified duration.
    *
    * @param key the key to check, must not be null
    * @param maxAge the maximum age duration, must not be null
    * @return {@code true} if the entry has aged beyond maxAge or doesn't exist,
    *         {@code false} otherwise
    * @throws IllegalArgumentException if any parameter is null
    */
   public static boolean hasAged(String key, Duration maxAge) {
      PSUtils.validateNotNull(key, "key");
      PSUtils.validateNotNull(maxAge, "maxAge");

      return Optional.ofNullable(agingCache.get(key))
         .map(entry -> entry.hasAged(maxAge))
         .orElse(true); // Consider missing entries as aged
   }

   /**
    * Gets the age of the entry for the given key.
    *
    * @param key the key to check, must not be null
    * @return the age duration, or empty if the key doesn't exist
    * @throws IllegalArgumentException if key is null
    */
   public static Optional<Duration> getAge(String key) {
      PSUtils.validateNotNull(key, "key");

      return Optional.ofNullable(agingCache.get(key))
         .map(AgingEntry::getAge);
   }

   /**
    * Removes the aging entry for the given key.
    *
    * @param key the key to remove, must not be null
    * @return {@code true} if an entry was removed, {@code false} otherwise
    * @throws IllegalArgumentException if key is null
    */
   public static boolean removeEntry(String key) {
      PSUtils.validateNotNull(key, "key");
      return agingCache.remove(key) != null;
   }

   /**
    * Clears all aging entries.
    */
   public static void clearAll() {
      agingCache.clear();
   }

   /**
    * Gets the number of aging entries currently stored.
    *
    * @return the number of entries
    */
   public static int getEntryCount() {
      return agingCache.size();
   }

   /**
    * Removes all entries that have aged beyond the specified duration.
    *
    * @param maxAge the maximum age for entries to keep, must not be null
    * @return the number of entries removed
    * @throws IllegalArgumentException if maxAge is null
    */
   public static int purgeAged(Duration maxAge) {
      PSUtils.validateNotNull(maxAge, "maxAge");

      var removedCount = 0;
      var iterator = agingCache.entrySet().iterator();

      while (iterator.hasNext()) {
         var entry = iterator.next();
         if (entry.getValue().hasAged(maxAge)) {
            iterator.remove();
            removedCount++;
         }
      }

      return removedCount;
   }

   /**
    * Internal class representing an aging entry with a timestamp.
    */
   private static final class AgingEntry {
      private final Instant timestamp;

      AgingEntry(Instant timestamp) {
         this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
      }

      boolean hasAged(Duration maxAge) {
         var age = Duration.between(timestamp, Instant.now());
         return age.compareTo(maxAge) > 0;
      }

      Duration getAge() {
         return Duration.between(timestamp, Instant.now());
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null || getClass() != obj.getClass()) return false;
         var that = (AgingEntry) obj;
         return Objects.equals(timestamp, that.timestamp);
      }

      @Override
      public int hashCode() {
         return Objects.hash(timestamp);
      }

      @Override
      public String toString() {
         return "AgingEntry{timestamp=" + timestamp + ", age=" + getAge() + "}";
      }
   }
}
