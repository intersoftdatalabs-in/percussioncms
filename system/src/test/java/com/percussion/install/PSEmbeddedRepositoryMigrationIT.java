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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end migrator using TableFactory export/import (T046 / T058–T060).
 *
 * <p>Source is an H2 file DB labeled as product-managed Derby is not required for the transfer
 * machinery test: we label the source as H2 so the full migrator path after gate is exercised by
 * calling {@link PSTableFactoryMigrationTransfer} indirectly only when source is Derby. For a true
 * Derby source IT, run on an install that still has Derby migration-window jars (FR-021).
 *
 * <p>This IT seeds a Derby-shaped install tree (rxrepository DERBY) but uses a dual-H2 transfer
 * smoke via {@link PSTableFactoryMigrationTransfer} plus cutover, and separately asserts the
 * migrator skip/success paths on H2-already / TableFactory round-trip for transfer.
 */
@Tag("IntegrationTest")
public class PSEmbeddedRepositoryMigrationIT {

  @TempDir Path installRoot;

  @Test
  void tableFactoryTransfer_and_cutover_likeMigrator() throws Exception {
    Path srcBase = installRoot.resolve("DerbyData").resolve("CMDB");
    Path h2Base = PSRepositoryConnectionHelper.defaultH2DatabaseBase(installRoot);
    Files.createDirectories(srcBase.getParent());
    Files.createDirectories(h2Base.getParent());

    String srcPath = srcBase.toAbsolutePath().toString().replace('\\', '/');
    Class.forName(PSJdbcUtils.H2_DRIVER_CLASS);
    // Simulate legacy data store content (would be Derby in production)
    try (Connection c =
            DriverManager.getConnection(
                "jdbc:h2:file:" + srcPath + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE CONTENT (ID INT PRIMARY KEY, TITLE VARCHAR(64), "
              + "FLAG CHAR(1), BODY CLOB)");
      st.execute(
          "CREATE TABLE NEXTNUMBER (KEYNAME VARCHAR(64) PRIMARY KEY, NEXTNR INT NOT NULL)");
      st.execute("INSERT INTO CONTENT VALUES (99, 'preserved', 'T', 'hello-clob')");
      st.execute("INSERT INTO NEXTNUMBER VALUES ('CONTENT', 200)");
    }

    Properties source = new Properties();
    source.setProperty("DB_BACKEND", "H2");
    source.setProperty("DB_DRIVER_NAME", "h2");
    source.setProperty("DB_DRIVER_CLASS_NAME", PSJdbcUtils.H2_DRIVER_CLASS);
    source.setProperty("DB_SERVER", "file:" + srcPath + ";DB_CLOSE_ON_EXIT=FALSE");
    source.setProperty("DB_SCHEMA", "PUBLIC");
    source.setProperty("UID", "sa");
    source.setProperty("PWD", "");
    source.setProperty("PWD_ENCRYPTED", "N");

    Properties h2Props =
        PSRepositoryConnectionHelper.buildH2TargetProperties(installRoot, h2Base);
    h2Props.remove("INSTALL_ROOT_HINT");
    h2Props.setProperty("PWD_ENCRYPTED", "N");

    Path staging = installRoot.resolve("PreInstall").resolve("tablefactory-migration");
    PSTableFactoryMigrationTransfer.Result transfer =
        PSTableFactoryMigrationTransfer.exportThenImport(source, h2Props, staging);
    assertTrue(transfer.tablesExported() >= 2);

    Path rxProps = installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE);
    Files.createDirectories(rxProps.getParent());
    // Pretend live config was still Derby before cutover
    Files.writeString(
        rxProps,
        "DB_BACKEND=DERBY\nDB_DRIVER_NAME=derby\nDB_DRIVER_CLASS_NAME=org.apache.derby.jdbc.EmbeddedDriver\n"
            + "DB_SERVER=//localhost:1527/CMDB\nUID=\nPWD=\n",
        StandardCharsets.UTF_8);

    Path percDs = installRoot.resolve(PSConfigCutover.PERC_DS_RELATIVE);
    Files.createDirectories(percDs.getParent());
    Files.writeString(
        percDs,
        "perc.ds.1.driver.name=derby\nperc.ds.1.driver.class=org.apache.derby.jdbc.EmbeddedDriver\n"
            + "perc.ds.1.server=//localhost:1527/CMDB\nperc.ds.1.uid=\nperc.ds.1.pwd=\n",
        StandardCharsets.UTF_8);

    PSConfigCutover.cutoverToH2(installRoot, h2Props);

    Properties live = new Properties();
    try (var in = Files.newInputStream(rxProps)) {
      live.load(in);
    }
    assertEquals("H2", live.getProperty("DB_BACKEND"));

    String h2Path = h2Base.toAbsolutePath().toString().replace('\\', '/');
    try (Connection h2 =
            DriverManager.getConnection(
                "jdbc:h2:file:" + h2Path + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        Statement st = h2.createStatement();
        ResultSet rs = st.executeQuery("SELECT TITLE FROM CONTENT WHERE ID = 99")) {
      assertTrue(rs.next());
      assertEquals("preserved", rs.getString(1));
    }
  }

  @Test
  void migrator_alreadyH2_skips() throws Exception {
    Path rxProps = installRoot.resolve(PSEmbeddedRepositoryMigrator.RXREPOSITORY_RELATIVE);
    Files.createDirectories(rxProps.getParent());
    Files.writeString(
        rxProps,
        """
        DB_BACKEND=H2
        DB_DRIVER_NAME=h2
        DB_DRIVER_CLASS_NAME=org.h2.Driver
        DB_SERVER=file:../../Repository/CMDB
        UID=sa
        PWD=
        """,
        StandardCharsets.UTF_8);
    PSMigrationOutcome outcome =
        new PSEmbeddedRepositoryMigrator(installRoot, new Properties(), false, null).migrate();
    assertEquals(PSMigrationOutcome.ALREADY_MIGRATED, outcome);
  }
}
