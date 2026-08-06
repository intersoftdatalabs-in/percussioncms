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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * EE11 classloader contract: {@code servlet-utils} must be packaged into the Rhythmyx WAR
 * (WEB-INF/lib), not only as a {@code provided} dep of perc-system or on the Jetty server
 * classloader.
 *
 * <p>Without a direct non-provided WebUI dependency, {@code PSContextLoaderListener} fails at
 * startup with {@code ClassNotFoundException: PSServletUtils}.
 */
class WebUiServletUtilsPackagingTest {

  @Test
  @DisplayName("WebUI pom declares non-provided servlet-utils for WAR packaging")
  void webUiDeclaresServletUtilsNotProvided() throws Exception {
    Path pom = resolveWebUiPom();
    assertTrue(Files.isRegularFile(pom), "WebUI pom must exist at " + pom);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // Harden against XXE even though the POM is a trusted repo file.
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    Document doc = factory.newDocumentBuilder().parse(pom.toFile());
    doc.getDocumentElement().normalize();

    NodeList deps = doc.getElementsByTagName("dependency");
    boolean found = false;
    for (int i = 0; i < deps.getLength(); i++) {
      Element dep = (Element) deps.item(i);
      if (!"servlet-utils".equals(textOf(dep, "artifactId"))) {
        continue;
      }
      found = true;
      String groupId = textOf(dep, "groupId");
      String scope = textOf(dep, "scope");
      assertTrue(
          groupId.isEmpty() || "com.percussion".equals(groupId),
          "servlet-utils groupId should be com.percussion, was: " + groupId);
      assertFalse(
          "provided".equalsIgnoreCase(scope) || "test".equalsIgnoreCase(scope),
          "servlet-utils must be compile/runtime so the WAR packages it; scope=" + scope);
    }
    assertTrue(found, "WebUI/pom.xml must declare com.percussion:servlet-utils");
  }

  @Test
  @DisplayName("classpath resource / distribution WAR layout documents WEB-INF expectation")
  void installXmlStillDeploysRhythmyxWebapp() throws Exception {
    // Sanity: upgrade still deploys Rhythmyx/** so WEB-INF/lib jars from the WAR land on disk
    try (InputStream in =
        getClass().getResourceAsStream("/distribution/rxconfig/Installer/install.xml")) {
      assertNotNull(in, "install.xml must be on test classpath");
      String xml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      assertTrue(
          xml.contains("jetty/base/webapps/Rhythmyx/**"),
          "upgrade.overwrite must still deploy Rhythmyx webapp tree");
    }
  }

  @Test
  @DisplayName(
      "WebUI WAR packaging must not exclude all slf4j jars (Artemis needs MessageFormatter)")
  void webUiDoesNotExcludeAllSlf4jFromWar() throws Exception {
    Path pom = resolveWebUiPom();
    String text = Files.readString(pom);
    // Historical exclude WEB-INF/lib/*slf4j*.jar left Artemis without MessageFormatter
    // on the EE11 webapp classloader (server slf4j is not visible to the WAR).
    assertFalse(
        text.contains("WEB-INF/lib/*slf4j*.jar") || text.contains("WEB-INF/lib/**/*slf4j*.jar"),
        "packagingExcludes must not blanket-exclude *slf4j*.jar; ship slf4j-api in WEB-INF/lib");
  }

  private static Path resolveWebUiPom() throws IOException {
    Path cwd = Paths.get("").toAbsolutePath().normalize();
    Path[] candidates =
        new Path[] {
          cwd.resolve("WebUI").resolve("pom.xml"),
          cwd.resolve("..").resolve("..").resolve("WebUI").resolve("pom.xml"),
          cwd.getParent() != null
              ? cwd.getParent().getParent() != null
                  ? cwd.getParent().getParent().resolve("WebUI").resolve("pom.xml")
                  : null
              : null,
        };
    for (Path p : candidates) {
      if (p != null && Files.isRegularFile(p.normalize())) {
        String text = Files.readString(p.normalize());
        if (text.contains("<artifactId>perc-web-ui</artifactId>")) {
          return p.normalize();
        }
      }
    }
    // Fallback: basedir property when surefire sets user.dir to module root
    Path moduleRelative = cwd.resolve("../../WebUI/pom.xml").normalize();
    if (Files.isRegularFile(moduleRelative)) {
      return moduleRelative;
    }
    throw new IOException("Could not locate WebUI/pom.xml from " + cwd);
  }

  private static String textOf(Element parent, String tag) {
    NodeList list = parent.getElementsByTagName(tag);
    if (list.getLength() == 0) {
      return "";
    }
    return list.item(0).getTextContent().trim();
  }
}
