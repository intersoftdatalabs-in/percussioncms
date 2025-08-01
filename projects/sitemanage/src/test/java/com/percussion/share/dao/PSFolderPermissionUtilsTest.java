// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSObjectAcl;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSFolderPermission.Principal;
import com.percussion.pathmanagement.data.PSFolderPermission.PrincipalType;

/**
 * Tests for {@link PSFolderPermissionUtils}.
 * Sunny Sal: "Folder permission utils, Java 11, and permission ka hero!"
 */
public class PSFolderPermissionUtilsTest {

    @Test
    void testGetPermission() {
        var folder = createFolder(PSFolderPermission.Access.ADMIN, "admin");
        var permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(PSFolderPermission.Access.ADMIN, permission.getAccessLevel());
        assertEquals(1, permission.getAdminPrincipals().size());
        assertEquals("admin", permission.getAdminPrincipals().get(0).getName());
        assertNull(permission.getReadPrincipals());
        assertNull(permission.getWritePrincipals());

        folder = createFolder(PSFolderPermission.Access.WRITE, "writer");
        permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(PSFolderPermission.Access.WRITE, permission.getAccessLevel());
        assertEquals(1, permission.getWritePrincipals().size());
        assertEquals("writer", permission.getWritePrincipals().get(0).getName());
        assertNull(permission.getAdminPrincipals());
        assertNull(permission.getReadPrincipals());

        folder = createFolder(PSFolderPermission.Access.READ, "reader");
        permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(PSFolderPermission.Access.READ, permission.getAccessLevel());
        assertEquals(1, permission.getReadPrincipals().size());
        assertEquals("reader", permission.getReadPrincipals().get(0).getName());
        assertNull(permission.getAdminPrincipals());
        assertNull(permission.getWritePrincipals());
    }

    @Test
    void testSetPermission() {
        var folder = createFolder(PSFolderPermission.Access.ADMIN, "admin");
        PSFolderPermissionUtils.setFolderPermission(folder, PSFolderPermission.Access.ADMIN);
        var permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(PSFolderPermission.Access.ADMIN, permission.getAccessLevel());

        PSFolderPermissionUtils.setFolderPermission(folder, PSFolderPermission.Access.WRITE);
        permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(PSFolderPermission.Access.WRITE, permission.getAccessLevel());

        PSFolderPermissionUtils.setFolderPermission(folder, PSFolderPermission.Access.READ);
        permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(PSFolderPermission.Access.READ, permission.getAccessLevel());
    }

    @Test
    void testSetFolderPermission() {
        var folder = createFolder(PSFolderPermission.Access.ADMIN, "admin");

        var perm = new PSFolderPermission();
        perm.setAccessLevel(PSFolderPermission.Access.ADMIN);
        var p = new Principal();
        p.setName("admin2");
        p.setType(PrincipalType.USER);
        var principals = new ArrayList<Principal>();
        principals.add(p);
        perm.setAdminPrincipals(principals);

        PSFolderPermissionUtils.setFolderPermission(folder, perm);
        var permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(perm, permission);

        perm = new PSFolderPermission();
        perm.setAccessLevel(PSFolderPermission.Access.WRITE);
        p.setName("writer");
        principals.clear();
        principals.add(p);
        perm.setWritePrincipals(principals);

        PSFolderPermissionUtils.setFolderPermission(folder, perm);
        permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(perm, permission);

        perm = new PSFolderPermission();
        perm.setAccessLevel(PSFolderPermission.Access.READ);
        p.setName("reader");
        principals.clear();
        principals.add(p);
        perm.setReadPrincipals(principals);

        PSFolderPermissionUtils.setFolderPermission(folder, perm);
        permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(perm, permission);

        perm = new PSFolderPermission();
        PSFolderPermissionUtils.setFolderPermission(folder, perm);
        permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertEquals(perm, permission);

        perm = new PSFolderPermission();
        perm.setAccessLevel(PSFolderPermission.Access.READ);
        var adminPrincipals = new ArrayList<Principal>();
        var writePrincipals = new ArrayList<Principal>();
        var readPrincipals = new ArrayList<Principal>();
        var adminUser = new Principal();
        adminUser.setName("user1");
        adminUser.setType(PrincipalType.USER);
        adminPrincipals.add(adminUser);
        perm.setAdminPrincipals(adminPrincipals);
        var writeUser = new Principal();
        writeUser.setName("user1");
        writeUser.setType(PrincipalType.USER);
        writePrincipals.add(writeUser);
        perm.setWritePrincipals(writePrincipals);
        var readUser = new Principal();
        readUser.setName("user1");
        readUser.setType(PrincipalType.USER);
        readPrincipals.add(readUser);
        perm.setReadPrincipals(readPrincipals);

        PSFolderPermissionUtils.setFolderPermission(folder, perm);
        permission = PSFolderPermissionUtils.getFolderPermission(folder);
        assertNotEquals(perm, permission);
        assertEquals(1, permission.getAdminPrincipals().size());
        assertEquals(adminUser, permission.getAdminPrincipals().get(0));
        assertNull(permission.getWritePrincipals());
        assertNull(permission.getReadPrincipals());
    }

    @Test
    void testGetUserAcl() {
        var folder = createFolder(PSFolderPermission.Access.ADMIN, PSFolderPermission.Access.WRITE, "user1");
        var acl = PSFolderPermissionUtils.getUserAcl(folder, "user1", Collections.singletonList("")).getFirst();
        assertEquals(PSFolderPermission.Access.WRITE, acl);

        acl = PSFolderPermissionUtils.getUserAcl(folder, "user2", Collections.singletonList("Admin")).getFirst();
        assertEquals(PSFolderPermission.Access.ADMIN, acl);
    }

    private PSFolder createFolder(PSFolderPermission.Access accessLevel, String user) {
        return createFolder(accessLevel, accessLevel, user);
    }

    private PSFolder createFolder(PSFolderPermission.Access virtualAccess, PSFolderPermission.Access userAccess, String userName) {
        var folder = new PSFolder("folder1", -1, 0, "Test folder");
        var acl = new PSObjectAcl();

        var permission = new PSFolderPermission();
        permission.setAccessLevel(virtualAccess);
        var principle = new PSFolderPermission.Principal();
        principle.setType(PSFolderPermission.PrincipalType.USER);
        principle.setName(userName);
        if (userAccess == PSFolderPermission.Access.ADMIN)
            permission.setAdminPrincipals(Collections.singletonList(principle));
        else if (userAccess == PSFolderPermission.Access.WRITE)
            permission.setWritePrincipals(Collections.singletonList(principle));
        else
            permission.setReadPrincipals(Collections.singletonList(principle));

        PSFolderPermissionUtils.setFolderPermission(folder, permission);

        return folder;
    }
}
