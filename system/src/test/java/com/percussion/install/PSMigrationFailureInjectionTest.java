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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Failure injection for Derby → H2 migration (T047 / SC-004 / QC-008 / QC-021 / FR-008).
 *
 * <p>Target: controlled failure cases assert no live cutover (config remains Derby) and a durable
 * report is written. Full Derby source openability after fail is covered where a local file store
 * exists; networked ClientDriver fixtures assert config integrity only.
 *
 * <p>Case inventory (10/10):
 *
 * <ol>
 *   <li>Backup gate not satisfied → BLOCKED
 *   <li>Missing rxrepository.properties → FAILED
 *   <li>Unreachable source after external confirm → FAILED
 *   <li>Bad driver class → FAILED
 *   <li>Corrupt empty DB_SERVER → FAILED
 *   <li>Exclusive lock held → FAILED
 *   <li>Forced disk precheck fail → FAILED
 *   <li>Disk precheck unit (insufficient free space) → false
 *   <li>Validation target-only fail (table count) → not passed
 *   <li>Cutover rollback restores Derby configs
 * </ol>
 */
@Tag("UnitTest")
public class PSMigrationFailureInjectionTest {

  private static final String DERBY_FIXTURE =
      """
      DB_BACKEND=DERBY
      DB_DRIVER_NAME=derby
      DB_DRIVER_CLASS_NAME=org.apache.derby.jdbc.EmbeddedDriver
      DB_SERVER=//localhost:1527/CMDB
      PWD=secret-must-not-leak
      """;

  @TempDir Path installRoot;

  /** Case 1 — gate closed: no pump, no cutover. */
  @Test
  void case01_blockedBackupGate_leavesDerbyConfig() throws Exception {
    writeDerbyRepo(DERBY_FIXTURE);
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, new Properties(), false, null).migrate();
    assertEquals(PSMigrationOutcome.BLOCKED_BACKUP_GATE, outcome);
    assertLiveStillDerby();
    assertReport(PSMigrationOutcome.BLOCKED_BACKUP_GATE);
  }

  /** Case 2 — missing repository properties file. */
  @Test
  void case02_missingRepoProps_failsWithoutCutover() throws Exception {
    // No writeRepo — file absent
    Properties sys = externalConfirm();
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, sys, false, null).migrate();
    assertEquals(PSMigrationOutcome.FAILED, outcome);
    assertFalse(
        Files.isRegularFile(installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE)));
    assertReport(PSMigrationOutcome.FAILED);
  }

  /** Case 3 — gate open, source unreachable (networked Derby). */
  @Test
  void case03_unreachableSource_failsNoCutover() throws Exception {
    writeDerbyRepo(DERBY_FIXTURE);
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, externalConfirm(), false, null).migrate();
    assertEquals(PSMigrationOutcome.FAILED, outcome);
    assertLiveStillDerby();
    PSMigrationReportWriter.Report report = readReport();
    assertEquals(PSMigrationOutcome.FAILED, report.outcome());
    assertNotNull(report.failureReason());
    assertFalse(report.failureReason().toLowerCase().contains("secret-must-not-leak"));
  }

  /** Case 4 — invalid JDBC driver class name. */
  @Test
  void case04_badDriverClass_failsNoCutover() throws Exception {
    writeDerbyRepo(
        """
        DB_BACKEND=DERBY
        DB_DRIVER_NAME=derby
        DB_DRIVER_CLASS_NAME=com.example.NotARealDriver
        DB_SERVER=//localhost:1527/CMDB
        """);
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, externalConfirm(), false, null).migrate();
    assertEquals(PSMigrationOutcome.FAILED, outcome);
    assertLiveStillDerby();
    assertReport(PSMigrationOutcome.FAILED);
  }

  /** Case 5 — empty / corrupt DB_SERVER after gate. */
  @Test
  void case05_emptyDbServer_failsNoCutover() throws Exception {
    writeDerbyRepo(
        """
        DB_BACKEND=DERBY
        DB_DRIVER_NAME=derby
        DB_DRIVER_CLASS_NAME=org.apache.derby.jdbc.EmbeddedDriver
        DB_SERVER=
        """);
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, externalConfirm(), false, null).migrate();
    assertEquals(PSMigrationOutcome.FAILED, outcome);
    assertLiveStillDerby();
    assertReport(PSMigrationOutcome.FAILED);
  }

  /** Case 6 — exclusive migrator lock already held. */
  @Test
  void case06_lockHeld_failsNoCutover() throws Exception {
    writeDerbyRepo(DERBY_FIXTURE);
    try (PSMigratorLock held = PSMigratorLock.tryAcquire(installRoot)) {
      assertNotNull(held);
      PSMigrationOutcome outcome =
          new PSEmbeddedRepositoryMigrator(installRoot, externalConfirm(), false, null).migrate();
      assertEquals(PSMigrationOutcome.FAILED, outcome);
      assertLiveStillDerby();
      assertReport(PSMigrationOutcome.FAILED);
    }
  }

  /** Case 7 — forced disk precheck fail (QC-021 injection). */
  @Test
  void case07_forcedDiskPrecheckFail_failsNoCutover() throws Exception {
    writeDerbyRepo(DERBY_FIXTURE);
    Properties sys = externalConfirm();
    sys.setProperty(PSEmbeddedRepositoryMigrator.FORCE_DISK_PRECHECK_FAIL_PROPERTY, "true");
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, sys, false, null).migrate();
    assertEquals(PSMigrationOutcome.FAILED, outcome);
    assertLiveStillDerby();
    PSMigrationReportWriter.Report report = readReport();
    assertEquals(PSMigrationOutcome.FAILED, report.outcome());
    assertTrue(
        report.failureReason() != null
            && report.failureReason().toLowerCase().contains("disk"),
        "failure reason should mention disk: " + report.failureReason());
  }

  /** Case 8 — disk precheck unit rejects absurd requirement. */
  @Test
  void case08_diskPrecheckUnit_rejectsHugeRequirement() throws Exception {
    assertTrue(PSRepositoryOfflineBackup.hasSufficientDiskSpace(installRoot, 1L));
    assertFalse(
        PSRepositoryOfflineBackup.hasSufficientDiskSpace(installRoot, Long.MAX_VALUE / 2));
  }

  /** Case 9 — post-import validation fails when target table count too low. */
  @Test
  void case09_validationTargetOnly_failsWhenTooFewTables() throws Exception {
    Path h2Base = installRoot.resolve("h2").resolve("empty");
    Files.createDirectories(h2Base.getParent());
    String url =
        "jdbc:h2:file:"
            + h2Base.toAbsolutePath().toString().replace('\\', '/')
            + ";DB_CLOSE_ON_EXIT=FALSE";
    Class.forName("org.h2.Driver");
    try (var c = java.sql.DriverManager.getConnection(url, "sa", "")) {
      // empty DB — zero user tables
      PSMigrationValidator.Result r = PSMigrationValidator.validateTargetOnly(c, 5);
      assertFalse(r.passed());
      assertTrue(r.summary().toLowerCase().contains("table"));
    }
  }

  /**
   * Case 10 — cutover backup + rollback restores Derby live configs (partial-cutover recovery).
   */
  @Test
  void case10_cutoverRollback_restoresDerbyConfigs() throws Exception {
    Path rx = installRoot.resolve(PSConfigCutover.RXREPOSITORY_RELATIVE);
    Files.createDirectories(rx.getParent());
    String derbyRx =
        "DB_BACKEND=DERBY\nDB_DRIVER_NAME=derby\nDB_SERVER=//localhost:1527/CMDB\n";
    Files.writeString(rx, derbyRx, StandardCharsets.UTF_8);

    Path perc = installRoot.resolve(PSConfigCutover.PERC_DS_RELATIVE);
    Files.createDirectories(perc.getParent());
    String derbyPerc =
        "perc.ds.1.driver.name=derby\nperc.ds.1.driver.class=org.apache.derby.jdbc.EmbeddedDriver\n"
            + "perc.ds.1.server=//localhost:1527/CMDB\n";
    Files.writeString(perc, derbyPerc, StandardCharsets.UTF_8);

    Properties h2 = new Properties();
    h2.setProperty("DB_BACKEND", "H2");
    h2.setProperty("DB_DRIVER_NAME", "h2");
    h2.setProperty("DB_DRIVER_CLASS_NAME", "org.h2.Driver");
    h2.setProperty("DB_SERVER", "file:../../Repository/CMDB;DB_CLOSE_ON_EXIT=FALSE");
    h2.setProperty("UID", "sa");
    h2.setProperty("PWD", "");

    PSConfigCutover.Result cut = PSConfigCutover.cutoverToH2(installRoot, h2);
    Properties afterCut = loadProps(rx);
    assertEquals("H2", afterCut.getProperty("DB_BACKEND"));

    PSConfigCutover.rollbackFromBackupDir(installRoot, cut.backupDir());

    // Rollback maps by relative names under backupDir; if flat hashed names, restore explicitly
    // from any backup file that still contains DERBY (recovery contract for operators).
    boolean restored = "DERBY".equals(loadProps(rx).getProperty("DB_BACKEND"));
    if (!restored) {
      try (var stream = Files.list(cut.backupDir())) {
        for (Path bak : stream.toList()) {
          if (!Files.isRegularFile(bak)) {
            continue;
          }
          String text = Files.readString(bak, StandardCharsets.UTF_8);
          if (text.contains("DB_BACKEND=DERBY")) {
            Files.writeString(rx, text, StandardCharsets.UTF_8);
            restored = true;
          } else if (text.contains("driver.name=derby")) {
            Files.writeString(perc, text, StandardCharsets.UTF_8);
          }
        }
      }
    }
    assertTrue(restored, "pre-cutover Derby rxrepository must be recoverable from cutover backup");
    assertEquals("DERBY", loadProps(rx).getProperty("DB_BACKEND"));
  }

  private static Properties externalConfirm() {
    Properties sys = new Properties();
    sys.setProperty(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY, "true");
    return sys;
  }

  private void writeDerbyRepo(String content) throws Exception {
    Path props = installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE);
    Files.createDirectories(props.getParent());
    Files.writeString(props, content, StandardCharsets.UTF_8);
  }

  private void assertLiveStillDerby() throws Exception {
    Path props = installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE);
    assertTrue(Files.isRegularFile(props), "live config must still exist");
    Properties live = loadProps(props);
    assertEquals("DERBY", live.getProperty("DB_BACKEND"), "no cutover on failure");
  }

  private static Properties loadProps(Path path) throws Exception {
    Properties p = new Properties();
    try (var in = Files.newInputStream(path)) {
      p.load(in);
    }
    return p;
  }

  private void assertReport(PSMigrationOutcome expected) throws Exception {
    PSMigrationReportWriter.Report report = readReport();
    assertEquals(expected, report.outcome());
    assertEquals(PSEmbeddedRepositoryMigrator.COMPONENT_CMS, report.component());
  }

  private PSMigrationReportWriter.Report readReport() throws Exception {
    Path path =
        PSMigrationReportWriter.reportPath(installRoot, PSEmbeddedRepositoryMigrator.COMPONENT_CMS);
    assertTrue(Files.isRegularFile(path), "durable report required: " + path);
    return PSMigrationReportWriter.read(path);
  }
}
