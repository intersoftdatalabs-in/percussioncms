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
package com.percussion.services.legacy.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSCmsObject;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSItemEntry}.
 *
 * <p>Regression coverage for the {@code isFolder()} method, which was broken in the JDK-21
 * stabilization pass (commit cc3d38494) where it was hard-coded to {@code return false}, causing
 * {@code FolderCache} to emit spurious "the owner is not a folder" validation errors on every
 * startup even on a clean install.
 */
class PSItemEntryTest {

  // PSCmsObject.TYPE_FOLDER == 2  (regular items use 1)
  private static final int ITEM_TYPE = 1;
  private static final int FOLDER_TYPE = PSCmsObject.TYPE_FOLDER;

  // -----------------------------------------------------------------------
  // isFolder()
  // -----------------------------------------------------------------------

  @Test
  void testIsFolderReturnsTrueForFolderObjectType() {
    PSItemEntry entry = new PSItemEntry(1, "Sites", -1, 101, FOLDER_TYPE);
    assertTrue(
        entry.isFolder(),
        "isFolder() must return true when objectType == PSCmsObject.TYPE_FOLDER ("
            + FOLDER_TYPE
            + ")");
  }

  @Test
  void testIsFolderReturnsFalseForNonFolderObjectType() {
    PSItemEntry entry = new PSItemEntry(10, "SomeItem", -1, 313, ITEM_TYPE);
    assertFalse(
        entry.isFolder(),
        "isFolder() must return false when objectType != PSCmsObject.TYPE_FOLDER");
  }

  @Test
  void testIsFolderForSystemFolders() {
    // Mirrors the pre-defined CONTENTSTATUS rows (IDs 1–8) that ship in
    // cmsTableData.xml; all have OBJECTTYPE=2 and must be recognised as folders.
    int[] systemFolderIds = {1, 2, 3, 4, 5, 6, 7, 8};
    String[] titles = {
      "Root", "Sites", "Folders", "$System$", "Templates", "UserProfiles", "Assets", "Recycler"
    };

    for (int i = 0; i < systemFolderIds.length; i++) {
      PSItemEntry entry = new PSItemEntry(systemFolderIds[i], titles[i], -1, 101, FOLDER_TYPE);
      assertTrue(
          entry.isFolder(),
          "System folder contentId="
              + systemFolderIds[i]
              + " ('"
              + titles[i]
              + "') must be identified as a folder");
    }
  }

  // -----------------------------------------------------------------------
  // getObjectType()
  // -----------------------------------------------------------------------

  @Test
  void testGetObjectTypeReturnsStoredValue() {
    PSItemEntry folder = new PSItemEntry(1, "Root", -1, 101, FOLDER_TYPE);
    PSItemEntry item = new PSItemEntry(2, "Page", -1, 313, ITEM_TYPE);

    assertTrue(
        folder.getObjectType() == FOLDER_TYPE,
        "getObjectType() must return FOLDER_TYPE for folder entries");
    assertTrue(
        item.getObjectType() == ITEM_TYPE,
        "getObjectType() must return ITEM_TYPE for non-folder entries");
  }
}
