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

package com.percussion.jetty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * GH-1484 / GH-1485 / GH-1486 / GH-1487: Jetty startup WARN hygiene — config-level
 * guards so SLF4J dual-provider, module forking, deprecated APIs, and DigesterFactory
 * noise do not regress in the shipped module/ini/XML overlays.
 *
 * <p>Resolves files whether surefire CWD is the module directory or the monorepo root.
 */
class StartupWarnHygieneTest {

  private static final Path MODULE_JETTY = Path.of("src", "main", "jetty");
  private static final Path REPO_JETTY = Path.of("modules", "perc-jetty").resolve(MODULE_JETTY);

  private static Path jettyRoot;

  @BeforeAll
  static void resolveJettyRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path[] candidates =
        new Path[] {
          cwd.resolve(MODULE_JETTY),
          cwd.resolve(REPO_JETTY),
          cwd.getParent() != null ? cwd.getParent().resolve(REPO_JETTY) : null,
        };
    for (Path c : candidates) {
      if (c != null && Files.isDirectory(c)) {
        jettyRoot = c.normalize();
        return;
      }
    }
    fail("Could not resolve modules/perc-jetty/src/main/jetty from CWD " + cwd);
  }

  private static Path file(String first, String... more) {
    Path p = jettyRoot.resolve(first);
    for (String m : more) {
      p = p.resolve(m);
    }
    return p;
  }

  private static String read(Path path) throws IOException {
    assertTrue(Files.isRegularFile(path), "missing file: " + path);
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  /** Strip `#` line comments and XML `<!-- ... -->` comments for structural assertions. */
  private static String stripComments(String text) {
    String noXmlComments = text.replaceAll("(?s)<!--.*?-->", "");
    return Stream.of(noXmlComments.replace("\r\n", "\n").split("\n"))
        .map(String::trim)
        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
        .reduce((a, b) -> a + "\n" + b)
        .orElse("");
  }

  /** GH-1484: perc-logging must own Jetty capability logging|default (exclude logging-jetty). */
  @Test
  void percLoggingProvidesLoggingDefaultCapability() throws Exception {
    String mod = stripComments(read(file("defaults", "modules", "perc-logging.mod")));
    assertTrue(
        mod.contains("logging|default"),
        "perc-logging.mod must [provides] logging|default so jetty-slf4j-impl is not also selected");
    assertFalse(
        mod.contains("[exec]"),
        "perc-logging.mod must not declare [exec] (GH-1485 fork hygiene)");
  }

  /** GH-1485: perc.mod must not force its own JVM fork via [exec]. */
  @Test
  void percModHasNoExecSection() throws Exception {
    String mod = stripComments(read(file("defaults", "modules", "perc.mod")));
    assertFalse(mod.contains("[exec]"), "perc.mod must not declare [exec] (consolidate in jvm.ini)");
    assertTrue(
        mod.contains("shutdown"),
        "perc.mod must depend on Jetty shutdown module (GH-1486 ShutdownService)");
  }

  /** GH-1485: consolidated JVM args live only under jvm.ini --exec. */
  @Test
  void jvmIniOwnsForkedJvmArgs() throws Exception {
    String jvm = read(file("defaults", "start.d", "jvm.ini"));
    assertTrue(jvm.contains("--exec"), "jvm.ini should retain --exec for consolidated CMS JVM args");
    assertTrue(
        jvm.contains("PSSaxParserFactoryImpl"),
        "jvm.ini must set Percussion SAXParserFactory (moved from perc.mod [exec])");
    assertTrue(
        jvm.contains("java.library.path"),
        "jvm.ini must set java.library.path (moved from perc.mod [exec])");
    assertTrue(
        jvm.contains("XmlParser.Validating=false"),
        "jvm.ini must keep intentional non-validating Jetty XML parse (GH-1487)");
  }

  /** GH-1486: ShutdownService configuration (not STOP.PORT ShutdownMonitor on server). */
  @Test
  void shutdownIniConfiguresShutdownService() throws Exception {
    String ini = read(file("defaults", "start.d", "shutdown.ini"));
    assertTrue(ini.contains("--module=shutdown"), "shutdown.ini must enable shutdown module");
    assertTrue(ini.contains("jetty.shutdown.port=50011"), "shutdown port must match StopJetty default");
    assertTrue(ini.contains("jetty.shutdown.key=SHUTDOWN"), "shutdown key must match StopJetty default");
  }

  /** GH-1486: StartJetty must not activate deprecated ShutdownMonitor via STOP.PORT. */
  @Test
  void startJettyBatDoesNotSetStopPortSystemProperties() throws Exception {
    String bat = read(file("StartJetty.bat"));
    // The java line must not include -DSTOP.PORT / -DSTOP.KEY (server-side monitor).
    List<String> javaLines =
        Stream.of(bat.replace("\r\n", "\n").split("\n"))
            .filter(l -> l.contains("start.jar"))
            .toList();
    assertFalse(javaLines.isEmpty(), "StartJetty.bat must invoke start.jar");
    for (String line : javaLines) {
      assertFalse(
          line.contains("-DSTOP.PORT"),
          "StartJetty.bat start.jar line must not set -DSTOP.PORT (use shutdown.ini): " + line);
      assertFalse(
          line.contains("-DSTOP.KEY"),
          "StartJetty.bat start.jar line must not set -DSTOP.KEY (use shutdown.ini): " + line);
    }
  }

  /** GH-1486: StopJetty client still uses STOP.PORT/KEY to talk to ShutdownService. */
  @Test
  void stopJettyBatStillUsesStopClientProperties() throws Exception {
    String bat = read(file("StopJetty.bat"));
    assertTrue(bat.contains("-DSTOP.PORT"), "StopJetty.bat must pass STOP.PORT for --stop client");
    assertTrue(bat.contains("-DSTOP.KEY"), "StopJetty.bat must pass STOP.KEY for --stop client");
    assertTrue(bat.contains("--stop"), "StopJetty.bat must invoke start.jar --stop");
  }

  /** GH-1486: Rhythmyx.xml must not call deprecated CookieConfig.setComment. */
  @Test
  void rhythmyxXmlUsesSameSiteAttributeNotComment() throws Exception {
    String xml = stripComments(read(file("base", "webapps", "Rhythmyx.xml")));
    assertFalse(
        xml.toLowerCase().contains("setcomment") || xml.contains("name=\"comment\""),
        "Rhythmyx.xml must not use deprecated CookieConfig comment/SameSite comment convention");
    assertTrue(
        xml.contains("setAttribute") && xml.contains("SameSite"),
        "Rhythmyx.xml must set SameSite via CookieConfig.setAttribute");
    assertTrue(xml.contains("Strict"), "default SameSite should remain Strict");
  }

  /** GH-1487: DigesterFactory schema sources must exist for assembly packaging. */
  @Test
  void digesterFactorySchemaSourcesPresent() throws Exception {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path[] roots =
        new Path[] {
          cwd.resolve(Path.of("src", "build", "perc-xml-schemas")),
          cwd.resolve(Path.of("modules", "perc-jetty", "src", "build", "perc-xml-schemas")),
        };
    Path schemaRoot = null;
    for (Path r : roots) {
      if (Files.isDirectory(r)) {
        schemaRoot = r;
        break;
      }
    }
    if (schemaRoot == null) {
      fail("perc-xml-schemas build resources not found from CWD " + cwd);
    }
    Path desc = schemaRoot.resolve(Path.of("org", "apache", "tomcat", "util", "descriptor"));
    for (String name : List.of("XMLSchema.dtd", "datatypes.dtd", "xml.xsd")) {
      Path f = desc.resolve(name);
      assertTrue(Files.isRegularFile(f), "missing DigesterFactory schema resource: " + f);
      assertTrue(Files.size(f) > 0, "empty schema resource: " + f);
    }
  }

  /** Sanity: perc-logging.ini still enables the module. */
  @Test
  void percLoggingIniEnablesModule() throws Exception {
    String ini = read(file("defaults", "start.d", "perc-logging.ini"));
    assertTrue(
        Stream.of(ini.replace("\r\n", "\n").split("\n"))
            .map(String::trim)
            .filter(l -> !l.isEmpty() && !l.startsWith("#"))
            .anyMatch(l -> l.contains("--module=perc-logging")),
        "perc-logging.ini must enable --module=perc-logging");
  }
}
