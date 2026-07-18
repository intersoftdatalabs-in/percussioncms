/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.guidmgr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.guidmgr.data.PSGuidGeneratorData;
import java.lang.reflect.Field;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Behavioral coverage for {@link PSGuidManager#updateNextLong(Integer)} after
 * Hibernate 7 package-install failures (StaleObjectStateException on merge).
 */
class PSGuidManagerUpdateNextLongTest {

  private PSGuidManager mgr;
  private Session session;

  @BeforeEach
  void setUp() throws Exception {
    mgr = new PSGuidManager();
    session = mock(Session.class);
    // Inject a mock EntityManager that unwraps to our Session
    var em = mock(jakarta.persistence.EntityManager.class);
    when(em.unwrap(Session.class)).thenReturn(session);
    Field f = PSGuidManager.class.getDeclaredField("entityManager");
    f.setAccessible(true);
    f.set(mgr, em);
  }

  @Test
  @DisplayName("new key uses persist without forcing @Version")
  void newKeyPersistsWithoutMerge() {
    when(session.get(eq(PSGuidGeneratorData.class), eq(17), eq(LockMode.PESSIMISTIC_WRITE)))
        .thenReturn(null);

    long first = mgr.updateNextLong(17);

    assertEquals(2L, first); // current=1 then return current+1
    ArgumentCaptor<PSGuidGeneratorData> cap = ArgumentCaptor.forClass(PSGuidGeneratorData.class);
    verify(session).persist(cap.capture());
    verify(session, never()).merge(any());
    PSGuidGeneratorData saved = cap.getValue();
    assertEquals(17, saved.getKey());
    assertEquals(1 + PSGuidManager.BLOCK_SIZE, saved.getValue());
    // Default version for new entities must remain -1 (Hibernate insert sentinel)
    assertEquals(-1, saved.getVersion());
  }

  @Test
  @DisplayName("existing managed row updates value without merge")
  void existingKeyDirtiesWithoutMerge() {
    PSGuidGeneratorData existing = new PSGuidGeneratorData(17, 50);
    existing.setVersion(3);
    when(session.get(eq(PSGuidGeneratorData.class), eq(17), eq(LockMode.PESSIMISTIC_WRITE)))
        .thenReturn(existing);

    long first = mgr.updateNextLong(17);

    assertEquals(51L, first);
    assertEquals(50 + PSGuidManager.BLOCK_SIZE, existing.getValue());
    verify(session, never()).persist(any());
    verify(session, never()).merge(any());
    verify(session).flush();
  }

  @Test
  @DisplayName("optimistic lock on first attempt retries and succeeds")
  void retriesOnOptimisticLock() {
    PSGuidGeneratorData firstLoad = new PSGuidGeneratorData(17, 10);
    firstLoad.setVersion(1);
    PSGuidGeneratorData secondLoad = new PSGuidGeneratorData(17, 20);
    secondLoad.setVersion(2);

    when(session.get(eq(PSGuidGeneratorData.class), eq(17), eq(LockMode.PESSIMISTIC_WRITE)))
        .thenReturn(firstLoad)
        .thenReturn(secondLoad);
    // First flush throws stale; second succeeds
    org.mockito.Mockito.doThrow(
            new org.hibernate.StaleObjectStateException(PSGuidGeneratorData.class.getName(), 17))
        .doNothing()
        .when(session)
        .flush();

    long first = mgr.updateNextLong(17);

    assertEquals(21L, first);
    assertTrue(secondLoad.getValue() >= 20 + PSGuidManager.BLOCK_SIZE - 1
        || secondLoad.getValue() == 20 + PSGuidManager.BLOCK_SIZE);
    verify(session, org.mockito.Mockito.atLeastOnce()).clear();
  }
}
