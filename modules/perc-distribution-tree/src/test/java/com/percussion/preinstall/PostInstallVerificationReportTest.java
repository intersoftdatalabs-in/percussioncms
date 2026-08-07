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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral coverage for post-install DB verification report (issue #2337 / parent #934 AC-5):
 * field inclusion for embedded H2 and one external backend, plus password/secret redaction.
 */
@Tag("UnitTest")
class PostInstallVerificationReportTest {

  @TempDir Path tempDir;

  @Test
  void embeddedH2DefaultIncludesTypePathSourceAndNeverPassword() {
    DbInstallConfigResolver.ResolvedDbConfig cfg =
        DbInstallConfigResolver.resolveDbConfig(Map.of());
    Path install = tempDir.resolve("cms-h2");
    String report = PostInstallVerificationReport.build(install, cfg);

    assertTrue(
        report.contains(PostInstallVerificationReport.SECTION_TITLE),
        "must use a clearly labeled section; was:\n" + report);
    assertTrue(report.contains("db.type"), report);
    assertTrue(report.toLowerCase().contains("h2"), report);
    // Empty CLI options still resolve via the structured path with product H2 defaults
    // (source label is "structured" today; explicit "default" covered below).
    assertTrue(
        report.contains("Config source: structured") || report.contains("Config source: default"),
        report);
    assertTrue(
        report.contains(PostInstallVerificationReport.EMBEDDED_REPOSITORY_RELATIVE)
            || report.contains("Repository"),
        "embedded path expected; was:\n" + report);
    assertTrue(report.contains("Location"), report);
    assertTrue(report.contains("Database"), report);
    assertTrue(report.contains("Schema"), report);
    assertTrue(report.contains("User"), report);
    // Default resolve may not put cmdb.password on the map, but still assert no secret leakage
    // patterns if a future change injects one.
    assertFalse(report.toLowerCase().contains("password="), report);
    assertFalse(report.contains("cmdb.password"), report);
  }

  @Test
  void embeddedH2WithOperatorPasswordNeverEchoesSecret() {
    Map<String, String> props = new HashMap<>();
    props.put("perc.db.type", "h2");
    props.put("cmdb.password", "super-secret-h2-pwd");
    DbInstallConfigResolver.ResolvedDbConfig cfg =
        new DbInstallConfigResolver.ResolvedDbConfig(props, "structured");
    Path install = tempDir.resolve("cms-h2-op");
    String report = PostInstallVerificationReport.build(install, cfg);

    assertTrue(report.contains("h2"), report);
    assertTrue(report.contains("Config source: structured"), report);
    assertTrue(report.contains("embedded"), report.toLowerCase());
    assertFalse(
        report.contains("super-secret-h2-pwd"),
        "must never echo operator H2 password; was:\n" + report);
    assertFalse(report.contains("cmdb.password"), report);
  }

  @Test
  void externalMysqlIncludesHostPortDatabaseUserSourceAndRedactsPassword() {
    Map<String, String> opts = new HashMap<>();
    opts.put("db.type", "mysql");
    opts.put("db.host", "db.example.com");
    opts.put("db.port", "3306");
    opts.put("db.name", "percussion");
    opts.put("db.user", "cms_user");
    opts.put("db.password", "s3cret-must-not-appear");
    DbInstallConfigResolver.ResolvedDbConfig cfg = DbInstallConfigResolver.resolveDbConfig(opts);

    Path install = tempDir.resolve("cms-mysql");
    String report = PostInstallVerificationReport.build(install, cfg);

    assertTrue(report.contains(PostInstallVerificationReport.SECTION_TITLE), report);
    assertTrue(report.contains("mysql"), report.toLowerCase());
    assertTrue(report.contains("db.example.com"), report);
    assertTrue(report.contains("3306"), report);
    assertTrue(report.contains("percussion"), report);
    assertTrue(report.contains("cms_user"), report);
    assertTrue(report.contains("Config source: structured"), report);
    assertTrue(report.contains("Host"), report);
    assertTrue(report.contains("Port"), report);
    assertTrue(report.contains("Database"), report);
    assertTrue(report.contains("User"), report);
    assertFalse(
        report.contains("s3cret-must-not-appear"),
        "password must be redacted from post-install report; was:\n" + report);
    assertFalse(report.toLowerCase().contains("password="), report);
    // Embedded path line should not appear for external backends
    assertFalse(
        report.contains(PostInstallVerificationReport.EMBEDDED_REPOSITORY_RELATIVE),
        "external backend must not claim embedded Repository/CMDB; was:\n" + report);
  }

  @Test
  void defaultSourceLabelIsReportedWhenConstructedAsDefault() {
    Map<String, String> props = new HashMap<>();
    props.put("perc.db.type", "h2");
    DbInstallConfigResolver.ResolvedDbConfig cfg =
        new DbInstallConfigResolver.ResolvedDbConfig(props, "default");
    String report = PostInstallVerificationReport.build(tempDir.resolve("def"), cfg);
    assertTrue(report.contains("Config source: default"), report);
    assertTrue(report.contains("h2"), report);
  }

  @Test
  void dbpropsSourceLabelIsReported() {
    Map<String, String> props = new HashMap<>();
    props.put("perc.db.type", "postgresql");
    props.put("perc.db.host", "pg.internal");
    props.put("perc.db.port", "5432");
    props.put("perc.db.name", "rxdb");
    props.put("perc.db.schema", "public");
    props.put("perc.db.user", "rx");
    props.put("perc.db.password", "dbprops-pwd-hidden");
    DbInstallConfigResolver.ResolvedDbConfig cfg =
        new DbInstallConfigResolver.ResolvedDbConfig(props, "dbprops");
    String report = PostInstallVerificationReport.build(tempDir.resolve("pg"), cfg);

    assertTrue(report.contains("Config source: dbprops"), report);
    assertTrue(report.contains("postgresql") || report.contains("POSTGRES"), report);
    assertTrue(report.contains("pg.internal"), report);
    assertTrue(report.contains("5432"), report);
    assertTrue(report.contains("rxdb"), report);
    assertTrue(report.contains("public"), report);
    assertTrue(report.contains("rx"), report);
    assertFalse(report.contains("dbprops-pwd-hidden"), report);
  }

  @Test
  void emitSendsEveryLineToConsumer() {
    DbInstallConfigResolver.ResolvedDbConfig cfg =
        DbInstallConfigResolver.resolveDbConfig(Map.of());
    List<String> lines = new ArrayList<>();
    PostInstallVerificationReport.emit(tempDir.resolve("emit"), cfg, lines::add);
    assertFalse(lines.isEmpty());
    assertTrue(
        lines.stream().anyMatch(l -> l.contains(PostInstallVerificationReport.SECTION_TITLE)),
        lines.toString());
  }

  @Test
  void nullConfigProducesUnknownReportWithoutThrowing() {
    String report = PostInstallVerificationReport.build(tempDir.resolve("x"), null);
    assertTrue(report.contains(PostInstallVerificationReport.SECTION_TITLE), report);
    assertTrue(report.contains("unknown"), report.toLowerCase());
  }
}
