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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSComponentSummary;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Behavioral tests for pure helpers on {@link PSItemRelationshipsManager}. */
public class PSItemRelationshipsManagerTest {

  @Test
  public void formatDependencyLabelNullAndEmptyRowData() {
    assertEquals("", PSItemRelationshipsManager.formatDependencyLabel(null, null));
    assertEquals("Base", PSItemRelationshipsManager.formatDependencyLabel("Base", null));
    assertEquals(
        "Base",
        PSItemRelationshipsManager.formatDependencyLabel("Base", Collections.emptyMap()));
  }

  @Test
  public void formatDependencyLabelAppendsRowValuesInOrder() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("sys_title", "Hello");
    row.put("sys_contentid", "42");

    assertEquals(
        "Item (Hello - 42)",
        PSItemRelationshipsManager.formatDependencyLabel("Item", row));
  }

  @Test
  public void formatDependencyLabelHandlesNullValue() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("col", null);
    assertEquals("L (null)", PSItemRelationshipsManager.formatDependencyLabel("L", row));
  }

  @Test
  public void isAllowedRelationshipSlotRegisteredOrInline() {
    List<String> reg = Arrays.asList("10", "20");
    List<String> inline = Arrays.asList("99");

    assertTrue(PSItemRelationshipsManager.isAllowedRelationshipSlot("10", reg, inline));
    assertTrue(PSItemRelationshipsManager.isAllowedRelationshipSlot("99", reg, inline));
    assertFalse(PSItemRelationshipsManager.isAllowedRelationshipSlot("55", reg, inline));
    assertFalse(PSItemRelationshipsManager.isAllowedRelationshipSlot(null, reg, inline));
    assertFalse(PSItemRelationshipsManager.isAllowedRelationshipSlot("10", null, null));
  }

  @Test
  public void filterSummariesByContentIdsKeepsMatchesInOrder() {
    PSComponentSummary a = summary(101, "a");
    PSComponentSummary b = summary(202, "b");
    PSComponentSummary c = summary(303, "c");
    Iterator<PSComponentSummary> all = Arrays.asList(a, b, c).iterator();

    List<PSComponentSummary> filtered =
        PSItemRelationshipsManager.filterSummariesByContentIds(
            all, Arrays.asList("303", "101"));

    assertEquals(2, filtered.size());
    assertEquals(101, filtered.get(0).getContentId());
    assertEquals(303, filtered.get(1).getContentId());
  }

  @Test
  public void filterSummariesByContentIdsNullSafe() {
    assertTrue(
        PSItemRelationshipsManager.filterSummariesByContentIds(null, Arrays.asList("1"))
            .isEmpty());
    assertTrue(
        PSItemRelationshipsManager.filterSummariesByContentIds(
                Collections.emptyIterator(), null)
            .isEmpty());
    assertTrue(
        PSItemRelationshipsManager.filterSummariesByContentIds(
                Collections.emptyIterator(), Collections.emptyList())
            .isEmpty());
  }

  private static PSComponentSummary summary(int contentId, String name) {
    return new PSComponentSummary(
        contentId,
        1,
        1,
        -1,
        PSComponentSummary.TYPE_ITEM,
        name,
        1L,
        0);
  }
}
