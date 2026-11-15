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

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Issue #6376: every DTS service WAR must bundle its own Log4j provider (api + core, compile
 * scope) so Log4j Core self-initializes from the embedded {@code WEB-INF/classes/log4j2.xml} and
 * creates the per-service {@code ${catalina.base}/logs/<service>.log} file.
 *
 * <p>Without this, webapp loggers have no provider (compile api without core) or bind to the
 * container (Tomcat) context only ({@code provided} api): the {@code log4jConfiguration}
 * context-param in {@code WEB-INF/web.xml} never loads — {@code log4j-web} 2.x registers only a
 * {@code javax.servlet} initializer, inert on the Tomcat 11 (Jakarta) runtime — and no
 * per-service log file is ever created, so service-level failures are invisible unless they
 * reach the console.
 *
 * <p>Reads sibling module sources via portable relative paths; runs during {@code mvn test} with
 * no container required.
 */
class DtsServiceWebappLoggingTest {

  /** Module dir under {@code delivery-tier-suite/} plus the per-service log file it must own. */
  private static final String[][] SERVICES = {
    {"comments", "comments.log"},
    {"feeds", "feeds.log"},
    {"forms", "forms.log"},
    {"membership", "membership.log"},
    {"metadata", "metadata.log"},
    {"polls", "polls.log"},
  };

  private static Path serviceFile(String module, String... rest) {
    Path p = Path.of("..", module);
    for (String r : rest) {
      p = p.resolve(r);
    }
    return p;
  }

  private static Document parsePom(Path pom) {
    try (InputStream in = Files.newInputStream(pom)) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      Document doc = factory.newDocumentBuilder().parse(in);
      doc.getDocumentElement().normalize();
      return doc;
    } catch (Exception e) {
      fail("cannot parse " + pom.toAbsolutePath() + ": " + e.getMessage());
      throw new IllegalStateException(e);
    }
  }

  private static String childText(Element parent, String tag) {
    NodeList nodes = parent.getElementsByTagName(tag);
    if (nodes.getLength() == 0) {
      return "";
    }
    return nodes.item(0).getTextContent().trim();
  }

  @Test
  void serviceWars_bundleLog4jProviderAtCompileScope() {
    List<String> problems = new ArrayList<>();
    for (String[] service : SERVICES) {
      checkServiceWarLogging(service[0], problems);
    }
    assertTrue(problems.isEmpty(), () -> "DTS webapp logging contract broken:\n"
        + String.join("\n", problems));
  }

  private static void checkServiceWarLogging(String module, List<String> problems) {
    Path pom = serviceFile(module, "pom.xml");
    if (!Files.isRegularFile(pom)) {
      problems.add(module + ": missing pom " + pom.toAbsolutePath());
      return;
    }
    Document doc = parsePom(pom);
    NodeList deps = doc.getElementsByTagName("dependency");
    boolean[] flags = new boolean[3];
    for (int i = 0; i < deps.getLength(); i++) {
      accumulateLog4jFlags((Element) deps.item(i), flags);
    }
    boolean apiCompile = flags[0];
    boolean coreCompile = flags[1];
    boolean webPresent = flags[2];
    if (!apiCompile) {
      problems.add(module + ": log4j-api must be compile scope (provided/test leaves "
          + "webapp loggers on the container context only)");
    }
    if (!coreCompile) {
      problems.add(module + ": log4j-core must be compile scope (api without core "
          + "means 'could not find a logging provider' and no per-service file)");
    }
    if (webPresent) {
      problems.add(module + ": log4j-web must stay out of Tomcat 11 WARs "
          + "(2.x is javax.servlet-only and never initializes on Jakarta)");
    }
  }

  /**
   * Folds one {@code <dependency>} element into {@code flags}:
   * {@code [apiCompile, coreCompile, webPresent]}.
   */
  private static void accumulateLog4jFlags(Element dep, boolean[] flags) {
    if (!"org.apache.logging.log4j".equals(childText(dep, "groupId"))) {
      return;
    }
    String artifact = childText(dep, "artifactId");
    String scope = childText(dep, "scope");
    boolean compileScope = scope.isEmpty() || "compile".equals(scope);
    if ("log4j-api".equals(artifact) && compileScope) {
      flags[0] = true;
    }
    if ("log4j-core".equals(artifact) && compileScope) {
      flags[1] = true;
    }
    if ("log4j-web".equals(artifact)) {
      flags[2] = true;
    }
  }

  @Test
  void serviceWars_embedPerServiceFileAppender() throws IOException {
    for (String[] service : SERVICES) {
      String module = service[0];
      String logFile = service[1];
      Path config = serviceFile(module, "src", "main", "resources", "log4j2.xml");
      assertTrue(Files.isRegularFile(config),
          () -> module + ": missing embedded log4j2.xml at " + config.toAbsolutePath());
      String text = Files.readString(config, StandardCharsets.UTF_8);
      assertTrue(text.contains("/logs/" + logFile),
          () -> module + ": embedded log4j2.xml must write /logs/" + logFile);
      assertTrue(text.contains("sys:catalina.base"),
          () -> module + ": embedded log4j2.xml must resolve the log dir from "
              + "sys:catalina.base so the cell path is stable");
    }
    assertEquals(6, SERVICES.length, "service matrix must cover all six DTS WARs");
  }
}
