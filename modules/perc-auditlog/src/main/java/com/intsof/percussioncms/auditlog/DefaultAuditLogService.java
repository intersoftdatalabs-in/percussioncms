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

import com.intsof.percussioncms.auditlog.format.MessageTemplateFormatter;
import com.intsof.percussioncms.auditlog.redact.AuditRedactor;
import com.intsof.percussioncms.auditlog.sink.AuditLogSink;
import com.intsof.percussioncms.auditlog.sink.Log4jAuditLogSink;
import com.intsof.percussioncms.auditlog.sink.RepositoryAuditLogSink;
import com.intsof.percussioncms.auditlog.spi.AuditLogRepository;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default dual-write implementation: prepare messages, redact, then write to all configured sinks
 * (Log4j + repository by default). Business requests never fail solely because a sink failed.
 */
public final class DefaultAuditLogService implements AuditLogService {

  private static final Logger FAILURE_LOG =
      LogManager.getLogger("com.intsof.percussioncms.auditlog.sink-failure");

  private static final AuditLogId SKIPPED = AuditLogId.of("00000000-0000-0000-0000-000000000000");

  private final AuditRedactor redactor;
  private final Clock clock;
  private final Supplier<AuditLogId> idSupplier;
  private final List<AuditLogSink> sinks;
  private final AtomicLong sinkFailureCount = new AtomicLong();
  private final String serverNode;

  private DefaultAuditLogService(Builder builder) {
    this.redactor = Objects.requireNonNull(builder.redactor, "redactor");
    this.clock = Objects.requireNonNull(builder.clock, "clock");
    this.idSupplier = Objects.requireNonNull(builder.idSupplier, "idSupplier");
    this.sinks = List.copyOf(builder.sinks);
    this.serverNode = builder.serverNode;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Process-wide default: Log4j + in-process memory repository until Spring wires the JPA
   * repository for {@code PSX_SYSTEM_AUDIT_LOG}.
   */
  public static DefaultAuditLogService createDefault() {
    return builder()
        .addSink(new Log4jAuditLogSink())
        .addSink(new RepositoryAuditLogSink(ConcurrentMemoryAuditLogRepository.INSTANCE))
        .build();
  }

  public static DefaultAuditLogService create(AuditLogRepository repository) {
    return builder()
        .addSink(new Log4jAuditLogSink())
        .addSink(new RepositoryAuditLogSink(repository))
        .build();
  }

  public long sinkFailureCount() {
    return sinkFailureCount.get();
  }

  @Override
  public AuditLogId log(SystemErrorCode code, Object... params) {
    return log(code, AuditContext.empty(), code.defaultOutcome(), params);
  }

  @Override
  public AuditLogId log(SystemErrorCode code, AuditContext context, Object... params) {
    return log(code, context, code.defaultOutcome(), params);
  }

  @Override
  public AuditLogId log(
      SystemErrorCode code, AuditContext context, AuditOutcome outcome, Object... params) {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(outcome, "outcome");

    // Authoritative gate: never write audit for non-auditable codes
    if (!code.isAuditable()) {
      return SKIPPED;
    }
    if (code.eventType() == null) {
      FAILURE_LOG.error(
          "AUDIT_SINK_FAILURE auditable code {} missing eventType; skipping dual write",
          code.qualifiedCode());
      sinkFailureCount.incrementAndGet();
      return SKIPPED;
    }

    Object[] safeParams = redactor.redactParams(params == null ? new Object[0] : params);
    String userBody =
        redactor.redact(
            MessageTemplateFormatter.format(code.userMessageTemplate(), safeParams));
    String logBody =
        redactor.redact(MessageTemplateFormatter.format(code.logMessageTemplate(), safeParams));

    AuditLogId logId = idSupplier.get();
    String formattedLine = MessageTemplateFormatter.formatLine(code, logId, logBody);

    AuditRecord record =
        AuditRecord.builder()
            .logId(logId)
            .eventTime(Instant.now(clock))
            .code(code)
            .outcome(outcome)
            .actor(context.actor().map(redactor::redact).orElse(null))
            .target(context.target().map(redactor::redact).orElse(null))
            .sourceIp(context.sourceIp().orElse(null))
            .sourceHost(context.sourceHost().orElse(null))
            .sessionIdHash(context.sessionIdHash().orElse(null))
            .correlationId(context.correlationId().orElse(null))
            .userMessage(userBody)
            .logMessage(logBody)
            .formattedLine(formattedLine)
            .attributes(redactor.redactAttributes(context.attributes()))
            .serverNode(serverNode)
            .build();

    for (AuditLogSink sink : sinks) {
      try {
        sink.write(record);
      } catch (RuntimeException ex) {
        sinkFailureCount.incrementAndGet();
        FAILURE_LOG.error(
            "AUDIT_SINK_FAILURE sink={} logId={} code={}: {}",
            sink.name(),
            logId.value(),
            code.qualifiedCode(),
            ex.toString());
        FAILURE_LOG.debug("AUDIT_SINK_FAILURE details", ex);
      }
    }
    return logId;
  }

  public static final class Builder {
    private AuditRedactor redactor = new AuditRedactor();
    private Clock clock = Clock.systemUTC();
    private Supplier<AuditLogId> idSupplier = AuditLogId::generate;
    private final List<AuditLogSink> sinks = new ArrayList<>();
    private String serverNode;

    public Builder redactor(AuditRedactor redactor) {
      this.redactor = redactor;
      return this;
    }

    public Builder clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    public Builder idSupplier(Supplier<AuditLogId> idSupplier) {
      this.idSupplier = idSupplier;
      return this;
    }

    public Builder addSink(AuditLogSink sink) {
      this.sinks.add(Objects.requireNonNull(sink, "sink"));
      return this;
    }

    public Builder serverNode(String serverNode) {
      this.serverNode = serverNode;
      return this;
    }

    public DefaultAuditLogService build() {
      if (sinks.isEmpty()) {
        throw new IllegalStateException("At least one AuditLogSink is required");
      }
      return new DefaultAuditLogService(this);
    }
  }

  /** Holder for a process-wide instance that system wiring may replace. */
  public static final class Holder {
    private static volatile AuditLogService instance = createDefault();

    private Holder() {}

    public static AuditLogService get() {
      return instance;
    }

    public static void set(AuditLogService service) {
      instance = Objects.requireNonNull(service, "service");
    }

    public static void resetToDefault() {
      instance = createDefault();
    }
  }
}
