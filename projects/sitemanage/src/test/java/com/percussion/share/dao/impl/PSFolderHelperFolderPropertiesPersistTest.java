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
package com.percussion.share.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.pathmanagement.data.PSFolderProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Folder Security property persist (#3206): locale and community id must be copied onto {@link
 * PSFolder} on save. Display-format name is transient and is not written here.
 */
@Tag("UnitTest")
class PSFolderHelperFolderPropertiesPersistTest {

  @Test
  void applyPersistableFolderProperties_copiesLocaleAndCommunity() {
    PSFolder folder = new PSFolder("Design", -1, PSObjectAclEntry.ACCESS_ADMIN, "test");
    folder.setLocale("en-us");
    folder.setCommunityId(-1);

    PSFolderProperties props = new PSFolderProperties();
    props.setLocale("fr-fr");
    props.setCommunityId(1001);

    PSFolderHelper.applyPersistableFolderProperties(folder, props);

    assertEquals("fr-fr", folder.getLocale());
    assertEquals(1001, folder.getCommunityId());
  }

  @Test
  void applyPersistableFolderProperties_blankLocale_leavesExisting() {
    PSFolder folder = new PSFolder("Design", -1, PSObjectAclEntry.ACCESS_ADMIN, "test");
    folder.setLocale("en-us");

    PSFolderProperties props = new PSFolderProperties();
    props.setLocale("   ");

    PSFolderHelper.applyPersistableFolderProperties(folder, props);

    assertEquals("en-us", folder.getLocale());
  }

  @Test
  void applyPersistableFolderProperties_nulls_noThrow() {
    PSFolder folder = new PSFolder("Design", -1, PSObjectAclEntry.ACCESS_ADMIN, "test");
    PSFolderHelper.applyPersistableFolderProperties(null, new PSFolderProperties());
    PSFolderHelper.applyPersistableFolderProperties(folder, null);
    assertNotEquals("", folder.getName());
  }
}
