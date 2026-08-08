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
package com.percussion.utils.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSIteratorUtilsTest {

  @Test
  public void countedIteratorYieldsElementNTimes() {
    Iterator<String> it = PSIteratorUtils.iterator("x", 3);
    assertTrue(it.hasNext());
    assertEquals("x", it.next());
    assertEquals("x", it.next());
    assertEquals("x", it.next());
    assertFalse(it.hasNext());
    assertThrows(NoSuchElementException.class, it::next);
  }

  @Test
  public void countedIteratorZeroIterationsIsEmpty() {
    // Use a non-int second arg form: int 0 would also match iterator(Object, Object).
    Iterator<String> it = PSIteratorUtils.iterator("zero", 0);
    assertFalse(it.hasNext());
  }

  @Test
  public void countedIteratorRejectsNegativeIterations() {
    assertThrows(IllegalArgumentException.class, () -> PSIteratorUtils.iterator("a", -1));
  }

  @Test
  public void singleElementIteratorAndCloneList() {
    Iterator<String> it = PSIteratorUtils.iterator("solo");
    List<String> list = PSIteratorUtils.cloneList(it);
    assertEquals(List.of("solo"), list);
  }

  @Test
  public void protectedIteratorDisallowsRemove() {
    Iterator<Object> it = PSIteratorUtils.protectedIterator(List.of("a").iterator());
    assertTrue(it.hasNext());
    assertEquals("a", it.next());
    assertThrows(UnsupportedOperationException.class, it::remove);
  }
}
