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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cx.objectstore.PSNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for pure helpers on {@link PSFolderActionManager} (CM1 site filter +
 * node/folder conversions) after rawtypes cleanup.
 */
public class PSFolderActionManagerTest {

  @Test
  public void removeCM1SiteFoldersFiltersRegisteredNames() {
    String dropName = "cm1-drop-" + UUID.randomUUID();
    PSFolderActionManager.addCM1SiteRootFolder("//Sites/" + dropName);

    PSNode keep = folder("keep-" + UUID.randomUUID(), "Keep");
    PSNode drop = folder(dropName, dropName);
    List<PSNode> filtered =
        PSFolderActionManager.removeCM1SiteFolders(Arrays.asList(keep, drop));

    assertEquals(1, filtered.size());
    assertEquals(keep.getName(), filtered.get(0).getName());
  }

  @Test
  public void removeCM1SiteFoldersNullAndEmpty() {
    assertTrue(PSFolderActionManager.removeCM1SiteFolders(null).isEmpty());
    assertTrue(PSFolderActionManager.removeCM1SiteFolders(new ArrayList<>()).isEmpty());
  }

  @Test
  public void removeCM1SiteFoldersPreservesUnrelated() {
    PSNode a = folder("a-" + UUID.randomUUID(), "A");
    PSNode b = folder("b-" + UUID.randomUUID(), "B");
    List<PSNode> filtered = PSFolderActionManager.removeCM1SiteFolders(Arrays.asList(a, b));
    assertEquals(2, filtered.size());
    assertEquals(a.getName(), filtered.get(0).getName());
    assertEquals(b.getName(), filtered.get(1).getName());
  }

  @Test
  public void folderToNodeAndNodeToFolderRoundTripIds() {
    PSFolder folder = new PSFolder("roundtrip", 4242, 7, 1, "desc");
    PSNode node = PSFolderActionManager.folderToNode(folder);

    assertEquals("roundtrip", node.getName());
    assertEquals(PSNode.TYPE_FOLDER, node.getType());
    assertEquals("4242", node.getContentId());

    PSFolder back = PSFolderActionManager.nodeToFolder(node);
    assertEquals("roundtrip", back.getName());
    assertEquals(4242, back.getLocator().getId());
  }

  @Test
  public void nodesToFoldersAndFoldersToNodesPreserveOrder() {
    PSNode n1 = folderWithId("n1", 101);
    PSNode n2 = folderWithId("n2", 202);

    List<PSFolder> folders =
        PSFolderActionManager.nodesToFolders(Arrays.asList(n1, n2).iterator());
    assertEquals(2, folders.size());
    assertEquals(101, folders.get(0).getLocator().getId());
    assertEquals(202, folders.get(1).getLocator().getId());

    Iterator<PSNode> nodes = PSFolderActionManager.foldersToNodes(folders.iterator());
    assertEquals("n1", nodes.next().getName());
    assertEquals("n2", nodes.next().getName());
    assertFalse(nodes.hasNext());
  }

  @Test
  public void nodesToFoldersRejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> PSFolderActionManager.nodesToFolders(null));
    assertThrows(IllegalArgumentException.class, () -> PSFolderActionManager.foldersToNodes(null));
    assertThrows(IllegalArgumentException.class, () -> PSFolderActionManager.folderToNode(null));
    assertThrows(IllegalArgumentException.class, () -> PSFolderActionManager.nodeToFolder(null));
  }

  @Test
  public void nodeToFolderRejectsMissingContentId() {
    PSNode bad = folder("no-id", "No Id");
    assertThrows(IllegalArgumentException.class, () -> PSFolderActionManager.nodeToFolder(bad));
  }

  private static PSNode folder(String name, String label) {
    return new PSNode(name, label, PSNode.TYPE_FOLDER, "url", null, false, 1);
  }

  private static PSNode folderWithId(String name, int contentId) {
    PSNode node = folder(name, name);
    node.setProperty(IPSConstants.PROPERTY_CONTENTID, Integer.toString(contentId));
    assertNotNull(node.getContentId());
    return node;
  }
}
