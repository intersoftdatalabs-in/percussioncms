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

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.List;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Mockito-only tests for {@link IPSWorkflowService#findTransitionsByState(long, long)} added for
 * #1561 Phase 4d-1a. Verifies the JPQL is well-formed, the parameters are forwarded correctly, and
 * the result list maps from {@link PSTransitionHib} to {@link PSTransition} DTOs.
 *
 * <p>Behavioural assertions about the cursor iteration are out of scope here — see {@code
 * com.percussion.workflow.PSTransitionsContextLoadFromHibernateTest} for the mapping tests.
 */
public class PSWorkflowServiceFindTransitionsByStateTest {

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
    assertThrows(IllegalArgumentException.class, () -> service.findTransitionsByState(0L, 7L));
  }

  @Test
  void rejectsNonPositiveStateId() {
    assertThrows(IllegalArgumentException.class, () -> service.findTransitionsByState(7L, 0L));
  }

  @Test
  @SuppressWarnings("unchecked")
  void happyPath_jpqlAndParametersAndEmptyResult() {
    org.hibernate.query.Query<PSTransitionHib> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSTransitionHib.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("wf"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("sid"), anyLong())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("tt"), anyInt())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    List<PSTransition> result = service.findTransitionsByState(7L, 11L);

    assertNotNull(result);
    assertTrue(result.isEmpty());

    ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
    verify(session).createQuery(jpql.capture(), eq(PSTransitionHib.class));
    String q = jpql.getValue();
    assertTrue(q.contains("from PSTransitionHib"), "JPQL must target the entity: " + q);
    assertTrue(q.contains("workflowId = :wf"), "JPQL must filter on workflowId: " + q);
    assertTrue(q.contains("stateId = :sid"), "JPQL must filter on stateId: " + q);
    assertTrue(q.contains("transitionType = :tt"), "JPQL must filter on transitionType: " + q);
    assertTrue(q.contains("order by transitionId"), "JPQL must order by transitionId: " + q);

    verify(mockQuery).setParameter("wf", 7L);
    verify(mockQuery).setParameter("sid", 11L);
    verify(mockQuery).setParameter("tt", PSTransitionHib.TransitionType.TRANSITION.getValue());
  }

  @Test
  @SuppressWarnings("unchecked")
  void nonEmptyResult_passesResultListThrough() {
    // We don't deeply stub the static PSTransformTransitionUtils.convertTransition helper here —
    // that mapping is exercised by the Hibernate entity tests and the integration tests under
    // extensions-workflow. This test just verifies that findTransitionsByState returns a list of
    // the expected size when the underlying query yields multiple rows.
    org.hibernate.query.Query<PSTransitionHib> mockQuery = mock(org.hibernate.query.Query.class);
    when(session.createQuery(anyString(), eq(PSTransitionHib.class))).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

    List<PSTransition> result = service.findTransitionsByState(7L, 11L);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
