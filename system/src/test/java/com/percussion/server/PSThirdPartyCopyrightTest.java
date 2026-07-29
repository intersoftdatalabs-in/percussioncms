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

package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

/**
 * Verifies the third-party component copyright message emitted at server startup and rendered in
 * the UI About dialog. The message is sourced from the {@code thirdPartyCopyright} entry in the
 * server {@link ResourceBundle}. See issue #1529.
 */
public class PSThirdPartyCopyrightTest {

  @Test
  void bundleExposesNonEmptyThirdPartyCopyright() {
    String text = PSServer.getRes().getString("thirdPartyCopyright");
    assertNotNull(text, "thirdPartyCopyright bundle key must be defined");
    assertFalse(text.isBlank(), "thirdPartyCopyright must not be blank");
  }

  @Test
  void thirdPartyCopyrightMentionsCurrentBundledComponentVersions() {
    String text = PSServer.getRes().getString("thirdPartyCopyright");
    assertTrue(text.contains("Apache Software Foundation"), "should mention Apache Foundation");
    assertTrue(
        text.contains("Apache License, Version 2.0"), "should reference the Apache 2.0 license");
    assertTrue(
        text.contains("jTDS JDBC Driver v1.3.1"), "should mention current jTDS driver version");
    assertTrue(text.contains("H2 Database Engine v2.3.232"), "should mention current H2 version");
    assertTrue(
        text.contains("PostgreSQL JDBC Driver v42.7.12"),
        "should mention current PostgreSQL version");
    assertTrue(text.contains("MariaDB Connector/J v3.5.7"), "should mention current MariaDB version");
    assertTrue(
        text.contains("Microsoft JDBC Driver for SQL Server v13.3.1.jre11-preview"),
        "should mention current Microsoft SQL Server JDBC version");
    assertTrue(
        text.contains("Oracle JDBC Driver ojdbc17"), "should mention current Oracle JDBC driver");
    assertTrue(
        text.contains("Hibernate ORM v7.2.6"), "should mention current Hibernate ORM version");
    assertTrue(
        text.contains("Hibernate Validator v8.0.1"),
        "should mention current Hibernate Validator version");
    assertTrue(text.contains("Apache Log4j 2.25.4"), "should mention current Log4j version");
    assertTrue(text.contains("Apache Lucene 8.11.4"), "should mention current Lucene version");
    assertTrue(text.contains("Apache Tika 3.2.3"), "should mention current Tika version");
    assertTrue(text.contains("Apache PDFBox 3.0.6"), "should mention current PDFBox version");
    assertTrue(text.contains("Apache POI 5.4.0"), "should mention current POI version");
    assertTrue(text.contains("Apache CXF 4.1.4"), "should mention current CXF version");
    assertTrue(text.contains("Apache Derby 10.17.1.0"), "should mention current Derby version");
    assertTrue(
        text.contains("Apache ActiveMQ Artemis 2.50.0"), "should mention current Artemis version");
    assertTrue(text.contains("XStream v1.4.21"), "should mention current XStream version");
    assertTrue(text.contains("ASM v9"), "should mention current ASM major version");
    assertTrue(
        text.contains("Bouncy Castle v1.84"), "should mention current Bouncy Castle version");
    assertTrue(text.contains("TinyMCE 6.8.6"), "should mention current TinyMCE version");
    assertTrue(text.contains("react-router 8.3.0"), "should mention current react-router version");
  }

  @Test
  void thirdPartyCopyrightHasIntersoftAttributionAndSeparateJdbcDriversSection() {
    String text = PSServer.getRes().getString("thirdPartyCopyright");
    assertTrue(
        text.contains("Intersoft Data Labs"),
        "should credit Intersoft Data Labs for ongoing maintenance");
    assertTrue(
        text.contains("Bundled JDBC drivers and relational persistence"),
        "should list JDBC drivers as their own section, separate from other Apache licenses");
  }

  @Test
  void copyrightKeyCreditsIntersoftDataLabs() {
    String text = PSServer.getRes().getString("copyright");
    assertNotNull(text, "copyright bundle key must be defined");
    assertTrue(
        text.contains("Intersoft Data Labs"),
        "copyright must credit Intersoft Data Labs for ongoing maintenance");
  }

  @Test
  void thirdPartyCopyrightNoLongerMentionsDroppedComponents() {
    String text = PSServer.getRes().getString("thirdPartyCopyright");
    assertFalse(text.contains("Lato"), "Lato font is no longer bundled; reference must be removed");
    assertFalse(
        text.contains("http://www.apache.org/"),
        "use the canonical https URL for the Apache Foundation");
  }

  @Test
  void thirdPartyCopyrightContainsParagraphBreaksForUiRendering() {
    String text = PSServer.getRes().getString("thirdPartyCopyright");
    assertTrue(
        text.contains("\n"),
        "thirdPartyCopyright must contain newline separators so the UI About dialog "
            + "can render each attribution as a separate paragraph");
  }

  @Test
  void noticeFileMirrorsCurrentThirdPartyCopyright() throws Exception {
    Path repoRoot = repoRoot();
    Path notice = repoRoot.resolve("NOTICE.txt");
    assertTrue(Files.exists(notice), "NOTICE.txt missing from repo root");
    String body = new String(Files.readAllBytes(notice), StandardCharsets.UTF_8);

    assertTrue(body.contains("Intersoft Data Labs"), "NOTICE.txt must credit Intersoft Data Labs");
    assertTrue(
        body.contains("Bundled JDBC drivers and relational persistence"),
        "NOTICE.txt must have a dedicated JDBC drivers section");
    assertTrue(body.contains("ojdbc17"), "NOTICE.txt must list Oracle JDBC (ojdbc17)");
    assertTrue(
        body.contains("Microsoft JDBC Driver for SQL Server"),
        "NOTICE.txt must list Microsoft SQL Server JDBC");
    assertTrue(body.contains("jTDS JDBC Driver v1.3.1"), "NOTICE.txt must reflect current jTDS version");
    assertTrue(
        body.contains("Apache ActiveMQ Artemis 2.50.0"),
        "NOTICE.txt must reflect current Artemis version");
    assertTrue(
        body.contains("Apache Log4j 2.25.4"), "NOTICE.txt must reflect current Log4j version");
    assertTrue(
        body.contains("Bouncy Castle v1.84"),
        "NOTICE.txt must reflect current Bouncy Castle version");
    assertFalse(body.contains("Lato"), "NOTICE.txt must no longer reference dropped Lato font");
    assertFalse(body.contains("v1.2.2"), "NOTICE.txt must no longer reference outdated jTDS 1.2.2");
    assertFalse(
        body.contains("2003-2005, Joe Walnes"),
        "NOTICE.txt must reflect updated XStream copyright range");
  }

  private Path repoRoot() throws Exception {
    Path p = Paths.get(".").toAbsolutePath().normalize();
    for (int i = 0; i < 6 && p != null; i++) {
      if (Files.exists(p.resolve("NOTICE.txt")) && Files.exists(p.resolve("pom.xml"))) {
        return p;
      }
      p = p.getParent();
    }
    throw new IllegalStateException(
        "Could not locate repo root (containing NOTICE.txt and pom.xml) from "
            + Paths.get(".").toAbsolutePath());
  }
}
