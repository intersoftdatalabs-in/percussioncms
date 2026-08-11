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
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for pure helpers on {@link PSSearchViewActionManager} after rawtypes cleanup.
 * Full search execution / catalog load requires a live applet and server.
 */
public class PSSearchViewActionManagerTest {

  @Test
  public void isNodeInitializableAcceptsConfiguredSearchTypes() {
    assertTrue(PSSearchViewActionManager.isNodeInitializable(node(PSNode.TYPE_NEW_SRCH)));
    assertTrue(PSSearchViewActionManager.isNodeInitializable(node(PSNode.TYPE_STANDARD_SRCH)));
    assertTrue(PSSearchViewActionManager.isNodeInitializable(node(PSNode.TYPE_CUSTOM_SRCH)));
    assertTrue(PSSearchViewActionManager.isNodeInitializable(node(PSNode.TYPE_EMPTY_SRCH)));
  }

  @Test
  public void isNodeInitializableRejectsNonSearchTypes() {
    assertFalse(PSSearchViewActionManager.isNodeInitializable(node(PSNode.TYPE_FOLDER)));
    assertFalse(PSSearchViewActionManager.isNodeInitializable(node(PSNode.TYPE_ITEM)));
  }

  @Test
  public void isNodeInitializableRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> PSSearchViewActionManager.isNodeInitializable(null));
  }

  @Test
  public void setAsInitializedMarksNewSearchProperty() {
    PSNode searchNode = node(PSNode.TYPE_NEW_SRCH);
    PSSearchViewActionManager.setAsInitialized(searchNode);
    assertEquals("1", searchNode.getProp("isInitialized"));
  }

  @Test
  public void setAsInitializedRejectsNonInitializableNode() {
    PSNode folder = node(PSNode.TYPE_FOLDER);
    assertThrows(
        IllegalArgumentException.class, () -> PSSearchViewActionManager.setAsInitialized(folder));
  }

  @Test
  public void setAsInitializedRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> PSSearchViewActionManager.setAsInitialized(null));
  }

  @Test
  public void msNodeTypesInitializableContainsExpectedTypes() {
    List<String> types = PSSearchViewActionManager.ms_nodeTypesInitializable;
    assertTrue(types.contains(PSNode.TYPE_STANDARD_SRCH));
    assertTrue(types.contains(PSNode.TYPE_CUSTOM_SRCH));
    assertTrue(types.contains(PSNode.TYPE_NEW_SRCH));
    assertTrue(types.contains(PSNode.TYPE_EMPTY_SRCH));
    // intentionally not TYPE_SAVE_SRCH (commented for backward compatibility)
    assertFalse(types.contains(PSNode.TYPE_SAVE_SRCH));
  }

  private static PSNode node(String type) {
    // Folders require non-negative permissions; other types use -1.
    int perms = PSNode.TYPE_FOLDER.equals(type) ? 1 : -1;
    return new PSNode("n", "Label", type, null, null, false, perms);
  }
}
