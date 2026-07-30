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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.workflow.data.PSNotification;
import com.percussion.services.workflow.impl.PSWorkflowService;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Mockito-only tests for {@link IPSWorkflowService#findTransitionNotifications(long, long)} added
 * for #1561 Phase 4c. Verifies the JPQL is well-formed and that the parameters are forwarded
 * correctly. Behavioural assertions about the result set are out of scope here — see {@code
 * com.percussion.workflow.PSTransitionNotificationsContextLoadFromHibernateTest} for the mapping
 * tests.
 */
public class PSWorkflowServiceFindTransitionNotificationsTest {

  private PSWorkflowService service;
  private Session session;
  private EntityManager entityManager;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    service = new PSWorkflowService(mock(IPSCacheAccess.class), mock(IPSGuidManager.class));
    session = mock(Session.class);
    entityManager = mock(EntityManager.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    ReflectionTestUtils.setField(service, "entityManager", entityManager);
  }

  @Test
  void rejectsNonPositiveWorkflowId() {
    assertThrows(IllegalArgumentException.class, () -> service.findTransitionNotifications(0L, 7L));
  }

  @Test
  void rejectsNonPositiveTransitionId() {
    assertThrows(IllegalArgumentException.class, () -> service.findTransitionNotifications(7L, 0L));
  }

  @Test
  @SuppressWarnings("unchecked")
  void happyPath_jpqlAndParameters() {
    org.hibernate.query.Query<PSNotification> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSNotification.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("wf"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("tid"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    List<PSNotification> result = service.findTransitionNotifications(7L, 11L);

    assertNotNull(result);
    assertTrue(result.isEmpty());

    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture(), eq(PSNotification.class));
    String q = jpql.getValue();
    assertTrue(q.contains("from PSNotification"), "JPQL must target the entity: " + q);
    assertTrue(q.contains("workflowId = :wf"), "JPQL must filter on workflowId: " + q);
    assertTrue(q.contains("transitionId = :tid"), "JPQL must filter on transitionId: " + q);
    assertTrue(
        q.contains("order by transitionNotificationId"),
        "JPQL must order by transitionNotificationId for cursor compatibility: " + q);

    verify(mockQuery).setParameter("wf", 7L);
    verify(mockQuery).setParameter("tid", 11L);
  }

  @Test
  @SuppressWarnings("unchecked")
  void emptyResultIsForwarded() {
    org.hibernate.query.Query<PSNotification> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSNotification.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), anyLong())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    assertEquals(Collections.emptyList(), service.findTransitionNotifications(7L, 11L));
  }

  @Test
  @SuppressWarnings("unchecked")
  void rowsArePassedThroughUnchanged() {
    org.hibernate.query.Query<PSNotification> mockQuery = mock(org.hibernate.query.Query.class);
    List<PSNotification> rows = new ArrayList<>();
    PSNotification row = mock(PSNotification.class);
    rows.add(row);
    when(session.createQuery(anyString(), eq(PSNotification.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), anyLong())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(rows);

    List<PSNotification> result = service.findTransitionNotifications(7L, 11L);
    assertEquals(1, result.size());
    assertEquals(row, result.get(0));
  }
}
