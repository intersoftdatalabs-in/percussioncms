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
import org.junit.jupiter.api.Test;

/**
 * Verifies the third-party component copyright message emitted at server startup and rendered in
 * the UI About dialog. The message is sourced from the {@code thirdPartyCopyright} entry in the
 * server {@link java.util.ResourceBundle}. The stable attribution blurb is intentionally
 * version-agnostic: the hand-edited bundle key and {@code NOTICE.txt} are the single source of
 * truth for the prose, and the versioned inventory is generated from the project dependency set at
 * build time. See issues #1529 and #1552.
 */
public class PSThirdPartyCopyrightTest {

  @Test
  void bundleExposesNonEmptyThirdPartyCopyright() {
    String text = PSServer.getRes().getString("thirdPartyCopyright");
    assertNotNull(text, "thirdPartyCopyright bundle key must be defined");
    assertFalse(text.isBlank(), "thirdPartyCopyright must not be blank");
  }

  @Test
  void thirdPartyCopyrightCarriesStableAttribution() {
    String text = PSServer.getRes().getString("thirdPartyCopyright");
    assertTrue(
        text.contains("Apache Software Foundation"),
        "stable attribution blurb should credit the Apache Software Foundation");
    assertTrue(
        text.contains("Apache License, Version 2.0"),
        "stable attribution blurb should reference the Apache 2.0 license");
    assertTrue(
        text.contains("Intersoft Data Labs"),
        "stable attribution blurb should credit Intersoft Data Labs for ongoing maintenance");
    assertTrue(
        text.contains("Bundled Apache and Apache-licensed components"),
        "stable attribution blurb should describe the Apache-licensed component category");
    assertTrue(
        text.contains("Bundled JDBC drivers and relational persistence"),
        "stable attribution blurb should give JDBC drivers their own section header");
    assertTrue(
        text.contains("Bundled JavaScript libraries"),
        "stable attribution blurb should describe the bundled JavaScript libraries");
    assertTrue(
        text.contains("generated from the project dependency set at build time"),
        "stable attribution blurb should state that the versioned inventory is build-generated");
    assertTrue(
        text.contains("single source of truth"),
        "stable attribution blurb should designate the LICENSE/NOTICE files as the single source of truth");
  }

  @Test
  void thirdPartyCopyrightHasNoDependencyVersionPins() {
    String text = PSServer.getRes().getString("thirdPartyCopyright");
    assertFalse(
        text.contains("v1.3.1"), "jTDS v1.3.1 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("v2.3.232"),
        "H2 v2.3.232 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("13.3.1.jre11-preview"),
        "Microsoft SQL Server JDBC preview pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("v42.7.12"),
        "PostgreSQL JDBC v42.7.12 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("v3.5.7"),
        "MariaDB Connector/J v3.5.7 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("v7.2.6"), "Hibernate ORM v7.2.6 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("2.25.4"), "Apache Log4j 2.25.4 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("8.11.4"), "Apache Lucene 8.11.4 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("v1.4.21"), "XStream v1.4.21 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("v1.84"), "Bouncy Castle v1.84 pin must not appear in the stable attribution blurb");
    assertFalse(
        text.contains("6.8.6"), "TinyMCE 6.8.6 pin must not appear in the stable attribution blurb");
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
  void noticeFileMirrorsStableThirdPartyCopyright() throws Exception {
    Path repoRoot = repoRoot();
    Path notice = repoRoot.resolve("NOTICE.txt");
    assertTrue(Files.exists(notice), "NOTICE.txt missing from repo root");
    String body = new String(Files.readAllBytes(notice), StandardCharsets.UTF_8);

    assertTrue(
        body.contains("Intersoft Data Labs"), "NOTICE.txt must credit Intersoft Data Labs");
    assertTrue(
        body.contains("Bundled JDBC drivers and relational persistence"),
        "NOTICE.txt must have a dedicated JDBC drivers section");
    assertTrue(
        body.contains("generated from the project dependency set at build time"),
        "NOTICE.txt must state that the versioned inventory is build-generated");
    assertTrue(
        body.contains("single source of truth"),
        "NOTICE.txt must designate the LICENSE/NOTICE files as the single source of truth");
    assertFalse(
        body.contains("Lato"), "NOTICE.txt must no longer reference the dropped Lato font");
    assertFalse(
        body.contains("v1.3.1"), "NOTICE.txt must not duplicate the jTDS v1.3.1 pin");
    assertFalse(
        body.contains("v2.3.232"), "NOTICE.txt must not duplicate the H2 v2.3.232 pin");
    assertFalse(
        body.contains("2.25.4"), "NOTICE.txt must not duplicate the Apache Log4j 2.25.4 pin");
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
