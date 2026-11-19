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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.percussion.services.auth.IPSAuthSecurityDao;
import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSMfaTotp;
import com.percussion.services.auth.mfa.PSTotpEnrollmentStart;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;

class PSTotpServiceTest {

  private final PSTotpSecretCrypto crypto =
      new PSTotpSecretCrypto(() -> "test-key-material-for-totp".getBytes(StandardCharsets.UTF_8));

  @Test
  void enrollConfirmVerifyAndAtomicReplayProtectionWork() throws Exception {
    IPSAuthSecurityDao dao = mock(IPSAuthSecurityDao.class);
    Map<String, PSMfaTotp> store = new ConcurrentHashMap<>();
    AtomicReference<PSAccountSecurity> secRef = new AtomicReference<>();

    when(dao.saveTotp(any(PSMfaTotp.class)))
        .thenAnswer(
            inv -> {
              PSMfaTotp t = inv.getArgument(0);
              store.put(t.getTotpId(), t);
              return t;
            });
    when(dao.findTotpById(any()))
        .thenAnswer(inv -> Optional.ofNullable(store.get(inv.<String>getArgument(0))));
    when(dao.findTotpByUserAndStatus(eq("alice"), eq(PSMfaTotp.STATUS_ACTIVE)))
        .thenAnswer(
            inv ->
                store.values().stream()
                    .filter(
                        t ->
                            "alice".equals(t.getUserName())
                                && PSMfaTotp.STATUS_ACTIVE.equals(t.getStatus()))
                    .toList());
    when(dao.findActiveTotpByUser("alice"))
        .thenAnswer(
            inv ->
                store.values().stream()
                    .filter(
                        t ->
                            "alice".equals(t.getUserName())
                                && PSMfaTotp.STATUS_ACTIVE.equals(t.getStatus()))
                    .toList());
    when(dao.findAccountSecurity("alice")).thenAnswer(inv -> Optional.ofNullable(secRef.get()));
    when(dao.saveAccountSecurity(any(PSAccountSecurity.class)))
        .thenAnswer(
            inv -> {
              PSAccountSecurity s = inv.getArgument(0);
              secRef.set(s);
              return s;
            });
    when(dao.advanceTotpLastUsedStepAtomically(any(), anyLong(), any()))
        .thenAnswer(
            inv -> {
              String id = inv.getArgument(0);
              long step = inv.getArgument(1);
              PSMfaTotp row = store.get(id);
              if (row == null) {
                return 0;
              }
              synchronized (row) {
                if (PSMfaTotp.STATUS_ACTIVE.equals(row.getStatus())
                    && (row.getLastUsedStep() == null || row.getLastUsedStep() < step)) {
                  row.setLastUsedStep(step);
                  return 1;
                }
                return 0;
              }
            });

    Instant baseTime = Instant.parse("2026-11-18T12:00:00Z");
    AtomicReference<Instant> clockRef = new AtomicReference<>(baseTime);
    Clock clock =
        new Clock() {
          @Override
          public ZoneOffset getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            return clockRef.get();
          }
        };

    Properties props = new Properties();
    PSTotpService service = new PSTotpService(dao, crypto, () -> props, clock);

    PSTotpEnrollmentStart start = service.startEnrollment("alice", "Phone");
    assertFalse(start.toString().contains(start.manualSecret()));
    assertTrue(start.otpauthUri().startsWith("otpauth://totp/"));

    String codeAtBase = generateCode(start.manualSecret(), baseTime);
    assertFalse(service.confirmEnrollment("alice", start.totpId(), "000001".equals(codeAtBase) ? "000002" : "000001"));
    assertTrue(service.confirmEnrollment("alice", start.totpId(), codeAtBase));
    assertEquals(PSAccountSecurity.MFA_STATE_ENROLLED, secRef.get().getMfaEnrollmentState());

    // Same step replay immediately rejected
    assertFalse(service.verifyActiveCode("alice", codeAtBase));

    // Next 30s step succeeds concurrently ONLY ONCE across racing threads
    Instant nextStep = baseTime.plusSeconds(30);
    clockRef.set(nextStep);
    String nextCode = generateCode(start.manualSecret(), nextStep);

    try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
      CountDownLatch gate = new CountDownLatch(1);
      List<Callable<Boolean>> tasks = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        tasks.add(
            () -> {
              gate.await();
              return service.verifyActiveCode("alice", nextCode);
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

  private static String generateCode(String base32Secret, Instant when) throws Exception {
    byte[] secret = new Base32().decode(base32Secret);
    TimeBasedOneTimePasswordGenerator gen =
        new TimeBasedOneTimePasswordGenerator(
            Duration.ofSeconds(30), 6, TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
    return gen.generateOneTimePasswordString(new SecretKeySpec(secret, "HmacSHA1"), when);
  }
}
