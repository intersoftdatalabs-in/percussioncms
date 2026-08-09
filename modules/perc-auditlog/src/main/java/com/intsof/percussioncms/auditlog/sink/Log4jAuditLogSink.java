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
package com.intsof.percussioncms.auditlog.sink;

import com.intsof.percussioncms.auditlog.AuditRecord;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Writes the canonical formatted audit line to Log4j (configured into {@code server.log} /
 * operational logs).
 */
public final class Log4jAuditLogSink implements AuditLogSink {

  /** Dedicated logger name so operators can filter or dual-bind appenders. */
  public static final String LOGGER_NAME = "com.intsof.percussioncms.auditlog";

  private final Logger logger;

  public Log4jAuditLogSink() {
    this(LogManager.getLogger(LOGGER_NAME));
  }

  /** Package-visible for tests. */
  Log4jAuditLogSink(Logger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @Override
  public String name() {
    return "log4j";
  }

  @Override
  public void write(AuditRecord record) {
    Objects.requireNonNull(record, "record");
    logger.info(
        "{} outcome={} actor={} target={}",
        record.formattedLine(),
        record.outcome(),
        record.actor().orElse("-"),
        record.target().orElse("-"));
  }
}
