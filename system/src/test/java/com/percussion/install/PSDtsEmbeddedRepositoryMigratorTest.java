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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    var d =
        PSDtsEmbeddedRepositoryMigrator.detect(server, "percmetadata", derby);
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
    Path props = server.resolve("webapps").resolve("perc-metadata-services").resolve("WEB-INF")
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
}
