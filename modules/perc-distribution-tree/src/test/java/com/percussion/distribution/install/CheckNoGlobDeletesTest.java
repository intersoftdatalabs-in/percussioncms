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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the Java port of {@code scripts/check-no-glob-deletes.sh}. Verifies the locator
 * returns glob names inside the {@code install_jdbc_drivers} {@code <delete>} block, and that an
 * installer with a glob-style {@code <include>} reports it cleanly.
 */
class CheckNoGlobDeletesTest {

  @Test
  @DisplayName("Collects globs from install_jdbc_drivers <delete>")
  void collectsGlobsFromDeleteBlock(@TempDir Path workdir) throws Exception {
    Path xml = workdir.resolve("install.xml");
    Files.writeString(
        xml,
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<project name=\"x\">"
            + "  <target name=\"other\"><delete><include name=\"foo.jar\"/></delete></target>"
            + "  <target name=\"install_jdbc_drivers\">"
            + "    <delete>"
            + "      <include name=\"mysql-connector-java-*.jar\"/>"
            + "      <include name=\"old-?-driver.jar\"/>"
            + "      <include name=\"good-driver.jar\"/>"
            + "    </delete>"
            + "  </target>"
            + "</project>",
        java.nio.charset.StandardCharsets.UTF_8);

    List<String> globs = CheckNoGlobDeletes.collectGlobsInDeleteBlock(xml);
    assertEquals(2, globs.size(), "two entries contain wildcards");
    assertTrue(globs.contains("mysql-connector-java-*.jar"));
    assertTrue(globs.contains("old-?-driver.jar"));
  }

  @Test
  @DisplayName("Other targets' <delete> blocks are ignored")
  void ignoresOtherTargets(@TempDir Path workdir) throws Exception {
    Path xml = workdir.resolve("install.xml");
    Files.writeString(
        xml,
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<project name=\"x\">"
            + "  <target name=\"other-jobs\"><delete>"
            + "    <include name=\"mysql-connector-java-*.jar\"/>"
            + "  </delete></target>"
            + "  <target name=\"install_jdbc_drivers\">"
            + "    <delete><include name=\"good.jar\"/></delete>"
            + "  </target>"
            + "</project>",
        java.nio.charset.StandardCharsets.UTF_8);

    List<String> globs = CheckNoGlobDeletes.collectGlobsInDeleteBlock(xml);
    assertTrue(globs.isEmpty(), "glob in unrelated target must NOT be flagged here");
  }

  @Test
  @DisplayName("Clean installer fixture yields empty glob list")
  void cleanFixtureReturnsEmptyList(@TempDir Path workdir) throws Exception {
    Path xml = workdir.resolve("install.xml");
    Files.writeString(
        xml,
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<project name=\"x\">"
            + "  <target name=\"install_jdbc_drivers\">"
            + "    <delete><include name=\"good-1.0.jar\"/></delete>"
            + "  </target>"
            + "</project>",
        java.nio.charset.StandardCharsets.UTF_8);

    List<String> globs = CheckNoGlobDeletes.collectGlobsInDeleteBlock(xml);
    assertTrue(globs.isEmpty());
  }

  @Test
  @DisplayName("Missing install_jdbc_drivers target is a hard parsing error")
  void missingTargetThrows(@TempDir Path workdir) throws Exception {
    Path xml = workdir.resolve("install.xml");
    Files.writeString(
        xml,
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<project name=\"x\"><target name=\"x\"/></project>",
        java.nio.charset.StandardCharsets.UTF_8);

    IOException ex =
        assertThrows(IOException.class, () -> CheckNoGlobDeletes.collectGlobsInDeleteBlock(xml));
    assertNotNull(ex.getMessage());
    assertTrue(ex.getMessage().contains("install_jdbc_drivers"), ex.getMessage());
  }

  @Test
  @DisplayName(
      "End-to-end on the real shipped install.xml returns no globs (current install expectations)")
  void shippedInstallXmlHasNoGlobs() throws Exception {
    // Uses multi-candidate resolution (CWD, basedir, monorepo walk) so the assertion still
    // runs when surefire's user.dir is not the module root. We do NOT silently skip when
    // the file is missing: a missing shipped install.xml means the invariant is not checked.
    Path xml = CheckNoGlobDeletes.computeDefaultInstallXmlPath();
    if (!Files.isRegularFile(xml)) {
      fail(
          "shipped install.xml not found (resolved default="
              + xml.toAbsolutePath()
              + ", user.dir="
              + Path.of("").toAbsolutePath()
              + ")");
    }
    List<String> globs = CheckNoGlobDeletes.collectGlobsInDeleteBlock(xml);
    assertTrue(
        globs.isEmpty(),
        "shipped install.xml must not contain glob-based <delete> entries; found: " + globs);
  }

  @Test
  @DisplayName("Default path resolution finds the shipped install.xml under the monorepo layout")
  void computeDefaultInstallXmlPathFindsShippedFile() {
    Path xml = CheckNoGlobDeletes.computeDefaultInstallXmlPath();
    assertTrue(
        Files.isRegularFile(xml),
        "expected to locate shipped install.xml; got " + xml.toAbsolutePath());
    assertTrue(
        xml.getFileName().toString().equals("install.xml"),
        "resolved path should end with install.xml: " + xml);
  }

  @Test
  @DisplayName("run() without args succeeds against shipped install.xml (no globs)")
  void runWithoutArgsAgainstShippedInstallXml() {
    int code = CheckNoGlobDeletes.run(new String[0]);
    assertEquals(CheckNoGlobDeletes.EXIT_OK, code, "gate should pass on current install.xml");
  }

  @Test
  @DisplayName("run() with --install-xml uses the explicit path")
  void runWithExplicitInstallXml(@TempDir Path workdir) throws Exception {
    Path xml = workdir.resolve("install.xml");
    Files.writeString(
        xml,
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<project name=\"x\">"
            + "  <target name=\"install_jdbc_drivers\">"
            + "    <delete><include name=\"good.jar\"/></delete>"
            + "  </target>"
            + "</project>",
        java.nio.charset.StandardCharsets.UTF_8);

    int code =
        CheckNoGlobDeletes.run(new String[] {"--install-xml", xml.toAbsolutePath().toString()});
    assertEquals(CheckNoGlobDeletes.EXIT_OK, code);
  }

  @Test
  @DisplayName("run() reports missing install.xml as invocation error")
  void runMissingInstallXml(@TempDir Path workdir) {
    Path missing = workdir.resolve("does-not-exist.xml");
    int code =
        CheckNoGlobDeletes.run(new String[] {"--install-xml", missing.toAbsolutePath().toString()});
    assertEquals(CheckNoGlobDeletes.EXIT_INVOCATION, code);
  }
}
