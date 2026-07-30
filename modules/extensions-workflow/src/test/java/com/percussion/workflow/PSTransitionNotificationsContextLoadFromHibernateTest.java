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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.services.workflow.data.PSNotification;
import com.percussion.services.workflow.data.PSNotification.PSStateRoleRecipientTypeEnum;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Pure mapping tests for {@link PSTransitionNotificationsContext#loadFromHibernate(int, int)} added
 * in #1561 Phase 4c. Mirrors {@code PSLoadFromHibernateTest} (Phase 4b) — these tests are placed
 * alongside the legacy classes they exercise, but the legacy classes still expose only raw-JDBC
 * read constructors, so the suite is {@link Disabled} until the Phase 4d-1d Spring+H2
 * infrastructure ships.
 *
 * <p>The mock service is wired into {@link PSWorkflowServiceLocator} via reflection on the private
 * static {@code AtomicReference} field so the disabled tests will pass as soon as the raw-JDBC read
 * path is replaced (Phase 4d-1d follow-up).
 */
@Disabled(
    "PSTransitionNotificationsContext read constructors still use the legacy raw-JDBC path;"
        + " will be re-enabled when Spring+H2 test"
        + " infrastructure ships (Phase 4d-1d follow-up).")
public class PSTransitionNotificationsContextLoadFromHibernateTest {

  private IPSWorkflowService mockWf;

  @BeforeEach
  void setUp() throws Exception {
    mockWf = mock(IPSWorkflowService.class);
    Field f = PSWorkflowServiceLocator.class.getDeclaredField("workflowService");
    f.setAccessible(true);
    f.set(null, new AtomicReference<>(mockWf));
  }

  @Test
  void rejectsNonPositiveWorkflowId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSTransitionNotificationsContext.loadFromHibernate(0, 7));
  }

  @Test
  void rejectsNonPositiveTransitionId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSTransitionNotificationsContext.loadFromHibernate(7, 0));
  }

  @Test
  void emptyResultIsAllowed() {
    when(mockWf.findTransitionNotifications(7L, 11L)).thenReturn(Collections.emptyList());

    PSTransitionNotificationsContext ctx =
        PSTransitionNotificationsContext.loadFromHibernate(7, 11);

    assertNotNull(ctx);
    assertEquals(0, ctx.getCount());
  }

  @Test
  void singleRowCursorIsPositionedAtIndex0() {
    // Locks in the cursor invariant: for N=1, the inner loop sets
    // m_nNotificationID directly so getNotificationID() returns row 0's data
    // without needing moveNext(). The legacy PSAbstractMultipleRecordWorkflowContext
    // intentionally guards MoveAccumulatedDataSet(0) behind m_nCount > 1; the
    // initial currentContextDataIndex = 0 IS the first entry position.
    PSNotification only = makeRow(101L, PSStateRoleRecipientTypeEnum.TO_STATE_RECIPIENTS, "", "");
    when(mockWf.findTransitionNotifications(7L, 11L)).thenReturn(Collections.singletonList(only));

    PSTransitionNotificationsContext ctx =
        PSTransitionNotificationsContext.loadFromHibernate(7, 11);

    assertEquals(1, ctx.getCount());
    // Without calling moveNext(), consumers expect getNotificationID() to return row 0.
    assertEquals(101, ctx.getNotificationID());
    // First moveNext() advances past the last row and returns false.
    assertEquals(false, ctx.moveNext());
  }

  @Test
  void rowsArePopulatedInOrder() {
    PSNotification n1 =
        makeRow(
            101L,
            PSStateRoleRecipientTypeEnum.TO_STATE_RECIPIENTS,
            "user1@example.com",
            "cc1@example.com");
    PSNotification n2 =
        makeRow(102L, PSStateRoleRecipientTypeEnum.FROM_STATE_RECIPIENTS, "user2@example.com", "");
    PSNotification n3 =
        makeRow(
            103L, PSStateRoleRecipientTypeEnum.TO_AND_FROM_STATE_RECIPIENTS, "", "cc3@example.com");
    List<PSNotification> rows = new ArrayList<>();
    rows.add(n1);
    rows.add(n2);
    rows.add(n3);
    when(mockWf.findTransitionNotifications(7L, 11L)).thenReturn(rows);

    PSTransitionNotificationsContext ctx =
        PSTransitionNotificationsContext.loadFromHibernate(7, 11);

    assertEquals(3, ctx.getCount());
    assertEquals(101, ctx.getNotificationID());
    assertTrue(ctx.notifyToStateRoles());
    assertTrue(!ctx.notifyFromStateRoles());

    // moveNext to row 2
    assertTrue(ctx.moveNext());
    assertEquals(102, ctx.getNotificationID());
    assertTrue(ctx.notifyFromStateRoles());
    assertTrue(!ctx.notifyToStateRoles());

    // moveNext to row 3
    assertTrue(ctx.moveNext());
    assertEquals(103, ctx.getNotificationID());
    assertTrue(ctx.notifyToStateRoles());
    assertTrue(ctx.notifyFromStateRoles());

    // End of cursor
    assertTrue(!ctx.moveNext());
  }

  @Test
  void aggregateRecipientFlagsAcrossRows() {
    PSNotification onlyTo = makeRow(1L, PSStateRoleRecipientTypeEnum.TO_STATE_RECIPIENTS, "", "");
    when(mockWf.findTransitionNotifications(7L, 11L)).thenReturn(Collections.singletonList(onlyTo));

    PSTransitionNotificationsContext ctx =
        PSTransitionNotificationsContext.loadFromHibernate(7, 11);

    assertTrue(ctx.requireToStateRoles());
    assertTrue(!ctx.requireFromStateRoles());
  }

  private static PSNotification makeRow(
      long notificationId,
      PSStateRoleRecipientTypeEnum recipientType,
      String recipientList,
      String ccList) {
    PSNotification row = mock(PSNotification.class);
    when(row.getNotificationId()).thenReturn(notificationId);
    when(row.getStateRoleRecipientType()).thenReturn(recipientType);
    when(row.getRecipients())
        .thenReturn(
            recipientList == null || recipientList.isEmpty()
                ? Collections.emptyList()
                : Collections.singletonList(recipientList));
    when(row.getCCRecipients())
        .thenReturn(
            ccList == null || ccList.isEmpty()
                ? Collections.emptyList()
                : Collections.singletonList(ccList));
    return row;
  }
}
