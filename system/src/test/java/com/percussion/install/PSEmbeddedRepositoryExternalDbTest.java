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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * US3 / FR-009 / FR-015: external MySQL/MSSQL installs skip embedded migration and never rewrite
 * connection identity (T076–T079, T078).
 */
@Tag("UnitTest")
public class PSEmbeddedRepositoryExternalDbTest {

  @TempDir Path installRoot;

  @Test
  void skippedNonDerbyForMysqlAndMssql() throws Exception {
    assertSkip(
        """
        DB_BACKEND=MYSQL
        DB_DRIVER_NAME=mysql
        DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
        DB_SERVER=//db.example.com:3306/percussion
        DB_NAME=percussion
        UID=cms
        PWD=changeit
        """);
    // fresh root via second installRoot write — re-use same root with overwrite
    assertSkip(
        """
        DB_BACKEND=MSSQL
        DB_DRIVER_NAME=sqlserver
        DB_DRIVER_CLASS_NAME=com.microsoft.sqlserver.jdbc.SQLServerDriver
        DB_SERVER=//sql.example.com:1433;databaseName=PercussionDB
        DB_NAME=PercussionDB
        DB_SCHEMA=dbo
        UID=cms
        PWD=changeit
        """);
  }

  @Test
  void detectorClassifiesExternalSamples() {
    Properties mysql = new Properties();
    mysql.setProperty("DB_BACKEND", "MYSQL");
    mysql.setProperty("DB_DRIVER_NAME", "mysql");
    mysql.setProperty("DB_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver");
    assertEquals(
        PSEmbeddedRepositoryDetector.Classification.NON_DERBY,
        PSEmbeddedRepositoryDetector.classify(mysql));
    assertEquals(
        PSMigrationOutcome.SKIPPED_NON_DERBY,
        PSEmbeddedRepositoryDetector.toSkipOutcome(
            PSEmbeddedRepositoryDetector.Classification.NON_DERBY));

    Properties mssql = new Properties();
    mssql.setProperty("DB_BACKEND", "MSSQL");
    mssql.setProperty("DB_DRIVER_NAME", "sqlserver");
    mssql.setProperty(
        "DB_DRIVER_CLASS_NAME", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    assertEquals(
        PSEmbeddedRepositoryDetector.Classification.NON_DERBY,
        PSEmbeddedRepositoryDetector.classify(mssql));
  }

  @Test
  void externalMysqlConnectionKeysUnchangedAfterMigrate() throws Exception {
    Path rx = installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE);
    Files.createDirectories(rx.getParent());
    String original =
        """
        DB_BACKEND=MYSQL
        DB_SERVER=//db.example.com:3306/percussion?useUnicode=yes
        DB_NAME=percussion
        DB_SCHEMA=
        DB_DRIVER_NAME=mysql
        DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
        UID=cms
        PWD=secret-must-not-change
        PWD_ENCRYPTED=N
        DSCONFIG_NAME=PercussionData
        """;
    Files.writeString(rx, original, StandardCharsets.UTF_8);

    Path perc = installRoot.resolve(PSConfigCutover.PERC_DS_RELATIVE);
    Files.createDirectories(perc.getParent());
    String percOriginal =
        """
        perc.ds.1.driver.name=mysql
        perc.ds.1.driver.class=org.mariadb.jdbc.Driver
        perc.ds.1.server=//db.example.com:3306/percussion
        perc.ds.1.uid=cms
        perc.ds.1.pwd=secret-must-not-change
        """;
    Files.writeString(perc, percOriginal, StandardCharsets.UTF_8);

    Properties sys = new Properties();
    sys.setProperty(PSRepositoryBackupGate.EXTERNAL_BACKUP_CONFIRMED_PROPERTY, "true");
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, sys, true, null).migrate();
    assertEquals(PSMigrationOutcome.SKIPPED_NON_DERBY, outcome);

    String afterRx = Files.readString(rx, StandardCharsets.UTF_8);
    // Key identity stable (normalize line endings)
    assertTrue(afterRx.contains("DB_BACKEND=MYSQL"));
    assertTrue(afterRx.contains("DB_SERVER=//db.example.com:3306/percussion?useUnicode=yes"));
    assertTrue(afterRx.contains("DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver"));
    assertTrue(afterRx.contains("UID=cms"));
    assertTrue(afterRx.contains("PWD=secret-must-not-change"));
    assertFalse(afterRx.contains("DB_BACKEND=H2"));

    String afterPerc = Files.readString(perc, StandardCharsets.UTF_8);
    assertTrue(afterPerc.contains("perc.ds.1.driver.name=mysql"));
    assertTrue(afterPerc.contains("secret-must-not-change"));
    assertFalse(afterPerc.contains("org.h2.Driver"));
  }

  @Test
  void mixedEstate_cmsMysql_dtsDerby_onlyDtsMigrates() throws Exception {
    // CMS external — skip
    Path cmsRoot = installRoot.resolve("cms");
    Path rx = cmsRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE);
    Files.createDirectories(rx.getParent());
    Files.writeString(
        rx,
        """
        DB_BACKEND=MYSQL
        DB_DRIVER_NAME=mysql
        DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
        DB_SERVER=//db:3306/cms
        """,
        StandardCharsets.UTF_8);
    assertEquals(
        PSMigrationOutcome.SKIPPED_NON_DERBY,
        new PSEmbeddedRepositoryMigrator(cmsRoot, new Properties(), false, null).migrate());

    // DTS service with derbydata present — candidate for product-managed Derby
    Path dtsRoot = installRoot.resolve("dts");
    Path server = dtsRoot.resolve("Deployment").resolve("Server");
    Path derby = server.resolve("derbydata").resolve("percmetadata");
    Files.createDirectories(derby);
    var det = PSDtsEmbeddedRepositoryMigrator.detect(server, "percmetadata", derby);
    assertEquals(
        PSDtsEmbeddedRepositoryMigrator.DetectionClass.PRODUCT_MANAGED_DERBY,
        det.classification());
  }

  private void assertSkip(String propsBody) throws Exception {
    Path rx = installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE);
    Files.createDirectories(rx.getParent());
    Files.writeString(rx, propsBody, StandardCharsets.UTF_8);
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, new Properties(), false, null).migrate();
    assertEquals(PSMigrationOutcome.SKIPPED_NON_DERBY, outcome);
    PSMigrationReportWriter.Report report =
        PSMigrationReportWriter.read(
            PSMigrationReportWriter.reportPath(
                installRoot, PSEmbeddedRepositoryMigrator.COMPONENT_CMS));
    assertEquals(PSMigrationOutcome.SKIPPED_NON_DERBY, report.outcome());
    assertEquals(PSBackupGateKind.NOT_EVALUATED, report.backupGate());
  }
}
