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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Migrator state-machine outcomes, secrets redaction, durable report (T044 / T052 / QC-010 / QC-022
 * / FR-017).
 */
@Tag("UnitTest")
public class PSEmbeddedRepositoryMigratorTest {

  @TempDir Path installRoot;

  @Test
  void skippedNonDerbyForMysql() throws Exception {
    writeRepo(
        """
        DB_BACKEND=MYSQL
        DB_DRIVER_NAME=mysql
        DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
        DB_SERVER=localhost:3306/cms
        """);
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, new Properties(), false, null).migrate();
    assertEquals(PSMigrationOutcome.SKIPPED_NON_DERBY, outcome);
    assertReportOutcome(PSMigrationOutcome.SKIPPED_NON_DERBY);
  }

  @Test
  void alreadyMigratedForH2() throws Exception {
    writeRepo(
        """
        DB_BACKEND=H2
        DB_DRIVER_NAME=h2
        DB_DRIVER_CLASS_NAME=org.h2.Driver
        DB_SERVER=file:../../Repository/CMDB
        """);
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, new Properties(), false, null).migrate();
    assertEquals(PSMigrationOutcome.ALREADY_MIGRATED, outcome);
    assertReportOutcome(PSMigrationOutcome.ALREADY_MIGRATED);
  }

  @Test
  void blockedBackupGateWhenNeitherSatisfied() throws Exception {
    writeDerbyRepo();
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, new Properties(), false, null).migrate();
    assertEquals(PSMigrationOutcome.BLOCKED_BACKUP_GATE, outcome);
    PSMigrationReportWriter.Report report =
        PSMigrationReportWriter.read(
            PSMigrationReportWriter.reportPath(
                installRoot, PSEmbeddedRepositoryMigrator.COMPONENT_CMS));
    assertEquals(PSBackupGateKind.NOT_SATISFIED, report.backupGate());
    assertNotNull(report.failureReason());
    assertFalse(report.failureReason().isBlank());
  }

  @Test
  void externalConfirmUnblocksGateThenFailsSafelyWhenSourceUnreachable() throws Exception {
    writeDerbyRepo();
    Properties sys = new Properties();
    sys.setProperty(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY, "true");
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, sys, false, null).migrate();
    // Gate opens; fake //localhost:1527 source is unreachable → FAILED, no cutover.
    assertEquals(PSMigrationOutcome.FAILED, outcome);
    PSMigrationReportWriter.Report report =
        PSMigrationReportWriter.read(
            PSMigrationReportWriter.reportPath(
                installRoot, PSEmbeddedRepositoryMigrator.COMPONENT_CMS));
    assertEquals(PSBackupGateKind.EXTERNAL_CONFIRM, report.backupGate());
    assertEquals("DERBY", report.sourceBackend());
    assertEquals("H2", report.targetBackend());
    // Live config still Derby (no cutover)
    Properties live = new Properties();
    try (var in =
        Files.newInputStream(
            installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE))) {
      live.load(in);
    }
    assertEquals("DERBY", live.getProperty("DB_BACKEND"));
  }

  @Test
  void productBackupUnblocksGateThenFailsWhenSourceUnreachable() throws Exception {
    writeDerbyRepo();
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, new Properties(), true, null).migrate();
    assertEquals(PSMigrationOutcome.FAILED, outcome);
    PSMigrationReportWriter.Report report =
        PSMigrationReportWriter.read(
            PSMigrationReportWriter.reportPath(
                installRoot, PSEmbeddedRepositoryMigrator.COMPONENT_CMS));
    assertEquals(PSBackupGateKind.PRODUCT_BACKUP, report.backupGate());
  }

  /**
   * Upgrade auto product backup (FR-018a) must succeed when the instance is offline even if stale
   * Derby lock markers remain from an unclean prior stop — those markers must not require {@code
   * perc.migration.externalBackupConfirmed}. Live locks are cleared and omitted from the backup
   * artifact so restore cannot reintroduce startup blockers.
   */
  @Test
  void autoProductBackupClearsStaleLockMarkersWhenOffline() throws Exception {
    writeRepo(
        """
        DB_BACKEND=DERBY
        DB_DRIVER_NAME=derby
        DB_DRIVER_CLASS_NAME=org.apache.derby.jdbc.EmbeddedDriver
        DB_SERVER=CMDB;create=true
        """);
    Path cmdb = installRoot.resolve("Repository").resolve("CMDB");
    Files.createDirectories(cmdb);
    Files.writeString(cmdb.resolve("seg0"), "data", StandardCharsets.UTF_8);
    Files.writeString(cmdb.resolve("db.lck"), "stale", StandardCharsets.UTF_8);
    Files.writeString(cmdb.resolve("dbex.lck"), "stale", StandardCharsets.UTF_8);

    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, new Properties(), false, null, true)
            .migrate();

    // Gate must open via product backup; source is not a real Derby store so pump fails safely.
    assertEquals(PSMigrationOutcome.FAILED, outcome);
    PSMigrationReportWriter.Report report =
        PSMigrationReportWriter.read(
            PSMigrationReportWriter.reportPath(
                installRoot, PSEmbeddedRepositoryMigrator.COMPONENT_CMS));
    assertEquals(PSBackupGateKind.PRODUCT_BACKUP, report.backupGate());
    assertNotNull(report.failureReason());
    assertFalse(
        report.failureReason().toLowerCase().contains("backup gate not satisfied"),
        "must not block on backup gate when offline auto-backup ran: " + report.failureReason());

    // Live source locks cleared for subsequent open/restore cleanliness
    assertFalse(Files.exists(cmdb.resolve("db.lck")));
    assertFalse(Files.exists(cmdb.resolve("dbex.lck")));
    assertTrue(Files.isRegularFile(cmdb.resolve("seg0")));

    // Product backup tree exists and must not contain lock markers
    Path backupParent = installRoot.resolve("PreInstall").resolve("migration-backup");
    assertTrue(Files.isDirectory(backupParent), "migration-backup dir expected");
    try (var backups = Files.list(backupParent)) {
      Path backupDir =
          backups
              .filter(Files::isDirectory)
              .findFirst()
              .orElseThrow(() -> new AssertionError("at least one product backup timestamp dir"));
      assertTrue(
          PSRepositoryOfflineBackup.findLiveMarkers(backupDir).isEmpty(),
          "backup must not archive engine lock markers");
      assertTrue(
          Files.isRegularFile(backupDir.resolve("repository-data").resolve("seg0")),
          "repository data must still be backed up");
    }
  }

  @Test
  void detectsClientDriverAsDerby() {
    Properties p = new Properties();
    p.setProperty("DB_BACKEND", "DERBY");
    p.setProperty("DB_DRIVER_NAME", "derby");
    p.setProperty("DB_DRIVER_CLASS_NAME", "org.apache.derby.jdbc.ClientDriver");
    assertEquals(
        PSEmbeddedRepositoryDetector.Classification.PRODUCT_MANAGED_DERBY,
        PSEmbeddedRepositoryDetector.classify(p));
    assertNull(
        PSEmbeddedRepositoryDetector.toSkipOutcome(
            PSEmbeddedRepositoryDetector.Classification.PRODUCT_MANAGED_DERBY));
  }

  @Test
  void redactorRemovesPasswordTokens() {
    String raw = "PWD=s3cret password=topsecret jdbc:mysql://u:p@host/db";
    String redacted = PSMigrationSecretsRedactor.redact(raw);
    assertFalse(redacted.contains("s3cret"));
    assertFalse(redacted.contains("topsecret"));
    assertTrue(redacted.contains("****"));
    assertTrue(PSMigrationSecretsRedactor.appearsToContainSecret(raw));
    assertFalse(PSMigrationSecretsRedactor.appearsToContainSecret(redacted));
  }

  @Test
  void outcomeEnumRoundTrip() {
    for (PSMigrationOutcome o : PSMigrationOutcome.values()) {
      assertEquals(o, PSMigrationOutcome.fromString(o.name().toLowerCase()));
    }
    assertNull(PSMigrationOutcome.fromString("nope"));
    assertNull(PSMigrationOutcome.fromString(null));
  }

  private void writeDerbyRepo() throws Exception {
    writeRepo(
        """
        DB_BACKEND=DERBY
        DB_DRIVER_NAME=derby
        DB_DRIVER_CLASS_NAME=org.apache.derby.jdbc.EmbeddedDriver
        DB_SERVER=//localhost:1527/CMDB
        PWD=should-not-appear-in-logs
        """);
  }

  private void writeRepo(String content) throws Exception {
    Path props = installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE);
    Files.createDirectories(props.getParent());
    Files.writeString(props, content, StandardCharsets.UTF_8);
  }

  private void assertReportOutcome(PSMigrationOutcome expected) throws Exception {
    Path path =
        PSMigrationReportWriter.reportPath(installRoot, PSEmbeddedRepositoryMigrator.COMPONENT_CMS);
    assertTrue(Files.isRegularFile(path), "durable report must exist");
    PSMigrationReportWriter.Report report = PSMigrationReportWriter.read(path);
    assertEquals(expected, report.outcome());
    assertEquals(PSEmbeddedRepositoryMigrator.COMPONENT_CMS, report.component());
    assertNotNull(report.finishedAt());
  }
}
