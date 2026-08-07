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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

/**
 * Issue #2290: installer Ant XML must be well-formed. XML forbids the sequence {@code --} inside
 * comments; a CLI flag mention such as {@code --demo-sites} inside {@code <!-- ... -->} causes
 * Apache Ant to abort with {@code The string "--" is not permitted within comments} mid-install.
 *
 * <p>Cheap regression guard: parse each top-level Installer Ant script and scan comment bodies for
 * consecutive hyphens. Nested {@code data/} table XML is covered by existing packaging tests.
 */
@Tag("UnitTest")
class InstallerXmlWellFormedTest {

  private static final Path INSTALLER_DIR =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer");

  /** Match XML comments (non-greedy, DOTALL). */
  private static final Pattern XML_COMMENT = Pattern.compile("<!--(.*?)-->", Pattern.DOTALL);

  @Test
  @DisplayName("Installer Ant XML comments must not embed consecutive hyphens (#2290)")
  void installerAntXmlCommentsHaveNoDoubleHyphen() throws IOException {
    List<Path> scripts = listTopLevelInstallerXml();
    assertFalse(scripts.isEmpty(), "expected Installer Ant XML under " + INSTALLER_DIR);

    List<String> violations = new ArrayList<>();
    for (Path script : scripts) {
      String text = Files.readString(script, StandardCharsets.UTF_8);
      Matcher m = XML_COMMENT.matcher(text);
      while (m.find()) {
        String inner = m.group(1);
        if (inner.contains("--")) {
          int line = 1 + (int) text.substring(0, m.start()).chars().filter(c -> c == '\n').count();
          violations.add(script.getFileName() + ":" + line + " comment contains \"--\"");
        }
      }
    }

    assertTrue(
        violations.isEmpty(),
        () ->
            "XML comments must not contain \"--\" (XML 1.0 §2.5). Reword CLI flag mentions"
                + " (e.g. demo-sites flag instead of double-dash demo-sites). Offenders:\n"
                + String.join("\n", violations));
  }

  @Test
  @DisplayName("Installer Ant XML files parse as well-formed XML (#2290)")
  void installerAntXmlIsWellFormed()
      throws IOException, ParserConfigurationException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setValidating(false);
    // Do not expand external entities; these are local packaging scripts.
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    DocumentBuilder builder = factory.newDocumentBuilder();

    List<Path> scripts = listTopLevelInstallerXml();
    assertFalse(scripts.isEmpty(), "expected Installer Ant XML under " + INSTALLER_DIR);

    for (Path script : scripts) {
      try (var in = Files.newInputStream(script)) {
        builder.parse(in);
      } catch (SAXException e) {
        fail("Installer Ant XML is not well-formed: " + script + " — " + e.getMessage(), e);
      }
    }
  }

  /** Top-level Ant scripts only (not data/*.xml table dumps). */
  private static List<Path> listTopLevelInstallerXml() throws IOException {
    assertTrue(
        Files.isDirectory(INSTALLER_DIR),
        () -> "missing Installer dir: " + INSTALLER_DIR.toAbsolutePath().normalize());
    try (Stream<Path> stream = Files.list(INSTALLER_DIR)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(".xml"))
          .sorted()
          .toList();
    }
  }
}
