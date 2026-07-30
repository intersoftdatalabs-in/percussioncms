/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.Permissions;
import com.percussion.rest.acls.AclEntry;
import com.percussion.rest.acls.AclEntryList;
import com.percussion.rest.acls.TypedPrincipal;
import com.percussion.rest.acls.UserAccessLevel;
import com.percussion.rest.acls.UserAccessLevelList;
import com.percussion.security.IPSTypedPrincipal;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.data.PSAccessLevelImpl;
import com.percussion.services.security.data.PSAclEntryImpl;
import java.util.Collection;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for {@link ApiUtils} ACL entry/permission conversion (P0.5e).
 *
 * <p>Covers null-safe permission filtering so malformed {@link UserAccessLevel} values do not break
 * ACL save round-trips.
 */
@Tag("UnitTest")
public class ApiUtilsAclConvertTest {

  @Test
  public void convertAclEntriesNullListIsEmpty() {
    Collection<PSAclEntryImpl> out = ApiUtils.convertAclEntries((AclEntryList) null);
    assertNotNull(out);
    assertTrue(out.isEmpty());
  }

  @Test
  public void convertAclEntriesSkipsNullPermissionLevels() {
    AclEntry entry = new AclEntry();
    entry.setId(10);
    entry.setName("Default");
    entry.setAclId(1);
    TypedPrincipal tp = new TypedPrincipal("Default", IPSTypedPrincipal.PrincipalTypes.ROLE);
    entry.setType(tp);

    UserAccessLevelList levels = new UserAccessLevelList();
    UserAccessLevel read = new UserAccessLevel();
    read.setId(100);
    read.setPermission(Permissions.READ);
    levels.add(read);

    // Malformed / empty permission — must not be converted or throw
    UserAccessLevel empty = new UserAccessLevel();
    empty.setId(101);
    empty.setPermission(null);
    levels.add(empty);

    UserAccessLevel update = new UserAccessLevel();
    update.setId(102);
    update.setPermission(Permissions.UPDATE);
    levels.add(update);

    entry.setPermissions(levels);

    AclEntryList list = new AclEntryList();
    list.add(entry);

    Collection<PSAclEntryImpl> converted = ApiUtils.convertAclEntries(list);
    assertEquals(1, converted.size());
    PSAclEntryImpl pEntry = converted.iterator().next();
    assertEquals("Default", pEntry.getName());
    assertEquals(IPSTypedPrincipal.PrincipalTypes.ROLE, pEntry.getType());

    Collection<PSAccessLevelImpl> perms = pEntry.getPermissions();
    assertNotNull(perms);
    assertEquals(2, perms.size(), "null permission level must be filtered out");
    var names =
        perms.stream()
            .map(PSAccessLevelImpl::getPermission)
            .map(PSPermissions::name)
            .collect(Collectors.toSet());
    assertTrue(names.contains("READ"));
    assertTrue(names.contains("UPDATE"));
  }

  @Test
  public void convertPermissionsNullPermissionDoesNotThrow() {
    UserAccessLevel empty = new UserAccessLevel();
    empty.setId(55);
    empty.setPermission(null);

    PSAccessLevelImpl out = ApiUtils.convertPermissions(empty);
    assertNotNull(out);
    assertEquals(55, out.getId());
    // PSAccessLevelImpl defaults ordinal 0 → READ when permission was never set.
    // convertAclEntries must filter isPresent() so these are not persisted as READ.
    assertEquals(PSPermissions.READ, out.getPermission());
  }

  @Test
  public void convertPermissionsMapsKnownEnums() {
    UserAccessLevel ual = new UserAccessLevel();
    ual.setId(7);
    ual.setPermission(Permissions.OWNER);

    PSAccessLevelImpl out = ApiUtils.convertPermissions(ual);
    assertEquals(PSPermissions.OWNER, out.getPermission());
    assertEquals(7, out.getId());
  }

  @Test
  public void convertAclEntriesResolvesPrincipalTypeFromTypedPrincipal() {
    AclEntry entry = new AclEntry();
    entry.setId(11);
    entry.setName("Admin");
    TypedPrincipal tp = new TypedPrincipal("Admin", IPSTypedPrincipal.PrincipalTypes.ROLE);
    entry.setType(tp);
    UserAccessLevelList levels = new UserAccessLevelList();
    UserAccessLevel owner = new UserAccessLevel();
    owner.setPermission(Permissions.OWNER);
    levels.add(owner);
    entry.setPermissions(levels);

    AclEntryList list = new AclEntryList();
    list.add(entry);

    PSAclEntryImpl pEntry = ApiUtils.convertAclEntries(list).iterator().next();
    assertEquals(IPSTypedPrincipal.PrincipalTypes.ROLE, pEntry.getType());
    assertEquals("Admin", pEntry.getName());
  }
}
