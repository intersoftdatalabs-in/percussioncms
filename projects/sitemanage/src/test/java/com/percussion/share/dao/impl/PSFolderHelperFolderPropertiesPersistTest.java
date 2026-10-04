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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.pathmanagement.data.PSFolderProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Folder Security property persist (#3206 / #5131): locale, community id, and display-format id
 * must be copied onto {@link PSFolder} on save. Display-format name is transient and is not
 * written here. A name-only update does not change {@code sys_displayformat}.
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
  void persistAclRepairOnRead_doesNotWrite() {
    assertFalse(PSFolderHelper.persistAclRepairOnRead());
  }

  @Test
  void applyPersistableFolderProperties_nulls_noThrow() {
    PSFolder folder = new PSFolder("Design", -1, PSObjectAclEntry.ACCESS_ADMIN, "test");
    PSFolderHelper.applyPersistableFolderProperties(null, new PSFolderProperties());
    PSFolderHelper.applyPersistableFolderProperties(folder, null);
    assertNotEquals("", folder.getName());
  }

  @Test
  void applyPersistableFolderProperties_writesDisplayFormatIdNotName() {
    PSFolder folder = new PSFolder("Design", -1, PSObjectAclEntry.ACCESS_ADMIN, "test");
    folder.setDisplayFormatPropertyValue("3");
    folder.setDisplayFormatName("Default");

    PSFolderProperties props = new PSFolderProperties();
    props.setDisplayFormatId("012");
    props.setDisplayFormatName("NotSimple");

    PSFolderHelper.applyPersistableFolderProperties(folder, props);

    assertEquals("12", folder.getDisplayFormatPropertyValue());
    assertEquals("Default", folder.getDisplayFormatName());
  }

  @Test
  void applyPersistableFolderProperties_nameOnlyDoesNotChangeStoredId() {
    PSFolder folder = new PSFolder("Design", -1, PSObjectAclEntry.ACCESS_ADMIN, "test");
    folder.setDisplayFormatPropertyValue("3");
    folder.setDisplayFormatName("Default");

    PSFolderProperties nameOnly = new PSFolderProperties();
    nameOnly.setDisplayFormatName("Simple");
    PSFolderHelper.applyPersistableFolderProperties(folder, nameOnly);
    assertEquals("3", folder.getDisplayFormatPropertyValue());
    assertEquals("Default", folder.getDisplayFormatName());

    PSFolderProperties namedId = new PSFolderProperties();
    namedId.setDisplayFormatId("Simple");
    PSFolderHelper.applyPersistableFolderProperties(folder, namedId);
    assertEquals("3", folder.getDisplayFormatPropertyValue());

    PSFolderProperties zero = new PSFolderProperties();
    zero.setDisplayFormatId("0");
    PSFolderHelper.applyPersistableFolderProperties(folder, zero);
    assertEquals("3", folder.getDisplayFormatPropertyValue());
    assertNull(new PSFolderProperties().getDisplayFormatId());
  }
}
