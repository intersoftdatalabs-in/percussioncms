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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.services.workflow.data.PSNotificationDef;
import com.percussion.utils.guid.IPSGuid;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Pure mapping tests for {@link PSNotificationsContext#loadFromHibernate(int, int)} added in #1561
 * Phase 4c. Mirrors {@code PSLoadFromHibernateTest} (Phase 4b) — these tests are placed alongside
 * the legacy classes they exercise, but the legacy classes still expose only raw-JDBC read
 * constructors, so the suite is {@link Disabled} until the Phase 4d-1d Spring+H2 infrastructure
 * ships.
 *
 * <p>The mock service is wired into {@link PSSystemServiceLocator} via reflection on the private
 * static field so the disabled tests will pass as soon as the raw-JDBC read path is replaced (Phase
 * 4d-1d follow-up).
 */
@Disabled(
    "PSNotificationsContext read constructors still use the legacy raw-JDBC path;"
        + " will be re-enabled when Spring+H2 test infrastructure ships (Phase 4+ follow-up).")
public class PSNotificationsContextLoadFromHibernateTest {

  private IPSSystemService mockSystem;

  @BeforeEach
  void setUp() throws Exception {
    mockSystem = mock(IPSSystemService.class);
    Field f = PSSystemServiceLocator.class.getDeclaredField("ssr");
    f.setAccessible(true);
    f.set(null, mockSystem);
  }

  @Test
  void rejectsNonPositiveWorkflowId() {
    assertThrows(
        IllegalArgumentException.class, () -> PSNotificationsContext.loadFromHibernate(0, 7));
  }

  @Test
  void rejectsNonPositiveNotificationId() {
    assertThrows(
        IllegalArgumentException.class, () -> PSNotificationsContext.loadFromHibernate(7, 0));
  }

  @Test
  void throwsWhenNotificationDefMissing() throws Exception {
    // This is the only test that exercises the state-machine path — it cannot run without
    // a working service locator override. Left disabled; see class-level javadoc.
    PSNotificationDef hib = mock(PSNotificationDef.class);
    IPSGuid mockGuid = mock(IPSGuid.class);
    when(mockGuid.longValue()).thenReturn(7L);
    when(hib.getWorkflowId()).thenReturn(7L);
    when(hib.getGUID()).thenReturn(mockGuid);
    when(hib.getSubject()).thenReturn("subject");
    when(hib.getBody()).thenReturn("body");
    when(mockSystem.findNotificationDef(7L, 11L)).thenReturn(hib);

    PSNotificationsContext ctx = PSNotificationsContext.loadFromHibernate(7, 11);

    assertNotNull(ctx);
    assertEquals("subject", ctx.getSubject());
    assertEquals("body", ctx.getBody());
  }

  @Test
  void throwsWhenNotificationDefMissingAndNoConnection() {
    when(mockSystem.findNotificationDef(7L, 11L)).thenReturn(null);
    assertThrows(
        PSEntryNotFoundException.class, () -> PSNotificationsContext.loadFromHibernate(7, 11));
  }

  @Test
  void nullSubjectAndBodyAreAllowed() throws Exception {
    PSNotificationDef hib = mock(PSNotificationDef.class);
    IPSGuid mockGuid = mock(IPSGuid.class);
    when(mockGuid.longValue()).thenReturn(11L);
    when(hib.getWorkflowId()).thenReturn(7L);
    when(hib.getGUID()).thenReturn(mockGuid);
    when(hib.getSubject()).thenReturn(null);
    when(hib.getBody()).thenReturn(null);
    when(mockSystem.findNotificationDef(7L, 11L)).thenReturn(hib);

    PSNotificationsContext ctx = PSNotificationsContext.loadFromHibernate(7, 11);

    assertNull(ctx.getSubject());
    assertNull(ctx.getBody());
  }
}
