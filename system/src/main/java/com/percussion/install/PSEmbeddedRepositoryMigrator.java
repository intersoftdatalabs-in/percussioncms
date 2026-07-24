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
 * <p>Control plane: detect, backup gate, exclusive lock, durable report. Data plane (T058–T060):
 * optional product offline backup, disk precheck, JDBC schema create + FK-safe pump, validation,
 * multi-file cutover. Safe-fail leaves Derby config and data intact until successful cutover.
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

  /** Minimum free space multiplier vs estimated source size for disk precheck. */
  private static final long DISK_FACTOR_NUM = 3;

  private static final long DISK_FACTOR_DEN = 1;
  private static final long DISK_MIN_BYTES = 64L * 1024 * 1024;

  private final Path installRoot;
  private final Properties systemProperties;
  private final boolean productBackupSucceeded;
  private final Path productBackupRoot;
  private final boolean performProductBackupIfNeeded;

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
    this(installRoot, systemProperties, productBackupSucceeded, productBackupRoot, false);
  }

  /**
   * @param performProductBackupIfNeeded when true and gate would otherwise need external confirm,
   *     attempt offline product backup of resolved source directory first
   */
  public PSEmbeddedRepositoryMigrator(
      Path installRoot,
      Properties systemProperties,
      boolean productBackupSucceeded,
      Path productBackupRoot,
      boolean performProductBackupIfNeeded) {
    this.installRoot =
        Objects.requireNonNull(installRoot, "installRoot").toAbsolutePath().normalize();
    this.systemProperties = Objects.requireNonNull(systemProperties, "systemProperties");
    this.productBackupSucceeded = productBackupSucceeded;
    this.productBackupRoot = productBackupRoot;
    this.performProductBackupIfNeeded = performProductBackupIfNeeded;
  }

  /**
   * Run detection, gate, lock, pump, validation, and cutover. Always writes a durable report for
   * terminal outcomes.
   *
   * @return terminal migration outcome
   */
  public PSMigrationOutcome migrate() {
    String sourceBackend = "UNKNOWN";
    String targetBackend = PSJdbcUtils.H2_DB_BACKEND;
    PSBackupGateKind gateKind = PSBackupGateKind.NOT_SATISFIED;
    PSMigrationOutcome outcome;
    String failureReason = null;
    boolean backupOk = productBackupSucceeded;
    Path backupRoot = productBackupRoot;

    try {
      Properties repoProps = loadRepositoryProperties();
      PSEmbeddedRepositoryDetector.Classification classification =
          PSEmbeddedRepositoryDetector.classify(repoProps);
      sourceBackend = resolveSourceLabel(repoProps, classification);

      PSMigrationOutcome skip = PSEmbeddedRepositoryDetector.toSkipOutcome(classification);
      if (skip != null) {
        outcome = skip;
        gateKind = PSBackupGateKind.NOT_EVALUATED;
        writeReport(outcome, gateKind, sourceBackend, targetBackend, null);
        logOutcome(outcome, "skip path for classification=" + classification);
        return outcome;
      }

      gateKind = PSRepositoryBackupGate.evaluate(backupOk, systemProperties);
      if (!PSRepositoryBackupGate.isSatisfied(gateKind)
          && performProductBackupIfNeeded
          && !backupOk) {
        try {
          backupRoot = performProductBackup(repoProps);
          backupOk = true;
          gateKind = PSBackupGateKind.PRODUCT_BACKUP;
        } catch (Exception backupEx) {
          failureReason =
              "Product offline backup failed: "
                  + PSMigrationSecretsRedactor.redact(backupEx.getMessage());
          outcome = PSMigrationOutcome.BLOCKED_BACKUP_GATE;
          // Gate was not satisfied — do not record PRODUCT_BACKUP (that means success)
          writeReport(
              outcome, PSBackupGateKind.NOT_SATISFIED, sourceBackend, targetBackend, failureReason);
          logOutcome(outcome, failureReason);
          return outcome;
        }
      }

      gateKind = PSRepositoryBackupGate.evaluate(backupOk, systemProperties);
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

        Path h2Base = PSRepositoryConnectionHelper.defaultH2DatabaseBase(installRoot);
        if (!diskPrecheck(h2Base.getParent() != null ? h2Base.getParent() : installRoot)) {
          outcome = PSMigrationOutcome.FAILED;
          failureReason =
              "Insufficient disk space for target repository + working space (QC-021). "
                  + "Source Derby left intact.";
          writeReport(outcome, gateKind, sourceBackend, targetBackend, failureReason);
          logOutcome(outcome, failureReason);
          return outcome;
        }

        Properties h2Props =
            PSRepositoryConnectionHelper.buildH2TargetProperties(installRoot, h2Base);
        h2Props.remove("INSTALL_ROOT_HINT");

        Files.createDirectories(
            h2Base.getParent() != null ? h2Base.getParent() : installRoot);

        // Resolve source DB_SERVER paths for TableFactory (file-relative / bare paths)
        Properties sourceProps = new Properties();
        sourceProps.putAll(repoProps);
        String server =
            sourceProps.getProperty(PSRepositoryConnectionHelper.KEY_DB_SERVER, "");
        String driver =
            sourceProps.getProperty(PSRepositoryConnectionHelper.KEY_DB_DRIVER_NAME, "");
        sourceProps.setProperty(
            PSRepositoryConnectionHelper.KEY_DB_SERVER,
            PSRepositoryConnectionHelper.resolveServerFragment(server, installRoot, driver));

        Path stagingDir =
            installRoot
                .resolve("PreInstall")
                .resolve("tablefactory-migration")
                .resolve(Long.toString(System.currentTimeMillis()));

        PSTableFactoryMigrationTransfer.Result transfer =
            PSTableFactoryMigrationTransfer.exportThenImport(
                sourceProps, h2Props, stagingDir);
        LOG.info(
            () ->
                "component="
                    + COMPONENT_CMS
                    + " TableFactory export/import tablesExported="
                    + transfer.tablesExported()
                    + " tablesImported="
                    + transfer.tablesImported()
                    + " staging="
                    + transfer.stagingDir());

        // Post-import smoke: open target and ensure at least tableDef tables landed
        try (java.sql.Connection target =
            PSRepositoryConnectionHelper.open(h2Props, installRoot)) {
          PSMigrationValidator.Result validation =
              PSMigrationValidator.validateTargetOnly(target, transfer.tablesImported());
          if (!validation.passed()) {
            outcome = PSMigrationOutcome.FAILED;
            failureReason =
                "Post-import validation failed before cutover: "
                    + validation.summary()
                    + ". Source Derby left intact; target H2 not live.";
            writeReport(outcome, gateKind, sourceBackend, targetBackend, failureReason);
            logOutcome(outcome, failureReason);
            return outcome;
          }

          PSConfigCutover.cutoverToH2(installRoot, h2Props);

          outcome = PSMigrationOutcome.SUCCESS;
          writeReport(
              outcome,
              gateKind,
              sourceBackend,
              targetBackend,
              "tableFactoryExport="
                  + transfer.tablesExported()
                  + "; tableFactoryImport="
                  + transfer.tablesImported()
                  + "; "
                  + validation.summary()
                  + (backupRoot != null ? "; productBackupRoot=" + backupRoot : ""));
          logOutcome(outcome, "migration SUCCESS via TableFactory export/import; Derby residue retained (FR-019)");
          return outcome;
        }
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
          PSMigrationSecretsRedactor.redact(e.getClass().getSimpleName() + ": " + e.getMessage());
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

  private Path performProductBackup(Properties repoProps) throws IOException {
    Path backupRoot =
        installRoot
            .resolve("PreInstall")
            .resolve("migration-backup")
            .resolve(Long.toString(System.currentTimeMillis()));
    Path sourceDir = guessSourceDataDir(repoProps);
    Path companion = installRoot.resolve(RXREPOSITORY_RELATIVE);
    if (sourceDir != null && Files.isDirectory(sourceDir)) {
      PSRepositoryOfflineBackup.copyRepositoryTree(sourceDir, backupRoot, companion);
    } else {
      // Config-only backup when data dir cannot be resolved (e.g. networked Derby without local
      // path). Still records companion config for operators.
      Files.createDirectories(backupRoot.resolve("companion-config"));
      if (Files.isRegularFile(companion)) {
        Files.copy(
            companion,
            backupRoot.resolve("companion-config").resolve(companion.getFileName().toString()));
      }
      Files.writeString(
          backupRoot.resolve("README.txt"),
          "Source repository data directory could not be resolved from DB_SERVER; "
              + "companion config backed up only. Prefer external backup confirmation for "
              + "networked Derby.\n");
    }
    LOG.info(() -> "Product offline backup written to " + backupRoot);
    return backupRoot;
  }

  private Path guessSourceDataDir(Properties repoProps) {
    String server = repoProps.getProperty(PSRepositoryConnectionHelper.KEY_DB_SERVER, "");
    if (server.isBlank()) {
      return installRoot.resolve("Repository");
    }
    String trimmed = server.trim();
    // H2 / file-URL form: file:/abs/path/CMDB;params or file:relative;params
    if (trimmed.regionMatches(true, 0, "file:", 0, 5)) {
      String pathAndParams = trimmed.substring(5);
      int semi = pathAndParams.indexOf(';');
      String pathPart = semi >= 0 ? pathAndParams.substring(0, semi) : pathAndParams;
      if (pathPart == null || pathPart.isBlank()) {
        return installRoot.resolve("Repository");
      }
      Path p;
      try {
        p = Path.of(pathPart);
      } catch (Exception e) {
        return installRoot.resolve("Repository");
      }
      if (!p.isAbsolute()) {
        Path base = installRoot.resolve("jetty").resolve("base");
        if (!java.nio.file.Files.isDirectory(base)) {
          base = installRoot;
        }
        p = base.resolve(pathPart).normalize();
      }
      // Parent of CMDB file base is the repository data directory when present
      Path parent = p.getParent();
      return parent != null ? parent : p;
    }
    // Networked Derby ClientDriver form //host:port/... — no local data dir
    if (trimmed.startsWith("//")) {
      return installRoot.resolve("Repository");
    }
    // Embedded directory-style server under Repository
    Path p = Path.of(trimmed.split(";")[0]);
    if (!p.isAbsolute()) {
      return installRoot.resolve("Repository").resolve(p);
    }
    return p;
  }

  private boolean diskPrecheck(Path volumePath) {
    try {
      long required = DISK_MIN_BYTES;
      Path repo = installRoot.resolve("Repository");
      if (Files.isDirectory(repo)) {
        long size = dirSize(repo);
        required = Math.max(required, (size * DISK_FACTOR_NUM) / DISK_FACTOR_DEN + DISK_MIN_BYTES);
      }
      return PSRepositoryOfflineBackup.hasSufficientDiskSpace(volumePath, required);
    } catch (IOException e) {
      LOG.warning("Disk precheck failed open: " + e.getMessage());
      return false;
    }
  }

  private static long dirSize(Path root) throws IOException {
    try (var walk = Files.walk(root)) {
      return walk.filter(Files::isRegularFile)
          .mapToLong(
              p -> {
                try {
                  return Files.size(p);
                } catch (IOException e) {
                  return 0L;
                }
              })
          .sum();
    }
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
