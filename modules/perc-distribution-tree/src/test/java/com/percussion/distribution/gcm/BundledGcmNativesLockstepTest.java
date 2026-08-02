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
package com.percussion.distribution.gcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lockstep guard for GCM native staging (pom property/dependency + ANT available/include).
 *
 * <p>Analogous to JDBC {@code BundledJdbcDrivers} / {@code InstallXmlDeleteSetTest}: a version or
 * platform-path rename must update every contract site in the same commit.
 */
class BundledGcmNativesLockstepTest {

  private static final Pattern POM_PROPERTY =
      Pattern.compile(
          "<"
              + Pattern.quote(BundledGcmNatives.POM_VERSION_PROPERTY)
              + ">([^<]+)</"
              + Pattern.quote(BundledGcmNatives.POM_VERSION_PROPERTY)
              + ">");

  private static final Pattern DEP_VERSION_NEAR_ARTIFACT =
      Pattern.compile(
          "<artifactId>"
              + Pattern.quote(BundledGcmNatives.ARTIFACT_ID)
              + "</artifactId>\\s*<version>\\$\\{"
              + Pattern.quote(BundledGcmNatives.POM_VERSION_PROPERTY)
              + "\\}</version>",
          Pattern.DOTALL);

  @Test
  @DisplayName("pom property and mkd-gcm-natives dependency version stay aligned")
  void pomPropertyMatchesDependency() throws Exception {
    String pom = Files.readString(resolveModulePom(), StandardCharsets.UTF_8);
    Matcher prop = POM_PROPERTY.matcher(pom);
    assertTrue(
        prop.find(), "pom.xml must declare <" + BundledGcmNatives.POM_VERSION_PROPERTY + ">");
    assertFalse(prop.group(1).isBlank(), "mkd.gcm.version must not be blank");
    assertTrue(
        pom.contains("<groupId>" + BundledGcmNatives.GROUP_ID + "</groupId>"),
        "pom must depend on " + BundledGcmNatives.GROUP_ID);
    assertTrue(
        DEP_VERSION_NEAR_ARTIFACT.matcher(pom).find(),
        "mkd-gcm-natives dependency version must be ${"
            + BundledGcmNatives.POM_VERSION_PROPERTY
            + "}");
    assertTrue(
        pom.contains("stage-gcm-natives"), "pom must declare stage-gcm-natives unpack execution");
    assertTrue(
        pom.contains("dev/monkeyking/gcm/native/**"),
        "stage-gcm-natives must unpack native/** layout");
  }

  @Test
  @DisplayName("ANT available paths and includes match BundledGcmNatives platforms")
  void antStagingMatchesBundledSet() throws Exception {
    String ant = Files.readString(resolveInstallAnt(), StandardCharsets.UTF_8);
    assertTrue(ant.contains("Staging mkd_gcm_ffi natives"), "GCM staging block must be present");

    Set<String> availableHits = new LinkedHashSet<>();
    for (String rel : BundledGcmNatives.AVAILABLE_RELATIVE_PATHS) {
      // ANT uses ${assembly-directory}/_gcm-native-stage/...
      String needle = rel.replace('\\', '/');
      assertTrue(ant.contains(needle), "installDistributionFiles.xml must <available> " + needle);
      availableHits.add(needle);
    }
    assertEquals(
        BundledGcmNatives.AVAILABLE_RELATIVE_PATHS.size(),
        availableHits.size(),
        "every platform available path must appear exactly once in the contract set");

    for (String name : BundledGcmNatives.INCLUDE_FILENAMES) {
      assertTrue(
          ant.contains("<include name=\"**/" + name + "\"")
              || ant.contains("<include name='**/" + name + "'"),
          "installDistributionFiles.xml must <include> **/" + name);
    }

    // Guard against inventing Darwin paths without updating BundledGcmNatives.
    assertFalse(
        ant.contains("darwin") || ant.contains("macos") || ant.contains("osx"),
        "Do not stage macOS paths until BundledGcmNatives and upstream natives ship them");
  }

  private static Path resolveModulePom() throws IOException {
    return resolveModuleFile("pom.xml");
  }

  private static Path resolveInstallAnt() throws IOException {
    return resolveModuleFile("src/main/resources/installDistributionFiles.xml");
  }

  private static Path resolveModuleFile(String relative) throws IOException {
    Path cwd = Paths.get("").toAbsolutePath().normalize();
    Path[] candidates =
        new Path[] {
          cwd.resolve(relative),
          cwd.resolve("modules/perc-distribution-tree").resolve(relative),
          cwd.resolve("../../modules/perc-distribution-tree").resolve(relative).normalize(),
        };
    for (Path p : candidates) {
      if (p != null && Files.isRegularFile(p.normalize())) {
        Path n = p.normalize();
        if (relative.endsWith("pom.xml")) {
          String text = Files.readString(n, StandardCharsets.UTF_8);
          if (text.contains("<artifactId>perc-distribution-tree</artifactId>")) {
            return n;
          }
        } else {
          return n;
        }
      }
    }
    throw new IOException("Could not locate " + relative + " from " + cwd);
  }
}
