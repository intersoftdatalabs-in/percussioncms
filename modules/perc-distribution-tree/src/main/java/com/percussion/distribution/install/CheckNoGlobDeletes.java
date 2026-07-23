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
package com.percussion.distribution.install;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Static assertion that the install/upgrade ANT script's {@code <delete>} block inside {@code
 * <target name="install_jdbc_drivers">} does not use glob patterns.
 *
 * <p>Java port of {@code modules/perc-distribution-tree/scripts/check-no-glob-deletes.sh}, bound to
 * the Maven {@code verify} phase via {@code exec-maven-plugin:java} so the build gate runs
 * identically on Windows, Linux, and macOS. See root {@code AGENTS.md} cross-platform rules.
 *
 * <p>Companion to {@code InstallXmlDeleteSetTest} (which asserts the same invariant via JUnit).
 * This class is the build-time gate; the JUnit test runs in {@code test} phase, this runs in
 * {@code verify} phase, so the build still catches the regression when run with {@code mvn -DskipTests
 * install}.
 *
 * <p>Like {@link VerifyJdbcDrivers}, {@code main} must not call {@link System#exit} under
 * {@code exec-maven-plugin:java} (in-process Maven JVM). See {@link BuildGateMains}.
 */
public final class CheckNoGlobDeletes {

  static final int EXIT_OK = 0;
  static final int EXIT_INVOCATION = 1;
  static final int EXIT_GLOB_FOUND = 7;

  private static final String INSTALL_XML_CLASSPATH =
      "/distribution/rxconfig/Installer/install.xml";

  private CheckNoGlobDeletes() {}

  /** Entry point for the build gate; returns via {@link BuildGateMains#complete(int, String)}. */
  public static void main(String[] args) {
    BuildGateMains.complete(run(args), "CheckNoGlobDeletes");
  }

  /**
   * Gate logic returning a process-style exit code (for tests and {@link #main}). Does not call
   * {@link System#exit}.
   */
  static int run(String[] args) {
    Path installXml = computeDefaultInstallXmlPath();
    for (int i = 0; i < args.length; i++) {
      String flag = args[i];
      if ("--install-xml".equals(flag)) {
        if (++i >= args.length) {
          System.err.println("ERROR: --install-xml requires a value");
          return EXIT_INVOCATION;
        }
        installXml = Paths.get(args[i]);
      } else {
        System.err.println("ERROR: unknown argument: " + flag);
        System.err.println("Usage: CheckNoGlobDeletes [--install-xml <path>]");
        return EXIT_INVOCATION;
      }
    }
    if (!Files.isRegularFile(installXml)) {
      System.err.println("ERROR: install.xml not found: " + installXml);
      return EXIT_INVOCATION;
    }

    List<String> globs;
    try {
      globs = collectGlobsInDeleteBlock(installXml);
    } catch (IOException | ParserConfigurationException | org.xml.sax.SAXException e) {
      System.err.println("ERROR: failed to parse install.xml: " + e.getMessage());
      return EXIT_INVOCATION;
    }
    if (!globs.isEmpty()) {
      System.err.println(
          "ERROR: glob-based <delete> patterns found in install_jdbc_drivers target of install.xml:");
      for (String g : globs) {
        System.err.println("  " + g);
      }
      System.err.println(
          "Fix: replace each glob with the exact bundled-driver filename (see BundledJdbcDrivers"
              + " constant in the test sources).");
      return EXIT_GLOB_FOUND;
    }
    System.out.println(
        "OK: install_jdbc_drivers <delete> uses exact filenames only; no glob patterns found");
    return EXIT_OK;
  }

  /**
   * Resolves the default {@code install.xml} when {@code --install-xml} is not passed.
   *
   * <p>Tries several locations so the gate works both when the process CWD is the module root
   * (standalone {@code mvn} from {@code modules/perc-distribution-tree}) and when Maven was
   * launched from the monorepo root (multi-module reactor). The Maven {@code verify} execution
   * still passes an explicit {@code --install-xml ${project.basedir}/...} path; this method is
   * the fallback for CLI / tests without that argument.
   */
  static Path computeDefaultInstallXmlPath() {
    Path relative =
        Paths.get("src", "main", "resources", "distribution", "rxconfig", "Installer", "install.xml");
    List<Path> candidates = new ArrayList<>();
    candidates.add(relative);

    // Maven / surefire often expose the module directory as -Dbasedir=...
    String basedir = System.getProperty("basedir");
    if (basedir != null && !basedir.isBlank()) {
      candidates.add(
          Paths.get(
              basedir,
              "src",
              "main",
              "resources",
              "distribution",
              "rxconfig",
              "Installer",
              "install.xml"));
    }

    // Reactor root when Maven sets maven.multiModuleProjectDirectory
    String multi = System.getProperty("maven.multiModuleProjectDirectory");
    if (multi != null && !multi.isBlank()) {
      candidates.add(
          Paths.get(
              multi,
              "modules",
              "perc-distribution-tree",
              "src",
              "main",
              "resources",
              "distribution",
              "rxconfig",
              "Installer",
              "install.xml"));
    }

    // Walk up from user.dir looking for the module layout (operator CLI from a subdir / monorepo root)
    Path cwd = Paths.get("").toAbsolutePath().normalize();
    Path dir = cwd;
    for (int depth = 0; dir != null && depth < 8; depth++, dir = dir.getParent()) {
      candidates.add(
          dir.resolve(
              Paths.get(
                  "src",
                  "main",
                  "resources",
                  "distribution",
                  "rxconfig",
                  "Installer",
                  "install.xml")));
      candidates.add(
          dir.resolve(
              Paths.get(
                  "modules",
                  "perc-distribution-tree",
                  "src",
                  "main",
                  "resources",
                  "distribution",
                  "rxconfig",
                  "Installer",
                  "install.xml")));
    }

    for (Path candidate : candidates) {
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
    }
    // Prefer the relative path in the error message when nothing matches
    return relative;
  }

  /**
   * Returns every {@code <include name="...">} value inside the {@code <delete>} of {@code
   * <target name="install_jdbc_drivers">} that contains a glob wildcard ({@code *} or {@code ?}).
   *
   * <p>Exposed at package-private visibility for unit testing. Parses with a hardened XML factory
   * (no external entities) so an attacker-controlled {@code install.xml} cannot inject entities or
   * DTDs.
   */
  static List<String> collectGlobsInDeleteBlock(Path installXml)
      throws IOException, ParserConfigurationException, org.xml.sax.SAXException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      dbf.setXIncludeAware(false);
      dbf.setExpandEntityReferences(false);
    } catch (Exception ignored) {
      // features are advisory on some parsers; fall through to strict defaults
    }

    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc;
    try (var in = Files.newInputStream(installXml)) {
      doc = db.parse(new InputSource(in));
    }

    Element jdbcTarget = null;
    NodeList targets = doc.getElementsByTagName("target");
    for (int i = 0; i < targets.getLength(); i++) {
      Element t = (Element) targets.item(i);
      if ("install_jdbc_drivers".equals(t.getAttribute("name"))) {
        jdbcTarget = t;
        break;
      }
    }
    if (jdbcTarget == null) {
      throw new IOException("<target name=\"install_jdbc_drivers\"> not found in " + installXml);
    }

    List<String> globs = new ArrayList<>();
    NodeList deletes = jdbcTarget.getElementsByTagName("delete");
    for (int i = 0; i < deletes.getLength(); i++) {
      Element deleteEl = (Element) deletes.item(i);
      NodeList includes = deleteEl.getElementsByTagName("include");
      for (int j = 0; j < includes.getLength(); j++) {
        Element inc = (Element) includes.item(j);
        String name = inc.getAttribute("name");
        if (name == null || name.isEmpty()) {
          continue;
        }
        if (name.indexOf('*') >= 0 || name.indexOf('?') >= 0) {
          globs.add(name);
        }
      }
    }
    return Collections.unmodifiableList(globs);
  }
}
