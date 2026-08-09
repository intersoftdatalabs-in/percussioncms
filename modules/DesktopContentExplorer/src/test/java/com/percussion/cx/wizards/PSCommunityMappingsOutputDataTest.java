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
package com.percussion.cx.wizards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSCloningOptions;
import com.percussion.cx.catalogers.PSCommunityCataloger;
import java.util.HashMap;
import java.util.Map;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed community mapping helpers used by copy-site cloning options (#2545).
 */
public class PSCommunityMappingsOutputDataTest {

  @Test
  public void appendMappingIfDifferent_addsOnlyWhenIdsDiffer() {
    Map<Integer, Integer> map = new HashMap<>();
    PSCommunityCataloger.Community src = community(1001, "Default");
    PSCommunityCataloger.Community same = community(1001, "Default");
    PSCommunityCataloger.Community tgt = community(1002, "Editor");

    assertTrue(PSCommunityMappingsPage.OutputData.appendMappingIfDifferent(map, src, same));
    assertTrue(map.isEmpty());

    assertTrue(PSCommunityMappingsPage.OutputData.appendMappingIfDifferent(map, src, tgt));
    assertEquals(1, map.size());
    assertEquals(Integer.valueOf(1002), map.get(1001));
  }

  @Test
  public void appendMappingIfDifferent_nullCommunityStopsScan() {
    Map<Integer, Integer> map = new HashMap<>();
    PSCommunityCataloger.Community src = community(1, "A");

    assertFalse(PSCommunityMappingsPage.OutputData.appendMappingIfDifferent(map, null, src));
    assertFalse(PSCommunityMappingsPage.OutputData.appendMappingIfDifferent(map, src, null));
    assertTrue(map.isEmpty());
  }

  @Test
  public void appendMappingIfDifferent_rejectsNullMap() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSCommunityMappingsPage.OutputData.appendMappingIfDifferent(
                null, community(1, "A"), community(2, "B")));
  }

  @Test
  public void buildMappingsFromModel_skipsIdentityAndStopsAtNullRow() {
    DefaultTableModel model =
        new DefaultTableModel(new Object[] {"Source", "Target"}, 0);
    model.addRow(new Object[] {community(10, "SrcA"), community(20, "TgtA")});
    model.addRow(new Object[] {community(30, "SrcB"), community(30, "SrcB")}); // identity
    model.addRow(new Object[] {community(40, "SrcC"), community(50, "TgtC")});
    model.addRow(new Object[] {null, community(60, "AfterNull")});
    model.addRow(new Object[] {community(70, "Unreached"), community(80, "Unreached")});

    Map<Integer, Integer> map = PSCommunityMappingsPage.OutputData.buildMappingsFromModel(model);

    assertEquals(2, map.size());
    assertEquals(Integer.valueOf(20), map.get(10));
    assertEquals(Integer.valueOf(50), map.get(40));
    assertFalse(map.containsKey(30));
    assertFalse(map.containsKey(70));
  }

  @Test
  public void buildMappingsFromModel_rejectsNullModel() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSCommunityMappingsPage.OutputData.buildMappingsFromModel(null));
  }

  /**
   * Verifies that a typed {@code Map<Integer, Integer>} (the OutputData shape produced by {@link
   * PSCommunityMappingsPage.OutputData#buildMappingsFromModel}) is accepted by {@link
   * PSCloningOptions} without an unchecked cast — same call-site shape as {@code PSActionManager}.
   */
  @Test
  public void typedMap_assignableToCloningOptionsParameter() {
    DefaultTableModel model = new DefaultTableModel(new Object[] {"Source", "Target"}, 0);
    model.addRow(new Object[] {community(100, "From"), community(200, "To")});

    Map<Integer, Integer> mappings =
        PSCommunityMappingsPage.OutputData.buildMappingsFromModel(model);

    PSCloningOptions options =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE_SUBFOLDER,
            "folder",
            PSCloningOptions.COPY_ALL_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_NEW_COPY,
            mappings);

    Map<Integer, Integer> stored = options.getCommunityMappings();
    assertEquals(1, stored.size());
    assertEquals(Integer.valueOf(200), stored.get(100));
  }

  private static PSCommunityCataloger.Community community(int id, String name) {
    return PSCommunityCataloger.createCommunity(id, name, name);
  }
}
