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
package com.percussion.services.assembly;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Typed views of JEXL / assembly binding maps stored as {@code Object} ({@code $sys},
 * {@code $sys.metadata}, nav variables). Prefer these over {@code @SuppressWarnings("unchecked")}.
 */
public final class PSAssemblyBindingMaps {

  private PSAssemblyBindingMaps() {}

  /**
   * Defensive copy of string-keyed entries. Returns {@code null} when {@code value} is not a {@link
   * Map}.
   */
  public static Map<String, Object> copyStringObjectMap(Object value) {
    if (!(value instanceof Map<?, ?> raw)) {
      return null;
    }
    Map<String, Object> typed = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : raw.entrySet()) {
      if (entry.getKey() instanceof String key) {
        typed.put(key, entry.getValue());
      }
    }
    return typed;
  }

  /**
   * Live {@code Map<String, Object>} view of an existing map. {@link Map#put} writes through to the
   * original instance so JEXL {@code $sys} holders stay in sync.
   *
   * @return {@code null} when {@code value} is not a {@link Map}
   */
  public static Map<String, Object> liveStringObjectMap(Object value) {
    if (!(value instanceof Map<?, ?> raw)) {
      return null;
    }
    return new LiveStringObjectMap(raw);
  }

  /**
   * {@code $sys} binding, or {@code null} when missing or not a map.
   */
  public static Map<String, Object> sysMap(Map<String, ?> bindings) {
    if (bindings == null) {
      return null;
    }
    return liveStringObjectMap(bindings.get("$sys"));
  }

  /**
   * Nested map under {@code $sys} (live view). {@code null} when {@code $sys} or the nested value
   * is missing or not a map.
   */
  public static Map<String, Object> sysNestedMap(Map<String, ?> bindings, String key) {
    Map<String, Object> sys = sysMap(bindings);
    if (sys == null || key == null) {
      return null;
    }
    return liveStringObjectMap(sys.get(key));
  }

  /**
   * Write-through view. The single residual unchecked conversion is isolated here: JEXL stores
   * {@code HashMap} / {@code LinkedHashMap} nodes that accept {@link String} keys.
   */
  static final class LiveStringObjectMap extends AbstractMap<String, Object> {
    private final Map<Object, Object> delegate;

    LiveStringObjectMap(Map<?, ?> raw) {
      this.delegate = erase(raw);
    }

    @Override
    public Object get(Object key) {
      return delegate.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
      return delegate.containsKey(key);
    }

    @Override
    public Object put(String key, Object value) {
      return delegate.put(key, value);
    }

    @Override
    public Object remove(Object key) {
      return delegate.remove(key);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      Set<Entry<String, Object>> entries = new LinkedHashSet<>();
      for (Map.Entry<Object, Object> entry : delegate.entrySet()) {
        if (entry.getKey() instanceof String key) {
          entries.add(new SimpleEntry<>(key, entry.getValue()));
        }
      }
      return entries;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> erase(Map<?, ?> raw) {
      return (Map<Object, Object>) raw;
    }
  }
}
