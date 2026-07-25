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
    Path jdk25 = makeHome(tempDir.resolve("jdk25"), "25", launcher);
    Path jdk8 = makeHome(tempDir.resolve("jdk8"), "1.8", launcher);
    Map<String, String> env = Map.of("JAVA_HOME", jdk21.toString());
    // PATH includes jdk8 and jdk25 bins so discovery considers them via PATH.
    String path =
        jdk8.resolve("bin") + java.io.File.pathSeparator + jdk25.resolve("bin");
    List<JavaCandidateDiscovery.Candidate> raw =
        JavaCandidateDiscovery.discover(env, jdk21.toString(), path);

    assertNotNull(raw);
    // Running JVM and JAVA_HOME both point at jdk21; we expect jdk21 to appear
    // once and jdk8/jdk25 to appear via PATH launcher inference.
    long jdk21Count = raw.stream().filter(c -> c.path().toString().equals(jdk21.toString())).count();
    long jdk8Count = raw.stream().filter(c -> c.path().toString().equals(jdk8.toString())).count();
    long jdk25Count = raw.stream().filter(c -> c.path().toString().equals(jdk25.toString())).count();
    assertEquals(1, jdk21Count, "jdk21 deduplicated across running JVM + env");
    assertTrue(jdk8Count >= 1, "jdk8 discovered via PATH launcher");
    assertTrue(jdk25Count >= 1, "jdk25 discovered via PATH launcher");

    List<JavaCandidateDiscovery.Candidate> eligible = JavaCandidateDiscovery.eligible(raw);
    assertTrue(
        eligible.stream().allMatch(c ->
            JavaCandidateDiscovery.Candidate.meetsMinimumMajor(c.versionDisplay())),
        "all eligible candidates meet minimum major 21");
    assertTrue(
        eligible.stream().anyMatch(c -> c.path().toString().equals(jdk21.toString())),
        "jdk21 must be eligible");
    assertTrue(
        eligible.stream().anyMatch(c -> c.path().toString().equals(jdk25.toString())),
        "jdk25 must be eligible (21+)");
    assertFalse(eligible.stream().anyMatch(c -> c.path().toString().equals(jdk8.toString())),
        "jdk8 must not be eligible");
  }

  @Test
  void meetsMinimumMajorAccepts21PlusRejectsOlder() {
    assertTrue(JavaCandidateDiscovery.Candidate.meetsMinimumMajor("21"));
    assertTrue(JavaCandidateDiscovery.Candidate.meetsMinimumMajor("25"));
    assertFalse(JavaCandidateDiscovery.Candidate.meetsMinimumMajor("17"));
    assertFalse(JavaCandidateDiscovery.Candidate.meetsMinimumMajor("1.8"));
    assertFalse(JavaCandidateDiscovery.Candidate.meetsMinimumMajor(""));
    assertFalse(JavaCandidateDiscovery.Candidate.meetsMinimumMajor(null));
  }

  /**
   * Regression: the per-major {@code JAVA_HOME_<NN>} env vars (e.g.
   * {@code JAVA_HOME_21}) are a developer-only convenience used by
   * {@code mvn-env.bat} and are NOT part of the installer / runtime
   * contract. The resolver must ignore them and rely only on the standard
   * {@code JAVA_HOME} and PATH / common-OS-locations discovery.
   */
  @Test
  void ignoresVersionedHomeEnvVars(@TempDir Path scratch) throws IOException {
    String launcher = launcherForCurrentPlatform();
    Path jdk21 = makeHome(scratch.resolve("jdk21"), "21", launcher);
    Path jdk8 = makeHome(scratch.resolve("jdk8"), "8", launcher);
    Map<String, String> env = Map.of(
        "JAVA_HOME_21", jdk21.toString(),
        "JAVA_HOME_8", jdk8.toString(),
        "PATH", scratch.resolve("jdk21").resolve("bin").toString());
    // JAVA_HOME_<NN> must NOT be promoted to a candidate on its own — only the
    // PATH-resident bin (jdk21) is discovered. jdk8 is on disk but not on PATH
    // and is not exposed via any standard env var, so it must NOT appear.
    List<JavaCandidateDiscovery.Candidate> raw =
        JavaCandidateDiscovery.discover(env, /* runningJavaHome */ null, env.get("PATH"));
    assertFalse(
        raw.stream().anyMatch(c -> c.path().toString().equals(jdk8.toString())),
        "JAVA_HOME_8 alone must not produce a candidate; found: " + raw);
    assertFalse(
        raw.stream().anyMatch(c -> c.path().toString().equals(jdk21.toString())
            && !c.path().toString().equals(scratch.resolve("jdk21").toString())),
        "JAVA_HOME_21 alone must not produce a candidate; found: " + raw);
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
    Path launcher = target.resolve("bin").resolve(launcherName);
    Files.createFile(launcher);
    // Eligibility requires an executable launcher on non-Windows hosts.
    launcher.toFile().setExecutable(true, false);
    return target;
  }
}
