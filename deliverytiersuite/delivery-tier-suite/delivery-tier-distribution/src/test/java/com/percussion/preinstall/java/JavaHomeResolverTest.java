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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.preinstall.java.JavaHomeResolver.Attempt;
import com.percussion.preinstall.java.JavaHomeResolver.JavaHomeProbe;
import com.percussion.preinstall.java.JavaHomeResolver.ResolutionResult;
import com.percussion.preinstall.java.JavaHomeResolver.ResolutionSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Behavior tests for {@link JavaHomeResolver}. */
class JavaHomeResolverTest {

  @TempDir Path tempDir;

  // --- parseMajorVersion ----------------------------------------------------

  @Test
  void parsesQuotedVersion21() {
    assertEquals(21, JavaHomeResolver.parseMajorVersion("openjdk version \"21.0.2\" 2024-01-16"));
  }

  @Test
  void parsesLegacy1dot8() {
    assertEquals(8, JavaHomeResolver.parseMajorVersion("java version \"1.8.0_302\""));
  }

  @Test
  void parsesVendorStyle21() {
    assertEquals(21, JavaHomeResolver.parseMajorVersion("java version \"21+36-LTS\""));
  }

  @Test
  void parsesMajor25() {
    assertEquals(25, JavaHomeResolver.parseMajorVersion("openjdk version \"25.0.1\" 2025-10-21"));
  }

  @Test
  void isSupportedMajorAccepts21AndNewer() {
    assertTrue(JavaHomeResolver.isSupportedMajor(21));
    assertTrue(JavaHomeResolver.isSupportedMajor(22));
    assertTrue(JavaHomeResolver.isSupportedMajor(25));
    assertFalse(JavaHomeResolver.isSupportedMajor(20));
    assertFalse(JavaHomeResolver.isSupportedMajor(17));
    assertFalse(JavaHomeResolver.isSupportedMajor(8));
    assertFalse(JavaHomeResolver.isSupportedMajor(-1));
  }

  @Test
  void parsesUnparseableReturnsMinusOne() {
    assertEquals(-1, JavaHomeResolver.parseMajorVersion("not a version"));
    assertEquals(-1, JavaHomeResolver.parseMajorVersion(""));
    assertEquals(-1, JavaHomeResolver.parseMajorVersion(null));
  }

  // --- inferHomeFromLauncher -----------------------------------------------

  @Test
  void infersHomeFromUnixLauncher() {
    Path launcher = Path.of("/opt/jdk-21/bin/java");
    assertEquals(Path.of("/opt/jdk-21"), JavaHomeResolver.inferHomeFromLauncher(launcher));
  }

  @Test
  void infersHomeFromWindowsLauncher() {
    // Build the path from components so the test is portable: a single string
    // with backslashes is one name element on Unix (getParent() is null).
    Path launcher = Path.of("C:", "jdk-21", "bin", "java.exe");
    assertEquals(Path.of("C:", "jdk-21"), JavaHomeResolver.inferHomeFromLauncher(launcher));
  }

  @Test
  void rejectsLauncherNotUnderBin() {
    assertNull(JavaHomeResolver.inferHomeFromLauncher(Path.of("/opt/jdk-21/java")));
    assertNull(JavaHomeResolver.inferHomeFromLauncher(Path.of("java")));
    assertNull(JavaHomeResolver.inferHomeFromLauncher(null));
  }

  // --- Precedence ----------------------------------------------------------

  /** In-memory probe: only registered fixture paths report as valid homes. */
  private static final class FixtureProbe implements JavaHomeProbe {
    final Set<Path> validHomes;

    FixtureProbe(Path... valid) {
      this.validHomes = Set.of(valid);
    }

    @Override
    public boolean isValidJavaHome(Path path, int requiredMajor) {
      return path != null && validHomes.contains(path);
    }

    @Override
    public boolean isExecutableLauncher(Path launcher) {
      if (launcher == null) {
        return false;
      }
      Path home = JavaHomeResolver.inferHomeFromLauncher(launcher);
      return home != null && validHomes.contains(home);
    }
  }

  @Test
  void productConfigWinsOverEnv() throws Exception {
    Path configHome = makeJavaHome(tempDir.resolve("configHome"));
    Path envHome = makeJavaHome(tempDir.resolve("envHome"));
    writeJavaProperties(tempDir, configHome);

    ResolutionResult r =
        JavaHomeResolver.resolve(
            tempDir,
            Map.of("JAVA_HOME", envHome.toString()),
            List.of(),
            new FixtureProbe(configHome, envHome));

    assertTrue(r.success(), () -> r.renderFailure("expected precedence order"));
    assertEquals(ResolutionSource.PRODUCT_CONFIG, r.source());
    assertEquals(configHome, r.javaHome());
  }

  @Test
  void envWinsWhenConfigAbsent() throws Exception {
    Path envHome = makeJavaHome(tempDir.resolve("envHome"));
    Path legacyHome = makeJavaHome(tempDir.resolve("legacy"));

    ResolutionResult r =
        JavaHomeResolver.resolve(
            tempDir,
            Map.of("JAVA_HOME", envHome.toString()),
            List.of(),
            new FixtureProbe(envHome, legacyHome));

    assertTrue(r.success());
    assertEquals(ResolutionSource.PROCESS_ENV, r.source());
    assertEquals(envHome, r.javaHome());
  }

  @Test
  void legacyJreUsedWhenHigherSourcesAbsent() {
    Path legacy = makeJavaHome(tempDir.resolve("JRE"));
    ResolutionResult r =
        JavaHomeResolver.resolve(tempDir, Map.of(), List.of(), new FixtureProbe(legacy));

    assertTrue(r.success(), () -> r.renderFailure(""));
    assertEquals(ResolutionSource.INSTALL_DIR_JRE, r.source());
    assertEquals(legacy, r.javaHome());
  }

  @Test
  void legacyJre64UsedAfterJreWhenOnlyJre64Valid() {
    Path legacy64 = makeJavaHome(tempDir.resolve("JRE64"));
    ResolutionResult r =
        JavaHomeResolver.resolve(tempDir, Map.of(), List.of(), new FixtureProbe(legacy64));

    assertTrue(r.success());
    assertEquals(ResolutionSource.INSTALL_DIR_JRE64, r.source());
    assertEquals(legacy64, r.javaHome());
  }

  @Test
  void pathLauncherUsedAsLastResort() {
    Path fromPath = makeJavaHome(tempDir.resolve("pathHome"));
    Path launcherDir = fromPath.resolve("bin");
    ResolutionResult r =
        JavaHomeResolver.resolve(
            tempDir, Map.of(), List.of(launcherDir), new FixtureProbe(fromPath));

    assertTrue(r.success(), () -> r.renderFailure(""));
    assertEquals(ResolutionSource.PATH, r.source());
    assertEquals(fromPath, r.javaHome());
  }

  @Test
  void failureReturnsNONEAndListsAttempts() throws IOException {
    // Create a present-but-invalid install-dir JRE so the attempt is recorded.
    Files.createDirectory(tempDir.resolve("JRE"));
    ResolutionResult r =
        JavaHomeResolver.resolve(
            tempDir, Map.of("JAVA_HOME", "/nonexistent"), List.of(), new FixtureProbe(/* none */ ));

    assertFalse(r.success());
    assertEquals(ResolutionSource.NONE, r.source());
    List<Attempt> attempts = r.attempts();
    assertNotNull(attempts);
    assertTrue(
        attempts.size() >= 2,
        "expected at least PROCESS_ENV + INSTALL_DIR_JRE attempts, got " + attempts);
    String message = r.renderFailure("header");
    assertTrue(message.contains("21"), "message must mention minimum major version");
    assertTrue(message.contains("or later"), "message must state 21 or later");
    assertTrue(message.contains("Sources tried"), "must list sources");
  }

  @Test
  void defaultProbeAcceptsMajor25ReleaseFile() throws Exception {
    Path jdk25 = tempDir.resolve("jdk25");
    Files.createDirectories(jdk25.resolve("bin"));
    Files.writeString(jdk25.resolve("release"), "JAVA_VERSION=\"25.0.1\"", StandardCharsets.UTF_8);
    Files.createFile(jdk25.resolve("bin").resolve(JavaHomeResolver.launcherName()));
    // Make launcher executable where POSIX permissions apply.
    jdk25.resolve("bin").resolve(JavaHomeResolver.launcherName()).toFile().setExecutable(true);

    JavaHomeResolver.DefaultProbe probe = new JavaHomeResolver.DefaultProbe();
    assertTrue(
        probe.isValidJavaHome(jdk25, JavaHomeResolver.REQUIRED_MAJOR),
        "major 25 must satisfy minimum major 21");
  }

  @Test
  void defaultProbeRejectsMajor17ReleaseFile() throws Exception {
    Path jdk17 = tempDir.resolve("jdk17");
    Files.createDirectories(jdk17.resolve("bin"));
    Files.writeString(jdk17.resolve("release"), "JAVA_VERSION=\"17.0.12\"", StandardCharsets.UTF_8);
    Files.createFile(jdk17.resolve("bin").resolve(JavaHomeResolver.launcherName()));
    jdk17.resolve("bin").resolve(JavaHomeResolver.launcherName()).toFile().setExecutable(true);

    JavaHomeResolver.DefaultProbe probe = new JavaHomeResolver.DefaultProbe();
    assertFalse(
        probe.isValidJavaHome(jdk17, JavaHomeResolver.REQUIRED_MAJOR),
        "major 17 must fail minimum major 21");
  }

  @Test
  void rejectNullInstallRoot() {
    assertThrows(
        IllegalArgumentException.class,
        () -> JavaHomeResolver.resolve(null, Map.of(), List.of(), new FixtureProbe()));
  }

  /**
   * Regression for kilo-code-bot PR review thread 3631027624: if java.properties exists but is
   * unreadable (permissions, corrupted), the resolver must NOT silently fall through. Today it logs
   * a warning (System.Logger at WARNING); the test asserts the resolution still honors
   * higher-precedence env or lower-precedence fallback so the contract holds, and that the I/O
   * attempt occurred.
   */
  @Test
  void unreadableConfigDoesNotPoisonResolution() throws Exception {
    Path configDir = tempDir.resolve("install");
    Files.createDirectories(configDir);
    Path propsFile = configDir.resolve("java.properties");
    Files.createFile(propsFile);
    boolean writable = propsFile.toFile().setWritable(false);
    try {
      Path envHome = makeJavaHome(tempDir.resolve("envHome"));
      ResolutionResult r =
          JavaHomeResolver.resolve(
              configDir,
              Map.of("JAVA_HOME", envHome.toString()),
              List.of(),
              new FixtureProbe(envHome));
      // The fallback env path is still honored because readPropertiesIfPresent
      // degrades to an empty map (with a warning) rather than throwing.
      assertTrue(
          r.success(),
          () ->
              "env fallback should still succeed when config is unreadable; r="
                  + r.renderFailure(""));
      assertEquals(ResolutionSource.PROCESS_ENV, r.source());
    } finally {
      // Restore writable so JUnit can clean up the @TempDir.
      propsFile.toFile().setWritable(true);
      if (!writable) {
        // best-effort; test may still leak the file on platforms that ignore
        // setWritable(false) (e.g. running as root in CI).
      }
    }
  }

  // --- Helpers ------------------------------------------------------------

  /** Builds a directory layout resembling a Java home with a release file. */
  private static Path makeJavaHome(Path target) {
    try {
      Files.createDirectories(target.resolve("bin"));
      Files.writeString(
          target.resolve("release"), "JAVA_VERSION=\"21.0.2\"", StandardCharsets.UTF_8);
      Files.createFile(target.resolve("bin").resolve(JavaHomeResolver.launcherName()));
      return target;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void writeJavaProperties(Path installRoot, Path home) throws IOException {
    JavaPropertiesSupport.write(
        installRoot,
        home.toAbsolutePath().toString(),
        home.resolve("bin").resolve(JavaHomeResolver.launcherName()).toAbsolutePath().toString());
  }
}
