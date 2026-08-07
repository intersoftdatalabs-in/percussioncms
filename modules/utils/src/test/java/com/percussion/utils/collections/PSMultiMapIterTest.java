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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.Test;

public class PSMultiMapIterTest {
  public PSMultiMapIterTest() {}

  @Test
  public void test1() throws Exception {
    MultiValuedMap<String, Integer> map = new ArrayListValuedHashMap<>();

    map.put("a", 1);
    map.put("b", 2);
    map.put("b", 3);
    map.put("b", 4);
    map.put("c", 5);
    map.put("d", 6);

    PSMultiMapIterator<Integer> simple = new PSMultiMapIterator<>(map.asMap(), null);
    Set<Integer> results = new HashSet<>();
    int count = 0;
    while (simple.hasNext()) {
      results.add(simple.next());
      count++;
    }

    assertEquals(6, count);
    assertTrue(results.contains(1));
    assertTrue(results.contains(2));
    assertTrue(results.contains(3));
    assertTrue(results.contains(4));
    assertTrue(results.contains(5));
    assertTrue(results.contains(6));
  }

  @Test
  public void test2() throws Exception {
    MultiValuedMap<String, Integer> map = new ArrayListValuedHashMap<>();

    map.put("a", 1);
    map.put("b", 2);
    map.put("b", 3);
    map.put("b", 4);
    map.put("c", 5);
    map.put("d", 6);

    Predicate<Object> keyIsB =
        new Predicate<Object>() {
          @Override
          public boolean evaluate(Object o) {
            return "b".equals(o);
          }
        };
    PSMultiMapIterator<Integer> simple = new PSMultiMapIterator<>(map.asMap(), keyIsB);

    Set<Integer> results = new HashSet<>();
    int count = 0;
    while (simple.hasNext()) {
      results.add(simple.next());
      count++;
    }

    assertEquals(3, count);
    assertTrue(results.contains(2));
    assertTrue(results.contains(3));
    assertTrue(results.contains(4));
  }
}
