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
package com.percussion.distribution.install;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Verifies that the assembled Percussion distribution artifact contains a valid, non-empty {@code
 * jetty/base/lib/jdbc/} directory with real JDBC driver JARs.
 *
 * <p>Java port of {@code modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh}, bound to
 * the Maven {@code verify} phase via {@code exec-maven-plugin:java} so the build gate runs
 * identically on Windows, Linux, and macOS. See {@code
 * modules/perc-distribution-tree/scripts/README.md} and root {@code AGENTS.md} cross-platform
 * rules.
 *
 * <p>Logical exit codes match the original POSIX script (and {@code run(String[])} return values)
 * so unit tests and the Python operator port stay aligned. The {@code main} method must
 * <strong>not</strong> call {@link System#exit} when invoked via {@code exec-maven-plugin:java}:
 * that goal runs in the Maven JVM, so {@code System.exit(0)} after a successful check aborts the
 * reactor mid-verify (no install, no later modules). Non-zero results throw so the mojo fails the
 * build; zero returns normally. Forked CLI runs that need process exit codes can pass {@code
 * -Dperc.build.gate.systemExit=true}.
 */
public final class VerifyJdbcDrivers {

  private static final int EXIT_OK = 0;
  private static final int EXIT_INVOCATION = 1;
  private static final int EXIT_MISSING_OR_EMPTY = 2;
  private static final int EXIT_ZERO_BYTE = 3;
  private static final int EXIT_INVALID_JAR = 4;
  private static final int EXIT_UNPACK_FAILED = 5;
  private static final int EXIT_EXPECTED_MISSING = 6;

  private VerifyJdbcDrivers() {}

  /**
   * Build-gate entry point; asserts the assembled distribution ships the expected JDBC drivers.
   *
   * @param args CLI arguments; supports {@code --artifact <jar>} and {@code --expected-driver-glob
   *     <comma-separated-globs>}.
   */
  public static void main(String[] args) {
    int code;
    try {
      code = run(args);
    } catch (IOException ioe) {
      System.err.println("ERROR: I/O failure: " + ioe.getMessage());
      code = EXIT_UNPACK_FAILED;
    } catch (RuntimeException e) {
      System.err.println("ERROR: " + e.getMessage());
      code = EXIT_INVOCATION;
    }
    BuildGateMains.complete(code, "VerifyJdbcDrivers");
  }

  /**
   * Entry point that returns instead of calling {@link System#exit(int)}; usable from unit tests
   * that do not want to terminate the JVM. {@link IOException} is propagated so callers (production
   * main and unit tests) can choose between logging or letting the JVM terminate.
   */
  static int run(String[] args) throws IOException {
    Options opts;
    try {
      opts = Options.parse(args);
    } catch (IllegalArgumentException e) {
      System.err.println("ERROR: " + e.getMessage());
      printUsage(System.err);
      return EXIT_INVOCATION;
    }
    if (opts.help) {
      printUsage(System.out);
      return EXIT_OK;
    }

    Path artifact = opts.artifact;
    if (!Files.isRegularFile(artifact)) {
      System.err.println("ERROR: artifact not found: " + artifact);
      return EXIT_INVOCATION;
    }

    boolean ownsWorkdir = false;
    Path workdir;
    if (opts.workdir != null) {
      try {
        Files.createDirectories(opts.workdir);
        workdir = opts.workdir;
      } catch (IOException e) {
        System.err.println("ERROR: cannot create workdir: " + opts.workdir);
        return EXIT_INVOCATION;
      }
    } else {
      try {
        // Unique name per invocation (millis + short UUID tail) so concurrent / repeated runs in
        // CI never share the same scratch directory and AV/indexers from previous runs can't keep a
        // file handle on the new one's namespace.
        String suffix =
            "-"
                + Long.toString(System.currentTimeMillis(), 36)
                + "-"
                + java.util.UUID.randomUUID().toString().substring(0, 8);
        workdir = Files.createTempDirectory("verify-jdbc-drivers" + suffix);
        ownsWorkdir = true;
      } catch (IOException e) {
        System.err.println("ERROR: cannot create temp workdir: " + e.getMessage());
        return EXIT_INVOCATION;
      }
    }

    try {
      Path distRoot = workdir.resolve("dist");
      try {
        Files.createDirectories(distRoot);
        unzipQuiet(artifact, distRoot);
      } catch (IOException e) {
        // Surface both class and message so Windows-specific failures (long paths,
        // sharing violations, antivirus interference) show up clearly in CI logs.
        System.err.println(
            "ERROR: failed to unpack artifact: "
                + artifact
                + " ("
                + e.getClass().getName()
                + ": "
                + e.getMessage()
                + ")");
        return EXIT_UNPACK_FAILED;
      }

      Path jdbcDir = findJdbcDir(distRoot);
      if (jdbcDir == null) {
        System.err.println(
            "ERROR: jdbc directory missing: jetty/base/lib/jdbc/ (also tried"
                + " distribution/jetty/base/lib/jdbc/)");
        return EXIT_MISSING_OR_EMPTY;
      }

      CheckResult checked = checkJars(jdbcDir);
      if (checked.total() == 0) {
        System.err.println("ERROR: no JARs found under jetty/base/lib/jdbc/");
        return EXIT_MISSING_OR_EMPTY;
      }
      if (checked.zeroByte() > 0) {
        System.err.println("ERROR: " + checked.zeroByte() + " zero-byte JAR(s) found");
        return EXIT_ZERO_BYTE;
      }
      if (checked.invalid() > 0) {
        System.err.println("ERROR: " + checked.invalid() + " invalid JAR(s) found");
        return EXIT_INVALID_JAR;
      }

      if (opts.expectedSet != null && !opts.expectedSet.isEmpty()) {
        List<String> missing = new ArrayList<>();
        for (String expected : opts.expectedSet) {
          if (!Files.isRegularFile(jdbcDir.resolve(expected))) {
            missing.add(expected);
          }
        }
        if (!missing.isEmpty()) {
          System.err.println(
              "ERROR: expected driver(s) missing from jetty/base/lib/jdbc/:"
                  + String.join(" ", missing));
          return EXIT_EXPECTED_MISSING;
        }
      }

      if (opts.expectedGlobs != null && !opts.expectedGlobs.isEmpty()) {
        List<String> missingGlobs = new ArrayList<>();
        Set<String> jarNames = listJarNames(jdbcDir);
        for (String pattern : opts.expectedGlobs) {
          if (!matchesGlob(jarNames, pattern)) {
            missingGlobs.add(pattern);
          }
        }
        if (!missingGlobs.isEmpty()) {
          System.err.println(
              "ERROR: no JAR matched any of expected driver globs:"
                  + String.join(" ", missingGlobs));
          return EXIT_EXPECTED_MISSING;
        }
      }

      System.out.println(
          "OK: " + checked.total() + " JDBC driver JAR(s) verified under jetty/base/lib/jdbc/");
      return EXIT_OK;
    } finally {
      if (ownsWorkdir) {
        deleteRecursively(workdir);
      }
    }
  }

  static String defaultArtifact(Path moduleDir) {
    return moduleDir.resolve("target").resolve("perc-distribution-tree.jar").toString();
  }

  /**
   * Parses CLI flags. Exposed at package-private visibility for testing.
   *
   * @param args raw CLI arguments (matches the original {@code .sh} contract)
   * @param defaultArtifact absolute path used when {@code --artifact} is omitted
   */
  static Options parseOptions(String[] args, String defaultArtifact) {
    Options opts = new Options();
    opts.artifact = Paths.get(defaultArtifact);
    for (int i = 0; i < args.length; i++) {
      String flag = args[i];
      switch (flag) {
        case "--artifact":
          opts.artifact = Paths.get(requireValue(args, ++i, flag));
          break;
        case "--workdir":
          opts.workdir = Paths.get(requireValue(args, ++i, flag));
          break;
        case "--expected-driver-set":
          opts.expectedSet = splitCsv(requireValue(args, ++i, flag));
          break;
        case "--expected-driver-glob":
          opts.expectedGlobs = splitCsv(requireValue(args, ++i, flag));
          break;
        case "-h":
        case "--help":
          opts.help = true;
          break;
        default:
          throw new IllegalArgumentException("unknown argument: " + flag);
      }
    }
    return opts;
  }

  private static String requireValue(String[] args, int idx, String flag) {
    if (idx >= args.length) {
      throw new IllegalArgumentException(flag + " requires a value");
    }
    return args[idx];
  }

  /**
   * Splits a comma-separated list, preserving empty fields only when the user explicitly included
   * them. Mirrors the {@code IFS=','} loop in the original {@code .sh}.
   */
  static List<String> splitCsv(String csv) {
    if (csv == null || csv.isEmpty()) {
      return Collections.emptyList();
    }
    String[] parts = csv.split(",", -1);
    List<String> out = new ArrayList<>(parts.length);
    for (String p : parts) {
      String trimmed = p.trim();
      if (!trimmed.isEmpty()) {
        out.add(trimmed);
      }
    }
    return out;
  }

  /**
   * Unpacks {@code source} into {@code target} without raising on individual entry errors. Exposed
   * at package-private visibility for testing.
   *
   * <p>Bounded retry: Windows antivirus/indexers briefly hold freshly-created files open, which
   * surfaces as {@code AccessDeniedException}, {@code DirectoryNotEmptyException}, or {@code
   * FileAlreadyExistsException} from a subsequent attempt. We retry the unpack a few times with a
   * short backoff and clear the target between attempts so a partial prior iteration does not
   * contaminate the next one.
   *
   * <p>Case-insensitive Windows path: the fat jar bundles both a top-level {@code LICENSE} file and
   * a top-level {@code license/} directory. The two entries refer to the same filesystem path on
   * NTFS. We pre-scan the archive and deterministically prefer the directory form (skipping the
   * colliding file entry) so the build completes without losing the per-directory license files.
   */
  static void unzipQuiet(Path source, Path target) throws IOException {
    IOException last = null;
    for (int attempt = 1; attempt <= 3; attempt++) {
      wipeTree(target);
      try (InputStream in = Files.newInputStream(source);
          ZipInputStream zin = new ZipInputStream(in)) {
        // First pass: collect entry names and figure out which ones we have to skip on Windows.
        java.util.Map<String, String> skip = java.util.Collections.emptyMap();
        if (isCaseInsensitiveFs()) {
          skip = collectCaseInsensitiveCollisions(source);
        }
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
          String name = entry.getName();
          if (skip.containsKey(name)) {
            // The directory form has already won the case-insensitive collision; skip the file.
            continue;
          }
          Path out = resolveAgainstRoot(target, name);
          if (entry.isDirectory()) {
            Files.createDirectories(out);
          } else {
            Path parent = out.getParent();
            if (parent != null) {
              Files.createDirectories(parent);
            }
            // Files.copy(ZipInputStream, Path, REPLACE_EXISTING) raises FileAlreadyExistsException
            // when target is a non-empty directory. On Windows, case-insensitive paths can also
            // collide across ZIP entries. Delete any pre-existing target so the copy always
            // lands on a freshly-empty path.
            if (Files.exists(out)) {
              if (Files.isDirectory(out)) {
                deleteRecursively(out);
              } else {
                try {
                  Files.deleteIfExists(out);
                } catch (IOException ignored) {
                  // best effort
                }
              }
            }
            Files.copy(zin, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          }
        }
        return;
      } catch (IOException ioe) {
        last = ioe;
      }
      try {
        Thread.sleep(200L * attempt);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw last != null ? last : new IOException("interrupted during unpack retry", ie);
      }
    }
    throw last != null ? last : new IOException("unzip failed after retries");
  }

  /**
   * Returns true if the host filesystem reports case-insensitive path resolution (e.g. Windows
   * NTFS, default macOS APFS). Exposed at package-private visibility for testing.
   */
  static boolean isCaseInsensitiveFs() {
    return java.io.File.separatorChar == '\\'
        || System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac");
  }

  /**
   * Reads the ZIP central directory and returns a map of entry names (the value of {@link
   * ZipEntry#getName()}) that should be skipped because they would case-insensitively collide with
   * another entry whose path is a directory. The colliding directory always wins because the
   * per-directory files in the archive carry the actual license text on this module.
   */
  private static java.util.Map<String, String> collectCaseInsensitiveCollisions(Path source) {
    java.util.Map<String, java.util.Set<String>> lowerToDirNames = new java.util.HashMap<>();
    try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(source.toFile())) {
      java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries();
      while (en.hasMoreElements()) {
        java.util.zip.ZipEntry e = en.nextElement();
        if (e.isDirectory()) {
          // Strip the trailing '/' so a directory entry and a same-named file entry hash to the
          // same key (ZIP files store directories as "name/" and files as "name"; we treat them
          // as the same logical path on case-insensitive filesystems).
          String key = stripTrailingSlash(e.getName()).toLowerCase(java.util.Locale.ROOT);
          lowerToDirNames.computeIfAbsent(key, k -> new java.util.HashSet<>()).add(e.getName());
        }
      }
      // Now walk all entries again; if any FILE entry has a lowercase key already used by a
      // directory, mark the file for skipping.
      java.util.Map<String, String> skip = new java.util.HashMap<>();
      en = zip.entries();
      while (en.hasMoreElements()) {
        java.util.zip.ZipEntry e = en.nextElement();
        if (e.isDirectory()) {
          continue;
        }
        String key = stripTrailingSlash(e.getName()).toLowerCase(java.util.Locale.ROOT);
        java.util.Set<String> dirs = lowerToDirNames.get(key);
        if (dirs != null && !dirs.contains(e.getName())) {
          // A same-case-insensitive directory already exists; skip the file. The directory's
          // contents (e.g. license/LICENSE) carry the actual content.
          skip.put(e.getName(), dirs.iterator().next());
        }
      }
      return skip;
    } catch (IOException ignored) {
      // best effort — if we cannot read the central directory, proceed without skipping
      return java.util.Collections.emptyMap();
    }
  }

  private static String stripTrailingSlash(String s) {
    if (s == null) {
      return "";
    }
    return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
  }

  /**
   * Best-effort recursive delete of {@code root}, used to clear a temp directory between retry
   * attempts. Missing root is a no-op.
   */
  private static void wipeTree(Path root) {
    if (root == null || !Files.exists(root)) {
      return;
    }
    try (var paths = Files.walk(root)) {
      paths
          .sorted((a, b) -> b.compareTo(a))
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignored) {
                  // best effort
                }
              });
    } catch (IOException ignored) {
      // best effort
    }
  }

  /** Recursive delete of {@code p}, swallowing any I/O exceptions. */
  private static void deleteRecursively(Path p) {
    if (p == null || !Files.exists(p)) {
      return;
    }
    try (var paths = Files.walk(p)) {
      paths
          .sorted((a, b) -> b.compareTo(a))
          .forEach(
              q -> {
                try {
                  Files.deleteIfExists(q);
                } catch (IOException ignored) {
                  // best effort
                }
              });
    } catch (IOException ignored) {
      // best effort
    }
  }

  /**
   * Resolves a Zip entry name against {@code root}, defending against {@link
   * java.nio.file.InvalidPathException} (e.g. zip-slip-style absolute or root-anchored names on
   * Windows). Returns a path under {@code root} only.
   */
  private static Path resolveAgainstRoot(Path root, String name) throws IOException {
    Path resolved = root.resolve(name).normalize();
    if (!resolved.startsWith(root)) {
      throw new IOException("zip entry escapes target root: " + name);
    }
    return resolved;
  }

  static Path findJdbcDir(Path distRoot) {
    for (String rel : Arrays.asList("jetty/base/lib/jdbc", "distribution/jetty/base/lib/jdbc")) {
      Path p = distRoot.resolve(rel);
      if (Files.isDirectory(p)) {
        return p;
      }
    }
    return null;
  }

  /**
   * Inspects every {@code *.jar} under {@code jdbcDir} and counts totals / zero-byte / invalid.
   * Exposed at package-private visibility for testing.
   */
  static CheckResult checkJars(Path jdbcDir) throws IOException {
    int total = 0;
    int zero = 0;
    int invalid = 0;
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(jdbcDir, "*.jar")) {
      for (Path jar : ds) {
        total++;
        String name = jar.getFileName().toString();
        long size = Files.size(jar);
        if (size == 0L) {
          System.out.println("  [FAIL] " + name + " - zero bytes");
          zero++;
          continue;
        }
        if (!isValidJar(jar)) {
          System.out.println("  [FAIL] " + name + " - not a valid JAR");
          invalid++;
          continue;
        }
        System.out.println("  [ OK ] " + name + " - " + size + " bytes");
      }
    }
    return new CheckResult(total, zero, invalid);
  }

  /** Reads the JAR via {@link ZipFile}, swallowing malformed-archive errors. */
  static boolean isValidJar(Path jar) {
    try (ZipFile zf = new ZipFile(jar.toFile())) {
      // iterating entries forces validation; an IOException thrown here
      // (e.g. ZipException invalid CEN header) means the JAR is malformed
      return zf.stream().findAny().isPresent();
    } catch (IOException e) {
      return false;
    }
  }

  static Set<String> listJarNames(Path jdbcDir) throws IOException {
    Set<String> names = new LinkedHashSet<>();
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(jdbcDir, "*.jar")) {
      for (Path p : ds) {
        names.add(p.getFileName().toString());
      }
    }
    return names;
  }

  /**
   * Returns true if at least one {@code candidateName} matches the shell-glob {@code pattern}.
   * Implements the subset of POSIX globbing the {@code .sh} relies on: {@code *} matches any run of
   * characters (including empty), {@code ?} matches exactly one character. Does not implement
   * character classes.
   */
  static boolean matchesGlob(Set<String> candidateNames, String pattern) {
    if (pattern == null || pattern.isEmpty()) {
      return true;
    }
    String regex = globToRegex(pattern);
    for (String name : candidateNames) {
      if (name.matches(regex)) {
        return true;
      }
    }
    return false;
  }

  static String globToRegex(String pattern) {
    StringBuilder sb = new StringBuilder("^");
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      switch (c) {
        case '*':
          sb.append(".*");
          break;
        case '?':
          sb.append('.');
          break;
        case '\\':
          // literal backslash in the input → match a single backslash in the regex
          sb.append("\\\\");
          break;
        case '.':
        case '(':
        case ')':
        case '+':
        case '|':
        case '^':
        case '$':
        case '@':
        case '%':
        case '{':
        case '}':
        case '[':
        case ']':
          sb.append('\\').append(c);
          break;
        default:
          sb.append(c);
      }
    }
    sb.append('$');
    return sb.toString();
  }

  private static void printUsage(java.io.PrintStream out) {
    out.println(
        "Usage: VerifyJdbcDrivers [--artifact <path>] [--workdir <dir>]"
            + " [--expected-driver-set <csv>] [--expected-driver-glob <csv>]");
    out.println(
        "  --artifact <path>            Path to perc-distribution-tree.jar"
            + " (default: <module>/target/perc-distribution-tree.jar)");
    out.println("  --workdir <dir>              Scratch dir for unpacking (default: temp)");
    out.println(
        "  --expected-driver-set <csv>  Comma-separated exact driver filenames that must"
            + " be present (default: empty)");
    out.println(
        "  --expected-driver-glob <csv> Comma-separated globs; for each glob at least"
            + " one matching JAR must be present (default: empty)");
  }

  /** Immutable holder returned by {@link #checkJars(Path)}. */
  record CheckResult(int total, int zeroByte, int invalid) {}

  static final class Options {
    Path artifact;
    Path workdir;
    List<String> expectedSet;
    List<String> expectedGlobs;
    boolean help;

    static Options parse(String[] args) {
      return parseOptions(args, defaultArtifact(Paths.get("").toAbsolutePath()));
    }
  }
}
