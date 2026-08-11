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
package com.percussion.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed search-exit helpers (#2873 / epic #2022 residual of #2386).
 */
public class PSGenerateSearchQueryExitTypedTest {

  @Test
  public void valueListIteratorCrossProductSingleList() {
    List<List<String>> values = new ArrayList<>();
    values.add(Arrays.asList("a", "b"));
    PSGenerateSearchQueryExit.PSValueListIterator it =
        new PSGenerateSearchQueryExit.PSValueListIterator(values);

    assertTrue(it.hasNext());
    assertEquals(Collections.singletonList("a"), it.next());
    assertEquals(Collections.singletonList("b"), it.next());
    assertFalse(it.hasNext());
    assertThrows(NoSuchElementException.class, it::next);
  }

  @Test
  public void valueListIteratorCrossProductTwoLists() {
    List<List<String>> values = new ArrayList<>();
    values.add(Arrays.asList("p1", "p2"));
    values.add(Arrays.asList("c1", "c2"));
    PSGenerateSearchQueryExit.PSValueListIterator it =
        new PSGenerateSearchQueryExit.PSValueListIterator(values);

    List<List<String>> combos = new ArrayList<>();
    while (it.hasNext()) {
      combos.add(it.next());
    }

    assertEquals(4, combos.size());
    assertEquals(Arrays.asList("p1", "c1"), combos.get(0));
    assertEquals(Arrays.asList("p2", "c1"), combos.get(1));
    assertEquals(Arrays.asList("p1", "c2"), combos.get(2));
    assertEquals(Arrays.asList("p2", "c2"), combos.get(3));
  }

  @Test
  public void valueListIteratorRejectsEmptyOuter() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSGenerateSearchQueryExit.PSValueListIterator(new ArrayList<>()));
  }

  @Test
  public void valueListIteratorRejectsEmptyInnerList() {
    List<List<String>> values = new ArrayList<>();
    values.add(Arrays.asList("a"));
    values.add(new ArrayList<>());
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSGenerateSearchQueryExit.PSValueListIterator(values));
  }

  @Test
  public void valueListIteratorRemoveUnsupported() {
    List<List<String>> values = new ArrayList<>();
    values.add(Arrays.asList("only"));
    PSGenerateSearchQueryExit.PSValueListIterator it =
        new PSGenerateSearchQueryExit.PSValueListIterator(values);
    assertThrows(UnsupportedOperationException.class, it::remove);
  }
}
