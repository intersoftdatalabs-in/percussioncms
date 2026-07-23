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

package com.percussion.jetty.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Behavioral script-invocation tests (FR-013 layer-3) for the runtime
 * {@code resolve-java-home.sh} helper on Linux + macOS.
 *
 * <p>The fixture lives at {@code src/test/resources/fixtures/fake-java-home/jre/bin/java}.
 * The fixture encodes the major version in its parent directory's basename suffix
 * (e.g. {@code fake-java-home-21/.../java}), so the same source file emits different
 * version strings depending on which {@code TempDir} the test creates the install
 * root in. No real JDK installations are required.
 *
 * <p>The test class is a no-op when the fixture or the resolve-java-home script
 * is missing (same guard pattern as {@code DtsInstallerJarContainsPercAntTest})
 * so IDE / ad-hoc runs are unaffected.
 *
 * <p><b>Scope:</b> Linux + macOS only. Windows requires a real PE-binary launcher
 * fixture (the {@code java.bat} content cannot execute as {@code java.exe}); Windows
 * coverage is provided by the structural tests {@code DtsJavaHomeScriptTest} and
 * {@code ResolveJavaHomeScriptTest} (FR-013 layer-2).
 */
class ResolveJavaHomeBehaviorTest {

  private static final Path FIXTURE =
      Path.of("src", "test", "resources", "fixtures", "fake-java-home", "jre", "bin", "java");
  private static final Path RESOLVE_SH =
      Path.of("src", "main", "jetty", "resolve-java-home.sh");

  @BeforeAll
  static void locateAssets() {
    Assumptions.assumeTrue(
        Files.isRegularFile(FIXTURE),
        "fake-java fixture missing at " + FIXTURE.toAbsolutePath()
            + "; skipping behavioral tests");
    Assumptions.assumeTrue(
        Files.isRegularFile(RESOLVE_SH),
        "resolve-java-home.sh missing at " + RESOLVE_SH.toAbsolutePath()
            + "; skipping behavioral tests");
  }

  /** Scenario 1: config-only happy path. */
  @ParameterizedTest
  @ValueSource(strings = {"21"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void configOnlyHappyPath(String fakeMajor, @TempDir Path scratch) throws Exception {
    setupFakeJavaHome(scratch, fakeMajor, true, false, false);
    ResolveResult r = runResolver(scratch);
    assertSuccess(r, "PRODUCT_CONFIG", "21");
  }

  /** Scenario 2: env-only happy path. */
  @ParameterizedTest
  @ValueSource(strings = {"21"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void envOnlyHappyPath(String fakeMajor, @TempDir Path scratch) throws Exception {
    setupFakeJavaHome(scratch, fakeMajor, false, true, false);
    ResolveResult r = runResolver(scratch);
    assertSuccess(r, "PROCESS_ENV", "21");
  }

  /** Scenario 3: PATH happy path. */
  @ParameterizedTest
  @ValueSource(strings = {"21"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void pathOnlyHappyPath(String fakeMajor, @TempDir Path scratch) throws Exception {
    setupFakeJavaHome(scratch, fakeMajor, false, false, true);
    ResolveResult r = runResolver(scratch);
    assertSuccess(r, "PATH", "21");
  }

  /** Scenario 4: config beats env. */
  @ParameterizedTest
  @ValueSource(strings = {"21"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void configBeatsEnv(String fakeMajor, @TempDir Path scratch) throws Exception {
    setupFakeJavaHome(scratch, fakeMajor, true, true, false);
    ResolveResult r = runResolver(scratch);
    assertSuccess(r, "PRODUCT_CONFIG", "21");
  }

  /** Scenario 5: env rejects wrong major (Java 8). */
  @ParameterizedTest
  @ValueSource(strings = {"8"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void envRejectsWrongMajor(String fakeMajor, @TempDir Path scratch) throws Exception {
    setupFakeJavaHome(scratch, fakeMajor, false, true, false);
    ResolveResult r = runResolver(scratch);
    assertFailure(r);
  }

  /** Scenario 6: PATH rejects wrong major (Java 8). */
  @ParameterizedTest
  @ValueSource(strings = {"8"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void pathRejectsWrongMajor(String fakeMajor, @TempDir Path scratch) throws Exception {
    setupFakeJavaHome(scratch, fakeMajor, false, false, true);
    ResolveResult r = runResolver(scratch);
    assertFailure(r);
  }

  /** Scenario 7: config rejects invalid path. */
  @ParameterizedTest
  @ValueSource(strings = {"21"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void configRejectsInvalidPath(String fakeMajor, @TempDir Path scratch) throws Exception {
    setupFakeJavaHome(scratch, fakeMajor, true, false, false);
    // Overwrite config with an invalid path
    Files.writeString(
        scratch.resolve("java.properties"),
        "JAVA_HOME=C:/does/not/exist\nJAVA=C:/does/not/exist/bin/java\n",
        StandardCharsets.UTF_8);
    ResolveResult r = runResolver(scratch);
    assertFailure(r);
  }

  // --- Helpers ---

  /**
   * Sets up a per-test install root with the fake-java fixture. The {@code @TempDir}
   * parent is named {@code fake-java-home-N-XXXX} so the fixture (which reads the
   * parent dir's suffix) emits the right major version.
   */
  private static void setupFakeJavaHome(
      Path installRoot, String fakeMajor, boolean writeConfig, boolean envHome, boolean onPath)
      throws java.io.IOException {
    // We need a directory whose basename ends with -NN so the fixture's case
    // statement picks the right version. JUnit @TempDir can place us under a
    // unique subdir; we rename to add the version suffix.
    Path withVersion = installRoot.resolveSibling(installRoot.getFileName() + "-" + fakeMajor);
    Files.move(installRoot, withVersion);
    installRoot = withVersion;

    Path bin = installRoot.resolve("bin");
    Files.createDirectories(bin);
    Files.copy(FIXTURE, bin.resolve("java"), StandardCopyOption.REPLACE_EXISTING);
    Path javaExe = bin.resolve("java");
    javaExe.toFile().setExecutable(true, false);
    try {
      Files.setPosixFilePermissions(javaExe, Set.of(
          PosixFilePermission.OWNER_READ,
          PosixFilePermission.OWNER_WRITE,
          PosixFilePermission.OWNER_EXECUTE,
          PosixFilePermission.GROUP_READ,
          PosixFilePermission.GROUP_EXECUTE,
          PosixFilePermission.OTHERS_READ,
          PosixFilePermission.OTHERS_EXECUTE));
    } catch (UnsupportedOperationException ignored) {
      // not POSIX (e.g., Windows)
    }

    if (writeConfig) {
      String home = installRoot.toAbsolutePath().toString();
      Files.writeString(
          installRoot.resolve("java.properties"),
          "JAVA_HOME=" + home + "\nJAVA=" + home + "/bin/java\n",
          StandardCharsets.UTF_8);
    }
  }

  /** Invokes resolve-java-home.sh with appropriate env. */
  private static ResolveResult runResolver(Path installRoot) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(
        "bash", RESOLVE_SH.toAbsolutePath().toString(), installRoot.toAbsolutePath().toString())
        .redirectErrorStream(true);
    Map<String, String> env = new HashMap<>(System.getenv());
    // Clear JAVA_HOME so each scenario's precedence assertion is deterministic.
    env.remove("JAVA_HOME");
    pb.environment().clear();
    pb.environment().putAll(env);
    pb.directory(installRoot.toFile());

    Process p = pb.start();
    StringBuilder out = new StringBuilder();
    try (InputStream in = p.getInputStream()) {
      byte[] buf = new byte[4096];
      int n;
      while ((n = in.read(buf)) > 0) {
        out.append(new String(buf, 0, n, StandardCharsets.UTF_8));
      }
    }
    p.waitFor(15, TimeUnit.SECONDS);
    return new ResolveResult(p.exitValue(), out.toString());
  }

  private static void assertSuccess(ResolveResult r, String expectedSource, String expectedMajor) {
    assertEquals(0, r.exit, "Expected zero exit. Output:\n" + r.output);
    assertTrue(
        r.output.toLowerCase().contains(expectedSource.toLowerCase()),
        "Output must contain source token '" + expectedSource + "'. Output:\n" + r.output);
    assertTrue(
        r.output.contains(expectedMajor),
        "Output must mention major version " + expectedMajor + ". Output:\n" + r.output);
  }

  private static void assertFailure(ResolveResult r) {
    assertNotEquals(0, r.exit, "Expected non-zero exit. Output:\n" + r.output);
    assertTrue(
        r.output.contains("21"),
        "Output must mention required major version 21. Output:\n" + r.output);
  }

  /** Captured exit code + combined stdout/stderr from the resolver invocation. */
  private record ResolveResult(int exit, String output) {}
}