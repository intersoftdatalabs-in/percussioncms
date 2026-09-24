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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs work in {@code REQUIRES_NEW} after unbinding the caller thread's Spring transaction
 * resources.
 *
 * <p>{@code JpaTransactionManager} suspends only the resources it bound. Site rename also binds a
 * JDBC connection holder on the same datasource. A nested {@code REQUIRES_NEW} then enlists that
 * holder, sees {@code isGlobalRollbackOnly}, and {@code loadItems} throws {@code
 * UnexpectedRollbackException} even when the navon save itself is valid (#4796).
 */
public final class PSCallerTransactionSuspension {

  private PSCallerTransactionSuspension() {}

  /**
   * Unbind caller resources, run {@code work} in a new transaction, then restore the caller
   * bindings.
   *
   * @param transactionManager manager for the new transaction; when {@code null}, {@code work}
   *     runs on the caller thread with no extra transaction
   * @param work unit of work, never {@code null}
   * @return the supplier result
   */
  public static <T> T executeRequiresNew(
      PlatformTransactionManager transactionManager, Supplier<T> work) {
    if (work == null) {
      throw new IllegalArgumentException("work is required");
    }
    if (transactionManager == null) {
      return work.get();
    }
    Map<Object, Object> suspended = new IdentityHashMap<>();
    for (Object key : new ArrayList<>(TransactionSynchronizationManager.getResourceMap().keySet())) {
      Object value = TransactionSynchronizationManager.unbindResourceIfPossible(key);
      if (value != null) {
        suspended.put(key, value);
      }
    }
    List<TransactionSynchronization> syncs = List.of();
    boolean syncActive = TransactionSynchronizationManager.isSynchronizationActive();
    if (syncActive) {
      syncs = new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
      TransactionSynchronizationManager.clearSynchronization();
    }
    try {
      TransactionTemplate template = new TransactionTemplate(transactionManager);
      template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      return template.execute(status -> work.get());
    } finally {
      for (Object key :
          new ArrayList<>(TransactionSynchronizationManager.getResourceMap().keySet())) {
        if (!suspended.containsKey(key)) {
          TransactionSynchronizationManager.unbindResourceIfPossible(key);
        }
      }
      if (syncActive && !TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.initSynchronization();
        for (TransactionSynchronization sync : syncs) {
          TransactionSynchronizationManager.registerSynchronization(sync);
        }
      }
      for (Map.Entry<Object, Object> entry : suspended.entrySet()) {
        if (TransactionSynchronizationManager.hasResource(entry.getKey())) {
          TransactionSynchronizationManager.unbindResource(entry.getKey());
        }
        TransactionSynchronizationManager.bindResource(entry.getKey(), entry.getValue());
      }
    }
  }
}
