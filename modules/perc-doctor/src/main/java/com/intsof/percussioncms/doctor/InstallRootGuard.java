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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.percussioncms.doctor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Path-safety helpers for doctor operations. All filesystem mutations must stay under a resolved
 * install root; paths that escape the root are rejected.
 */
public final class InstallRootGuard {

  private InstallRootGuard() {}

  /**
   * Normalize {@code installRoot} to an absolute path and require that it exists and is a
   * directory.
   *
   * @param installRoot raw install root (relative or absolute)
   * @return absolute normalized install root
   * @throws IllegalArgumentException if missing, not a directory, or null
   */
  public static Path requireInstallRoot(Path installRoot) {
    Objects.requireNonNull(installRoot, "installRoot");
    Path normalized = installRoot.toAbsolutePath().normalize();
    if (!Files.exists(normalized)) {
      throw new IllegalArgumentException("Install root does not exist: " + normalized);
    }
    if (!Files.isDirectory(normalized)) {
      throw new IllegalArgumentException("Install root is not a directory: " + normalized);
    }
    return normalized;
  }

  /**
   * Returns true if {@code candidate} is the install root itself or a descendant of it (after
   * absolute normalize). Does not follow the candidate as a real path (avoids requiring the file
   * to exist for the check).
   *
   * <p>On Windows (case-insensitive FS), comparison is case-insensitive so {@code c:\percussion}
   * matches descendants resolved as {@code C:\Percussion\...}.
   *
   * @param installRoot install root (relative or absolute)
   * @param candidate path to test
   * @return true when {@code candidate} is under {@code installRoot}
   */
  public static boolean isUnderInstallRoot(Path installRoot, Path candidate) {
    Objects.requireNonNull(installRoot, "installRoot");
    Objects.requireNonNull(candidate, "candidate");
    Path root = installRoot.toAbsolutePath().normalize();
    Path path = candidate.toAbsolutePath().normalize();
    return pathStartsWithRoot(path, root);
  }

  /**
   * Require that {@code candidate} is under {@code installRoot}.
   *
   * @param installRoot install root
   * @param candidate path that must stay under the root
   * @return absolute normalized {@code candidate}
   * @throws IllegalArgumentException if the path escapes the install root
   */
  public static Path requireUnderInstallRoot(Path installRoot, Path candidate) {
    Path root = installRoot.toAbsolutePath().normalize();
    Path path = candidate.toAbsolutePath().normalize();
    if (!pathStartsWithRoot(path, root)) {
      throw new IllegalArgumentException(
          "Path is outside install root: " + path + " (root=" + root + ")");
    }
    return path;
  }

  /**
   * Whether {@code path} is {@code root} or a descendant. Uses {@link Path#startsWith(Path)} first;
   * on case-insensitive platforms (Windows) also compares folded path strings so mixed-case roots
   * still match.
   */
  static boolean pathStartsWithRoot(Path path, Path root) {
    if (path.startsWith(root)) {
      return true;
    }
    if (!isCaseInsensitiveFileSystem()) {
      return false;
    }
    // Path.startsWith is case-sensitive even on Windows; fold for containment only.
    Path pathFolded = Path.of(path.toString().toLowerCase(Locale.ROOT));
    Path rootFolded = Path.of(root.toString().toLowerCase(Locale.ROOT));
    return pathFolded.startsWith(rootFolded);
  }

  /** Windows treats path comparisons as case-insensitive; other OSes stay case-sensitive. */
  static boolean isCaseInsensitiveFileSystem() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    return os.contains("win");
  }

  /**
   * Allowlisted heap-dump file name: ends with {@code .hprof} (case-insensitive). No other
   * extensions are accepted by {@code clean-heap-dumps}.
   *
   * @param fileName bare file name (not a path)
   * @return true if the name is an allowlisted heap dump
   */
  public static boolean isHeapDumpFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return false;
    }
    // Reject path separators smuggled into a "name"
    if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
      return false;
    }
    String lower = fileName.toLowerCase(Locale.ROOT);
    return lower.endsWith(".hprof");
  }

  /**
   * Allowlisted installer / upgrade backup file name for {@code clean-install-backups}.
   *
   * <p>Patterns are drawn from {@code perc-distribution-tree} installer and assembly excludes —
   * not arbitrary user globs:
   *
   * <ul>
   *   <li>{@code AppServer_backup_&lt;timestamp&gt;.zip} (see {@code install.xml} {@code
   *       zip_AppServer})
   *   <li>any file ending with {@code .bak} (assembly / install excludes)
   *   <li>any file ending with {@code .backup} (assembly / install excludes; includes known
   *       {@code *.properties.backup} such as {@code Navigation.properties.backup})
   * </ul>
   *
   * <p>Matching is case-insensitive. Path separators in {@code fileName} are rejected.
   *
   * @param fileName bare file name (not a path)
   * @return true if the name matches an allowlisted install-backup pattern
   */
  public static boolean isInstallBackupFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return false;
    }
    // Reject path separators smuggled into a "name"
    if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
      return false;
    }
    String lower = fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".bak") || lower.endsWith(".backup")) {
      return true;
    }
    // AppServer_backup_${timestamp}.zip — require non-empty timestamp segment
    return isAppServerBackupZipName(lower);
  }

  /**
   * {@code AppServer_backup_<timestamp>.zip} with a non-empty timestamp (case already folded).
   */
  static boolean isAppServerBackupZipName(String lowerFileName) {
    final String prefix = "appserver_backup_";
    final String suffix = ".zip";
    if (!lowerFileName.startsWith(prefix) || !lowerFileName.endsWith(suffix)) {
      return false;
    }
    int midLen = lowerFileName.length() - prefix.length() - suffix.length();
    return midLen > 0;
  }

  /**
   * Relative log directory roots under a CMS install (portable segments joined with {@link
   * Path#of(String, String...)}). Documented in the module README.
   *
   * <ul>
   *   <li>{@code jetty/base/logs} — Jetty / CMS Log4j2 and install layout ({@code install.xml})
   *   <li>{@code jetty/base/modules/perc-logging/logs} — Log4j2 default relative to
   *       perc-logging module config
   *   <li>{@code Deployment/Server/logs} — DTS / Tomcat ({@code catalina.base}/logs)
   * </ul>
   *
   * <p>Missing directories are skipped at walk time (not an error).
   */
  public static final String[] LOG_DIR_RELATIVE = {
    "jetty/base/logs",
    "jetty/base/modules/perc-logging/logs",
    "Deployment/Server/logs"
  };

  /**
   * Resolve allowlisted log directory roots that exist under {@code installRoot}. Only existing
   * directories that stay under the install root are returned.
   *
   * @param installRoot resolved install root
   * @return list of existing log dir paths under the root (may be empty)
   */
  public static java.util.List<Path> existingLogDirs(Path installRoot) {
    Objects.requireNonNull(installRoot, "installRoot");
    Path root = installRoot.toAbsolutePath().normalize();
    java.util.List<Path> dirs = new java.util.ArrayList<>();
    for (String relative : LOG_DIR_RELATIVE) {
      Path dir = resolveRelativeUnderRoot(root, relative);
      if (dir == null) {
        continue;
      }
      if (Files.isDirectory(dir) && isUnderInstallRoot(root, dir)) {
        dirs.add(dir);
      }
    }
    return dirs;
  }

  /**
   * Join a forward-slash relative path under {@code root}. Rejects {@code ..} segments and
   * absolute relative inputs so the result cannot escape the root via the relative string.
   *
   * @return resolved path under root, or null if the relative path is invalid
   */
  static Path resolveRelativeUnderRoot(Path root, String relativeSlashPath) {
    if (relativeSlashPath == null || relativeSlashPath.isEmpty()) {
      return null;
    }
    if (relativeSlashPath.indexOf('\\') >= 0) {
      return null;
    }
    String[] parts = relativeSlashPath.split("/");
    Path resolved = root;
    for (String part : parts) {
      if (part.isEmpty() || ".".equals(part)) {
        continue;
      }
      if ("..".equals(part)) {
        return null;
      }
      resolved = resolved.resolve(part);
    }
    Path normalized = resolved.toAbsolutePath().normalize();
    if (!pathStartsWithRoot(normalized, root.toAbsolutePath().normalize())) {
      return null;
    }
    return normalized;
  }

  /**
   * Allowlisted log file name for {@code clean-logs}: ends with {@code .log}, {@code .log.gz}, or
   * {@code .out} (e.g. {@code catalina.out}). Case-insensitive. Path separators rejected.
   *
   * @param fileName bare file name (not a path)
   * @return true if the name is a log candidate
   */
  public static boolean isLogFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return false;
    }
    if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
      return false;
    }
    String lower = fileName.toLowerCase(Locale.ROOT);
    return lower.endsWith(".log")
        || lower.endsWith(".log.gz")
        || lower.endsWith(".out");
  }

  /**
   * Whether {@code fileName} is an identifiable <em>current / active</em> log that {@code
   * --keep-current} should retain.
   *
   * <p>Current logs are non-compressed {@code *.log} / {@code *.out} basenames that do not embed a
   * rolled date token ({@code yyyy-MM-dd}) and are not numbered backups like {@code name.log.1}.
   * Examples kept: {@code server.log}, {@code catalina.log}, {@code catalina.out}. Examples not
   * current: {@code server-2024-01-15-1.log}, {@code catalina.2024-01-15.log.gz}.
   *
   * @param fileName bare file name
   * @return true if the file should be treated as the active log
   */
  public static boolean isCurrentLogFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return false;
    }
    if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
      return false;
    }
    String lower = fileName.toLowerCase(Locale.ROOT);
    // Compressed / explicitly rolled archives are never "current active"
    if (lower.endsWith(".gz")) {
      return false;
    }
    // name.log.N style (common Tomcat / JUL rotation)
    if (lower.matches(".*\\.log\\.\\d+$") || lower.matches(".*\\.out\\.\\d+$")) {
      return false;
    }
    // Embedded ISO date token used by Log4j2 / Tomcat rolled names
    if (containsIsoDateToken(lower)) {
      return false;
    }
    return lower.endsWith(".log") || lower.endsWith(".out");
  }

  /** True when {@code lowerName} contains a {@code yyyy-MM-dd} token. */
  static boolean containsIsoDateToken(String lowerName) {
    if (lowerName == null || lowerName.length() < 10) {
      return false;
    }
    // Lightweight scan for yyyy-mm-dd without regex overhead on every file
    for (int i = 0; i <= lowerName.length() - 10; i++) {
      if (isDigit(lowerName.charAt(i))
          && isDigit(lowerName.charAt(i + 1))
          && isDigit(lowerName.charAt(i + 2))
          && isDigit(lowerName.charAt(i + 3))
          && lowerName.charAt(i + 4) == '-'
          && isDigit(lowerName.charAt(i + 5))
          && isDigit(lowerName.charAt(i + 6))
          && lowerName.charAt(i + 7) == '-'
          && isDigit(lowerName.charAt(i + 8))
          && isDigit(lowerName.charAt(i + 9))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }
}
