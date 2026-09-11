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
package com.percussion.services.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.mail2.core.EmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * GH-4456: perc-system must keep Commons Email 2 on the compile classpath so the Rhythmyx WAR
 * WEB-INF/lib (and skip-image-build hot-deploy) ship {@code commons-email2-core} / {@code
 * commons-email2-jakarta}. Marking them {@code provided} reproduces {@code Failed startup of
 * context} / {@code NoClassDefFoundError: EmailException} on {@code sys_emailQueueListener}.
 */
class CommonsEmail2WarLibContractTest {

  private static final Set<String> REQUIRED =
      Set.of("commons-email2-core", "commons-email2-jakarta", "jakarta.mail");

  @Test
  @DisplayName("EmailException is loadable from commons-email2-core")
  void emailExceptionIsOnClasspath() {
    assertTrue(EmailException.class.getName().startsWith("org.apache.commons.mail2.core"));
  }

  @Test
  @DisplayName("system pom declares compile-scoped commons-email2-core and jakarta")
  void systemPomDeclaresMail2CompileDeps() throws Exception {
    Path pom = locateSystemPom();
    assertTrue(Files.isRegularFile(pom), "system pom must exist at " + pom);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    Document doc = factory.newDocumentBuilder().parse(pom.toFile());
    doc.getDocumentElement().normalize();

    Set<String> found = new HashSet<>();
    NodeList deps = doc.getElementsByTagName("dependency");
    for (int i = 0; i < deps.getLength(); i++) {
      Element dep = (Element) deps.item(i);
      String artifactId = textOf(dep, "artifactId");
      if (!REQUIRED.contains(artifactId)) {
        continue;
      }
      String groupId = textOf(dep, "groupId");
      String scope = textOf(dep, "scope");
      if ("jakarta.mail".equals(artifactId)) {
        assertTrue(
            "com.sun.mail".equals(groupId),
            artifactId + " groupId should be com.sun.mail, was: " + groupId);
      } else {
        assertTrue(
            groupId.isEmpty() || "org.apache.commons".equals(groupId),
            artifactId + " groupId should be org.apache.commons, was: " + groupId);
      }
      assertFalse(
          "provided".equalsIgnoreCase(scope) || "test".equalsIgnoreCase(scope),
          artifactId + " must be compile/runtime so the WAR packages it; scope=" + scope);
      found.add(artifactId);
    }
    assertTrue(found.containsAll(REQUIRED), "system/pom.xml must declare " + REQUIRED + "; found " + found);
  }

  private static Path locateSystemPom() throws IOException {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path[] candidates =
        new Path[] {
          cwd.resolve("pom.xml"),
          cwd.resolve("system").resolve("pom.xml"),
        };
    for (Path p : candidates) {
      if (Files.isRegularFile(p)) {
        String text = Files.readString(p);
        if (text.contains("<artifactId>perc-system</artifactId>")
            && text.contains("<artifactId>commons-email2-core</artifactId>")) {
          return p;
        }
      }
    }
    throw new IOException("Could not locate system/pom.xml from " + cwd);
  }

  private static String textOf(Element parent, String tag) {
    NodeList list = parent.getElementsByTagName(tag);
    if (list.getLength() == 0) {
      return "";
    }
    return list.item(0).getTextContent().trim();
  }
}
