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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.security.IPSTypedPrincipal.PrincipalTypes;
import com.percussion.services.security.data.PSAccessLevelImpl;
import com.percussion.services.security.data.PSAclEntryImpl;
import com.percussion.services.security.data.PSAclImpl;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Merge-vs-insert identity for Display Format Object ACL save (#3384).
 *
 * <p>Hibernate {@code session.merge} on a new {@link PSAclImpl} that reuses an existing SYSID
 * inserts a duplicate {@code PK_PSX_ACLS}. The persist path must copy entries onto the loaded
 * identity and keep version / object identity.
 */
@Tag("UnitTest")
class PSAclPersistMergerTest {

  @Test
  void nullExistingReturnsIncomingForInsert() {
    PSAclImpl incoming = newAcl(0, null, 31, 5);
    addEntry(incoming, "Admin", PrincipalTypes.USER, PSPermissions.OWNER);

    assertSame(incoming, PSAclPersistMerger.mergeOntoExisting(null, incoming));
  }

  @Test
  void sameInstanceIsUnchanged() {
    PSAclImpl existing = newAcl(42, 3, 31, 5);
    assertSame(existing, PSAclPersistMerger.mergeOntoExisting(existing, existing));
  }

  @Test
  void mergeKeepsExistingSysidAndVersion() {
    PSAclImpl existing = newAcl(42, 3, 31, 5);
    addEntry(existing, "Admin", PrincipalTypes.USER, PSPermissions.OWNER);

    PSAclImpl incoming = newAcl(42, null, 31, 5);
    addEntry(incoming, "Default", PrincipalTypes.USER, PSPermissions.READ);
    addEntry(incoming, "AnyCommunity", PrincipalTypes.COMMUNITY, PSPermissions.RUNTIME_VISIBLE);
    addEntry(incoming, "Admin", PrincipalTypes.USER, PSPermissions.OWNER);

    PSAclImpl merged = PSAclPersistMerger.mergeOntoExisting(existing, incoming);

    assertSame(existing, merged, "must reuse Hibernate identity — not a new insert");
    assertNotSame(incoming, merged);
    assertEquals(42, merged.getId());
    assertEquals(Integer.valueOf(3), merged.getVersion());
    assertEquals(31, merged.getObjectType());
    assertEquals(5, merged.getObjectId());
    assertEquals(
        Set.of("Default", "AnyCommunity", "Admin"),
        merged.getEntries().stream().map(e -> ((PSAclEntryImpl) e).getName()).collect(Collectors.toSet()));
  }

  @Test
  void mergeDoesNotWipeObjectIdentityWhenIncomingOmitsIt() {
    PSAclImpl existing = newAcl(7, 1, 31, 5);
    addEntry(existing, "Admin", PrincipalTypes.USER, PSPermissions.OWNER);

    PSAclImpl incoming = newAcl(7, null, 0, 0);
    addEntry(incoming, "Default", PrincipalTypes.USER, PSPermissions.READ);
    addEntry(incoming, "AnyCommunity", PrincipalTypes.COMMUNITY, PSPermissions.RUNTIME_VISIBLE);
    addEntry(incoming, "Admin", PrincipalTypes.USER, PSPermissions.OWNER);

    PSAclImpl merged = PSAclPersistMerger.mergeOntoExisting(existing, incoming);
    assertEquals(31, merged.getObjectType());
    assertEquals(5, merged.getObjectId());
    assertTrue(merged.getObjectGuid().toString().contains("31"));
  }

  @Test
  void managedAclFlushesAndDoesNotMerge() {
    Session session = mock(Session.class);
    PSAclImpl acl = newAcl(42, 3, 31, 5);
    when(session.contains(acl)).thenReturn(true);

    IPSAcl result = PSAclPersistMerger.persistInSession(session, acl);

    assertSame(acl, result);
    verify(session).flush();
    verify(session, never()).merge(any());
  }

  @Test
  void detachedAclIsMerged() {
    Session session = mock(Session.class);
    PSAclImpl incoming = newAcl(42, null, 31, 5);
    PSAclImpl merged = newAcl(42, 1, 31, 5);
    when(session.contains(incoming)).thenReturn(false);
    when(session.merge(incoming)).thenReturn(merged);

    IPSAcl result = PSAclPersistMerger.persistInSession(session, incoming);

    assertSame(merged, result);
    verify(session).merge(incoming);
    verify(session, never()).flush();
  }

  @Test
  void sessionIdentityDoesNotLookUpSecondRepresentation() {
    Session session = mock(Session.class);
    PSAclImpl src = newAcl(42, 3, 31, 5);
    when(session.contains(src)).thenReturn(true);

    assertTrue(PSAclPersistMerger.isSessionIdentity(session, src));
    verify(session, never()).get(any(Class.class), any());
    verify(session, never()).get(any(String.class), any());
  }

  @Test
  void detachedIsNotSessionIdentity() {
    Session session = mock(Session.class);
    PSAclImpl src = newAcl(42, 3, 31, 5);
    when(session.contains(src)).thenReturn(false);

    assertFalse(PSAclPersistMerger.isSessionIdentity(session, src));
  }

  @Test
  void persistInSessionRejectsNulls() {
    Session session = mock(Session.class);
    PSAclImpl acl = newAcl(1, 0, 31, 5);
    assertThrows(IllegalArgumentException.class, () -> PSAclPersistMerger.persistInSession(null, acl));
    assertThrows(
        IllegalArgumentException.class, () -> PSAclPersistMerger.persistInSession(session, null));
  }

  private static PSAclImpl newAcl(long id, Integer version, int objectType, long objectId) {
    PSAclImpl acl = new PSAclImpl();
    if (id != 0) {
      acl.setId(id);
    }
    acl.setVersion(version);
    acl.setName("By_Author ACL");
    acl.setObjectType(objectType);
    if (objectId > 0) {
      acl.setObjectId(objectId);
    }
    return acl;
  }

  private static void addEntry(
      PSAclImpl acl, String name, PrincipalTypes type, PSPermissions permission) {
    PSAclEntryImpl entry = new PSAclEntryImpl(new PSTypedPrincipal(name, type));
    entry.addPermission(new PSAccessLevelImpl(entry, permission));
    acl.addEntry(entry);
  }
}
