/*
 * Copyright 1999-2026 Percussion Software, Inc. and Intersoft Data Labs, Inc.
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
package com.percussion.server.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSFolderAcl;
import com.percussion.cms.objectstore.PSObjectAcl;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.cms.objectstore.PSObjectPermissions;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link PSFolderEntry#updateFolder(PSFolder)} ACL conversion (#3077). Ensures Assets-style
 * permission reset can cache a {@link PSFolderAcl} without the historic PSXFolderAcl vs
 * PSXObjectAcl ERROR path leaving {@code m_folderAcl} null.
 */
public class PSFolderEntryUpdateFolderTest {

  @Test
  public void updateFolderCachesFolderAclFromObjectAcl() {
    PSFolder folder =
        new PSFolder("Assets", 1001, 10, PSObjectPermissions.ACCESS_WRITE, "root assets");
    PSObjectAcl acl = new PSObjectAcl();
    acl.add(
        new PSObjectAclEntry(
            PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL,
            PSObjectAclEntry.ACL_ENTRY_EVERYONE,
            PSObjectAclEntry.ACCESS_WRITE));
    folder.setAcl(acl);

    // Package ctor calls updateFolder — same conversion path as cache refresh after setDefaultPermissions
    PSFolderEntry entry = new PSFolderEntry(folder);

    PSFolderAcl cached = entry.getFolderAcl();
    assertNotNull(cached, "folder ACL must be cached after updateFolder");
    assertEquals(1001, cached.getContentId());
    assertEquals(10, cached.getCommunityId());
    assertEquals(1, cached.size());
    assertNotNull(
        cached.getAclEntry(
            PSObjectAclEntry.ACL_ENTRY_EVERYONE, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL));
    assertEquals(
        PSObjectAclEntry.ACCESS_WRITE,
        cached
            .getAclEntry(
                PSObjectAclEntry.ACL_ENTRY_EVERYONE, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL)
            .getPermissions());
    assertEquals("Assets", entry.getName());
  }

  @Test
  public void updateFolderWithEmptyAclStillCachesEmptyFolderAcl() {
    PSFolder folder =
        new PSFolder("EmptyAcl", 55, -1, PSObjectPermissions.ACCESS_READ, "no entries");
    // PSFolder starts with an empty PSObjectAcl
    PSFolderEntry entry = new PSFolderEntry(folder);

    PSFolderAcl cached = entry.getFolderAcl();
    assertNotNull(cached);
    assertEquals(55, cached.getContentId());
    assertEquals(-1, cached.getCommunityId());
    assertEquals(0, cached.size());
  }
}
