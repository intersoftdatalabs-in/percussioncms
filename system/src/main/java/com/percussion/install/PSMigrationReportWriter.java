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
package com.percussion.install;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

/**
 * Durable migration report under the install tree (FR-017, contracts/migration-observability.md).
 *
 * <p>Default path: {@code <install-root>/rxconfig/Installer/migration-report-&lt;component&gt;.properties}
 */
public final class PSMigrationReportWriter {

  public static final String REPORT_DIR_RELATIVE =
      Path.of("rxconfig", "Installer").toString();

  private PSMigrationReportWriter() {}

  /**
   * Snapshot of a migration attempt for durable storage.
   *
   * @param component component id (e.g. {@code CMS}, {@code DTS_metadata})
   * @param outcome terminal outcome
   * @param backupGate gate kind used
   * @param sourceBackend source backend label
   * @param targetBackend target backend label
   * @param failureReason optional failure reason (no secrets)
   * @param finishedAt finish timestamp
   */
  public record Report(
      String component,
      PSMigrationOutcome outcome,
      PSBackupGateKind backupGate,
      String sourceBackend,
      String targetBackend,
      String failureReason,
      Instant finishedAt) {}

  /**
   * Resolve default report path for a component.
   *
   * @param installRoot install root
   * @param component component id (sanitized for filename)
   * @return path; parent may not yet exist
   */
  public static Path reportPath(Path installRoot, String component) {
    Objects.requireNonNull(installRoot, "installRoot");
    Objects.requireNonNull(component, "component");
    String safe = component.replaceAll("[^A-Za-z0-9._-]", "_");
    return installRoot
        .resolve(REPORT_DIR_RELATIVE)
        .resolve("migration-report-" + safe + ".properties");
  }

  /**
   * Write a durable report file (overwrites previous).
   *
   * @param path destination path
   * @param report report content
   * @throws IOException on write failure
   */
  public static void write(Path path, Report report) throws IOException {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(report, "report");
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Properties props = new Properties();
    props.setProperty("component", nullToEmpty(report.component()));
    props.setProperty("outcome", report.outcome() == null ? "" : report.outcome().name());
    props.setProperty(
        "backupGate", report.backupGate() == null ? "" : report.backupGate().name());
    props.setProperty("sourceBackend", nullToEmpty(report.sourceBackend()));
    props.setProperty("targetBackend", nullToEmpty(report.targetBackend()));
    props.setProperty(
        "failureReason",
        PSMigrationSecretsRedactor.redact(nullToEmpty(report.failureReason())));
    props.setProperty(
        "finishedAt",
        report.finishedAt() == null ? Instant.now().toString() : report.finishedAt().toString());
    try (OutputStream out = Files.newOutputStream(path)) {
      props.store(out, "Percussion CMS embedded repository migration report (#548) — no secrets");
    }
  }

  /**
   * Read a previously written report.
   *
   * @param path report path
   * @return parsed report
   * @throws IOException if missing or unreadable
   */
  public static Report read(Path path) throws IOException {
    Objects.requireNonNull(path, "path");
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      props.load(in);
    }
    return new Report(
        props.getProperty("component", ""),
        PSMigrationOutcome.fromString(props.getProperty("outcome")),
        PSBackupGateKind.fromString(props.getProperty("backupGate")),
        props.getProperty("sourceBackend", ""),
        props.getProperty("targetBackend", ""),
        props.getProperty("failureReason", ""),
        parseInstant(props.getProperty("finishedAt")));
  }

  private static Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value.trim());
    } catch (Exception e) {
      return null;
    }
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
