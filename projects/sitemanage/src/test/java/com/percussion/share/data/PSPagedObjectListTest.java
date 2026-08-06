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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.share.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PSPagedObjectList}. */
class PSPagedObjectListTest {

  private static List<Integer> sample(int n) {
    return IntStream.rangeClosed(1, n).boxed().toList();
  }

  @Test
  void getPageReturnsFirstPageWhenStartIndexIsOne() {
    var page = PSPagedObjectList.getPage(sample(10), 1, 3);
    assertEquals(1, page.getStartIndex());
    assertEquals(List.of(1, 2, 3), page.getChildrenInPage());
  }

  @Test
  void getPageClampsZeroStartIndexToOne() {
    // Per the documented contract, a startIndex less than 1 must be treated as the first page.
    var page = PSPagedObjectList.getPage(sample(10), 0, 3);
    assertEquals(1, page.getStartIndex());
    assertEquals(List.of(1, 2, 3), page.getChildrenInPage());
  }

  @Test
  void getPageClampsNegativeStartIndexToOne() {
    var page = PSPagedObjectList.getPage(sample(10), -5, 3);
    assertEquals(1, page.getStartIndex());
    assertEquals(List.of(1, 2, 3), page.getChildrenInPage());
  }

  @Test
  void getPageReturnsRequestedStartIndexForInRangeValue() {
    var page = PSPagedObjectList.getPage(sample(10), 4, 3);
    assertEquals(4, page.getStartIndex());
    assertEquals(List.of(4, 5, 6), page.getChildrenInPage());
  }

  @Test
  void getPageRejectsNullStartIndex() {
    assertThrows(NullPointerException.class, () -> PSPagedObjectList.getPage(sample(5), null, 3));
  }

  @Test
  void getPageRejectsZeroMaxResults() {
    assertThrows(IllegalArgumentException.class, () -> PSPagedObjectList.getPage(sample(5), 1, 0));
  }
}
