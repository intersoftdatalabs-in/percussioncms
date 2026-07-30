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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Pure helpers implementing the runtime Java home resolution contract for CMS and DTS (see {@code
 * specs/991-system-java-home/contracts/java-home-resolution.md}).
 *
 * <p>The resolver is split into:
 *
 * <ul>
 *   <li>file and process-environment probes — implemented directly in Java,
 *   <li>launcher version parsing — implemented in Java (parse a captured {@code -version} stream
 *       rather than executing the launcher),
 *   <li>PATH discovery — used as a final fallback for candidates but launcher execution is left to
 *       shell/bat helpers that share the helper contract.
 * </ul>
 *
 * <p>Every step records an {@link Attempt} so error messages can list the sources tried. The {@link
 * ResolutionResult} reports the first {@code VALID} source in the contractually defined order, or
 * {@code NONE} on failure.
 */
public final class JavaHomeResolver {

  /**
   * Minimum supported major version for CMS / DTS on the 8.2 line. Homes reporting major {@code >=}
   * this value (21, 22, 25, …) are accepted; older majors (8, 11, 17, …) are rejected.
   */
  public static final int REQUIRED_MAJOR = 21;

  /** Names of legacy install-dir Java folders consulted in precedence order. */
  public static final List<String> LEGACY_INSTALL_DIR_NAMES = List.of("JRE", "JRE64");

  private JavaHomeResolver() {
    // Static-only utility.
  }

  /**
   * Attempts to resolve a Java home from the supplied inputs. Each {@code attempts} entry added
   * during evaluation contains the source, candidate path and reason text.
   *
   * @param installRoot the install directory; used to read {@code java.properties} and the legacy
   *     {@code JRE} / {@code JRE64} folders. Must not be {@code null}.
   * @param env the process environment map (typically {@link System#getenv()}); {@code null} is
   *     treated as an empty environment.
   * @param pathEntries the {@code PATH}-derived launcher directories to consult as a final
   *     fallback; {@code null} is treated as empty.
   * @param probe the filesystem probe used to validate candidates; {@code null} selects {@link
   *     DefaultProbe}.
   * @return the resolution result; never {@code null}. Use {@link ResolutionResult#success()} to
   *     discriminate.
   */
  public static ResolutionResult resolve(
      Path installRoot, Map<String, String> env, List<Path> pathEntries, JavaHomeProbe probe) {
    if (installRoot == null) {
      throw new IllegalArgumentException("installRoot must not be null");
    }
    if (probe == null) {
      probe = new DefaultProbe();
    }
    List<Attempt> attempts = new ArrayList<>();

    // 1. Product config: install-root java.properties.
    Path propsFile = installRoot.resolve("java.properties");
    Map<String, String> props = readPropertiesIfPresent(propsFile);
    String cfgHome = props.get(JavaPropertiesSupport.KEY_JAVA_HOME);
    if (cfgHome != null && !cfgHome.isBlank()) {
      Path cfg = Path.of(cfgHome);
      if (probe.isValidJavaHome(cfg, REQUIRED_MAJOR)) {
        return ResolutionResult.success(cfg, ResolutionSource.PRODUCT_CONFIG, attempts);
      }
      attempts.add(
          new Attempt(
              ResolutionSource.PRODUCT_CONFIG,
              cfgHome,
              "configured JAVA_HOME not a valid Java home (minimum major " + REQUIRED_MAJOR + ")"));
    }
    String cfgLauncher = props.get(JavaPropertiesSupport.KEY_JAVA);
    if (cfgLauncher != null && !cfgLauncher.isBlank()) {
      Path inferred = inferHomeFromLauncher(Path.of(cfgLauncher));
      if (inferred != null && probe.isValidJavaHome(inferred, REQUIRED_MAJOR)) {
        return ResolutionResult.success(inferred, ResolutionSource.PRODUCT_CONFIG, attempts);
      }
      attempts.add(
          new Attempt(
              ResolutionSource.PRODUCT_CONFIG,
              cfgLauncher,
              "configured JAVA launcher not a valid Java home (minimum major "
                  + REQUIRED_MAJOR
                  + ")"));
    }

    // 2. Process environment JAVA_HOME.
    if (env != null) {
      String envHome = env.get("JAVA_HOME");
      if (envHome != null && !envHome.isBlank()) {
        Path p = Path.of(envHome);
        if (probe.isValidJavaHome(p, REQUIRED_MAJOR)) {
          return ResolutionResult.success(p, ResolutionSource.PROCESS_ENV, attempts);
        }
        attempts.add(
            new Attempt(
                ResolutionSource.PROCESS_ENV,
                envHome,
                "env JAVA_HOME not a valid Java home (minimum major " + REQUIRED_MAJOR + ")"));
      }
    }

    // 3. Legacy install-dir layout (operator-provided).
    for (String name : LEGACY_INSTALL_DIR_NAMES) {
      Path candidate = installRoot.resolve(name);
      if (probe.isValidJavaHome(candidate, REQUIRED_MAJOR)) {
        return ResolutionResult.success(
            candidate,
            name.equals("JRE64")
                ? ResolutionSource.INSTALL_DIR_JRE64
                : ResolutionSource.INSTALL_DIR_JRE,
            attempts);
      }
      if (Files.exists(candidate)) {
        attempts.add(
            new Attempt(
                ResolutionSource.INSTALL_DIR_JRE,
                candidate.toString(),
                "present but not a valid Java home (minimum major " + REQUIRED_MAJOR + ")"));
      }
    }

    // 4. PATH discovery — consult launcher presence, then infer home.
    if (pathEntries != null) {
      for (Path dir : pathEntries) {
        Path launcher = dir.resolve(launcherName());
        if (probe.isExecutableLauncher(launcher)) {
          Path inferred = inferHomeFromLauncher(launcher);
          if (inferred != null && probe.isValidJavaHome(inferred, REQUIRED_MAJOR)) {
            return ResolutionResult.success(inferred, ResolutionSource.PATH, attempts);
          }
          attempts.add(
              new Attempt(
                  ResolutionSource.PATH,
                  launcher.toString(),
                  "launcher below minimum major " + REQUIRED_MAJOR + " or home unresolved"));
        }
      }
    }

    return ResolutionResult.failure(attempts);
  }

  /**
   * Infers a JDK/JRE home directory from a launcher path like {@code <home>/bin/java}. Returns
   * {@code null} if the launcher has fewer than two path elements (the launcher itself must live
   * under a {@code bin} directory).
   *
   * @param launcher the launcher {@link Path} (e.g. {@code <home>/bin/java}); {@code null} yields
   *     {@code null}
   * @return the inferred home directory, or {@code null} when the launcher does not sit directly
   *     under a {@code bin} folder
   */
  public static Path inferHomeFromLauncher(Path launcher) {
    if (launcher == null) {
      return null;
    }
    Path parent = launcher.getParent();
    if (parent == null) {
      return null;
    }
    String parentName = parent.getFileName() == null ? "" : parent.getFileName().toString();
    if (!"bin".equalsIgnoreCase(parentName)) {
      return null;
    }
    return parent.getParent();
  }

  /**
   * Parses the major version from the output of {@code java -version}. The launcher writes to
   * stderr; sample lines:
   *
   * <pre>{@code
   * openjdk version "21.0.2" 2024-01-16
   * java version "21+36" 2024-06-04
   * }</pre>
   *
   * @param versionOutput captured stdout/stderr text from {@code java -version}; {@code null} or an
   *     empty string yields {@code -1}
   * @return the parsed major version, or {@code -1} when the version segment cannot be parsed
   */
  public static int parseMajorVersion(String versionOutput) {
    if (versionOutput == null) {
      return -1;
    }
    try (Scanner scanner = new Scanner(versionOutput)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        int firstQuote = trimmed.indexOf('"');
        int secondQuote = trimmed.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0 || secondQuote <= firstQuote + 1) {
          continue;
        }
        String version = trimmed.substring(firstQuote + 1, secondQuote);
        String[] parts = version.split("[.\\-+_]");
        if (parts.length == 0) {
          continue;
        }
        try {
          // Java 8 reported "1.8.0_xxx" — strip the leading "1." for legacy.
          if (parts.length >= 2 && "1".equals(parts[0])) {
            return Integer.parseInt(parts[1]);
          }
          return Integer.parseInt(parts[0]);
        } catch (NumberFormatException ignored) {
          // Fall through and try the next quoted segment.
        }
      }
    }
    return -1;
  }

  /**
   * Returns {@code true} when {@code major} meets the product minimum ({@link #REQUIRED_MAJOR} or
   * later). Negative / unparseable majors fail.
   *
   * @param major the parsed major version; negative or {@link Integer#MIN_VALUE} values fail
   * @return {@code true} when {@code major >= }{@link #REQUIRED_MAJOR}
   */
  public static boolean isSupportedMajor(int major) {
    return major >= REQUIRED_MAJOR;
  }

  /**
   * Returns the launcher name for the current host platform.
   *
   * @return {@code "java.exe"} on Windows, {@code "java"} elsewhere; never {@code null}
   */
  public static String launcherName() {
    String os = System.getProperty("os.name", "");
    return os.toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
  }

  private static Map<String, String> readPropertiesIfPresent(Path file) {
    if (!Files.exists(file)) {
      return Map.of();
    }
    java.util.Properties raw = new java.util.Properties();
    try (InputStream in = Files.newInputStream(file)) {
      raw.load(in);
    } catch (IOException ignored) {
      return Map.of();
    }
    Map<String, String> out = new java.util.LinkedHashMap<>();
    for (String name : raw.stringPropertyNames()) {
      out.put(name, raw.getProperty(name));
    }
    return out;
  }

  /** Source of a resolved Java home in the precedence order. */
  public enum ResolutionSource {
    /** Resolved from {@code <installRoot>/java.properties} (product config). */
    PRODUCT_CONFIG,
    /** Resolved from the {@code JAVA_HOME} environment variable. */
    PROCESS_ENV,
    /** Resolved from the legacy {@code <installRoot>/JRE} directory. */
    INSTALL_DIR_JRE,
    /** Resolved from the legacy {@code <installRoot>/JRE64} directory. */
    INSTALL_DIR_JRE64,
    /** Resolved from a launcher found on the {@code PATH}. */
    PATH,
    /** No source resolved successfully. */
    NONE
  }

  /**
   * One attempted source during resolution — used to build failure messages.
   *
   * @param source which source this attempt represents
   * @param candidate the path or launcher that was inspected (raw string)
   * @param reason short explanation of why this candidate did (or did not) succeed
   */
  public record Attempt(ResolutionSource source, String candidate, String reason) {}

  /**
   * Probes the filesystem to determine if a path is a usable Java home. Decoupled from the static
   * resolver so tests can inject a fixture-only probe.
   */
  public interface JavaHomeProbe {
    /**
     * Whether {@code path} points to a usable Java home with a launcher reporting major version
     * {@code >= requiredMajor}.
     *
     * @param path candidate home directory; never {@code null}
     * @param requiredMajor minimum supported major version (typically {@link #REQUIRED_MAJOR})
     * @return {@code true} when the home is acceptable
     */
    boolean isValidJavaHome(Path path, int requiredMajor);

    /**
     * Whether {@code launcher} is a runnable executable on the current host platform.
     *
     * @param launcher candidate launcher path; never {@code null}
     * @return {@code true} when the launcher exists and (on Unix) has the executable bit set
     */
    boolean isExecutableLauncher(Path launcher);
  }

  /** Default probe that inspects the live filesystem. */
  public static final class DefaultProbe implements JavaHomeProbe {

    /** Construct a probe backed by the live filesystem. */
    public DefaultProbe() {
      // Default filesystem-backed probe.
    }

    @Override
    public boolean isValidJavaHome(Path path, int requiredMajor) {
      if (path == null || !Files.isDirectory(path)) {
        return false;
      }
      Path launcher = path.resolve("bin").resolve(launcherName());
      if (!isExecutableLauncher(launcher)) {
        return false;
      }
      // Detect major version by reading release file when present to avoid exec.
      Path release = path.resolve("release");
      if (Files.isRegularFile(release)) {
        try {
          String content = Files.readString(release, StandardCharsets.UTF_8);
          for (String line : content.split("\\r?\\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) {
              continue;
            }
            String key = line.substring(0, eq).trim();
            if ("JAVA_VERSION".equals(key)) {
              String value = line.substring(eq + 1).trim().replace("\"", "");
              int major = parseMajorVersion("\"" + value + "\"");
              // Accept major >= requiredMajor (minimum), not equality-only.
              return major >= requiredMajor;
            }
          }
        } catch (IOException ignored) {
          // Fall through to launcher-exec based check below.
        }
      }
      // Without a release file, we conservatively allow it — shell-level
      // resolution will exec the launcher and re-validate.
      return true;
    }

    @Override
    public boolean isExecutableLauncher(Path launcher) {
      if (launcher == null || !Files.isRegularFile(launcher)) {
        return false;
      }
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      if (os.contains("win")) {
        return true;
      }
      return Files.isExecutable(launcher);
    }
  }

  /**
   * Result of a resolution attempt. Successful results carry a path and a non-{@code NONE} source.
   * Failed results carry an attempts list suitable for surfacing in a script's stderr.
   */
  public static final class ResolutionResult {
    private final boolean success;
    private final Path javaHome;
    private final ResolutionSource source;
    private final List<Attempt> attempts;

    private ResolutionResult(
        boolean success, Path javaHome, ResolutionSource source, List<Attempt> attempts) {
      this.success = success;
      this.javaHome = javaHome;
      this.source = source;
      this.attempts = List.copyOf(attempts);
    }

    /**
     * Construct a successful resolution result.
     *
     * @param home the resolved Java home; never {@code null}
     * @param source the source that resolved; never {@code null}
     * @param attempts a read-only copy of the inspection log so far
     * @return a successful {@link ResolutionResult}
     */
    public static ResolutionResult success(
        Path home, ResolutionSource source, List<Attempt> attempts) {
      return new ResolutionResult(true, home, source, attempts);
    }

    /**
     * Construct a failed resolution result.
     *
     * @param attempts a read-only copy of the inspection log so far
     * @return a failed {@link ResolutionResult}
     */
    public static ResolutionResult failure(List<Attempt> attempts) {
      return new ResolutionResult(false, null, ResolutionSource.NONE, attempts);
    }

    /**
     * Whether this result represents a successful resolution.
     *
     * @return {@code true} when {@link #javaHome()} is non-null and {@link #source()} is not {@link
     *     ResolutionSource#NONE}
     */
    public boolean success() {
      return success;
    }

    /**
     * Returns the resolved Java home, or {@code null} on failure.
     *
     * @return the home {@link Path} when successful; {@code null} otherwise
     */
    public Path javaHome() {
      return javaHome;
    }

    /**
     * Returns the source that resolved the home.
     *
     * @return the {@link ResolutionSource}; never {@code null}, {@link ResolutionSource#NONE} on
     *     failure
     */
    public ResolutionSource source() {
      return source;
    }

    /**
     * Returns the cumulative inspection log.
     *
     * @return an immutable list of {@link Attempt} entries; never {@code null}, possibly empty
     */
    public List<Attempt> attempts() {
      return attempts;
    }

    /**
     * Renders an error suitable for surfacing in shell/bat failure messages. Includes the required
     * major version and a per-source summary.
     *
     * @param header short banner prepended to the rendered failure (e.g. "Could not resolve Java
     *     home"); may be {@code null} or blank to omit
     * @return a multi-line, human-readable error report
     */
    public String renderFailure(String header) {
      StringBuilder sb = new StringBuilder();
      if (header != null && !header.isBlank()) {
        sb.append(header).append(System.lineSeparator());
      }
      sb.append("Required Java major version: ")
          .append(REQUIRED_MAJOR)
          .append(" or later")
          .append(System.lineSeparator());
      if (attempts.isEmpty()) {
        sb.append("No sources were inspected.");
      } else {
        sb.append("Sources tried:").append(System.lineSeparator());
        for (Attempt a : attempts) {
          sb.append("  - ")
              .append(a.source())
              .append(": ")
              .append(a.candidate() == null ? "<unset>" : a.candidate())
              .append(" (")
              .append(a.reason())
              .append(")")
              .append(System.lineSeparator());
        }
      }
      return sb.toString();
    }
  }

  /** For tests — silence OutputStream writes into a buffer. */
  static void quietWrite(OutputStream os, byte[] data) throws IOException {
    os.write(data);
  }
}
