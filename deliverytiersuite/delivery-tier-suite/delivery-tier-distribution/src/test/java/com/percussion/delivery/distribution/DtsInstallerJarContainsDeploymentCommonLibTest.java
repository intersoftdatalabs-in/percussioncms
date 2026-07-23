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
 * Regression guard for issue #1473.
 *
 * <p>The bundled Ant installer (installDts.xml:344-349) sources log4j,
 * commons-logging, slf4j, and disruptor jars from
 * {@code ${install.src}/Deployment/Server/common/lib/}. Without the fix
 * in this commit, that directory is absent from the shipping jar and the
 * install fails at line 344 with
 * {@code Deployment\Server\common\lib does not exist}.
 *
 * <p>This test asserts the shipping jar contains the expected runtime jars
 * under {@code distribution/Deployment/Server/common/lib/}. The test is a
 * no-op when run outside Maven (no build artifact on disk) so IDE / ad-hoc
 * runs are unaffected.
 */
class DtsInstallerJarContainsDeploymentCommonLibTest {

  private static final String COMMON_LIB_PATH_PREFIX =
      "distribution/Deployment/Server/common/lib/";

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
        "log4j-api-",
        "log4j-core-",
        "commons-logging-",
        "slf4j-api-",
        "disruptor-"
    };

    StringBuilder missing = new StringBuilder();
    for (String prefix : requiredPrefixes) {
      boolean found = matches.stream().anyMatch(name -> {
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
              + ". installDts.xml:344 reads from this directory and"
              + " the install fails with 'Deployment\\Server\\common\\lib does not exist' (issue #1473).");
    }

    // Sanity: the directory entry must exist (zip directories are usually
    // recorded alongside their file entries).
    assertTrue(
        matches.stream().anyMatch(name -> name.equals(COMMON_LIB_PATH_PREFIX))
            || matches.stream().anyMatch(name -> name.startsWith(COMMON_LIB_PATH_PREFIX)),
        () ->
            "No entries under " + COMMON_LIB_PATH_PREFIX + " in " + jar
                + "; expected the runtime jars staged by the"
                + " copy-deployment-server-common-lib maven-dependency-plugin:copy"
                + " execution.");
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
}