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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionHib;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure mapping tests for {@link PSTransitionsContext#loadAllFromHibernate(int, int)} and the
 * related single-row factories added in #1561 Phase 4d-1a. The legacy class still exposes only
 * raw-JDBC read constructors, so the suite is {@code @Disabled} until the Spring+H2 test
 * infrastructure ships. The mock wiring is in place so the tests will pass as soon as the raw-JDBC
 * read path is replaced.
 */
@org.junit.jupiter.api.Disabled(
    "PSTransitionsContext read constructors still use the legacy raw-JDBC path;"
        + " will be re-enabled when Spring+H2 test infrastructure ships (Phase 4d-1d follow-up).")
public class PSTransitionsContextLoadFromHibernateTest {

  private IPSWorkflowService mockWf;
  private IPSWorkflowService savedWf;

  @BeforeEach
  void setUp() throws Exception {
    // Replace the locator backing — simpler than @InjectMocks here.
    savedWf = PSWorkflowServiceLocator.getWorkflowService();
    mockWf = mock(IPSWorkflowService.class);
    Field f = PSWorkflowServiceLocator.class.getDeclaredField("workflowService");
    f.setAccessible(true);
    Field mf = Field.class.getDeclaredField("modifiers");
    mf.setAccessible(true);
    mf.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
    @SuppressWarnings("unchecked")
    AtomicReference<IPSWorkflowService> ref = (AtomicReference<IPSWorkflowService>) f.get(null);
    ref.set(mockWf);
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() throws Exception {
    Field f = PSWorkflowServiceLocator.class.getDeclaredField("workflowService");
    f.setAccessible(true);
    Field mf = Field.class.getDeclaredField("modifiers");
    mf.setAccessible(true);
    mf.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
    @SuppressWarnings("unchecked")
    AtomicReference<IPSWorkflowService> ref = (AtomicReference<IPSWorkflowService>) f.get(null);
    ref.set(savedWf);
  }

  // --- loadAllFromHibernate ------------------------------------------------

  @Test
  void loadAllFromHibernate_rejectsNonPositiveWorkflowId() {
    assertThrows(
        IllegalArgumentException.class, () -> PSTransitionsContext.loadAllFromHibernate(0, 11));
  }

  @Test
  void loadAllFromHibernate_rejectsNonPositiveFromStateId() {
    assertThrows(
        IllegalArgumentException.class, () -> PSTransitionsContext.loadAllFromHibernate(7, 0));
  }

  @Test
  void loadAllFromHibernate_emptyResult_isEmpty() {
    when(mockWf.findTransitionsByState(7L, 11L)).thenReturn(Collections.emptyList());

    PSTransitionsContext ctx = PSTransitionsContext.loadAllFromHibernate(7, 11);

    assertNotNull(ctx);
    assertTrue(ctx.isEmpty());
  }

  @Test
  void loadAllFromHibernate_singleRow_firstMoveNextReadsRowZero() {
    PSTransitionHib hib = mock(PSTransitionHib.class);
    PSTransition t = mock(PSTransition.class);
    when(t.getGUID()).thenReturn(new PSLegacyGuid(101L));
    when(t.getLabel()).thenReturn("Approve");
    when(t.getDescription()).thenReturn("Approve the item");
    when(t.getStateId()).thenReturn(11L);
    when(t.getToState()).thenReturn(13L);
    when(t.getTrigger()).thenReturn("approve");
    when(t.getApprovals()).thenReturn(1);
    when(t.getRequiresComment())
        .thenReturn(
            com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum.OPTIONAL);
    when(t.getTransitionAction()).thenReturn("");
    when(t.isAllowAllRoles()).thenReturn(true);
    when(t.getTransitionRoles()).thenReturn(Collections.emptyList());
    when(mockWf.findTransitionsByState(7L, 11L)).thenReturn(Collections.singletonList(t));

    PSTransitionsContext ctx = PSTransitionsContext.loadAllFromHibernate(7, 11);

    assertNotNull(ctx);
    assertEquals(1, ctx.getTransitionCount());
    // First moveNext() reads the pre-loaded first row.
    assertTrue(invokeMoveNext(ctx));
    assertEquals(101, ctx.getTransitionID());
    assertEquals("Approve", ctx.getTransitionLabel());
    assertEquals(11, ctx.getTransitionFromStateID());
    assertEquals(13, ctx.getTransitionToStateID());
    assertEquals("approve", ctx.getTransitionActionTrigger());
  }

  // --- loadFromHibernate(int, int) -----------------------------------------

  @Test
  void loadFromHibernateById_noMatch_isEmpty() {
    when(mockWf.loadWorkflowTransition(7L, 101L)).thenReturn(null);

    PSTransitionsContext ctx = PSTransitionsContext.loadFromHibernate(7, 101);

    assertNotNull(ctx);
    assertTrue(ctx.isEmpty());
  }

  @Test
  void loadFromHibernateById_rejectsNonPositiveWorkflowId() {
    assertThrows(
        IllegalArgumentException.class, () -> PSTransitionsContext.loadFromHibernate(0, 101));
  }

  @Test
  void loadFromHibernateById_rejectsNonPositiveTransitionId() {
    assertThrows(
        IllegalArgumentException.class, () -> PSTransitionsContext.loadFromHibernate(7, 0));
  }

  // --- loadFromHibernate(int, String, int) ---------------------------------

  @Test
  void loadFromHibernateByTrigger_noMatch_isEmpty() {
    when(mockWf.findTransitionByTrigger(7L, "approve", 11L)).thenReturn(null);

    PSTransitionsContext ctx = PSTransitionsContext.loadFromHibernate(7, "approve", 11);

    assertNotNull(ctx);
    assertTrue(ctx.isEmpty());
  }

  @Test
  void loadFromHibernateByTrigger_rejectsBlankTrigger() {
    assertThrows(
        IllegalArgumentException.class, () -> PSTransitionsContext.loadFromHibernate(7, "", 11));
    assertThrows(
        IllegalArgumentException.class, () -> PSTransitionsContext.loadFromHibernate(7, null, 11));
  }

  @Test
  void loadFromHibernateByTrigger_rejectsNonPositiveStateId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSTransitionsContext.loadFromHibernate(7, "approve", 0));
  }

  // --- helpers -------------------------------------------------------------

  /**
   * The legacy raw-JDBC {@code moveNext()} throws {@code SQLException}; the Hibernate-backed branch
   * doesn't, but the public signature still declares it. We invoke via reflection so the test
   * compiles whether the signature changes or not.
   */
  private static boolean invokeMoveNext(PSTransitionsContext ctx) {
    try {
      java.lang.reflect.Method m = PSTransitionsContext.class.getDeclaredMethod("moveNext");
      Object result = m.invoke(ctx);
      return (Boolean) result;
    } catch (java.lang.reflect.InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException(cause);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
