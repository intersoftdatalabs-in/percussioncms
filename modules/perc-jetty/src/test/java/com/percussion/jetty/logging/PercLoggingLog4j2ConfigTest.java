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

package com.percussion.jetty.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * GH-939: Jetty perc-logging must rotate at 10 MB and retain only the latest 10 rolled files
 * (deleting older dated archives).
 *
 * <p>{@code DefaultRolloverStrategy max="10"} alone does not delete dated files when {@code
 * filePattern} contains {@code %d{yyyy-MM-dd}}; a {@code Delete}/ {@code IfAccumulatedFileCount}
 * policy is required (same approach as DTS Tomcat {@code log4j2-tomcat.xml}).
 */
class PercLoggingLog4j2ConfigTest {

  private static final Path RELATIVE_FROM_MODULE =
      Path.of(
          "src", "main", "jetty", "defaults", "modules", "perc-logging", "resources", "log4j2.xml");

  private static final Path RELATIVE_FROM_REPO_ROOT =
      Path.of("modules", "perc-jetty").resolve(RELATIVE_FROM_MODULE);

  /** Expected rolled-file glob prefix per RollingFile appender name. */
  private static final Map<String, String> APPENDER_GLOBS =
      Map.of(
          "FILE", "server-*.log",
          "RXGLOBALTEMPLATES", "globaltemplate-*.log",
          "VELOCITY", "velocity-*.log",
          "RevisionPurgeApp", "revisionPurge-*.log");

  private static Document configDoc;
  private static Path log4j2Config;

  /**
   * Resolve log4j2.xml whether surefire CWD is the module dir or the repo root (multi-module
   * reactor / IDE).
   */
  private static Path resolveLog4j2Config() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path[] candidates =
        new Path[] {
          cwd.resolve(RELATIVE_FROM_MODULE),
          cwd.resolve(RELATIVE_FROM_REPO_ROOT),
          // Walk up a few levels for nested IDE run configs
          cwd.getParent() != null ? cwd.getParent().resolve(RELATIVE_FROM_REPO_ROOT) : null,
        };
    for (Path c : candidates) {
      if (c != null && Files.isRegularFile(c)) {
        return c.normalize();
      }
    }
    return cwd.resolve(RELATIVE_FROM_MODULE);
  }

  @BeforeAll
  static void loadConfig() throws Exception {
    log4j2Config = resolveLog4j2Config();
    assertTrue(
        Files.isRegularFile(log4j2Config),
        () ->
            "Missing perc-logging log4j2.xml (tried module- and repo-relative paths from "
                + Path.of("").toAbsolutePath()
                + ")");
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setExpandEntityReferences(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    try (InputStream in = Files.newInputStream(log4j2Config)) {
      configDoc = factory.newDocumentBuilder().parse(in);
    }
  }

  @Test
  void everyRollingFile_hasSizePolicyTenMegabytes() {
    for (Element rolling : rollingFiles()) {
      String name = rolling.getAttribute("name");
      Element sizePolicy = firstChild(rolling, "SizeBasedTriggeringPolicy");
      assertTrue(sizePolicy != null, name + " missing SizeBasedTriggeringPolicy");
      String size = sizePolicy.getAttribute("size").replace(" ", "");
      assertTrue(
          size.equalsIgnoreCase("10MB"),
          name + " size should be 10 MB, was: " + sizePolicy.getAttribute("size"));
    }
  }

  @Test
  void everyRollingFile_hasMaxTenAndDeleteRetention() {
    assertEquals(APPENDER_GLOBS.size(), rollingFiles().size(), "unexpected RollingFile count");

    for (Element rolling : rollingFiles()) {
      String name = rolling.getAttribute("name");
      assertTrue(APPENDER_GLOBS.containsKey(name), "unexpected RollingFile: " + name);

      Element strategy = firstChild(rolling, "DefaultRolloverStrategy");
      assertTrue(strategy != null, name + " missing DefaultRolloverStrategy");
      assertEquals("10", strategy.getAttribute("max"), name + " max must be 10");

      Element delete = firstChild(strategy, "Delete");
      assertTrue(delete != null, name + " missing Delete retention (GH-939)");
      assertEquals("1", delete.getAttribute("maxDepth"), name + " Delete maxDepth");

      Element ifFileName = firstChild(delete, "IfFileName");
      assertTrue(ifFileName != null, name + " missing IfFileName");
      assertEquals(
          APPENDER_GLOBS.get(name),
          ifFileName.getAttribute("glob"),
          name + " Delete glob must match filePattern prefix");

      Element count = firstChild(delete, "IfAccumulatedFileCount");
      assertTrue(count != null, name + " missing IfAccumulatedFileCount");
      assertEquals("10", count.getAttribute("exceeds"), name + " must keep only 10 rolled files");
    }
  }

  @Test
  void filePatterns_useDatedIndexNames_withSizeOnlyTrigger() {
    // Documents the historical pitfall: date in filePattern + max alone does not
    // delete older days — Delete policy above is mandatory.
    for (Element rolling : rollingFiles()) {
      String pattern = rolling.getAttribute("filePattern");
      assertTrue(pattern.contains("%d{yyyy-MM-dd}"), rolling.getAttribute("name") + " filePattern");
      assertTrue(pattern.contains("%i"), rolling.getAttribute("name") + " filePattern needs %i");
      // No TimeBasedTriggeringPolicy — size-only rollover per GH-939
      assertTrue(
          firstChild(rolling, "TimeBasedTriggeringPolicy") == null,
          rolling.getAttribute("name") + " should not use time-based trigger for this policy");
    }
  }

  @Test
  void logdirProperty_isDefinedForPortablePathJoin() {
    NodeList props = configDoc.getElementsByTagName("Property");
    boolean found = false;
    for (int i = 0; i < props.getLength(); i++) {
      Element p = (Element) props.item(i);
      if ("logdir".equals(p.getAttribute("name"))) {
        found = true;
        String value = p.getAttribute("value");
        if (value == null || value.isEmpty()) {
          value = p.getTextContent();
        }
        assertTrue(
            value != null && value.contains("logs"),
            "logdir property should point at logs directory");
      }
    }
    assertTrue(found, "logdir Property must be defined for RollingFile paths");
  }

  /**
   * Issue #1824 (slice 1): Betwixt logger overrides are obsolete after Jackson XML migration noise
   * cleanup. Do not reintroduce {@code org.apache.commons.betwixt.*} AsyncLogger entries.
   */
  @Test
  void noBetwixtLoggerOverrides() {
    for (String tag : new String[] {"AsyncLogger", "Logger"}) {
      NodeList loggers = configDoc.getElementsByTagName(tag);
      for (int i = 0; i < loggers.getLength(); i++) {
        Element logger = (Element) loggers.item(i);
        String name = logger.getAttribute("name");
        assertTrue(
            name == null || !name.contains("org.apache.commons.betwixt"),
            () -> tag + " must not silence Betwixt (removed in #1824): " + name);
      }
    }
  }

  private static List<Element> rollingFiles() {
    NodeList nodes = configDoc.getElementsByTagName("RollingFile");
    List<Element> list = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      list.add((Element) nodes.item(i));
    }
    return list;
  }

  private static Element firstChild(Element parent, String tag) {
    NodeList children = parent.getElementsByTagName(tag);
    if (children.getLength() == 0) {
      return null;
    }
    for (int i = 0; i < children.getLength(); i++) {
      Element el = (Element) children.item(i);
      if (isUnder(el, parent)) {
        return el;
      }
    }
    return (Element) children.item(0);
  }

  private static boolean isUnder(Element child, Element ancestor) {
    org.w3c.dom.Node n = child.getParentNode();
    while (n != null) {
      if (n == ancestor) {
        return true;
      }
      n = n.getParentNode();
    }
    return false;
  }
}
