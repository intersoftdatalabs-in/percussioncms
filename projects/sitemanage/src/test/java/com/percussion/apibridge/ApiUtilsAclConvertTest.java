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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.Guid;
import com.percussion.rest.Permissions;
import com.percussion.rest.acls.Acl;
import com.percussion.rest.acls.AclEntry;
import com.percussion.rest.acls.AclEntryList;
import com.percussion.rest.acls.AclList;
import com.percussion.rest.acls.TypedPrincipal;
import com.percussion.rest.acls.UserAccessLevel;
import com.percussion.rest.acls.UserAccessLevelList;
import com.percussion.security.IPSTypedPrincipal;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.PSTypedPrincipal;
import com.percussion.services.security.data.PSAccessLevelImpl;
import com.percussion.services.security.IPSAcl;
import com.percussion.services.security.data.PSAclEntryImpl;
import com.percussion.services.security.data.PSAclImpl;
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

  @Test
  public void applyAclObjectIdentityDerivesTypeAndIdFromObjectGuidString() {
    Acl acl = new Acl();
    acl.setName("By_Author ACL");
    Guid objectGuid = new Guid();
    objectGuid.setStringValue("0-31-5");
    acl.setObjectGuid(objectGuid);

    PSAclImpl pAcl = new PSAclImpl();
    ApiUtils.applyAclObjectIdentity(acl, pAcl);

    assertEquals(31, pAcl.getObjectType());
    assertEquals(5, pAcl.getObjectId());
    assertNotNull(pAcl.getObjectGuid());
  }

  @Test
  public void convertAclAcceptsGuidWithoutStringValue() {
    Acl acl = new Acl();
    acl.setId(7);
    acl.setName("x5");
    acl.setObjectType(31);
    acl.setObjectId(5);
    Guid guid = new Guid();
    guid.setHostId(6622135);
    guid.setLongValue(7281114506216867000L);
    guid.setType((short) 17);
    guid.setUuid(1418);
    acl.setGuid(guid);
    Guid objectGuid = new Guid();
    objectGuid.setHostId(0);
    objectGuid.setLongValue(5);
    objectGuid.setType((short) 31);
    objectGuid.setUuid(5);
    acl.setObjectGuid(objectGuid);

    PSAclImpl converted = ApiUtils.convertAcl(acl);
    assertEquals("x5", converted.getName());
    assertEquals(31, converted.getObjectType());
    assertEquals(5, converted.getObjectId());
    assertNotNull(converted.getGUID());
    assertEquals(17, converted.getGUID().getType());
    assertEquals(1418, converted.getGUID().getUUID());
  }

  @Test
  public void convertAclsSavePreservesObjectIdentityFromGuid() {
    Acl acl = new Acl();
    acl.setId(7);
    acl.setName("By_Author ACL");
    Guid objectGuid = new Guid();
    objectGuid.setStringValue("0-31-5");
    acl.setObjectGuid(objectGuid);
    AclList list = new AclList();
    list.add(acl);

    java.util.List<IPSAcl> converted = ApiUtils.convertAcls(list);
    assertEquals(1, converted.size());
    PSAclImpl pAcl = (PSAclImpl) converted.get(0);
    assertEquals(31, pAcl.getObjectType());
    assertEquals(5, pAcl.getObjectId());
  }

  @Test
  public void convertAclLoadIncludesObjectType() {
    PSAclImpl pAcl = new PSAclImpl();
    pAcl.setName("By_Author ACL");
    pAcl.setId(7);
    pAcl.setObjectType(31);
    pAcl.setObjectId(5);

    Acl rest = ApiUtils.convertAcl(pAcl);
    assertNotNull(rest);
    assertEquals(31, rest.getObjectType());
    assertEquals(5, rest.getObjectId());
  }

  @Test
  public void convertAclLoadIncludesDefaultAnyCommunityAndUser() {
    PSAclImpl pAcl = new PSAclImpl();
    pAcl.setId(7);
    pAcl.setName("By_Author ACL");
    pAcl.setObjectType(31);
    pAcl.setObjectId(5);
    addEntry(pAcl, "Default", IPSTypedPrincipal.PrincipalTypes.USER, PSPermissions.READ);
    addEntry(
        pAcl, "AnyCommunity", IPSTypedPrincipal.PrincipalTypes.COMMUNITY, PSPermissions.RUNTIME_VISIBLE);
    addEntry(pAcl, "Admin", IPSTypedPrincipal.PrincipalTypes.USER, PSPermissions.OWNER);

    Acl rest = ApiUtils.convertAcl(pAcl);
    assertNotNull(rest.getAclEntries().orElse(null));
    assertEquals(3, rest.getAclEntries().get().size());
    var names =
        rest.getAclEntries().get().stream()
            .map(e -> e.getName().orElse(""))
            .collect(Collectors.toSet());
    assertTrue(names.contains("Default"));
    assertTrue(names.contains("AnyCommunity"));
    assertTrue(names.contains("Admin"));
    assertEquals(31, rest.getObjectType());
    assertEquals(5, rest.getObjectId());
    assertNotNull(rest.getObjectGuid().orElse(null));
  }

  @Test
  public void convertAclsSaveKeepsAclIdGuidAndEntries() {
    Acl acl = new Acl();
    acl.setId(7);
    acl.setName("By_Author ACL");
    Guid objectGuid = new Guid();
    objectGuid.setStringValue("0-31-5");
    acl.setObjectGuid(objectGuid);
    AclEntryList entries = new AclEntryList();
    entries.add(restEntry("Default", IPSTypedPrincipal.PrincipalTypes.USER, Permissions.READ));
    entries.add(
        restEntry(
            "AnyCommunity",
            IPSTypedPrincipal.PrincipalTypes.COMMUNITY,
            Permissions.RUNTIME_VISIBLE));
    entries.add(restEntry("Admin", IPSTypedPrincipal.PrincipalTypes.USER, Permissions.OWNER));
    acl.setAclEntries(entries);
    AclList list = new AclList();
    list.add(acl);

    java.util.List<IPSAcl> converted = ApiUtils.convertAcls(list);
    PSAclImpl pAcl = (PSAclImpl) converted.get(0);
    assertEquals(7, pAcl.getId());
    assertEquals(31, pAcl.getObjectType());
    assertEquals(5, pAcl.getObjectId());
    assertNotNull(pAcl.getGUID());
    var names =
        pAcl.getEntries().stream()
            .map(e -> ((PSAclEntryImpl) e).getName())
            .collect(Collectors.toSet());
    assertEquals(java.util.Set.of("Default", "AnyCommunity", "Admin"), names);
  }

  private static void addEntry(
      PSAclImpl acl,
      String name,
      IPSTypedPrincipal.PrincipalTypes type,
      PSPermissions permission) {
    PSAclEntryImpl entry = new PSAclEntryImpl(new PSTypedPrincipal(name, type));
    entry.addPermission(new PSAccessLevelImpl(entry, permission));
    acl.addEntry(entry);
  }

  private static AclEntry restEntry(
      String name, IPSTypedPrincipal.PrincipalTypes type, Permissions perm) {
    AclEntry entry = new AclEntry();
    entry.setName(name);
    entry.setType(new TypedPrincipal(name, type));
    UserAccessLevelList levels = new UserAccessLevelList();
    UserAccessLevel level = new UserAccessLevel();
    level.setPermission(perm);
    levels.add(level);
    entry.setPermissions(levels);
    return entry;
  }
}
