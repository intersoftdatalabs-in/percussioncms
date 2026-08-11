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
package com.percussion.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.assembly.data.PSAssemblyWorkItem;
import com.percussion.services.contentmgr.data.PSContentNode;
import com.percussion.services.contentmgr.impl.PSContentUtils;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed services contentmgr / publisher / assembly APIs after rawtypes cleanup
 * (#2934 / parent #2877).
 */
@Tag("UnitTest")
@DisplayName("services contentmgr/publisher/assembly package generics")
class PSServicesPackageTypedTest {

  @Test
  @DisplayName("PSContentUtils.makeQueryRef indexes typed Class<?> list")
  void makeQueryRefUsesTypedClassList() {
    List<Class<?>> classes = new ArrayList<>();
    classes.add(String.class);
    classes.add(Integer.class);
    PSPair<String, Class<?>> ref = new PSPair<>("title", String.class);
    assertEquals("c0.title", PSContentUtils.makeQueryRef(ref, classes));

    PSPair<String, Class<?>> second = new PSPair<>("count", Integer.class);
    assertEquals("c1.count", PSContentUtils.makeQueryRef(second, classes));

    PSPair<String, Class<?>> unknown = new PSPair<>("x", Double.class);
    assertThrows(IllegalStateException.class, () -> PSContentUtils.makeQueryRef(unknown, classes));

    PSPair<String, Class<?>> noClass = new PSPair<>("cs.m_title", null);
    assertEquals("cs.m_title", PSContentUtils.makeQueryRef(noClass, classes));
  }

  @Test
  @DisplayName("PSContentUtils.makeQueryRef rejects null inputs")
  void makeQueryRefNullGuards() {
    List<Class<?>> classes = new ArrayList<>();
    assertThrows(IllegalArgumentException.class, () -> PSContentUtils.makeQueryRef(null, classes));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSContentUtils.makeQueryRef(new PSPair<>("x", String.class), null));
  }

  @Test
  @DisplayName("PSContentNode child MultiValuedMap is typed for getNode/getIndex")
  void contentNodeTypedChildren() throws Exception {
    PSContentNode parent = new PSContentNode(null, "root", null, null, null, null);
    // Mark children loaded so unit test does not require Spring repository wiring.
    parent.setChildrenLoaded(true);
    Node child = parent.addNode("kid");
    assertNotNull(child);
    assertEquals("kid", child.getName());

    Node lookedUp = parent.getNode("kid");
    assertTrue(lookedUp instanceof PSContentNode);
    assertEquals(0, ((PSContentNode) lookedUp).getIndex());

    assertThrows(PathNotFoundException.class, () -> parent.getNode("missing"));
  }

  @Test
  @DisplayName("PSContentNode getIndex: parent null → 0; empty same-name list → -1")
  void contentNodeGetIndexMissingChildSentinel() throws Exception {
    // Root / no parent: index is 0 (JCR single-root convention).
    PSContentNode root = new PSContentNode(null, "root", null, null, null, null);
    root.setChildrenLoaded(true);
    assertEquals(0, root.getIndex());

    // Child present under parent has same-name-sibling index 0.
    Node child = root.addNode("solo");
    assertEquals(0, ((PSContentNode) child).getIndex());

    // Parent map has no entries for this name: MultiValuedMap#get yields an empty
    // collection (not null). Pre-generics code then did indexOf → -1; do not force 0.
    PSContentNode parent = new PSContentNode(null, "p", null, null, null, null);
    parent.setChildrenLoaded(true);
    PSContentNode ghost = new PSContentNode(null, "ghost", parent, null, null, null);
    assertEquals(-1, ghost.getIndex());
  }

  @Test
  @DisplayName("PSAssemblyWorkItem.getMetaData reads typed $sys map")
  void assemblyWorkItemTypedSysMetadata() {
    PSAssemblyWorkItem item = new PSAssemblyWorkItem();
    Map<String, Object> bindings = new HashMap<>();
    Map<String, Object> sys = new HashMap<>();
    Map<String, Object> meta = new HashMap<>();
    meta.put("k", "v");
    sys.put("metadata", meta);
    bindings.put("$sys", sys);
    item.setBindings(bindings);

    Map<String, Object> result = item.getMetaData();
    assertNotNull(result);
    assertEquals("v", result.get("k"));
  }

  @Test
  @DisplayName("PSAssemblyWorkItem.getMetaData returns null when $sys is not a Map")
  void assemblyWorkItemRejectsNonMapSys() {
    PSAssemblyWorkItem item = new PSAssemblyWorkItem();
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", "not-a-map");
    item.setBindings(bindings);
    assertEquals(null, item.getMetaData());
  }
}
