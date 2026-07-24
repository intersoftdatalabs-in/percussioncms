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

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates CMS product-managed Derby → H2 migration on upgrade (#548).
 *
 * <p>This class implements the control plane: detect, backup gate, exclusive lock, durable report,
 * and skip outcomes. Data pump / schema create / multi-file cutover are wired in subsequent tasks
 * (T058–T060); until then, a Derby detection that passes the gate records {@link
 * PSMigrationOutcome#FAILED} with a clear reason rather than cutting over live config.
 *
 * <p>See contracts/migration-upgrade.md and contracts/migration-observability.md.
 */
public class PSEmbeddedRepositoryMigrator {

  private static final Logger LOG =
      Logger.getLogger(PSEmbeddedRepositoryMigrator.class.getName());

  public static final String COMPONENT_CMS = "CMS";

  /** Relative path to repository properties under install root. */
  public static final String RXREPOSITORY_RELATIVE =
      Path.of("rxconfig", "Installer", "rxrepository.properties").toString();

  private final Path installRoot;
  private final Properties systemProperties;
  private final boolean productBackupSucceeded;
  private final Path productBackupRoot;

  /**
   * @param installRoot CMS install root
   * @param systemProperties typically {@link System#getProperties()} (or test fixture)
   * @param productBackupSucceeded whether FR-018a product backup already completed
   * @param productBackupRoot optional path of product backup (for logging); may be null
   */
  public PSEmbeddedRepositoryMigrator(
      Path installRoot,
      Properties systemProperties,
      boolean productBackupSucceeded,
      Path productBackupRoot) {
    this.installRoot = Objects.requireNonNull(installRoot, "installRoot").toAbsolutePath().normalize();
    this.systemProperties = Objects.requireNonNull(systemProperties, "systemProperties");
    this.productBackupSucceeded = productBackupSucceeded;
    this.productBackupRoot = productBackupRoot;
  }

  /**
   * Run detection, gate, lock, and (when implemented) pump/cutover. Always writes a durable report
   * for terminal outcomes.
   *
   * @return terminal migration outcome
   */
  public PSMigrationOutcome migrate() {
    String sourceBackend = "UNKNOWN";
    String targetBackend = PSJdbcUtils.H2_DB_BACKEND;
    PSBackupGateKind gateKind = PSBackupGateKind.NOT_SATISFIED;
    PSMigrationOutcome outcome;
    String failureReason = null;

    try {
      Properties repoProps = loadRepositoryProperties();
      PSEmbeddedRepositoryDetector.Classification classification =
          PSEmbeddedRepositoryDetector.classify(repoProps);
      sourceBackend = resolveSourceLabel(repoProps, classification);

      PSMigrationOutcome skip = PSEmbeddedRepositoryDetector.toSkipOutcome(classification);
      if (skip != null) {
        outcome = skip;
        gateKind = PSBackupGateKind.NOT_SATISFIED;
        writeReport(outcome, gateKind, sourceBackend, targetBackend, null);
        logOutcome(outcome, "skip path for classification=" + classification);
        return outcome;
      }

      gateKind = PSRepositoryBackupGate.evaluate(productBackupSucceeded, systemProperties);
      if (!PSRepositoryBackupGate.isSatisfied(gateKind)) {
        outcome = PSMigrationOutcome.BLOCKED_BACKUP_GATE;
        failureReason =
            "Backup gate not satisfied. Complete product offline backup or set "
                + PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY
                + "=true after verifying an external backup.";
        writeReport(outcome, gateKind, sourceBackend, targetBackend, failureReason);
        logOutcome(outcome, failureReason);
        return outcome;
      }

      try (PSMigratorLock lock = PSMigratorLock.tryAcquire(installRoot)) {
        LOG.info(
            () ->
                "component="
                    + COMPONENT_CMS
                    + " migration lock acquired path="
                    + lock.getLockPath());

        // T058–T060: schema create, FK-safe pump, validation, multi-file cutover.
        // Until data pump is implemented, fail safely without touching live config.
        outcome = PSMigrationOutcome.FAILED;
        failureReason =
            "Derby→H2 data pump and cutover not yet implemented (US2 T058–T060). "
                + "Backup gate satisfied ("
                + gateKind
                + "); source Derby repository left intact."
                + (productBackupRoot != null ? " productBackupRoot=" + productBackupRoot : "");
        writeReport(outcome, gateKind, sourceBackend, targetBackend, failureReason);
        logOutcome(outcome, failureReason);
        return outcome;
      } catch (PSMigratorLock.MigratorLockException e) {
        outcome = PSMigrationOutcome.FAILED;
        failureReason = PSMigrationSecretsRedactor.redact(e.getMessage());
        writeReport(outcome, gateKind, sourceBackend, targetBackend, failureReason);
        logOutcome(outcome, failureReason);
        return outcome;
      }
    } catch (Exception e) {
      outcome = PSMigrationOutcome.FAILED;
      failureReason =
          PSMigrationSecretsRedactor.redact(
              e.getClass().getSimpleName() + ": " + e.getMessage());
      try {
        writeReport(outcome, gateKind, sourceBackend, targetBackend, failureReason);
      } catch (Exception writeEx) {
        LOG.log(Level.SEVERE, "Failed to write migration report", writeEx);
      }
      logOutcome(outcome, failureReason);
      return outcome;
    }
  }

  /**
   * Load {@code rxrepository.properties} from the install root.
   *
   * @return properties; never null
   * @throws IOException if missing or unreadable
   */
  public Properties loadRepositoryProperties() throws IOException {
    Path propsPath = installRoot.resolve(RXREPOSITORY_RELATIVE);
    if (!Files.isRegularFile(propsPath)) {
      throw new IOException("Repository properties not found: " + propsPath);
    }
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(propsPath)) {
      props.load(in);
    }
    return props;
  }

  private void writeReport(
      PSMigrationOutcome outcome,
      PSBackupGateKind gate,
      String sourceBackend,
      String targetBackend,
      String failureReason)
      throws IOException {
    Path path = PSMigrationReportWriter.reportPath(installRoot, COMPONENT_CMS);
    PSMigrationReportWriter.write(
        path,
        new PSMigrationReportWriter.Report(
            COMPONENT_CMS,
            outcome,
            gate,
            sourceBackend,
            targetBackend,
            failureReason,
            Instant.now()));
    LOG.info(() -> "component=" + COMPONENT_CMS + " migration report written path=" + path);
  }

  private static String resolveSourceLabel(
      Properties repoProps, PSEmbeddedRepositoryDetector.Classification classification) {
    String backend = repoProps.getProperty(PSEmbeddedRepositoryDetector.KEY_DB_BACKEND);
    if (backend != null && !backend.isBlank()) {
      return backend.trim();
    }
    return switch (classification) {
      case PRODUCT_MANAGED_DERBY -> PSJdbcUtils.DERBY_DB_BACKEND;
      case ALREADY_H2 -> PSJdbcUtils.H2_DB_BACKEND;
      case NON_DERBY -> "EXTERNAL";
    };
  }

  private static void logOutcome(PSMigrationOutcome outcome, String detail) {
    String safe = PSMigrationSecretsRedactor.redact(detail);
    LOG.info(
        () ->
            "component="
                + COMPONENT_CMS
                + " outcome="
                + outcome
                + (safe == null || safe.isBlank() ? "" : " detail=" + safe));
  }
}
