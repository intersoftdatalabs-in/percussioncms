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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.percussion.services.security.IPSAcl;
import com.percussion.services.security.IPSAclService;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.PSTypedPrincipal;
import com.percussion.services.security.data.PSAccessLevelImpl;
import com.percussion.services.security.data.PSAclEntryImpl;
import com.percussion.services.security.data.PSAclImpl;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link AclAdaptor#saveAcls} must merge onto the existing {@link PSAclImpl} identity
 * (objectGuid / SYSID) so Hibernate does not insert a duplicate {@code PK_PSX_ACLS} (#3384).
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class AclAdaptorSaveMergeTest {

  @Mock private IPSAclService aclService;

  private AclAdaptor adaptor;

  @BeforeEach
  void setUp() {
    adaptor = new AclAdaptor();
    adaptor.setAclService(aclService);
  }

  @Test
  void saveMergesEntriesOntoExistingIdentity() throws Exception {
    PSAclImpl existing = new PSAclImpl();
    existing.setId(7);
    existing.setVersion(2);
    existing.setName("By_Author ACL");
    existing.setObjectType(31);
    existing.setObjectId(5);
    PSAclEntryImpl owner =
        new PSAclEntryImpl(new PSTypedPrincipal("Admin", IPSTypedPrincipal.PrincipalTypes.USER));
    owner.addPermission(new PSAccessLevelImpl(owner, PSPermissions.OWNER));
    existing.addEntry(owner);

    when(aclService.loadAclForObjectModifiable(any())).thenReturn(existing);

    adaptor.saveAcls(displayFormatSaveList());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSAcl>> captor = ArgumentCaptor.forClass(List.class);
    verify(aclService).saveAcls(captor.capture());
    verify(aclService, never()).loadAclsModifiable(anyList());

    List<IPSAcl> saved = captor.getValue();
    assertEquals(1, saved.size());
    assertSame(existing, saved.get(0), "must persist the loaded identity, not a new PSAclImpl");
    PSAclImpl persisted = (PSAclImpl) saved.get(0);
    assertEquals(7, persisted.getId());
    assertEquals(Integer.valueOf(2), persisted.getVersion());
    assertEquals(31, persisted.getObjectType());
    assertEquals(5, persisted.getObjectId());
    Set<String> names =
        persisted.getEntries().stream()
            .map(e -> ((PSAclEntryImpl) e).getName())
            .collect(Collectors.toSet());
    assertEquals(Set.of("Default", "AnyCommunity", "Admin"), names);
  }

  @Test
  void saveInsertsWhenNoExistingAcl() throws Exception {
    when(aclService.loadAclForObjectModifiable(any())).thenReturn(null);
    when(aclService.loadAclsModifiable(anyList())).thenReturn(List.of());

    adaptor.saveAcls(displayFormatSaveList());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSAcl>> captor = ArgumentCaptor.forClass(List.class);
    verify(aclService).saveAcls(captor.capture());
    PSAclImpl inserted = (PSAclImpl) captor.getValue().get(0);
    assertEquals(7, inserted.getId());
    assertEquals(31, inserted.getObjectType());
    assertEquals(5, inserted.getObjectId());
    assertTrue(inserted.getEntries().stream().anyMatch(e -> "Default".equals(e.getName())));
  }

  private static AclList displayFormatSaveList() {
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
            "AnyCommunity", IPSTypedPrincipal.PrincipalTypes.COMMUNITY, Permissions.RUNTIME_VISIBLE));
    entries.add(restEntry("Admin", IPSTypedPrincipal.PrincipalTypes.USER, Permissions.OWNER));
    acl.setAclEntries(entries);
    AclList list = new AclList();
    list.add(acl);
    return list;
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
