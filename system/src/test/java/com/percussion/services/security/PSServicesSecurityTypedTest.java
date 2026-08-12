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
package com.percussion.services.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.shim.acl.Group;
import com.percussion.security.shim.acl.Permission;
import com.percussion.services.security.data.PSAclEntryImpl;
import com.percussion.services.security.loginmods.data.PSGroup;
import com.percussion.services.security.loginmods.data.PSPrincipal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for typed {@code com.percussion.services.security} collections (issue
 * #3181).
 */
class PSServicesSecurityTypedTest {

  @Test
  @DisplayName("findOrCreateGroup finds existing role group and rejects null collection")
  void findOrCreateGroupTyped() {
    Set<Principal> principals = new HashSet<>();
    Group existing = new PSGroup(PSJaasUtils.ROLE_GROUP_NAME);
    principals.add(existing);

    Group found = PSJaasUtils.findOrCreateGroup(principals, PSJaasUtils.ROLE_GROUP_NAME);
    assertSame(existing, found);

    assertThrows(
        IllegalArgumentException.class,
        () -> PSJaasUtils.findOrCreateGroup(null, PSJaasUtils.ROLE_GROUP_NAME));
  }

  @Test
  @DisplayName("findOrCreateGroup creates and appends missing group")
  void findOrCreateGroupCreates() {
    List<Principal> principals = new ArrayList<>();
    Group created = PSJaasUtils.findOrCreateGroup(principals, "CallerPrincipal");
    assertNotNull(created);
    assertEquals("CallerPrincipal", created.getName());
    assertEquals(1, principals.size());
    assertTrue(principals.contains(created));
  }

  @Test
  @DisplayName("findFirstPSPrincipal returns first PSPrincipal or null")
  void findFirstPSPrincipalTyped() {
    List<Principal> principals = new ArrayList<>();
    principals.add(new PSGroup("Roles"));
    PSPrincipal user = new PSPrincipal("admin1");
    principals.add(user);
    principals.add(new PSPrincipal("other"));

    assertSame(user, PSJaasUtils.findFirstPSPrincipal(principals));
    assertNull(PSJaasUtils.findFirstPSPrincipal(List.of(new PSGroup("Roles"))));
    assertThrows(IllegalArgumentException.class, () -> PSJaasUtils.findFirstPSPrincipal(null));
  }

  @Test
  @DisplayName("PSAclEntryImpl.permissions enumerates typed Permission values")
  void aclEntryPermissionsTyped() {
    PSAclEntryImpl entry = new PSAclEntryImpl();
    entry.setName("tester");
    entry.setType(com.percussion.security.IPSTypedPrincipal.PrincipalTypes.USER);
    entry.addPermission(PSPermissions.READ);
    entry.addPermission(PSPermissions.UPDATE);

    Enumeration<Permission> perms = entry.permissions();
    Set<Permission> found = new HashSet<>();
    while (perms.hasMoreElements()) {
      found.add(perms.nextElement());
    }
    assertTrue(found.contains(PSPermissions.READ));
    assertTrue(found.contains(PSPermissions.UPDATE));
    assertFalse(found.isEmpty());
  }

  @Test
  @DisplayName("PSAclEntryImpl permissions empty when no access levels")
  void aclEntryPermissionsEmpty() {
    PSAclEntryImpl entry = new PSAclEntryImpl();
    entry.setName("empty");
    Enumeration<Permission> perms = entry.permissions();
    assertFalse(perms.hasMoreElements());
  }
}
