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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.tx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Caller connection holders must not be visible inside navon {@code REQUIRES_NEW} (#4796). */
class PSCallerTransactionSuspensionTest {

  private static final Object RESOURCE_KEY = "caller-connection";

  @AfterEach
  void clearThread() {
    if (TransactionSynchronizationManager.hasResource(RESOURCE_KEY)) {
      TransactionSynchronizationManager.unbindResource(RESOURCE_KEY);
    }
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void executeRequiresNewUnbindsCallerResourceThenRestoresIt() {
    Object caller = new Object();
    TransactionSynchronizationManager.bindResource(RESOURCE_KEY, caller);
    TransactionSynchronizationManager.initSynchronization();
    RecordingManager manager = new RecordingManager();

    String result =
        PSCallerTransactionSuspension.executeRequiresNew(
            manager,
            () -> {
              assertFalse(TransactionSynchronizationManager.hasResource(RESOURCE_KEY));
              assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
              return "saved";
            });

    assertEquals("saved", result);
    assertTrue(manager.sawRequiresNew);
    assertFalse(manager.resourceVisible);
    assertSame(caller, TransactionSynchronizationManager.getResource(RESOURCE_KEY));
    assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
  }

  @Test
  void nullManagerRunsWorkOnCaller() {
    assertEquals("direct", PSCallerTransactionSuspension.executeRequiresNew(null, () -> "direct"));
  }

  @Test
  void nullWorkRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSCallerTransactionSuspension.executeRequiresNew(new RecordingManager(), null));
  }

  private static final class RecordingManager implements PlatformTransactionManager {
    private boolean resourceVisible;
    private boolean sawRequiresNew;

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition)
        throws TransactionException {
      sawRequiresNew =
          definition != null
              && definition.getPropagationBehavior()
                  == TransactionDefinition.PROPAGATION_REQUIRES_NEW;
      resourceVisible = TransactionSynchronizationManager.hasResource(RESOURCE_KEY);
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {}

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {}
  }
}
