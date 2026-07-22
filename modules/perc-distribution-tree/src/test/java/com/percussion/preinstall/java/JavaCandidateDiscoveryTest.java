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

package com.percussion.preinstall.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** T028: candidate discovery / filter tests. */
class JavaCandidateDiscoveryTest {

  @TempDir Path tempDir;

  @Test
  void discoversRunningJvmAndEnvAndPaths() throws IOException {
    String launcher = launcherForCurrentPlatform();
    Path jdk21 = makeHome(tempDir.resolve("jdk21"), "21", launcher);
    Path jdk8 = makeHome(tempDir.resolve("jdk8"), "1.8", launcher);
    Map<String, String> env = Map.of("JAVA_HOME", jdk21.toString());
    // PATH includes jdk8 bin so discovery would consider it via PATH source.
    List<JavaCandidateDiscovery.Candidate> raw =
        JavaCandidateDiscovery.discover(env, jdk21.toString(),
            jdk8.resolve("bin").toString());

    assertNotNull(raw);
    // Running JVM and JAVA_HOME both point at jdk21; we expect jdk21 to appear
    // once and jdk8 to appear via PATH launcher inference.
    long jdk21Count = raw.stream().filter(c -> c.path().toString().equals(jdk21.toString())).count();
    long jdk8Count = raw.stream().filter(c -> c.path().toString().equals(jdk8.toString())).count();
    assertEquals(1, jdk21Count, "jdk21 deduplicated across running JVM + env");
    assertTrue(jdk8Count >= 1, "jdk8 discovered via PATH launcher");

    List<JavaCandidateDiscovery.Candidate> eligible = JavaCandidateDiscovery.eligible(raw);
    assertTrue(eligible.stream().allMatch(c -> c.versionDisplay().startsWith("21")),
        "all eligible candidates report Java 21 version");
    assertFalse(eligible.stream().anyMatch(c -> c.path().toString().equals(jdk8.toString())),
        "jdk8 must not be eligible");
  }

  private static String launcherForCurrentPlatform() {
    String os = System.getProperty("os.name", "");
    return os.toLowerCase(java.util.Locale.ROOT).contains("win") ? "java.exe" : "java";
  }

  @Test
  void pathLauncherHelperDetectsHomesUnderBin() throws IOException {
    Path home = makeHome(tempDir.resolve("realjdk"), "21", "java");
    // Sanity: inferHomeFromLauncher returns the parent of bin.
    Path launcher = home.resolve("bin").resolve("java");
    assertEquals(home, JavaHomeResolver.inferHomeFromLauncher(launcher));
  }

  // ---- helpers ----

  static Path makeHome(Path target, String majorMinor, String launcherName) throws IOException {
    Files.createDirectories(target.resolve("bin"));
    Files.writeString(target.resolve("release"),
        "JAVA_VERSION=\"" + majorMinor + ".0.1\"",
        StandardCharsets.UTF_8);
    Files.createFile(target.resolve("bin").resolve(launcherName));
    return target;
  }
}
