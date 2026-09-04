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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.Guid;
import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityRole;
import com.percussion.rest.communities.CommunityRoleList;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * SE-02 community↔role assign/unassign via {@link CommunityAdaptor#updateCommunityRoles} (full-set
 * replace) and {@link CommunityAdaptor#listAvailableRoles}.
 */
@Tag("UnitTest")
class CommunityAdaptorRolesTest {

  private IPSSecurityDesignWs securityDesignWs;
  private CommunityAdaptor adaptor;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    securityDesignWs = mock(IPSSecurityDesignWs.class);
    adaptor = new CommunityAdaptor();
    Field field = CommunityAdaptor.class.getDeclaredField("securityDesignWs");
    field.setAccessible(true);
    field.set(adaptor, securityDesignWs);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void listAvailableRoles_sortedByName() {
    IPSGuid editorGuid = guid(PSTypeEnum.ROLE, 102);
    IPSGuid adminGuid = guid(PSTypeEnum.ROLE, 101);
    IPSCatalogSummary editor = roleSummary("Editor", editorGuid);
    IPSCatalogSummary admin = roleSummary("Admin", adminGuid);
    List<IPSCatalogSummary> roles = List.of(editor, admin);
    when(securityDesignWs.findRoles(isNull())).thenReturn(roles);

    CommunityRoleList out = adaptor.listAvailableRoles();
    assertEquals(2, out.size());
    assertEquals("Admin", out.get(0).getRoleName());
    assertEquals(101L, out.get(0).getRoleId());
    assertNotNull(out.get(0).getRoleGuid());
    assertEquals("Editor", out.get(1).getRoleName());
  }

  @Test
  void listAvailableRoles_emptyCatalog() {
    when(securityDesignWs.findRoles(isNull())).thenReturn(List.of());
    assertTrue(adaptor.listAvailableRoles().isEmpty());
  }

  @Test
  void updateCommunityRoles_assignsRolesViaReplace() throws Exception {
    stubCommunityRoundTrip(List.of());

    CommunityRoleList body = new CommunityRoleList();
    body.add(roleBody(101, "Admin"));
    body.add(roleBody(102, "Editor"));

    Community updated = adaptor.updateCommunityRoles("Default", body);
    assertNotNull(updated);
    assertEquals("Default", updated.getName());

    // saveCommunities converts REST DTOs via ApiUtils.convertCommunity (real PSCommunity),
    // not the loadCommunities mock — assert role associations on that converted instance.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSCommunity>> saved = ArgumentCaptor.forClass(List.class);
    verify(securityDesignWs, times(1))
        .saveCommunities(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    PSCommunity persisted = saved.getValue().get(0);
    assertEquals(2, persisted.getRoleAssociations().size());
    assertEquals(
        List.of(101, 102),
        persisted.getRoleAssociations().stream().map(IPSGuid::getUUID).toList());
  }

  @Test
  void updateCommunityRoles_unassignsByReplacingWithEmpty() throws Exception {
    IPSGuid adminGuid = guid(PSTypeEnum.ROLE, 101);
    stubCommunityRoundTrip(List.of(adminGuid));

    Community updated = adaptor.updateCommunityRoles("Default", new CommunityRoleList());
    assertNotNull(updated);

    // Empty roleList → convertCommunity adds no associations on the saved PSCommunity.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSCommunity>> saved = ArgumentCaptor.forClass(List.class);
    verify(securityDesignWs)
        .saveCommunities(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertTrue(saved.getValue().get(0).getRoleAssociations().isEmpty());
  }

  @Test
  void updateCommunityRoles_synthesizesRoleGuidFromRoleId() throws Exception {
    stubCommunityRoundTrip(List.of());

    CommunityRole onlyId = new CommunityRole();
    onlyId.setRoleId(101);
    onlyId.setRoleName("Admin");
    CommunityRoleList body = new CommunityRoleList();
    body.add(onlyId);

    assertNotNull(adaptor.updateCommunityRoles("Default", body));
    assertNotNull(onlyId.getRoleGuid());
    assertEquals(PSTypeEnum.ROLE.getOrdinal(), onlyId.getRoleGuid().getType());
    assertEquals(101, onlyId.getRoleGuid().getUuid());
  }

  @Test
  void updateCommunityRoles_missingRoleIdentityIsIllegalArgument() {
    stubFindCommunitySummary();
    CommunityRole blank = new CommunityRole();
    blank.setRoleName("NoId");
    CommunityRoleList body = new CommunityRoleList();
    body.add(blank);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.updateCommunityRoles("Default", body));
    String msg = ex.getMessage().toLowerCase();
    assertTrue(msg.contains("roleguid") || msg.contains("roleid"), msg);
  }

  @Test
  void updateCommunityRoles_unknownCommunityReturnsNull() {
    when(securityDesignWs.findCommunities(eq("missing"))).thenReturn(List.of());
    assertNull(adaptor.updateCommunityRoles("missing", new CommunityRoleList()));
  }

  @Test
  void updateCommunityRoles_blankIdIsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.updateCommunityRoles("  ", null));
  }

  @Test
  void ensureRoleIdentity_rejectsBlank() {
    assertThrows(
        IllegalArgumentException.class, () -> CommunityAdaptor.ensureRoleIdentity(new CommunityRole()));
  }

  @Test
  void ensureRoleIdentity_overflowRoleIdThrows() {
    CommunityRole r = new CommunityRole();
    r.setRoleId(Integer.MAX_VALUE + 1L);
    assertThrows(ArithmeticException.class, () -> CommunityAdaptor.ensureRoleIdentity(r));
  }

  @Test
  void ensureRoleIdentity_normalizesTypeWhenOnlyStringValuePresent() {
    CommunityRole r = new CommunityRole();
    Guid g = new Guid();
    g.setStringValue("0-0-101");
    g.setType((short) 0);
    g.setUuid(0);
    r.setRoleGuid(g);
    CommunityAdaptor.ensureRoleIdentity(r);
    assertEquals(PSTypeEnum.ROLE.getOrdinal(), r.getRoleGuid().getType());
  }

  private void stubCommunityRoundTrip(List<IPSGuid> currentRoles) throws Exception {
    stubFindCommunitySummary();
    PSCommunity detail = community("Default", 10, currentRoles);
    IPSCatalogSummary adminRole = roleSummary("Admin", guid(PSTypeEnum.ROLE, 101));
    when(securityDesignWs.loadCommunities(anyList(), anyBoolean(), anyBoolean(), any(), any()))
        .thenReturn(List.of(detail));
    when(securityDesignWs.findRoles(isNull())).thenReturn(List.of(adminRole));
  }

  private void stubFindCommunitySummary() {
    IPSGuid communityGuid = guid(PSTypeEnum.COMMUNITY_DEF, 10);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getName()).thenReturn("Default");
    when(sum.getDescription()).thenReturn("Default Community");
    when(sum.getLabel()).thenReturn("Default");
    when(sum.getGUID()).thenReturn(communityGuid);
    List<IPSCatalogSummary> sums = List.of(sum);
    when(securityDesignWs.findCommunities(eq("Default"))).thenReturn(sums);
  }

  private static PSCommunity community(String name, long id, List<IPSGuid> roles) {
    PSCommunity c = mock(PSCommunity.class);
    IPSGuid g = guid(PSTypeEnum.COMMUNITY_DEF, id);
    when(c.getId()).thenReturn(id);
    when(c.getName()).thenReturn(name);
    when(c.getDescription()).thenReturn(name + " desc");
    when(c.getLabel()).thenReturn(name);
    when(c.getGUID()).thenReturn(g);
    when(c.getRoleAssociations()).thenReturn(roles);
    return c;
  }

  private static CommunityRole roleBody(long roleId, String name) {
    CommunityRole r = new CommunityRole();
    r.setRoleId(roleId);
    r.setRoleName(name);
    Guid g = new Guid();
    g.setHostId(0);
    g.setType(PSTypeEnum.ROLE.getOrdinal());
    g.setUuid((int) roleId);
    g.setLongValue(roleId);
    r.setRoleGuid(g);
    return r;
  }

  private static IPSCatalogSummary roleSummary(String name, IPSGuid guid) {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getName()).thenReturn(name);
    when(sum.getGUID()).thenReturn(guid);
    return sum;
  }

  private static IPSGuid guid(PSTypeEnum type, long uuid) {
    IPSGuid g = mock(IPSGuid.class);
    when(g.getHostId()).thenReturn(0L);
    when(g.getType()).thenReturn(type.getOrdinal());
    when(g.getUUID()).thenReturn((int) uuid);
    when(g.longValue()).thenReturn(uuid);
    when(g.toString()).thenReturn("0-" + type.getOrdinal() + "-" + uuid);
    when(g.toStringUntyped()).thenReturn("0-" + uuid);
    return g;
  }
}
