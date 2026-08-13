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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.objectstore.PSNode;
import com.percussion.search.PSBaseExecutableSearch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed category sorting helpers on {@link PSExecutableSearch}. Full search
 * execution requires a live applet/server; these cover the pure sort surface parameterized for
 * rawtypes clearance.
 */
public class PSExecutableSearchTest {

  @Test
  public void categoryLabelComparatorOrdersByLabel() {
    Comparator<PSNode> comp = PSExecutableSearch.categoryLabelComparator();
    PSNode zebra = new PSNode("z", "Zebra", PSNode.TYPE_CATEGORY, null, null, false, -1);
    PSNode apple = new PSNode("a", "Apple", PSNode.TYPE_CATEGORY, null, null, false, -1);
    assertTrue(comp.compare(apple, zebra) < 0);
    assertTrue(comp.compare(zebra, apple) > 0);
    assertEquals(0, comp.compare(apple, apple));
  }

  @Test
  public void sortCategoryChildrenOrdersTopLevelCategoryChildren() {
    PSNode root = new PSNode("root", "Root", PSNode.TYPE_PARENT, null, null, false, -1);
    PSNode c = new PSNode("c", "Charlie", PSNode.TYPE_CATEGORY, null, null, false, -1);
    PSNode a = new PSNode("a", "Alpha", PSNode.TYPE_CATEGORY, null, null, false, -1);
    PSNode b = new PSNode("b", "Bravo", PSNode.TYPE_CATEGORY, null, null, false, -1);
    root.setChildren(Arrays.asList(c, a, b).iterator());

    PSExecutableSearch.sortCategoryChildren(root, PSExecutableSearch.categoryLabelComparator());

    List<String> labels = new ArrayList<>();
    Iterator<PSNode> kids = root.getChildren();
    while (kids.hasNext()) {
      labels.add(kids.next().getLabel());
    }
    assertEquals(Arrays.asList("Alpha", "Bravo", "Charlie"), labels);
  }

  @Test
  public void sortCategoryChildrenRecursesIntoNestedCategoryChildren() {
    PSNode root = new PSNode("root", "Root", PSNode.TYPE_PARENT, null, null, false, -1);
    PSNode catOuter = new PSNode("outer", "Outer", PSNode.TYPE_CATEGORY, null, null, false, -1);
    PSNode nestedZ = new PSNode("z", "Zulu", PSNode.TYPE_CATEGORY, null, null, false, -1);
    PSNode nestedA = new PSNode("a", "Able", PSNode.TYPE_CATEGORY, null, null, false, -1);
    catOuter.setChildren(Arrays.asList(nestedZ, nestedA).iterator());
    root.setChildren(Arrays.asList(catOuter).iterator());

    PSExecutableSearch.sortCategoryChildren(root, PSExecutableSearch.categoryLabelComparator());

    Iterator<PSNode> outerKids = root.getChildren().next().getChildren();
    assertEquals("Able", outerKids.next().getLabel());
    assertEquals("Zulu", outerKids.next().getLabel());
    assertFalse(outerKids.hasNext());
  }

  @Test
  public void sortCategoryChildrenSkipsNonCategoryChildren() {
    PSNode root = new PSNode("root", "Root", PSNode.TYPE_PARENT, null, null, false, -1);
    PSNode itemB = new PSNode("b", "Beta", PSNode.TYPE_ITEM, null, null, false, -1);
    PSNode itemA = new PSNode("a", "Alpha", PSNode.TYPE_ITEM, null, null, false, -1);
    root.setChildren(Arrays.asList(itemB, itemA).iterator());

    PSExecutableSearch.sortCategoryChildren(root, PSExecutableSearch.categoryLabelComparator());

    Iterator<PSNode> kids = root.getChildren();
    // items are not reordered (first child is not TYPE_CATEGORY)
    assertEquals("Beta", kids.next().getLabel());
    assertEquals("Alpha", kids.next().getLabel());
  }

  @Test
  public void copyContentIdListNullReturnsNull() {
    assertNull(PSExecutableSearch.copyContentIdList(null));
  }

  @Test
  public void copyContentIdListCopiesIntegersAndSkipsNonIntegers() {
    List<Object> raw = new ArrayList<>();
    raw.add(Integer.valueOf(7));
    raw.add("not-an-id");
    raw.add(Integer.valueOf(9));
    List<Integer> copy = PSExecutableSearch.copyContentIdList(raw);
    assertEquals(Arrays.asList(7, 9), copy);
    copy.add(11);
    assertEquals(3, raw.size());
    assertFalse(raw.contains(11));
  }

  @Test
  public void copySearchPropSetUsesCxAndRelatedContentBases() {
    Set<String> cx = PSExecutableSearch.copySearchPropSet(false);
    Set<String> rc = PSExecutableSearch.copySearchPropSet(true);
    assertTrue(cx.containsAll(PSBaseExecutableSearch.ms_cxPropSet));
    assertTrue(rc.containsAll(PSBaseExecutableSearch.ms_cxRCPropSet));
    assertTrue(cx.contains("sys_contentid"));
    assertTrue(rc.contains("sys_variantid"));
    cx.add("extra-cx");
    rc.add("extra-rc");
    assertFalse(PSBaseExecutableSearch.ms_cxPropSet.contains("extra-cx"));
    assertFalse(PSBaseExecutableSearch.ms_cxRCPropSet.contains("extra-rc"));
  }
}
