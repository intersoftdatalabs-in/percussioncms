/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.utils.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.util.PSSqlHelper;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PSJdbcUtilsTest {

  @TempDir Path tempDir;

  @Test
  public void testGetDriverFromUrl() {
    assertEquals("oracle:thin", PSJdbcUtils.getDriverFromUrl("jdbc:oracle:thin:serverName"));
    assertEquals("jtds:sqlserver", PSJdbcUtils.getDriverFromUrl("jdbc:jtds:sqlserver://bender"));

    assertEquals("sqlserver", PSJdbcUtils.getDriverFromUrl("jdbc:sqlserver://bender"));
  }

  @Test
  public void testGetServerFromUrl() {
    assertEquals("serverName", PSJdbcUtils.getServerFromUrl("jdbc:oracle:thin:serverName"));
    assertEquals("//fffooo", PSJdbcUtils.getServerFromUrl("jdbc:odbc://fffooo"));
    assertEquals("//bender", PSJdbcUtils.getServerFromUrl("jdbc:jtds:sqlserver://bender"));
    assertEquals("//bender", PSJdbcUtils.getServerFromUrl("jdbc:sqlserver://bender"));
  }

  @Test
  public void testGetJdbcUrl() {
    assertEquals(
        "jdbc:oracle:thin:serverName", PSJdbcUtils.getJdbcUrl("oracle:thin", "serverName"));
  }

  @Test
  public void testGetDBBackendForDriver() {
    assertEquals(
        PSJdbcUtils.SPRINTA_DB_BACKEND, PSJdbcUtils.getDBBackendForDriver(PSJdbcUtils.SPRINTA));
    assertEquals(PSJdbcUtils.DB2_DB_BACKEND, PSJdbcUtils.getDBBackendForDriver(PSJdbcUtils.DB2));
    assertEquals(
        PSJdbcUtils.JTDS_DB_BACKEND, PSJdbcUtils.getDBBackendForDriver(PSJdbcUtils.JTDS_DRIVER));
    assertEquals(
        PSJdbcUtils.ORACLE_DB_BACKEND, PSJdbcUtils.getDBBackendForDriver(PSJdbcUtils.ORACLE));
    assertEquals(
        PSJdbcUtils.H2_DB_BACKEND, PSJdbcUtils.getDBBackendForDriver(PSJdbcUtils.H2_DRIVER));
    assertEquals(PSJdbcUtils.H2_DB_BACKEND, PSJdbcUtils.getDBBackendForDriver("h2"));
  }

  @Test
  public void testH2JdbcUrlAndDriverMap() {
    // VALUE and DAY are H2 keywords used as product columns; getJdbcUrl merges NON_KEYWORDS.
    assertEquals(
        "jdbc:h2:./data/cms;NON_KEYWORDS=VALUE,DAY",
        PSJdbcUtils.getJdbcUrl(PSJdbcUtils.H2_DRIVER, "./data/cms"));
    assertEquals(
        "jdbc:h2:file:../../Repository/CMDB;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE,DAY",
        PSJdbcUtils.getJdbcUrl(
            PSJdbcUtils.H2_DRIVER, "file:../../Repository/CMDB;DB_CLOSE_ON_EXIT=FALSE"));
    // Merge DAY into a legacy VALUE-only list; keep extra tokens (KEY).
    assertEquals(
        "jdbc:h2:./data/cms;NON_KEYWORDS=VALUE,KEY,DAY",
        PSJdbcUtils.getJdbcUrl(PSJdbcUtils.H2_DRIVER, "./data/cms;NON_KEYWORDS=VALUE,KEY"));
    assertEquals(
        "jdbc:h2:./data/cms;NON_KEYWORDS=VALUE,DAY",
        PSJdbcUtils.getJdbcUrl(PSJdbcUtils.H2_DRIVER, "./data/cms;NON_KEYWORDS=VALUE"));
    assertEquals("h2", PSJdbcUtils.getDriverFromUrl("jdbc:h2:./data/cms"));
    // File/mem/tcp subprotocols must not surface as "h2:file" (PSSchedulerBean / install).
    assertEquals(
        "h2",
        PSJdbcUtils.getDriverFromUrl(
            "jdbc:h2:file:../../Repository/CMDB;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE"));
    assertEquals("h2", PSJdbcUtils.getDriverFromUrl("jdbc:h2:mem:test"));
    assertEquals("h2", PSJdbcUtils.getDriverFromUrl("jdbc:h2:tcp://localhost/~/test"));
    assertEquals(PSJdbcUtils.H2_DRIVER_CLASS, "org.h2.Driver");
    assertEquals(PSJdbcUtils.H2, PSJdbcUtils.H2_DRIVER);
  }

  @Test
  public void testH2NonKeywordsAllowsUnquotedDayColumn() throws Exception {
    try {
      Class.forName("org.h2.Driver");
    } catch (ClassNotFoundException e) {
      Assumptions.assumeTrue(false, "org.h2.Driver not on test classpath");
    }
    String url =
        PSJdbcUtils.getJdbcUrl(
            PSJdbcUtils.H2_DRIVER, "mem:holiday_day;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE");
    assertTrue(url.toUpperCase().contains("DAY"), url);
    try (Connection c = DriverManager.getConnection(url, "sa", "");
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE RXS_CT_HOLIDAY (HOLIDAYID INTEGER NOT NULL, DAY TIMESTAMP NULL,"
              + " NAME VARCHAR(200) NULL, CONSTRAINT PK_RXS_CT_HOLIDAY PRIMARY KEY (HOLIDAYID))");
    }
  }

  @Test
  public void testResolveEmbeddedFileServer_relativeProductDefault() {
    Path installRoot = tempDir.resolve("Percussion").toAbsolutePath().normalize();
    String resolved =
        PSJdbcUtils.resolveEmbeddedFileServer(
            "file:../../Repository/CMDB;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE", installRoot);
    String expected =
        "file:"
            + installRoot
                .resolve("Repository")
                .resolve("CMDB")
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace('\\', '/')
            + ";DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE";
    assertEquals(expected, resolved);
  }

  @Test
  public void testResolveEmbeddedFileServer_absoluteUnchangedExceptNormalize() {
    Path absDb = tempDir.resolve("abs").resolve("CMDB").toAbsolutePath().normalize();
    String absFragment = "file:" + absDb.toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
    String resolved = PSJdbcUtils.resolveEmbeddedFileServer(absFragment, tempDir);
    assertEquals(
        "file:" + absDb.toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE", resolved);
  }

  @Test
  public void testResolveEmbeddedFileServer_nullsAndNonFile() {
    assertNull(PSJdbcUtils.resolveEmbeddedFileServer(null, tempDir));
    assertEquals(
        "file:../../Repository/CMDB",
        PSJdbcUtils.resolveEmbeddedFileServer("file:../../Repository/CMDB", null));
    assertEquals(
        "//localhost:5432/cms",
        PSJdbcUtils.resolveEmbeddedFileServer("//localhost:5432/cms", tempDir));
  }

  @Test
  public void testSqlHelperH2AndEmbeddedFileStore() {
    assertTrue(PSSqlHelper.isH2("h2"));
    assertTrue(PSSqlHelper.isH2("H2"));
    assertFalse(PSSqlHelper.isH2("derby"));
    assertFalse(PSSqlHelper.isH2(null));
    assertTrue(PSSqlHelper.isEmbeddedFileStore("h2"));
    assertTrue(PSSqlHelper.isEmbeddedFileStore("derby"));
    assertFalse(PSSqlHelper.isEmbeddedFileStore("mysql"));
  }

  @Test
  public void testPostgresJdbcUrlDriverMapAndBackend() {
    assertEquals(
        "jdbc:postgresql://db.example.com:5432/percussion",
        PSJdbcUtils.getJdbcUrl(PSJdbcUtils.POSTGRES_DRIVER, "//db.example.com:5432/percussion"));
    assertEquals(
        "postgresql", PSJdbcUtils.getDriverFromUrl("jdbc:postgresql://db.example.com:5432/cms"));
    assertEquals(PSJdbcUtils.POSTGRES_DRIVER_CLASS, "org.postgresql.Driver");
    assertEquals(
        PSJdbcUtils.POSTGRES_DB_BACKEND,
        PSJdbcUtils.getDBBackendForDriver(PSJdbcUtils.POSTGRES_DRIVER));
    assertEquals(PSJdbcUtils.POSTGRES_DB_BACKEND, PSJdbcUtils.getDBBackendForDriver("postgres"));
    assertTrue(PSSqlHelper.isPostgres("postgresql"));
    assertTrue(PSSqlHelper.isPostgres("POSTGRESQL"));
    assertTrue(PSSqlHelper.isPostgres("postgres"));
    assertFalse(PSSqlHelper.isPostgres("mysql"));
    assertFalse(PSSqlHelper.isPostgres(null));
    assertFalse(PSSqlHelper.isEmbeddedFileStore("postgresql"));
    assertTrue(PSJdbcUtils.isExternalDriver(PSJdbcUtils.POSTGRES_DRIVER));
  }

  @Test
  public void testMysqlConnParamsIncludeUtf8mb4Collation() {
    // Matrix MySQL 8 failed install views with latin1 connection vs utf8mb4 tables.
    assertTrue(
        PSJdbcUtils.MYSQL_CONN_PARAMS.contains("connectionCollation=utf8mb4_unicode_ci"),
        PSJdbcUtils.MYSQL_CONN_PARAMS);
    assertTrue(PSJdbcUtils.MYSQL_CONN_PARAMS.contains("characterEncoding=UTF-8"));
  }

  @Test
  public void testGetDatabaseFromUrl() {
    String DB_NAME = "myDatabase";

    // \/\/\
    // jTDS
    // \/\/\
    // test format for jTDS driver, === positive ===
    String url = "jdbc:jtds:sqlserver://localhost/" + DB_NAME;
    String dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:jtds:sqlserver://localhost:1433/" + DB_NAME;
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:jtds:sqlserver://localhost/" + DB_NAME + ";user=u;password=p";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:jtds:sqlserver://localhost:1433;database=" + DB_NAME + ";user=u;password=p";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:jtds:sqlserver://localhost:1433;user=u;password=p;database=" + DB_NAME;
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    // test format for jTDS driver, === negative ===

    url = "jdbc:jtds:sqlserver://localhost:1433";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);

    url = "jdbc:jtds:sqlserver://localhost:1433/";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);

    url = "jdbc:jtds:sqlserver://localhost:1433;user=u;password=p";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);

    url = "jdbc:jtds:sqlserver://localhost:1433/;database=" + DB_NAME + ";user=u;password=p";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertFalse(DB_NAME.equals(dbName));

    // \/\/\
    // mySQL
    // \/\/\
    url = "jdbc:mysql://localhost/" + DB_NAME;
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:mysql://localhost:1431/" + DB_NAME;
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:mysql://localhost:1431/";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);

    url = "jdbc:mysql://localhost:1431";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);

    // \/\/\
    // DB2
    // \/\/\
    url = "jdbc:db2://localhost:1234/" + DB_NAME;
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:db2://localhost/";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);

    url = "jdbc:db2://localhost:1234/";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);

    // \/\/\/\/
    // SPRINTA
    // \/\/\/\/
    url = "jdbc:inetdae7:localhost:1234?database=" + DB_NAME;
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:inetdae7:localhost?database=" + DB_NAME;
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:inetdae7:localhost?user=u&database=" + DB_NAME + "&password=p";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertEquals(DB_NAME, dbName);

    url = "jdbc:inetdae7:localhost?user=u&password=p";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);

    // \/\/\/\/
    // Oracle
    // \/\/\/\/
    url = "jdbc:oracle:thin://@qadb:1521:qadb";
    dbName = PSJdbcUtils.getDatabaseFromUrl(url);
    assertNull(dbName);
  }
}
