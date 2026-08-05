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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * GH-1978 / GH-962 slice 3: CMS distribution peers for Jetty {@code service/} dual-ship.
 *
 * <p>{@code installDistributionFiles.xml} copies the entire unpacked perc-jetty tree into {@code
 * jetty/} with no exclude of {@code service/}. {@code install.xml} upgrade overwrite includes all
 * shell scripts so {@code jetty/service/install-jetty-service.sh} refreshes on upgrade. This test
 * is structural (source Ant/XML contracts) plus an optional staged-assembly check when a prior
 * perc-jetty unpack is present under {@code target/jetty/}.
 *
 * <p>Dual-ship policy: keep init.d until soak; do not remove init.d (issue 1976 deferred).
 */
class JettyServiceDualShipPackagingTest {

  private static final String[] REQUIRED_SERVICE_RELATIVE = {
    "service/install-jetty-service.sh",
    "service/percussion-cms.service.in",
    "service/README-systemd.md",
  };

  @Test
  void installDistributionFiles_copiesWholePercJettyTreeWithoutServiceExclude() throws IOException {
    Path ant = resolveModuleResource("src/main/resources/installDistributionFiles.xml");
    assertTrue(Files.isRegularFile(ant), () -> "missing " + ant.toAbsolutePath());
    String xml = Files.readString(ant, StandardCharsets.UTF_8);

    assertTrue(
        xml.contains("perc-jetty-${project.version}")
            || xml.contains("perc-jetty-")
            || xml.contains("${jetty-directory}"),
        "installDistributionFiles must unpack/copy perc-jetty into assembly");
    assertTrue(
        xml.contains("<copy todir=\"${assembly-directory}/jetty\">")
            || xml.contains("todir=\"${assembly-directory}/jetty\""),
        "must copy jetty tree to assembly-directory/jetty");

    // No intentional strip of service dual-ship assets
    assertFalse(
        xml.contains("service/install-jetty-service") && xml.contains("<exclude"),
        "must not pair service installer with an exclude in a way that drops dual-ship");
    assertFalse(
        xml.contains("<exclude name=\"**/service/**\"")
            || xml.contains("<exclude name=\"service/**\"")
            || xml.contains("<exclude name=\"**/install-jetty-service.sh\""),
        "must not exclude jetty/service dual-ship tree from distribution assembly");
  }

  @Test
  void installXml_upgradeOverwriteIncludesShellScripts_forServiceRefresh() throws IOException {
    Path installXml =
        resolveModuleResource("src/main/resources/distribution/rxconfig/Installer/install.xml");
    assertTrue(Files.isRegularFile(installXml), () -> "missing " + installXml.toAbsolutePath());
    String xml = Files.readString(installXml, StandardCharsets.UTF_8);

    // upgrade.overwrite patternset includes **/*.sh → jetty/service/*.sh refreshed
    assertTrue(
        xml.contains("upgrade.overwrite"), "install.xml must define upgrade.overwrite patternset");
    assertTrue(
        xml.contains("<include name=\"**/*.sh\"") || xml.contains("name=\"**/*.sh\""),
        "upgrade.overwrite must include **/*.sh so jetty/service install scripts refresh");

    // Must not exclude jetty/service from upgrade preserve/overwrite paths
    assertFalse(
        xml.contains("<exclude name=\"jetty/service/**\"")
            || xml.contains("<exclude name=\"**/service/install-jetty-service.sh\""),
        "must not exclude CMS jetty service dual-ship from install.xml");
  }

  @Test
  void stagedPercJettyUnpack_includesServiceDualShipWhenPresent() throws IOException {
    Path jettyRoot = Paths.get("target", "jetty");
    if (!Files.isDirectory(jettyRoot)) {
      jettyRoot = Paths.get("modules", "perc-distribution-tree", "target", "jetty");
    }
    assumeTrue(Files.isDirectory(jettyRoot), "perc-jetty not unpacked under target/jetty yet");

    Path tree = findPercJettyTree(jettyRoot);
    assumeTrue(tree != null, "no perc-jetty-* directory under target/jetty");

    for (String rel : REQUIRED_SERVICE_RELATIVE) {
      Path f = tree;
      for (String part : rel.split("/")) {
        f = f.resolve(part);
      }
      final Path expected = f;
      final String relative = rel;
      assertTrue(
          Files.isRegularFile(expected),
          () ->
              "unpacked perc-jetty missing "
                  + relative
                  + " under "
                  + tree
                  + " (GH-1978 dual-ship; rebuild perc-jetty then this module)");
    }
  }

  private static Path findPercJettyTree(Path jettyRoot) throws IOException {
    try (var stream = Files.list(jettyRoot)) {
      return stream
          .filter(Files::isDirectory)
          .filter(p -> p.getFileName().toString().startsWith("perc-jetty-"))
          .findFirst()
          .orElse(null);
    }
  }

  private static Path resolveModuleResource(String relativeUnixPath) {
    Path direct = Path.of("").toAbsolutePath().resolve(relativeUnixPath);
    if (Files.isRegularFile(direct)) {
      return direct.normalize();
    }
    // Parts for portable Path resolve
    String[] parts = relativeUnixPath.split("/");
    Path fromModule = Path.of("modules", "perc-distribution-tree");
    for (String p : parts) {
      fromModule = fromModule.resolve(p);
    }
    if (Files.isRegularFile(fromModule)) {
      return fromModule.toAbsolutePath().normalize();
    }
    // Walk up from CWD looking for installDistributionFiles.xml marker
    Path cwd = Path.of("").toAbsolutePath().normalize();
    for (int i = 0; i < 6 && cwd != null; i++) {
      Path candidate = cwd;
      for (String p : parts) {
        candidate = candidate.resolve(p);
      }
      if (Files.isRegularFile(candidate)) {
        return candidate.normalize();
      }
      cwd = cwd.getParent();
    }
    return Path.of(relativeUnixPath);
  }
}
