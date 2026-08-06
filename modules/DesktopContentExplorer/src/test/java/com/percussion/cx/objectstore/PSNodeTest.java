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

import java.util.Iterator;
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
    Iterator origChildren = node1.getChildren();
    Iterator copyChildren = copy.getChildren();
    assertNotNull(origChildren);
    assertNotNull(copyChildren);
    assertTrue(origChildren.hasNext());
    assertTrue(copyChildren.hasNext());
    PSNode origChild = (PSNode) origChildren.next();
    PSNode clonedChild = (PSNode) copyChildren.next();
    assertNotSame(origChild, clonedChild);
    assertEquals(origChild.getName(), clonedChild.getName());
  }
}
