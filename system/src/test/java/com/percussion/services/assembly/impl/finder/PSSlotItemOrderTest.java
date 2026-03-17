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

package com.percussion.services.assembly.impl.finder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.percussion.services.assembly.impl.finder.PSContentFinderBase.ContentItem;
import com.percussion.services.assembly.impl.finder.PSContentFinderBase.ContentItemOrder;
import com.percussion.utils.guid.IPSGuid;
import java.util.Comparator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test class for
 * com.percussion.services.assembly.impl.finder.PSBaseSlotContentFinder.SlotItemOrder
 */
@ExtendWith(MockitoExtension.class)
public class PSSlotItemOrderTest {
  // Mockito will be used directly; no JMock context

  /** Tests SlotItemOrder.compare() with variously configured <code>SlotItem</code>s. */
  @Test
  @Disabled("TODO: This test is broken, please fix me")
  public void testOrder() {
    Comparator<ContentItem> c = new ContentItemOrder();
    int result;

    IPSGuid itemId = mock(IPSGuid.class);
    IPSGuid templateId = mock(IPSGuid.class);

    // #1: check with valid sort ranks
    ContentItem item1 = new ContentItem(itemId, templateId, 1);
    ContentItem item2 = new ContentItem(itemId, templateId, 2);

    result = c.compare(item1, item2);
    assertTrue(result < 0);
    result = c.compare(item2, item1);
    assertTrue(result > 0);

    // #2: equal sort ranks, no relationship ids = should use item id
    item1.setSortrank(0);
    item2.setSortrank(0);

    when(itemId.longValue()).thenReturn(1L, 2L);

    result = c.compare(item1, item2);
    assertTrue(result < 0);

    // #3: equal sort ranks, only one relationship id = should use item id
    IPSGuid relationshipId = mock(IPSGuid.class);
    item1.setRelationshipId(relationshipId);
    // itemId stub already returns 1 then 2 again
    when(itemId.longValue()).thenReturn(1L, 2L);

    result = c.compare(item1, item2);
    assertTrue(result < 0);

    // #4: equal sort ranks, both relationship id = should use relationship id
    item2.setRelationshipId(relationshipId);
    when(relationshipId.longValue()).thenReturn(20L, 10L);

    result = c.compare(item1, item2);
    assertTrue(result > 0);
  }
}
