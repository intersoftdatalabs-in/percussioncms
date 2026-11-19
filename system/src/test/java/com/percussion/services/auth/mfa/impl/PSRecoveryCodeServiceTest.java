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
package com.percussion.services.auth.mfa.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSMfaRecoveryCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class PSRecoveryCodeServiceTest {

  private static PSRecoveryCodeService createServiceWithInMemoryDao() {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    Map<String, PSMfaRecoveryCode> store = new ConcurrentHashMap<>();

    doAnswer(
            inv -> {
              List<PSMfaRecoveryCode> rows = inv.getArgument(0);
              for (PSMfaRecoveryCode row : rows) {
                store.put(row.getCodeId(), row);
              }
              return null;
            })
        .when(dao)
        .saveRecoveryCodes(any());
    when(dao.invalidateUnusedRecoveryCodesByUser(eq("alice"), any()))
        .thenAnswer(inv -> invalidateUnusedForUser(store, "alice", inv.getArgument(1)));
    when(dao.findUnusedRecoveryCodesByUser("alice"))
        .thenAnswer(
            inv ->
                store.values().stream()
                    .filter(r -> "alice".equals(r.getUserName()) && r.getUsedAt() == null)
                    .toList());
    when(dao.consumeRecoveryCodeAtomically(any(), any()))
        .thenAnswer(inv -> consumeAtomically(store, inv.getArgument(0), inv.getArgument(1)));

    Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);
    return new PSRecoveryCodeService(dao, clock, 1000);
  }

  private static int invalidateUnusedForUser(
      Map<String, PSMfaRecoveryCode> store, String userName, Date usedAt) {
    int count = 0;
    for (PSMfaRecoveryCode row : store.values()) {
      if (userName.equals(row.getUserName()) && row.getUsedAt() == null) {
        row.setUsedAt(usedAt);
        count++;
      }
    }
    return count;
  }

  private static int consumeAtomically(
      Map<String, PSMfaRecoveryCode> store, String id, Date usedAt) {
    PSMfaRecoveryCode row = store.get(id);
    if (row == null) {
      return 0;
    }
    synchronized (row) {
      if (row.getUsedAt() == null) {
        row.setUsedAt(usedAt);
        return 1;
      }
      return 0;
    }
  }

  @Test
  void generatesCrockfordBatchNormalizesInputAndInvalidatesOnRegen() {
    PSRecoveryCodeService service = createServiceWithInMemoryDao();

    List<String> firstBatch = service.generateAndReplaceCodes("alice");
    assertEquals(10, firstBatch.size());

    // Normalizes case, whitespace, and missing hyphens
    String code0 = firstBatch.get(0);
    String messy = "  " + code0.toLowerCase().replace("-", " ") + " ";
    assertTrue(service.consumeCode("alice", messy));
    assertFalse(service.consumeCode("alice", code0));

    // Regenerating invalidates unused prior batch codes
    List<String> secondBatch = service.generateAndReplaceCodes("alice");
    assertFalse(service.consumeCode("alice", firstBatch.get(2)));
    assertTrue(service.consumeCode("alice", secondBatch.get(0)));
  }

  @Test
  void preventsConcurrentReplayOfSameRecoveryCode() throws Exception {
    PSRecoveryCodeService service = createServiceWithInMemoryDao();
    List<String> batch = service.generateAndReplaceCodes("alice");
    String code = batch.get(1);

    try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
      CountDownLatch gate = new CountDownLatch(1);
      List<Callable<Boolean>> tasks = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        tasks.add(
            () -> {
              gate.await();
              return service.consumeCode("alice", code);
            });
      }
      List<Future<Boolean>> futures = new ArrayList<>();
      for (Callable<Boolean> task : tasks) {
        futures.add(pool.submit(task));
      }
      gate.countDown();
      int winners = 0;
      for (Future<Boolean> f : futures) {
        if (Boolean.TRUE.equals(f.get())) {
          winners++;
        }
      }
      assertEquals(1, winners);
    }
  }
}
