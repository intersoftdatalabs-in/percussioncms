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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.jdbc.PSJdbcUtils;
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
 * TableFactory export → import round-trip (T058). Uses H2 for both ends so tests do not depend on
 * Derby module packaging; production migration uses Derby source props the same way.
 */
@Tag("UnitTest")
public class PSTableFactoryMigrationTransferTest {

  @TempDir Path temp;

  @Test
  void exportThenImport_preservesExplicitIdsAndNextNumber() throws Exception {
    Path srcDb = temp.resolve("src").resolve("CMDB");
    Path tgtDb = temp.resolve("tgt").resolve("CMDB");
    Path staging = temp.resolve("export");
    Files.createDirectories(srcDb.getParent());
    Files.createDirectories(tgtDb.getParent());

    String srcPath = srcDb.toAbsolutePath().toString().replace('\\', '/');
    String tgtPath = tgtDb.toAbsolutePath().toString().replace('\\', '/');

    Class.forName(PSJdbcUtils.H2_DRIVER_CLASS);
    try (Connection c =
            DriverManager.getConnection(
                "jdbc:h2:file:" + srcPath + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        Statement st = c.createStatement()) {
      // Use CHAR flags like product Derby BIT→char flags; CLOB for LOB probe
      st.execute(
          "CREATE TABLE CONTENT (ID INT PRIMARY KEY, TITLE VARCHAR(64), FLAG CHAR(1), BODY CLOB)");
      st.execute("CREATE TABLE NEXTNUMBER (KEYNAME VARCHAR(64) PRIMARY KEY, NEXTNR INT NOT NULL)");
      st.execute("INSERT INTO CONTENT VALUES (42, 'item', 'T', 'clob-body')");
      st.execute("INSERT INTO CONTENT VALUES (7, 'other', 'F', NULL)");
      st.execute("INSERT INTO NEXTNUMBER VALUES ('CONTENT', 100)");
    }

    Properties source = h2Props(srcPath);
    Properties target = h2Props(tgtPath);

    PSTableFactoryMigrationTransfer.Result result =
        PSTableFactoryMigrationTransfer.exportThenImport(source, target, staging);

    assertTrue(result.tablesExported() >= 2, "exported=" + result.tablesExported());
    assertEquals(result.tablesExported(), result.tablesImported());
    assertTrue(Files.isRegularFile(staging.resolve("defData").resolve("tableDef.xml")));

    try (Connection c =
            DriverManager.getConnection(
                "jdbc:h2:file:" + tgtPath + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT TITLE FROM CONTENT WHERE ID = 42")) {
      assertTrue(rs.next());
      assertEquals("item", rs.getString(1));
    }
    try (Connection c =
            DriverManager.getConnection(
                "jdbc:h2:file:" + tgtPath + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT NEXTNR FROM NEXTNUMBER WHERE KEYNAME = 'CONTENT'")) {
      assertTrue(rs.next());
      assertEquals(100, rs.getInt(1));
    }
  }

  private static Properties h2Props(String filePathNoExt) {
    Properties p = new Properties();
    p.setProperty("DB_BACKEND", "H2");
    p.setProperty("DB_DRIVER_NAME", "h2");
    p.setProperty("DB_DRIVER_CLASS_NAME", PSJdbcUtils.H2_DRIVER_CLASS);
    p.setProperty("DB_SERVER", "file:" + filePathNoExt + ";DB_CLOSE_ON_EXIT=FALSE");
    p.setProperty("DB_SCHEMA", "PUBLIC");
    p.setProperty("UID", "sa");
    p.setProperty("PWD", "");
    p.setProperty("PWD_ENCRYPTED", "N");
    return p;
  }
}
