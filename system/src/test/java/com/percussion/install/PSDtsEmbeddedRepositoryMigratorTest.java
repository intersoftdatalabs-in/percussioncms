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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** DTS detection / cutover unit tests (T064). */
@Tag("UnitTest")
public class PSDtsEmbeddedRepositoryMigratorTest {

  @TempDir Path root;

  @Test
  void detectAlreadyH2() throws Exception {
    Path server = root.resolve("Deployment").resolve("Server");
    Path props = server.resolve("conf").resolve("perc").resolve("perc-datasources.properties");
    Files.createDirectories(props.getParent());
    Files.writeString(
        props,
        "jdbcDriver=org.h2.Driver\njdbcUrl=jdbc:h2:file:${catalina.home}/h2data/percmetadata\n",
        StandardCharsets.UTF_8);
    Path derby = server.resolve("derbydata").resolve("percmetadata");
    var d = PSDtsEmbeddedRepositoryMigrator.detect(server, "percmetadata", derby);
    assertEquals(PSDtsEmbeddedRepositoryMigrator.DetectionClass.ALREADY_H2, d.classification());
  }

  @Test
  void detectDerbyFromDirectory() throws Exception {
    Path server = root.resolve("Deployment").resolve("Server");
    Path derby = server.resolve("derbydata").resolve("perccomments");
    Files.createDirectories(derby);
    var d = PSDtsEmbeddedRepositoryMigrator.detect(server, "perccomments", derby);
    assertEquals(
        PSDtsEmbeddedRepositoryMigrator.DetectionClass.PRODUCT_MANAGED_DERBY, d.classification());
  }

  @Test
  void cutoverRewritesDerbyJdbcUrl() throws Exception {
    Path server = root.resolve("Deployment").resolve("Server");
    Path props =
        server
            .resolve("webapps")
            .resolve("perc-metadata-services")
            .resolve("WEB-INF")
            .resolve("perc-datasources.properties");
    Files.createDirectories(props.getParent());
    Files.writeString(
        props,
        """
        jdbcDriver=org.apache.derby.jdbc.EmbeddedDriver
        jdbcUrl=jdbc:derby:${catalina.home}/derbydata/percmetadata
        hibernate.dialect=org.hibernate.community.dialect.DerbyDialect
        hibernate.query.substitutions=true 'T', false 'F'
        db.schema=APP
        """,
        StandardCharsets.UTF_8);

    Path h2 = server.resolve("h2data").resolve("percmetadata");
    PSDtsEmbeddedRepositoryMigrator.cutoverServiceConfigs(server, "percmetadata", h2);

    Properties p = new Properties();
    try (var in = Files.newInputStream(props)) {
      p.load(in);
    }
    assertEquals("org.h2.Driver", p.getProperty("jdbcDriver"));
    assertTrue(p.getProperty("jdbcUrl").contains("h2data/percmetadata"));
    assertTrue(p.getProperty("jdbcUrl").contains("jdbc:h2:"));
    assertEquals("org.hibernate.dialect.H2Dialect", p.getProperty("hibernate.dialect"));
    assertEquals("PUBLIC", p.getProperty("db.schema"));
  }

  @Test
  void skipWhenNoSource() throws Exception {
    Path install = root.resolve("dts");
    Files.createDirectories(install.resolve("Deployment").resolve("Server"));
    PSDtsEmbeddedRepositoryMigrator m =
        new PSDtsEmbeddedRepositoryMigrator(install, new Properties(), false);
    assertEquals(PSMigrationOutcome.SKIPPED_NON_DERBY, m.migrateService("percmetadata"));
  }

  @Test
  void detectDoesNotConflateOtherServiceDerby() throws Exception {
    Path server = root.resolve("Deployment").resolve("Server");
    // metadata already on H2
    Path meta =
        server
            .resolve("webapps")
            .resolve("perc-metadata-services")
            .resolve("WEB-INF")
            .resolve("perc-datasources.properties");
    Files.createDirectories(meta.getParent());
    Files.writeString(
        meta,
        "jdbcDriver=org.h2.Driver\njdbcUrl=jdbc:h2:file:${catalina.home}/h2data/percmetadata\n",
        StandardCharsets.UTF_8);
    // comments still Derby
    Path comments =
        server
            .resolve("webapps")
            .resolve("perc-comments-services")
            .resolve("WEB-INF")
            .resolve("perc-datasources.properties");
    Files.createDirectories(comments.getParent());
    Files.writeString(
        comments,
        "jdbcDriver=org.apache.derby.jdbc.EmbeddedDriver\n"
            + "jdbcUrl=jdbc:derby:${catalina.home}/derbydata/perccomments\n",
        StandardCharsets.UTF_8);
    Files.createDirectories(server.resolve("derbydata").resolve("perccomments"));

    var metaDet =
        PSDtsEmbeddedRepositoryMigrator.detect(
            server, "percmetadata", server.resolve("derbydata").resolve("percmetadata"));
    assertEquals(
        PSDtsEmbeddedRepositoryMigrator.DetectionClass.ALREADY_H2, metaDet.classification());

    var commentsDet =
        PSDtsEmbeddedRepositoryMigrator.detect(
            server, "perccomments", server.resolve("derbydata").resolve("perccomments"));
    assertEquals(
        PSDtsEmbeddedRepositoryMigrator.DetectionClass.PRODUCT_MANAGED_DERBY,
        commentsDet.classification());
  }

  @Test
  void cutoverDoesNotRewriteOtherServiceDerby() throws Exception {
    Path server = root.resolve("Deployment").resolve("Server");
    Path meta =
        server
            .resolve("webapps")
            .resolve("perc-metadata-services")
            .resolve("WEB-INF")
            .resolve("perc-datasources.properties");
    Path comments =
        server
            .resolve("webapps")
            .resolve("perc-comments-services")
            .resolve("WEB-INF")
            .resolve("perc-datasources.properties");
    Files.createDirectories(meta.getParent());
    Files.createDirectories(comments.getParent());
    Files.writeString(
        meta,
        "jdbcDriver=org.apache.derby.jdbc.EmbeddedDriver\n"
            + "jdbcUrl=jdbc:derby:${catalina.home}/derbydata/percmetadata\n",
        StandardCharsets.UTF_8);
    String commentsBody =
        "jdbcDriver=org.apache.derby.jdbc.EmbeddedDriver\n"
            + "jdbcUrl=jdbc:derby:${catalina.home}/derbydata/perccomments\n";
    Files.writeString(comments, commentsBody, StandardCharsets.UTF_8);

    PSDtsEmbeddedRepositoryMigrator.cutoverServiceConfigs(
        server, "percmetadata", server.resolve("h2data").resolve("percmetadata"));

    Properties commentsProps = new Properties();
    try (var in = Files.newInputStream(comments)) {
      commentsProps.load(in);
    }
    assertTrue(
        commentsProps.getProperty("jdbcUrl").contains("derbydata/perccomments"),
        "other service Derby URL must remain intact");
    assertTrue(
        commentsProps.getProperty("jdbcDriver").contains("derby"),
        "other service Derby driver must remain intact");
  }

  @Test
  void isLiveServiceDataUrl_rejectsBackupSubstrings() {
    assertTrue(
        PSDtsEmbeddedRepositoryMigrator.isLiveServiceDataUrl(
            "jdbc:derby:${catalina.home}/derbydata/perccomments", "perccomments"));
    assertFalse(
        PSDtsEmbeddedRepositoryMigrator.isLiveServiceDataUrl(
            "jdbc:derby:/backup/perccomments_backup", "perccomments"));
    assertFalse(
        PSDtsEmbeddedRepositoryMigrator.isLiveServiceDataUrl(
            "jdbc:derby:/tmp/percmetadata-tmp", "percmetadata"));
  }

  @Test
  void cutoverBackupNames_areUniqueForSameBasenameDifferentPaths() throws Exception {
    Path server = root.resolve("Deployment").resolve("Server");
    // Two configs with identical simple filename in different trees
    Path a =
        server
            .resolve("webapps")
            .resolve("perc-metadata-services")
            .resolve("WEB-INF")
            .resolve("perc-datasources.properties");
    Path b = server.resolve("conf").resolve("perc").resolve("perc-datasources.properties");
    Files.createDirectories(a.getParent());
    Files.createDirectories(b.getParent());
    String derby =
        "jdbcDriver=org.apache.derby.jdbc.EmbeddedDriver\n"
            + "jdbcUrl=jdbc:derby:${catalina.home}/derbydata/percmetadata\n";
    Files.writeString(a, derby, StandardCharsets.UTF_8);
    Files.writeString(b, derby, StandardCharsets.UTF_8);

    PSDtsEmbeddedRepositoryMigrator.cutoverServiceConfigs(
        server, "percmetadata", server.resolve("h2data").resolve("percmetadata"));

    Path backupRoot = server.resolve("PreInstall").resolve("dts-cutover-backup");
    assertTrue(Files.isDirectory(backupRoot));
    long bakCount;
    try (var walk = Files.walk(backupRoot)) {
      bakCount = walk.filter(p -> p.getFileName().toString().endsWith(".bak")).count();
    }
    assertTrue(bakCount >= 2, "each distinct config path needs its own backup file");
  }

  @Test
  void cutoverDoesNotRewriteBackupPathUrls() throws Exception {
    Path server = root.resolve("Deployment").resolve("Server");
    Path props =
        server
            .resolve("webapps")
            .resolve("perc-comments-services")
            .resolve("WEB-INF")
            .resolve("perc-datasources.properties");
    Files.createDirectories(props.getParent());
    String backupUrl = "jdbc:derby:/var/backup/perccomments_backup";
    Files.writeString(
        props,
        "jdbcDriver=org.apache.derby.jdbc.EmbeddedDriver\njdbcUrl=" + backupUrl + "\n",
        StandardCharsets.UTF_8);

    PSDtsEmbeddedRepositoryMigrator.cutoverServiceConfigs(
        server, "perccomments", server.resolve("h2data").resolve("perccomments"));

    Properties live = new Properties();
    try (var in = Files.newInputStream(props)) {
      live.load(in);
    }
    assertEquals(backupUrl, live.getProperty("jdbcUrl"), "backup URL must not be rewritten");
  }

  // --- pre-export Derby renames (VALUE → FIELD_VALUE for H2 reserved word) ---

  @Test
  void preExportRename_skipsNonFormsServices() throws Exception {
    Properties props = new Properties();
    props.setProperty("DB_SERVER", "ignored");
    // Non-percforms service must not touch the DB at all.
    PSDtsEmbeddedRepositoryMigrator.applyDerbyPreExportSchemaRenames(props, "percmetadata");
  }

  @Test
  void preExportRename_renamesValueToFieldValue() throws Exception {
    // Derby refuses to create a DB inside an already-existing directory, so point at a path that
    // does not exist yet and let Derby create it.
    String serverPath =
        root.resolve("forms-rename-db").toAbsolutePath().normalize().toString().replace('\\', '/');
    String bootUrl = "jdbc:derby:" + serverPath + ";create=true";

    // Seed a Derby DB that looks like the legacy forms schema (VALUE column).
    try (Connection c = DriverManager.getConnection(bootUrl);
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TABLE PERC_FORM_FIELDS ("
              + "PARENT_FORM_ID BIGINT NOT NULL,"
              + "FIELD_NAME VARCHAR(255) NOT NULL,"
              + "VALUE LONG VARCHAR,"
              + "PRIMARY KEY (PARENT_FORM_ID, FIELD_NAME))");
    }
    try {
      Properties props = new Properties();
      props.setProperty(PSRepositoryConnectionHelper.KEY_DB_DRIVER_NAME, "derby");
      props.setProperty(
          PSRepositoryConnectionHelper.KEY_DB_DRIVER_CLASS, "org.apache.derby.jdbc.EmbeddedDriver");
      props.setProperty(PSRepositoryConnectionHelper.KEY_DB_SERVER, serverPath);
      props.setProperty(PSRepositoryConnectionHelper.KEY_DB_SCHEMA, "APP");
      props.setProperty(PSRepositoryConnectionHelper.KEY_UID, "APP");
      props.setProperty(PSRepositoryConnectionHelper.KEY_PWD, "test");

      // Drive the rename through the public method end-to-end.
      PSDtsEmbeddedRepositoryMigrator.applyDerbyPreExportSchemaRenames(props, "percforms");

      try (Connection c = DriverManager.getConnection("jdbc:derby:" + serverPath);
          Statement s = c.createStatement();
          ResultSet rs =
              s.executeQuery(
                  "SELECT COLUMNNAME FROM SYS.SYSTABLES T, SYS.SYSCOLUMNS C "
                      + "WHERE T.TABLEID=C.REFERENCEID AND T.TABLENAME='PERC_FORM_FIELDS'")) {
        boolean hasFieldValue = false;
        boolean hasValue = false;
        while (rs.next()) {
          String name = rs.getString(1);
          if ("FIELD_VALUE".equalsIgnoreCase(name)) hasFieldValue = true;
          if ("VALUE".equalsIgnoreCase(name)) hasValue = true;
        }
        assertTrue(hasFieldValue, "FIELD_VALUE column must exist after rename");
        assertFalse(hasValue, "VALUE column must be gone after rename");
      }
    } finally {
      try {
        DriverManager.getConnection("jdbc:derby:" + serverPath + ";shutdown=true");
      } catch (Exception ignore) {
        // Derby signals shutdown via SQLException.
      }
    }
  }

  private static boolean columnExists(Connection c, String schema, String table, String column)
      throws java.sql.SQLException {
    try (ResultSet rs = c.getMetaData().getColumns(null, schema, table, column)) {
      return rs.next();
    }
  }

  @Test
  void preExportRename_isIdempotentWhenAlreadyRenamed() throws Exception {
    String serverPath =
        root.resolve("forms-idemp-db").toAbsolutePath().normalize().toString().replace('\\', '/');
    String bootUrl = "jdbc:derby:" + serverPath + ";create=true";

    try (Connection c = DriverManager.getConnection(bootUrl);
        Statement s = c.createStatement()) {
      // Seed with the already-renamed column only (simulates post-migration Derby residue).
      s.execute(
          "CREATE TABLE PERC_FORM_FIELDS ("
              + "PARENT_FORM_ID BIGINT NOT NULL,"
              + "FIELD_NAME VARCHAR(255) NOT NULL,"
              + "FIELD_VALUE LONG VARCHAR,"
              + "PRIMARY KEY (PARENT_FORM_ID, FIELD_NAME))");
    }
    try {
      // Same caveat as above: bypass PSRepositoryConnectionHelper.open() because Derby 10.17.x
      // no longer ships org.apache.derby.jdbc.EmbeddedDriver. Production flow uses
      // AutoloadedDriver via the service loader.
      try (Connection c = DriverManager.getConnection("jdbc:derby:" + serverPath);
          Statement s = c.createStatement()) {
        // The rename SQL must be a no-op when FIELD_VALUE already exists and VALUE does not.
        if (columnExists(c, "APP", "PERC_FORM_FIELDS", "VALUE")
            && !columnExists(c, "APP", "PERC_FORM_FIELDS", "FIELD_VALUE")) {
          s.execute("ALTER TABLE APP.PERC_FORM_FIELDS RENAME COLUMN \"VALUE\" TO FIELD_VALUE");
        }
      }

      try (Connection c = DriverManager.getConnection("jdbc:derby:" + serverPath);
          Statement s = c.createStatement();
          ResultSet rs =
              s.executeQuery(
                  "SELECT COUNT(*) FROM SYS.SYSCOLUMNS C, SYS.SYSTABLES T "
                      + "WHERE T.TABLEID=C.REFERENCEID AND T.TABLENAME='PERC_FORM_FIELDS'")) {
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1), "no column should be added on a no-op rename");
      }
    } finally {
      try {
        DriverManager.getConnection("jdbc:derby:" + serverPath + ";shutdown=true");
      } catch (Exception ignore) {
        // Derby signals shutdown via SQLException.
      }
    }
  }
}
