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
package com.percussion.services.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.publisher.data.PSContentListItem;
import com.percussion.services.publisher.data.PSContentListResults;
import com.percussion.services.publisher.impl.PSIteratorChain;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for typed {@code com.percussion.services.publisher} collections (issue
 * #3188 residual of #2022 after security batch #3181).
 */
@Tag("UnitTest")
@DisplayName("services.publisher package generics")
class PSServicesPublisherTypedTest {

  private static PSContentListItem item(int contentId) {
    IPSGuid cid = new PSLegacyGuid(contentId, 1);
    IPSGuid tid = new PSGuid(PSTypeEnum.TEMPLATE, 100L + contentId);
    return new PSContentListItem(cid, null, tid, null, 1);
  }

  @Test
  @DisplayName("PSContentListResults exposes typed Iterator once")
  void contentListResultsTypedIterator() {
    PSContentListItem a = item(10);
    PSContentListItem b = item(11);
    List<PSContentListItem> items = Arrays.asList(a, b);
    PSContentListResults results = new PSContentListResults(items.iterator(), items.size());

    assertEquals(2, results.getEstimatedSize());
    Iterator<PSContentListItem> it = results.iterator();
    assertTrue(it.hasNext());
    assertEquals(a, it.next());
    assertEquals(b, it.next());
    assertFalse(it.hasNext());

    // Second call must fail: iterator is single-use by design.
    assertThrows(IllegalArgumentException.class, results::iterator);
  }

  @Test
  @DisplayName("PSIteratorChain flattens typed iterator sequence")
  void iteratorChainTypedFlatten() {
    List<String> first = Arrays.asList("a", "b");
    List<String> second = Arrays.asList("c");
    List<Iterator<String>> sources = new ArrayList<>();
    sources.add(first.iterator());
    sources.add(second.iterator());

    PSIteratorChain<String> chain =
        new PSIteratorChain<String>() {
          private int index = 0;

          @Override
          protected Iterator<String> nextIterator() {
            if (index >= sources.size()) {
              return null;
            }
            return sources.get(index++);
          }
        };

    List<String> flattened = new ArrayList<>();
    while (chain.hasNext()) {
      flattened.add(chain.next());
    }
    assertEquals(Arrays.asList("a", "b", "c"), flattened);
    assertThrows(NoSuchElementException.class, chain::next);
  }

  @Test
  @DisplayName("PSIteratorChain empty when nextIterator always null")
  void iteratorChainEmpty() {
    PSIteratorChain<Integer> chain =
        new PSIteratorChain<Integer>() {
          @Override
          protected Iterator<Integer> nextIterator() {
            return null;
          }
        };
    assertFalse(chain.hasNext());
  }
}
