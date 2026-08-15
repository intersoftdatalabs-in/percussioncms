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
package com.percussion.services.security.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.IPSTypedPrincipal.PrincipalTypes;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.PSTypedPrincipal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSAclEntryImpl#merge(PSAclEntryImpl)}.
 *
 * <p>Upgrade package install ({@code perc.responsiveTemplates}) failed after #3387 because {@code
 * merge} always attached the incoming {@link PSAccessLevelImpl} (stray {@code if (...);} ).
 * Hibernate then saw two representations of the same permission SYSID.
 */
@Tag("UnitTest")
class PSAclEntryImplMergeTest {

  private static final long SHARED_DELETE_SYSID = 8896224446637942734L;

  @Test
  void nullSourceThrows() {
    PSAclEntryImpl target = entry("Default", PrincipalTypes.USER, PSPermissions.OWNER);
    assertThrows(IllegalArgumentException.class, () -> target.merge(null));
  }

  @Test
  void samePermissionTypeIsNotDuplicated() {
    PSAclEntryImpl target = entry("Default", PrincipalTypes.USER, PSPermissions.DELETE);
    setPermissionId(target, PSPermissions.DELETE, SHARED_DELETE_SYSID);

    PSAclEntryImpl incoming = entry("Default", PrincipalTypes.USER, PSPermissions.DELETE);
    // Distinct incoming SYSID so HashSet.equals cannot hide an attach.
    setPermissionId(incoming, PSPermissions.DELETE, 99L);

    PSAccessLevelImpl incomingDelete = only(incoming, PSPermissions.DELETE);
    target.merge(incoming);

    assertEquals(1, target.getPsPermissions().size());
    PSAccessLevelImpl kept = only(target, PSPermissions.DELETE);
    assertEquals(SHARED_DELETE_SYSID, kept.getId());
    assertNotSame(incomingDelete, kept);
    assertTrue(
        target.getPsPermissions().stream().noneMatch(p -> p == incomingDelete),
        "must keep the target permission instance, not attach the incoming entity");
  }

  @Test
  void incomingPermissionEntityIsNotAttached() {
    PSAclEntryImpl target = entry("Default", PrincipalTypes.USER, PSPermissions.OWNER);
    PSAclEntryImpl incoming =
        entry("Default", PrincipalTypes.USER, PSPermissions.OWNER, PSPermissions.READ);

    PSAccessLevelImpl incomingRead = only(incoming, PSPermissions.READ);
    incomingRead.setId(42L);

    target.merge(incoming);

    assertEquals(
        Set.of(PSPermissions.OWNER, PSPermissions.READ),
        target.getPsPermissions().stream()
            .map(PSAccessLevelImpl::getPermission)
            .collect(Collectors.toSet()));
    PSAccessLevelImpl addedRead = only(target, PSPermissions.READ);
    assertNotSame(incomingRead, addedRead);
    assertEquals(0L, addedRead.getId(), "new permission row must not copy the incoming SYSID");
  }

  @Test
  void permissionTypesAbsentFromIncomingAreRemoved() {
    PSAclEntryImpl target =
        entry("Editor", PrincipalTypes.ROLE, PSPermissions.READ, PSPermissions.UPDATE);
    PSAclEntryImpl incoming = entry("Editor", PrincipalTypes.ROLE, PSPermissions.READ);

    target.merge(incoming);

    assertEquals(1, target.getPsPermissions().size());
    assertEquals(PSPermissions.READ, target.getPsPermissions().iterator().next().getPermission());
  }

  @Test
  void mergeDoesNotAttachSharedSysidTwiceOnAclImplMerge() {
    PSAclImpl existing = new PSAclImpl();
    existing.setId(7);
    existing.setName("By_Author ACL");
    PSAclEntryImpl existingDefault =
        entry("Default", PrincipalTypes.USER, PSPermissions.DELETE, PSPermissions.OWNER);
    setPermissionId(existingDefault, PSPermissions.DELETE, SHARED_DELETE_SYSID);
    existing.addEntry(existingDefault);

    PSAclImpl incoming = new PSAclImpl();
    incoming.setId(7);
    incoming.setName("temp");
    PSAclEntryImpl incomingDefault =
        entry("Default", PrincipalTypes.USER, PSPermissions.DELETE, PSPermissions.OWNER);
    setPermissionId(incomingDefault, PSPermissions.DELETE, 99L);
    incoming.addEntry(incomingDefault);

    existing.merge(incoming);

    PSAclEntryImpl mergedDefault =
        existing.getEntries().stream()
            .filter(e -> "Default".equals(((PSAclEntryImpl) e).getName()))
            .map(PSAclEntryImpl.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals(2, mergedDefault.getPsPermissions().size());
    long deleteCount =
        mergedDefault.getPsPermissions().stream()
            .filter(p -> p.getPermission() == PSPermissions.DELETE)
            .count();
    assertEquals(1, deleteCount);
    assertEquals(SHARED_DELETE_SYSID, only(mergedDefault, PSPermissions.DELETE).getId());
  }

  private static PSAclEntryImpl entry(
      String name, PrincipalTypes type, PSPermissions... permissions) {
    PSAclEntryImpl entry = new PSAclEntryImpl(new PSTypedPrincipal(name, type));
    for (PSPermissions permission : permissions) {
      entry.addPermission(new PSAccessLevelImpl(entry, permission));
    }
    return entry;
  }

  private static void setPermissionId(
      PSAclEntryImpl entry, PSPermissions permission, long id) {
    only(entry, permission).setId(id);
  }

  private static PSAccessLevelImpl only(PSAclEntryImpl entry, PSPermissions permission) {
    return entry.getPsPermissions().stream()
        .filter(p -> p.getPermission() == permission)
        .findFirst()
        .orElseThrow();
  }
}
