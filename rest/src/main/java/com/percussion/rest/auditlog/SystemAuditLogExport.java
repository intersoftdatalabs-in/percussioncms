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
package com.percussion.rest.auditlog;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Formats {@link SystemAuditLogEntry} lists for downloadable CSV/JSON export (#2715 / Phase 5).
 *
 * <p>CSV is RFC 4180-style (comma-separated, double-quote escaping, CRLF record separators). JSON is
 * a root array of entry objects with ISO-8601 instants (no root wrapper).
 */
public final class SystemAuditLogExport {

  /** Default max rows when the client omits or passes a non-positive limit. */
  public static final int DEFAULT_MAX_ROWS = 5_000;

  /** Hard cap on export size (resource/adaptor clamp). */
  public static final int MAX_ROWS = 10_000;

  /** CSV column header order (stable for consumers). */
  public static final String[] CSV_HEADERS = {
    "auditId",
    "eventTime",
    "moduleCode",
    "messageCode",
    "eventType",
    "outcome",
    "actor",
    "target",
    "sourceIp",
    "sourceHost",
    "userMessage",
    "logMessage",
    "correlationId",
    "attributesJson",
    "serverNode"
  };

  private static final JsonMapper JSON =
      JsonMapper.builder()
          .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(SerializationFeature.WRAP_ROOT_VALUE)
          .build();

  private SystemAuditLogExport() {}

  /**
   * Clamp requested export size to {@code 1..MAX_ROWS}; non-positive → {@link #DEFAULT_MAX_ROWS}.
   */
  public static int clampMaxRows(int maxRows) {
    if (maxRows <= 0) {
      return DEFAULT_MAX_ROWS;
    }
    return Math.min(maxRows, MAX_ROWS);
  }

  /** Serialize entries as a JSON array string. */
  public static String toJson(List<SystemAuditLogEntry> entries) {
    Objects.requireNonNull(entries, "entries");
    try {
      return JSON.writeValueAsString(entries);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize audit log export as JSON", e);
    }
  }

  /** Serialize entries as RFC 4180 CSV with a header row. */
  public static String toCsv(List<SystemAuditLogEntry> entries) {
    Objects.requireNonNull(entries, "entries");
    StringBuilder sb = new StringBuilder(Math.max(256, entries.size() * 128));
    appendCsvRow(sb, CSV_HEADERS);
    for (SystemAuditLogEntry e : entries) {
      if (e == null) {
        continue;
      }
      appendCsvRow(
          sb,
          new String[] {
            nullToEmpty(e.getAuditId()),
            instantToEmpty(e.getEventTime()),
            nullToEmpty(e.getModuleCode()),
            e.getMessageCode() == null ? "" : Integer.toString(e.getMessageCode()),
            nullToEmpty(e.getEventType()),
            nullToEmpty(e.getOutcome()),
            nullToEmpty(e.getActor()),
            nullToEmpty(e.getTarget()),
            nullToEmpty(e.getSourceIp()),
            nullToEmpty(e.getSourceHost()),
            nullToEmpty(e.getUserMessage()),
            nullToEmpty(e.getLogMessage()),
            nullToEmpty(e.getCorrelationId()),
            nullToEmpty(e.getAttributesJson()),
            nullToEmpty(e.getServerNode())
          });
    }
    return sb.toString();
  }

  static String escapeCsvField(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    boolean needsQuotes =
        value.indexOf(',') >= 0
            || value.indexOf('"') >= 0
            || value.indexOf('\n') >= 0
            || value.indexOf('\r') >= 0;
    if (!needsQuotes) {
      return value;
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  private static void appendCsvRow(StringBuilder sb, String[] fields) {
    for (int i = 0; i < fields.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(escapeCsvField(fields[i]));
    }
    sb.append("\r\n");
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static String instantToEmpty(Instant instant) {
    return instant == null ? "" : instant.toString();
  }

  /** Normalize format query value to {@code csv} or {@code json}; null/blank defaults to json. */
  public static String normalizeFormat(String format) {
    if (format == null || format.isBlank()) {
      return "json";
    }
    String f = format.trim().toLowerCase(Locale.ROOT);
    if ("csv".equals(f) || "json".equals(f)) {
      return f;
    }
    throw new IllegalArgumentException("format must be csv or json, got: " + format);
  }
}
