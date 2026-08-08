/*
 * Copyright (c) 2023 Intersoft Data Labs, Inc.
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
package com.percussion.cx.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.util.PSEntrySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Test case for the {@link PSNode} class. */
public class PSNodeTest {
  /**
   * Test the clone method to ensure child collections are properly supported.
   *
   * @throws Exception if there are any errors
   */
  @Test
  public void testClone() throws Exception {
    PSNode node1 = new PSNode("test1", "test 1", PSNode.TYPE_FOLDER, "url", "iconKey", true, 1);

    // add a child to node1 to ensure children are cloned
    PSNode child = new PSNode("child1", "child 1", PSNode.TYPE_ITEM, "", "", false, -1);
    node1.addChild(child);

    PSNode copy = (PSNode) node1.clone();

    assertNotSame(node1, copy);
    assertEquals(node1.getName(), copy.getName());
    assertEquals(node1.getLabel(), copy.getLabel());
    assertEquals(node1.getType(), copy.getType());
    assertEquals(node1.shouldExpand(), copy.shouldExpand());

    // verify children were cloned (deep copy)
    Iterator<PSNode> origChildren = node1.getChildren();
    Iterator<PSNode> copyChildren = copy.getChildren();
    assertNotNull(origChildren);
    assertNotNull(copyChildren);
    assertTrue(origChildren.hasNext());
    assertTrue(copyChildren.hasNext());
    PSNode origChild = origChildren.next();
    PSNode clonedChild = copyChildren.next();
    assertNotSame(origChild, clonedChild);
    assertEquals(origChild.getName(), clonedChild.getName());
  }

  @Test
  public void testSetAndGetChildrenTyped() {
    PSNode parent = new PSNode("p", "Parent", PSNode.TYPE_FOLDER, "url", null, false, 1);
    PSNode a = new PSNode("a", "A", PSNode.TYPE_ITEM, "", null, false, -1);
    PSNode b = new PSNode("b", "B", PSNode.TYPE_ITEM, "", null, false, -1);

    List<PSNode> kids = Arrays.asList(a, b);
    parent.setChildren(kids.iterator());

    assertEquals(2, parent.getChildCount());
    Iterator<PSNode> it = parent.getChildren();
    assertEquals("a", it.next().getName());
    assertEquals("b", it.next().getName());
    assertFalse(it.hasNext());

    parent.setChildren(null);
    assertNull(parent.getChildren());
    assertEquals(-1, parent.getChildCount());
  }

  @Test
  public void testReplaceChild() {
    PSNode parent = new PSNode("p", "Parent", PSNode.TYPE_FOLDER, "url", null, false, 1);
    PSNode oldChild = new PSNode("c1", "Old", PSNode.TYPE_ITEM, "", null, false, -1);
    PSNode newChild = new PSNode("c1", "New", PSNode.TYPE_ITEM, "", null, false, -1);
    parent.addChild(oldChild);
    parent.replaceChild(newChild);

    Iterator<PSNode> it = parent.getChildren();
    assertTrue(it.hasNext());
    assertEquals("New", it.next().getLabel());
  }

  @Test
  public void testRowDataRoundTrip() {
    PSNode node = new PSNode("i", "Item", PSNode.TYPE_ITEM, "", null, false, -1);
    Map<String, Object> data = new HashMap<>();
    data.put("sys_title", "Hello");
    data.put("sys_contentid", Integer.valueOf(42));
    node.setRowData(data);

    Map<String, Object> out = node.getRowData();
    assertNotNull(out);
    assertEquals("Hello", out.get("sys_title"));
    assertEquals(42, out.get("sys_contentid"));
    // defensive copy: mutations to returned map must not affect node
    out.put("sys_title", "mutated");
    assertEquals("Hello", node.getRowData().get("sys_title"));
  }

  @Test
  public void testChildrenDisplayFormat() {
    PSNode node = new PSNode("p", "Parent", PSNode.TYPE_FOLDER, "url", null, false, 1);
    List<Map.Entry<String, String>> defs = new ArrayList<>();
    defs.add(new PSEntrySet<>("sys_title", PSNode.DATA_TYPE_TEXT));
    defs.add(new PSEntrySet<>("sys_contentid", PSNode.DATA_TYPE_NUMBER));

    node.setChildrenDisplayFormat(defs.iterator());
    Iterator<Map.Entry<String, String>> format = node.getChildrenDisplayFormat();
    assertNotNull(format);
    assertTrue(format.hasNext());
    Map.Entry<String, String> first = format.next();
    assertEquals("sys_title", first.getKey());
    assertEquals(PSNode.DATA_TYPE_TEXT, first.getValue());

    node.clearChildrenDisplayFormat();
    assertNull(node.getChildrenDisplayFormat());
  }

  @Test
  public void testFolderAndSearchTypesCollections() {
    Collection<String> folders = PSNode.getFolderTypes();
    assertTrue(folders.contains(PSNode.TYPE_FOLDER));
    assertTrue(folders.contains(PSNode.TYPE_SITE));

    Collection<String> searches = PSNode.getSearchTypes();
    assertTrue(searches.contains(PSNode.TYPE_NEW_SRCH));
    assertTrue(searches.contains(PSNode.TYPE_VIEW));

    assertThrows(UnsupportedOperationException.class, () -> folders.add("X"));
    assertThrows(UnsupportedOperationException.class, () -> searches.add("X"));
  }

  @Test
  public void testDirtyChildren() {
    PSNode parent = new PSNode("p", "Parent", PSNode.TYPE_CATEGORY, "", null, false, -1);
    PSNode dirty = new PSNode("d", "Dirty", PSNode.TYPE_ITEM, "", null, false, -1);
    dirty.setIsDirty(true);
    PSNode clean = new PSNode("c", "Clean", PSNode.TYPE_ITEM, "", null, false, -1);
    parent.setChildren(Arrays.asList(dirty, clean).iterator());

    assertTrue(parent.hasDirtyChildren());
    Iterator<PSNode> dirtyIt = parent.getDirtyChildren(false);
    assertTrue(dirtyIt.hasNext());
    assertEquals("d", dirtyIt.next().getName());
    assertFalse(dirtyIt.hasNext());

    parent.clearDirtyChildren(false);
    assertFalse(dirty.isDirty());
    assertFalse(parent.hasDirtyChildren());
  }

  @Test
  public void testLastSortColumns() {
    PSNode node = new PSNode("p", "Parent", PSNode.TYPE_FOLDER, "url", null, false, 1);
    List<Integer> cols = Arrays.asList(0, 2);
    node.setLastSortColumns(cols);
    assertEquals(cols, node.getLastSortColumns());
    node.setLastSortColumns(null);
    assertNull(node.getLastSortColumns());
  }

  @Test
  public void testHasChildOfTypeAndFindChild() {
    PSNode parent = new PSNode("p", "Parent", PSNode.TYPE_FOLDER, "url", null, false, 1);
    PSNode item = new PSNode("i1", "Item", PSNode.TYPE_ITEM, "", null, false, -1);
    item.setProperty("sys_contentid", "99");
    parent.addChild(item);

    assertTrue(parent.hasChildOfType(PSNode.TYPE_ITEM));
    assertFalse(parent.hasChildOfType(PSNode.TYPE_VIEW));

    PSNode found = parent.findChildNode("99", PSNode.TYPE_ITEM, false);
    assertNotNull(found);
    assertEquals("i1", found.getName());
    assertNull(parent.findChildNode("nope", PSNode.TYPE_ITEM, false));
  }
}
