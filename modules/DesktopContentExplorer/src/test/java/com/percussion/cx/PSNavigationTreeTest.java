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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for pure {@link PSNavigationTree} helpers (full tree construction requires a
 * live {@link PSActionManager} HTTP session).
 */
public class PSNavigationTreeTest {

  @Test
  public void treeAndNodeClassesAreFinal() {
    assertTrue(
        Modifier.isFinal(PSNavigationTree.class.getModifiers()),
        "PSNavigationTree must be final to avoid this-escape in the ctor");
    assertTrue(
        Modifier.isFinal(PSNavigationTree.PSTreeNode.class.getModifiers()),
        "PSTreeNode must be final to avoid this-escape in the ctor");
  }

  @Test
  public void getRefreshNodeTypeMapsKnownHints() {
    assertEquals(
        PSNavigationTree.NODE_ROOT,
        PSNavigationTree.getRefreshNodeType(PSActionEvent.REFRESH_NAV_ROOT));
    assertEquals(
        PSNavigationTree.NODE_SELECTED,
        PSNavigationTree.getRefreshNodeType(PSActionEvent.REFRESH_NAV_SELECTED));
    assertEquals(
        PSNavigationTree.NODE_SEL_PARENT,
        PSNavigationTree.getRefreshNodeType(PSActionEvent.REFRESH_NAV_SEL_PARENT));
    assertEquals(-1, PSNavigationTree.getRefreshNodeType("unknown-hint"));
  }

  @Test
  public void getRefreshNodeTypeRejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> PSNavigationTree.getRefreshNodeType(null));
  }
}
