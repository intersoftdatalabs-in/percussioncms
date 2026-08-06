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
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for the DTS installer jar packaging.
 *
 * <p>The preinstall extractor (MainDTSPreInstall.extractArchive) writes the shipping jar's {@code
 * distribution/} directory into a temp folder, then expects to find {@code
 * distribution/rxconfig/Installer/perc-ant-X.Y.Z.jar} alongside {@code
 * distribution/rxconfig/Installer/installDts.xml}.
 *
 * <p>maven-antrun-plugin and maven-dependency-plugin:copy both stage {@code perc-ant-*.jar} into
 * {@code target/classes/distribution/rxconfig/Installer/}, but maven-jar-plugin's default excludes
 * pattern silently dropped the bundled jar from the shipping jar (manifest showed only the .xml
 * files). This test asserts the shipping jar contains the expected bundle.
 *
 * <p>If a future pom change re-enables the default excludes, this test fails before the broken
 * installer can ship.
 */
class DtsInstallerJarContainsPercAntTest {

  private static final String PERC_ANT_PATH_PREFIX = "distribution/rxconfig/Installer/perc-ant-";

  /**
   * No-op when run outside Maven (e.g. IDE) since target/delivery-tier-distribution.jar is a build
   * artifact. The antrun 'verify-pathvalidation-shaded' gate already runs at verify-time and
   * asserts similar structural invariants on the same jar.
   */
  @Test
  void shippingJarBundlesPercAnt() {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    List<String> matches = new ArrayList<>();
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      zip.stream()
          .map(java.util.zip.ZipEntry::getName)
          .filter(name -> name.startsWith(PERC_ANT_PATH_PREFIX))
          .forEach(matches::add);
    } catch (IOException io) {
      fail("Could not read " + jar + ": " + io.getMessage());
    }

    Pattern namePattern = Pattern.compile("^distribution/rxconfig/Installer/perc-ant-.+\\.jar$");
    List<String> invalid = matches.stream().filter(namePattern.asPredicate().negate()).toList();
    if (!invalid.isEmpty()) {
      fail(
          "Shipping jar contains entries under "
              + PERC_ANT_PATH_PREFIX
              + " that do not match the expected file pattern: "
              + invalid);
    }
    assertTrue(
        matches.size() >= 1,
        () ->
            "Expected at least one perc-ant jar entry under "
                + PERC_ANT_PATH_PREFIX
                + " in "
                + jar
                + "; none found. The shipping jar's installer payload must bundle"
                + " perc-ant.jar because MainDTSPreInstall.extractArchive unpacks the"
                + " jar into a temp folder and then executes the ant installer from there."
                + " Check whether maven-jar-plugin default excludes (which contains jar"
                + " file patterns) has been re-enabled in the pom. If so, set"
                + " addDefaultExcludes=false on the maven-jar-plugin configuration"
                + " to keep the bundled jar.");
  }
}
