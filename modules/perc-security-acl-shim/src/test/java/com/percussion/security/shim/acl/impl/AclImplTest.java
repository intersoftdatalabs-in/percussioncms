/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.security.shim.acl.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.shim.acl.AclEntry;
import com.percussion.security.shim.acl.LastOwnerException;
import com.percussion.security.shim.acl.NotOwnerException;
import com.percussion.security.shim.acl.Permission;
import java.security.Principal;
import java.util.Set;
import javax.security.auth.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * JUnit 5 tests validating core ACL semantics: - Owner management with last-owner invariant -
 * Deny-overrides-allow permission checks - Subject principal matching
 */
class AclImplTest {

  private static final class SimplePrincipal implements Principal {
    private final String name;

    SimplePrincipal(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SimplePrincipal)) return false;
      return name.equals(((SimplePrincipal) o).name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  private enum SimplePermission implements Permission {
    READ,
    WRITE
  }

  private Subject subjectFor(Principal... principals) {
    Subject s = new Subject();
    for (Principal p : principals) {
      s.getPrincipals().add(p);
    }
    return s;
  }

  @Test
  void testAddAndDeleteOwner_enforcesOwnerChecks() throws Exception {
    Principal owner1 = new SimplePrincipal("owner1");
    Principal owner2 = new SimplePrincipal("owner2");

    AclImpl acl = new AclImpl(owner1, "acl");

    // Non-owner cannot add owner
    Executable addByNonOwner = () -> acl.addOwner(owner2, owner2);
    assertThrows(NotOwnerException.class, addByNonOwner);

    // Owner can add another owner
    assertTrue(acl.addOwner(owner1, owner2));
    assertTrue(acl.isOwner(owner2));

    // Non-owner cannot remove owner
    // Use a true non-owner principal to verify NotOwnerException
    Principal notOwner = new SimplePrincipal("notOwner");
    Executable removeByNonOwner = () -> acl.deleteOwner(notOwner, owner1);
    assertThrows(NotOwnerException.class, removeByNonOwner);

    // Owner can remove other owner (not last)
    assertTrue(acl.deleteOwner(owner1, owner2));
    assertFalse(acl.isOwner(owner2));
  }

  @Test
  void testDeleteLastOwner_throwsLastOwnerException() throws Exception {
    Principal owner1 = new SimplePrincipal("owner1");
    AclImpl acl = new AclImpl(owner1, "acl");

    Executable removeLast = () -> acl.deleteOwner(owner1, owner1);
    assertThrows(LastOwnerException.class, removeLast);
  }

  @Test
  void testCheckPermission_positiveAllowsWhenNoDeny() throws Exception {
    Principal owner = new SimplePrincipal("owner");
    Principal user = new SimplePrincipal("user");
    Subject subject = subjectFor(user);

    AclImpl acl = new AclImpl(owner, "acl");

    AclEntry entry = new AclEntryImpl();
    assertTrue(entry.setPrincipal(user));
    assertTrue(entry.addPermission(SimplePermission.READ));

    assertTrue(acl.addEntry(owner, entry));

    assertTrue(acl.checkPermission(subject, SimplePermission.READ));
    assertFalse(acl.checkPermission(subject, SimplePermission.WRITE));
  }

  @Test
  void testCheckPermission_negativeDeniesEvenIfAllowPresent() throws Exception {
    Principal owner = new SimplePrincipal("owner");
    Principal user = new SimplePrincipal("user");
    Subject subject = subjectFor(user);

    AclImpl acl = new AclImpl(owner, "acl");

    // Positive allow
    AclEntry allow = new AclEntryImpl();
    assertTrue(allow.setPrincipal(user));
    assertTrue(allow.addPermission(SimplePermission.READ));
    assertTrue(acl.addEntry(owner, allow));

    // Negative deny
    AclEntry deny = new AclEntryImpl();
    assertTrue(deny.setPrincipal(user));
    deny.setNegativePermissions();
    assertTrue(deny.addPermission(SimplePermission.READ));
    assertTrue(acl.addEntry(owner, deny));

    // Deny overrides allow
    assertFalse(acl.checkPermission(subject, SimplePermission.READ));
  }

  @Test
  void testCheckPermission_matchesSubjectPrincipals() throws Exception {
    Principal owner = new SimplePrincipal("owner");
    Principal user = new SimplePrincipal("user");
    Principal group = new SimplePrincipal("group");
    Subject subject = new Subject(true, Set.of(user, group), Set.of(), Set.of());

    AclImpl acl = new AclImpl(owner, "acl");

    // Entry assigned to 'group', not to 'user' directly
    AclEntry groupEntry = new AclEntryImpl();
    assertTrue(groupEntry.setPrincipal(group));
    assertTrue(groupEntry.addPermission(SimplePermission.WRITE));
    assertTrue(acl.addEntry(owner, groupEntry));

    assertTrue(acl.checkPermission(subject, SimplePermission.WRITE));
    assertFalse(acl.checkPermission(subject, SimplePermission.READ));
  }
}
