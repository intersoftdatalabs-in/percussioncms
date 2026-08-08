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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.objectstore.PSNode;
import com.percussion.utils.collections.PSIteratorUtils;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed {@link PSSelection} node/list APIs used by action managers. */
public class PSSelectionTest {

  @Test
  public void getNodeListReturnsTypedNodesInOrder() {
    PSNode parent = folder("parent", "Parent");
    PSNode a = item("a", "A");
    PSNode b = item("b", "B");
    PSUiMode mode = new PSUiMode(PSUiMode.TYPE_VIEW_CX, PSUiMode.TYPE_MODE_NAV);

    PSSelection sel = new PSSelection(mode, parent, Arrays.asList(a, b).iterator());

    Iterator<PSNode> nodes = sel.getNodeList();
    assertEquals("a", nodes.next().getName());
    assertEquals("b", nodes.next().getName());
    assertFalse(nodes.hasNext());
    assertEquals(2, sel.getNodeListSize());
    assertTrue(sel.isMultiSelect());
    assertTrue(sel.containsNode(a));
  }

  @Test
  public void getTypesIsUnionOfNodeTypes() {
    PSNode parent = folder("parent", "Parent");
    PSNode folder = folder("f1", "F1");
    PSNode item = item("i1", "I1");
    PSUiMode mode = new PSUiMode(PSUiMode.TYPE_VIEW_CX, PSUiMode.TYPE_MODE_NAV);

    PSSelection sel = new PSSelection(mode, parent, Arrays.asList(folder, item, folder).iterator());

    List<String> types = sel.getTypes();
    assertEquals(2, types.size());
    assertTrue(types.contains(PSNode.TYPE_FOLDER));
    assertTrue(types.contains(PSNode.TYPE_ITEM));
    assertEquals("", sel.getType()); // mixed selection
  }

  @Test
  public void sameTypeSelectionReportsTypeAndFolderHelpers() {
    PSNode parent = folder("parent", "Parent");
    PSNode f1 = folder("f1", "F1");
    PSNode f2 = folder("f2", "F2");
    PSUiMode mode = new PSUiMode(PSUiMode.TYPE_VIEW_CX, PSUiMode.TYPE_MODE_NAV);

    PSSelection sel = new PSSelection(mode, parent, Arrays.asList(f1, f2).iterator());
    assertEquals(PSNode.TYPE_FOLDER, sel.getType());
    assertTrue(sel.isFolderType());
    assertTrue(sel.isAnyFolderType());
    assertFalse(sel.isSystemFolderType());
  }

  @Test
  public void constructorAcceptsSingletonIteratorHelper() {
    PSNode node = item("only", "Only");
    PSUiMode mode = new PSUiMode(PSUiMode.TYPE_VIEW_CX, PSUiMode.TYPE_MODE_NAV);
    PSSelection sel = new PSSelection(mode, null, PSIteratorUtils.iterator(node));
    assertEquals(1, sel.getNodeListSize());
    assertEquals("only", sel.getNodeList().next().getName());
  }

  @Test
  public void constructorRejectsEmptyNodeList() {
    PSUiMode mode = new PSUiMode(PSUiMode.TYPE_VIEW_CX, PSUiMode.TYPE_MODE_NAV);
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSSelection(mode, null, Arrays.<PSNode>asList().iterator()));
  }

  private static PSNode folder(String name, String label) {
    return new PSNode(name, label, PSNode.TYPE_FOLDER, "url", null, false, 1);
  }

  private static PSNode item(String name, String label) {
    return new PSNode(name, label, PSNode.TYPE_ITEM, "", null, false, -1);
  }
}
