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
package com.percussion.fastforward.sfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for {@link PSContentList} and {@link PSContentListItem} generics / ordering
 * cleanup (issue #2035 batch 1).
 */
class PSContentListTest {

  private static PSContentListItem item(String contentId, String variantId) {
    return new PSContentListItem(
        contentId,
        "1",
        variantId,
        "/assemble",
        "title",
        "10",
        new Date(),
        null,
        "admin",
        "1",
        "0",
        "/Sites/demo",
        new PSSiteFolderContentListLinkGenerator(),
        null,
        "http",
        "localhost",
        9992,
        Set.of("sys_siteid"),
        false);
  }

  @Test
  void addItemAndSize() {
    PSContentList list = new PSContentList("0", "filesystem");
    assertEquals(0, list.size());
    list.addItem(item("100", "1"));
    list.addItem(item("200", "2"));
    assertEquals(2, list.size());
  }

  @Test
  void addItemRejectsNull() {
    PSContentList list = new PSContentList("0", "filesystem");
    assertThrows(IllegalArgumentException.class, () -> list.addItem(null));
  }

  @Test
  void sortOrdersByContentIdAndVariantId() {
    PSContentList list = new PSContentList("0", "filesystem");
    list.addItem(item("200", "1"));
    list.addItem(item("100", "2"));
    list.addItem(item("100", "1"));
    list.sort();
    // compareTo key is contentId + "," + variantId — assert list order after sort
    assertEquals(3, list.size());
    assertEquals("100", list.getItem(0).getContentId());
    assertEquals("1", list.getItem(0).getVariantId());
    assertEquals("100", list.getItem(1).getContentId());
    assertEquals("2", list.getItem(1).getVariantId());
    assertEquals("200", list.getItem(2).getContentId());
    assertEquals("1", list.getItem(2).getVariantId());
    assertTrue(item("100", "1").compareTo(item("100", "2")) < 0);
    assertTrue(item("100", "2").compareTo(item("200", "1")) < 0);
  }

  @Test
  void compareToNullThrowsNpe() {
    assertThrows(NullPointerException.class, () -> item("1", "1").compareTo(null));
  }

  @Test
  void clearEmptiesList() {
    PSContentList list = new PSContentList("0", "filesystem");
    list.addItem(item("1", "1"));
    list.clear();
    assertEquals(0, list.size());
  }
}
