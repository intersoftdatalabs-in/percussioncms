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
package com.percussion.services.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.system.impl.PSSystemService;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSNotificationDef;
import com.percussion.services.workflow.data.PSNotificationDefPK;
import com.percussion.utils.jdbc.IPSDatasourceManager;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Mockito-only tests for {@link PSSystemService#findNotificationDef(long, long)} added for #1561
 * Phase 4c. Verifies the {@link Session#get(Class, Object)} call targets the {@link
 * PSNotificationDef} entity with the correct composite key.
 */
public class PSSystemServiceFindNotificationDefTest {

  private PSSystemService service;
  private Session session;
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    service =
        new PSSystemService(
            mock(IPSDatasourceManager.class),
            mock(IPSGuidManager.class),
            mock(IPSWorkflowService.class),
            mock(IPSCmsObjectMgr.class));
    session = mock(Session.class);
    entityManager = mock(EntityManager.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    ReflectionTestUtils.setField(service, "entityManager", entityManager);
  }

  @Test
  void rejectsNonPositiveWorkflowId() {
    assertThrows(IllegalArgumentException.class, () -> service.findNotificationDef(0L, 7L));
    assertThrows(IllegalArgumentException.class, () -> service.findNotificationDef(-1L, 7L));
  }

  @Test
  void rejectsNonPositiveNotificationId() {
    assertThrows(IllegalArgumentException.class, () -> service.findNotificationDef(7L, 0L));
    assertThrows(IllegalArgumentException.class, () -> service.findNotificationDef(7L, -1L));
  }

  @Test
  void usesCompositeKey() {
    when(session.get(eq(PSNotificationDef.class), any(PSNotificationDefPK.class))).thenReturn(null);

    PSNotificationDef result = service.findNotificationDef(7L, 11L);

    assertNull(result);
    verify(session).get(eq(PSNotificationDef.class), eq(new PSNotificationDefPK(7L, 11L)));
  }

  @Test
  void passesTheResultThrough() {
    PSNotificationDef row = mock(PSNotificationDef.class);
    when(session.get(eq(PSNotificationDef.class), any(PSNotificationDefPK.class))).thenReturn(row);

    PSNotificationDef result = service.findNotificationDef(7L, 11L);
    assertNotNull(result);
    assertEquals(row, result);
  }
}
