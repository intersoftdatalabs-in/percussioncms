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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Scale fixture for TableFactory migration transfer (T050 / QC-029 / SC-002).
 *
 * <p>Seeds ≥1000 content rows, runs export→import, asserts row count, and appends wall-clock to
 * {@code target/migration-timing.md} under the current working directory (best-effort; never fails
 * the build if missing). Keeping the log under {@code target/} ensures tests do not mutate tracked
 * source files.
 */
@Tag("IntegrationTest")
public class PSMigrationScaleFixtureTest {

  private static final int SCALE_ROWS = 1000;

  @TempDir Path temp;

  @Test
  void scaleTransfer_atLeast1000ContentRows() throws Exception {
    Path srcDb = temp.resolve("src").resolve("CMDB");
    Path tgtDb = temp.resolve("tgt").resolve("CMDB");
    Path staging = temp.resolve("export");
    Files.createDirectories(srcDb.getParent());
    Files.createDirectories(tgtDb.getParent());

    String srcPath = srcDb.toAbsolutePath().toString().replace('\\', '/');
    String tgtPath = tgtDb.toAbsolutePath().toString().replace('\\', '/');

    Class.forName(PSJdbcUtils.H2_DRIVER_CLASS);
    long seedStart = System.nanoTime();
    try (Connection c =
            DriverManager.getConnection(
                "jdbc:h2:file:" + srcPath + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE CONTENT (ID INT PRIMARY KEY, TITLE VARCHAR(128), FLAG CHAR(1), BODY CLOB)");
      st.execute("CREATE TABLE NEXTNUMBER (KEYNAME VARCHAR(64) PRIMARY KEY, NEXTNR INT NOT NULL)");
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO CONTENT VALUES (?, ?, 'T', ?)")) {
        for (int i = 1; i <= SCALE_ROWS; i++) {
          ps.setInt(1, i);
          ps.setString(2, "item-" + i);
          ps.setString(3, "body-" + i);
          ps.addBatch();
          if (i % 200 == 0) {
            ps.executeBatch();
          }
        }
        ps.executeBatch();
      }
      st.execute("INSERT INTO NEXTNUMBER VALUES ('CONTENT', " + (SCALE_ROWS + 1) + ")");
    }
    long seedMs = (System.nanoTime() - seedStart) / 1_000_000L;

    Properties source = h2Props(srcPath);
    Properties target = h2Props(tgtPath);

    long xferStart = System.nanoTime();
    PSTableFactoryMigrationTransfer.Result result =
        PSTableFactoryMigrationTransfer.exportThenImport(source, target, staging);
    long xferMs = (System.nanoTime() - xferStart) / 1_000_000L;

    assertTrue(result.tablesExported() >= 2);
    assertEquals(result.tablesExported(), result.tablesImported());

    int count;
    try (Connection c =
            DriverManager.getConnection(
                "jdbc:h2:file:" + tgtPath + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM CONTENT")) {
      assertTrue(rs.next());
      count = rs.getInt(1);
    }
    assertEquals(SCALE_ROWS, count, "all content rows must transfer");

    appendTimingLog(seedMs, xferMs, count, result.tablesExported());
  }

  private static Properties h2Props(String filePath) {
    Properties p = new Properties();
    p.setProperty("DB_BACKEND", "H2");
    p.setProperty("DB_DRIVER_NAME", "h2");
    p.setProperty("DB_DRIVER_CLASS_NAME", PSJdbcUtils.H2_DRIVER_CLASS);
    p.setProperty("DB_SERVER", "file:" + filePath + ";DB_CLOSE_ON_EXIT=FALSE");
    p.setProperty("DB_SCHEMA", "PUBLIC");
    p.setProperty("UID", "sa");
    p.setProperty("PWD", "");
    p.setProperty("PWD_ENCRYPTED", "N");
    return p;
  }

  private static void appendTimingLog(long seedMs, long xferMs, int rows, int tables) {
    try {
      Path logFile = resolveTimingLogFile();
      if (logFile == null) {
        return;
      }
      if (!Files.isRegularFile(logFile)) {
        Path parent = logFile.getParent();
        if (parent == null) {
          return;
        }
        Files.createDirectories(parent);
        Files.writeString(
            logFile,
            "# Migration timing log (T050 / QC-029)\n\n"
                + "| When (UTC) | Host note | Rows | Tables | Seed ms | Transfer ms |\n"
                + "|------------|-----------|------|--------|---------|-------------|\n",
            StandardCharsets.UTF_8);
      }
      String line =
          String.format(
              "| %s | PSMigrationScaleFixtureTest | %d | %d | %d | %d |%n",
              java.time.Instant.now(), rows, tables, seedMs, xferMs);
      Files.writeString(
          logFile, line, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
    } catch (Exception ignored) {
      // Best-effort timing log; transfer assertions above are the hard gate
    }
  }

  /**
   * Resolve the timing-log markdown file path under the build {@code target/} directory. This keeps
   * test output out of tracked source files and avoids breaking the ai-build-integrity seal.
   *
   * @return path to write, or null if no suitable location
   */
  static Path resolveTimingLogFile() {
    return Path.of("target", "migration-timing.md").toAbsolutePath().normalize();
  }
}
