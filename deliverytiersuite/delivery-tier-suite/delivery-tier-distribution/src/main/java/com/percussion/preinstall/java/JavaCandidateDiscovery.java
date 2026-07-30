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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Discovers Java candidate homes (major {@link JavaHomeResolver#REQUIRED_MAJOR} or later) on the
 * local host without invoking any launcher executable (so the discovery itself is portable and safe
 * to run during preinstall). Candidates are drawn from:
 *
 * <ol>
 *   <li>the JVM currently running the preinstall,
 *   <li>process {@code JAVA_HOME},
 *   <li>common OS install locations (Linux/macOS /usr/lib/jvm, macOS /Library/Java, Windows {@code
 *       %ProgramFiles%\Java}, {@code %ProgramFiles%\Eclipse Adoptium}),
 *   <li>{@code PATH} entries containing a {@code java} / {@code java.exe} launcher.
 * </ol>
 *
 * <p>Eligible candidates pass the minimum-major check (parsed from a sibling {@code release} file
 * when present) and have an executable launcher.
 */
public final class JavaCandidateDiscovery {

  private JavaCandidateDiscovery() {
    // Static-only.
  }

  /**
   * Returns the ordered, deduplicated list of Java home candidates on this host.
   *
   * @return the candidate list, deduplicated by absolute path; never {@code null}
   */
  public static List<Candidate> discover() {
    return discover(System.getenv(), System.getProperty("java.home"), System.getProperty("PATH"));
  }

  static List<Candidate> discover(
      java.util.Map<String, String> env, String runningJavaHome, String pathValue) {
    Set<Path> seen = new LinkedHashSet<>();
    List<Candidate> result = new ArrayList<>();
    addCandidate(seen, result, runningJavaHome != null ? Path.of(runningJavaHome) : null);
    addCandidate(seen, result, env != null ? readJavaHome(env) : null);
    addCommonOsLocations(seen, result);
    addPathLaunchers(seen, result, pathValue);
    return result;
  }

  /**
   * Filters {@link #discover()} results to eligible (major {@code >=} {@link
   * JavaHomeResolver#REQUIRED_MAJOR}, executable launcher).
   *
   * @param rawCandidates the candidate list to filter; {@code null} yields an empty list
   * @return the eligible candidates preserving input order; never {@code null}
   */
  public static List<Candidate> eligible(List<Candidate> rawCandidates) {
    if (rawCandidates == null) {
      return List.of();
    }
    List<Candidate> eligible = new ArrayList<>();
    for (Candidate c : rawCandidates) {
      if (c.eligible) {
        eligible.add(c);
      }
    }
    return List.copyOf(eligible);
  }

  /**
   * Convenience overload: discovers and filters to eligible in one pass.
   *
   * @return the eligible candidates (see {@link #discover()} and {@link #eligible(List)}); never
   *     {@code null}
   */
  public static List<Candidate> discoverEligible() {
    return eligible(discover());
  }

  // ---- internals -----------------------------------------------------------

  private static Path readJavaHome(java.util.Map<String, String> env) {
    String v = env.get("JAVA_HOME");
    if (v == null || v.isBlank()) {
      return null;
    }
    return Path.of(v);
  }

  private static void addCandidate(Set<Path> seen, List<Candidate> sink, Path home) {
    if (home == null) {
      return;
    }
    Path normalized = home.toAbsolutePath().normalize();
    if (seen.add(normalized)) {
      sink.add(evaluate(home));
    }
  }

  private static Candidate evaluate(Path home) {
    return new Candidate(
        home, readVersion(home), isExecutableLauncher(home.resolve("bin").resolve(launcherName())));
  }

  static String readVersion(Path home) {
    Path release = home.resolve("release");
    if (!Files.isRegularFile(release)) {
      return "";
    }
    try (InputStream in = Files.newInputStream(release)) {
      String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      for (String line : content.split("\\r?\\n")) {
        int eq = line.indexOf('=');
        if (eq <= 0) {
          continue;
        }
        String key = line.substring(0, eq).trim();
        if ("JAVA_VERSION".equals(key)) {
          String value = line.substring(eq + 1).trim().replace("\"", "");
          if (value.startsWith("1.")) {
            String[] parts = value.split("\\.");
            return parts.length >= 2 ? "1." + parts[1] : value;
          }
          int dot = value.indexOf('.');
          return dot > 0 ? value.substring(0, dot) : value;
        }
      }
    } catch (IOException io) {
      return "";
    }
    return "";
  }

  private static boolean isExecutableLauncher(Path launcher) {
    if (launcher == null || !Files.isRegularFile(launcher)) {
      return false;
    }
    String os = System.getProperty("os.name", "");
    if (os.toLowerCase(Locale.ROOT).contains("win")) {
      return true;
    }
    return Files.isExecutable(launcher);
  }

  private static String launcherName() {
    String os = System.getProperty("os.name", "");
    return os.toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
  }

  private static void addCommonOsLocations(Set<Path> seen, List<Candidate> sink) {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (os.contains("win")) {
      addIfDir(seen, sink, Path.of("C:/Program Files/Java"));
      addIfDir(seen, sink, Path.of("C:/Program Files/Eclipse Adoptium"));
      addIfDir(seen, sink, Path.of("C:/Program Files/Microsoft"));
    } else if (os.contains("mac")) {
      addIfDir(seen, sink, Path.of("/Library/Java/JavaVirtualMachines"));
      addIfDir(seen, sink, Path.of("/usr/libexec/java_home"));
    } else {
      addIfDir(seen, sink, Path.of("/usr/lib/jvm"));
      addIfDir(seen, sink, Path.of("/usr/java"));
      addIfDir(seen, sink, Path.of("/opt/jdk"));
    }
  }

  private static void addIfDir(Set<Path> seen, List<Candidate> sink, Path dir) {
    if (!Files.isDirectory(dir)) {
      return;
    }
    try (java.util.stream.Stream<Path> children = Files.list(dir)) {
      for (Path child : (Iterable<Path>) children::iterator) {
        if (Files.isDirectory(child)) {
          addCandidate(seen, sink, child);
        }
      }
    } catch (IOException ignored) {
      // Best-effort; cannot read directory contents.
    }
  }

  private static void addPathLaunchers(Set<Path> seen, List<Candidate> sink, String pathValue) {
    if (pathValue == null || pathValue.isBlank()) {
      return;
    }
    String sep = System.getProperty("path.separator", ":");
    for (String entry : pathValue.split(java.util.regex.Pattern.quote(sep))) {
      if (entry.isBlank()) {
        continue;
      }
      Path dir = Path.of(entry);
      Path launcher = dir.resolve(launcherName());
      if (Files.isRegularFile(launcher)) {
        addCandidate(seen, sink, JavaHomeResolver.inferHomeFromLauncher(launcher));
      }
    }
  }

  /** A single Java home candidate discovered on the host. */
  public static final class Candidate {
    private final Path path;
    private final String versionDisplay;
    private final boolean eligible;

    /**
     * Build a candidate from the discovered path, parsed version display, and launcher
     * executability.
     *
     * @param path absolute or relative home directory discovered on the host; never {@code null}
     * @param versionDisplay major-version display string (typically from the sibling {@code
     *     release} file); {@code null} or blank is treated as "version unknown"
     * @param executable whether the home's launcher is executable on the current platform
     */
    public Candidate(Path path, String versionDisplay, boolean executable) {
      this.path = path;
      this.versionDisplay = versionDisplay == null ? "" : versionDisplay;
      this.eligible = executable && meetsMinimumMajor(this.versionDisplay);
    }

    /**
     * {@code versionDisplay} is typically the major token from {@code release} ({@code "21"},
     * {@code "25"}, or legacy {@code "1.8"}). Accept major {@code >=} {@link
     * JavaHomeResolver#REQUIRED_MAJOR}.
     */
    static boolean meetsMinimumMajor(String versionDisplay) {
      if (versionDisplay == null || versionDisplay.isBlank()) {
        return false;
      }
      // Reuse parser: wrap as a quoted -version style segment.
      int major = JavaHomeResolver.parseMajorVersion("version \"" + versionDisplay + "\"");
      return JavaHomeResolver.isSupportedMajor(major);
    }

    /**
     * Returns the discovered home path.
     *
     * @return the {@link Path} as supplied at construction; never {@code null}
     */
    public Path path() {
      return path;
    }

    /**
     * Returns the parsed major-version display string (e.g. {@code "21"}).
     *
     * @return the display string; empty if the candidate has no sibling {@code release} file
     */
    public String versionDisplay() {
      return versionDisplay;
    }

    /**
     * Returns whether this candidate meets the minimum-major + executable launcher criteria.
     *
     * @return {@code true} when eligible for selection by the install wizard
     */
    public boolean eligible() {
      return eligible;
    }

    @Override
    public String toString() {
      return "Candidate[" + path + " v=" + versionDisplay + " eligible=" + eligible + "]";
    }
  }
}
