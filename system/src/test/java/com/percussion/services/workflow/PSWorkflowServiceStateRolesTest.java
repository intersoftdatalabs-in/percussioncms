/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.services.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.workflow.data.PSAssignedRole;
import com.percussion.services.workflow.data.PSAssignedRolePK;
import com.percussion.services.workflow.data.PSWorkflowRole;
import com.percussion.services.workflow.data.PSWorkflowRolePK;
import com.percussion.services.workflow.impl.PSWorkflowService;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for the Hibernate-backed state-roles query methods added for #1561 Phase 4b:
 * {@link IPSWorkflowService#findStateRoles(long, long, int)} and
 * {@link IPSWorkflowService#findWorkflowRoles(long, Set)}.
 *
 * <p>The service uses {@code @PersistenceContext EntityManager} + a private
 * {@code Session getSession()} helper. We mock both via Mockito (and inject the
 * {@code EntityManager} field via {@link ReflectionTestUtils}) so these tests run
 * without a Spring context or a live database.</p>
 */
public class PSWorkflowServiceStateRolesTest {

  private PSWorkflowService service;
  private Session session;
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    service = new PSWorkflowService(mock(IPSCacheAccess.class), mock(IPSGuidManager.class));
    session = mock(Session.class);
    entityManager = mock(EntityManager.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    ReflectionTestUtils.setField(service, "entityManager", entityManager);
  }

  /** Argument validation: non-positive ids fail fast. */
  @Test
  void findStateRoles_rejectsNonPositiveIds() {
    assertThrows(
        IllegalArgumentException.class, () -> service.findStateRoles(0L, 7L, 1));
    assertThrows(
        IllegalArgumentException.class, () -> service.findStateRoles(7L, 0L, 1));
  }

  /**
   * Happy path: the query is a typed {@code createQuery} with the right JPQL + parameters, and the
   * returned rows are passed through unchanged. Empty result is returned as an empty list.
   */
  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void findStateRoles_happyPath() {
    org.hibernate.query.Query mockQuery = mock(org.hibernate.query.Query.class);
    List<PSAssignedRole> rows = new ArrayList<>();
    PSAssignedRole row = mock(PSAssignedRole.class);
    rows.add(row);
    when(session.createQuery(any(String.class), eq(PSAssignedRole.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("wf"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("sid"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("at"), any(Integer.class))).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(rows);

    List<PSAssignedRole> result = service.findStateRoles(7L, 11L, 2);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(row, result.get(0));

    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture(), eq(PSAssignedRole.class));
    String q = jpql.getValue();
    assertTrue(q.contains("from PSAssignedRole"), "JPQL must target the entity: " + q);
    assertTrue(q.contains("workflowId = :wf"), "JPQL must filter on workflowId: " + q);
    assertTrue(q.contains("stateId = :sid"), "JPQL must filter on stateId: " + q);
    assertTrue(q.contains("assignmentType >= :at"), "JPQL must filter on assignmentType: " + q);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void findStateRoles_emptyResultReturnsEmptyList() {
    org.hibernate.query.Query mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(any(String.class), eq(PSAssignedRole.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(any(String.class), any())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    assertEquals(Collections.emptyList(), service.findStateRoles(7L, 11L, 1));
  }

  @Test
  void findWorkflowRoles_rejectsNonPositiveWorkflowId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.findWorkflowRoles(0L, new HashSet<>(Collections.singletonList(11L))));
  }

  @Test
  void findWorkflowRoles_rejectsNullSet() {
    assertThrows(
        IllegalArgumentException.class, () -> service.findWorkflowRoles(7L, null));
  }

  /** Empty role-id set returns empty list without ever touching Hibernate. */
  @Test
  void findWorkflowRoles_emptySetReturnsEmptyList() {
    assertEquals(
        Collections.emptyList(),
        service.findWorkflowRoles(7L, new HashSet<>(Collections.emptyList())));
    org.mockito.Mockito.verifyNoInteractions(session);
  }

  /**
   * Happy path: the query is a typed {@code createQuery} with the right JPQL + parameters,
   * and the returned rows are passed through.
   */
  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void findWorkflowRoles_happyPath() {
    org.hibernate.query.Query mockQuery = mock(org.hibernate.query.Query.class);
    PSWorkflowRole row = mock(PSWorkflowRole.class);
    List<PSWorkflowRole> rows = Collections.singletonList(row);
    Set<Long> roleIds = new HashSet<>();
    roleIds.add(11L);
    roleIds.add(13L);

    when(session.createQuery(any(String.class), eq(PSWorkflowRole.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("wf"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("ids"), any())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(rows);

    List<PSWorkflowRole> result = service.findWorkflowRoles(7L, roleIds);

    assertEquals(1, result.size());

    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture(), eq(PSWorkflowRole.class));
    String q = jpql.getValue();
    assertTrue(q.contains("from PSWorkflowRole"), "JPQL must target the entity: " + q);
    assertTrue(q.contains("workflowId = :wf"), "JPQL must filter on workflowId: " + q);
    assertTrue(q.contains("roleId in :ids"), "JPQL must filter on roleId IN : " + q);
  }
}