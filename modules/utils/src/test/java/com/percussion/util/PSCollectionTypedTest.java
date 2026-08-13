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
 * PSConcurrentList<E>}. Residual of #3015 batch 8 / #3173: locking the type to {@code
 * List<Object>} broke perc-system covariant {@code iterator()} overrides.
 */
@Tag("UnitTest")
public class PSCollectionTypedTest {

  @Test
  public void memberClassAndElementsRoundTripThroughTypedListApi() {
    PSCollection<String> coll = new PSCollection<>(String.class);
    assertEquals(String.class, coll.getMemberClassType());
    assertTrue(coll.add("a"));
    coll.add(0, "b");
    assertEquals(Arrays.asList("b", "a"), List.copyOf(coll));
    assertEquals("b", coll.set(0, "c"));
    assertEquals("c", coll.get(0));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void classNameCtorLoadsMemberClassWithoutUncheckedCast() throws ClassNotFoundException {
    PSCollection<String> coll = new PSCollection<>(String.class.getName());
    assertEquals(String.class, coll.getMemberClassType());
    assertTrue(coll.add("ok"));
    PSCollection raw = coll;
    assertThrows(ClassCastException.class, () -> raw.add(Integer.valueOf(1)));
    assertThrows(ClassNotFoundException.class, () -> new PSCollection("no.such.Type" + System.nanoTime()));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void addAllRejectsWrongElementType() {
    PSCollection<String> coll = new PSCollection<>(String.class);
    coll.add("ok");
    List<Object> mixed = new ArrayList<>();
    mixed.add("fine");
    mixed.add(Integer.valueOf(1));
    PSCollection raw = coll;
    assertThrows(ClassCastException.class, () -> raw.addAll(mixed));
    assertEquals(1, coll.size());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void iteratorCtorInfersMemberClass() {
    Iterator<String> it = Arrays.asList("x", "y").iterator();
    PSCollection<String> coll = new PSCollection<>(it);
    assertEquals(String.class, coll.getMemberClassType());
    assertEquals(2, coll.size());
    assertEquals("x", coll.get(0));
    PSCollection raw = coll;
    assertThrows(ClassCastException.class, () -> raw.add(Integer.valueOf(3)));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void initialCapacityCtorStillEnforcesType() {
    PSCollection<Integer> coll = new PSCollection<>(Integer.class, 8);
    assertTrue(coll.add(Integer.valueOf(1)));
    PSCollection raw = coll;
    assertThrows(ClassCastException.class, () -> raw.add("nope"));
  }

  @Test
  public void typedCollectionIteratorIsElementType() {
    PSCollection<String> coll = new PSCollection<>(String.class);
    coll.add("a");
    Iterator<String> it = coll.iterator();
    assertEquals("a", it.next());
    Iterable<String> iterable = coll;
    int n = 0;
    for (String s : iterable) {
      n++;
      assertEquals("a", s);
    }
    assertEquals(1, n);
  }

  /**
   * perc-system pattern: raw {@code PSCollection} / {@code PSCollectionComponent} subclasses
   * override {@code iterator()} to {@code Iterator<Specific>} and pass the set as {@code
   * Iterable<Specific>}.
   */
  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void rawSubclassMayOverrideCovariantIterator() {
    class TypedRaw extends PSCollection {
      private static final long serialVersionUID = 1L;

      TypedRaw() {
        super(String.class);
      }

      @Override
      public Iterator<String> iterator() {
        return super.iterator();
      }
    }
    TypedRaw coll = new TypedRaw();
    coll.add("a");
    Iterator<String> it = coll.iterator();
    assertEquals("a", it.next());
    Iterable<String> iterable = coll;
    assertEquals("a", iterable.iterator().next());
  }
}
