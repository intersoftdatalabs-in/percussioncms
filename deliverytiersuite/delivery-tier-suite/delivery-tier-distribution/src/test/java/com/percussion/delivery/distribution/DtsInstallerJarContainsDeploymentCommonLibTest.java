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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for issue #1473.
 *
 * <p>The bundled Ant installer (installDts.xml:344-349) sources log4j, commons-logging, slf4j, and
 * disruptor jars from {@code ${install.src}/Deployment/Server/common/lib/}. Without the fix in this
 * commit, that directory is absent from the shipping jar and the install fails at line 344 with
 * {@code Deployment\Server\common\lib does not exist}.
 *
 * <p>This test asserts the shipping jar contains the expected runtime jars under {@code
 * distribution/Deployment/Server/common/lib/}. The test is a no-op when run outside Maven (no build
 * artifact on disk) so IDE / ad-hoc runs are unaffected.
 */
class DtsInstallerJarContainsDeploymentCommonLibTest {

  private static final String COMMON_LIB_PATH_PREFIX = "distribution/Deployment/Server/common/lib/";

  @Test
  void shippingJarBundlesRuntimeCommonLibJars() throws IOException {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    List<String> matches = new ArrayList<>();
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      zip.stream()
          .map(java.util.zip.ZipEntry::getName)
          .filter(name -> name.startsWith(COMMON_LIB_PATH_PREFIX))
          .forEach(matches::add);
    } catch (IOException io) {
      fail("Could not read " + jar + ": " + io.getMessage());
    }

    // Verify each required runtime jar is present. installDts.xml:345-349
    // selects log4j*, commons-logging*, slf4j*, disrupter*. Although the
    // install script uses the typo'd glob `disrupter*.jar` (should be
    // `disruptor*.jar`), the actual com.lmax:disruptor jar is bundled so
    // a corrected glob picks it up.
    String[] requiredPrefixes = {
      "log4j-api-", "log4j-core-", "commons-logging-", "slf4j-api-", "disruptor-"
    };

    StringBuilder missing = new StringBuilder();
    for (String prefix : requiredPrefixes) {
      boolean found =
          matches.stream()
              .anyMatch(
                  name -> {
                    String base = name.substring(COMMON_LIB_PATH_PREFIX.length());
                    return base.startsWith(prefix) && base.endsWith(".jar");
                  });
      if (!found) {
        if (missing.length() > 0) {
          missing.append(", ");
        }
        missing.append(prefix).append("*");
      }
    }
    if (missing.length() > 0) {
      fail(
          "Shipping jar is missing required runtime jar(s) under "
              + COMMON_LIB_PATH_PREFIX
              + ": "
              + missing
              + ". installDts.xml:344 reads from this directory and the install fails with"
              + " 'Deployment\\Server\\common\\lib does not exist' (issue #1473).");
    }

    // Sanity: the directory entry must exist (zip directories are usually
    // recorded alongside their file entries).
    assertTrue(
        matches.stream().anyMatch(name -> name.equals(COMMON_LIB_PATH_PREFIX))
            || matches.stream().anyMatch(name -> name.startsWith(COMMON_LIB_PATH_PREFIX)),
        () ->
            "No entries under "
                + COMMON_LIB_PATH_PREFIX
                + " in "
                + jar
                + "; expected the runtime jars staged by the"
                + " copy-deployment-server-common-lib maven-dependency-plugin:copy"
                + " execution.");
  }

  /**
   * #1500 DTS matrix: {@code server.xml} loads {@code
   * com.percussion.tomcat.valves.PSSimpleRedirectorValve}. A missing package declaration compiled
   * that class into the default package (jar root) and Tomcat failed with FATAL "Cannot start
   * server".
   */
  @Test
  void percTomcatCommonBundlesRedirectorValveInCorrectPackage() throws IOException {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    String tomcatCommonEntry = null;
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      tomcatCommonEntry =
          zip.stream()
              .map(ZipEntry::getName)
              .filter(n -> n.startsWith(COMMON_LIB_PATH_PREFIX))
              .filter(n -> n.contains("perc-tomcat-common") && n.endsWith(".jar"))
              .findFirst()
              .orElse(null);
    }
    if (tomcatCommonEntry == null) {
      fail("Shipping jar missing perc-tomcat-common under " + COMMON_LIB_PATH_PREFIX);
    }

    // Nested jar: extract entry bytes to a temp file and inspect package path.
    Path nested = Files.createTempFile("perc-tomcat-common-", ".jar");
    try {
      try (ZipFile zip = new ZipFile(jar.toFile())) {
        ZipEntry entry = zip.getEntry(tomcatCommonEntry);
        try (var in = zip.getInputStream(entry)) {
          Files.write(nested, in.readAllBytes());
        }
      }
      boolean foundCorrect = false;
      boolean foundDefaultPackage = false;
      try (ZipFile nestedZip = new ZipFile(nested.toFile())) {
        for (var it = nestedZip.entries().asIterator(); it.hasNext(); ) {
          String name = it.next().getName();
          if ("com/percussion/tomcat/valves/PSSimpleRedirectorValve.class".equals(name)) {
            foundCorrect = true;
          }
          if ("PSSimpleRedirectorValve.class".equals(name)) {
            foundDefaultPackage = true;
          }
        }
      }
      assertTrue(
          foundCorrect,
          "perc-tomcat-common must contain"
              + " com/percussion/tomcat/valves/PSSimpleRedirectorValve.class"
              + " (package declaration required for Tomcat digester)");
      assertTrue(
          !foundDefaultPackage,
          "PSSimpleRedirectorValve.class must not be in the default package (jar root)");
    } finally {
      Files.deleteIfExists(nested);
    }
  }

  @Test
  void commonLibEntriesMatchExpectedFilePattern() {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    Pattern pattern =
        Pattern.compile(
            "^distribution/Deployment/Server/common/lib/.+\\.(jar|properties|xsd|xml)$");
    List<String> invalid = new ArrayList<>();
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      zip.stream()
          .map(java.util.zip.ZipEntry::getName)
          .filter(name -> name.startsWith(COMMON_LIB_PATH_PREFIX))
          .filter(name -> !name.equals(COMMON_LIB_PATH_PREFIX))
          .forEach(
              name -> {
                if (!pattern.matcher(name).matches()) {
                  invalid.add(name);
                }
              });
    } catch (IOException io) {
      fail("Could not read " + jar + ": " + io.getMessage());
    }
    if (!invalid.isEmpty()) {
      fail("Unexpected entries under " + COMMON_LIB_PATH_PREFIX + ": " + invalid);
    }
  }

  /**
   * Product embedded default is H2 (#548). Cargo must not package Derby into Tomcat {@code
   * common/lib} — MANIFEST Class-Path locale jars cause StandardJarScanner NoSuchFileException
   * WARNs on every startup, and new installs do not use Derby.
   */
  @Test
  void shippingJarDoesNotBundleDerbyInCommonLib() throws IOException {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    List<String> derbyHits = new ArrayList<>();
    boolean h2Found = false;
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      zip.stream()
          .map(ZipEntry::getName)
          .filter(name -> name.startsWith(COMMON_LIB_PATH_PREFIX))
          .filter(name -> name.endsWith(".jar"))
          .forEach(
              name -> {
                String base = name.substring(COMMON_LIB_PATH_PREFIX.length()).toLowerCase();
                if (base.startsWith("derby")) {
                  derbyHits.add(name);
                }
              });
      h2Found =
          zip.stream()
              .map(ZipEntry::getName)
              .filter(n -> n.startsWith(COMMON_LIB_PATH_PREFIX) && n.endsWith(".jar"))
              .map(n -> n.substring(COMMON_LIB_PATH_PREFIX.length()).toLowerCase())
              .anyMatch(base -> base.startsWith("h2-"));
    }

    if (!derbyHits.isEmpty()) {
      fail(
          "Shipping jar must not put Derby on Tomcat common/lib (embedded default is H2)."
              + " Found: "
              + derbyHits
              + ". Remove org.apache.derby:* from cargo container dependencies in pom.xml.");
    }
    assertTrue(
        h2Found,
        "Shipping jar must include H2 under "
            + COMMON_LIB_PATH_PREFIX
            + " (cargo container dependency com.h2database:h2).");
  }
}
