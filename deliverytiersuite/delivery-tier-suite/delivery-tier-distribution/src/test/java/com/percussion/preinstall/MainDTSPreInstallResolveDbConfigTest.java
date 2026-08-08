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

package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for {@link MainDTSPreInstall#resolveDbConfig(Map)} and the DTS datasource
 * property-write contract (issue #2338 / parent #934 AC-2).
 */
@Tag("UnitTest")
class MainDTSPreInstallResolveDbConfigTest {

  @TempDir Path tempDir;

  @Test
  void defaultIsH2WhenNoOptions() {
    MainDTSPreInstall.ResolvedDbConfig cfg = MainDTSPreInstall.resolveDbConfig(Map.of());
    assertEquals("h2", cfg.systemProperties().get("perc.db.type"));
    assertFalse(cfg.systemProperties().containsKey("perc.db.dts.jdbcUrl"));
    assertFalse(MainDTSPreInstall.shouldWriteDtsDatasourceProperties("h2"));
  }

  @Test
  void structuredPostgresqlMapsDtsJdbcAndWriteContract() throws IOException {
    Map<String, String> opts = externalOpts("postgresql", "pg.example.com", "5432", "percussion");
    opts.put("db.schema", "public");

    MainDTSPreInstall.ResolvedDbConfig cfg = MainDTSPreInstall.resolveDbConfig(opts);
    Map<String, String> p = cfg.systemProperties();
    assertEquals("postgresql", p.get("perc.db.type"));
    assertEquals("jdbc:postgresql://pg.example.com:5432/percussion", p.get("perc.db.dts.jdbcUrl"));
    assertEquals("org.postgresql.Driver", p.get("perc.db.dts.jdbcDriver"));
    assertEquals("org.hibernate.dialect.PostgreSQLDialect", p.get("perc.db.dts.hibernateDialect"));
    assertEquals("public", p.get("perc.db.dts.schema"));
    assertTrue(MainDTSPreInstall.shouldWriteDtsDatasourceProperties(p.get("perc.db.type")));

    Path propsFile = tempDir.resolve("perc-datasources.properties");
    // Seed a sample default so write merges rather than creating only our keys.
    Files.writeString(
        propsFile,
        "jdbcUrl=jdbc:h2:file:./DTSDB/percDB;IFEXISTS=TRUE\njdbcDriver=org.h2.Driver\n");
    MainDTSPreInstall.writeDtsDatasourceProperties(propsFile, p);

    Properties written = loadProps(propsFile);
    assertEquals("jdbc:postgresql://pg.example.com:5432/percussion", written.getProperty("jdbcUrl"));
    assertEquals("org.postgresql.Driver", written.getProperty("jdbcDriver"));
    assertEquals("org.hibernate.dialect.PostgreSQLDialect", written.getProperty("hibernate.dialect"));
    assertEquals("public", written.getProperty("db.schema"));
    assertEquals("cms", written.getProperty("db.username"));
    assertEquals("s3cret", written.getProperty("db.password"));
    assertEquals("true", written.getProperty("db.ssl.enabled"));
  }

  @Test
  void structuredOracleMapsDtsJdbcSymmetricallyWithCms() {
    Map<String, String> opts = externalOpts("oracle", "ora.example.com", "1521", "ORCL");
    opts.put("db.schema", "percuser");

    MainDTSPreInstall.ResolvedDbConfig cfg = MainDTSPreInstall.resolveDbConfig(opts);
    Map<String, String> p = cfg.systemProperties();
    assertEquals("oracle", p.get("perc.db.type"));
    assertEquals(
        "jdbc:oracle:thin:@//ora.example.com:1521/ORCL", p.get("perc.db.dts.jdbcUrl"));
    assertEquals(MainDTSPreInstall.ORACLE_DRIVER_CLASS, p.get("perc.db.dts.jdbcDriver"));
    assertEquals("org.hibernate.dialect.Oracle12cDialect", p.get("perc.db.dts.hibernateDialect"));
    assertEquals("percuser", p.get("perc.db.dts.schema"));
  }

  @Test
  void structuredOracleDefaultsSchemaToUserWhenSchemaOmitted() {
    Map<String, String> opts = externalOpts("oracle", "oracle", "1521", "XEPDB1");
    // no db.schema → use db.user (CMS parity)
    MainDTSPreInstall.ResolvedDbConfig cfg = MainDTSPreInstall.resolveDbConfig(opts);
    assertEquals("cms", cfg.systemProperties().get("perc.db.dts.schema"));
    assertEquals(
        "jdbc:oracle:thin:@//oracle:1521/XEPDB1",
        cfg.systemProperties().get("perc.db.dts.jdbcUrl"));
  }

  @Test
  void oraAliasNormalizesToOracle() {
    Map<String, String> opts = externalOpts("ora", "ora.example.com", "1521", "ORCL");
    MainDTSPreInstall.ResolvedDbConfig cfg = MainDTSPreInstall.resolveDbConfig(opts);
    assertEquals("oracle", cfg.systemProperties().get("perc.db.type"));
    assertTrue(cfg.systemProperties().get("perc.db.dts.jdbcUrl").startsWith("jdbc:oracle:thin:"));
  }

  @Test
  void structuredOracleWriteContractMatchesInstallDtsPropertyKeys() throws IOException {
    Map<String, String> opts = externalOpts("oracle", "ora.example.com", "1521", "XEPDB1");
    opts.put("db.schema", "PERC");
    MainDTSPreInstall.ResolvedDbConfig cfg = MainDTSPreInstall.resolveDbConfig(opts);
    Path propsFile = tempDir.resolve("perc-datasources-oracle.properties");
    MainDTSPreInstall.writeDtsDatasourceProperties(propsFile, cfg.systemProperties());
    Properties written = loadProps(propsFile);
    assertEquals(
        "jdbc:oracle:thin:@//ora.example.com:1521/XEPDB1", written.getProperty("jdbcUrl"));
    assertEquals(MainDTSPreInstall.ORACLE_DRIVER_CLASS, written.getProperty("jdbcDriver"));
    assertEquals("org.hibernate.dialect.Oracle12cDialect", written.getProperty("hibernate.dialect"));
    assertEquals("PERC", written.getProperty("db.schema"));
  }

  @Test
  void unknownDbTypeFailsFastWithoutSilentH2Fallback() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "cockroach");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> MainDTSPreInstall.resolveDbConfig(opts));
    assertTrue(ex.getMessage().contains("cockroach"));
    assertTrue(ex.getMessage().toLowerCase().contains("allowed"));
  }

  @Test
  void structuredOracleMissingFieldsThrowsWithoutPasswordInMessage() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "oracle");
    opts.put("db.password", "super-secret-pw");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> MainDTSPreInstall.resolveDbConfig(opts));
    assertTrue(ex.getMessage().contains("db.host"));
    assertFalse(ex.getMessage().contains("super-secret-pw"));
  }

  @Test
  void normalizeStructuredDbTypeAliases() {
    assertEquals("sqlserver", MainDTSPreInstall.normalizeStructuredDbType("MSSQL"));
    assertEquals("oracle", MainDTSPreInstall.normalizeStructuredDbType("ORA"));
    assertEquals("postgresql", MainDTSPreInstall.normalizeStructuredDbType("postgres"));
    assertEquals("h2", MainDTSPreInstall.normalizeStructuredDbType(null));
  }

  @Test
  void repositoryProbeBuildJdbcUrlSupportsOracle() {
    Map<String, String> p = new HashMap<>();
    p.put("perc.db.host", "ora.example.com");
    p.put("perc.db.port", "1521");
    p.put("perc.db.name", "XEPDB1");
    assertEquals(
        "jdbc:oracle:thin:@//ora.example.com:1521/XEPDB1",
        RepositoryConnectionProbe.buildJdbcUrl("oracle", p));
  }

  private static Map<String, String> externalOpts(
      String type, String host, String port, String name) {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", type);
    opts.put("db.host", host);
    opts.put("db.port", port);
    opts.put("db.name", name);
    opts.put("db.user", "cms");
    opts.put("db.password", "s3cret");
    return opts;
  }

  private static Properties loadProps(Path file) throws IOException {
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      props.load(in);
    }
    return props;
  }
}
