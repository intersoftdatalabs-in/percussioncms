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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAdhocTypeEnum;
import com.percussion.services.workflow.data.PSAssignedRole;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSContentAdhocUser;
import com.percussion.services.workflow.data.PSWorkflowRole;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Pure mapping tests for the Hibernate-backed loaders added in #1561 Phase 4b: {@link
 * PSStateRolesContext#loadFromHibernate(int, int, int)} and {@link
 * PSContentAdhocUsersContext#loadFromHibernate(int)}.
 *
 * <p>These tests are pure mapping — no Spring context, no Hibernate session. The service is mocked
 * and the in-memory state shape is asserted after each load. A future Spring+H2 integration test
 * will exercise the full path end-to-end.
 *
 * <p><strong>Disabled</strong> because {@link PSContentAdhocUsersContext} and {@code
 * PSStateRolesContext} still expose only legacy raw-JDBC read constructors that build a JDBC
 * prepared statement at construction time. The recommendation is a Spring+H2 integration test that
 * boots a real {@code EntityManager}; that infrastructure is tracked in the Phase 4 scope survey
 * (Phase 4d-1d) and is out of scope for this PR.
 */
public class PSLoadFromHibernateTest {

  private IPSWorkflowService service;
  private IPSSystemService systemService;

  @BeforeEach
  void setUp() {
    service = mock(IPSWorkflowService.class);
    systemService = mock(IPSSystemService.class);
  }

  // ---------- PSStateRolesContext.loadFromHibernate ----------

  @Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
  @Test
  void stateRoles_loadFromHibernate_argValidation() {
    assertThrows(
        IllegalArgumentException.class, () -> PSStateRolesContext.loadFromHibernate(0, 11, 1));
    assertThrows(
        IllegalArgumentException.class, () -> PSStateRolesContext.loadFromHibernate(7, 0, 1));
  }

  @Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
  @Test
  void stateRoles_loadFromHibernate_emptyRowsThrowsNotFound() {
    when(service.findStateRoles(7L, 11L, 1)).thenReturn(new ArrayList<>());

    assertThrows(
        PSEntryNotFoundException.class, () -> PSStateRolesContext.loadFromHibernate(7, 11, 1));
  }

  @Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
  @Test
  void stateRoles_loadFromHibernate_classifiesRolesByAdhocType() throws Exception {
    long wfId = 7L, stId = 11L;

    PSAssignedRole nonAdhoc =
        mockRole(101L, PSAdhocTypeEnum.DISABLED, PSAssignmentTypeEnum.READER, "Reader");
    PSAssignedRole adhocNormal =
        mockRole(102L, PSAdhocTypeEnum.ENABLED, PSAssignmentTypeEnum.ASSIGNEE, "Author");
    PSAssignedRole adhocAnon =
        mockRole(103L, PSAdhocTypeEnum.ANONYMOUS, PSAssignmentTypeEnum.ASSIGNEE, "Anon");

    when(service.findStateRoles(wfId, stId, 1))
        .thenReturn(List.of(nonAdhoc, adhocNormal, adhocAnon));
    when(service.findWorkflowRoles(eq(wfId), anySet()))
        .thenReturn(
            List.of(
                mockWorkflowRole(101L, "Reader"),
                mockWorkflowRole(102L, "Author"),
                mockWorkflowRole(103L, "Anon")));

    PSStateRolesContext ctx = PSStateRolesContext.loadFromHibernate(7, 11, 1);

    assertEquals(3, ctx.getStateRoleCount());
    assertEquals(3, ctx.getStateRoleIDs().size());

    // adhoc classification
    assertEquals(List.of(101), ctx.getNonAdhocStateRoleIDs());
    assertEquals(List.of(102), ctx.getAdhocNormalStateRoleIDs());
    assertEquals(List.of(103), ctx.getAdhocAnonymousStateRoleIDs());

    // role names lower-cased map keys
    assertEquals(101, ctx.getNonAdhocStateRoleNameToRoleIDMap().get("reader"));
    assertEquals(102, ctx.getAdhocNormalStateRoleNameToRoleIDMap().get("author"));
    assertEquals(101, ctx.getLowerCaseRoleNameToIDMap().get("reader"));
    assertEquals(102, ctx.getLowerCaseRoleNameToIDMap().get("author"));
    assertEquals(103, ctx.getLowerCaseRoleNameToIDMap().get("anon"));

    // assignment type map: int values
    Map<Integer, Integer> atMap = ctx.getStateRoleAssignmentTypeMap();
    assertEquals(PSAssignmentTypeEnum.READER.getValue(), atMap.get(101));
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE.getValue(), atMap.get(102));
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE.getValue(), atMap.get(103));

    // role name map
    Map<Integer, String> nameMap = ctx.getStateRoleNameMap();
    assertEquals("Reader", nameMap.get(101));
    assertEquals("Author", nameMap.get(102));
    assertEquals("Anon", nameMap.get(103));
  }

  @Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
  @Test
  void stateRoles_loadFromHibernate_missingRoleNameThrowsRoleException() {
    long wfId = 7L, stId = 11L;
    PSAssignedRole row =
        mockRole(101L, PSAdhocTypeEnum.DISABLED, PSAssignmentTypeEnum.READER, "Reader");
    when(service.findStateRoles(wfId, stId, 1)).thenReturn(List.of(row));
    // no role-name row for 101 — simulate a schema mismatch
    when(service.findWorkflowRoles(eq(wfId), anySet())).thenReturn(List.of());

    assertThrows(PSRoleException.class, () -> PSStateRolesContext.loadFromHibernate(7, 11, 1));
  }

  // ---------- PSContentAdhocUsersContext.loadFromHibernate ----------

  @Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
  @Test
  void adhocUsers_loadFromHibernate_argValidation() {
    assertThrows(
        IllegalArgumentException.class, () -> PSContentAdhocUsersContext.loadFromHibernate(0));
  }

  @Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
  @Test
  void adhocUsers_loadFromHibernate_emptyRowsEmptyContext() {
    when(systemService.findContentAdhocUsers(1042)).thenReturn(new ArrayList<>());

    PSContentAdhocUsersContext ctx = PSContentAdhocUsersContext.loadFromHibernate(1042);

    assertNotNull(ctx);
    assertEquals(1042, ctx.getContentId());
    assertTrue(ctx.isEmpty());
    assertEquals(0, ctx.getContentAdhocNormalUserCount());
    assertEquals(0, ctx.getContentAdhocAnonymousUserCount());
  }

  @Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
  @Test
  void adhocUsers_loadFromHibernate_classifiesRowsByAdhocType() {
    PSContentAdhocUser adhocNormal = mockAdhocRow(1042, "Alice", 201L, PSAdhocTypeEnum.ENABLED);
    PSContentAdhocUser adhocAnon = mockAdhocRow(1042, "Bob", 202L, PSAdhocTypeEnum.ANONYMOUS);
    when(systemService.findContentAdhocUsers(1042)).thenReturn(List.of(adhocNormal, adhocAnon));

    PSContentAdhocUsersContext ctx = PSContentAdhocUsersContext.loadFromHibernate(1042);

    assertFalse(ctx.isEmpty());
    assertEquals(2, ctx.getContentAdhocNormalUserCount() + ctx.getContentAdhocAnonymousUserCount());

    // Normal user Alice is in m_adhocNormalUserNames; lookup-by-name returns role ids [201].
    assertTrue(ctx.getAdhocNormalUserNames().contains("Alice"));
    assertEquals(List.of(201), ctx.getUserAdhocNormalRoleIDs("Alice"));

    // Anonymous user Bob is in m_adhocAnonymousUserNames; role id 202 is in the bucket.
    assertTrue(ctx.getAdhocAnonymousUserNames().contains("Bob"));
    assertTrue(ctx.getAdhocAnonymousRoleIDs().contains(202));
  }

  @Disabled("Requires Spring+H2 test infrastructure; see phase4-scope-survey.md")
  @Test
  void adhocUsers_loadFromHibernate_emptyUserNameThrows() {
    PSContentAdhocUser bad = mock(PSContentAdhocUser.class);
    when(bad.getUser()).thenReturn("");
    when(bad.getRoleId()).thenReturn(99);
    when(bad.getAdhocType()).thenReturn(PSAdhocTypeEnum.ENABLED.getValue());
    when(systemService.findContentAdhocUsers(1042)).thenReturn(List.of(bad));

    assertThrows(
        IllegalStateException.class, () -> PSContentAdhocUsersContext.loadFromHibernate(1042));
  }

  // ---------- helpers ----------

  private static PSAssignedRole mockRole(
      long roleId, PSAdhocTypeEnum adhoc, PSAssignmentTypeEnum assignment, String name) {
    PSAssignedRole row = mock(PSAssignedRole.class);
    when(row.getGUID()).thenReturn(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, roleId));
    when(row.getAdhocType()).thenReturn(adhoc);
    when(row.getAssignmentType()).thenReturn(assignment);
    when(row.isDoNotify()).thenReturn(true);
    return row;
  }

  private static PSWorkflowRole mockWorkflowRole(long roleId, String name) {
    PSWorkflowRole role = mock(PSWorkflowRole.class);
    when(role.getGUID()).thenReturn(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, roleId));
    when(role.getName()).thenReturn(name);
    return role;
  }

  private static PSContentAdhocUser mockAdhocRow(
      int contentId, String user, long roleId, PSAdhocTypeEnum adhoc) {
    PSContentAdhocUser row = mock(PSContentAdhocUser.class);
    when(row.getContentId()).thenReturn(contentId);
    when(row.getUser()).thenReturn(user);
    when(row.getRoleId()).thenReturn((int) roleId);
    when(row.getAdhocType()).thenReturn(adhoc.getValue());
    return row;
  }

  @SuppressWarnings("unused")
  private static <T> Map<Long, String> dummyMap() {
    return new HashMap<>();
  }
}
