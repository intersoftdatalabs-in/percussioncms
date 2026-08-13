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
package com.percussion.server.webservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.PSCmsException;
import com.percussion.design.objectstore.PSLocator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed folder locator path collection (#3290 / parent #2299). Lookup is
 * injected so tests do not construct {@link PSServerFolderProcessor} (static singleton needs a live
 * CMS object catalog).
 */
@DisplayName("PSServerFolderProcessor folder locator paths")
class PSServerFolderProcessorLocatorPathsTest {

  @Test
  @DisplayName("null locator is rejected")
  void collect_rejectsNullLocator() {
    assertThrows(
        IllegalArgumentException.class, () -> PSFolderLocatorPaths.collect(null, locator -> List.of()));
  }

  @Test
  @DisplayName("null parent lookup is rejected")
  void collect_rejectsNullLookup() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSFolderLocatorPaths.collect(new PSLocator(100, 1), null));
  }

  @Test
  @DisplayName("item with no folder parents yields an empty path list")
  void collect_noParents_empty() throws PSCmsException {
    PSLocator item = new PSLocator(100, 1);
    List<List<PSLocator>> paths = PSFolderLocatorPaths.collect(item, locator -> List.of());
    assertTrue(paths.isEmpty());
  }

  @Test
  @DisplayName("item in two folders yields one parent-first path per folder")
  void collect_twoParents_twoPaths() throws PSCmsException {
    PSLocator item = new PSLocator(100, 1);
    PSLocator folderA = new PSLocator(10, 1);
    PSLocator folderB = new PSLocator(20, 1);
    PSLocator root = new PSLocator(1, 1);

    Map<Integer, List<PSLocator>> tree = new HashMap<>();
    tree.put(item.getId(), List.of(folderA, folderB));
    tree.put(folderA.getId(), List.of(root));
    tree.put(folderB.getId(), List.of());
    tree.put(root.getId(), List.of());

    List<List<PSLocator>> paths = PSFolderLocatorPaths.collect(item, lookup(tree));

    assertEquals(2, paths.size());
    assertEquals(List.of(folderA, root), paths.get(0));
    assertEquals(List.of(folderB), paths.get(1));
  }

  @Test
  @DisplayName("ancestor walk follows only the first immediate parent")
  void collect_walkUsesFirstParentOnly() throws PSCmsException {
    PSLocator item = new PSLocator(100, 1);
    PSLocator folder = new PSLocator(10, 1);
    PSLocator firstRoot = new PSLocator(1, 1);
    PSLocator ignoredRoot = new PSLocator(2, 1);

    Map<Integer, List<PSLocator>> tree = new HashMap<>();
    tree.put(item.getId(), List.of(folder));
    tree.put(folder.getId(), List.of(firstRoot, ignoredRoot));
    tree.put(firstRoot.getId(), List.of());

    List<List<PSLocator>> paths = PSFolderLocatorPaths.collect(item, lookup(tree));

    assertEquals(1, paths.size());
    assertEquals(List.of(folder, firstRoot), paths.get(0));
  }

  private static PSFolderLocatorPaths.FolderParentLookup lookup(
      Map<Integer, List<PSLocator>> tree) {
    return locator -> {
      List<PSLocator> parents = tree.get(locator.getId());
      return parents == null ? new ArrayList<>() : parents;
    };
  }
}
