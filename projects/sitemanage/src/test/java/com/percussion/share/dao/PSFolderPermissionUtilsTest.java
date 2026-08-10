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
package com.percussion.share.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSObjectAcl;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSFolderPermission.Access;
import com.percussion.pathmanagement.data.PSFolderPermission.Principal;
import com.percussion.pathmanagement.data.PSFolderPermission.PrincipalType;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSFolderPermissionUtils} null-safety paths used by Folder Security
 * open/save (#2749).
 */
class PSFolderPermissionUtilsTest {

  private static PSFolder newFolder() {
    // communityId -1 = all communities; permissions bit ignored for these unit tests
    return new PSFolder("Design", -1, PSObjectAclEntry.ACCESS_ADMIN, "test folder");
  }

  @Test
  void getFolderPermission_nullFolder_throwsValidatedObjectIsNull() {
    NullPointerException npe =
        assertThrows(
            NullPointerException.class, () -> PSFolderPermissionUtils.getFolderPermission(null));
    assertTrue(
        npe.getMessage() == null
            || npe.getMessage().contains("validated object is null")
            || npe.getMessage().contains("null"),
        "expected Validate.notNull-style message, got: " + npe.getMessage());
  }

  @Test
  void getFolderPermission_emptyAcl_defaultsAdminAndNeverNull() {
    PSFolder folder = newFolder();
    PSFolderPermission permission = PSFolderPermissionUtils.getFolderPermission(folder);
    assertNotNull(permission);
    assertEquals(Access.ADMIN, permission.getAccessLevel());
  }

  @Test
  void getFolderPermission_virtualRead_mapsAccessLevel() {
    PSFolder folder = newFolder();
    PSObjectAcl acl = folder.getAcl();
    acl.add(
        new PSObjectAclEntry(
            PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL,
            PSObjectAclEntry.ACL_ENTRY_EVERYONE,
            PSObjectAclEntry.ACCESS_READ));
    PSFolderPermission permission = PSFolderPermissionUtils.getFolderPermission(folder);
    assertEquals(Access.READ, permission.getAccessLevel());
  }

  @Test
  void getFolderPermission_userAdminPrincipal_listed() {
    PSFolder folder = newFolder();
    folder
        .getAcl()
        .add(
            new PSObjectAclEntry(
                PSObjectAclEntry.ACL_ENTRY_TYPE_USER,
                "EditorUser",
                PSFolderPermissionUtils.ADMIN_ACCESS));
    PSFolderPermission permission = PSFolderPermissionUtils.getFolderPermission(folder);
    assertNotNull(permission.getAdminPrincipals());
    assertEquals(1, permission.getAdminPrincipals().size());
    assertEquals("EditorUser", permission.getAdminPrincipals().get(0).getName());
  }

  @Test
  void setFolderPermission_nullAccessLevel_defaultsAdmin() {
    PSFolder folder = newFolder();
    PSFolderPermission perm = new PSFolderPermission();
    perm.setAccessLevel(null);
    Principal p = new Principal();
    p.setName("Contributor");
    p.setType(PrincipalType.USER);
    perm.setWritePrincipals(Collections.singletonList(p));

    PSFolderPermissionUtils.setFolderPermission(folder, perm);

    PSFolderPermission reread = PSFolderPermissionUtils.getFolderPermission(folder);
    assertEquals(Access.ADMIN, reread.getAccessLevel());
    assertNotNull(reread.getWritePrincipals());
    assertEquals("Contributor", reread.getWritePrincipals().get(0).getName());
  }

  @Test
  void setFolderPermission_nullPerm_throws() {
    PSFolder folder = newFolder();
    PSFolderPermission nullPerm = null;
    assertThrows(
        NullPointerException.class,
        () -> PSFolderPermissionUtils.setFolderPermission(folder, nullPerm));
  }

  @Test
  void setFolderPermission_accessOnly_clearsUserEntries() {
    PSFolder folder = newFolder();
    folder
        .getAcl()
        .add(
            new PSObjectAclEntry(
                PSObjectAclEntry.ACL_ENTRY_TYPE_USER,
                "Doomed",
                PSFolderPermissionUtils.WRITE_ACCESS));
    PSFolderPermissionUtils.setFolderPermission(folder, Access.READ);
    PSFolderPermission reread = PSFolderPermissionUtils.getFolderPermission(folder);
    assertEquals(Access.READ, reread.getAccessLevel());
    assertNull(reread.getWritePrincipals());
    assertNull(reread.getAdminPrincipals());
  }
}
