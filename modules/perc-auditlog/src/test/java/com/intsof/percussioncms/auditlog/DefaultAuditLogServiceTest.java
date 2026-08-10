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
package com.intsof.percussioncms.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.AuditSubsystemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import com.intsof.percussioncms.auditlog.sink.AuditLogSink;
import com.intsof.percussioncms.auditlog.sink.CapturingAuditLogSink;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultAuditLogServiceTest {

  @Test
  void dualWritesSameLogIdToAllSinks() {
    CapturingAuditLogSink log4j = new CapturingAuditLogSink("log4j");
    CapturingAuditLogSink repo = new CapturingAuditLogSink("repository");
    AuditLogId fixed = AuditLogId.of("11111111-2222-3333-4444-555555555555");
    DefaultAuditLogService svc =
        DefaultAuditLogService.builder()
            .clock(Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC))
            .idSupplier(() -> fixed)
            .addSink(log4j)
            .addSink(repo)
            .build();

    AuditLogId id =
        svc.log(
            AuthenticationErrorCodes.LOGIN_SUCCESS,
            AuditContext.builder().actor("jdoe").sourceIp("10.0.0.1").build(),
            AuditOutcome.SUCCESS,
            "jdoe",
            "10.0.0.1");

    assertEquals(fixed, id);
    assertEquals(1, log4j.records().size());
    assertEquals(1, repo.records().size());
    assertEquals(fixed, log4j.records().get(0).logId());
    assertEquals(fixed, repo.records().get(0).logId());
    assertTrue(
        log4j.records()
            .get(0)
            .formattedLine()
            .startsWith("[AUTH-1001]-[11111111-2222-3333-4444-555555555555]"));
    assertEquals(AuditOutcome.SUCCESS, log4j.records().get(0).outcome());
  }

  @Test
  void nonAuditableCodesDoNotWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id = svc.log(AuthenticationErrorCodes.SESSION_CACHE_MISS, "key1");

    assertEquals("00000000-0000-0000-0000-000000000000", id.value());
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void sinkFailureDoesNotPreventOtherSinks() {
    CapturingAuditLogSink ok = new CapturingAuditLogSink("ok");
    CapturingAuditLogSink bad = new CapturingAuditLogSink("bad", true);
    DefaultAuditLogService svc =
        DefaultAuditLogService.builder().addSink(bad).addSink(ok).build();

    AuditLogId id =
        svc.log(
            AuthenticationErrorCodes.LOGIN_FAILURE,
            AuditContext.builder().actor("jdoe").build(),
            "jdoe",
            "password=hunter2",
            "1.2.3.4");

    assertEquals(1, ok.records().size());
    assertEquals(0, bad.records().size());
    assertEquals(id, ok.records().get(0).logId());
    assertEquals(1, svc.sinkFailureCount());
    assertTrue(
        !ok.records().get(0).logMessage().contains("hunter2"),
        "secret must be redacted even when one sink fails");
  }

  @Test
  void redactsSecretsInMessages() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    svc.log(
        AuthenticationErrorCodes.LOGIN_FAILURE,
        AuditContext.builder().actor("jdoe").build(),
        "jdoe",
        "password=hunter2",
        "10.0.0.1");

    String logMsg = sink.records().get(0).logMessage();
    assertTrue(logMsg.contains("[REDACTED]") || !logMsg.contains("hunter2"));
  }

  @Test
  void nestedLogDuringSinkWriteIsSkippedToPreventRecursion() {
    CapturingAuditLogSink cap = new CapturingAuditLogSink("cap");
    AtomicInteger nestedAttempts = new AtomicInteger();
    DefaultAuditLogService[] holder = new DefaultAuditLogService[1];
    AuditLogSink recursive =
        new AuditLogSink() {
          @Override
          public String name() {
            return "recursive";
          }

          @Override
          public void write(AuditRecord record) {
            nestedAttempts.incrementAndGet();
            // Nested dual-write (audit-of-audit storm path) must not write again.
            AuditLogId nested =
                holder[0].log(
                    AuditSubsystemErrorCodes.VIEWER_ACCESS,
                    AuditContext.builder().actor("nested").build(),
                    AuditOutcome.SUCCESS,
                    "nested",
                    "list",
                    "filters");
            assertEquals("00000000-0000-0000-0000-000000000000", nested.value());
          }
        };
    holder[0] =
        DefaultAuditLogService.builder().addSink(recursive).addSink(cap).build();

    AuditLogId outer =
        holder[0].log(
            AuditSubsystemErrorCodes.VIEWER_ACCESS,
            AuditContext.builder().actor("admin").build(),
            AuditOutcome.SUCCESS,
            "admin",
            "list",
            "module=AUTH");

    assertEquals(1, nestedAttempts.get());
    assertEquals(1, cap.records().size());
    assertEquals(outer, cap.records().get(0).logId());
    assertEquals(1, cap.records().get(0).code().numericCode());
  }

  @Test
  void auditSubsystemViewerAccessIsAuditable() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    svc.log(
        AuditSubsystemErrorCodes.VIEWER_ACCESS_DENIED,
        AuditContext.builder().actor("author").build(),
        AuditOutcome.FAILURE,
        "author",
        "list",
        "forbidden");

    assertEquals(1, sink.records().size());
    assertEquals(AuditSubsystemErrorCodes.VIEWER_ACCESS_DENIED, sink.records().get(0).code());
    assertEquals(AuditOutcome.FAILURE, sink.records().get(0).outcome());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[AUDIT-3]-"));
  }
}
