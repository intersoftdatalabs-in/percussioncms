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
package com.percussion.utils.collections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Deep copier
 *
 * @author dougrand
 */
public class PSCopier {
  /**
   * Deep copy the passed map. Most map elements will be copied by value but any map values will be
   * deep copied themselves.
   *
   * @param input the input map, never <code>null</code>
   * @return a deep copied map, never <code>null</code>
   */

  public static <K, V> Map<K, V> deepCopy(Map<K, V> input) {
    Map<K, V> rval = new HashMap<>();
    Iterator<? extends Map.Entry<K, V>> eiter = input.entrySet().iterator();
    while (eiter.hasNext()) {
      Map.Entry<K, V> entry = eiter.next();
      V value = entry.getValue();
      if (value instanceof Map) {
        Map<?, ?> nested = (Map<?, ?>) value;
        @SuppressWarnings("unchecked")
        V copiedValue = (V) deepCopy(nested);
        rval.put(entry.getKey(), copiedValue);
      } else {
        rval.put(entry.getKey(), value);
      }
    }
    return rval;
  }
}
