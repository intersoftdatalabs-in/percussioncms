/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
class DbInstallConfigResolverTest {

  @TempDir Path tempDir;

  @Test
  void defaultIsH2WhenNoOptions() {
    DbInstallConfigResolver.ResolvedDbConfig cfg =
        DbInstallConfigResolver.resolveDbConfig(Map.of());
    assertEquals("h2", cfg.systemProperties().get("perc.db.type"));
    assertFalse(cfg.systemProperties().containsKey("perc.db.cms.backend"));
  }

  @Test
  void backendLabelForTypeMapsKnownTypesIncludingPostgres() {
    assertEquals("H2", DbInstallConfigResolver.backendLabelForType("h2"));
    assertEquals("DERBY", DbInstallConfigResolver.backendLabelForType("derby"));
    assertEquals("MYSQL", DbInstallConfigResolver.backendLabelForType("mysql"));
    assertEquals("POSTGRES", DbInstallConfigResolver.backendLabelForType("postgresql"));
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> DbInstallConfigResolver.backendLabelForType("cockroach"));
    assertTrue(ex.getMessage().contains("cockroach"));
    assertTrue(ex.getMessage().toLowerCase().contains("allowed"));
  }

  @Test
  void structuredPostgresqlMapsCmsFields() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "postgresql");
    opts.put("db.host", "pg.example.com");
    opts.put("db.port", "5432");
    opts.put("db.name", "percussion");
    opts.put("db.user", "cms");
    opts.put("db.password", "s3cret");

    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    Map<String, String> p = cfg.systemProperties();
    assertEquals("postgresql", p.get("perc.db.type"));
    assertEquals("POSTGRES", p.get("perc.db.cms.backend"));
    assertEquals("postgresql", p.get("perc.db.cms.driverName"));
    assertEquals("org.postgresql.Driver", p.get("perc.db.cms.driverClass"));
    assertEquals("//pg.example.com:5432/percussion", p.get("perc.db.cms.server"));
    assertEquals("public", p.get("perc.db.cms.schema"));
    assertEquals("cms", p.get("perc.db.user"));
    assertEquals("s3cret", p.get("perc.db.password"));
    assertEquals("jdbc:postgresql://pg.example.com:5432/percussion", p.get("perc.db.dts.jdbcUrl"));
    assertEquals("org.hibernate.dialect.PostgreSQLDialect", p.get("perc.db.dts.hibernateDialect"));
  }

  @Test
  void structuredPostgresAliasNormalizesToPostgresql() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "postgres");
    opts.put("db.host", "pg.example.com");
    opts.put("db.port", "5432");
    opts.put("db.name", "cmsdb");
    opts.put("db.user", "u");
    opts.put("db.password", "p");
    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    assertEquals("postgresql", cfg.systemProperties().get("perc.db.type"));
    assertEquals("POSTGRES", cfg.systemProperties().get("perc.db.cms.backend"));
  }

  @Test
  void dbpropsPostgresBackendAccepted() throws Exception {
    Path props = tempDir.resolve("rxrepository.postgresql.properties");
    Files.writeString(
        props,
        """
        DB_BACKEND=POSTGRES
        DB_SERVER=//db.example.com:5432/percussion
        DB_NAME=percussion
        DB_SCHEMA=public
        DB_DRIVER_NAME=postgresql
        DB_DRIVER_CLASS_NAME=org.postgresql.Driver
        UID=cms
        PWD=changeit
        """,
        StandardCharsets.UTF_8);
    Map<String, String> opts = new HashMap<>();
    opts.put("dbprops", props.toString());
    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    assertEquals("postgresql", cfg.systemProperties().get("perc.db.type"));
    assertEquals("POSTGRES", cfg.systemProperties().get("perc.db.cms.backend"));
    assertEquals("postgresql", cfg.systemProperties().get("perc.db.cms.driverName"));
    assertEquals("public", cfg.systemProperties().get("perc.db.cms.schema"));
    // Locks applyDtsHintsFromCms: "jdbc:postgresql:" + DB_SERVER("//host:port/db")
    // must yield jdbc:postgresql://host:port/db (two slashes after the scheme colon).
    assertEquals(
        "jdbc:postgresql://db.example.com:5432/percussion",
        cfg.systemProperties().get("perc.db.dts.jdbcUrl"));
    assertEquals(
        "org.hibernate.dialect.PostgreSQLDialect",
        cfg.systemProperties().get("perc.db.dts.hibernateDialect"));
  }

  @Test
  void structuredMysqlMapsCmsFieldsWithMariaDbDriver() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "mysql");
    opts.put("db.host", "db.example.com");
    opts.put("db.port", "3306");
    opts.put("db.name", "percussion");
    opts.put("db.user", "cms");
    opts.put("db.password", "s3cret");

    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    Map<String, String> p = cfg.systemProperties();
    assertEquals("mysql", p.get("perc.db.type"));
    assertEquals("MYSQL", p.get("perc.db.cms.backend"));
    assertEquals("mysql", p.get("perc.db.cms.driverName"));
    assertEquals("org.mariadb.jdbc.Driver", p.get("perc.db.cms.driverClass"));
    assertTrue(p.get("perc.db.cms.server").contains("db.example.com"));
    assertEquals("cms", p.get("perc.db.user"));
    assertEquals("s3cret", p.get("perc.db.password"));
  }

  @Test
  void structuredMysqlMissingFieldsThrowsWithoutPasswordInMessage() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "mysql");
    opts.put("db.password", "super-secret-pw");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> DbInstallConfigResolver.resolveDbConfig(opts));
    assertTrue(ex.getMessage().contains("db.host"));
    assertFalse(ex.getMessage().contains("super-secret-pw"));
  }

  @Test
  void structuredSqlServerMapsBackend() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "sqlserver");
    opts.put("db.host", "sql.example.com");
    opts.put("db.port", "1433");
    opts.put("db.name", "PercussionDB");
    opts.put("db.user", "sa");
    opts.put("db.password", "x");

    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    assertEquals("sqlserver", cfg.systemProperties().get("perc.db.type"));
    assertEquals("MSSQL", cfg.systemProperties().get("perc.db.cms.backend"));
    assertEquals("dbo", cfg.systemProperties().get("perc.db.cms.schema"));
  }

  @Test
  void structuredOracleMapsBackend() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "oracle");
    opts.put("db.host", "ora.example.com");
    opts.put("db.port", "1521");
    opts.put("db.name", "ORCL");
    opts.put("db.user", "cms");
    opts.put("db.password", "x");

    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    assertEquals("oracle", cfg.systemProperties().get("perc.db.type"));
    assertEquals("ORACLE", cfg.systemProperties().get("perc.db.cms.backend"));
    assertEquals("oracle:thin", cfg.systemProperties().get("perc.db.cms.driverName"));
    // Easy Connect service form (required for multi-tenant service names such as XEPDB1).
    assertEquals("@//ora.example.com:1521/ORCL", cfg.systemProperties().get("perc.db.cms.server"));
    assertEquals(
        "jdbc:oracle:thin:@//ora.example.com:1521/ORCL",
        cfg.systemProperties().get("perc.db.dts.jdbcUrl"));
    // Product DB_NAME must be empty for Oracle (service lives only in DB_SERVER).
    assertEquals("", cfg.systemProperties().get("perc.db.cms.name"));
  }

  @Test
  void structuredOracleXepdb1UsesServiceEasyConnectForm() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "oracle");
    opts.put("db.host", "oracle");
    opts.put("db.port", "1521");
    opts.put("db.name", "XEPDB1");
    opts.put("db.user", "percuser");
    opts.put("db.password", "x");
    opts.put("db.schema", "percuser");

    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    assertEquals("@//oracle:1521/XEPDB1", cfg.systemProperties().get("perc.db.cms.server"));
    // CLI --db.name is still required for service composition; cms.name / DB_NAME stays blank.
    assertEquals("XEPDB1", cfg.systemProperties().get("perc.db.name"));
    assertEquals("", cfg.systemProperties().get("perc.db.cms.name"));
    assertEquals("percuser", cfg.systemProperties().get("perc.db.cms.schema"));
  }

  @Test
  void structuredOracleRequiresDbNameForServiceOrSid() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "oracle");
    opts.put("db.host", "ora.example.com");
    opts.put("db.port", "1521");
    opts.put("db.user", "cms");
    opts.put("db.password", "x");
    // no db.name → must not produce @host:port:
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> DbInstallConfigResolver.resolveDbConfig(opts));
    assertTrue(ex.getMessage().contains("db.name"));
  }

  @Test
  void structuredSqlServerDefaultsSchemaToDbo() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "sqlserver");
    opts.put("db.host", "sql.example.com");
    opts.put("db.port", "1433");
    opts.put("db.name", "PercussionDB");
    opts.put("db.user", "sa");
    opts.put("db.password", "x");

    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    assertEquals("dbo", cfg.systemProperties().get("perc.db.cms.schema"));
  }

  @Test
  void dbpropsMysqlFileMapsKeys() throws Exception {
    Path props = tempDir.resolve("mysql.properties");
    Files.writeString(
        props,
        """
        DB_BACKEND=MYSQL
        DB_SERVER=//db.example.com:3306/percussion
        DB_NAME=percussion
        DB_SCHEMA=
        DB_DRIVER_NAME=mysql
        DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
        UID=cms
        PWD=file-secret
        """,
        StandardCharsets.UTF_8);

    Map<String, String> opts = Map.of("dbprops", props.toString());
    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    Map<String, String> p = cfg.systemProperties();
    assertEquals("dbprops", cfg.source());
    assertEquals("mysql", p.get("perc.db.type"));
    assertEquals("MYSQL", p.get("perc.db.cms.backend"));
    assertEquals("//db.example.com:3306/percussion", p.get("perc.db.cms.server"));
    assertEquals("org.mariadb.jdbc.Driver", p.get("perc.db.cms.driverClass"));
    assertEquals("cms", p.get("perc.db.user"));
    assertEquals("file-secret", p.get("perc.db.password"));
  }

  @Test
  void dbpropsMssqlAndOracle() throws Exception {
    Path mssql = tempDir.resolve("mssql.properties");
    Files.writeString(
        mssql,
        """
        DB_BACKEND=MSSQL
        DB_SERVER=//sql:1433;databaseName=P
        DB_NAME=P
        DB_SCHEMA=dbo
        DB_DRIVER_NAME=sqlserver
        DB_DRIVER_CLASS_NAME=com.microsoft.sqlserver.jdbc.SQLServerDriver
        UID=sa
        PWD=x
        """,
        StandardCharsets.UTF_8);
    DbInstallConfigResolver.ResolvedDbConfig m =
        DbInstallConfigResolver.resolveDbConfig(Map.of("dbprops", mssql.toString()));
    assertEquals("sqlserver", m.systemProperties().get("perc.db.type"));
    assertEquals("MSSQL", m.systemProperties().get("perc.db.cms.backend"));
    assertEquals("dbo", m.systemProperties().get("perc.db.cms.schema"));

    Path ora = tempDir.resolve("ora.properties");
    Files.writeString(
        ora,
        """
        DB_BACKEND=ORACLE
        DB_SERVER=@ora:1521:ORCL
        DB_NAME=
        DB_SCHEMA=CMS
        DB_DRIVER_NAME=oracle:thin
        DB_DRIVER_CLASS_NAME=oracle.jdbc.OracleDriver
        UID=cms
        PWD=x
        """,
        StandardCharsets.UTF_8);
    DbInstallConfigResolver.ResolvedDbConfig o =
        DbInstallConfigResolver.resolveDbConfig(Map.of("dbprops", ora.toString()));
    assertEquals("oracle", o.systemProperties().get("perc.db.type"));
    assertEquals("ORACLE", o.systemProperties().get("perc.db.cms.backend"));
    assertEquals("@ora:1521:ORCL", o.systemProperties().get("perc.db.cms.server"));
  }

  @Test
  void dbpropsTakesPrecedenceOverConflictingCliType() throws Exception {
    Path props = tempDir.resolve("mysql.properties");
    Files.writeString(
        props,
        """
        DB_BACKEND=MYSQL
        DB_SERVER=//db:3306/p
        DB_NAME=p
        DB_DRIVER_NAME=mysql
        DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
        UID=u
        PWD=p
        """,
        StandardCharsets.UTF_8);
    Map<String, String> opts = new HashMap<>();
    opts.put("dbprops", props.toString());
    opts.put("db.type", "sqlserver");
    opts.put("db.host", "ignored");
    opts.put("db.port", "1433");
    opts.put("db.name", "ignored");
    opts.put("db.user", "ignored");
    opts.put("db.password", "ignored");

    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    assertEquals("mysql", cfg.systemProperties().get("perc.db.type"));
    assertEquals("MYSQL", cfg.systemProperties().get("perc.db.cms.backend"));
  }

  @Test
  void missingDbpropsPathFailsWithPathInMessage() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                DbInstallConfigResolver.resolveDbConfig(
                    Map.of("dbprops", tempDir.resolve("missing.properties").toString())));
    assertTrue(
        ex.getMessage().toLowerCase().contains("not found")
            || ex.getMessage().toLowerCase().contains("not readable"));
  }

  @Test
  void incompleteDbpropsListsKeysNotPassword() throws Exception {
    Path props = tempDir.resolve("incomplete.properties");
    Files.writeString(
        props,
        """
        DB_BACKEND=MYSQL
        PWD=do-not-leak-this
        """,
        StandardCharsets.UTF_8);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> DbInstallConfigResolver.resolveDbConfig(Map.of("dbprops", props.toString())));
    assertTrue(ex.getMessage().contains("DB_SERVER") || ex.getMessage().contains("UID"));
    assertFalse(ex.getMessage().contains("do-not-leak-this"));
  }

  @Test
  void unknownBackendFailsWithAllowedList() throws Exception {
    Path props = tempDir.resolve("bad.properties");
    Files.writeString(props, "DB_BACKEND=COCKROACH\n", StandardCharsets.UTF_8);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> DbInstallConfigResolver.resolveDbConfig(Map.of("dbprops", props.toString())));
    assertTrue(ex.getMessage().contains("MYSQL"));
    assertTrue(ex.getMessage().contains("ORACLE"));
    assertTrue(ex.getMessage().contains("POSTGRES"));
  }

  @Test
  void parseArgsInstallPathAndOptions() {
    DbInstallConfigResolver.ParsedArgs parsed =
        DbInstallConfigResolver.parseArgs(
            new String[] {"/opt/install", "--dbprops=/tmp/a.properties", "--db.type=mysql"});
    assertEquals(Path.of("/opt/install"), parsed.installPath());
    assertEquals("/tmp/a.properties", parsed.options().get("dbprops"));
    assertEquals("mysql", parsed.options().get("db.type"));
  }

  @Test
  void structuredH2SurfacesCmdbPasswordAsSystemProperty() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "h2");
    opts.put(InteractiveDbConfigCollector.EMBEDDED_H2_DB_PASSWORD_KEY, "operator-chosen-pwd");
    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);
    assertEquals("h2", cfg.systemProperties().get("perc.db.type"));
    assertEquals(
        "operator-chosen-pwd",
        cfg.systemProperties().get("cmdb.password"),
        "interactive H2 installs must expose cmdb.password for ANT installRepository.xml");
  }

  @Test
  void structuredH2WithoutPasswordDoesNotEmitCmdbPassword() {
    // Silent / non-interactive path: no EMBEDDED_H2_DB_PASSWORD_KEY supplied; ANT's
    // PSGenerateRepositoryPassword (random mode) takes over and emits cmdb.password
    // itself. The resolver must not invent a value here.
    DbInstallConfigResolver.ResolvedDbConfig cfg =
        DbInstallConfigResolver.resolveDbConfig(Map.of("db.type", "h2"));
    assertEquals("h2", cfg.systemProperties().get("perc.db.type"));
    assertFalse(
        cfg.systemProperties().containsKey("cmdb.password"),
        "silent path must not synthesize a cmdb.password; ANT generates it");
  }

  @Test
  void parseDemoSitesFlagDefaultsFalseAndHonorsAliases() {
    String prev = System.getProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);
    try {
      System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);
      assertFalse(DbInstallConfigResolver.parseDemoSitesFlag(null));
      assertFalse(DbInstallConfigResolver.parseDemoSitesFlag(Map.of()));
      assertTrue(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "true")));
      assertTrue(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "yes")));
      assertTrue(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "Y")));
      assertTrue(DbInstallConfigResolver.parseDemoSitesFlag(Map.of("install.demo.sites", "true")));
      assertFalse(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "false")));
      assertFalse(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "no")));

      // CLI flag wins when both are set; system property is only consulted when
      // CLI is blank / unset. Mirrors ObsoleteInstallDirCleaner.parseCleanInstallDirFlag.
      System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "true");
      assertFalse(
          DbInstallConfigResolver.parseDemoSitesFlag(
              Map.of(DbInstallConfigResolver.DEMO_SITES_KEY, "false")));

      // System property is the fallback when CLI is absent.
      System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "true");
      assertTrue(DbInstallConfigResolver.parseDemoSitesFlag(Map.of()));
      System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "no");
      assertFalse(DbInstallConfigResolver.parseDemoSitesFlag(Map.of()));

      // Unparseable values fall back to false (Boolean.parseBoolean("xyz") == false).
      System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, "maybe");
      assertFalse(DbInstallConfigResolver.parseDemoSitesFlag(Map.of()));
    } finally {
      if (prev == null) {
        System.clearProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY);
      } else {
        System.setProperty(DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY, prev);
      }
    }
  }
}
