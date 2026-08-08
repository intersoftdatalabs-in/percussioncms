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
package com.percussion.utils.testing;

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Useful printer methods to aid in debugging
 *
 * @author dougrand
 */
public class PSTestPrinter {
  /**
   * Print the map entries in alphabetic order of the contained keys
   *
   * @param map the map, never <code>null</code>
   */
  public static void printMapEntries(Map<?, ?> map) {
    if (map == null) {
      throw new IllegalArgumentException("map may not be null");
    }
    Map<String, String> values = new TreeMap<>();
    for (Map.Entry<?, ?> e : map.entrySet()) {
      Object key = e.getKey();
      Object value = e.getValue();
      values.put(String.valueOf(key), String.valueOf(value));
    }

    for (Map.Entry<String, String> e : values.entrySet()) {
      System.out.println(e.getKey() + ": " + e.getValue());
    }
  }

  /**
   * Print the property entries in alphabetic order of the contained keys
   *
   * @param props the properties, never <code>null</code>
   */
  public static void printMapEntries(Properties props) {
    if (props == null) {
      throw new IllegalArgumentException("props may not be null");
    }
    Map<String, String> values = new TreeMap<>();
    for (Map.Entry<Object, Object> e : props.entrySet()) {
      values.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
    }

    for (Map.Entry<String, String> e : values.entrySet()) {
      System.out.println(e.getKey() + ": " + e.getValue());
    }
  }
}
