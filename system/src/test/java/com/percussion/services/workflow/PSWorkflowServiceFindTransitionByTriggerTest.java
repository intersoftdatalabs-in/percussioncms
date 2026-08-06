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
package com.percussion.services.workflow;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionHib;
import com.percussion.services.workflow.impl.PSWorkflowService;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Mockito-only tests for {@link IPSWorkflowService#findTransitionByTrigger(long, String, long)}
 * added for #1561 Phase 4d-1a. Verifies the JPQL is well-formed, the parameters are forwarded
 * correctly, and the empty / single-result branches return the expected value.
 */
public class PSWorkflowServiceFindTransitionByTriggerTest {

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
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "entityManager", entityManager);
  }

  @Test
  void rejectsNonPositiveWorkflowId() {
    assertThrows(
        IllegalArgumentException.class, () -> service.findTransitionByTrigger(0L, "approve", 11L));
  }

  @Test
  void rejectsNullOrBlankTrigger() {
    assertThrows(
        IllegalArgumentException.class, () -> service.findTransitionByTrigger(7L, null, 11L));
    assertThrows(
        IllegalArgumentException.class, () -> service.findTransitionByTrigger(7L, "", 11L));
    assertThrows(
        IllegalArgumentException.class, () -> service.findTransitionByTrigger(7L, "  ", 11L));
  }

  @Test
  void rejectsNonPositiveStateId() {
    assertThrows(
        IllegalArgumentException.class, () -> service.findTransitionByTrigger(7L, "approve", 0L));
  }

  @Test
  @SuppressWarnings("unchecked")
  void happyPath_noMatchReturnsNull() {
    org.hibernate.query.Query<PSTransitionHib> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSTransitionHib.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("wf"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("sid"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("trig"), anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("tt"), anyInt())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    PSTransition result = service.findTransitionByTrigger(7L, "approve", 11L);

    assertNull(result);

    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture(), eq(PSTransitionHib.class));
    String q = jpql.getValue();
    assertTrue(q.contains("from PSTransitionHib"), "JPQL must target the entity: " + q);
    assertTrue(q.contains("workflowId = :wf"), "JPQL must filter on workflowId: " + q);
    assertTrue(q.contains("stateId = :sid"), "JPQL must filter on stateId: " + q);
    assertTrue(q.contains("trigger = :trig"), "JPQL must filter on trigger: " + q);
    assertTrue(q.contains("transitionType = :tt"), "JPQL must filter on transitionType: " + q);

    verify(mockQuery).setParameter("wf", 7L);
    verify(mockQuery).setParameter("sid", 11L);
    verify(mockQuery).setParameter("trig", "approve");
    verify(mockQuery).setParameter("tt", PSTransitionHib.TransitionType.TRANSITION.getValue());
  }

  @Test
  @SuppressWarnings("unchecked")
  void multipleMatches_throws() {
    PSTransitionHib hib1 = mock(PSTransitionHib.class);
    PSTransitionHib hib2 = mock(PSTransitionHib.class);
    org.hibernate.query.Query<PSTransitionHib> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSTransitionHib.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(java.util.Arrays.asList(hib1, hib2));

    assertThrows(
        IllegalStateException.class, () -> service.findTransitionByTrigger(7L, "approve", 11L));
  }
}
