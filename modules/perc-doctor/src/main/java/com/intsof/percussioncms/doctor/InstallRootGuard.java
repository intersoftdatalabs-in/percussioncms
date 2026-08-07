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
}
