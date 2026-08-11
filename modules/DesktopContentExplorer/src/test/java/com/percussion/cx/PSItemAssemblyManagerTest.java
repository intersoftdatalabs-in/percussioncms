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
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for pure helpers on {@link PSItemAssemblyManager} after rawtypes cleanup
 * (#3057). Does not open the Swing applet (requires live server resources).
 */
public class PSItemAssemblyManagerTest {

  @Test
  public void isSlotTargetTrueForSlotType() {
    PSNode slot = new PSNode("s", "Slot", PSNode.TYPE_SLOT, "", null, false, -1);
    assertTrue(PSItemAssemblyManager.isSlotTarget(slot));
  }

  @Test
  public void isSlotTargetFalseForItemOrNull() {
    PSNode item = new PSNode("i", "Item", PSNode.TYPE_SLOT_ITEM, "", null, false, -1);
    assertFalse(PSItemAssemblyManager.isSlotTarget(item));
    assertFalse(PSItemAssemblyManager.isSlotTarget(null));
  }

  @Test
  public void resolveDropIndexSlotIsMaxValue() {
    PSNode slot = new PSNode("s", "Slot", PSNode.TYPE_SLOT, "", null, false, -1);
    assertEquals(Integer.MAX_VALUE, PSItemAssemblyManager.resolveDropIndex(slot));
  }

  @Test
  public void resolveDropIndexItemUsesSortRank() {
    PSNode item = new PSNode("i", "Item", PSNode.TYPE_SLOT_ITEM, "", null, false, -1);
    item.setProperty(IPSConstants.PROPERTY_SORTRANK, "7");
    assertEquals(7, PSItemAssemblyManager.resolveDropIndex(item));
  }

  @Test
  public void resolveDropIndexRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> PSItemAssemblyManager.resolveDropIndex(null));
  }

  @Test
  public void resolveReorderDropIndexMoveDownUsesMaxRankPlusAdjustedDirection() {
    PSNode a = rankNode("a", 2);
    PSNode b = rankNode("b", 5);
    // direction 1 -> moveDown increments to 2; max rank 5; result 7
    assertEquals(
        7,
        PSItemAssemblyManager.resolveReorderDropIndex(Arrays.asList(a, b).iterator(), 1));
  }

  @Test
  public void resolveReorderDropIndexMoveUpUsesMinRankPlusDirection() {
    PSNode a = rankNode("a", 2);
    PSNode b = rankNode("b", 5);
    // direction -1; min rank 2; result 1
    assertEquals(
        1,
        PSItemAssemblyManager.resolveReorderDropIndex(Arrays.asList(a, b).iterator(), -1));
  }

  @Test
  public void resolveReorderDropIndexRejectsNullNodes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSItemAssemblyManager.resolveReorderDropIndex(null, 1));
  }

  @Test
  public void buildActiveAssemblerParamsTypedMap() {
    Map<String, String> params =
        PSItemAssemblyManager.buildActiveAssemblerParams("insert", "<doc/>");
    assertEquals("insert", params.get("sys_command"));
    assertEquals("<doc/>", params.get(PSItemAssemblyManager.INPUT_DOC));
    assertEquals(2, params.size());
  }

  @Test
  public void buildActiveAssemblerParamsRejectsNulls() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSItemAssemblyManager.buildActiveAssemblerParams(null, "<doc/>"));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSItemAssemblyManager.buildActiveAssemblerParams("insert", null));
  }

  @Test
  public void buildActiveAssemblerParamsEmptyDocAllowed() {
    Map<String, String> params =
        PSItemAssemblyManager.buildActiveAssemblerParams("delete", "");
    assertEquals("", params.get(PSItemAssemblyManager.INPUT_DOC));
  }

  private static PSNode rankNode(String name, int rank) {
    PSNode node = new PSNode(name, name, PSNode.TYPE_SLOT_ITEM, "", null, false, -1);
    node.setProperty(IPSConstants.PROPERTY_SORTRANK, Integer.toString(rank));
    return node;
  }
}
