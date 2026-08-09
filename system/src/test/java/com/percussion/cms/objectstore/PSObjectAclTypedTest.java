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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed {@link PSObjectAcl} as {@link
 * PSDbComponentSet}{@code <PSObjectAclEntry>} (#2442). Verifies typed iterators, lookup helpers, and
 * delete-list set ops without casts.
 */
public class PSObjectAclTypedTest {

  private static PSObjectAclEntry userEntry(String name, int permissions) {
    return new PSObjectAclEntry(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, name, permissions);
  }

  private static PSObjectAclEntry persistedUserEntry(int id, String name, int permissions) {
    return new PSObjectAclEntry(id, PSObjectAclEntry.ACL_ENTRY_TYPE_USER, name, permissions);
  }

  private static PSObjectAclEntry roleEntry(String name, int permissions) {
    return new PSObjectAclEntry(PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, name, permissions);
  }

  @Test
  public void typedIteratorYieldsAclEntriesWithoutCast() {
    PSObjectAcl acl = new PSObjectAcl();
    PSObjectAclEntry alice = userEntry("alice", PSObjectAclEntry.ACCESS_READ);
    PSObjectAclEntry editors = roleEntry("Editors", PSObjectAclEntry.ACCESS_WRITE);
    assertTrue(acl.add(alice));
    assertTrue(acl.add(editors));
    assertEquals(2, acl.size());

    int count = 0;
    Iterator<PSObjectAclEntry> it = acl.iterator();
    while (it.hasNext()) {
      PSObjectAclEntry entry = it.next();
      assertNotNull(entry.getName());
      assertTrue(
          entry.getType() == PSObjectAclEntry.ACL_ENTRY_TYPE_USER
              || entry.getType() == PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE);
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getAclEntryLookupIsCaseInsensitiveAndTyped() {
    PSObjectAcl acl = new PSObjectAcl();
    PSObjectAclEntry alice = userEntry("Alice", PSObjectAclEntry.ACCESS_ADMIN);
    acl.add(alice);
    acl.add(roleEntry("Editors", PSObjectAclEntry.ACCESS_READ));

    assertSame(alice, acl.getAclEntry("alice", PSObjectAclEntry.ACL_ENTRY_TYPE_USER));
    assertSame(alice, acl.getAclEntry("ALICE", PSObjectAclEntry.ACL_ENTRY_TYPE_USER));
    assertNull(acl.getAclEntry("alice", PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE));
    assertNull(acl.getAclEntry("missing", PSObjectAclEntry.ACL_ENTRY_TYPE_USER));
  }

  @Test
  public void addRejectsDuplicateUserRoleVirtualIdentity() {
    PSObjectAcl acl = new PSObjectAcl();
    PSObjectAclEntry first = userEntry("bob", PSObjectAclEntry.ACCESS_READ);
    PSObjectAclEntry duplicate = userEntry("bob", PSObjectAclEntry.ACCESS_WRITE);

    assertTrue(acl.add(first));
    assertFalse(acl.add(duplicate));
    assertEquals(1, acl.size());
    assertEquals(
        PSObjectAclEntry.ACCESS_READ,
        acl.getAclEntry("bob", PSObjectAclEntry.ACL_ENTRY_TYPE_USER).getPermissions());
  }

  @Test
  public void removePersistedEntryAppearsInTypedDeletedIterator() {
    PSObjectAcl acl = new PSObjectAcl();
    PSObjectAclEntry persisted =
        persistedUserEntry(42, "admin1", PSObjectAclEntry.ACCESS_ADMIN);
    PSObjectAclEntry other = userEntry("qa1", PSObjectAclEntry.ACCESS_READ);
    assertTrue(acl.add(persisted));
    assertTrue(acl.add(other));
    assertTrue(persisted.isPersisted());

    assertTrue(acl.remove(persisted));
    assertEquals(1, acl.size());
    assertNull(acl.getAclEntry("admin1", PSObjectAclEntry.ACL_ENTRY_TYPE_USER));
    assertEquals(1, acl.getDeleteCollection().size());

    Iterator<PSObjectAclEntry> deleted = acl.getDeletedAclEntries();
    assertTrue(deleted.hasNext());
    PSObjectAclEntry removed = deleted.next();
    assertEquals("admin1", removed.getName());
    assertEquals(PSObjectAclEntry.ACL_ENTRY_TYPE_USER, removed.getType());
    assertFalse(deleted.hasNext());
  }

  @Test
  public void removeNewEntryDoesNotEnterDeleteList() {
    PSObjectAcl acl = new PSObjectAcl();
    PSObjectAclEntry draft = userEntry("temp", PSObjectAclEntry.ACCESS_READ);
    assertTrue(acl.add(draft));
    assertFalse(draft.isPersisted());

    assertTrue(acl.remove(draft));
    assertEquals(0, acl.size());
    assertFalse(acl.getDeletedAclEntries().hasNext());
    assertEquals(0, acl.getDeleteCollection().size());
  }
}
