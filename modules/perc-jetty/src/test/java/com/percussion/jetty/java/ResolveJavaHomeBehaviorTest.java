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

package com.percussion.jetty.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.List;
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
 * Behavioral script-invocation tests (FR-013 layer-3) for the runtime {@code resolve-java-home.sh}
 * helper on Linux + macOS.
 *
 * <p>The fixture lives at {@code src/test/resources/fixtures/fake-java-home/jre/bin/java}. The
 * fixture encodes the major version in its parent directory's basename suffix (e.g. {@code
 * fake-java-home-21/.../java}), so the same source file emits different version strings depending
 * on which {@code TempDir} the test creates the install root in. No real JDK installations are
 * required.
 *
 * <p>The test class is a no-op when the fixture or the resolve-java-home script is missing (same
 * guard pattern as {@code DtsInstallerJarContainsPercAntTest}) so IDE / ad-hoc runs are unaffected.
 *
 * <p><b>Scope:</b> Linux + macOS only. Windows requires a real PE-binary launcher fixture (the
 * {@code java.bat} content cannot execute as {@code java.exe}); Windows coverage is provided by the
 * structural tests {@code DtsJavaHomeScriptTest} and {@code ResolveJavaHomeScriptTest} (FR-013
 * layer-2).
 */
class ResolveJavaHomeBehaviorTest {

  private static final Path FIXTURE =
      Path.of("src", "test", "resources", "fixtures", "fake-java-home", "jre", "bin", "java");
  private static final Path RESOLVE_SH = Path.of("src", "main", "jetty", "resolve-java-home.sh");

  @BeforeAll
  static void locateAssets() {
    Assumptions.assumeTrue(
        Files.isRegularFile(FIXTURE),
        "fake-java fixture missing at " + FIXTURE.toAbsolutePath() + "; skipping behavioral tests");
    Assumptions.assumeTrue(
        Files.isRegularFile(RESOLVE_SH),
        "resolve-java-home.sh missing at "
            + RESOLVE_SH.toAbsolutePath()
            + "; skipping behavioral tests");
  }

  /** Scenario 1: config-only happy path (minimum 21 and newer majors). */
  @ParameterizedTest
  @ValueSource(strings = {"21", "25"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void configOnlyHappyPath(String fakeMajor, @TempDir Path scratch) throws Exception {
    runScript(scratch, fakeMajor, true, false, false, "PRODUCT_CONFIG", fakeMajor);
  }

  /** Scenario 2: env-only happy path (minimum 21 and newer majors). */
  @ParameterizedTest
  @ValueSource(strings = {"21", "25"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void envOnlyHappyPath(String fakeMajor, @TempDir Path scratch) throws Exception {
    runScript(scratch, fakeMajor, false, true, false, "PROCESS_ENV", fakeMajor);
  }

  /** Scenario 3: PATH happy path (minimum 21 and newer majors). */
  @ParameterizedTest
  @ValueSource(strings = {"21", "25"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void pathOnlyHappyPath(String fakeMajor, @TempDir Path scratch) throws Exception {
    runScript(scratch, fakeMajor, false, false, true, "PATH", fakeMajor);
  }

  /** Scenario 4: config beats env. */
  @ParameterizedTest
  @ValueSource(strings = {"21", "25"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void configBeatsEnv(String fakeMajor, @TempDir Path scratch) throws Exception {
    runScript(scratch, fakeMajor, true, true, false, "PRODUCT_CONFIG", fakeMajor);
  }

  /** Scenario 5: env rejects wrong major (Java 8). */
  @ParameterizedTest
  @ValueSource(strings = {"8"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void envRejectsWrongMajor(String fakeMajor, @TempDir Path scratch) throws Exception {
    runScriptFailure(scratch, fakeMajor, false, true, false, /*overwriteInvalidConfig*/ false);
  }

  /** Scenario 6: PATH rejects wrong major (Java 8). */
  @ParameterizedTest
  @ValueSource(strings = {"8"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void pathRejectsWrongMajor(String fakeMajor, @TempDir Path scratch) throws Exception {
    runScriptFailure(scratch, fakeMajor, false, false, true, /*overwriteInvalidConfig*/ false);
  }

  /** Scenario 7: config rejects invalid path. */
  @ParameterizedTest
  @ValueSource(strings = {"21"})
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void configRejectsInvalidPath(String fakeMajor, @TempDir Path scratch) throws Exception {
    runScriptFailure(scratch, fakeMajor, true, false, false, /*overwriteInvalidConfig*/ true);
  }

  // --- Helpers ---

  /**
   * Sets up a per-test install root with the fake-java fixture. The {@code @TempDir} parent is
   * renamed to {@code fake-java-home-N-XXXX} so the fixture (which reads the parent dir's suffix)
   * emits the right major version.
   */
  private static Path setupFakeJavaHome(
      Path installRoot, String fakeMajor, boolean writeConfig, boolean overwriteInvalidConfig)
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
      Files.setPosixFilePermissions(
          javaExe,
          Set.of(
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
    if (overwriteInvalidConfig) {
      // Force the resolver to reject the config source. The path is
      // intentionally non-existent so the PRODUCT_CONFIG source fails the
      // existence/launcher checks in the resolver. The companion test
      // (`configRejectsInvalidPath`) does not set envHome or onPath, so the
      // lower-precedence sources are absent and the resolver fails after
      // exhausting all sources; this test exercises the config rejection
      // path specifically, not the fallback chain.
      Files.writeString(
          installRoot.resolve("java.properties"),
          "JAVA_HOME=C:/does/not/exist\nJAVA=C:/does/not/exist/bin/java\n",
          StandardCharsets.UTF_8);
    }
    return installRoot;
  }

  /**
   * Runs the resolver script and asserts the precedence-layer success case. The {@code envHome} /
   * {@code onPath} flags control the parent-process environment (and the {@code PATH}) that is set
   * up before invocation: {@code envHome=true} sets {@code JAVA_HOME} on the parent process to the
   * fake Java home (so the resolver sees PROCESS_ENV); {@code onPath=true} prepends the fake {@code
   * bin} to {@code PATH} (so the resolver sees PATH). {@code writeConfig=true} writes {@code
   * java.properties} so the resolver sees PRODUCT_CONFIG. Each scenario is independent.
   */
  private static void runScript(
      Path scratch,
      String fakeMajor,
      boolean writeConfig,
      boolean envHome,
      boolean onPath,
      String expectedSource,
      String expectedMajor)
      throws Exception {
    Path installRoot = setupFakeJavaHome(scratch, fakeMajor, writeConfig, false);
    ResolveResult r = invokeResolver(installRoot, envHome, onPath);
    assertEquals(0, r.exit, "Expected zero exit. Output:\n" + r.output);
    assertTrue(
        r.output.toLowerCase().contains(expectedSource.toLowerCase()),
        "Output must contain source token '" + expectedSource + "'. Output:\n" + r.output);
    assertTrue(
        r.output.contains(expectedMajor),
        "Output must mention major version " + expectedMajor + ". Output:\n" + r.output);
  }

  /**
   * As {@link #runScript} but asserts the failure path: exit non-zero, output mentions the minimum
   * major version (21 or later). The {@code overwriteInvalidConfig} parameter, when true, replaces
   * the config with a non-existent path so the resolver rejects the PRODUCT_CONFIG source. When the
   * caller additionally passes {@code envHome=true} or {@code onPath=true}, the lower-precedence
   * layers will be tried as part of the failure outcome; when both are false (as in {@code
   * configRejectsInvalidPath}) the test exercises the config-rejection path only and the resolver
   * fails with "no sources" after exhausting all (absent) sources.
   */
  private static void runScriptFailure(
      Path scratch,
      String fakeMajor,
      boolean writeConfig,
      boolean envHome,
      boolean onPath,
      boolean overwriteInvalidConfig)
      throws Exception {
    Path installRoot = setupFakeJavaHome(scratch, fakeMajor, writeConfig, overwriteInvalidConfig);
    ResolveResult r = invokeResolver(installRoot, envHome, onPath);
    assertNotEquals(0, r.exit, "Expected non-zero exit. Output:\n" + r.output);
    assertTrue(
        r.output.contains("21"),
        "Output must mention minimum major version 21. Output:\n" + r.output);
    assertTrue(
        r.output.toLowerCase().contains("or later") || r.output.toLowerCase().contains("minimum"),
        "Output must describe 21 as a minimum (21+). Output:\n" + r.output);
  }

  /**
   * Invokes {@code resolve-java-home.sh} under a controlled environment:
   *
   * <ul>
   *   <li>If {@code envHome} is true, the parent process {@code JAVA_HOME} is set to the fake
   *       install root before launching — so the resolver sees PROCESS_ENV.
   *   <li>If {@code onPath} is true, the parent process {@code PATH} includes only the fake {@code
   *       bin} (plus a tools dir) — so the resolver sees PATH discovery from the fixture alone.
   *   <li>If {@code writeConfig} is true (set during {@link #setupFakeJavaHome}), the resolver also
   *       sees PRODUCT_CONFIG.
   * </ul>
   *
   * <p>PATH is always isolated from the host JVM's PATH. Without isolation the resolver falls
   * through to a real system Java 21 (e.g. {@code /usr/bin/java}) after config/env/fake-Java-8
   * fail, and the negative scenarios incorrectly exit 0. Tools the script needs ({@code awk},
   * {@code sed}, …) are exposed via a dedicated tools directory of symlinks that deliberately omits
   * {@code java}.
   *
   * <p>After invocation we verify that the exit code is reported (with {@code destroyForcibly} on
   * timeout) so a hung child process cannot mask test failures with {@code
   * IllegalThreadStateException}.
   */
  private static ResolveResult invokeResolver(Path installRoot, boolean envHome, boolean onPath)
      throws Exception {
    Map<String, String> env = new HashMap<>(System.getenv());
    // Drop any inherited JAVA_HOME unless the scenario sets it explicitly so that
    // the "config-only" and "PATH" scenarios see a clean env. The "env-only"
    // and "config beats env" scenarios set envHome below.
    env.remove("JAVA_HOME");
    if (envHome) {
      env.put("JAVA_HOME", installRoot.toAbsolutePath().toString());
    }
    // Always isolate PATH so host Java 21 cannot satisfy fallback sources.
    env.put("PATH", buildIsolatedPath(installRoot, onPath));

    // Resolve bash via the host PATH (absolute) so the child need not inherit it.
    String bash = findOnHostPath("bash");
    if (bash == null) {
      bash = "/bin/bash";
    }

    ProcessBuilder pb =
        new ProcessBuilder(
                bash,
                RESOLVE_SH.toAbsolutePath().toString(),
                installRoot.toAbsolutePath().toString())
            .redirectErrorStream(true);
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
    boolean finished = p.waitFor(15, TimeUnit.SECONDS);
    if (!finished) {
      p.destroyForcibly();
      // drain remaining output for diagnostics
      try (InputStream in = p.getInputStream()) {
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
          out.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        }
      }
    }
    int exit = p.exitValue();
    return new ResolveResult(exit, out.toString());
  }

  /**
   * Builds a PATH that never exposes the host {@code java} launcher.
   *
   * <p>On merged-usr systems ({@code /bin → /usr/bin}) putting {@code /bin} or {@code /usr/bin} on
   * PATH would reintroduce system Java and defeat the negative scenarios. Instead we symlink only
   * the utilities the resolver script invokes ({@code awk}, {@code sed}, {@code head}, {@code tr},
   * {@code uname}, …) into a private tools directory under the install root.
   *
   * @param installRoot fixture install root (also the home of the tools dir)
   * @param onPath when true, prepend the fake {@code bin} so PATH discovery sees the fixture
   *     launcher only
   */
  private static String buildIsolatedPath(Path installRoot, boolean onPath) throws Exception {
    Path toolsDir = installRoot.resolve(".resolve-test-tools");
    Files.createDirectories(toolsDir);

    // Utilities used by resolve-java-home.sh (validation + config parsing).
    // Deliberately exclude "java" so PATH discovery cannot fall through to the host.
    for (String cmd : List.of("awk", "sed", "head", "tr", "uname", "dirname", "basename", "cat")) {
      linkHostCommand(toolsDir, cmd);
    }

    StringBuilder path = new StringBuilder();
    if (onPath) {
      path.append(installRoot.resolve("bin").toAbsolutePath());
      path.append(File.pathSeparator);
    }
    path.append(toolsDir.toAbsolutePath());
    return path.toString();
  }

  /**
   * Creates a symlink (or copy fallback) from {@code toolsDir/cmd} to the host executable for
   * {@code cmd}. No-op when the host command cannot be found.
   */
  private static void linkHostCommand(Path toolsDir, String cmd) throws Exception {
    String host = findOnHostPath(cmd);
    if (host == null) {
      return;
    }
    Path link = toolsDir.resolve(cmd);
    if (Files.exists(link)) {
      return;
    }
    Path target = Path.of(host);
    try {
      Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | java.nio.file.FileSystemException ex) {
      // Some environments disallow symlinks; a plain copy still keeps java off PATH.
      Files.copy(target, link, StandardCopyOption.REPLACE_EXISTING);
      link.toFile().setExecutable(true, false);
    }
  }

  /**
   * Locates {@code cmd} on the host process PATH (not the isolated test PATH). Returns an absolute
   * path string, or {@code null} if not found.
   */
  private static String findOnHostPath(String cmd) {
    String hostPath = System.getenv("PATH");
    if (hostPath == null || hostPath.isBlank()) {
      return null;
    }
    for (String dir : hostPath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
      if (dir == null || dir.isBlank()) {
        continue;
      }
      Path candidate = Path.of(dir, cmd);
      if (Files.isExecutable(candidate) && Files.isRegularFile(candidate)) {
        return candidate.toAbsolutePath().toString();
      }
      // Allow following symlinks (e.g. /usr/bin/java -> alternatives); isRegularFile
      // follows links by default. For directory-style shims use isExecutable alone
      // after exists check:
      if (Files.isExecutable(candidate) && Files.exists(candidate)) {
        return candidate.toAbsolutePath().toString();
      }
    }
    return null;
  }

  /** Captured exit code + combined stdout/stderr from the resolver invocation. */
  private record ResolveResult(int exit, String output) {}
}
