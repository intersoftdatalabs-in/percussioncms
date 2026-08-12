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

import com.percussion.cms.objectstore.PSObjectAclEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for pure ACL list ordering helpers on {@link PSFolderAclEditorDialog} after
 * rawtypes cleanup (no Swing UI).
 */
public class PSFolderAclEditorDialogTest {

  @Test
  public void sortAclEntriesOrdersVirtualRoleUserThenAlpha() {
    PSObjectAclEntry userB = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, "bob");
    PSObjectAclEntry userA = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, "alice");
    PSObjectAclEntry roleZ = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, "zrole");
    PSObjectAclEntry roleA = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, "arole");
    PSObjectAclEntry virtE =
        entry(PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL, PSObjectAclEntry.ACL_ENTRY_EVERYONE);
    PSObjectAclEntry virtC =
        entry(PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL, PSObjectAclEntry.ACL_ENTRY_FOLDER_COMMUNITY);

    List<PSObjectAclEntry> list =
        new ArrayList<>(Arrays.asList(userB, roleZ, virtE, userA, roleA, virtC));
    PSFolderAclEditorDialog.sortAclEntries(list);

    assertEquals(PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL, list.get(0).getType());
    assertEquals(PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL, list.get(1).getType());
    assertEquals(PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, list.get(2).getType());
    assertEquals(PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, list.get(3).getType());
    assertEquals(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, list.get(4).getType());
    assertEquals(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, list.get(5).getType());

    // alpha within type
    assertTrue(list.get(0).getName().compareTo(list.get(1).getName()) <= 0);
    assertEquals("arole", list.get(2).getName());
    assertEquals("zrole", list.get(3).getName());
    assertEquals("alice", list.get(4).getName());
    assertEquals("bob", list.get(5).getName());
  }

  @Test
  public void sortAclEntriesNullThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> PSFolderAclEditorDialog.sortAclEntries(null));
  }

  @Test
  public void sortAclEntriesEmptyIsNoOp() {
    List<PSObjectAclEntry> empty = new ArrayList<>();
    PSFolderAclEditorDialog.sortAclEntries(empty);
    assertTrue(empty.isEmpty());
  }

  @Test
  public void comparatorSameTypeAlpha() {
    PSObjectAclEntry a = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, "a");
    PSObjectAclEntry b = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, "b");
    assertTrue(PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(a, b) < 0);
    assertTrue(PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(b, a) > 0);
    assertEquals(0, PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(a, a));
  }

  @Test
  public void comparatorNullsLastNoNpe() {
    PSObjectAclEntry a = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, "a");
    assertEquals(0, PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(null, null));
    assertTrue(PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(a, null) < 0);
    assertTrue(PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(null, a) > 0);
  }

  @Test
  public void comparatorMatchesSecurityPanelOrdering() {
    PSObjectAclEntry user = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, "u");
    PSObjectAclEntry role = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, "r");
    PSObjectAclEntry virt = entry(PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL, "v");

    assertEquals(
        Integer.signum(PSFolderSecurityPanel.ACL_ENTRY_COMPARATOR.compare(virt, role)),
        Integer.signum(PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(virt, role)));
    assertEquals(
        Integer.signum(PSFolderSecurityPanel.ACL_ENTRY_COMPARATOR.compare(role, user)),
        Integer.signum(PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(role, user)));
    assertEquals(
        Integer.signum(PSFolderSecurityPanel.ACL_ENTRY_COMPARATOR.compare(virt, user)),
        Integer.signum(PSFolderAclEditorDialog.ACL_ENTRY_COMPARATOR.compare(virt, user)));
  }

  private static PSObjectAclEntry entry(int type, String name) {
    return new PSObjectAclEntry(type, name, PSObjectAclEntry.ACCESS_READ);
  }
}
