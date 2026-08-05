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
package com.percussion.webui.tags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A cache to hold Tmx key/value pairs for multiple languages. Used for the tmx tag libs. */
public class TmxCache {
  /** Private ctor to prevent instantiation. */
  private TmxCache() {}

  /**
   * Returns the singleton instance of the TmxCache object.
   *
   * @return the singleton object, never <code>null</code>.
   */
  public static TmxCache getInstance() {
    if (msInstance == null) {
      msInstance = new TmxCache();
    }
    return msInstance;
  }

  /**
   * Sets the lang/prefixes that have been indexed.
   *
   * @param lang cannot be <code>null</code> or empty.
   * @param prefixes cannot be <code>null</code> but may be empty.
   */
  public void setIndexed(String lang, String prefixes) {
    if (lang == null || lang.isEmpty()) {
      throw new IllegalArgumentException("lang cannot be null or empty.");
    }
    if (prefixes == null) {
      throw new IllegalArgumentException("prefixes cannot be null");
    }
    miCachedIndex.computeIfAbsent(lang, k -> new HashSet<>());
    miCachedIndex.get(lang).add(prefixes);
  }

  /**
   * Add a new entry to the cache.
   *
   * @param lang cannot be <code>null</code> or empty.
   * @param key cannot be <code>null</code> or empty.
   * @param val may be <code>null</code> or empty.
   */
  public void addEntry(String lang, String key, String val) {
    if (lang == null || lang.isEmpty()) {
      throw new IllegalArgumentException("lang cannot be null or empty.");
    }
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("key cannot be null or empty.");
    }
    miCache.computeIfAbsent(lang, k -> new HashMap<>());
    miCache.get(lang).put(key, val);
  }

  /**
   * Retrieve a set of all keys for a specified language.
   *
   * @param lang cannot be <code>null</code> or empty.
   * @return set of keys, never <code>null</code>, may be empty.
   */
  public Set<String> getKeys(String lang) {
    if (lang == null || lang.isEmpty()) {
      throw new IllegalArgumentException("lang cannot be null or empty.");
    }
    return miCache.getOrDefault(lang, new HashMap<>()).keySet();
  }

  /**
   * Retrieve the value based on the lang/key.
   *
   * @param lang cannot be <code>null</code> or empty.
   * @param key cannot be <code>null</code> or empty.
   * @return the value or <code>null</code> if no found.
   */
  public String getValue(String lang, String key) {
    if (lang == null || lang.isEmpty()) {
      throw new IllegalArgumentException("lang cannot be null or empty.");
    }
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("key cannot be null or empty.");
    }
    return miCache.getOrDefault(lang, new HashMap<>()).get(key);
  }

  /**
   * Indicates whether the supplied prefix set has been indexed for the given language.
   *
   * @param lang the language code
   * @param prefixes the comma-separated prefix set
   * @return {@code true} if the prefix set is cached for this language
   */
  public boolean isIndexed(String lang, String prefixes) {
    Set<String> prefixSet = miCachedIndex.get(lang);
    return prefixSet != null && prefixSet.contains(prefixes);
  }

  /**
   * Clear cache per lang or if no lang specified then clear all.
   *
   * @param lang may be <code>null</code> in which case everything will be cleared.
   */
  public void clear(String lang) {
    if (lang != null) {
      miCachedIndex.remove(lang);
      miCache.remove(lang);
    } else {
      miCachedIndex.clear();
      miCache.clear();
    }
  }

  /** The singleton instance of the tmx cache. */
  private static TmxCache msInstance;

  /** The cached index used to keep track of which lang/prefixes have been cached. */
  private final Map<String, Set<String>> miCachedIndex = new HashMap<>();

  /** The cache holding the key/values for each lang indexed. */
  private final Map<String, Map<String, String>> miCache = new HashMap<>();
}
