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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionHib;
import com.percussion.services.workflow.data.PSTransitionHib.TransitionType;
import com.percussion.services.workflow.data.PSTransitionPK;
import com.percussion.services.workflow.impl.PSWorkflowService;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link PSWorkflowService#loadWorkflowTransition(long, long)} — the
 * Hibernate-backed transition lookup added for #1561 Phase 3 to replace the raw-JDBC
 * {@code PSTransitionsContext} read inside {@code PSExitUpdateHistory}.
 *
 * <p>The service uses {@code @PersistenceContext EntityManager} + a private
 * {@code Session getSession()} helper. We mock both via Mockito (and inject the
 * {@code EntityManager} field via {@link ReflectionTestUtils}) so these tests run
 * without a Spring context or a live database.
 */
public class PSWorkflowServiceLoadWorkflowTransitionTest {

  private PSWorkflowService service;
  private Session session;
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    // PSWorkflowService has a 2-arg constructor (cache, guidMgr) used by the Spring
    // container; for unit tests we pass mocks for both.
    service = new PSWorkflowService(mock(IPSCacheAccess.class), mock(IPSGuidManager.class));
    session = mock(Session.class);
    entityManager = mock(EntityManager.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    ReflectionTestUtils.setField(service, "entityManager", entityManager);
  }

  /**
   * Builds a minimally-valid {@link PSTransitionHib} mock for the tests that exercise the
   * happy path. {@code PSTransformTransitionUtils.convertTransition(hib)} reads the
   * following getters; returning non-null / sane values for each keeps the test focused on
   * the service's own logic rather than on Hibernate's mapping.
   */
  private static PSTransitionHib fullyStubbedHib() {
    PSTransitionHib hib = mock(PSTransitionHib.class);
    when(hib.getTransitionType()).thenReturn(TransitionType.TRANSITION);
    when(hib.getGUID())
        .thenReturn(
            new com.percussion.services.guidmgr.data.PSGuid(
                com.percussion.services.catalog.PSTypeEnum.WORKFLOW_TRANSITION, 11L));
    when(hib.getDescription()).thenReturn("");
    when(hib.getLabel()).thenReturn("Approve");
    when(hib.getNotifications()).thenReturn(new java.util.ArrayList<>());
    when(hib.getStateId()).thenReturn(0L);
    when(hib.getToState()).thenReturn(0L);
    when(hib.getTransitionAction()).thenReturn("");
    when(hib.getTrigger()).thenReturn("");
    when(hib.getWorkflowId()).thenReturn(7L);
    when(hib.isAllowAllRoles()).thenReturn(true);
    when(hib.getApprovals()).thenReturn(1);
    when(hib.isDefaultTransition()).thenReturn(false);
    when(hib.getRequiresComment())
        .thenReturn(com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum.OPTIONAL);
    when(hib.getTransitionRoles()).thenReturn(new java.util.ArrayList<>());
    return hib;
  }

  /**
   * Argument validation: non-positive {@code workflowAppId} and
   * {@code transitionId} must fail fast with {@link IllegalArgumentException}
   * before any Hibernate call.
   */
  @Test
  void rejectsNonPositiveWorkflowAppId() {
    assertThrows(
        IllegalArgumentException.class, () -> service.loadWorkflowTransition(0L, 11L));
    assertThrows(
        IllegalArgumentException.class, () -> service.loadWorkflowTransition(-1L, 11L));
  }

  @Test
  void rejectsNonPositiveTransitionId() {
    assertThrows(
        IllegalArgumentException.class, () -> service.loadWorkflowTransition(7L, 0L));
    assertThrows(
        IllegalArgumentException.class, () -> service.loadWorkflowTransition(7L, -3L));
  }

  /**
   * Aging transitions ({@code TransitionType.AGING}) are intentionally ignored:
   * the exit that calls this only cares about non-aging transitions. The
   * service must return {@code null} for them rather than silently returning a
   * converted aging transition.
   */
  @Test
  void agingTransitionReturnsNull() {
    PSTransitionHib hib = fullyStubbedHib();
    when(hib.getTransitionType()).thenReturn(TransitionType.AGING);
    when(session.get(eq(PSTransitionHib.class), any(PSTransitionPK.class))).thenReturn(hib);

    assertNull(service.loadWorkflowTransition(7L, 11L));
  }

  /**
   * If Hibernate returns no row for the supplied key, the service returns
   * {@code null}. The exit treats that as "transition not found".
   */
  @Test
  void missingRowReturnsNull() {
    when(session.get(eq(PSTransitionHib.class), any(PSTransitionPK.class))).thenReturn(null);

    assertNull(service.loadWorkflowTransition(7L, 11L));
  }

  /**
   * Happy path: a non-aging row in {@code TRANSITIONS} is fetched and converted
   * into a {@link PSTransition} DTO via
   * {@code PSTransformTransitionUtils.convertTransition}. The returned DTO is
   * non-null and the Hibernate {@code Session.get} is called with a
   * {@link PSTransitionPK} matching the supplied id pair.
   */
  @Test
  void happyPathReturnsConvertedTransition() {
    PSTransitionHib hib = fullyStubbedHib();
    when(session.get(eq(PSTransitionHib.class), any(PSTransitionPK.class))).thenReturn(hib);

    PSTransition result = service.loadWorkflowTransition(7L, 11L);

    assertNotNull(result);
    // PSTransition exposes the transition id via its GUID.
    assertEquals(11L, result.getGUID().longValue());
    assertEquals(7L, result.getWorkflowId());
    assertEquals("Approve", result.getLabel());
  }

  /**
   * The service must build the {@link PSTransitionPK} from the supplied
   * (workflowAppId, transitionId) pair exactly. This guards against future
   * refactors that might accidentally swap or coalesce the key.
   */
  @Test
  void usesCompositeKey() {
    PSTransitionHib hib = fullyStubbedHib();
    when(session.get(eq(PSTransitionHib.class), any(PSTransitionPK.class))).thenReturn(hib);

    service.loadWorkflowTransition(7L, 11L);

    org.mockito.Mockito.verify(session)
        .get(
            eq(PSTransitionHib.class),
            org.mockito.ArgumentMatchers.argThat(
                pk -> pk instanceof PSTransitionPK
                    && ((PSTransitionPK) pk).getWorkflowId() == 7L
                    && ((PSTransitionPK) pk).getTransitionId() == 11L));
  }
}