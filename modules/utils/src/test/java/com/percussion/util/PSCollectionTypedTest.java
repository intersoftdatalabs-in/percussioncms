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
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSCollection} after parameterizing as {@code
 * PSConcurrentList<Object>} with {@code Class<?>}/{@code Collection<?>}/{@code Iterator<?>}
 * surfaces (#3015 batch 8).
 */
@Tag("UnitTest")
public class PSCollectionTypedTest {

  @Test
  public void memberClassAndElementsRoundTripThroughObjectListApi() {
    PSCollection coll = new PSCollection(String.class);
    assertEquals(String.class, coll.getMemberClassType());
    assertTrue(coll.add("a"));
    coll.add(0, "b");
    assertEquals(Arrays.asList("b", "a"), List.copyOf(coll));
    assertEquals("b", coll.set(0, "c"));
    assertEquals("c", coll.get(0));
  }

  @Test
  public void addAllRejectsWrongElementType() {
    PSCollection coll = new PSCollection(String.class);
    coll.add("ok");
    List<Object> mixed = new ArrayList<>();
    mixed.add("fine");
    mixed.add(Integer.valueOf(1));
    assertThrows(ClassCastException.class, () -> coll.addAll(mixed));
    assertEquals(1, coll.size());
  }

  @Test
  public void iteratorCtorInfersMemberClass() {
    Iterator<String> it = Arrays.asList("x", "y").iterator();
    PSCollection coll = new PSCollection(it);
    assertEquals(String.class, coll.getMemberClassType());
    assertEquals(2, coll.size());
    assertEquals("x", coll.get(0));
    assertThrows(ClassCastException.class, () -> coll.add(Integer.valueOf(3)));
  }

  @Test
  public void initialCapacityCtorStillEnforcesType() {
    PSCollection coll = new PSCollection(Integer.class, 8);
    assertTrue(coll.add(Integer.valueOf(1)));
    assertThrows(ClassCastException.class, () -> coll.add("nope"));
  }
}
